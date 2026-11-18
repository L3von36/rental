package com.loypos.app.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.data.local.SaleWithItems
import com.loypos.app.data.local.SettingsModel
import java.io.File
import java.util.Calendar
import kotlin.math.abs

enum class SalesReportPeriod {
    DAILY,
    MONTHLY,
}

data class SalesCsvReportResult(
    val period: SalesReportPeriod,
    val periodLabel: String,
    val filename: String,
    val csvContent: String,
    val completedCount: Int,
    val refundedCount: Int,
    val netTotalCents: Long,
)

object SalesCsvExporter {

    private val monthShort = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun dateRangeForDaily(year: Int, monthZeroBased: Int, dayOfMonth: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun dateRangeForMonthly(year: Int, monthZeroBased: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(year, monthZeroBased, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun buildCsvReport(
        period: SalesReportPeriod,
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int,
        sales: List<SaleWithItems>,
        branches: List<BranchEntity>,
        settings: SettingsModel,
        branchFilterId: String?,
    ): SalesCsvReportResult {
        val branchNames = branches.associate { it.id to it.name }
        val currency = settings.currencySymbol
        val filtered = sales.filter { swl ->
            branchFilterId == null || swl.receipt.branchId == null || swl.receipt.branchId == branchFilterId
        }
        val completed = filtered.filter { it.receipt.isCompleted }
        val refunded = filtered.filter { !it.receipt.isCompleted }

        val grossCents = completed.sumOf { it.receipt.subtotalCents }
        val discountCents = completed.sumOf { it.receipt.discountCents }
        val taxCents = completed.sumOf { it.receipt.taxCents }
        val netCents = completed.sumOf { it.receipt.totalCents }
        val cashCents = completed.filter { it.receipt.paymentType != "card" }.sumOf { it.receipt.totalCents }
        val cardCents = completed.filter { it.receipt.paymentType == "card" }.sumOf { it.receipt.totalCents }

        val monthTwoDigit = (monthZeroBased + 1).toString().padStart(2, '0')
        val dayTwoDigit = dayOfMonth.toString().padStart(2, '0')

        val periodLabel: String
        val filename: String
        when (period) {
            SalesReportPeriod.DAILY -> {
                periodLabel = "$year-$monthTwoDigit-$dayTwoDigit"
                filename = "loypos-daily-sales-$periodLabel.csv"
            }
            SalesReportPeriod.MONTHLY -> {
                periodLabel = "${monthShort[monthZeroBased.coerceIn(0, 11)]} $year"
                filename = "loypos-monthly-sales-$year-$monthTwoDigit.csv"
            }
        }

        val branchLabel = if (branchFilterId == null) {
            "All branches"
        } else {
            branchNames[branchFilterId] ?: "Selected branch"
        }

        val sb = StringBuilder()
        // UTF-8 BOM for clean spreadsheet import (Excel / Google Sheets / accounting software)
        sb.append('\uFEFF')

        // 1. Accounting Summary Header
        sb.appendLine(csvRow("Section", "Metric", "Value", "Currency"))
        sb.appendLine(csvRow("Summary", "Store Name", settings.storeName, ""))
        sb.appendLine(csvRow("Summary", "Branch Scope", branchLabel, ""))
        sb.appendLine(
            csvRow(
                "Summary",
                "Report Type",
                if (period == SalesReportPeriod.DAILY) "Daily Sales Report" else "Monthly Sales Report",
                "",
            )
        )
        sb.appendLine(csvRow("Summary", "Period", periodLabel, ""))
        sb.appendLine(csvRow("Summary", "Completed Sales Count", completed.size.toString(), ""))
        sb.appendLine(csvRow("Summary", "Refunded Sales Count", refunded.size.toString(), ""))
        sb.appendLine(csvRow("Summary", "Gross Subtotal", formatDecimalAmount(grossCents), currency))
        sb.appendLine(csvRow("Summary", "Total Discounts", formatDecimalAmount(discountCents), currency))
        sb.appendLine(csvRow("Summary", "Total Tax", formatDecimalAmount(taxCents), currency))
        sb.appendLine(csvRow("Summary", "Cash Revenue", formatDecimalAmount(cashCents), currency))
        sb.appendLine(csvRow("Summary", "Card Revenue", formatDecimalAmount(cardCents), currency))
        sb.appendLine(csvRow("Summary", "Net Revenue", formatDecimalAmount(netCents), currency))
        sb.appendLine()

        // 2. Daily Breakdown (for Monthly Reports)
        if (period == SalesReportPeriod.MONTHLY) {
            sb.appendLine(
                csvRow(
                    "Date",
                    "Completed Orders",
                    "Refunded Orders",
                    "Gross Subtotal",
                    "Discounts",
                    "Tax",
                    "Cash Revenue",
                    "Card Revenue",
                    "Net Revenue",
                    "Currency",
                )
            )
            val cal = Calendar.getInstance()
            val monthCal = Calendar.getInstance().apply {
                set(year, monthZeroBased, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val byDay = filtered.groupBy { swl ->
                cal.timeInMillis = swl.receipt.createdAt
                cal.get(Calendar.DAY_OF_MONTH)
            }
            for (d in 1..daysInMonth) {
                val daySales = byDay[d].orEmpty()
                val dayCompleted = daySales.filter { it.receipt.isCompleted }
                val dayRefunded = daySales.filter { !it.receipt.isCompleted }
                val dGross = dayCompleted.sumOf { it.receipt.subtotalCents }
                val dDisc = dayCompleted.sumOf { it.receipt.discountCents }
                val dTax = dayCompleted.sumOf { it.receipt.taxCents }
                val dCash = dayCompleted.filter { it.receipt.paymentType != "card" }.sumOf { it.receipt.totalCents }
                val dCard = dayCompleted.filter { it.receipt.paymentType == "card" }.sumOf { it.receipt.totalCents }
                val dNet = dayCompleted.sumOf { it.receipt.totalCents }
                val dayStr = "$year-$monthTwoDigit-${d.toString().padStart(2, '0')}"
                sb.appendLine(
                    csvRow(
                        dayStr,
                        dayCompleted.size.toString(),
                        dayRefunded.size.toString(),
                        formatDecimalAmount(dGross),
                        formatDecimalAmount(dDisc),
                        formatDecimalAmount(dTax),
                        formatDecimalAmount(dCash),
                        formatDecimalAmount(dCard),
                        formatDecimalAmount(dNet),
                        currency,
                    )
                )
            }
            sb.appendLine()
        }

        // 3. Transaction Ledger (Rental orders + item totals)
        sb.appendLine(
            csvRow(
                "Rental #",
                "Date",
                "Time",
                "Status",
                "Rental Status",
                "Days",
                "Return Due",
                "Event",
                "Venue",
                "Branch",
                "Dispatcher",
                "Client",
                "Payment Type",
                "Items Qty",
                "Subtotal",
                "Discount",
                "Tax",
                "Deposit",
                "Net Total",
                "Paid",
                "Change",
                "Currency",
                "Note",
            )
        )
        val cal = Calendar.getInstance()
        for (swl in filtered) {
            val s = swl.receipt
            cal.timeInMillis = s.createdAt
            val rowDate = "${cal.get(Calendar.YEAR)}-${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}-${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}"
            val rowTime = "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}:${cal.get(Calendar.SECOND).toString().padStart(2, '0')}"
            val qtySum = swl.lines.sumOf { it.qty }
            val endMs = if (s.rentalEndMs > 0L) s.rentalEndMs else s.createdAt + s.rentalDays.coerceAtLeast(1) * 86_400_000L
            sb.appendLine(
                csvRow(
                    "#${s.number.toString().padStart(4, '0')}",
                    rowDate,
                    rowTime,
                    s.status,
                    s.rentalStatus,
                    s.rentalDays.coerceAtLeast(1).toString(),
                    formatShortDate(endMs),
                    s.eventName,
                    s.eventVenue,
                    branchNames[s.branchId] ?: "",
                    s.cashierName ?: "",
                    s.customerName ?: "",
                    s.paymentType,
                    formatQty(qtySum),
                    formatDecimalAmount(s.subtotalCents),
                    formatDecimalAmount(s.discountCents),
                    formatDecimalAmount(s.taxCents),
                    formatDecimalAmount(s.depositCents),
                    formatDecimalAmount(s.totalCents),
                    formatDecimalAmount(s.paidCents),
                    formatDecimalAmount(s.changeCents),
                    currency,
                    s.note,
                )
            )
        }

        return SalesCsvReportResult(
            period = period,
            periodLabel = periodLabel,
            filename = filename,
            csvContent = sb.toString(),
            completedCount = completed.size,
            refundedCount = refunded.size,
            netTotalCents = netCents,
        )
    }

    fun writeTempCsvAndShare(context: Context, result: SalesCsvReportResult): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, result.filename)
        file.writeText(result.csvContent, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "LoyPOS ${if (result.period == SalesReportPeriod.DAILY) "Daily" else "Monthly"} Sales Report (${result.periodLabel})",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Sales Report CSV"))
        return file
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
