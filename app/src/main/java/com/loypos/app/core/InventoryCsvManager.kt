package com.loypos.app.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.ProductEntity
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class InventoryCsvExportResult(
    val filename: String,
    val csvContent: String,
    val itemCount: Int,
    val trackedCount: Int,
    val totalStockUnits: Double,
)

data class ParsedCsvProductRow(
    val rowNumber: Int,
    val name: String,
    val sku: String,
    val barcode: String,
    val categoryName: String,
    val priceCents: Long,
    val costCents: Long,
    val trackStock: Boolean,
    val stock: Double,
    val archived: Boolean,
    val matchedExistingId: String?,
    val isNewCategory: Boolean,
) {
    val isUpdate: Boolean get() = matchedExistingId != null
}

data class CsvRowError(
    val rowNumber: Int,
    val reason: String,
)

data class CsvImportPreview(
    val filenameLabel: String,
    val rows: List<ParsedCsvProductRow>,
    val errors: List<CsvRowError>,
) {
    val newItemsCount: Int get() = rows.count { !it.isUpdate }
    val updateItemsCount: Int get() = rows.count { it.isUpdate }
    val newCategoriesCount: Int get() = rows
        .filter { it.isNewCategory && it.categoryName.isNotEmpty() }
        .map { it.categoryName.lowercase(Locale.ROOT) }
        .distinct()
        .size
}

data class CsvImportCommitResult(
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val createdCategoriesCount: Int,
)

object InventoryCsvManager {

    private val HEADER_ALIASES = mapOf(
        "name" to setOf("name", "product", "product_name", "product name", "item", "item_name", "item name", "title"),
        "sku" to setOf("sku", "code", "item_code", "item code", "product_code", "product code"),
        "barcode" to setOf("barcode", "bar_code", "upc", "ean", "gtin"),
        "category" to setOf("category", "category_name", "category name", "group", "department"),
        "price" to setOf("price", "daily_rate", "daily rate", "rate", "rental_rate", "rental rate", "selling_price", "selling price", "unit_price", "unit price"),
        "cost" to setOf("cost", "replacement_cost", "replacement cost", "unit_cost", "unit cost", "cost_price", "cost price", "purchase_price", "purchase price"),
        "trackStock" to setOf("track_stock", "track stock", "track", "tracked", "inventory_tracked", "track inventory"),
        "stock" to setOf("stock", "qty", "quantity", "stock_quantity", "stock quantity", "on_hand", "on hand", "count"),
        "archived" to setOf("archived", "inactive", "disabled"),
    )

    fun buildInventoryCsv(
        products: List<ProductEntity>,
        categories: List<CategoryEntity>,
        includeArchived: Boolean = false,
    ): InventoryCsvExportResult {
        val catNames = categories.associate { it.id to it.name }
        val filtered = products.filter { includeArchived || !it.archived }
        val trackedCount = filtered.count { it.trackStock }
        val totalUnits = filtered.filter { it.trackStock }.sumOf { it.stock.coerceAtLeast(0.0) }

        val cal = Calendar.getInstance()
        val dateStamp = "${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}-${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
        val filename = "loypos-inventory-$dateStamp.csv"

        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine(
            csvRow(
                "Name",
                "SKU",
                "Barcode",
                "Category",
                "Price",
                "Cost",
                "Track Stock",
                "Stock",
                "Archived",
            )
        )
        for (p in filtered) {
            sb.appendLine(
                csvRow(
                    p.name,
                    p.sku,
                    p.barcode,
                    catNames[p.categoryId] ?: "",
                    formatDecimalAmount(p.priceCents),
                    formatDecimalAmount(p.costCents),
                    p.trackStock.toString(),
                    formatQty(p.stock),
                    p.archived.toString(),
                )
            )
        }

        return InventoryCsvExportResult(
            filename = filename,
            csvContent = sb.toString(),
            itemCount = filtered.size,
            trackedCount = trackedCount,
            totalStockUnits = totalUnits,
        )
    }

