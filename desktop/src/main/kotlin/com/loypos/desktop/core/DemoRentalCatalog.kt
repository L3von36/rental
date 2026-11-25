package com.loypos.desktop.core

import com.loypos.desktop.model.DesktopCategory
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopDeliveryStop
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopEventKitItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

fun newId(): String = UUID.randomUUID().toString()

fun nowMs(): Long = System.currentTimeMillis()

fun formatCents(cents: Long, symbol: String = "$"): String {
    val neg = cents < 0L
    val absVal = abs(cents) / 100.0
    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(absVal)
    return if (neg) "-$symbol$formatted" else "$symbol$formatted"
}

fun formatQty(qty: Double): String {
    return if (abs(qty - qty.roundToLong()) < 0.0001) {
        qty.roundToLong().toString()
    } else {
        String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }
}

fun formatShortDate(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))
}

fun parseAmountToCents(input: String): Long? {
    val clean = input.trim().replace(",", ".")
    if (clean.isEmpty()) return null
    val d = clean.toDoubleOrNull() ?: return null
    if (d < 0.0) return null
    return (d * 100.0).roundToLong()
}

data class RentalCalcResult(
    val subtotalCents: Long,
    val discountCents: Long,
    val taxCents: Long,
    val totalCents: Long,
)

fun computeRentalTotals(
    rawSubtotalCents: Long,
    discountPercent: Double,
    fixedDiscountCents: Long,
    taxRatePercent: Double,
    taxIncluded: Boolean,
): RentalCalcResult {
    val pctDiscount = if (discountPercent > 0.0) {
        (rawSubtotalCents * (discountPercent / 100.0)).roundToLong()
    } else {
        0L
    }
    val discount = (pctDiscount + fixedDiscountCents).coerceIn(0L, rawSubtotalCents)
    val discounted = (rawSubtotalCents - discount).coerceAtLeast(0L)
    return if (taxRatePercent <= 0.0) {
        RentalCalcResult(rawSubtotalCents, discount, 0L, discounted)
    } else if (taxIncluded) {
        val tax = (discounted * taxRatePercent / (100.0 + taxRatePercent)).roundToLong()
        RentalCalcResult(rawSubtotalCents, discount, tax, discounted)
    } else {
        val tax = (discounted * (taxRatePercent / 100.0)).roundToLong()
        RentalCalcResult(rawSubtotalCents, discount, tax, discounted + tax)
    }
}

object DemoRentalCatalog {
    fun buildCategories(): List<DesktopCategory> = listOf(
        DesktopCategory("cat-tents", "Tents & Canopies", 0),
        DesktopCategory("cat-chairs", "Chairs & Seating", 1),
        DesktopCategory("cat-tables", "Tables & Linens", 2),
        DesktopCategory("cat-lighting", "Stage & Lighting", 3),
        DesktopCategory("cat-sound", "Sound & AV", 4),
        DesktopCategory("cat-stage", "Staging & Flooring", 5),
        DesktopCategory("cat-power", "Power & HVAC", 6),
        DesktopCategory("cat-decor", "Decor & Catering", 7),
    )

    private data class SeedSpec(
        val id: String,
        val catId: String,
        val name: String,
        val dailyRateDollars: Double,
        val replacementDollars: Double,
        val units: Double,
    )

