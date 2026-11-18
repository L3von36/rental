package com.loypos.app.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.data.local.SettingsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar

enum class ReceiptPaperFormat(val label: String) {
    THERMAL_80MM("80mm Thermal"),
    A4_STANDARD("A4 Document"),
}

data class ReceiptPdfPreviewResult(
    val file: File,
    val format: ReceiptPaperFormat,
    val pageCount: Int,
    val previewBitmaps: List<Bitmap>,
)

object ReceiptPdfGenerator {
    // 80mm thermal roll width at 72dpi (~226pt)
    private const val THERMAL_WIDTH = 226
    private const val THERMAL_MARGIN = 12f

    // A4 portrait at 72dpi (595 x 842 pt)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val A4_MARGIN = 40f

    suspend fun generateReceiptPreview(
        context: Context,
        receiptWithLines: ReceiptWithLines,
        settings: SettingsModel,
        branchName: String?,
        branchAddress: String?,
        format: ReceiptPaperFormat = ReceiptPaperFormat.THERMAL_80MM,
    ): ReceiptPdfPreviewResult = withContext(Dispatchers.IO) {
        val file = createReceiptPdf(
            context = context,
            receiptWithLines = receiptWithLines,
            settings = settings,
            branchName = branchName,
            branchAddress = branchAddress,
            format = format,
        )
        val bitmaps = renderPdfPages(file, targetWidthPx = 900)
        ReceiptPdfPreviewResult(
            file = file,
            format = format,
            pageCount = bitmaps.size.coerceAtLeast(1),
            previewBitmaps = bitmaps,
        )
    }

