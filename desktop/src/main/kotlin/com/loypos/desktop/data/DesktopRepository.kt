package com.loypos.desktop.data

import com.loypos.desktop.core.DemoRentalCatalog
import com.loypos.desktop.core.DesktopQrCodeUtils
import com.loypos.desktop.core.DesktopRentalAgreementEngine
import com.loypos.desktop.core.ParsedDesktopQrCode
import com.loypos.desktop.core.computeRentalTotals
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.model.DesktopAgreementTemplate
import com.loypos.desktop.model.DesktopCartLine
import com.loypos.desktop.model.DesktopCategory
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopDamageReport
import com.loypos.desktop.model.DesktopDeliveryStop
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopEquipmentScanLog
import com.loypos.desktop.model.DesktopEquipmentUnit
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopEventKitItem
import com.loypos.desktop.model.DesktopHeldQuote
import com.loypos.desktop.model.DesktopMaintenanceSchedule
import com.loypos.desktop.model.DesktopMonthlyIncomePoint
import com.loypos.desktop.model.DesktopRentalLine
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopSettings
import com.loypos.desktop.model.DesktopStoreSnapshot
import com.loypos.desktop.model.DesktopUtilizationPoint
import com.loypos.desktop.model.ExportSaleRecord
import com.loypos.desktop.model.LoyPosExportPayload
import com.loypos.desktop.model.LoyPosExportV1
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

