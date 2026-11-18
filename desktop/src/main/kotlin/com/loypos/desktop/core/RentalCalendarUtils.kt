package com.loypos.desktop.core

import com.loypos.desktop.model.DesktopCartLine
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopRentalOrder
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DesktopDayBounds(
    val startMs: Long,
    val endMs: Long,
)

data class DesktopCalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val activeCount: Int,
    val overdueCount: Int,
    val returnedCount: Int,
) {
    val totalCount: Int get() = activeCount + returnedCount
}

enum class DesktopAvailabilityStatus {
    AVAILABLE,
    LOW,
    FULLY_BOOKED,
    UNLIMITED,
}

data class DesktopEquipmentAvailability(
    val item: DesktopEquipment,
    val totalCapacity: Double,
    val reservedUnits: Double,
    val availableUnits: Double,
    val bookingsCount: Int,
    val status: DesktopAvailabilityStatus,
) {
    val utilizationRatio: Float
        get() = if (totalCapacity <= 0.0) 0f else (reservedUnits / totalCapacity).toFloat().coerceIn(0f, 1f)
}

data class DesktopCartConflict(
    val itemId: String,
    val itemName: String,
    val requestedQty: Double,
    val availableQty: Double,
)

private val desktopMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val desktopFullDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(desktopMonthFormatter)

fun formatFullLocalDate(date: LocalDate): String = date.format(desktopFullDateFormatter)

fun localDateFromMs(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
    val safe = if (millis > 0L) millis else nowMs()
    return Instant.ofEpochMilli(safe).atZone(zoneId).toLocalDate()
}

fun dayStartMs(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zoneId).toInstant().toEpochMilli()

fun dayBoundsFor(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): DesktopDayBounds {
    val start = dayStartMs(date, zoneId)
    val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    return DesktopDayBounds(start, end)
}

fun intervalsOverlap(startA: Long, endA: Long, startB: Long, endB: Long): Boolean {
    val normEndA = maxOf(startA, endA)
    val normEndB = maxOf(startB, endB)
    return startA <= normEndB && normEndA >= startB
}

