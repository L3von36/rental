package com.loypos.app.core

import com.loypos.app.data.local.CartLine
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.data.local.SaleWithItems
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RentalDayBounds(
    val startMs: Long,
    val endMs: Long,
)

data class RentalCalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val activeCount: Int,
    val overdueCount: Int,
    val returnedCount: Int,
) {
    val totalCount: Int get() = activeCount + returnedCount
}

enum class AvailabilityStatus {
    AVAILABLE,
    LOW,
    FULLY_BOOKED,
    UNLIMITED,
}

data class EquipmentDayAvailability(
    val item: ProductEntity,
    val totalCapacity: Double,
    val reservedUnits: Double,
    val availableUnits: Double,
    val activeBookingsCount: Int,
    val status: AvailabilityStatus,
) {
    val utilizationRatio: Float
        get() = if (totalCapacity <= 0.0) 0f else (reservedUnits / totalCapacity).toFloat().coerceIn(0f, 1f)
}

data class CartAvailabilityConflict(
    val itemId: String,
    val itemName: String,
    val requestedQty: Double,
    val availableQty: Double,
)

private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)

fun formatFullLocalDate(date: LocalDate): String = date.format(fullDateFormatter)

fun localDateFromMs(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    val safeMs = if (millis > 0L) millis else nowMs()
    return Instant.ofEpochMilli(safeMs).atZone(zoneId).toLocalDate()
}

fun dayStartMs(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zoneId).toInstant().toEpochMilli()

fun dayBoundsFor(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): RentalDayBounds {
    val start = dayStartMs(date, zoneId)
    val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    return RentalDayBounds(start, end)
}

fun intervalsOverlap(startA: Long, endA: Long, startB: Long, endB: Long): Boolean {
    val normEndA = maxOf(startA, endA)
    val normEndB = maxOf(startB, endB)
    return startA <= normEndB && normEndA >= startB
}