    private val specs = listOf(
        SeedSpec("eq-001", "cat-tents", "10x10 Pop-Up Canopy Tent", 45.0, 28.0, 22.0),
        SeedSpec("eq-002", "cat-tents", "20x20 High-Peak Frame Tent", 185.0, 110.0, 10.0),
        SeedSpec("eq-003", "cat-tents", "20x40 Wedding Marquee Tent", 380.0, 210.0, 6.0),
        SeedSpec("eq-004", "cat-tents", "30x60 Clear-Span Event Tent", 790.0, 420.0, 3.0),
        SeedSpec("eq-005", "cat-tents", "Clear Vinyl Tent Sidewall (20ft)", 32.0, 18.0, 24.0),
        SeedSpec("eq-006", "cat-tents", "Weighted Concrete Tent Ballast", 12.0, 6.0, 64.0),
        SeedSpec("eq-007", "cat-tents", "Pagoda VIP Gazebo (16x16)", 240.0, 130.0, 5.0),
        SeedSpec("eq-008", "cat-chairs", "White Folding Resin Chair", 3.5, 1.8, 450.0),
        SeedSpec("eq-009", "cat-chairs", "Gold Chiavari Ballroom Chair", 8.5, 4.0, 280.0),
        SeedSpec("eq-010", "cat-chairs", "Crossback Rustic Oak Chair", 9.0, 4.5, 180.0),
        SeedSpec("eq-011", "cat-chairs", "Padded Black Banquet Chair", 4.5, 2.2, 320.0),
        SeedSpec("eq-012", "cat-chairs", "Acrylic Ghost Wedding Chair", 12.0, 5.5, 120.0),
        SeedSpec("eq-013", "cat-chairs", "Modern Barstool (Chrome & White)", 16.0, 8.0, 48.0),
        SeedSpec("eq-014", "cat-chairs", "White Faux-Leather VIP Sofa", 95.0, 52.0, 8.0),
        SeedSpec("eq-015", "cat-tables", "6ft Rectangular Banquet Table", 12.0, 6.0, 85.0),
        SeedSpec("eq-016", "cat-tables", "8ft Heavy-Duty Folding Table", 15.0, 7.5, 60.0),
        SeedSpec("eq-017", "cat-tables", "60\" Round Reception Table", 14.0, 7.0, 72.0),
        SeedSpec("eq-018", "cat-tables", "Cocktail Highboy Pub Table", 18.0, 9.0, 45.0),
        SeedSpec("eq-019", "cat-tables", "White Stretch Spandex Cover", 9.0, 4.0, 95.0),
        SeedSpec("eq-020", "cat-tables", "Ivory Satin Tablecloth (120\" Rnd)", 16.0, 7.0, 65.0),
        SeedSpec("eq-021", "cat-tables", "LED Lighted Serpentine Bar", 145.0, 75.0, 6.0),
        SeedSpec("eq-022", "cat-lighting", "Warm Bistro String Lights (50ft)", 22.0, 11.0, 42.0),
        SeedSpec("eq-023", "cat-lighting", "Wireless RGBW LED Uplight", 18.0, 9.0, 64.0),
        SeedSpec("eq-024", "cat-lighting", "Moving Head Beam Spotlight", 55.0, 28.0, 16.0),
        SeedSpec("eq-025", "cat-lighting", "8-Tier Crystal Tent Chandelier", 110.0, 55.0, 6.0),
        SeedSpec("eq-026", "cat-lighting", "DMX Lighting Control Console", 65.0, 32.0, 4.0),
        SeedSpec("eq-027", "cat-lighting", "Custom Monogram Gobo Projector", 45.0, 22.0, 5.0),
        SeedSpec("eq-028", "cat-lighting", "Flood Work Light Tripod Pair", 28.0, 14.0, 14.0),
        SeedSpec("eq-029", "cat-sound", "1000W Powered PA Speaker + Stand", 48.0, 24.0, 20.0),
        SeedSpec("eq-030", "cat-sound", "18\" Active Event Subwoofer", 65.0, 32.0, 8.0),
        SeedSpec("eq-031", "cat-sound", "Dual UHF Wireless Mic Kit", 40.0, 18.0, 14.0),
        SeedSpec("eq-032", "cat-sound", "12-Channel Digital Audio Mixer", 55.0, 26.0, 6.0),
        SeedSpec("eq-033", "cat-sound", "Pro DJ Controller & Booth Table", 120.0, 60.0, 4.0),
        SeedSpec("eq-034", "cat-sound", "5500-Lumen Laser Projector + Screen", 160.0, 85.0, 5.0),
        SeedSpec("eq-035", "cat-stage", "4x8 Heavy-Duty Stage Deck", 55.0, 28.0, 36.0),
        SeedSpec("eq-036", "cat-stage", "Oak Parquet Dance Floor (3x3 Panel)", 14.0, 6.5, 110.0),
        SeedSpec("eq-037", "cat-stage", "Checkered B&W Dance Floor (15x15)", 260.0, 130.0, 4.0),
        SeedSpec("eq-038", "cat-stage", "3-Step Stage Staircase w/ Rail", 42.0, 20.0, 6.0),
        SeedSpec("eq-039", "cat-stage", "Aluminum Box Truss Section (10ft)", 38.0, 18.0, 24.0),
        SeedSpec("eq-040", "cat-stage", "Clear Acrylic Presenter Podium", 75.0, 38.0, 5.0),
        SeedSpec("eq-041", "cat-power", "7000W Quiet Inverter Generator", 135.0, 68.0, 7.0),
        SeedSpec("eq-042", "cat-power", "50kW Towable Whisper Generator", 390.0, 210.0, 2.0),
        SeedSpec("eq-043", "cat-power", "Outdoor Spider Power Distro Box", 45.0, 22.0, 10.0),
        SeedSpec("eq-044", "cat-power", "50ft 10-Gauge Heavy Extension Cord", 9.0, 4.0, 40.0),
        SeedSpec("eq-045", "cat-power", "5-Channel Heavy Cable Protector Ramp", 15.0, 7.0, 32.0),
        SeedSpec("eq-046", "cat-power", "Stainless Pyramid Patio Heater", 55.0, 26.0, 22.0),
        SeedSpec("eq-047", "cat-decor", "Chrome Velvet Rope & Stanchion Set", 24.0, 11.0, 28.0),
        SeedSpec("eq-048", "cat-decor", "Red VIP Event Carpet (4x25ft)", 48.0, 22.0, 8.0),
        SeedSpec("eq-049", "cat-decor", "Pipe & Drape Backdrop Kit (10x20ft)", 65.0, 30.0, 16.0),
        SeedSpec("eq-050", "cat-decor", "Stainless Full-Size Chafer Dish", 18.0, 8.0, 48.0),
        SeedSpec("eq-051", "cat-decor", "36-Quart Beverage Cooling Tub", 16.0, 7.5, 22.0),
        SeedSpec("eq-052", "cat-decor", "Floral Gold Hexagon Ceremony Arch", 75.0, 34.0, 6.0),
    )

