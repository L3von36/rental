package com.loypos.desktop.core

import com.loypos.desktop.model.DesktopAgreementTemplate
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopSettings
import java.io.File

data class DesktopAgreementPlaceholder(
    val token: String,
    val label: String,
    val description: String,
)

object DesktopRentalAgreementEngine {
    val placeholders: List<DesktopAgreementPlaceholder> = listOf(
        DesktopAgreementPlaceholder("{{store_name}}", "Company Name", "Your rental business name"),
        DesktopAgreementPlaceholder("{{order_number}}", "Order #", "Formatted rental order number"),
        DesktopAgreementPlaceholder("{{order_date}}", "Booking Date", "Date and time order was placed"),
        DesktopAgreementPlaceholder("{{client_name}}", "Client Name", "Renter or organizer name"),
        DesktopAgreementPlaceholder("{{client_phone}}", "Client Phone", "Client phone number"),
        DesktopAgreementPlaceholder("{{client_email}}", "Client Email", "Client email address"),
        DesktopAgreementPlaceholder("{{event_name}}", "Event Name", "Event title or occasion"),
        DesktopAgreementPlaceholder("{{event_venue}}", "Event Venue", "Delivery or venue location"),
        DesktopAgreementPlaceholder("{{pickup_date}}", "Pickup / Start", "Rental start date"),
        DesktopAgreementPlaceholder("{{return_date}}", "Return Due", "Scheduled equipment return date"),
        DesktopAgreementPlaceholder("{{rental_days}}", "Duration", "Number of rental days"),
        DesktopAgreementPlaceholder("{{equipment_list}}", "Equipment List", "Itemized rented gear, quantities & totals"),
        DesktopAgreementPlaceholder("{{subtotal}}", "Subtotal", "Rental subtotal before discount/tax"),
        DesktopAgreementPlaceholder("{{discount}}", "Discount", "Applied discount amount"),
        DesktopAgreementPlaceholder("{{tax}}", "Tax", "Tax amount"),
        DesktopAgreementPlaceholder("{{deposit}}", "Security Deposit", "Refundable damage/security deposit"),
        DesktopAgreementPlaceholder("{{total_cost}}", "Total Cost", "Final rental total due"),
        DesktopAgreementPlaceholder("{{payment_method}}", "Payment Method", "Cash or Card"),
        DesktopAgreementPlaceholder("{{order_notes}}", "Order Notes", "Special instructions or delivery notes"),
    )

    fun defaultTemplates(now: Long): List<DesktopAgreementTemplate> = listOf(
        DesktopAgreementTemplate(
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
        DesktopAgreementTemplate(
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
        DesktopAgreementTemplate(
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
        order: DesktopRentalOrder,
        client: DesktopClient?,
        settings: DesktopSettings,
    ): String {
        val sym = settings.currencySymbol
        val eqLines = if (order.lines.isEmpty()) {
            "  (No equipment items recorded)"
        } else {
            order.lines.joinToString("\n") { line ->
                val qty = formatQty(line.qty)
                val unit = formatCents(line.priceCents, sym)
                val total = formatCents(line.lineTotalCents, sym)
                "• ${line.name} — $qty × $unit/day × ${order.rentalDays}d = $total"
            }
        }
        val discountStr = if (order.discountCents > 0L) "-${formatCents(order.discountCents, sym)}" else formatCents(0L, sym)

        val replacements = mapOf(
            "{{store_name}}" to settings.storeName.ifBlank { "EventGear Rentals" },
            "{{order_number}}" to order.number.toString().padStart(4, '0'),
            "{{order_date}}" to formatDateTime(order.createdAt),
            "{{client_name}}" to (order.customerName?.takeIf { it.isNotBlank() } ?: client?.name ?: "Walk-in Client"),
            "{{client_phone}}" to (client?.phone?.takeIf { it.isNotBlank() } ?: "Not provided"),
            "{{client_email}}" to (client?.email?.takeIf { it.isNotBlank() } ?: "Not provided"),
            "{{event_name}}" to order.eventName.ifBlank { "Private Event Rental" },
            "{{event_venue}}" to order.eventVenue.ifBlank { "Client Pickup / Specified Venue" },
            "{{pickup_date}}" to formatShortDate(order.rentalStartMs),
            "{{return_date}}" to formatShortDate(order.rentalEndMs),
            "{{rental_days}}" to order.rentalDays.toString(),
            "{{equipment_list}}" to eqLines,
            "{{subtotal}}" to formatCents(order.subtotalCents, sym),
            "{{discount}}" to discountStr,
            "{{tax}}" to formatCents(order.taxCents, sym),
            "{{deposit}}" to formatCents(order.depositCents, sym),
            "{{total_cost}}" to formatCents(order.totalCents, sym),
            "{{payment_method}}" to order.paymentType.uppercase(),
            "{{order_notes}}" to order.note.ifBlank { "None" },
        )

        var output = templateBody
        for ((token, value) in replacements) {
            output = output.replace(token, value)
        }
        return output
    }

    fun populateSamplePreview(
        templateBody: String,
        settings: DesktopSettings,
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

    fun exportHtmlAgreement(targetFile: File, title: String, populatedText: String): File {
        fun esc(s: String): String = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>${esc(title)}</title>
<style>
  body { font-family: Georgia, serif; max-width: 780px; margin: 40px auto; padding: 32px; color: #111827; line-height: 1.55; background: #ffffff; }
  h1 { color: #166534; font-size: 20px; border-bottom: 2px solid #16a34a; padding-bottom: 8px; }
  pre { white-space: pre-wrap; font-family: Georgia, serif; font-size: 14px; }
  @media print { body { margin: 12mm; padding: 0; } }
</style>
</head>
<body>
<h1>${esc(title)}</h1>
<pre>${esc(populatedText)}</pre>
</body>
</html>
        """.trimIndent()
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(html, Charsets.UTF_8)
        return targetFile
    }
}
