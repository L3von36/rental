package com.loypos.app.core

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.data.local.SettingsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object RentalHistoryExporter {

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

    private fun csvRow(vararg fields: String): String =
        fields.joinToString(",") { csvEscape(it) }

    fun buildRentalHistoryCsv(
        receipts: List<ReceiptWithLines>,
        settings: SettingsModel,
    ): String {
        val sym = settings.currencySymbol
        val sb = StringBuilder()
        sb.append('\uFEFF') // UTF-8 BOM

        // Header info
        sb.appendLine(csvRow("Section", "Metric", "Value", "Currency"))
        sb.appendLine(csvRow("Header", "Store Name", settings.storeName, ""))
        sb.appendLine(csvRow("Header", "Export Date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()), ""))
        sb.appendLine(csvRow("Header", "Total Orders", receipts.size.toString(), ""))
        val completed = receipts.filter { it.receipt.isCompleted }
        val outCount = receipts.count { it.receipt.isRentalOut }
        val returnedCount = receipts.count { it.receipt.isReturned }
        val totalRevenueCents = completed.sumOf { it.receipt.totalCents }
        val totalDepositCents = completed.sumOf { it.receipt.depositCents }
        sb.appendLine(csvRow("Summary", "Active Rented Out", outCount.toString(), ""))
        sb.appendLine(csvRow("Summary", "Returned", returnedCount.toString(), ""))
        sb.appendLine(csvRow("Summary", "Total Revenue", formatDecimalAmount(totalRevenueCents), sym))
        sb.appendLine(csvRow("Summary", "Total Security Deposits", formatDecimalAmount(totalDepositCents), sym))
        sb.appendLine()

        // Transaction Ledger
        sb.appendLine(
            csvRow(
                "Rental #",
                "Date",
                "Time",
                "Status",
                "Rental Status",
                "Duration (Days)",
                "Rental Start",
                "Return Due",
                "Client Name",
                "Event Name",
                "Event Venue",
                "Dispatcher",
                "Payment Method",
                "Items Count",
                "Subtotal",
                "Discounts",
                "Tax",
                "Security Deposit",
                "Total Amount",
                "Paid Amount",
                "Currency",
                "Items Summary",
                "Notes",
            )
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

        for (item in receipts) {
            val r = item.receipt
            val startMs = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
            val endMs = if (r.rentalEndMs > 0L) r.rentalEndMs else startMs + r.rentalDays.coerceAtLeast(1) * 86_400_000L
            val itemsSummary = item.lines.joinToString("; ") { "${it.name} (x${formatQty(it.qty)})" }
            val totalQty = item.lines.sumOf { it.qty }

            sb.appendLine(
                csvRow(
                    "#${r.number.toString().padStart(4, '0')}",
                    dateFormat.format(Date(r.createdAt)),
                    timeFormat.format(Date(r.createdAt)),
                    r.status,
                    r.rentalStatus,
                    r.rentalDays.coerceAtLeast(1).toString(),
                    dateFormat.format(Date(startMs)),
                    dateFormat.format(Date(endMs)),
                    r.customerName ?: "Walk-in Client",
                    r.eventName,
                    r.eventVenue,
                    r.cashierName ?: "Operator",
                    r.paymentType.replaceFirstChar { it.uppercase() },
                    formatQty(totalQty),
                    formatDecimalAmount(r.subtotalCents),
                    formatDecimalAmount(r.discountCents),
                    formatDecimalAmount(r.taxCents),
                    formatDecimalAmount(r.depositCents),
                    formatDecimalAmount(r.totalCents),
                    formatDecimalAmount(r.paidCents),
                    sym,
                    itemsSummary,
                    r.note,
                )
            )
        }
        return sb.toString()
    }

    suspend fun writeAndShareCsv(
        context: Context,
        receipts: List<ReceiptWithLines>,
        settings: SettingsModel,
    ): File = withContext(Dispatchers.IO) {
        val csvContent = buildRentalHistoryCsv(receipts, settings)
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val file = File(dir, "rental-history-$dateStamp.csv")
        file.writeText(csvContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LoyPOS Rental History Export ($dateStamp)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Rental History CSV"))
        file
    }

    suspend fun buildRentalHistoryPdf(
        context: Context,
        receipts: List<ReceiptWithLines>,
        settings: SettingsModel,
    ): File = withContext(Dispatchers.IO) {
        val sym = settings.currencySymbol
        val doc = PdfDocument()

        val pageWidth = 595 // A4 width (pt)
        val pageHeight = 842 // A4 height (pt)
        val margin = 36f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2A4A")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2A4A")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 8.5f
            typeface = Typeface.DEFAULT
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A6B82")
            textSize = 8f
            typeface = Typeface.DEFAULT
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E0E4E9")
            strokeWidth = 0.8f
        }
        val tableHeaderBg = Paint().apply {
            color = Color.parseColor("#EDF2F7")
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = 46f

        fun checkPage(needed: Float) {
            if (y + needed > pageHeight - 50f) {
                // draw page footer
                canvas.drawText("Page $pageNum · ${settings.storeName}", margin, pageHeight - 24f, mutedPaint)
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 46f
            }
        }

        // Title and Header
        canvas.drawText("${settings.storeName.uppercase()} · RENTAL HISTORY REPORT", margin, y, titlePaint)
        y += 16f
        val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $dateStr · ${receipts.size} records", margin, y, mutedPaint)
        y += 18f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 14f

        // Accounting Summary
        val completed = receipts.filter { it.receipt.isCompleted }
        val outCount = receipts.count { it.receipt.isRentalOut }
        val retCount = receipts.count { it.receipt.isReturned }
        val overdueCount = receipts.count { it.receipt.isOverdue() }
        val totalRev = completed.sumOf { it.receipt.totalCents }
        val totalDep = completed.sumOf { it.receipt.depositCents }

        canvas.drawText("Summary:", margin, y, bodyBoldPaint)
        y += 12f
        canvas.drawText(
            "Total Orders: ${receipts.size}   |   Active Out: $outCount   |   Returned: $retCount   |   Overdue: $overdueCount",
            margin,
            y,
            bodyPaint,
        )
        y += 12f
        canvas.drawText(
            "Net Revenue: $sym${formatDecimalAmount(totalRev)}   |   Security Deposits Held: $sym${formatDecimalAmount(totalDep)}",
            margin,
            y,
            bodyBoldPaint,
        )
        y += 18f

        // Table Header
        fun drawTableHeader() {
            canvas.drawRect(margin, y - 10f, pageWidth - margin, y + 6f, tableHeaderBg)
            canvas.drawText("ORDER", margin + 4f, y, headerPaint)
            canvas.drawText("DATE", margin + 44f, y, headerPaint)
            canvas.drawText("CLIENT / EVENT", margin + 115f, y, headerPaint)
            canvas.drawText("STATUS", margin + 265f, y, headerPaint)
            canvas.drawText("DUE", margin + 345f, y, headerPaint)
            canvas.drawText("TOTAL", margin + 430f, y, headerPaint)
            canvas.drawText("DEPOSIT", margin + 490f, y, headerPaint)
            y += 14f
        }

        drawTableHeader()

        val shortDate = SimpleDateFormat("MM/dd", Locale.US)

        for (item in receipts) {
            checkPage(24f)
            val r = item.receipt
            val orderNum = "#${r.number.toString().padStart(4, '0')}"
            val dStr = shortDate.format(Date(r.createdAt))
            val clientStr = (r.customerName?.take(18) ?: "Walk-in") + if (r.eventName.isNotEmpty()) " (${r.eventName.take(12)})" else ""
            val statusStr = when {
                !r.isCompleted -> "Refunded"
                r.isReturned -> "Returned"
                r.isOverdue() -> "OVERDUE"
                else -> "Out (${r.rentalDays}d)"
            }
            val dueStr = shortDate.format(Date(if (r.rentalEndMs > 0L) r.rentalEndMs else r.createdAt + r.rentalDays * 86_400_000L))
            val totalStr = "$sym${formatDecimalAmount(r.totalCents)}"
            val depStr = if (r.depositCents > 0L) "$sym${formatDecimalAmount(r.depositCents)}" else "-"

            canvas.drawText(orderNum, margin + 4f, y, bodyBoldPaint)
            canvas.drawText(dStr, margin + 44f, y, mutedPaint)
            canvas.drawText(clientStr, margin + 115f, y, bodyPaint)
            canvas.drawText(statusStr, margin + 265f, y, if (r.isOverdue()) bodyBoldPaint else bodyPaint)
            canvas.drawText(dueStr, margin + 345f, y, mutedPaint)
            canvas.drawText(totalStr, margin + 430f, y, bodyBoldPaint)
            canvas.drawText(depStr, margin + 490f, y, bodyPaint)

            y += 14f
            canvas.drawLine(margin, y - 3f, pageWidth - margin, y - 3f, dividerPaint)
            y += 4f
        }

        canvas.drawText("Page $pageNum · ${settings.storeName}", margin, pageHeight - 24f, mutedPaint)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val file = File(dir, "rental-history-$dateStamp.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        file
    }

    suspend fun writeAndSharePdf(
        context: Context,
        receipts: List<ReceiptWithLines>,
        settings: SettingsModel,
    ): File = withContext(Dispatchers.IO) {
        val file = buildRentalHistoryPdf(context, receipts, settings)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LoyPOS Rental History Audit Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Rental History PDF"))
        file
    }

    private fun formatDecimalAmount(cents: Long): String {
        val neg = cents < 0L
        val absVal = abs(cents)
        val whole = absVal / 100L
        val frac = (absVal % 100L).toString().padStart(2, '0')
        return "${if (neg) "-" else ""}$whole.$frac"
    }
}
