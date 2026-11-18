package com.loypos.desktop.core

import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopEquipmentScanLog
import com.loypos.desktop.model.DesktopEquipmentUnit
import com.loypos.desktop.model.DesktopHeldQuote
import com.loypos.desktop.model.DesktopRentalOrder

enum class DesktopRentalTimeframeFilter(val label: String) {
    ALL("All"),
    CURRENT("Current"),
    FUTURE("Future"),
    PAST("Past"),
}

enum class DesktopRentalAgreementTimeframe(val label: String) {
    CURRENT("Current"),
    FUTURE("Future"),
    PAST("Past"),
}

data class DesktopRentalAgreementSearchResult(
    val id: String,
    val isHeldQuote: Boolean,
    val agreementNumberLabel: String,
    val customerName: String,
    val eventName: String,
    val eventVenue: String,
    val rentalStartMs: Long,
    val rentalEndMs: Long,
    val rentalDays: Int,
    val totalCents: Long,
    val timeframe: DesktopRentalAgreementTimeframe,
    val statusBadge: String,
    val isOverdue: Boolean,
    val isRefunded: Boolean,
    val equipmentIds: List<String>,
    val equipmentSummaries: List<String>,
    val sortTimestampMs: Long,
)

data class DesktopRentalAgreementSearchOutput(
    val results: List<DesktopRentalAgreementSearchResult>,
    val allCount: Int,
    val currentCount: Int,
    val futureCount: Int,
    val pastCount: Int,
)

