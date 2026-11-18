package com.loypos.app.core

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

fun parseAmountToCents(input: String): Long? {
    val cleaned = input.trim().replace(",", ".").replace(" ", "")
    if (cleaned.isEmpty()) return null
    val match = Regex("""^(\d+)(?:\.(\d{1,2}))?$""").matchEntire(cleaned) ?: return null
    val whole = match.groupValues[1].toLongOrNull() ?: return null
    val frac = match.groupValues[2]
    val fracCents = when (frac.length) {
        1 -> (frac.toLongOrNull() ?: 0L) * 10L
        2 -> frac.toLongOrNull() ?: 0L
        else -> 0L
    }
    return whole * 100L + fracCents
}

fun parseQty(input: String): Double? {
    val cleaned = input.trim().replace(",", ".")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0.0 || value.isNaN() || value.isInfinite()) return null
    return value
}

fun formatCents(cents: Long, symbol: String): String {
    val negative = cents < 0L
    val absVal = abs(cents)
    val whole = (absVal / 100L).toString()
    val withSeparators = whole.replace(Regex("""\B(?=(\d{3})+(?!\d))"""), ",")
    val frac = (absVal % 100L).toString().padStart(2, '0')
    return "${if (negative) "-" else ""}$symbol$withSeparators.$frac"
}

fun formatCentsShort(cents: Long, symbol: String): String {
    val negative = cents < 0L
    val absVal = abs(cents)
    val units = absVal / 100.0
    val body = when {
        units >= 1_000_000.0 -> "${trimShort(units / 1_000_000.0)}M"
        units >= 10_000.0 -> "${trimShort(units / 1_000.0)}k"
        absVal % 100L == 0L -> (absVal / 100L).toString()
        else -> trimShort(units)
    }
    return "${if (negative) "-" else ""}$symbol$body"
}

private fun trimShort(v: Double): String {
    var s = if (v < 100.0) String.format(Locale.US, "%.1f", v) else String.format(Locale.US, "%.0f", v)
    if (s.endsWith(".0")) s = s.substring(0, s.length - 2)
    return s
}

fun formatQty(qty: Double): String {
    val rounded = qty.roundToLong().toDouble()
    return if (qty == rounded) {
        qty.roundToLong().toString()
    } else {
        String.format(Locale.US, "%.2f", qty)
    }
}

data class DiscountInput(
    val type: String, // "percent" | "amount"
    val value: Double,
) {
    companion object {
        fun percent(value: Double) = DiscountInput("percent", value)
        fun amount(value: Double) = DiscountInput("amount", value)
    }
}

data class SaleTotals(
    val subtotalCents: Long,
    val discountCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

fun computeTotals(
    lineTotalsCents: Iterable<Long>,
    discount: DiscountInput?,
    taxRatePercent: Double,
    taxIncluded: Boolean,
): SaleTotals {
    var subtotal = 0L
    for (line in lineTotalsCents) {
        subtotal += line
    }
    var discountCents = 0L
    if (discount != null) {
        discountCents = if (discount.type == "percent") {
            (subtotal * discount.value / 100.0).roundToLong()
        } else {
            (discount.value * 100.0).roundToLong()
        }
        if (discountCents < 0L) discountCents = 0L
        if (discountCents > subtotal) discountCents = subtotal
    }
    val base = subtotal - discountCents
    var taxCents = 0L
    if (taxRatePercent > 0.0 && base > 0L) {
        val r = taxRatePercent / 100.0
        taxCents = if (taxIncluded) {
            (base - base / (1.0 + r)).roundToLong()
        } else {
            (base * r).roundToLong()
        }
    }
    val total = if (taxIncluded) base else base + taxCents
    return SaleTotals(
        subtotalCents = subtotal,
        discountCents = discountCents,
        taxCents = taxCents,
        totalCents = total,
    )
}

fun pointsForTotal(totalCents: Long): Int = (totalCents / 100L).toInt().coerceAtLeast(0)

private val monthNames = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

fun formatDateTime(ms: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ms }
    val month = monthNames[cal.get(Calendar.MONTH)]
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$month $day, $year $hour:$min"
}

fun formatTime(ms: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ms }
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$hour:$min"
}

fun formatShortDayMonth(ms: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ms }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = monthNames[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$day $month $year, $hour:$min"
}

fun formatShortDate(ms: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ms }
    val month = monthNames[cal.get(Calendar.MONTH)]
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    return "$month $day, $year"
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${String.format(Locale.US, "%.1f", kb)} KB"
    val mb = kb / 1024.0
    return "${String.format(Locale.US, "%.1f", mb)} MB"
}

fun startOfTodayMs(): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private var idCounter = 0L

fun newId(): String {
    idCounter++
    return "${System.currentTimeMillis().toString(36)}${idCounter.toString(36)}${(100..999).random()}"
}

fun nowMs(): Long = System.currentTimeMillis()