fun buildMonthCalendarGrid(
    yearMonth: YearMonth,
    orders: List<DesktopRentalOrder>,
    nowMs: Long = nowMs(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<DesktopCalendarCell> {
    val firstOfMonth = yearMonth.atDay(1)
    val leadingDays = firstOfMonth.dayOfWeek.value - 1
    val gridStart = firstOfMonth.minusDays(leadingDays.toLong())
    val completed = orders.filter { it.isCompleted }

    return (0 until 42).map { offset ->
        val date = gridStart.plusDays(offset.toLong())
        val bounds = dayBoundsFor(date, zoneId)
        var active = 0
        var overdue = 0
        var returned = 0

        for (o in completed) {
            val oStart = if (o.rentalStartMs > 0L) o.rentalStartMs else o.createdAt
            val oEnd = if (o.rentalEndMs > oStart) o.rentalEndMs else oStart + o.rentalDays.coerceAtLeast(1) * 86_400_000L
            if (intervalsOverlap(oStart, oEnd, bounds.startMs, bounds.endMs)) {
                if (o.isReturned) {
                    returned++
                } else {
                    active++
                    if (o.isOverdue(nowMs)) overdue++
                }
            }
        }

        DesktopCalendarCell(
            date = date,
            isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
            activeCount = active,
            overdueCount = overdue,
            returnedCount = returned,
        )
    }
}

fun schedulesForDate(
    date: LocalDate,
    orders: List<DesktopRentalOrder>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<DesktopRentalOrder> {
    val bounds = dayBoundsFor(date, zoneId)
    return orders
        .filter { o ->
            if (!o.isCompleted) return@filter false
            val oStart = if (o.rentalStartMs > 0L) o.rentalStartMs else o.createdAt
            val oEnd = if (o.rentalEndMs > oStart) o.rentalEndMs else oStart + o.rentalDays.coerceAtLeast(1) * 86_400_000L
            intervalsOverlap(oStart, oEnd, bounds.startMs, bounds.endMs)
        }
        .sortedWith(compareBy<DesktopRentalOrder> { it.isReturned }.thenBy { it.rentalEndMs })
}

fun upcomingRentalsFrom(
    fromDate: LocalDate,
    orders: List<DesktopRentalOrder>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<DesktopRentalOrder> {
    val threshold = dayStartMs(fromDate, zoneId)
    return orders
        .filter { it.isCompleted && !it.isReturned && it.rentalEndMs >= threshold }
        .sortedBy { if (it.rentalStartMs > 0L) it.rentalStartMs else it.createdAt }
}

fun computeEquipmentAvailabilityForRange(
    items: List<DesktopEquipment>,
    orders: List<DesktopRentalOrder>,
    rangeStartMs: Long,
    rangeEndMs: Long,
    lowThreshold: Double = 3.0,
): List<DesktopEquipmentAvailability> {
    val activeOut = orders.filter { it.isRentalOut }
    val outByItem = mutableMapOf<String, Double>()
    val reservedByItem = mutableMapOf<String, Double>()
    val countByItem = mutableMapOf<String, Int>()

    for (o in activeOut) {
        val oStart = if (o.rentalStartMs > 0L) o.rentalStartMs else o.createdAt
        val oEnd = if (o.rentalEndMs > oStart) o.rentalEndMs else oStart + o.rentalDays.coerceAtLeast(1) * 86_400_000L
        val overlaps = intervalsOverlap(oStart, oEnd, rangeStartMs, rangeEndMs)
        val seen = mutableSetOf<String>()

        for (line in o.lines) {
            val itemId = line.itemId ?: continue
            outByItem[itemId] = (outByItem[itemId] ?: 0.0) + line.qty
            if (overlaps) {
                reservedByItem[itemId] = (reservedByItem[itemId] ?: 0.0) + line.qty
                if (seen.add(itemId)) {
                    countByItem[itemId] = (countByItem[itemId] ?: 0) + 1
                }
            }
        }
    }

    return items.map { item ->
        if (!item.trackStock) {
            DesktopEquipmentAvailability(
                item = item,
                totalCapacity = 0.0,
                reservedUnits = reservedByItem[item.id] ?: 0.0,
                availableUnits = Double.POSITIVE_INFINITY,
                bookingsCount = countByItem[item.id] ?: 0,
                status = DesktopAvailabilityStatus.UNLIMITED,
            )
        } else {
            val totalCap = (item.stock + (outByItem[item.id] ?: 0.0)).coerceAtLeast(0.0)
            val reserved = reservedByItem[item.id] ?: 0.0
            val avail = (totalCap - reserved).coerceAtLeast(0.0)
            val status = when {
                avail <= 0.0 -> DesktopAvailabilityStatus.FULLY_BOOKED
                avail < lowThreshold || (totalCap > 0.0 && avail / totalCap <= 0.2) -> DesktopAvailabilityStatus.LOW
                else -> DesktopAvailabilityStatus.AVAILABLE
            }
            DesktopEquipmentAvailability(
                item = item,
                totalCapacity = totalCap,
                reservedUnits = reserved,
                availableUnits = avail,
                bookingsCount = countByItem[item.id] ?: 0,
                status = status,
            )
        }
    }
}

fun checkCartAvailabilityConflicts(
    lines: List<DesktopCartLine>,
    catalog: List<DesktopEquipment>,
    orders: List<DesktopRentalOrder>,
    rangeStartMs: Long,
    rangeEndMs: Long,
): List<DesktopCartConflict> {
    if (lines.isEmpty()) return emptyList()
    val availById = computeEquipmentAvailabilityForRange(catalog, orders, rangeStartMs, rangeEndMs)
        .associateBy { it.item.id }
    return lines.mapNotNull { line ->
        val avail = availById[line.itemId] ?: return@mapNotNull null
        if (!avail.item.trackStock) return@mapNotNull null
        if (line.qty > avail.availableUnits + 0.0001) {
            DesktopCartConflict(
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