fun searchDesktopRentalAgreements(
    query: String,
    timeframeFilter: DesktopRentalTimeframeFilter,
    orders: List<DesktopRentalOrder>,
    heldQuotes: List<DesktopHeldQuote>,
    items: List<DesktopEquipment>,
    clients: List<DesktopClient>,
    equipmentUnits: List<DesktopEquipmentUnit>,
    scanLogs: List<DesktopEquipmentScanLog>,
    nowMs: Long = nowMs(),
): DesktopRentalAgreementSearchOutput {
    val itemById = items.associateBy { it.id }
    val clientById = clients.associateBy { it.id }
    val unitsByOrder = equipmentUnits
        .filter { !it.currentOrderId.isNullOrBlank() }
        .groupBy { it.currentOrderId!! }
    val logsByOrder = scanLogs
        .filter { !it.orderId.isNullOrBlank() }
        .groupBy { it.orderId!! }

    val tokens = query.trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

    val candidates = mutableListOf<Pair<DesktopRentalAgreementSearchResult, Int>>()

    for (o in orders) {
        val resolvedCustomer = o.customerName?.trim()?.takeIf { it.isNotEmpty() }
            ?: o.customerId?.let { clientById[it]?.name }?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Walk-in Client"

        val oStart = if (o.rentalStartMs > 0L) o.rentalStartMs else o.createdAt
        val oEnd = if (o.rentalEndMs > oStart) o.rentalEndMs else oStart + o.rentalDays.coerceAtLeast(1) * 86_400_000L

        val timeframe = when {
            !o.isCompleted || o.isReturned -> DesktopRentalAgreementTimeframe.PAST
            oStart > nowMs -> DesktopRentalAgreementTimeframe.FUTURE
            else -> DesktopRentalAgreementTimeframe.CURRENT
        }
        val overdue = timeframe == DesktopRentalAgreementTimeframe.CURRENT && o.isOverdue(nowMs)
        val statusBadge = when {
            !o.isCompleted -> "Refunded"
            o.isReturned -> "Returned"
            overdue -> "Overdue"
            timeframe == DesktopRentalAgreementTimeframe.FUTURE -> "Upcoming"
            else -> "Active"
        }

        val eqIds = linkedSetOf<String>()
        val eqNames = mutableListOf<String>()
        val eqSummaries = mutableListOf<String>()

        for (line in o.lines) {
            val itemId = line.itemId.orEmpty()
            val item = itemById[itemId]
            if (itemId.isNotBlank()) eqIds.add(itemId)
            if (item != null) {
                if (item.sku.isNotBlank()) eqIds.add(item.sku)
                if (item.barcode.isNotBlank()) eqIds.add(item.barcode)
            }
            eqNames.add(line.name)
            eqSummaries.add("${formatQty(line.qty)}x ${line.name}")
        }
        for (unit in unitsByOrder[o.id].orEmpty()) {
            if (unit.unitCode.isNotBlank()) eqIds.add(unit.unitCode)
            if (unit.itemId.isNotBlank()) eqIds.add(unit.itemId)
            if (unit.id.isNotBlank()) eqIds.add(unit.id)
        }
        for (log in logsByOrder[o.id].orEmpty()) {
            if (log.unitCode.isNotBlank()) eqIds.add(log.unitCode)
            if (log.itemId.isNotBlank()) eqIds.add(log.itemId)
        }

        val agreementNum = "#${o.number.toString().padStart(4, '0')}"
        val matchScore = computeDesktopSearchScore(
            tokens = tokens,
            customerName = resolvedCustomer,
            equipmentIds = eqIds,
            equipmentNames = eqNames,
            agreementNumber = agreementNum,
            agreementId = o.id,
            eventName = o.eventName,
            eventVenue = o.eventVenue,
        )
        if (matchScore < 0) continue

        candidates += DesktopRentalAgreementSearchResult(
            id = o.id,
            isHeldQuote = false,
            agreementNumberLabel = agreementNum,
            customerName = resolvedCustomer,
            eventName = o.eventName,
            eventVenue = o.eventVenue,
            rentalStartMs = oStart,
            rentalEndMs = oEnd,
            rentalDays = o.rentalDays.coerceAtLeast(1),
            totalCents = o.totalCents,
            timeframe = timeframe,
            statusBadge = statusBadge,
            isOverdue = overdue,
            isRefunded = !o.isCompleted,
            equipmentIds = eqIds.toList(),
            equipmentSummaries = eqSummaries,
            sortTimestampMs = oStart,
        ) to matchScore
    }

    for (q in heldQuotes) {
        val resolvedCustomer = q.customerName?.trim()?.takeIf { it.isNotEmpty() }
            ?: q.customerId?.let { clientById[it]?.name }?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Walk-in Client"

        val eqIds = linkedSetOf<String>()
        val eqNames = mutableListOf<String>()
        val eqSummaries = mutableListOf<String>()

        for (line in q.lines) {
            val item = itemById[line.itemId]
            if (line.itemId.isNotBlank()) eqIds.add(line.itemId)
            if (item != null) {
                if (item.sku.isNotBlank()) eqIds.add(item.sku)
                if (item.barcode.isNotBlank()) eqIds.add(item.barcode)
            }
            eqNames.add(line.name)
            eqSummaries.add("${formatQty(line.qty)}x ${line.name}")
        }

        val quoteLabel = if (q.name.isNotBlank()) "QUOTE · ${q.name}" else "QUOTE"
        val matchScore = computeDesktopSearchScore(
            tokens = tokens,
            customerName = resolvedCustomer,
            equipmentIds = eqIds,
            equipmentNames = eqNames,
            agreementNumber = quoteLabel,
            agreementId = q.id,
            eventName = q.eventName,
            eventVenue = q.eventVenue,
        )
        if (matchScore < 0) continue

        val qStart = q.createdAt
        val qEnd = q.createdAt + q.rentalDays.coerceAtLeast(1) * 86_400_000L

        candidates += DesktopRentalAgreementSearchResult(
            id = q.id,
            isHeldQuote = true,
            agreementNumberLabel = quoteLabel,
            customerName = resolvedCustomer,
            eventName = q.eventName,
            eventVenue = q.eventVenue,
            rentalStartMs = qStart,
            rentalEndMs = qEnd,
            rentalDays = q.rentalDays.coerceAtLeast(1),
            totalCents = q.lines.sumOf { it.lineTotalCents },
            timeframe = DesktopRentalAgreementTimeframe.FUTURE,
            statusBadge = "Reserved Quote",
            isOverdue = false,
            isRefunded = false,
            equipmentIds = eqIds.toList(),
            equipmentSummaries = eqSummaries,
            sortTimestampMs = qStart,
        ) to matchScore
    }

    val currentCount = candidates.count { it.first.timeframe == DesktopRentalAgreementTimeframe.CURRENT }
    val futureCount = candidates.count { it.first.timeframe == DesktopRentalAgreementTimeframe.FUTURE }
    val pastCount = candidates.count { it.first.timeframe == DesktopRentalAgreementTimeframe.PAST }

    val filtered = candidates
        .filter { (item, _) ->
            when (timeframeFilter) {
                DesktopRentalTimeframeFilter.ALL -> true
                DesktopRentalTimeframeFilter.CURRENT -> item.timeframe == DesktopRentalAgreementTimeframe.CURRENT
                DesktopRentalTimeframeFilter.FUTURE -> item.timeframe == DesktopRentalAgreementTimeframe.FUTURE
                DesktopRentalTimeframeFilter.PAST -> item.timeframe == DesktopRentalAgreementTimeframe.PAST
            }
        }
        .sortedWith(
            compareByDescending<Pair<DesktopRentalAgreementSearchResult, Int>> { it.second }
                .thenBy { it.first.timeframe.ordinal }
                .thenByDescending { it.first.sortTimestampMs }
        )
        .map { it.first }

    return DesktopRentalAgreementSearchOutput(
        results = filtered,
        allCount = candidates.size,
        currentCount = currentCount,
        futureCount = futureCount,
        pastCount = pastCount,
    )
}

private fun computeDesktopSearchScore(
    tokens: List<String>,
    customerName: String,
    equipmentIds: Set<String>,
    equipmentNames: List<String>,
    agreementNumber: String,
    agreementId: String,
    eventName: String,
    eventVenue: String,
): Int {
    if (tokens.isEmpty()) return 0

    val custLower = customerName.lowercase()
    val idsLower = equipmentIds.map { it.lowercase() }
    val namesLower = equipmentNames.map { it.lowercase() }
    val numLower = agreementNumber.lowercase()
    val idLower = agreementId.lowercase()
    val eventLower = eventName.lowercase()
    val venueLower = eventVenue.lowercase()

    var score = 0
    for (token in tokens) {
        val matchesCust = custLower.contains(token)
        val exactId = idsLower.any { it == token }
        val partialId = idsLower.any { it.contains(token) }
        val matchesName = namesLower.any { it.contains(token) }
        val matchesMeta = numLower.contains(token) || idLower.contains(token) ||
            eventLower.contains(token) || venueLower.contains(token)

        if (!matchesCust && !partialId && !matchesName && !matchesMeta) {
            return -1
        }
        score += when {
            exactId -> 100
            custLower.startsWith(token) -> 80
            matchesCust -> 60
            partialId -> 55
            matchesMeta -> 30
            else -> 15
        }
    }
    return score
}
