package com.loypos.desktop.data

import com.loypos.desktop.core.DemoRentalCatalog
import com.loypos.desktop.core.computeRentalTotals
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.model.DesktopCartLine
import com.loypos.desktop.model.DesktopCategory
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopHeldQuote
import com.loypos.desktop.model.DesktopRentalLine
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopSettings
import com.loypos.desktop.model.DesktopStoreSnapshot
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
                    return parsed
                }
            } catch (_: Exception) {
            }
        }
        val initial = buildDefaultSnapshot()
        saveSnapshot(initial)
        return initial
    }

    private fun buildDefaultSnapshot(): DesktopStoreSnapshot {
        val now = nowMs()
        return DesktopStoreSnapshot(
            categories = DemoRentalCatalog.buildCategories(),
            items = DemoRentalCatalog.buildEquipment(now),
            clients = DemoRentalCatalog.buildClients(now),
            orders = emptyList(),
            heldQuotes = emptyList(),
            settings = DesktopSettings(),
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
            snap.copy(items = updatedItems, orders = updatedOrders)
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
}
