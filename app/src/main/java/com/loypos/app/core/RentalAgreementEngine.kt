package com.loypos.app.core

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.loypos.app.data.local.AgreementTemplateEntity
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.data.local.SettingsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AgreementPlaceholder(
    val token: String,
    val label: String,
    val description: String,
)

object RentalAgreementEngine {
    val placeholders: List<AgreementPlaceholder> = listOf(
        AgreementPlaceholder("{{store_name}}", "Company Name", "Your rental business name"),
        AgreementPlaceholder("{{order_number}}", "Order #", "Formatted rental order number"),
        AgreementPlaceholder("{{order_date}}", "Booking Date", "Date and time order was placed"),
        AgreementPlaceholder("{{client_name}}", "Client Name", "Renter or organizer name"),
        AgreementPlaceholder("{{client_phone}}", "Client Phone", "Client phone number"),
        AgreementPlaceholder("{{client_email}}", "Client Email", "Client email address"),
        AgreementPlaceholder("{{event_name}}", "Event Name", "Event title or occasion"),
        AgreementPlaceholder("{{event_venue}}", "Event Venue", "Delivery or venue location"),
        AgreementPlaceholder("{{pickup_date}}", "Pickup / Start", "Rental start date"),
        AgreementPlaceholder("{{return_date}}", "Return Due", "Scheduled equipment return date"),
        AgreementPlaceholder("{{rental_days}}", "Duration", "Number of rental days"),
        AgreementPlaceholder("{{equipment_list}}", "Equipment List", "Itemized rented gear, quantities & totals"),
        AgreementPlaceholder("{{subtotal}}", "Subtotal", "Rental subtotal before discount/tax"),
        AgreementPlaceholder("{{discount}}", "Discount", "Applied discount amount"),
        AgreementPlaceholder("{{tax}}", "Tax", "Tax amount"),
        AgreementPlaceholder("{{deposit}}", "Security Deposit", "Refundable damage/security deposit"),
        AgreementPlaceholder("{{total_cost}}", "Total Cost", "Final rental total due"),
        AgreementPlaceholder("{{payment_method}}", "Payment Method", "Cash or Card"),
        AgreementPlaceholder("{{order_notes}}", "Order Notes", "Special instructions or delivery notes"),
    )

