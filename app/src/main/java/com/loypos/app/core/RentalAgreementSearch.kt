package com.loypos.app.core

import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.EquipmentScanLogEntity
import com.loypos.app.data.local.EquipmentUnitEntity
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.data.local.SaleWithItems
import com.loypos.app.data.local.TicketWithLines

enum class RentalTimeframeFilter(val label: String) {
    ALL("All"),
    CURRENT("Current"),
    FUTURE("Future"),
    PAST("Past"),
}

enum class RentalAgreementTimeframe(val label: String) {
    CURRENT("Current"),
    FUTURE("Future"),
    PAST("Past"),
}

data class RentalAgreementSearchResult(
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
    val timeframe: RentalAgreementTimeframe,
    val statusBadge: String,
    val isOverdue: Boolean,
    val isRefunded: Boolean,
    val equipmentIds: List<String>,
    val equipmentSummaries: List<String>,
    val matchedByHint: String,
    val sortTimestampMs: Long,
)

data class RentalAgreementSearchOutput(
    val results: List<RentalAgreementSearchResult>,
    val allCount: Int,
    val currentCount: Int,
    val futureCount: Int,
    val pastCount: Int,
)

fun searchRentalAgreements(
    query: String,
    timeframeFilter: RentalTimeframeFilter,
    receipts: List<SaleWithItems>,
    tickets: List<TicketWithLines>,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    equipmentUnits: List<EquipmentUnitEntity>,
    scanLogs: List<EquipmentScanLogEntity>,
    branchId: String? = null,
    nowMs: Long = nowMs(),
): RentalAgreementSearchOutput {
    val productById = products.associateBy { it.id }
    val customerById = customers.associateBy { it.id }
    val unitsByReceipt = equipmentUnits
        .filter { !it.currentReceiptId.isNullOrBlank() }
        .groupBy { it.currentReceiptId!! }
    val logsByReceipt = scanLogs
        .filter { !it.receiptId.isNullOrBlank() }
        .groupBy { it.receiptId!! }

    val tokens = query.trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

    val candidates = mutableListOf<Pair<RentalAgreementSearchResult, Int>>()

    for (entry in receipts) {
        val r = entry.receipt
        if (branchId != null && r.branchId != null && r.branchId != branchId) continue

        val resolvedCustomer = r.customerName?.trim()?.takeIf { it.isNotEmpty() }
            ?: r.customerId?.let { customerById[it]?.name }?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Walk-in Client"

        val rStart = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
        val rEnd = if (r.rentalEndMs > rStart) r.rentalEndMs else rStart + r.rentalDays.coerceAtLeast(1) * 86_400_000L

        val timeframe = when {
            !r.isCompleted || r.isReturned -> RentalAgreementTimeframe.PAST
            rStart > nowMs -> RentalAgreementTimeframe.FUTURE
            else -> RentalAgreementTimeframe.CURRENT
        }
        val overdue = timeframe == RentalAgreementTimeframe.CURRENT && r.isOverdue(nowMs)
        val statusBadge = when {
            !r.isCompleted -> "Refunded"
            r.isReturned -> "Returned"
            overdue -> "Overdue"
            timeframe == RentalAgreementTimeframe.FUTURE -> "Upcoming"
            else -> "Active"
        }

        val eqIds = linkedSetOf<String>()
        val eqNames = mutableListOf<String>()
        val eqSummaries = mutableListOf<String>()

        for (line in entry.lines) {
            val item = productById[line.itemId]
            if (line.itemId.isNotBlank()) eqIds.add(line.itemId)
            if (item != null) {
                if (item.sku.isNotBlank()) eqIds.add(item.sku)
                if (item.barcode.isNotBlank()) eqIds.add(item.barcode)
            }
            eqNames.add(line.name)
            eqSummaries.add("${formatQty(line.qty)}x ${line.name}")
        }
        for (unit in unitsByReceipt[r.id].orEmpty()) {
            if (unit.unitCode.isNotBlank()) eqIds.add(unit.unitCode)
            if (unit.itemId.isNotBlank()) eqIds.add(unit.itemId)
            if (unit.id.isNotBlank()) eqIds.add(unit.id)
        }
        for (log in logsByReceipt[r.id].orEmpty()) {
            if (log.unitCode.isNotBlank()) eqIds.add(log.unitCode)
            if (log.itemId.isNotBlank()) eqIds.add(log.itemId)
        }

        val agreementNum = "#${r.number.toString().padStart(4, '0')}"
        val matchScore = computeSearchScore(
            tokens = tokens,
            customerName = resolvedCustomer,
            equipmentIds = eqIds,
            equipmentNames = eqNames,
            agreementNumber = agreementNum,
            agreementId = r.id,
            eventName = r.eventName,
            eventVenue = r.eventVenue,
        )
        if (matchScore < 0) continue

        val matchedEqId = if (tokens.isNotEmpty()) {
            eqIds.firstOrNull { id -> tokens.any { tok -> id.lowercase().contains(tok) } }
        } else null
        val matchedCustomer = tokens.isNotEmpty() && tokens.any { resolvedCustomer.lowercase().contains(it) }
        val matchedByHint = when {
            matchedEqId != null -> "Equipment ID: $matchedEqId"
            matchedCustomer -> "Customer: $resolvedCustomer"
            tokens.isNotEmpty() && r.eventName.isNotBlank() && tokens.any { r.eventName.lowercase().contains(it) } ->
                "Event: ${r.eventName}"
            else -> ""
        }

        candidates += RentalAgreementSearchResult(
            id = r.id,
            isHeldQuote = false,
            agreementNumberLabel = agreementNum,
            customerName = resolvedCustomer,
            eventName = r.eventName,
            eventVenue = r.eventVenue,
            rentalStartMs = rStart,
            rentalEndMs = rEnd,
            rentalDays = r.rentalDays.coerceAtLeast(1),
            totalCents = r.totalCents,
            timeframe = timeframe,
            statusBadge = statusBadge,
            isOverdue = overdue,
            isRefunded = !r.isCompleted,
            equipmentIds = eqIds.toList(),
            equipmentSummaries = eqSummaries,
            matchedByHint = matchedByHint,
            sortTimestampMs = rStart,
        ) to matchScore
    }

    for (ticketWithLines in tickets) {
        val t = ticketWithLines.ticket
        if (branchId != null && t.branchId != null && t.branchId != branchId) continue

        val resolvedCustomer = t.customerName?.trim()?.takeIf { it.isNotEmpty() }
            ?: t.customerId?.let { customerById[it]?.name }?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Walk-in Client"

        val tStart = if (t.rentalStartMs > 0L) t.rentalStartMs else t.createdAt
        val tEnd = if (t.rentalEndMs > tStart) t.rentalEndMs else tStart + t.rentalDays.coerceAtLeast(1) * 86_400_000L

        val timeframe = if (tEnd in 1 until nowMs) {
            RentalAgreementTimeframe.PAST
        } else {
            RentalAgreementTimeframe.FUTURE
        }

        val eqIds = linkedSetOf<String>()
        val eqNames = mutableListOf<String>()
        val eqSummaries = mutableListOf<String>()
        var rawSubtotal = 0L

        for (line in ticketWithLines.lines) {
            val item = productById[line.itemId]
            if (line.itemId.isNotBlank()) eqIds.add(line.itemId)
            if (item != null) {
                if (item.sku.isNotBlank()) eqIds.add(item.sku)
                if (item.barcode.isNotBlank()) eqIds.add(item.barcode)
            }
            eqNames.add(line.name)
            eqSummaries.add("${formatQty(line.qty)}x ${line.name}")
            rawSubtotal += (line.priceCents * line.qty * t.rentalDays.coerceAtLeast(1)).toLong()
        }

        val quoteLabel = if (t.name.isNotBlank()) "QUOTE · ${t.name}" else "QUOTE"
        val matchScore = computeSearchScore(
            tokens = tokens,
            customerName = resolvedCustomer,
            equipmentIds = eqIds,
            equipmentNames = eqNames,
            agreementNumber = quoteLabel,
            agreementId = t.id,
            eventName = t.eventName,
            eventVenue = t.eventVenue,
        )
        if (matchScore < 0) continue

        val matchedEqId = if (tokens.isNotEmpty()) {
            eqIds.firstOrNull { id -> tokens.any { tok -> id.lowercase().contains(tok) } }
        } else null
        val matchedCustomer = tokens.isNotEmpty() && tokens.any { resolvedCustomer.lowercase().contains(it) }
        val matchedByHint = when {
            matchedEqId != null -> "Equipment ID: $matchedEqId"
            matchedCustomer -> "Customer: $resolvedCustomer"
            else -> ""
        }

        candidates += RentalAgreementSearchResult(
            id = t.id,
            isHeldQuote = true,
            agreementNumberLabel = quoteLabel,
            customerName = resolvedCustomer,
            eventName = t.eventName,
            eventVenue = t.eventVenue,
            rentalStartMs = tStart,
            rentalEndMs = tEnd,
            rentalDays = t.rentalDays.coerceAtLeast(1),
            totalCents = rawSubtotal,
            timeframe = timeframe,
            statusBadge = "Reserved Quote",
            isOverdue = false,
            isRefunded = false,
            equipmentIds = eqIds.toList(),
            equipmentSummaries = eqSummaries,
            matchedByHint = matchedByHint,
            sortTimestampMs = tStart,
        ) to matchScore
    }

    val currentCount = candidates.count { it.first.timeframe == RentalAgreementTimeframe.CURRENT }
    val futureCount = candidates.count { it.first.timeframe == RentalAgreementTimeframe.FUTURE }
    val pastCount = candidates.count { it.first.timeframe == RentalAgreementTimeframe.PAST }

    val filtered = candidates
        .filter { (item, _) ->
            when (timeframeFilter) {
                RentalTimeframeFilter.ALL -> true
                RentalTimeframeFilter.CURRENT -> item.timeframe == RentalAgreementTimeframe.CURRENT
                RentalTimeframeFilter.FUTURE -> item.timeframe == RentalAgreementTimeframe.FUTURE
                RentalTimeframeFilter.PAST -> item.timeframe == RentalAgreementTimeframe.PAST
            }
        }
        .sortedWith(
            compareByDescending<Pair<RentalAgreementSearchResult, Int>> { it.second }
                .thenBy { it.first.timeframe.ordinal }
                .thenByDescending { it.first.sortTimestampMs }
        )
        .map { it.first }

    return RentalAgreementSearchOutput(
        results = filtered,
        allCount = candidates.size,
        currentCount = currentCount,
        futureCount = futureCount,
        pastCount = pastCount,
    )
}

private fun computeSearchScore(
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
