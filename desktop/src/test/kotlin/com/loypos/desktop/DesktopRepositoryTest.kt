package com.loypos.desktop

import com.loypos.desktop.core.DesktopAvailabilityStatus
import com.loypos.desktop.core.DesktopQrCodeUtils
import com.loypos.desktop.core.DesktopRentalAgreementEngine
import com.loypos.desktop.core.ParsedDesktopQrCode
import com.loypos.desktop.core.buildMonthCalendarGrid
import com.loypos.desktop.core.checkCartAvailabilityConflicts
import com.loypos.desktop.core.computeEquipmentAvailabilityForRange
import com.loypos.desktop.core.computeRentalTotals
import com.loypos.desktop.core.dayBoundsFor
import com.loypos.desktop.core.dayStartMs
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.schedulesForDate
import com.loypos.desktop.core.upcomingRentalsFrom
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopAgreementTemplate
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

    @Test
    fun qrCodeGenerationRoundTripAndUnitCheckInOutLifecycle() {
        val (repo, dir) = tempRepo()
        try {
            val tent = repo.state.value.items.first { it.id == "eq-001" }
            val startStock = tent.stock

            // Pre-seeded units exist
            assertTrue(repo.state.value.equipmentUnits.isNotEmpty())

            // Generate custom serialized QR unit
            val created = repo.addEquipmentUnit(
                itemId = tent.id,
                customUnitCode = "TENT-2026-99",
                conditionNote = "Inspected canvas & stakes",
            ).getOrThrow()
            assertEquals("checked_in", created.status)
            assertEquals("LOYPOS:UNIT:${tent.id}:TENT-2026-99", created.qrPayload)

            // Parse QR payload
            val parsed = DesktopQrCodeUtils.parsePayload(created.qrPayload)
            assertTrue(parsed is ParsedDesktopQrCode.UnitTag)
            assertEquals("TENT-2026-99", (parsed as ParsedDesktopQrCode.UnitTag).unitCode)

            // Save PNG sticker and decode QR back from file using ZXing
            val pngFile = File(dir, "unit-sticker.png")
            DesktopQrCodeUtils.saveStickerPng(
                targetFile = pngFile,
                storeName = repo.state.value.settings.storeName,
                itemName = tent.name,
                codeLabel = created.unitCode,
                subtitle = "SKU ${tent.sku}",
                qrPayload = created.qrPayload,
            )
            assertTrue(pngFile.exists())
            val decodedText = DesktopQrCodeUtils.decodeQrFromFile(pngFile)
            assertEquals(created.qrPayload, decodedText)

            // Resolve QR target from decoded payload
            val target = repo.resolveQrTarget(decodedText!!)
            assertNotNull(target)
            assertEquals(created.id, target.unit?.id)

            // Standalone Check-Out deducts 1 unit from available catalog stock and logs scan
            val checkedOut = repo.processEquipmentScan(
                unitId = created.id,
                itemId = tent.id,
                action = "check_out",
                order = null,
                note = "Dispatched to loading bay 2",
            ).getOrThrow()
            assertEquals("checked_out", checkedOut.status)
            assertEquals(startStock - 1.0, repo.state.value.items.first { it.id == tent.id }.stock, 0.001)
            assertEquals("check_out", repo.state.value.scanLogs.first().action)

            // Check-In restores 1 unit back to catalog stock
            val checkedIn = repo.processEquipmentScan(
                unitId = created.id,
                itemId = tent.id,
                action = "check_in",
                order = null,
                note = "Returned clean and complete",
            ).getOrThrow()
            assertEquals("checked_in", checkedIn.status)
            assertEquals(startStock, repo.state.value.items.first { it.id == tent.id }.stock, 0.001)

            // Order-linked unit check-out does NOT double-deduct stock, and markRentalReturned returns the QR unit
            val order = repo.checkoutRental(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 1.0, 2)),
                rentalDays = 2,
                paymentType = "card",
                paidCents = 0L,
                discountPercent = 0.0,
                fixedDiscountCents = 0L,
                depositCents = 5_000L,
                client = repo.state.value.clients.first(),
                eventName = "Summer Pavilion",
                eventVenue = "Lakeside Lawn",
                note = "",
            )
            assertEquals(startStock - 1.0, repo.state.value.items.first { it.id == tent.id }.stock, 0.001)

            repo.processEquipmentScan(
                unitId = created.id,
                itemId = tent.id,
                action = "check_out",
                order = order,
                note = "Assigned to Summer Pavilion",
            ).getOrThrow()
            // Stock remains startStock - 1.0 (no double deduction)
            assertEquals(startStock - 1.0, repo.state.value.items.first { it.id == tent.id }.stock, 0.001)

            assertTrue(repo.markRentalReturned(order.id))
            val unitAfterOrderReturn = repo.state.value.equipmentUnits.first { it.id == created.id }
            assertEquals("checked_in", unitAfterOrderReturn.status)
            assertEquals(null, unitAfterOrderReturn.currentOrderId)
            assertEquals(startStock, repo.state.value.items.first { it.id == tent.id }.stock, 0.001)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun rentalAgreementTemplatesCrudAndAutoPopulation() {
        val (repo, dir) = tempRepo()
        try {
            // Default templates seeded
            assertTrue(repo.state.value.agreementTemplates.size >= 3)
            val defaultTpl = repo.state.value.agreementTemplates.first { it.isDefault }

            // Save custom template & switch default
            val customTpl = DesktopAgreementTemplate(
                id = newId(),
                title = "VIP Stage & Generator Agreement",
                category = "VIP Festival",
                body = "Agreement #{{order_number}} for {{client_name}} at {{event_venue}} ({{event_name}})\nItems:\n{{equipment_list}}\nTotal: {{total_cost}} | Deposit: {{deposit}}",
                isDefault = true,
            )
            repo.saveAgreementTemplate(customTpl)
            val savedTpl = repo.state.value.agreementTemplates.first { it.id == customTpl.id }
            assertTrue(savedTpl.isDefault)
            assertFalse(repo.state.value.agreementTemplates.first { it.id == defaultTpl.id }.isDefault)

            val tent = repo.state.value.items.first { it.id == "eq-001" }
            val client = repo.state.value.clients.first()
            val order = repo.checkoutRental(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 2.0, 3)),
                rentalDays = 3,
                paymentType = "cash",
                paidCents = 0L,
                discountPercent = 0.0,
                fixedDiscountCents = 0L,
                depositCents = 15_000L,
                client = client,
                eventName = "City Music Showcase",
                eventVenue = "Civic Plaza",
                note = "Stage right access",
            )

            val populated = DesktopRentalAgreementEngine.populateTemplate(
                templateBody = savedTpl.body,
                order = order,
                client = client,
                settings = repo.state.value.settings,
            )
            assertFalse(populated.contains("{{"))
            assertTrue(populated.contains(client.name))
            assertTrue(populated.contains("Civic Plaza"))
            assertTrue(populated.contains("City Music Showcase"))
            assertTrue(populated.contains(tent.name))
            assertTrue(populated.contains("$150.00"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun perItemSafetyThresholdMonthlyIncomeAndCategoryUtilization() {
        val (repo, dir) = tempRepo()
        try {
            val tent = repo.state.value.items.first { it.id == "eq-001" }
            val withCustomSafety = tent.copy(stock = 6.0, safetyThreshold = 8.0)
            assertFalse(tent.copy(stock = 6.0).isBelowSafetyThreshold(3.0))
            assertTrue(withCustomSafety.isBelowSafetyThreshold(3.0))
            assertEquals(8.0, withCustomSafety.effectiveSafetyThreshold(3.0), 0.001)

            repo.checkoutRental(
                lines = listOf(DesktopCartLine(tent.id, tent.name, tent.priceCents, 2.0, 2)),
                rentalDays = 2,
                paymentType = "card",
                paidCents = 0L,
                discountPercent = 0.0,
                fixedDiscountCents = 0L,
                depositCents = 0L,
                client = null,
                eventName = "Expo Showcase",
                eventVenue = "Hall B",
                note = "",
            )

            val monthly = repo.computeMonthlyIncome(6)
            assertEquals(6, monthly.size)
            assertTrue(monthly.last().totalCents > 0L)
            assertEquals(1, monthly.last().ordersCount)

            val util = repo.computeCategoryUtilization()
            assertTrue(util.isNotEmpty())
            val tentCategory = util.first { it.id == tent.categoryId }
            assertTrue(tentCategory.utilizationPercent > 0.0)
            assertTrue(tentCategory.activeOutUnits >= 2.0)
        } finally {
            dir.deleteRecursively()
        }
    }
}