    fun defaultTemplates(now: Long): List<AgreementTemplateEntity> = listOf(
        AgreementTemplateEntity(
            id = "tpl-standard-event",
            title = "Standard Event Equipment Rental Agreement",
            category = "General Event",
            isDefault = true,
            createdAt = now,
            updatedAt = now,
            body = """
DIGITAL RENTAL AGREEMENT
{{store_name}}

Agreement Reference: #{{order_number}}
Date Issued: {{order_date}}

1. PARTIES & EVENT DETAILS
Provider: {{store_name}}
Renter / Client: {{client_name}}
Phone: {{client_phone}} | Email: {{client_email}}
Event: {{event_name}}
Venue / Delivery Address: {{event_venue}}

2. RENTAL SCHEDULE
Dispatch / Pickup Date: {{pickup_date}}
Scheduled Return Date: {{return_date}}
Duration: {{rental_days}} day(s)

3. RENTED EQUIPMENT SCHEDULE
{{equipment_list}}

4. FEES, DEPOSIT & PAYMENT
Rental Subtotal: {{subtotal}}
Discount Applied: {{discount}}
Tax: {{tax}}
Total Rental Charge: {{total_cost}}
Refundable Security Deposit: {{deposit}}
Payment Method: {{payment_method}}
Notes: {{order_notes}}

5. TERMS & CONDITIONS
a) Care & Inspection: All equipment is inspected prior to check-out via QR asset tracking and confirmed in working condition. Renter agrees to protect items from weather damage, unauthorized sub-leasing, or misuse.
b) Late Returns: Equipment not checked in via QR scan by {{return_date}} incurs a prorated daily extension fee equal to the standard daily rental rate per unreturned item.
c) Loss & Damage: Security deposit ({{deposit}}) will be refunded upon full QR check-in inspection. Missing cables, broken frames, or damaged fixtures will be billed at repair or replacement cost.

AUTHORIZED SIGNATURES

Client Signature: ___________________________    Date: _______________

For {{store_name}}: ______________________    Date: _______________
            """.trimIndent(),
        ),
        AgreementTemplateEntity(
            id = "tpl-wedding-marquee",
            title = "Wedding Marquee, Tents & Decor Contract",
            category = "Wedding / Gala",
            isDefault = false,
            createdAt = now,
            updatedAt = now,
            body = """
EVENT TENTING, SEATING & DECOR RENTAL CONTRACT
Provider: {{store_name}} | Order #{{order_number}}

CLIENT & VENUE
Client / Couple / Planner: {{client_name}} ({{client_phone}}, {{client_email}})
Celebration: {{event_name}}
Venue Location: {{event_venue}}
Load-In Date: {{pickup_date}} | Strike & Return Date: {{return_date}} ({{rental_days}} days)

RESERVED STRUCTURES & DECOR INVENTORY
{{equipment_list}}

FINANCIAL SUMMARY
Subtotal: {{subtotal}} | Discount: {{discount}} | Tax: {{tax}}
Total Contract Amount: {{total_cost}}
Refundable Security Deposit: {{deposit}}
Settled Via: {{payment_method}}

SITE ACCESS & WEATHER CLAUSE
1. Renter guarantees clear vehicular access to {{event_venue}} for tent rigging and de-rigging crew.
2. Underground utilities must be marked before tent staking; weighted ballasts are required on paved plazas.
3. All linens, chairs, tables, and canopy poles must be packed dry and accounted for on final QR check-in.

Additional Instructions: {{order_notes}}

Accepted By ({{client_name}}): _______________________
Representative ({{store_name}}): _______________________
            """.trimIndent(),
        ),
        AgreementTemplateEntity(
            id = "tpl-av-production",
            title = "Audio, Stage & Lighting Production Waiver",
            category = "AV / Production",
            isDefault = false,
            createdAt = now,
            updatedAt = now,
            body = """
TECHNICAL AV, STAGE & LIGHTING RENTAL WAIVER
{{store_name}} · Order #{{order_number}} ({{order_date}})

Production Client: {{client_name}} ({{client_phone}})
Show / Event: {{event_name}} at {{event_venue}}
Production Window: {{pickup_date}} through {{return_date}} ({{rental_days}} day(s))

ASSIGNED GEAR & RIGGING
{{equipment_list}}

BILLING & DEPOSIT HOLD
Rental Fee Total: {{total_cost}} (Subtotal {{subtotal}}, Discount {{discount}}, Tax {{tax}})
Security Hold / Deposit: {{deposit}}
Payment Type: {{payment_method}}

TECHNICAL COMPLIANCE
- Renter assumes responsibility for clean power supply (regulated voltage) for amplifiers, LED walls, and DMX lighting rigs.
- Individual serialized QR tags are scanned out at dispatch and scanned back in at warehouse return.
- Flight cases, XLR/DMX cables, and wireless microphones must match serial check-out counts.

Notes: {{order_notes}}

Technical Sign-Off ({{client_name}}): _____________________
            """.trimIndent(),
        ),
    )

    fun populateTemplate(
        templateBody: String,
        receipt: ReceiptWithLines,
        customer: CustomerEntity?,
        settings: SettingsModel,
    ): String {
        val r = receipt.receipt
        val sym = settings.currencySymbol
        val eqLines = if (receipt.lines.isEmpty()) {
            "  (No equipment items recorded)"
        } else {
            receipt.lines.joinToString("\n") { line ->
                val qty = formatQty(line.qty)
                val unit = formatCents(line.priceCents, sym)
                val total = formatCents(line.lineTotalCents, sym)
                "• ${line.name} — $qty × $unit/day × ${r.rentalDays}d = $total"
            }
        }
        val discountStr = if (r.discountCents > 0L) "-${formatCents(r.discountCents, sym)}" else formatCents(0L, sym)

        val replacements = mapOf(
            "{{store_name}}" to settings.storeName.ifBlank { "EventGear Rentals" },
            "{{order_number}}" to r.number.toString().padStart(4, '0'),
            "{{order_date}}" to formatDateTime(r.createdAt),
            "{{client_name}}" to (r.customerName?.takeIf { it.isNotBlank() } ?: customer?.name ?: "Walk-in Client"),
            "{{client_phone}}" to (customer?.phone?.takeIf { it.isNotBlank() } ?: "Not provided"),
            "{{client_email}}" to (customer?.email?.takeIf { it.isNotBlank() } ?: "Not provided"),
            "{{event_name}}" to r.eventName.ifBlank { "Private Event Rental" },
            "{{event_venue}}" to r.eventVenue.ifBlank { "Client Pickup / Specified Venue" },
            "{{pickup_date}}" to formatShortDate(r.rentalStartMs),
            "{{return_date}}" to formatShortDate(r.rentalEndMs),
            "{{rental_days}}" to r.rentalDays.toString(),
            "{{equipment_list}}" to eqLines,
            "{{subtotal}}" to formatCents(r.subtotalCents, sym),
            "{{discount}}" to discountStr,
            "{{tax}}" to formatCents(r.taxCents, sym),
            "{{deposit}}" to formatCents(r.depositCents, sym),
            "{{total_cost}}" to formatCents(r.totalCents, sym),
            "{{payment_method}}" to r.paymentType.uppercase(),
            "{{order_notes}}" to r.note.ifBlank { "None" },
        )

        var output = templateBody
        for ((token, value) in replacements) {
            output = output.replace(token, value)
        }
        return output
    }