    suspend fun createReceiptPdf(
        context: Context,
        receiptWithLines: ReceiptWithLines,
        settings: SettingsModel,
        branchName: String?,
        branchAddress: String?,
        format: ReceiptPaperFormat = ReceiptPaperFormat.THERMAL_80MM,
    ): File = withContext(Dispatchers.IO) {
        val r = receiptWithLines.receipt
        val doc = when (format) {
            ReceiptPaperFormat.THERMAL_80MM -> buildThermalDocument(receiptWithLines, settings, branchName, branchAddress)
            ReceiptPaperFormat.A4_STANDARD -> buildA4Document(receiptWithLines, settings, branchName, branchAddress)
        }

        val dir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
        val suffix = if (format == ReceiptPaperFormat.A4_STANDARD) "-a4" else ""
        val file = File(dir, "receipt-${r.number.toString().padStart(4, '0')}$suffix.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }

    private fun buildThermalDocument(
        receiptWithLines: ReceiptWithLines,
        settings: SettingsModel,
        branchName: String?,
        branchAddress: String?,
    ): PdfDocument {
        val r = receiptWithLines.receipt
        val lines = receiptWithLines.lines
        val sym = settings.currencySymbol

        val wrappedNames = lines.map { wrapText(it.name, 28) }
        val totalWrappedLines = wrappedNames.sumOf { it.size }
        val estHeight = (290 + totalWrappedLines * 11 + lines.size * 15 +
            (if (!r.cashierName.isNullOrEmpty()) 14 else 0) +
            (if (!r.customerName.isNullOrEmpty()) 14 else 0) +
            (if (r.eventName.isNotEmpty()) 14 else 0) +
            (if (r.eventVenue.isNotEmpty()) 14 else 0) +
            (if (r.depositCents > 0L) 16 else 0) +
            (if (r.discountCents > 0L) 16 else 0) +
            (if (r.taxCents > 0L) 16 else 0) +
            (if (r.changeCents > 0L) 16 else 0) +
            (if (r.note.isNotEmpty()) 24 else 0) +
            26).coerceAtLeast(340)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_WIDTH, estHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.DEFAULT
        }
        val semiBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 7.5f
            typeface = Typeface.DEFAULT
        }
        val dashedPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f)
        }

        var y = 20f
        fun drawCentered(text: String, paint: Paint) {
            val w = paint.measureText(text)
            canvas.drawText(text, ((THERMAL_WIDTH - w) / 2f).coerceAtLeast(THERMAL_MARGIN), y, paint)
            y += paint.textSize + 4f
        }
        fun drawRow(left: String, right: String, paint: Paint = regularPaint) {
            val rw = paint.measureText(right)
            canvas.drawText(left, THERMAL_MARGIN, y, paint)
            canvas.drawText(right, THERMAL_WIDTH - THERMAL_MARGIN - rw, y, paint)
            y += paint.textSize + 5f
        }
        fun divider() {
            y += 2f
            canvas.drawLine(THERMAL_MARGIN, y, THERMAL_WIDTH - THERMAL_MARGIN, y, dashedPaint)
            y += 10f
        }

        drawCentered(settings.storeName.uppercase(), boldPaint)
        if (!branchName.isNullOrEmpty()) drawCentered(branchName, mutedPaint)
        if (!branchAddress.isNullOrEmpty()) drawCentered(branchAddress, mutedPaint)
        divider()

        val dateStr = formatShortPdfDate(r.createdAt)
        drawRow("Rental #${r.number.toString().padStart(4, '0')}", dateStr, semiBoldPaint)
        val days = r.rentalDays.coerceAtLeast(1)
        val startMs = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
        val endMs = if (r.rentalEndMs > 0L) r.rentalEndMs else startMs + days * 86_400_000L
        drawRow("Duration: $days ${if (days == 1) "day" else "days"}", "Due ${formatShortDate(endMs)}", mutedPaint)
        if (!r.cashierName.isNullOrEmpty()) {
            canvas.drawText("Dispatcher: ${r.cashierName}", THERMAL_MARGIN, y, mutedPaint)
            y += 12f
        }
        if (!r.customerName.isNullOrEmpty()) {
            canvas.drawText("Client: ${r.customerName}", THERMAL_MARGIN, y, mutedPaint)
            y += 12f
        }
        if (r.eventName.isNotEmpty()) {
            canvas.drawText("Event: ${r.eventName.take(34)}", THERMAL_MARGIN, y, mutedPaint)
            y += 12f
        }
        if (r.eventVenue.isNotEmpty()) {
            canvas.drawText("Venue: ${r.eventVenue.take(34)}", THERMAL_MARGIN, y, mutedPaint)
            y += 12f
        }
        divider()

        lines.forEachIndexed { idx, l ->
            wrappedNames[idx].forEach { nameLine ->
                canvas.drawText(nameLine, THERMAL_MARGIN, y, semiBoldPaint)
                y += 11f
            }
            drawRow("${formatQty(l.qty)} x ${formatCents(l.priceCents, sym)}/d × ${days}d", formatCents(l.lineTotalCents, sym))
        }
        divider()

        drawRow("Subtotal", formatCents(r.subtotalCents, sym))
        if (r.discountCents > 0L) {
            drawRow(r.discountLabel(), "-${formatCents(r.discountCents, sym)}")
        }
        if (r.taxCents > 0L) {
            drawRow("Tax", formatCents(r.taxCents, sym))
        }
        drawRow("RENTAL TOTAL", formatCents(r.totalCents, sym), boldPaint)
        if (r.depositCents > 0L) {
            drawRow("Security Deposit", formatCents(r.depositCents, sym))
        }
        drawRow(if (r.paymentType == "card") "Paid by card" else "Paid in cash", formatCents(r.paidCents, sym))
        if (r.changeCents > 0L) {
            drawRow("Change", formatCents(r.changeCents, sym))
        }
        if (r.note.isNotEmpty()) {
            y += 2f
            canvas.drawText("Note: ${r.note.take(36)}", THERMAL_MARGIN, y, mutedPaint)
            y += 12f
        }
        when {
            !r.isCompleted -> {
                val refundPaint = Paint(semiBoldPaint).apply { color = Color.RED }
                drawCentered("*** REFUNDED ***", refundPaint)
            }
            r.isReturned -> {
                val retPaint = Paint(semiBoldPaint).apply { color = Color.parseColor("#1B5E20") }
                drawCentered("EQUIPMENT RETURNED", retPaint)
            }
            else -> {
                drawCentered("EQUIPMENT OUT · DUE ${formatShortDate(endMs).uppercase()}", mutedPaint)
            }
        }
        divider()
        wrapText(settings.receiptFooter, 36).forEach { footerLine ->
            drawCentered(footerLine, mutedPaint)
        }
        drawCentered("Printed by LoyPOS Event Rental", mutedPaint)

        doc.finishPage(page)
        return doc
    }

    private fun buildA4Document(
        receiptWithLines: ReceiptWithLines,
        settings: SettingsModel,
        branchName: String?,
        branchAddress: String?,
    ): PdfDocument {
        val r = receiptWithLines.receipt
        val lines = receiptWithLines.lines
        val sym = settings.currencySymbol
        val doc = PdfDocument()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#162436")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#162436")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 10.5f
            typeface = Typeface.DEFAULT
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1B1F")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#F1F3F5")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }
        val accentPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            strokeWidth = 3f
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = 48f

        fun newPageIfNeeded(neededHeight: Float) {
            if (y + neededHeight > A4_HEIGHT - 60f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        // Top Accent Bar & Header
        canvas.drawLine(A4_MARGIN, 32f, A4_WIDTH - A4_MARGIN, 32f, accentPaint)
        canvas.drawText(settings.storeName, A4_MARGIN, y + 6f, titlePaint)
        val receiptTag = "RENTAL #${r.number.toString().padStart(4, '0')}"
        val tagW = sectionPaint.measureText(receiptTag)
        canvas.drawText(receiptTag, A4_WIDTH - A4_MARGIN - tagW, y + 4f, sectionPaint)
        y += 22f

        if (!branchName.isNullOrEmpty()) {
            canvas.drawText(branchName, A4_MARGIN, y, mutedPaint)
            y += 14f
        }
        if (!branchAddress.isNullOrEmpty()) {
            canvas.drawText(branchAddress, A4_MARGIN, y, mutedPaint)
            y += 14f
        }

        y += 8f
        canvas.drawLine(A4_MARGIN, y, A4_WIDTH - A4_MARGIN, y, dividerPaint)
        y += 18f

        // Metadata Grid
        val days = r.rentalDays.coerceAtLeast(1)
        val startMs = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
        val endMs = if (r.rentalEndMs > 0L) r.rentalEndMs else startMs + days * 86_400_000L
        canvas.drawText("Date: ${formatDateTime(r.createdAt)}", A4_MARGIN, y, bodyPaint)
        val statusText = when {
            !r.isCompleted -> "Status: REFUNDED"
            r.isReturned -> "Status: RETURNED"
            else -> "Status: EQUIPMENT OUT"
        }
        val statusPaint = when {
            !r.isCompleted -> Paint(bodyBoldPaint).apply { color = Color.RED }
            r.isReturned -> Paint(bodyBoldPaint).apply { color = Color.parseColor("#1B5E20") }
            else -> bodyBoldPaint
        }
        val statusW = statusPaint.measureText(statusText)
        canvas.drawText(statusText, A4_WIDTH - A4_MARGIN - statusW, y, statusPaint)
        y += 16f

        canvas.drawText(
            "Rental Term: $days ${if (days == 1) "day" else "days"} (${formatShortDate(startMs)} → ${formatShortDate(endMs)})",
            A4_MARGIN,
            y,
            bodyBoldPaint,
        )
        y += 15f

        if (!r.cashierName.isNullOrEmpty()) {
            canvas.drawText("Dispatcher: ${r.cashierName}", A4_MARGIN, y, mutedPaint)
            y += 14f
        }
        if (!r.customerName.isNullOrEmpty()) {
            canvas.drawText("Client: ${r.customerName}", A4_MARGIN, y, mutedPaint)
            y += 14f
        }
        if (r.eventName.isNotEmpty()) {
            canvas.drawText("Event: ${r.eventName}", A4_MARGIN, y, mutedPaint)
            y += 14f
        }
        if (r.eventVenue.isNotEmpty()) {
            canvas.drawText("Venue: ${r.eventVenue}", A4_MARGIN, y, mutedPaint)
            y += 14f
        }
        y += 10f

        // Table Header
        canvas.drawRect(A4_MARGIN, y - 14f, A4_WIDTH - A4_MARGIN, y + 8f, headerBgPaint)
        canvas.drawText("EQUIPMENT", A4_MARGIN + 8f, y, sectionPaint)
        val qtyHeader = "QTY × DAYS"
        val unitHeader = "RATE/DAY"
        val totalHeader = "AMOUNT"
        canvas.drawText(qtyHeader, 360f - sectionPaint.measureText(qtyHeader), y, sectionPaint)
        canvas.drawText(unitHeader, 450f - sectionPaint.measureText(unitHeader), y, sectionPaint)
        canvas.drawText(totalHeader, A4_WIDTH - A4_MARGIN - 8f - sectionPaint.measureText(totalHeader), y, sectionPaint)
        y += 22f

        // Line Items
        for (l in lines) {
            newPageIfNeeded(28f)
            val wrapped = wrapText(l.name, 40)
            wrapped.forEachIndexed { idx, part ->
                canvas.drawText(part, A4_MARGIN + 8f, y, bodyPaint)
                if (idx == 0) {
                    val qtyStr = "${formatQty(l.qty)} × ${days}d"
                    val unitStr = formatCents(l.priceCents, sym)
                    val lineStr = formatCents(l.lineTotalCents, sym)
                    canvas.drawText(qtyStr, 360f - bodyPaint.measureText(qtyStr), y, bodyPaint)
                    canvas.drawText(unitStr, 450f - bodyPaint.measureText(unitStr), y, bodyPaint)
                    canvas.drawText(lineStr, A4_WIDTH - A4_MARGIN - 8f - bodyBoldPaint.measureText(lineStr), y, bodyBoldPaint)
                }
                y += 15f
            }
            canvas.drawLine(A4_MARGIN, y - 5f, A4_WIDTH - A4_MARGIN, y - 5f, dividerPaint)
            y += 8f
        }

        newPageIfNeeded(145f)
        y += 6f

        fun drawSummaryRow(label: String, value: String, paint: Paint = bodyPaint) {
            canvas.drawText(label, 340f, y, paint)
            val vw = paint.measureText(value)
            canvas.drawText(value, A4_WIDTH - A4_MARGIN - 8f - vw, y, paint)
            y += paint.textSize + 7f
        }

        drawSummaryRow("Subtotal", formatCents(r.subtotalCents, sym))
        if (r.discountCents > 0L) {
            drawSummaryRow(r.discountLabel(), "-${formatCents(r.discountCents, sym)}")
        }
        if (r.taxCents > 0L) {
            drawSummaryRow("Tax", formatCents(r.taxCents, sym))
        }
        y += 4f
        canvas.drawLine(335f, y - 10f, A4_WIDTH - A4_MARGIN, y - 10f, dividerPaint)
        drawSummaryRow("RENTAL TOTAL", formatCents(r.totalCents, sym), totalPaint)
        if (r.depositCents > 0L) {
            drawSummaryRow("Security Deposit", formatCents(r.depositCents, sym), bodyBoldPaint)
        }
        drawSummaryRow(
            if (r.paymentType == "card") "Paid (Card)" else "Paid (Cash)",
            formatCents(r.paidCents, sym),
            bodyPaint,
        )
        if (r.changeCents > 0L) {
            drawSummaryRow("Change", formatCents(r.changeCents, sym), bodyPaint)
        }

        if (r.note.isNotEmpty()) {
            y += 8f
            canvas.drawText("Note: ${r.note}", A4_MARGIN, y, mutedPaint)
            y += 16f
        }

        y += 18f
        canvas.drawLine(A4_MARGIN, y, A4_WIDTH - A4_MARGIN, y, dividerPaint)
        y += 18f
        val footer = settings.receiptFooter
        val fw = mutedPaint.measureText(footer)
        canvas.drawText(footer, ((A4_WIDTH - fw) / 2f).coerceAtLeast(A4_MARGIN), y, mutedPaint)
        y += 14f
        val brand = "Generated by LoyPOS Event Rental"
        val bw = mutedPaint.measureText(brand)
        canvas.drawText(brand, ((A4_WIDTH - bw) / 2f).coerceAtLeast(A4_MARGIN), y, mutedPaint)

        doc.finishPage(page)
        return doc
    }

    private fun renderPdfPages(file: File, targetWidthPx: Int): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1).toFloat()
                        val w = targetWidthPx.coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bmp)
                    }
                }
            }
        }
        return bitmaps
    }

    fun printPdfFile(context: Context, file: File, jobName: String) {
        val activity = context.findActivity() ?: context
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Print service is unavailable on this device")

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback,
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                    } else {
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt PDF"))
    }

    fun shareReceiptText(
        context: Context,
        receiptWithLines: ReceiptWithLines,
        settings: SettingsModel,
        branchName: String?,
    ) {
        val r = receiptWithLines.receipt
        val sym = settings.currencySymbol
        val days = r.rentalDays.coerceAtLeast(1)
        val startMs = if (r.rentalStartMs > 0L) r.rentalStartMs else r.createdAt
        val endMs = if (r.rentalEndMs > 0L) r.rentalEndMs else startMs + days * 86_400_000L
        val sb = StringBuilder()
        sb.appendLine(settings.storeName)
        if (!branchName.isNullOrEmpty()) sb.appendLine(branchName)
        sb.appendLine("Rental #${r.number.toString().padStart(4, '0')} · ${formatDateTime(r.createdAt)}")
        sb.appendLine("Period: $days ${if (days == 1) "day" else "days"} (Due ${formatShortDate(endMs)})")
        if (!r.customerName.isNullOrEmpty()) sb.appendLine("Client: ${r.customerName}")
        if (r.eventName.isNotEmpty()) sb.appendLine("Event: ${r.eventName}")
        if (r.eventVenue.isNotEmpty()) sb.appendLine("Venue: ${r.eventVenue}")
        sb.appendLine("--------------------------------")
        for (l in receiptWithLines.lines) {
            sb.appendLine("${l.name} (${formatQty(l.qty)} × ${formatCents(l.priceCents, sym)}/d × ${days}d) = ${formatCents(l.lineTotalCents, sym)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Subtotal: ${formatCents(r.subtotalCents, sym)}")
        if (r.discountCents > 0L) sb.appendLine("${r.discountLabel()}: -${formatCents(r.discountCents, sym)}")
        if (r.taxCents > 0L) sb.appendLine("Tax: ${formatCents(r.taxCents, sym)}")
        sb.appendLine("RENTAL TOTAL: ${formatCents(r.totalCents, sym)}")
        if (r.depositCents > 0L) sb.appendLine("Security Deposit: ${formatCents(r.depositCents, sym)}")
        sb.appendLine("${if (r.paymentType == "card") "Card" else "Cash"}: ${formatCents(r.paidCents, sym)}")
        if (r.changeCents > 0L) sb.appendLine("Change: ${formatCents(r.changeCents, sym)}")
        when {
            !r.isCompleted -> sb.appendLine("*** REFUNDED ***")
            r.isReturned -> sb.appendLine("Status: EQUIPMENT RETURNED")
            else -> sb.appendLine("Status: EQUIPMENT OUT")
        }
        sb.appendLine(settings.receiptFooter)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rental #${r.number.toString().padStart(4, '0')}")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Share Rental Receipt"))
    }

    private fun formatShortPdfDate(ms: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ms }
        return "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)} " +
            "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val clean = text.trim()
        if (clean.isEmpty()) return emptyList()
        val words = clean.split(Regex("""\s+"""))
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isEmpty()) {
                if (word.length <= maxChars) {
                    current.append(word)
                } else {
                    var idx = 0
                    while (idx < word.length) {
                        lines.add(word.substring(idx, (idx + maxChars).coerceAtMost(word.length)))
                        idx += maxChars
                    }
                }
            } else if (current.length + 1 + word.length <= maxChars) {
                current.append(' ').append(word)
            } else {
                lines.add(current.toString())
                current.setLength(0)
                if (word.length <= maxChars) {
                    current.append(word)
                } else {
                    var idx = 0
                    while (idx < word.length) {
                        lines.add(word.substring(idx, (idx + maxChars).coerceAtMost(word.length)))
                        idx += maxChars
                    }
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