    fun buildEquipment(createdAt: Long = nowMs()): List<DesktopEquipment> = specs.mapIndexed { idx, spec ->
        val skuNum = (idx + 1).toString().padStart(3, '0')
        DesktopEquipment(
            id = spec.id,
            categoryId = spec.catId,
            name = spec.name,
            priceCents = (spec.dailyRateDollars * 100.0).roundToLong(),
            costCents = (spec.replacementDollars * 100.0).roundToLong(),
            sku = "EVT-$skuNum",
            barcode = "8901000${skuNum}9",
            trackStock = true,
            stock = spec.units,
            createdAt = createdAt,
        )
    }

    fun buildClients(createdAt: Long = nowMs()): List<DesktopClient> = listOf(
        DesktopClient(
            id = "cust-demo-1",
            name = "Grand Horizon Events",
            phone = "+1 555-0148",
            email = "events@grandhorizon.example",
            note = "Preferred wedding & gala organizer",
            points = 240L,
            ordersCount = 4,
            createdAt = createdAt,
        ),
        DesktopClient(
            id = "cust-demo-2",
            name = "Apex Conferences & Expo",
            phone = "+1 555-0192",
            email = "logistics@apexexpo.example",
            note = "Monthly corporate summit account",
            points = 120L,
            ordersCount = 2,
            createdAt = createdAt,
        ),
    )