class DesktopRepository(
    private val dataDir: File = File(System.getProperty("user.home"), ".loypos-rental"),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val storeFile: File = File(dataDir, "store.json")
    private val lock = Any()

    private val _state = MutableStateFlow(loadOrSeed())
    val state: StateFlow<DesktopStoreSnapshot> = _state.asStateFlow()

    private fun loadOrSeed(): DesktopStoreSnapshot {
        if (storeFile.exists()) {
            try {
                val raw = storeFile.readText(Charsets.UTF_8)
                val parsed = json.decodeFromString<DesktopStoreSnapshot>(raw)
                if (parsed.items.isNotEmpty() || parsed.categories.isNotEmpty()) {
                    val now = nowMs()
                    val hydrated = parsed.copy(
                        agreementTemplates = parsed.agreementTemplates.ifEmpty {
                            DesktopRentalAgreementEngine.defaultTemplates(now)
                        },
                        equipmentUnits = parsed.equipmentUnits.ifEmpty {
                            buildInitialUnits(parsed.items, now)
                        },
                        eventKits = parsed.eventKits.ifEmpty {
                            DemoRentalCatalog.buildDefaultEventKits(now)
                        },
                        maintenanceSchedules = parsed.maintenanceSchedules.ifEmpty {
                            DemoRentalCatalog.buildDefaultMaintenanceSchedules(now)
                        },
                        deliveryStops = parsed.deliveryStops.ifEmpty {
                            DemoRentalCatalog.buildDefaultDeliveryStops(now)
                        },
                    )
                    if (hydrated != parsed) saveSnapshot(hydrated)
                    return hydrated
                }
            } catch (_: Exception) {
            }
        }
        val initial = buildDefaultSnapshot()
        saveSnapshot(initial)
        return initial
    }

    private fun buildInitialUnits(items: List<DesktopEquipment>, now: Long): List<DesktopEquipmentUnit> =
        items.flatMap { item ->
            val count = item.stock.toInt().coerceIn(1, 3)
            (1..count).map { idx ->
                val unitCode = DesktopQrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
                DesktopEquipmentUnit(
                    id = "unit-${item.id}-$idx",
                    itemId = item.id,
                    itemName = item.name,
                    unitCode = unitCode,
                    qrPayload = DesktopQrCodeUtils.buildUnitPayload(item.id, unitCode),
                    status = "checked_in",
                    updatedAt = now,
                )
            }
        }

    private fun buildDefaultSnapshot(): DesktopStoreSnapshot {
        val now = nowMs()
        val items = DemoRentalCatalog.buildEquipment(now)
        return DesktopStoreSnapshot(
            categories = DemoRentalCatalog.buildCategories(),
            items = items,
            clients = DemoRentalCatalog.buildClients(now),
            orders = emptyList(),
            heldQuotes = emptyList(),
            settings = DesktopSettings(),
            equipmentUnits = buildInitialUnits(items, now),
            scanLogs = emptyList(),
            agreementTemplates = DesktopRentalAgreementEngine.defaultTemplates(now),
            eventKits = DemoRentalCatalog.buildDefaultEventKits(now),
            damageReports = emptyList(),
            maintenanceSchedules = DemoRentalCatalog.buildDefaultMaintenanceSchedules(now),
            deliveryStops = DemoRentalCatalog.buildDefaultDeliveryStops(now),
        )
    }

    private fun saveSnapshot(snapshot: DesktopStoreSnapshot) {
        dataDir.mkdirs()
        val tmp = File(dataDir, "store.json.tmp")
        tmp.writeText(json.encodeToString(snapshot), Charsets.UTF_8)
        Files.move(tmp.toPath(), storeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun mutate(transform: (DesktopStoreSnapshot) -> DesktopStoreSnapshot): DesktopStoreSnapshot {
        synchronized(lock) {
            val next = transform(_state.value)
            saveSnapshot(next)
            _state.value = next
            return next
        }
    }

    fun checkoutRental(
        lines: List<DesktopCartLine>,
        rentalDays: Int,
        paymentType: String,
        paidCents: Long,
        discountPercent: Double,
        fixedDiscountCents: Long,
        depositCents: Long,
        client: DesktopClient?,
        eventName: String,
        eventVenue: String,
        note: String,
        rentalStartMs: Long = nowMs(),
    ): DesktopRentalOrder {
        val days = rentalDays.coerceAtLeast(1)
        val snap = _state.value
        val normalizedLines = lines.map { it.copy(rentalDays = days) }
        val rawSubtotal = normalizedLines.sumOf { it.lineTotalCents }
        val calc = computeRentalTotals(
            rawSubtotalCents = rawSubtotal,
            discountPercent = discountPercent,
            fixedDiscountCents = fixedDiscountCents,
            taxRatePercent = snap.settings.taxRatePercent,
            taxIncluded = snap.settings.taxIncluded,
        )
        val actualPaid = if (paymentType == "card") calc.totalCents else paidCents.coerceAtLeast(calc.totalCents)
        val change = (actualPaid - calc.totalCents).coerceAtLeast(0L)
        val createdTime = nowMs()
        val start = if (rentalStartMs > 0L) rentalStartMs else createdTime
        val end = start + days * 86_400_000L

        val order = DesktopRentalOrder(
            id = newId(),
            number = snap.settings.nextReceiptNumber,
            createdAt = createdTime,
            subtotalCents = calc.subtotalCents,
            discountCents = calc.discountCents,
            taxCents = calc.taxCents,
            totalCents = calc.totalCents,
            paymentType = paymentType,
            paidCents = actualPaid,
            changeCents = change,
            customerId = client?.id,
            customerName = client?.name,
            dispatcherName = "Desktop Operator",
            branchName = "Main Depot",
            note = note.trim(),
            status = "completed",
            rentalDays = days,
            rentalStartMs = start,
            rentalEndMs = end,
            depositCents = depositCents.coerceAtLeast(0L),
            rentalStatus = "out",
            returnedAtMs = null,
            eventName = eventName.trim(),
            eventVenue = eventVenue.trim(),
            lines = normalizedLines.map { line ->
                DesktopRentalLine(
                    itemId = line.itemId,
                    name = line.name,
                    qty = line.qty,
                    priceCents = line.dailyRateCents,
                    lineTotalCents = line.lineTotalCents,
                    taxPercent = snap.settings.taxRatePercent,
                )
            },
        )

        mutate { current ->
            val updatedItems = current.items.map { item ->
                val rented = normalizedLines.filter { it.itemId == item.id }.sumOf { it.qty }
                if (rented > 0.0 && item.trackStock) {
                    item.copy(stock = (item.stock - rented).coerceAtLeast(0.0))
                } else {
                    item
                }
            }
            val ptsEarned = (calc.totalCents / 100L).coerceAtLeast(0L)
            val updatedClients = if (client != null) {
                current.clients.map { c ->
                    if (c.id == client.id) {
                        c.copy(points = c.points + ptsEarned, ordersCount = c.ordersCount + 1)
                    } else {
                        c
                    }
                }
            } else {
                current.clients
            }
            current.copy(
                items = updatedItems,
                clients = updatedClients,
                orders = listOf(order) + current.orders,
                settings = current.settings.copy(nextReceiptNumber = current.settings.nextReceiptNumber + 1),
            )
        }
        return order
    }

    fun markRentalReturned(orderId: String): Boolean {
        val current = _state.value
        val target = current.orders.firstOrNull { it.id == orderId } ?: return false
        if (!target.isCompleted || target.isReturned) return false

        val returnedTime = nowMs()
        mutate { snap ->
            val updatedItems = snap.items.map { item ->
                val qty = target.lines.filter { it.itemId == item.id }.sumOf { it.qty }
                if (qty > 0.0 && item.trackStock) {
                    item.copy(stock = item.stock + qty)
                } else {
                    item
                }
            }
            val updatedOrders = snap.orders.map { o ->
                if (o.id == orderId) o.copy(rentalStatus = "returned", returnedAtMs = returnedTime) else o
            }
            val returningUnits = snap.equipmentUnits.filter { it.currentOrderId == orderId }
            val updatedUnits = snap.equipmentUnits.map { u ->
                if (u.currentOrderId == orderId) {
                    u.copy(
                        status = "checked_in",
                        currentOrderId = null,
                        currentEventName = "",
                        currentCustomerName = "",
                        lastScanAt = returnedTime,
                        updatedAt = returnedTime,
                    )
                } else {
                    u
                }
            }
            val extraLogs = returningUnits.map { u ->
                DesktopEquipmentScanLog(
                    id = newId(),
                    unitId = u.id,
                    unitCode = u.unitCode,
                    itemId = u.itemId,
                    itemName = u.itemName,
                    action = "check_in",
                    orderId = orderId,
                    eventName = target.eventName,
                    customerName = target.customerName ?: "",
                    note = "Returned with order #${target.number.toString().padStart(4, '0')}",
                    createdAt = returnedTime,
                )
            }
            snap.copy(
                items = updatedItems,
                orders = updatedOrders,
                equipmentUnits = updatedUnits,
                scanLogs = (extraLogs + snap.scanLogs).take(200),
            )
        }
        return true
    }

    fun refundRental(orderId: String): Boolean {
        val current = _state.value
        val target = current.orders.firstOrNull { it.id == orderId } ?: return false
        if (!target.isCompleted) return false

        mutate { snap ->
            val shouldRestoreStock = target.rentalStatus != "returned"
            val updatedItems = if (shouldRestoreStock) {
                snap.items.map { item ->
                    val qty = target.lines.filter { it.itemId == item.id }.sumOf { it.qty }
                    if (qty > 0.0 && item.trackStock) {
                        item.copy(stock = item.stock + qty)
                    } else {
                        item
                    }
                }
            } else {
                snap.items
            }
            val updatedOrders = snap.orders.map { o ->
                if (o.id == orderId) o.copy(status = "refunded") else o
            }
            snap.copy(items = updatedItems, orders = updatedOrders)
        }
        return true
    }

    fun holdQuote(quote: DesktopHeldQuote) {
        mutate { current ->
            current.copy(heldQuotes = listOf(quote) + current.heldQuotes.filterNot { it.id == quote.id })
        }
    }

    fun deleteQuote(quoteId: String) {
        mutate { current ->
            current.copy(heldQuotes = current.heldQuotes.filterNot { it.id == quoteId })
        }
    }

    fun saveEquipment(equipment: DesktopEquipment) {
        mutate { current ->
            val exists = current.items.any { it.id == equipment.id }
            val nextItems = if (exists) {
                current.items.map { if (it.id == equipment.id) equipment else it }
            } else {
                listOf(equipment) + current.items
            }
            current.copy(items = nextItems)
        }
    }

    fun adjustEquipmentStock(itemId: String, delta: Double) {
        mutate { current ->
            val nextItems = current.items.map { item ->
                if (item.id == itemId) {
                    item.copy(stock = (item.stock + delta).coerceAtLeast(0.0))
                } else {
                    item
                }
            }
            current.copy(items = nextItems)
        }
    }

    fun deleteEquipment(itemId: String) {
        mutate { current ->
            current.copy(items = current.items.filterNot { it.id == itemId })
        }
    }

    fun saveCategory(category: DesktopCategory) {
        mutate { current ->
            val exists = current.categories.any { it.id == category.id }
            val next = if (exists) {
                current.categories.map { if (it.id == category.id) category else it }
            } else {
                current.categories + category
            }
            current.copy(categories = next.sortedBy { it.position })
        }
    }

    fun saveClient(client: DesktopClient) {
        mutate { current ->
            val exists = current.clients.any { it.id == client.id }
            val next = if (exists) {
                current.clients.map { if (it.id == client.id) client else it }
            } else {
                listOf(client) + current.clients
            }
            current.copy(clients = next)
        }
    }

    fun deleteClient(clientId: String) {
        mutate { current ->
            current.copy(clients = current.clients.filterNot { it.id == clientId })
        }
    }

    fun updateSettings(transform: (DesktopSettings) -> DesktopSettings) {
        mutate { current -> current.copy(settings = transform(current.settings)) }
    }

    fun resetDemoFleet() {
        mutate { current ->
            val defaults = buildDefaultSnapshot()
            defaults.copy(
                settings = defaults.settings.copy(
                    storeName = current.settings.storeName,
                    currencySymbol = current.settings.currencySymbol,
                    cloudApiUrl = current.settings.cloudApiUrl,
                    cloudSyncToken = current.settings.cloudSyncToken,
                ),
            )
        }
    }

    // --- QR Code Unit Tracking (Check-In / Check-Out) ---
    data class DesktopResolvedQrTarget(
        val item: DesktopEquipment,
        val unit: DesktopEquipmentUnit?,
        val matchedCode: String,
    )

    fun resolveQrTarget(rawCode: String): DesktopResolvedQrTarget? {
        val code = rawCode.trim()
        if (code.isEmpty()) return null
        val snap = _state.value
        val parsed = DesktopQrCodeUtils.parsePayload(code)
        return when (parsed) {
            is ParsedDesktopQrCode.UnitTag -> {
                val unit = snap.equipmentUnits.firstOrNull {
                    it.unitCode.equals(parsed.unitCode, ignoreCase = true) ||
                        it.qrPayload.equals(code, ignoreCase = true)
                }
                val item = (unit?.itemId ?: parsed.itemId)?.let { id ->
                    snap.items.firstOrNull { it.id == id }
                } ?: unit?.let { u -> snap.items.firstOrNull { it.id == u.itemId } }
                if (item != null) DesktopResolvedQrTarget(item, unit, parsed.unitCode) else null
            }
            is ParsedDesktopQrCode.EquipmentTag -> {
                val item = parsed.itemId?.let { id -> snap.items.firstOrNull { it.id == id } }
                    ?: snap.items.firstOrNull {
                        it.sku.equals(parsed.skuOrCode, ignoreCase = true) ||
                            it.barcode.equals(parsed.skuOrCode, ignoreCase = true)
                    }
                if (item != null) DesktopResolvedQrTarget(item, null, parsed.skuOrCode) else null
            }
            is ParsedDesktopQrCode.RawCode -> {
                val unit = snap.equipmentUnits.firstOrNull {
                    it.unitCode.equals(parsed.code, ignoreCase = true) ||
                        it.qrPayload.equals(parsed.code, ignoreCase = true) ||
                        it.id == parsed.code
                }
                if (unit != null) {
                    val item = snap.items.firstOrNull { it.id == unit.itemId }
                    if (item != null) return DesktopResolvedQrTarget(item, unit, unit.unitCode)
                }
                val item = snap.items.firstOrNull {
                    it.id == parsed.code ||
                        it.sku.equals(parsed.code, ignoreCase = true) ||
                        it.barcode.equals(parsed.code, ignoreCase = true) ||
                        it.name.equals(parsed.code, ignoreCase = true)
                }
                if (item != null) DesktopResolvedQrTarget(item, null, parsed.code) else null
            }
        }
    }

    fun addEquipmentUnit(
        itemId: String,
        customUnitCode: String = "",
        conditionNote: String = "",
    ): Result<DesktopEquipmentUnit> {
        val snap = _state.value
        val item = snap.items.firstOrNull { it.id == itemId }
            ?: return Result.failure(IllegalArgumentException("Equipment item not found"))
        val existingForItem = snap.equipmentUnits.filter { it.itemId == itemId }
        val used = snap.equipmentUnits.map { it.unitCode.lowercase() }.toSet()
        val unitCode = customUnitCode.trim().ifEmpty {
            var idx = existingForItem.size + 1
            var candidate = DesktopQrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
            while (candidate.lowercase() in used) {
                idx++
                candidate = DesktopQrCodeUtils.buildDefaultUnitCode(item.sku, item.id, idx)
            }
            candidate
        }
        if (unitCode.lowercase() in used) {
            return Result.failure(IllegalStateException("Unit code $unitCode already exists"))
        }
        val now = nowMs()
        val created = DesktopEquipmentUnit(
            id = newId(),
            itemId = item.id,
            itemName = item.name,
            unitCode = unitCode,
            qrPayload = DesktopQrCodeUtils.buildUnitPayload(item.id, unitCode),
            status = "checked_in",
            conditionNote = conditionNote.trim(),
            updatedAt = now,
        )
        mutate { cur -> cur.copy(equipmentUnits = cur.equipmentUnits + created) }
        return Result.success(created)
    }

    fun deleteEquipmentUnit(unitId: String) {
        mutate { cur -> cur.copy(equipmentUnits = cur.equipmentUnits.filterNot { it.id == unitId }) }
    }

    fun processEquipmentScan(
        unitId: String?,
        itemId: String,
        action: String, // "check_out" | "check_in" | "maintenance" | "inspect"
        order: DesktopRentalOrder? = null,
        note: String = "",
        adjustStock: Boolean = order == null,
    ): Result<DesktopEquipmentUnit> {
        val item = _state.value.items.firstOrNull { it.id == itemId }
            ?: return Result.failure(IllegalArgumentException("Equipment item not found"))
        val now = nowMs()
        var resultUnit: DesktopEquipmentUnit? = null

        mutate { cur ->
            val existingUnit = when {
                unitId != null -> cur.equipmentUnits.firstOrNull { it.id == unitId }
                action == "check_out" -> cur.equipmentUnits.firstOrNull { it.itemId == itemId && it.status == "checked_in" }
                action == "check_in" -> cur.equipmentUnits.firstOrNull { it.itemId == itemId && it.status == "checked_out" }
                    ?: cur.equipmentUnits.firstOrNull { it.itemId == itemId }
                else -> cur.equipmentUnits.firstOrNull { it.itemId == itemId }
            }

            val baseUnit = existingUnit ?: run {
                val existingForItem = cur.equipmentUnits.filter { it.itemId == itemId }
                val code = DesktopQrCodeUtils.buildDefaultUnitCode(item.sku, item.id, existingForItem.size + 1)
                DesktopEquipmentUnit(
                    id = newId(),
                    itemId = item.id,
                    itemName = item.name,
                    unitCode = code,
                    qrPayload = DesktopQrCodeUtils.buildUnitPayload(item.id, code),
                    status = "checked_in",
                    updatedAt = now,
                )
            }

            val wasCheckedOut = baseUnit.status == "checked_out"
            val nextStatus = when (action) {
                "check_out" -> "checked_out"
                "check_in" -> "checked_in"
                "maintenance" -> "maintenance"
                else -> baseUnit.status
            }

            val updatedUnit = baseUnit.copy(
                itemName = item.name,
                status = nextStatus,
                currentOrderId = if (action == "check_out") order?.id else if (action == "check_in" || action == "maintenance") null else baseUnit.currentOrderId,
                currentEventName = if (action == "check_out") (order?.eventName ?: "") else if (action == "check_in" || action == "maintenance") "" else baseUnit.currentEventName,
                currentCustomerName = if (action == "check_out") (order?.customerName ?: "") else if (action == "check_in" || action == "maintenance") "" else baseUnit.currentCustomerName,
                conditionNote = note.trim().ifEmpty { baseUnit.conditionNote },
                lastScanAt = now,
                updatedAt = now,
            )
            resultUnit = updatedUnit

            val updatedUnits = if (cur.equipmentUnits.any { it.id == updatedUnit.id }) {
                cur.equipmentUnits.map { if (it.id == updatedUnit.id) updatedUnit else it }
            } else {
                cur.equipmentUnits + updatedUnit
            }

            val updatedItems = if (adjustStock && item.trackStock) {
                cur.items.map { i ->
                    if (i.id != item.id) {
                        i
                    } else if (action == "check_out" && !wasCheckedOut && order == null && i.stock > 0.0) {
                        i.copy(stock = (i.stock - 1.0).coerceAtLeast(0.0))
                    } else if (action == "check_in" && wasCheckedOut && baseUnit.currentOrderId == null) {
                        i.copy(stock = i.stock + 1.0)
                    } else {
                        i
                    }
                }
            } else {
                cur.items
            }

            val log = DesktopEquipmentScanLog(
                id = newId(),
                unitId = updatedUnit.id,
                unitCode = updatedUnit.unitCode,
                itemId = item.id,
                itemName = item.name,
                action = action,
                orderId = order?.id ?: baseUnit.currentOrderId,
                eventName = order?.eventName?.ifEmpty { baseUnit.currentEventName } ?: baseUnit.currentEventName,
                customerName = order?.customerName ?: baseUnit.currentCustomerName,
                note = note.trim(),
                createdAt = now,
            )

            cur.copy(
                items = updatedItems,
                equipmentUnits = updatedUnits,
                scanLogs = (listOf(log) + cur.scanLogs).take(200),
            )
        }
        return Result.success(resultUnit!!)
    }

    // --- Digital Rental Agreement Templates ---
    fun saveAgreementTemplate(template: DesktopAgreementTemplate) {
        val now = nowMs()
        val clean = template.copy(
            title = template.title.trim().ifEmpty { "Untitled Rental Agreement" },
            category = template.category.trim().ifEmpty { "General Event" },
            createdAt = if (template.createdAt == 0L) now else template.createdAt,
            updatedAt = now,
        )
        mutate { cur ->
            val exists = cur.agreementTemplates.any { it.id == clean.id }
            val updatedList = if (exists) {
                cur.agreementTemplates.map {
                    when {
                        it.id == clean.id -> clean
                        clean.isDefault -> it.copy(isDefault = false)
                        else -> it
                    }
                }
            } else {
                val existing = if (clean.isDefault) cur.agreementTemplates.map { it.copy(isDefault = false) } else cur.agreementTemplates
                listOf(clean) + existing
            }
            cur.copy(agreementTemplates = updatedList)
        }
    }

    fun setDefaultAgreementTemplate(templateId: String) {
        mutate { cur ->
            cur.copy(
                agreementTemplates = cur.agreementTemplates.map {
                    it.copy(isDefault = it.id == templateId)
                }
            )
        }
    }

    fun duplicateAgreementTemplate(source: DesktopAgreementTemplate): DesktopAgreementTemplate {
        val now = nowMs()
        val copy = source.copy(
            id = newId(),
            title = "${source.title} (Copy)",
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )
        saveAgreementTemplate(copy)
        return copy
    }

    fun deleteAgreementTemplate(templateId: String) {
        mutate { cur ->
            val remaining = cur.agreementTemplates.filterNot { it.id == templateId }
            val normalized = if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
                remaining.mapIndexed { idx, t -> if (idx == 0) t.copy(isDefault = true) else t }
            } else {
                remaining
            }
            cur.copy(agreementTemplates = normalized)
        }
    }

    // --- Event Kits / Bundles ---
    fun saveEventKit(kit: DesktopEventKit): DesktopEventKit {
        val now = nowMs()
        val id = kit.id.ifBlank { newId() }
        val normalizedItems = kit.items
            .filter { it.itemId.isNotBlank() && it.qty > 0.0 }
            .groupBy { it.itemId }
            .values
            .map { group ->
                DesktopEventKitItem(
                    itemId = group.first().itemId,
                    itemName = group.first().itemName,
                    qty = group.sumOf { it.qty },
                )
            }
        val clean = kit.copy(
            id = id,
            name = kit.name.trim().ifEmpty { "Event Kit" },
            description = kit.description.trim(),
            category = kit.category.trim().ifEmpty { "Event Package" },
            discountPercent = kit.discountPercent.coerceIn(0.0, 90.0),
            createdAt = if (kit.createdAt == 0L) now else kit.createdAt,
            updatedAt = now,
            items = normalizedItems,
        )
        mutate { cur ->
            val exists = cur.eventKits.any { it.id == id }
            val nextKits = if (exists) {
                cur.eventKits.map { if (it.id == id) clean else it }
            } else {
                listOf(clean) + cur.eventKits
            }
            cur.copy(eventKits = nextKits)
        }
        return clean
    }

    fun deleteEventKit(kitId: String) {
        mutate { cur -> cur.copy(eventKits = cur.eventKits.filterNot { it.id == kitId }) }
    }

    // --- Equipment Damage Report Log ---
    fun saveDamagePhoto(sourceFile: File): String {
        val photosDir = File(dataDir, "damage-photos").apply { mkdirs() }
        val ext = sourceFile.extension.lowercase().ifBlank { "jpg" }
        val dest = File(photosDir, "${newId()}.$ext")
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    fun saveDamageReport(
        report: DesktopDamageReport,
        markUnitMaintenance: Boolean = true,
    ): DesktopDamageReport {
        val now = nowMs()
        val id = report.id.ifBlank { newId() }
        val cleanStatus = report.status.ifBlank { "open" }
        val resolvedAt = if (cleanStatus == "resolved") (report.resolvedAt ?: now) else null
        val clean = report.copy(
            id = id,
            itemId = report.itemId.trim(),
            itemSku = report.itemSku.trim(),
            itemName = report.itemName.trim().ifEmpty { report.itemId.trim() },
            unitCode = report.unitCode.trim(),
            customerName = report.customerName.trim(),
            severity = report.severity.ifBlank { "minor" },
            status = cleanStatus,
            repairCostCents = report.repairCostCents.coerceAtLeast(0L),
            notes = report.notes.trim(),
            photoPaths = report.photoPaths.filter { it.isNotBlank() }.distinct(),
            reportedBy = report.reportedBy.trim().ifEmpty { "Desktop Operator" },
            createdAt = if (report.createdAt == 0L) now else report.createdAt,
            resolvedAt = resolvedAt,
        )
        mutate { cur ->
            val exists = cur.damageReports.any { it.id == id }
            val nextReports = if (exists) {
                cur.damageReports.map { if (it.id == id) clean else it }
            } else {
                listOf(clean) + cur.damageReports
            }
            val nextUnits = if (markUnitMaintenance && clean.unitCode.isNotBlank()) {
                val noteSummary = clean.notes.take(120).ifEmpty { "Damage logged (${clean.severity})" }
                cur.equipmentUnits.map { u ->
                    if (u.unitCode.equals(clean.unitCode, ignoreCase = true)) {
                        u.copy(
                            status = "maintenance",
                            conditionNote = noteSummary,
                            lastScanAt = now,
                            updatedAt = now,
                        )
                    } else {
                        u
                    }
                }
            } else {
                cur.equipmentUnits
            }
            cur.copy(damageReports = nextReports, equipmentUnits = nextUnits)
        }
        return clean
    }

    fun updateDamageReportStatus(reportId: String, status: String) {
        val now = nowMs()
        mutate { cur ->
            cur.copy(
                damageReports = cur.damageReports.map { r ->
                    if (r.id == reportId) {
                        r.copy(
                            status = status,
                            resolvedAt = if (status == "resolved") now else null,
                        )
                    } else {
                        r
                    }
                }
            )
        }
    }

    fun deleteDamageReport(reportId: String) {
        val target = _state.value.damageReports.firstOrNull { it.id == reportId }
        mutate { cur ->
            cur.copy(damageReports = cur.damageReports.filterNot { it.id == reportId })
        }
        target?.photoPaths?.forEach { path ->
            runCatching { File(path).delete() }
        }
    }

    fun buildPlainTextReceipt(order: DesktopRentalOrder, settings: DesktopSettings): String {
        val sym = settings.currencySymbol
        val sb = StringBuilder()
        sb.appendLine("====================================")
        sb.appendLine(settings.storeName.uppercase())
        sb.appendLine("EVENT EQUIPMENT RENTAL RECEIPT")
        sb.appendLine("Order #${order.number.toString().padStart(4, '0')} · ${formatDateTime(order.createdAt)}")
        sb.appendLine("Status: ${
            when {
                !order.isCompleted -> "REFUNDED"
                order.isReturned -> "RETURNED"
                else -> "OUT (Due ${formatShortDate(order.rentalEndMs)})"
            }
        }")
        if (order.eventName.isNotEmpty()) sb.appendLine("Event: ${order.eventName}")
        if (order.eventVenue.isNotEmpty()) sb.appendLine("Venue: ${order.eventVenue}")
        if (!order.customerName.isNullOrEmpty()) sb.appendLine("Client: ${order.customerName}")
        sb.appendLine("Duration: ${order.rentalDays} ${if (order.rentalDays == 1) "day" else "days"} (${formatShortDate(order.rentalStartMs)} -> ${formatShortDate(order.rentalEndMs)})")
        sb.appendLine("------------------------------------")
        order.lines.forEach { line ->
            sb.appendLine("${line.name}")
            sb.appendLine("  ${formatQty(line.qty)} x ${formatCents(line.priceCents, sym)}/d x ${order.rentalDays}d = ${formatCents(line.lineTotalCents, sym)}")
        }
        sb.appendLine("------------------------------------")
        sb.appendLine("Subtotal:   ${formatCents(order.subtotalCents, sym)}")
        if (order.discountCents > 0L) {
            sb.appendLine("Discount:   -${formatCents(order.discountCents, sym)}")
        }
        if (order.taxCents > 0L) {
            sb.appendLine("Tax:        ${formatCents(order.taxCents, sym)}")
        }
        sb.appendLine("TOTAL DUE:  ${formatCents(order.totalCents, sym)}")
        if (order.depositCents > 0L) {
            sb.appendLine("Deposit (Refundable): ${formatCents(order.depositCents, sym)}")
        }
        sb.appendLine("Paid (${order.paymentType.uppercase()}): ${formatCents(order.paidCents, sym)}")
        if (order.changeCents > 0L) {
            sb.appendLine("Change:     ${formatCents(order.changeCents, sym)}")
        }
        if (settings.receiptFooter.isNotBlank()) {
            sb.appendLine("------------------------------------")
            sb.appendLine(settings.receiptFooter)
        }
        sb.appendLine("====================================")
        return sb.toString()
    }

    fun exportRentalsCsv(targetFile: File): Int {
        val snap = _state.value
        val sym = snap.settings.currencySymbol
        val sb = StringBuilder()
        sb.appendLine("OrderNumber,BookedAt,RentalDays,DueDate,RentalStatus,OrderStatus,Client,Event,Venue,PaymentType,Total,Deposit")
        snap.orders.forEach { o ->
            fun csv(v: String): String = "\"${v.replace("\"", "\"\"")}\""
            sb.appendLine(
                listOf(
                    "#${o.number.toString().padStart(4, '0')}",
                    csv(formatDateTime(o.createdAt)),
                    o.rentalDays.toString(),
                    csv(formatShortDate(o.rentalEndMs)),
                    csv(o.rentalStatus),
                    csv(o.status),
                    csv(o.customerName ?: ""),
                    csv(o.eventName),
                    csv(o.eventVenue),
                    csv(o.paymentType),
                    csv(formatCents(o.totalCents, sym)),
                    csv(formatCents(o.depositCents, sym)),
                ).joinToString(",")
            )
        }
        targetFile.writeText(sb.toString(), Charsets.UTF_8)
        return snap.orders.size
    }

    fun exportBackupJson(targetFile: File) {
        val snap = _state.value
        val salesRecords = snap.orders.map { o ->
            ExportSaleRecord(
                id = o.id,
                number = o.number,
                createdAt = o.createdAt,
                subtotalCents = o.subtotalCents,
                discountCents = o.discountCents,
                taxCents = o.taxCents,
                totalCents = o.totalCents,
                paymentType = o.paymentType,
                paidCents = o.paidCents,
                changeCents = o.changeCents,
                customerId = o.customerId,
                customerName = o.customerName,
                note = o.note,
                status = o.status,
                rentalDays = o.rentalDays,
                rentalStartMs = o.rentalStartMs,
                rentalEndMs = o.rentalEndMs,
                depositCents = o.depositCents,
                rentalStatus = o.rentalStatus,
                returnedAtMs = o.returnedAtMs,
                eventName = o.eventName,
                eventVenue = o.eventVenue,
                lines = o.lines,
            )
        }
        val envelope = LoyPosExportV1(
            payload = LoyPosExportPayload(
                categories = snap.categories,
                items = snap.items,
                customers = snap.clients,
                sales = salesRecords,
                receipts = snap.orders,
            ),
        )
        targetFile.writeText(json.encodeToString(envelope), Charsets.UTF_8)
    }

    fun importBackupJson(sourceFile: File): String {
        val raw = sourceFile.readText(Charsets.UTF_8)
        val envelope = json.decodeFromString<LoyPosExportV1>(raw)
        if (envelope.app != "loypos-export") {
            error("Unsupported backup format: ${envelope.app}")
        }
        val p = envelope.payload
        val importedOrders = if (p.receipts.isNotEmpty()) {
            p.receipts
        } else {
            p.sales.map { s ->
                DesktopRentalOrder(
                    id = s.id,
                    number = s.number,
                    createdAt = s.createdAt,
                    subtotalCents = s.subtotalCents,
                    discountCents = s.discountCents,
                    taxCents = s.taxCents,
                    totalCents = s.totalCents,
                    paymentType = s.paymentType,
                    paidCents = s.paidCents,
                    changeCents = s.changeCents,
                    customerId = s.customerId,
                    customerName = s.customerName,
                    note = s.note,
                    status = s.status,
                    rentalDays = s.rentalDays,
                    rentalStartMs = s.rentalStartMs,
                    rentalEndMs = s.rentalEndMs,
                    depositCents = s.depositCents,
                    rentalStatus = s.rentalStatus,
                    returnedAtMs = s.returnedAtMs,
                    eventName = s.eventName,
                    eventVenue = s.eventVenue,
                    lines = s.lines,
                )
            }
        }
        val maxOrderNum = importedOrders.maxOfOrNull { it.number } ?: 0
        mutate { cur ->
            cur.copy(
                categories = p.categories.ifEmpty { cur.categories },
                items = p.items.ifEmpty { cur.items },
                clients = p.customers,
                orders = importedOrders.sortedByDescending { it.createdAt },
                settings = cur.settings.copy(nextReceiptNumber = maxOrderNum + 1),
            )
        }
        return "Imported ${p.items.size} equipment items and ${importedOrders.size} rental orders"
    }

    fun syncCloudHealth(): Result<String> {
        val snap = _state.value
        val baseUrl = snap.settings.cloudApiUrl.trim().trimEnd('/')
        if (baseUrl.isEmpty()) {
            return Result.failure(IllegalStateException("Cloud API URL is empty"))
        }
        return runCatching {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
            val reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/health"))
                .timeout(Duration.ofSeconds(12))
                .GET()
            val token = snap.settings.cloudSyncToken.trim()
            if (token.isNotEmpty()) {
                reqBuilder.header("Authorization", "Bearer $token")
            }
            val resp = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() in 200..299) {
                "Cloud endpoint reachable (HTTP ${resp.statusCode()})"
            } else {
                error("Server responded with HTTP ${resp.statusCode()}")
            }
        }
    }

    fun computeMonthlyIncome(months: Int = 6): List<DesktopMonthlyIncomePoint> {
        val clamped = months.coerceIn(1, 24)
        val zone = ZoneId.systemDefault()
        val nowYm = YearMonth.now(zone)
        val completed = _state.value.orders.filter { it.isCompleted }
        val grouped = completed.groupBy {
            YearMonth.from(Instant.ofEpochMilli(it.createdAt).atZone(zone))
        }
        return ((clamped - 1) downTo 0).map { offset ->
            val ym = nowYm.minusMonths(offset.toLong())
            val orders = grouped[ym].orEmpty()
            DesktopMonthlyIncomePoint(
                yearMonthKey = ym.toString(),
                label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                totalCents = orders.sumOf { it.totalCents },
                ordersCount = orders.size,
            )
        }
    }

    fun computeCategoryUtilization(): List<DesktopUtilizationPoint> {
        val snap = _state.value
        val activeOut = mutableMapOf<String, Double>()
        snap.orders.filter { it.isRentalOut }.forEach { order ->
            order.lines.forEach { line ->
                val id = line.itemId ?: return@forEach
                activeOut[id] = (activeOut[id] ?: 0.0) + line.qty
            }
        }
        val catNames = snap.categories.associate { it.id to it.name }
        return snap.items
            .filter { it.trackStock }
            .groupBy { it.categoryId ?: "other" }
            .map { (catId, group) ->
                val outUnits = group.sumOf { activeOut[it.id] ?: 0.0 }
                val totalCap = group.sumOf { (it.stock + (activeOut[it.id] ?: 0.0)).coerceAtLeast(0.0) }
                val pct = if (totalCap <= 0.0) 0.0 else (outUnits / totalCap * 100.0).coerceIn(0.0, 100.0)
                DesktopUtilizationPoint(
                    id = catId,
                    label = catNames[catId] ?: "Other",
                    utilizationPercent = pct,
                    activeOutUnits = outUnits,
                    totalCapacityUnits = totalCap,
                )
            }
            .sortedByDescending { it.utilizationPercent }
    }

    fun saveMaintenanceSchedule(schedule: DesktopMaintenanceSchedule) {
        mutate { state ->
            val existing = state.maintenanceSchedules.toMutableList()
            val idx = existing.indexOfFirst { it.id == schedule.id }
            val now = nowMs()
            val toSave = if (schedule.createdAt == 0L) schedule.copy(createdAt = now) else schedule
            if (idx >= 0) {
                existing[idx] = toSave
            } else {
                existing.add(0, toSave)
            }
            state.copy(maintenanceSchedules = existing)
        }
    }

    fun deleteMaintenanceSchedule(id: String) {
        mutate { state ->
            state.copy(maintenanceSchedules = state.maintenanceSchedules.filterNot { it.id == id })
        }
    }

    fun updateMaintenanceStatus(id: String, status: String) {
        mutate { state ->
            val list = state.maintenanceSchedules.map { m ->
                if (m.id == id) m.copy(status = status) else m
            }
            state.copy(maintenanceSchedules = list)
        }
    }

    fun completeMaintenance(id: String) {
        mutate { state ->
            val now = nowMs()
            val list = state.maintenanceSchedules.map { m ->
                if (m.id == id) {
                    val nextDue = now + (m.intervalDays.coerceAtLeast(1) * 86_400_000L)
                    m.copy(lastServicedMs = now, nextDueMs = nextDue, status = "completed")
                } else m
            }
            state.copy(maintenanceSchedules = list)
        }
    }

    fun getItemRentalCount(itemId: String): Int {
        var count = 0
        _state.value.orders.filter { it.isCompleted }.forEach { ord ->
            ord.lines.forEach { l ->
                if (l.itemId == itemId) {
                    count += l.qty.toInt().coerceAtLeast(1)
                }
            }
        }
        return count
    }

    // --- Delivery & Pickup Dispatch Board ---
    fun saveDeliveryStop(stop: DesktopDeliveryStop) {
        mutate { state ->
            val existing = state.deliveryStops.toMutableList()
            val idx = existing.indexOfFirst { it.id == stop.id }
            val now = nowMs()
            val toSave = if (stop.createdAt == 0L) stop.copy(createdAt = now) else stop
            if (idx >= 0) {
                existing[idx] = toSave
            } else {
                existing.add(0, toSave)
            }
            state.copy(deliveryStops = existing)
        }
    }

    fun deleteDeliveryStop(id: String) {
        mutate { state ->
            state.copy(deliveryStops = state.deliveryStops.filterNot { it.id == id })
        }
    }

    fun advanceDeliveryStop(id: String) {
        mutate { state ->
            val now = nowMs()
            val list = state.deliveryStops.map { s ->
                when {
                    s.id == id && s.status == "scheduled" -> s.copy(status = "en_route")
                    s.id == id && s.status == "en_route" -> s.copy(status = "done", completedAtMs = now)
                    else -> s
                }
            }
            state.copy(deliveryStops = list)
        }
    }

    fun failDeliveryStop(id: String) {
        mutate { state ->
            val now = nowMs()
            val list = state.deliveryStops.map { s ->
                if (s.id == id) s.copy(status = "failed", completedAtMs = now) else s
            }
            state.copy(deliveryStops = list)
        }
    }

    fun scheduleStopsForOrder(orderId: String): Int {
        val snap = _state.value
        val order = snap.orders.firstOrNull { it.id == orderId } ?: return 0
        if (order.eventVenue.isBlank()) return 0
        val existingTypes = snap.deliveryStops.filter { it.orderId == orderId }.map { it.type }.toSet()
        val toCreate = mutableListOf<DesktopDeliveryStop>()
        val now = nowMs()
        val itemsSummary = order.lines.joinToString(", ") { l ->
            "${formatQty(l.qty)}× ${l.name}"
        }.take(180)
        if ("delivery" !in existingTypes) {
            toCreate += DesktopDeliveryStop(
                id = newId(),
                type = "delivery",
                orderId = order.id,
                orderNumber = order.number,
                customerName = order.customerName ?: "",
                eventName = order.eventName,
                venue = order.eventVenue,
                scheduledMs = maxOf(order.rentalStartMs, now),
                itemsSummary = itemsSummary,
                status = "scheduled",
                createdAt = now,
            )
        }
        if ("pickup" !in existingTypes) {
            toCreate += DesktopDeliveryStop(
                id = newId(),
                type = "pickup",
                orderId = order.id,
                orderNumber = order.number,
                customerName = order.customerName ?: "",
                eventName = order.eventName,
                venue = order.eventVenue,
                scheduledMs = maxOf(order.rentalEndMs, now + 86_400_000L),
                itemsSummary = itemsSummary,
                notes = "Pickup after event — verify all units back against the packing list.",
                status = "scheduled",
                createdAt = now,
            )
        }
        if (toCreate.isEmpty()) return 0
        mutate { state ->
            state.copy(deliveryStops = toCreate + state.deliveryStops)
        }
        return toCreate.size
    }

    fun exportRentalHistoryCsv(targetFile: File): Int {
        val snapshot = _state.value
        val sb = StringBuilder()
        sb.append("OrderNumber,CreatedAt,ClientName,EventName,EventVenue,RentalDays,RentalStart,RentalEnd,Status,PaymentType,TotalCents,DepositCents,LineItemName,LineQty,LinePriceCents,LineTotalCents\n")
        var rowCount = 0
        snapshot.orders.forEach { ord ->
            val dateStr = formatDateTime(ord.createdAt)
            val startStr = formatShortDate(ord.rentalStartMs)
            val endStr = formatShortDate(ord.rentalEndMs)
            val client = (ord.customerName ?: "Walk-in").replace(",", " ")
            val event = ord.eventName.replace(",", " ")
            val venue = ord.eventVenue.replace(",", " ")
            val status = if (ord.isReturned) "Returned" else if (ord.isRentalOut) "Active Out" else if (!ord.isCompleted) "Refunded" else "Completed"
            if (ord.lines.isEmpty()) {
                sb.append("#${ord.number},\"$dateStr\",\"$client\",\"$event\",\"$venue\",${ord.rentalDays},\"$startStr\",\"$endStr\",$status,${ord.paymentType},${ord.totalCents},${ord.depositCents},-,0,0,0\n")
                rowCount++
            } else {
                ord.lines.forEach { line ->
                    val lineName = line.name.replace(",", " ")
                    sb.append("#${ord.number},\"$dateStr\",\"$client\",\"$event\",\"$venue\",${ord.rentalDays},\"$startStr\",\"$endStr\",$status,${ord.paymentType},${ord.totalCents},${ord.depositCents},\"$lineName\",${line.qty},${line.priceCents},${line.lineTotalCents}\n")
                    rowCount++
                }
            }
        }
        targetFile.writeText(sb.toString(), Charsets.UTF_8)
        return rowCount
    }

    fun exportRentalHistoryReportPdf(targetFile: File): Boolean {
        val snapshot = _state.value
        val sym = snapshot.settings.currencySymbol
        val completed = snapshot.orders.filter { it.isCompleted }
        val totalRevenue = completed.sumOf { it.totalCents }
        val totalDeposits = completed.sumOf { it.depositCents }
        val sb = StringBuilder()
        sb.append("========================================================\n")
        sb.append("         EVENTGEAR RENTALS — ACCOUNTING STATEMENT       \n")
        sb.append("         Rental History & Revenue Record-Keeping         \n")
        sb.append("========================================================\n\n")
        sb.append("Generated On: ${formatDateTime(nowMs())}\n")
        sb.append("Company Name: ${snapshot.settings.storeName}\n")
        sb.append("Total Completed Orders: ${completed.size}\n")
        sb.append("Total Rental Revenue: ${formatCents(totalRevenue, sym)}\n")
        sb.append("Total Security Deposits Held: ${formatCents(totalDeposits, sym)}\n")
        sb.append("Active Fleet Inventory SKUs: ${snapshot.items.size}\n")
        sb.append("Open Maintenance Tasks: ${snapshot.maintenanceSchedules.count { !it.isDue(nowMs()) && it.status != "completed" }}\n\n")
        val stops = snapshot.deliveryStops
        val activeStops = stops.filter { it.isActive }
        sb.append("Dispatch Board — Active Runs: ${activeStops.size}, Completed: ${stops.count { it.status == "done" }}, Failed: ${stops.count { it.status == "failed" }}\n")
        activeStops.sortedBy { it.scheduledMs }.take(10).forEach { s ->
            sb.append("  ${if (s.isDelivery) "DELIVERY" else "PICKUP"} ${formatDateTime(s.scheduledMs)} — ${s.venue}${if (s.driverName.isNotBlank()) " (${s.driverName})" else ""}\n")
        }
        sb.append("\n")
        sb.append("--------------------------------------------------------\n")
        sb.append(" ITEMIZED RENTAL HISTORY LOGS\n")
        sb.append("--------------------------------------------------------\n")
        completed.forEach { ord ->
            sb.append("Order #${ord.number.toString().padStart(4, '0')} | ${formatDateTime(ord.createdAt)} | Client: ${ord.customerName ?: "Walk-in"}\n")
            sb.append("  Event: ${ord.eventName.ifBlank { "General Event" }} (${formatShortDate(ord.rentalStartMs)} → ${formatShortDate(ord.rentalEndMs)}, ${ord.rentalDays}d)\n")
            sb.append("  Status: ${if (ord.isReturned) "Returned" else "Active Out"} | Total: ${formatCents(ord.totalCents, sym)}\n")
            ord.lines.forEach { l ->
                sb.append("    • ${formatQty(l.qty)} × ${l.name} @ ${formatCents(l.priceCents, sym)}/d = ${formatCents(l.lineTotalCents, sym)}\n")
            }
            sb.append("\n")
        }
        sb.append("========================================================\n")
        sb.append(" End of Accounting Statement — LoyPOS Rental Engine       \n")
        sb.append("========================================================\n")
        targetFile.writeText(sb.toString(), Charsets.UTF_8)
        return true
    }
}