    fun buildTemplateCsv(): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine(csvRow("Name", "SKU", "Barcode", "Category", "Price", "Cost", "Track Stock", "Stock"))
        sb.appendLine(csvRow("10x10m Pagoda Tent", "EVT-101", "890123450101", "Tents & Canopies", "18000.00", "120000.00", "true", "6"))
        sb.appendLine(csvRow("Gold Chiavari Chair", "EVT-102", "890123450102", "Chairs & Seating", "250.00", "2200.00", "true", "200"))
        sb.appendLine(csvRow("LED Par Wash Light", "EVT-103", "", "Lighting & FX", "850.00", "5200.00", "true", "40"))
        return sb.toString()
    }

    fun writeTempCsvAndShare(
        context: Context,
        filename: String,
        csvContent: String,
        chooserTitle: String = "Export Inventory CSV",
        subject: String = "LoyPOS Inventory CSV Export",
    ): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, filename)
        file.writeText(csvContent, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
        return file
    }

    fun parseInventoryCsv(
        rawCsv: String,
        filenameLabel: String,
        existingProducts: List<ProductEntity>,
        existingCategories: List<CategoryEntity>,
    ): CsvImportPreview {
        val cleaned = rawCsv.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(cleaned)
        val parsedTable = parseCsvTable(cleaned, delimiter)
            .filter { row -> row.any { it.isNotBlank() } }

        if (parsedTable.isEmpty()) {
            return CsvImportPreview(
                filenameLabel = filenameLabel,
                rows = emptyList(),
                errors = listOf(CsvRowError(1, "CSV file is empty")),
            )
        }

        val headerRow = parsedTable.first().map { normalizeHeader(it) }
        val colIndex = resolveColumnIndices(headerRow)
        if (!colIndex.containsKey("name")) {
            return CsvImportPreview(
                filenameLabel = filenameLabel,
                rows = emptyList(),
                errors = listOf(
                    CsvRowError(
                        rowNumber = 1,
                        reason = "Missing required 'Name' column header. Expected columns like: Name, SKU, Barcode, Category, Price, Cost, Track Stock, Stock.",
                    )
                ),
            )
        }

        val existingCatNames = existingCategories.map { it.name.trim().lowercase(Locale.ROOT) }.toSet()
        val bySku = existingProducts
            .filter { it.sku.isNotBlank() }
            .associateBy { it.sku.trim().lowercase(Locale.ROOT) }
        val byBarcode = existingProducts
            .filter { it.barcode.isNotBlank() }
            .associateBy { it.barcode.trim().lowercase(Locale.ROOT) }
        val byName = existingProducts
            .associateBy { it.name.trim().lowercase(Locale.ROOT) }

        val validRows = mutableListOf<ParsedCsvProductRow>()
        val errors = mutableListOf<CsvRowError>()

        for (i in 1 until parsedTable.size) {
            val rowNumber = i + 1
            val row = parsedTable[i]
            fun cell(key: String): String {
                val idx = colIndex[key] ?: return ""
                return row.getOrNull(idx)?.trim() ?: ""
            }

            val name = cell("name")
            if (name.isEmpty()) {
                errors.add(CsvRowError(rowNumber, "Missing product name"))
                continue
            }

            val sku = cell("sku")
            val barcode = cell("barcode")
            val categoryName = cell("category")

            val priceRaw = cell("price")
            val priceCents = if (priceRaw.isEmpty()) {
                0L
            } else {
                parseMoneyInputToCents(priceRaw) ?: run {
                    errors.add(CsvRowError(rowNumber, "Invalid price '$priceRaw' for $name"))
                    continue
                }
            }

            val costRaw = cell("cost")
            val costCents = if (costRaw.isEmpty()) {
                0L
            } else {
                parseMoneyInputToCents(costRaw) ?: run {
                    errors.add(CsvRowError(rowNumber, "Invalid cost '$costRaw' for $name"))
                    continue
                }
            }

            val stockRaw = cell("stock")
            val stock = if (stockRaw.isEmpty()) {
                0.0
            } else {
                parseQty(stockRaw) ?: run {
                    errors.add(CsvRowError(rowNumber, "Invalid stock quantity '$stockRaw' for $name"))
                    continue
                }
            }

            val trackRaw = cell("trackStock")
            val trackStock = if (trackRaw.isEmpty()) {
                stockRaw.isNotEmpty()
            } else {
                parseBool(trackRaw, default = false)
            }

            val archivedRaw = cell("archived")
            val archived = parseBool(archivedRaw, default = false)

            val matched = when {
                sku.isNotEmpty() -> bySku[sku.lowercase(Locale.ROOT)]
                barcode.isNotEmpty() -> byBarcode[barcode.lowercase(Locale.ROOT)]
                else -> byName[name.lowercase(Locale.ROOT)]
            }

            val isNewCat = categoryName.isNotEmpty() && !existingCatNames.contains(categoryName.lowercase(Locale.ROOT))

            validRows.add(
                ParsedCsvProductRow(
                    rowNumber = rowNumber,
                    name = name,
                    sku = sku,
                    barcode = barcode,
                    categoryName = categoryName,
                    priceCents = priceCents,
                    costCents = costCents,
                    trackStock = trackStock,
                    stock = stock,
                    archived = archived,
                    matchedExistingId = matched?.id,
                    isNewCategory = isNewCat,
                )
            )
        }

        return CsvImportPreview(
            filenameLabel = filenameLabel,
            rows = validRows,
            errors = errors,
        )
    }

    private fun resolveColumnIndices(normalizedHeaders: List<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        normalizedHeaders.forEachIndexed { idx, h ->
            for ((canonicalKey, aliases) in HEADER_ALIASES) {
                if (h in aliases && !result.containsKey(canonicalKey)) {
                    result[canonicalKey] = idx
                }
            }
        }
        return result
    }

    private fun normalizeHeader(raw: String): String =
        raw.trim().lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ")

    private fun detectDelimiter(content: String): Char {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        var commas = 0
        var semis = 0
        var inQuotes = false
        for (ch in firstLine) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) commas++
                ';' -> if (!inQuotes) semis++
            }
        }
        return if (semis > commas && semis > 0) ';' else ','
    }

    private fun parseCsvTable(input: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    val next = input.getOrNull(i + 1)
                    if (next == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                    }
                    '\r' -> {
                        if (input.getOrNull(i + 1) == '\n') i++
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    '\n' -> {
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                    }
                    else -> cell.append(ch)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString())
            rows.add(currentRow)
        }
        return rows
    }

    private fun parseMoneyInputToCents(raw: String): Long? {
        val stripped = raw.trim().replace(Regex("""[^\d.,-]"""), "")
        if (stripped.isEmpty()) return null
        if (stripped.startsWith("-")) return null
        parseAmountToCents(stripped)?.let { return it }
        val normalized = stripped.replace(",", "")
        val d = normalized.toDoubleOrNull() ?: return null
        if (d < 0.0 || d.isNaN() || d.isInfinite()) return null
        return (d * 100.0).toLong()
    }

    private fun parseBool(raw: String, default: Boolean): Boolean {
        return when (raw.trim().lowercase(Locale.ROOT)) {
            "true", "yes", "y", "1", "on" -> true
            "false", "no", "n", "0", "off" -> false
            else -> default
        }
    }

    private fun formatDecimalAmount(cents: Long): String {
        val neg = cents < 0L
        val absVal = abs(cents)
        val whole = absVal / 100L
        val frac = (absVal % 100L).toString().padStart(2, '0')
        return "${if (neg) "-" else ""}$whole.$frac"
    }

    private fun csvRow(vararg fields: String): String =
        fields.joinToString(",") { csvEscape(it) }

    private fun csvEscape(raw: String): String {
        val isNumber = raw.toDoubleOrNull() != null
        val safe = if (!isNumber && raw.isNotEmpty() && raw[0] in charArrayOf('=', '+', '-', '@')) {
            "'$raw"
        } else {
            raw
        }
        return if (safe.contains(',') || safe.contains('"') || safe.contains('\n') || safe.contains('\r')) {
            "\"${safe.replace("\"", "\"\"")}\""
        } else {
            safe
        }
    }
}