    fun buildDefaultEventKits(now: Long = nowMs()): List<DesktopEventKit> = listOf(
        DesktopEventKit(
            id = "kit-wedding-50",
            name = "Wedding Package",
            description = "Complete reception setup for ~50 guests: chairs, round tables, satin linens, and PA sound.",
            category = "Wedding",
            discountPercent = 10.0,
            createdAt = now,
            updatedAt = now,
            items = listOf(
                DesktopEventKitItem("eq-009", "Gold Chiavari Ballroom Chair", 50.0),
                DesktopEventKitItem("eq-017", "60\" Round Reception Table", 5.0),
                DesktopEventKitItem("eq-020", "Ivory Satin Tablecloth (120\" Rnd)", 5.0),
                DesktopEventKitItem("eq-029", "1000W Powered PA Speaker + Stand", 1.0),
            ),
        ),
        DesktopEventKit(
            id = "kit-corporate-av",
            name = "Corporate Conference Kit",
            description = "Turnkey AV & seating bundle for town halls, keynotes, and board workshops.",
            category = "Corporate",
            discountPercent = 5.0,
            createdAt = now,
            updatedAt = now,
            items = listOf(
                DesktopEventKitItem("eq-011", "Padded Black Banquet Chair", 40.0),
                DesktopEventKitItem("eq-015", "6ft Rectangular Banquet Table", 4.0),
                DesktopEventKitItem("eq-034", "5500-Lumen Laser Projector + Screen", 1.0),
                DesktopEventKitItem("eq-031", "Dual UHF Wireless Mic Kit", 1.0),
            ),
        ),
        DesktopEventKit(
            id = "kit-outdoor-party",
            name = "Outdoor Night Party Kit",
            description = "All-weather marquee tent with ambient lighting and sound system.",
            category = "Outdoor",
            discountPercent = 8.0,
            createdAt = now,
            updatedAt = now,
            items = listOf(
                DesktopEventKitItem("eq-002", "20x20 High-Peak Frame Tent", 1.0),
                DesktopEventKitItem("eq-023", "Wireless RGBW LED Uplight", 8.0),
                DesktopEventKitItem("eq-022", "Warm Bistro String Lights (50ft)", 2.0),
                DesktopEventKitItem("eq-029", "1000W Powered PA Speaker + Stand", 1.0),
            ),
        ),
    )

    fun buildDefaultDeliveryStops(now: Long = nowMs()): List<com.loypos.desktop.model.DesktopDeliveryStop> {
        val today0900 = dayStartMs(LocalDate.now()) + 9L * 3_600_000L
        val today1300 = today0900 + 4L * 3_600_000L
        val tomorrow0730 = today0900 + 86_400_000L - 1_800_000L
        val tomorrow1200 = tomorrow0730 + 4L * 3_600_000L + 1_800_000L
        return listOf(
            com.loypos.desktop.model.DesktopDeliveryStop(
                id = "stop-demo-1",
                type = "delivery",
                orderNumber = null,
                customerName = "Grand Horizon Events",
                eventName = "Riverside Wedding Gala",
                venue = "Lakeside Grand Ballroom — Dock B",
                contactPhone = "+1 555-0148",
                scheduledMs = today0900,
                driverName = "Marcus T.",
                vehicle = "Isuzu NPR Box Truck",
                itemsSummary = "20× Gold Chiavari Chair, 2× 60\" Round Table, 1× PA Speaker + Stand",
                notes = "Service elevator access after 08:30; ask for venue manager Rosa.",
                status = "scheduled",
                createdAt = now,
            ),
            com.loypos.desktop.model.DesktopDeliveryStop(
                id = "stop-demo-2",
                type = "delivery",
                orderNumber = null,
                customerName = "Apex Conferences & Expo",
                eventName = "Annual Tech Summit — Day 1",
                venue = "Convention Plaza, Hall C",
                contactPhone = "+1 555-0192",
                scheduledMs = today1300,
                driverName = "Priya K.",
                vehicle = "Sprinter Cargo Van 02",
                itemsSummary = "6× Folding Banquet Table, 30× Padded Chair, 2× PA Speaker",
                notes = "Loading dock closes at 15:00 sharp — stagehand will meet driver.",
                status = "scheduled",
                createdAt = now,
            ),
            com.loypos.desktop.model.DesktopDeliveryStop(
                id = "stop-demo-3",
                type = "pickup",
                orderNumber = null,
                customerName = "Grand Horizon Events",
                eventName = "Riverside Wedding Gala",
                venue = "Lakeside Grand Ballroom — Dock B",
                contactPhone = "+1 555-0148",
                scheduledMs = tomorrow0730,
                driverName = "Marcus T.",
                vehicle = "Isuzu NPR Box Truck",
                itemsSummary = "Same load as delivery — verify counts before leaving site",
                notes = "Inspect linens for stains on site; flag damage log if torn.",
                status = "scheduled",
                createdAt = now,
            ),
            com.loypos.desktop.model.DesktopDeliveryStop(
                id = "stop-demo-4",
                type = "pickup",
                orderNumber = null,
                customerName = "Apex Conferences & Expo",
                eventName = "Annual Tech Summit — Breakdown",
                venue = "Convention Plaza, Hall C",
                contactPhone = "+1 555-0192",
                scheduledMs = tomorrow1200,
                driverName = "Priya K.",
                vehicle = "Sprinter Cargo Van 02",
                itemsSummary = "6× Folding Banquet Table, 30× Padded Chair, 2× PA Speaker",
                notes = "Cable coil & case check with venue AV tech before sign-off.",
                status = "scheduled",
                createdAt = now,
            ),
        )
    }