    fun populateSamplePreview(
        templateBody: String,
        settings: SettingsModel,
    ): String {
        val now = nowMs()
        val sym = settings.currencySymbol
        val replacements = mapOf(
            "{{store_name}}" to settings.storeName.ifBlank { "EventGear Rentals" },
            "{{order_number}}" to "0042",
            "{{order_date}}" to formatDateTime(now),
            "{{client_name}}" to "Selamawit Events & Galas",
            "{{client_phone}}" to "+251 911 245 890",
            "{{client_email}}" to "bookings@selamevents.com",
            "{{event_name}}" to "Annual Corporate Gala Night",
            "{{event_venue}}" to "Millennium Hall Pavilion B",
            "{{pickup_date}}" to formatShortDate(now),
            "{{return_date}}" to formatShortDate(now + 3L * 86_400_000L),
            "{{rental_days}}" to "3",
            "{{equipment_list}}" to listOf(
                "• 10x10m Frame Marquee Tent — 1 × ${formatCents(15000L, sym)}/day × 3d = ${formatCents(45000L, sym)}",
                "• Tiffany Gold Chiavari Chair — 60 × ${formatCents(450L, sym)}/day × 3d = ${formatCents(81000L, sym)}",
                "• JBL PRX Active PA Speaker Pair — 2 × ${formatCents(8500L, sym)}/day × 3d = ${formatCents(51000L, sym)}",
            ).joinToString("\n"),
            "{{subtotal}}" to formatCents(177000L, sym),
            "{{discount}}" to formatCents(0L, sym),
            "{{tax}}" to formatCents(0L, sym),
            "{{deposit}}" to formatCents(35000L, sym),
            "{{total_cost}}" to formatCents(177000L, sym),
            "{{payment_method}}" to "CARD",
            "{{order_notes}}" to "Deliver by 08:00 AM via Gate 3 loading bay.",
        )

        var output = templateBody
        for ((token, value) in replacements) {
            output = output.replace(token, value)
        }
        return output
    }

    suspend fun createAgreementPdfFile(
        context: Context,
        title: String,
        orderLabel: String,
        populatedText: String,
    ): File = withContext(Dispatchers.IO) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 44
        val contentWidth = pageWidth - margin * 2

        val doc = PdfDocument()
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 163, 74)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(17, 24, 39)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 41, 55)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 114, 128)
            textSize = 9f
        }

        val fullLayout = StaticLayout.Builder
            .obtain(populatedText, 0, populatedText.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.18f)
            .build()

        var lineIdx = 0
        var pageNum = 1
        while (lineIdx < fullLayout.lineCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            var cursorY = margin.toFloat()
            if (pageNum == 1) {
                canvas.drawText(orderLabel.uppercase(), margin.toFloat(), cursorY + 10f, headerPaint)
                cursorY += 20f
                canvas.drawText(title, margin.toFloat(), cursorY + 14f, titlePaint)
                cursorY += 28f
            }

            val maxY = (pageHeight - margin - 24).toFloat()
            canvas.save()
            canvas.translate(margin.toFloat(), cursorY)
            var drawnHeight = 0f
            while (lineIdx < fullLayout.lineCount) {
                val lineHeight = (fullLayout.getLineBottom(lineIdx) - fullLayout.getLineTop(lineIdx)).toFloat()
                if (cursorY + drawnHeight + lineHeight > maxY && drawnHeight > 0f) {
                    break
                }
                val start = fullLayout.getLineStart(lineIdx)
                val end = fullLayout.getLineEnd(lineIdx)
                val lineText = populatedText.substring(start, end).trimEnd('\n')
                canvas.drawText(lineText, 0f, drawnHeight - fullLayout.getLineAscent(lineIdx).toFloat(), bodyPaint)
                drawnHeight += lineHeight
                lineIdx++
            }
            canvas.restore()

            canvas.drawText("Page $pageNum · Digital Rental Agreement", margin.toFloat(), (pageHeight - 20).toFloat(), footerPaint)
            doc.finishPage(page)
            pageNum++
        }

        val dir = File(context.cacheDir, "agreements").apply { mkdirs() }
        val safeOrder = orderLabel.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(dir, "agreement-$safeOrder.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
            out.flush()
        }
        doc.close()
        file
    }

    fun shareAgreementText(context: Context, title: String, populatedText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, populatedText)
        }
        val chooser = Intent.createChooser(intent, "Share Rental Agreement").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareAgreementPdf(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
