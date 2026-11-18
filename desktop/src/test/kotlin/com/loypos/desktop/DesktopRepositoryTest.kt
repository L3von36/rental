package com.loypos.desktop

import com.loypos.desktop.core.DesktopAvailabilityStatus
import com.loypos.desktop.core.buildMonthCalendarGrid
import com.loypos.desktop.core.checkCartAvailabilityConflicts
import com.loypos.desktop.core.computeEquipmentAvailabilityForRange
import com.loypos.desktop.core.computeRentalTotals
import com.loypos.desktop.core.dayBoundsFor
import com.loypos.desktop.core.dayStartMs
import com.loypos.desktop.core.schedulesForDate
import com.loypos.desktop.core.upcomingRentalsFrom
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopCartLine
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopRepositoryTest {
    private fun tempRepo(): Pair<DesktopRepository, File> {
        val dir = Files.createTempDirectory("loypos-desktop-test").toFile()
        return DesktopRepository(dir) to dir
    }

    @Test
    fun seedsEventRentalDemoFleetAndCategories() {
        val (repo, dir) = tempRepo()
        try {
            val snap = repo.state.value
            assertEquals(8, snap.categories.size)
            assertEquals(52, snap.items.size)
            assertTrue(snap.items.any { it.id == "eq-001" && it.name.contains("Canopy Tent") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun multiDayRentalCheckoutDeductsStockAndRestoresOnReturn() {
        val (repo, dir) = tempRepo()
        try {
            val tent = repo.state.value.items.first { it.id == "eq-001" }
            val initialStock = tent.stock

            val order = repo.checkoutRental(
                lines = listOf(
                    DesktopCartLine(
                        itemId = tent.id,
                        name = tent.name,
                        dailyRateCents = tent.priceCents,
                        qty = 2.0,
                        rentalDays = 3,
                    )
                ),
                rentalDays = 3,
                paymentType = "card",
                paidCents = 0L,
                discountPercent = 0.0,
                fixedDiscountCents = 0L,
                depositCents = 5_000L,
                client = repo.state.value.clients.firstOrNull(),
                eventName = "City Gala",
                eventVenue = "Convention Plaza",
                note = "Deliver Friday AM",
            )

            assertEquals(3, order.rentalDays)
            assertEquals(tent.priceCents * 2L * 3L, order.totalCents)
            assertEquals("out", order.rentalStatus)
            assertEquals(5_000L, order.depositCents)

            val afterCheckout = repo.state.value.items.first { it.id == tent.id }
            assertEquals(initialStock - 2.0, afterCheckout.stock, 0.001)

            // Mark returned restores rented stock
            assertTrue(repo.markRentalReturned(order.id))
            val returnedOrder = repo.state.value.orders.first { it.id == order.id }
            assertTrue(returnedOrder.isReturned)
            assertNotNull(returnedOrder.returnedAtMs)

            val afterReturn = repo.state.value.items.first { it.id == tent.id }
            assertEquals(initialStock, afterReturn.stock, 0.001)

            // Double return is rejected
            assertFalse(repo.markRentalReturned(order.id))

            // Refunding an already-returned rental must NOT double-restore stock
            assertTrue(repo.refundRental(order.id))
            val afterRefund = repo.state.value.items.first { it.id == tent.id }
            assertEquals(initialStock, afterRefund.stock, 0.001)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun jsonBackupExportAndImportPreservesRentalOrders() {
        val (repo1, dir1) = tempRepo()
        val (repo2, dir2) = tempRepo()
        try {
            val chair = repo1.state.value.items.first { it.id == "eq-008" }
            repo1.checkoutRental(
                lines = listOf(DesktopCartLine(chair.id, chair.name, chair.priceCents, 10.0, 2)),
                rentalDays = 2,
                paymentType = "cash",
                paidCents = 10_000L,
                discountPercent = 10.0,
                fixedDiscountCents = 0L,
                depositCents = 2_000L,
                client = null,
                eventName = "Outdoor Reception",
                eventVenue = "Botanical Garden",
                note = "",
            )

            val backupFile = File(dir1, "backup.json")
            repo1.exportBackupJson(backupFile)
            assertTrue(backupFile.exists())

            repo2.importBackupJson(backupFile)
            assertEquals(1, repo2.state.value.orders.size)
            assertEquals("Outdoor Reception", repo2.state.value.orders.first().eventName)
            assertEquals(2, repo2.state.value.orders.first().rentalDays)
        } finally {
            dir1.deleteRecursively()
            dir2.deleteRecursively()
        }
    }

    @Test
    fun rentalDiscountAndExclusiveTaxCalculation() {
        val calc = computeRentalTotals(
            rawSubtotalCents = 10_000L,
            discountPercent = 10.0,
            fixedDiscountCents = 0L,
            taxRatePercent = 15.0,
            taxIncluded = false,
        )
        assertEquals(1_000L, calc.discountCents)
        assertEquals(1_350L, calc.taxCents)
        assertEquals(10_350L, calc.totalCents)
    }

    @Test
    fun calendarTracksSchedulesEquipmentAvailabilityAndDateConflicts() {
        val (repo, dir) = tempRepo()
        try {
            val tent = repo.state.value.items.first { it.id == "eq-004" } // 30x60 Clear-Span Event Tent (3 total units)
            val totalFleet = tent.stock // 3.0
            assertEquals(3.0, totalFleet, 0.001)

            val eventDate = LocalDate.of(2026, 12, 10)
            val eventStartMs = dayStartMs(eventDate)

            repo.checkoutRental(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 2.0, 3)),
                rentalDays = 3,
                paymentType = "card",
                paidCents = 0L,
                discountPercent = 0.0,
                fixedDiscountCents = 0L,
                depositCents = 10_000L,
                client = repo.state.value.clients.first(),
                eventName = "Winter Ballroom Gala",
                eventVenue = "Harbor Hall",
                note = "",
                rentalStartMs = eventStartMs,
            )

            val snap = repo.state.value
            val grid = buildMonthCalendarGrid(YearMonth.of(2026, 12), snap.orders)
            assertEquals(42, grid.size)
            val dec11Cell = grid.first { it.date == LocalDate.of(2026, 12, 11) }
            assertEquals(1, dec11Cell.activeCount)

            val dec11Schedules = schedulesForDate(LocalDate.of(2026, 12, 11), snap.orders)
            assertEquals(1, dec11Schedules.size)
            assertEquals("Winter Ballroom Gala", dec11Schedules.first().eventName)

            val dec20Schedules = schedulesForDate(LocalDate.of(2026, 12, 20), snap.orders)
            assertTrue(dec20Schedules.isEmpty())

            val upcoming = upcomingRentalsFrom(LocalDate.of(2026, 12, 10), snap.orders)
            assertEquals(1, upcoming.size)

            // Check equipment availability on Dec 11: 2 reserved out of 3 fleet -> 1 available
            val dec11Bounds = dayBoundsFor(LocalDate.of(2026, 12, 11))
            val dec11Avail = computeEquipmentAvailabilityForRange(
                snap.items, snap.orders, dec11Bounds.startMs, dec11Bounds.endMs
            ).first { it.item.id == tent.id }
            assertEquals(3.0, dec11Avail.totalCapacity, 0.001)
            assertEquals(2.0, dec11Avail.reservedUnits, 0.001)
            assertEquals(1.0, dec11Avail.availableUnits, 0.001)
            assertEquals(DesktopAvailabilityStatus.LOW, dec11Avail.status)

            // Attempting to book 2 tents overlapping Dec 11 should flag a conflict
            val conflicts = checkCartAvailabilityConflicts(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 2.0, 2)),
                catalog = snap.items,
                orders = snap.orders,
                rangeStartMs = dayStartMs(LocalDate.of(2026, 12, 11)),
                rangeEndMs = dayStartMs(LocalDate.of(2026, 12, 13)),
            )
            assertEquals(1, conflicts.size)
            assertEquals(1.0, conflicts.first().availableQty, 0.001)

            // Non-overlapping later date (Dec 20..22) has full 3.0 capacity available
            val dec20Conflicts = checkCartAvailabilityConflicts(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 3.0, 2)),
                catalog = snap.items,
                orders = snap.orders,
                rangeStartMs = dayStartMs(LocalDate.of(2026, 12, 20)),
                rangeEndMs = dayStartMs(LocalDate.of(2026, 12, 22)),
            )
            assertTrue(dec20Conflicts.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