    fun buildDefaultMaintenanceSchedules(now: Long = nowMs()): List<com.loypos.desktop.model.DesktopMaintenanceSchedule> = listOf(
        com.loypos.desktop.model.DesktopMaintenanceSchedule(
            id = "maint-001",
            itemId = "eq-029",
            itemName = "1000W Powered PA Speaker + Stand",
            serviceTitle = "Acoustic calibration & amp check",
            intervalDays = 30,
            intervalRentals = 12,
            lastServicedMs = now - 14L * 86_400_000L,
            nextDueMs = now + 16L * 86_400_000L,
            status = "scheduled",
            priority = "routine",
            technicianName = "Lead Sound Tech",
            notes = "Check woofer voice coil and Bluetooth receiver sync.",
            createdAt = now,
        ),
        com.loypos.desktop.model.DesktopMaintenanceSchedule(
            id = "maint-002",
            itemId = "eq-024",
            itemName = "Moving Head Beam Spotlight",
            serviceTitle = "Optics cleaning & DMX firmware sync",
            intervalDays = 21,
            intervalRentals = 8,
            lastServicedMs = now - 25L * 86_400_000L,
            nextDueMs = now - 4L * 86_400_000L,
            status = "overdue",
            priority = "high",
            technicianName = "Lighting Specialist",
            notes = "Clean pan/tilt mirrors and check color wheel servo alignment.",
            createdAt = now,
        ),
        com.loypos.desktop.model.DesktopMaintenanceSchedule(
            id = "maint-003",
            itemId = "eq-002",
            itemName = "20x20 High-Peak Frame Tent",
            serviceTitle = "Waterproofing spray & ratchet strap test",
            intervalDays = 45,
            intervalRentals = 15,
            lastServicedMs = now - 10L * 86_400_000L,
            nextDueMs = now + 35L * 86_400_000L,
            status = "scheduled",
            priority = "routine",
            technicianName = "Rigging Crew",
            notes = "Inspect PVC canopy seams and corner poles for rust.",
            createdAt = now,
        ),
        com.loypos.desktop.model.DesktopMaintenanceSchedule(
            id = "maint-004",
            itemId = "eq-033",
            itemName = "Pro DJ Controller & Booth Table",
            serviceTitle = "Fader lubrication & jog wheel calibration",
            intervalDays = 30,
            intervalRentals = 10,
            lastServicedMs = now - 5L * 86_400_000L,
            nextDueMs = now + 25L * 86_400_000L,
            status = "scheduled",
            priority = "routine",
            technicianName = "AV Tech",
            notes = "Check crossfader tension and USB audio driver handshake.",
            createdAt = now,
        ),
    )
}