fun buildMonthCalendarGrid(
    yearMonth: YearMonth,
    receipts: List<SaleWithItems>,
    nowMs: Long = nowMs(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<RentalCalendarCell> {
    val firstOfMonth = yearMonth.atDay(1)
    // Monday-first grid (Monday = 1 .. Sunday = 7)
    val leadingDays = firstOfMonth.dayOfWeek.value - 1
    val gridStart = firstOfMonth.minusDays(leadingDays.toLong())
    val totalDaysInGrid = 42 // 6 weeks x 7 columns

    val completedReceipts = receipts.filter { it.receipt.isCompleted }

    return (0 until totalDaysInGrid).map { offset ->
        val date = gridStart.plusDays(offset.toLong())
        val bounds = dayBoundsFor(date, zoneId)
        var activeCount = 0
        var overdueCount = 0
        var returnedCount = 0

        for (entry in completedReceipts) {
            val r = entry.receipt
            val rStart = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
            val rEnd = if (r.rentalEndMs > rStart) r.rentalEndMs else rStart + r.rentalDays.coerceAtLeast(1) * 86_400_000L
            if (intervalsOverlap(rStart, rEnd, bounds.startMs, bounds.endMs)) {
                if (r.isReturned) {
                    returnedCount++
                } else {
                    activeCount++
                    if (r.isOverdue(nowMs)) overdueCount++
                }
            }
        }

        RentalCalendarCell(
            date = date,
            isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
            activeCount = activeCount,
            overdueCount = overdueCount,
            returnedCount = returnedCount,
        )
    }
}

fun schedulesForDate(
    date: LocalDate,
    receipts: List<SaleWithItems>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<SaleWithItems> {
    val bounds = dayBoundsFor(date, zoneId)
    return receipts
        .filter { entry ->
            val r = entry.receipt
            if (!r.isCompleted) return@filter false
            val rStart = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
            val rEnd = if (r.rentalEndMs > rStart) r.rentalEndMs else rStart + r.rentalDays.coerceAtLeast(1) * 86_400_000L
            intervalsOverlap(rStart, rEnd, bounds.startMs, bounds.endMs)
        }
        .sortedWith(compareBy<SaleWithItems> { it.receipt.isReturned }.thenBy { it.receipt.rentalEndMs })
}

fun upcomingRentalsFrom(
    fromDate: LocalDate,
    receipts: List<SaleWithItems>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<SaleWithItems> {
    val threshold = dayStartMs(fromDate, zoneId)
    return receipts
        .filter { entry ->
            val r = entry.receipt
            r.isCompleted && !r.isReturned && r.rentalEndMs >= threshold
        }
        .sortedBy { if (it.receipt.rentalStartMs > 0L) it.receipt.rentalStartMs else it.receipt.createdAt }
}

fun computeEquipmentAvailabilityForRange(
    items: List<ProductEntity>,
    receipts: List<SaleWithItems>,
    rangeStartMs: Long,
    rangeEndMs: Long,
    lowThreshold: Double = 3.0,
): List<EquipmentDayAvailability> {
    val activeOutRentals = receipts.filter { it.receipt.isRentalOut }

    val currentlyOutByItem = mutableMapOf<String, Double>()
    val reservedInRangeByItem = mutableMapOf<String, Double>()
    val bookingCountByItem = mutableMapOf<String, Int>()

    for (entry in activeOutRentals) {
        val r = entry.receipt
        val rStart = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
        val rEnd = if (r.rentalEndMs > rStart) r.rentalEndMs else rStart + r.rentalDays.coerceAtLeast(1) * 86_400_000L
        val overlapsRange = intervalsOverlap(rStart, rEnd, rangeStartMs, rangeEndMs)
        val seenInOrder = mutableSetOf<String>()

        for (line in entry.lines) {
            currentlyOutByItem[line.itemId] = (currentlyOutByItem[line.itemId] ?: 0.0) + line.qty
            if (overlapsRange) {
                reservedInRangeByItem[line.itemId] = (reservedInRangeByItem[line.itemId] ?: 0.0) + line.qty
                if (seenInOrder.add(line.itemId)) {
                    bookingCountByItem[line.itemId] = (bookingCountByItem[line.itemId] ?: 0) + 1
                }
            }
        }
    }

    return items.map { item ->
        if (!item.trackStock) {
            val reserved = reservedInRangeByItem[item.id] ?: 0.0
            EquipmentDayAvailability(
                item = item,
                totalCapacity = 0.0,
                reservedUnits = reserved,
                availableUnits = Double.POSITIVE_INFINITY,
                activeBookingsCount = bookingCountByItem[item.id] ?: 0,
                status = AvailabilityStatus.UNLIMITED,
            )
        } else {
            val totalCap = (item.stock + (currentlyOutByItem[item.id] ?: 0.0)).coerceAtLeast(0.0)
            val reserved = reservedInRangeByItem[item.id] ?: 0.0
            val avail = (totalCap - reserved).coerceAtLeast(0.0)
            val status = when {
                avail <= 0.0 -> AvailabilityStatus.FULLY_BOOKED
                avail < lowThreshold || (totalCap > 0.0 && avail / totalCap <= 0.2) -> AvailabilityStatus.LOW
                else -> AvailabilityStatus.AVAILABLE
            }
            EquipmentDayAvailability(
                item = item,
                totalCapacity = totalCap,
                reservedUnits = reserved,
                availableUnits = avail,
                activeBookingsCount = bookingCountByItem[item.id] ?: 0,
                status = status,
            )
        }
    }
}

fun checkCartAvailabilityConflicts(
    cartItems: List<CartLine>,
    catalog: List<ProductEntity>,
    receipts: List<SaleWithItems>,
    rangeStartMs: Long,
    rangeEndMs: Long,
): List<CartAvailabilityConflict> {
    if (cartItems.isEmpty()) return emptyList()
    val availById = computeEquipmentAvailabilityForRange(catalog, receipts, rangeStartMs, rangeEndMs)
        .associateBy { it.item.id }
    return cartItems.mapNotNull { line ->
        val avail = availById[line.itemId] ?: return@mapNotNull null
        if (!avail.item.trackStock) return@mapNotNull null
        if (line.qty > avail.availableUnits + 0.0001) {
            CartAvailabilityConflict(
                itemId = line.itemId,
                itemName = line.name,
                requestedQty = line.qty,
                availableQty = avail.availableUnits,
            )
        } else {
            null
        }
    }
}
