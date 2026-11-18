package com.loypos.app.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.loypos.app.data.LoyPosRepository
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.data.local.ReceiptEntity
import com.loypos.app.data.local.ReceiptLineEntity
import com.loypos.app.data.local.StockMovementEntity
import com.loypos.app.data.local.TicketEntity
import com.loypos.app.data.local.TicketLineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

data class LocalBackup(
    val path: String,
    val createdAtMs: Long,
    val sizeBytes: Long,
) {
    val sizeLabel: String get() = formatBytes(sizeBytes)
    val label: String get() = formatShortDayMonth(createdAtMs)
}

data class BackupResult(
    val items: Int,
    val categories: Int,
    val customers: Int,
    val branches: Int,
    val receipts: Int,
    val tickets: Int,
    val sourceApp: String,
)

object BackupManager {
    const val FORMAT = "loypos-export"
    const val VERSION = 1
    private const val KEEP_COPIES = 5

    private fun backupDir(context: Context): File =
        File(context.filesDir, "safety-backups").apply { if (!exists()) mkdirs() }

    suspend fun buildSnapshotJson(context: Context, repo: LoyPosRepository): JSONObject =
        withContext(Dispatchers.IO) {
            val dao = repo.dao
            val settings = repo.loadRawSettings()
            val branches = dao.getAllBranches()
            val categories = dao.getAllCategories()
            val items = dao.getAllItems()
            val customers = dao.getAllCustomers()
            val receipts = dao.getAllReceipts()
            val receiptLinesById = dao.getAllReceiptLines().groupBy { it.receiptId }
            val tickets = dao.getAllTickets()
            val ticketLinesById = dao.getAllTicketLines().groupBy { it.ticketId }
            val movements = dao.getAllStockMovements()

            val skipKeys = setOf("session_user", "cloud_team_id")
            val settingsJson = JSONObject()
            settings.entries.filter { it.key !in skipKeys }.forEach {
                settingsJson.put(it.key, it.value)
            }

            val root = JSONObject()
            root.put("format", FORMAT)
            root.put("version", VERSION)
            root.put("exported_at", nowMs())
            root.put("source_app", "loypos-android")
            root.put("settings", settingsJson)

            root.put("branches", JSONArray().apply {
                branches.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("address", b.address)
                        put("phone", b.phone)
                        put("created_at", b.createdAt)
                        put("archived", if (b.archived) 1 else 0)
                    })
                }
            })

            root.put("categories", JSONArray().apply {
                categories.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("position", c.position)
                    })
                }
            })

            root.put("items", JSONArray().apply {
                items.forEach { it ->
                    put(JSONObject().apply {
                        put("id", it.id)
                        put("category_id", it.categoryId ?: JSONObject.NULL)
                        put("name", it.name)
                        put("price_cents", it.priceCents)
                        put("cost_cents", it.costCents)
                        put("sku", it.sku)
                        put("barcode", it.barcode)
                        put("track_stock", if (it.trackStock) 1 else 0)
                        put("stock", it.stock)
                        put("archived", if (it.archived) 1 else 0)
                        put("branch_ids", it.branchIds)
                        put("image_path", it.imagePath)
                        if (it.hasPhoto) {
                            ItemPhotos.readBase64(it.imagePath)?.let { b64 -> put("photo_b64", b64) }
                        }
                    })
                }
            })

            root.put("customers", JSONArray().apply {
                customers.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phone", c.phone)
                        put("email", c.email)
                        put("note", c.note)
                        put("points", c.points)
                        put("created_at", c.createdAt)
                    })
                }
            })

            root.put("receipts", JSONArray().apply {
                receipts.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("number", r.number)
                        put("created_at", r.createdAt)
                        put("subtotal_cents", r.subtotalCents)
                        put("discount_cents", r.discountCents)
                        put("discount_type", r.discountType ?: JSONObject.NULL)
                        put("discount_value", r.discountValue ?: JSONObject.NULL)
                        put("tax_cents", r.taxCents)
                        put("total_cents", r.totalCents)
                        put("paid_cents", r.paidCents)
                        put("change_cents", r.changeCents)
                        put("payment_type", r.paymentType)
                        put("customer_id", r.customerId ?: JSONObject.NULL)
                        put("customer_name", r.customerName ?: JSONObject.NULL)
                        put("status", r.status)
                        put("note", r.note)
                        put("branch_id", r.branchId ?: JSONObject.NULL)
                        put("cashier_id", r.cashierId ?: JSONObject.NULL)
                        put("cashier_name", r.cashierName ?: JSONObject.NULL)
                        put("rental_days", r.rentalDays)
                        put("rental_start_ms", r.rentalStartMs)
                        put("rental_end_ms", r.rentalEndMs)
                        put("deposit_cents", r.depositCents)
                        put("rental_status", r.rentalStatus)
                        put("returned_at_ms", r.returnedAtMs ?: JSONObject.NULL)
                        put("event_name", r.eventName)
                        put("event_venue", r.eventVenue)
                        put("lines", JSONArray().apply {
                            receiptLinesById[r.id].orEmpty().forEach { l ->
                                put(JSONObject().apply {
                                    put("id", l.id)
                                    put("receipt_id", l.receiptId)
                                    put("item_id", l.itemId)
                                    put("name", l.name)
                                    put("price_cents", l.priceCents)
                                    put("qty", l.qty)
                                    put("line_total_cents", l.lineTotalCents)
                                })
                            }
                        })
                    })
                }
            })

            root.put("tickets", JSONArray().apply {
                tickets.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("created_at", t.createdAt)
                        put("name", t.name)
                        put("discount_type", t.discountType ?: JSONObject.NULL)
                        put("discount_value", t.discountValue ?: JSONObject.NULL)
                        put("customer_id", t.customerId ?: JSONObject.NULL)
                        put("customer_name", t.customerName ?: JSONObject.NULL)
                        put("note", t.note)
                        put("branch_id", t.branchId ?: JSONObject.NULL)
                        put("rental_days", t.rentalDays)
                        put("rental_start_ms", t.rentalStartMs)
                        put("rental_end_ms", t.rentalEndMs)
                        put("deposit_cents", t.depositCents)
                        put("event_name", t.eventName)
                        put("event_venue", t.eventVenue)
                        put("lines", JSONArray().apply {
                            ticketLinesById[t.id].orEmpty().forEach { l ->
                                put(JSONObject().apply {
                                    put("id", l.id)
                                    put("ticket_id", l.ticketId)
                                    put("item_id", l.itemId)
                                    put("name", l.name)
                                    put("price_cents", l.priceCents)
                                    put("qty", l.qty)
                                })
                            }
                        })
                    })
                }
            })

            root.put("stock_movements", JSONArray().apply {
                movements.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("item_id", m.itemId)
                        put("qty", m.qty)
                        put("type", m.type)
                        put("created_at", m.createdAt)
                        put("note", m.note)
                        put("receipt_id", m.receiptId ?: JSONObject.NULL)
                    })
                }
            })

            root
        }

    suspend fun exportAndShare(context: Context, repo: LoyPosRepository): File =
        withContext(Dispatchers.IO) {
            val json = buildSnapshotJson(context, repo)
            val cal = Calendar.getInstance()
            val name = "loypos-export-${cal.get(Calendar.YEAR)}" +
                "${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}" +
                "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}-" +
                "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}" +
                "${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}.json"
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(dir, name)
            file.writeText(json.toString(2))

            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "LoyPOS data export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export LoyPOS Data"))
            }
            file
        }

    suspend fun saveSafetyCopy(context: Context, repo: LoyPosRepository): LocalBackup =
        withContext(Dispatchers.IO) {
            val json = buildSnapshotJson(context, repo)
            val dir = backupDir(context)
            val stamp = nowMs()
            val file = File(dir, "safety-$stamp.json")
            file.writeText(json.toString(2))
            pruneLocal(context)
            LocalBackup(file.absolutePath, file.lastModified(), file.length())
        }

    fun listLocalBackups(context: Context): List<LocalBackup> {
        val dir = backupDir(context)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.map { LocalBackup(it.absolutePath, it.lastModified(), it.length()) }
            ?.sortedByDescending { it.createdAtMs }
            .orEmpty()
    }

    private fun pruneLocal(context: Context) {
        val all = listLocalBackups(context)
        all.drop(KEEP_COPIES).forEach { File(it.path).delete() }
    }

    suspend fun importRawJson(
        context: Context,
        repo: LoyPosRepository,
        rawJson: String,
    ): BackupResult = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            throw IllegalArgumentException("This file is not valid JSON.")
        }
        if (root.optString("format") != FORMAT) {
            throw IllegalArgumentException("This file is not a LoyPOS export.")
        }
        if (root.optInt("version", 0) > VERSION) {
            throw IllegalArgumentException("This export was made by a newer LoyPOS version.")
        }

        // Write a safety backup of the current shop before replacing
        saveSafetyCopy(context, repo)

        val settingsObj = root.optJSONObject("settings") ?: JSONObject()
        val settingsMap = buildMap<String, String> {
            settingsObj.keys().forEach { key ->
                put(key, settingsObj.optString(key, ""))
            }
        }

        val branches = parseArray(root.optJSONArray("branches")) { o ->
            BranchEntity(
                id = o.optString("id").ifEmpty { newId() },
                name = o.optString("name", "Branch"),
                address = o.optString("address", ""),
                phone = o.optString("phone", ""),
                createdAt = o.optLong("created_at", nowMs()),
                archived = o.optInt("archived", 0) == 1 || o.optBoolean("archived", false),
                updatedAt = nowMs(),
            )
        }

        val categories = parseArray(root.optJSONArray("categories")) { o ->
            CategoryEntity(
                id = o.optString("id").ifEmpty { newId() },
                name = o.optString("name", "Category"),
                position = o.optInt("position", 0),
                updatedAt = nowMs(),
            )
        }

        val items = parseArray(root.optJSONArray("items")) { o ->
            val b64 = o.optString("photo_b64", "")
            val photoPath = if (b64.isNotEmpty()) {
                ItemPhotos.decodeAndSaveBase64(context, b64) ?: ""
            } else ""
            ItemEntity(
                id = o.optString("id").ifEmpty { newId() },
                categoryId = o.optString("category_id").ifEmpty { null },
                name = o.optString("name", "Item"),
                priceCents = o.optLong("price_cents", 0L),
                costCents = o.optLong("cost_cents", 0L),
                sku = o.optString("sku", ""),
                barcode = o.optString("barcode", ""),
                trackStock = o.optInt("track_stock", 0) == 1 || o.optBoolean("track_stock", false),
                stock = o.optDouble("stock", 0.0),
                archived = o.optInt("archived", 0) == 1 || o.optBoolean("archived", false),
                branchIds = o.optString("branch_ids", ""),
                imagePath = photoPath,
                updatedAt = nowMs(),
            )
        }

        val customers = parseArray(root.optJSONArray("customers")) { o ->
            CustomerEntity(
                id = o.optString("id").ifEmpty { newId() },
                name = o.optString("name", "Customer"),
                phone = o.optString("phone", ""),
                email = o.optString("email", ""),
                note = o.optString("note", ""),
                points = o.optInt("points", 0),
                createdAt = o.optLong("created_at", nowMs()),
                updatedAt = nowMs(),
            )
        }

        val receiptLines = mutableListOf<ReceiptLineEntity>()
        val receipts = parseArray(root.optJSONArray("receipts")) { o ->
            val rid = o.optString("id").ifEmpty { newId() }
            val linesArr = o.optJSONArray("lines")
            if (linesArr != null) {
                for (i in 0 until linesArr.length()) {
                    val l = linesArr.getJSONObject(i)
                    receiptLines.add(
                        ReceiptLineEntity(
                            id = l.optString("id").ifEmpty { newId() },
                            receiptId = rid,
                            itemId = l.optString("item_id", ""),
                            name = l.optString("name", ""),
                            priceCents = l.optLong("price_cents", 0L),
                            qty = l.optDouble("qty", 1.0),
                            lineTotalCents = l.optLong("line_total_cents", 0L),
                        )
                    )
                }
            }
            val createdAt = o.optLong("created_at", nowMs())
            val rDays = o.optInt("rental_days", 1).coerceAtLeast(1)
            val rStart = o.optLong("rental_start_ms", createdAt).let { if (it <= 0L) createdAt else it }
            val rEnd = o.optLong("rental_end_ms", rStart + rDays * 86_400_000L).let { if (it <= 0L) rStart + rDays * 86_400_000L else it }
            val statusVal = o.optString("status", "completed")
            val rStatus = o.optString("rental_status", if (statusVal == "refunded") "refunded" else "out")
            ReceiptEntity(
                id = rid,
                number = o.optInt("number", 1),
                createdAt = createdAt,
                subtotalCents = o.optLong("subtotal_cents", 0L),
                discountCents = o.optLong("discount_cents", 0L),
                discountType = o.optString("discount_type").ifEmpty { null },
                discountValue = if (o.isNull("discount_value")) null else o.optDouble("discount_value"),
                taxCents = o.optLong("tax_cents", 0L),
                totalCents = o.optLong("total_cents", 0L),
                paidCents = o.optLong("paid_cents", 0L),
                changeCents = o.optLong("change_cents", 0L),
                paymentType = o.optString("payment_type", "cash"),
                customerId = o.optString("customer_id").ifEmpty { null },
                customerName = o.optString("customer_name").ifEmpty { null },
                status = statusVal,
                note = o.optString("note", ""),
                branchId = o.optString("branch_id").ifEmpty { null },
                cashierId = o.optString("cashier_id").ifEmpty { null },
                cashierName = o.optString("cashier_name").ifEmpty { null },
                updatedAt = nowMs(),
                rentalDays = rDays,
                rentalStartMs = rStart,
                rentalEndMs = rEnd,
                depositCents = o.optLong("deposit_cents", 0L),
                rentalStatus = rStatus,
                returnedAtMs = if (o.isNull("returned_at_ms")) null else o.optLong("returned_at_ms"),
                eventName = o.optString("event_name", ""),
                eventVenue = o.optString("event_venue", ""),
            )
        }

        val ticketLines = mutableListOf<TicketLineEntity>()
        val tickets = parseArray(root.optJSONArray("tickets")) { o ->
            val tid = o.optString("id").ifEmpty { newId() }
            val linesArr = o.optJSONArray("lines")
            if (linesArr != null) {
                for (i in 0 until linesArr.length()) {
                    val l = linesArr.getJSONObject(i)
                    ticketLines.add(
                        TicketLineEntity(
                            id = l.optString("id").ifEmpty { newId() },
                            ticketId = tid,
                            itemId = l.optString("item_id", ""),
                            name = l.optString("name", ""),
                            priceCents = l.optLong("price_cents", 0L),
                            qty = l.optDouble("qty", 1.0),
                        )
                    )
                }
            }
            val tCreated = o.optLong("created_at", nowMs())
            val tDays = o.optInt("rental_days", 1).coerceAtLeast(1)
            val tStart = o.optLong("rental_start_ms", tCreated).let { if (it <= 0L) tCreated else it }
            val tEnd = o.optLong("rental_end_ms", tStart + tDays * 86_400_000L).let { if (it <= 0L) tStart + tDays * 86_400_000L else it }
            TicketEntity(
                id = tid,
                createdAt = tCreated,
                name = o.optString("name", ""),
                discountType = o.optString("discount_type").ifEmpty { null },
                discountValue = if (o.isNull("discount_value")) null else o.optDouble("discount_value"),
                customerId = o.optString("customer_id").ifEmpty { null },
                customerName = o.optString("customer_name").ifEmpty { null },
                note = o.optString("note", ""),
                branchId = o.optString("branch_id").ifEmpty { null },
                rentalDays = tDays,
                rentalStartMs = tStart,
                rentalEndMs = tEnd,
                depositCents = o.optLong("deposit_cents", 0L),
                eventName = o.optString("event_name", ""),
                eventVenue = o.optString("event_venue", ""),
            )
        }

        val movements = parseArray(root.optJSONArray("stock_movements")) { o ->
            StockMovementEntity(
                id = o.optString("id").ifEmpty { newId() },
                itemId = o.optString("item_id", ""),
                qty = o.optDouble("qty", 0.0),
                type = o.optString("type", "adjust"),
                createdAt = o.optLong("created_at", nowMs()),
                note = o.optString("note", ""),
                receiptId = o.optString("receipt_id").ifEmpty { null },
            )
        }

        repo.importReplace(
            settings = settingsMap,
            branches = branches,
            categories = categories,
            items = items,
            customers = customers,
            receipts = receipts,
            receiptLines = receiptLines,
            tickets = tickets,
            ticketLines = ticketLines,
            movements = movements,
        )

        BackupResult(
            items = items.size,
            categories = categories.size,
            customers = customers.size,
            branches = branches.size,
            receipts = receipts.size,
            tickets = tickets.size,
            sourceApp = root.optString("source_app", "unknown"),
        )
    }

    private inline fun <T> parseArray(arr: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                add(mapper(arr.getJSONObject(i)))
            }
        }
    }
}
