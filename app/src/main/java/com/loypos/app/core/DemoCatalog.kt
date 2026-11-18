package com.loypos.app.core

import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.EventKitEntity
import com.loypos.app.data.local.EventKitItemEntity
import com.loypos.app.data.local.EventKitWithItems
import com.loypos.app.data.local.ItemEntity

object DemoCatalog {
    val categories: List<CategoryEntity> = listOf(
        CategoryEntity("cat-tents", "Tents & Canopies", 0, 0L),
        CategoryEntity("cat-chairs", "Chairs & Seating", 1, 0L),
        CategoryEntity("cat-tables", "Tables & Linens", 2, 0L),
        CategoryEntity("cat-lighting", "Lighting & FX", 3, 0L),
        CategoryEntity("cat-sound", "Sound & AV", 4, 0L),
        CategoryEntity("cat-stage", "Staging & Truss", 5, 0L),
        CategoryEntity("cat-power", "Power & Cooling", 6, 0L),
        CategoryEntity("cat-decor", "Decor & Catering", 7, 0L),
    )

    private fun equipment(
        id: String,
        catId: String,
        name: String,
        dailyRateCents: Long,
        replacementCostCents: Long,
        sku: String,
        quantity: Double,
        safetyThreshold: Double? = null,
    ) = ItemEntity(
        id = id,
        categoryId = catId,
        name = name,
        priceCents = dailyRateCents,
        costCents = replacementCostCents,
        sku = sku,
        barcode = "",
        trackStock = true,
        stock = quantity,
        archived = false,
        branchIds = "",
        imagePath = "",
        safetyThreshold = safetyThreshold,
        updatedAt = 0L,
    )

    val items: List<ItemEntity> = listOf(
        // Tents & Canopies
        equipment("eq-001", "cat-tents", "10x10m Pagoda Frame Tent", 1800000, 12000000, "EVT-001", 6.0, safetyThreshold = 2.0),
        equipment("eq-002", "cat-tents", "6x6m Pyramid Canopy Tent", 950000, 5800000, "EVT-002", 10.0, safetyThreshold = 3.0),
        equipment("eq-003", "cat-tents", "3x3m Heavy-Duty Pop-Up Gazebo", 350000, 1800000, "EVT-003", 18.0, safetyThreshold = 4.0),
        equipment("eq-004", "cat-tents", "Clear-Span Wedding Marquee 10x20m", 4200000, 28000000, "EVT-004", 3.0, safetyThreshold = 2.0),
        equipment("eq-005", "cat-tents", "White Sidewall Panel 6m (Windowed)", 120000, 750000, "EVT-005", 32.0, safetyThreshold = 8.0),
        equipment("eq-006", "cat-tents", "Tent Heavy Ballast Weight 40kg", 45000, 320000, "EVT-006", 48.0, safetyThreshold = 12.0),
        equipment("eq-007", "cat-tents", "Stretch Bedouin Sail Canopy", 1500000, 9000000, "EVT-007", 4.0, safetyThreshold = 2.0),

        // Chairs & Seating
        equipment("eq-008", "cat-chairs", "Gold Chiavari Ballroom Chair", 25000, 220000, "EVT-008", 400.0, safetyThreshold = 50.0),
        equipment("eq-009", "cat-chairs", "White Folding Resin Chair", 15000, 120000, "EVT-009", 600.0, safetyThreshold = 80.0),
        equipment("eq-010", "cat-chairs", "Cross-Back Wooden Wedding Chair", 32000, 280000, "EVT-010", 220.0, safetyThreshold = 40.0),
        equipment("eq-011", "cat-chairs", "Black Banquet Stackable Chair", 18000, 140000, "EVT-011", 500.0, safetyThreshold = 60.0),
        equipment("eq-012", "cat-chairs", "VIP Tufted Lounge Loveseat", 350000, 2400000, "EVT-012", 14.0),
        equipment("eq-013", "cat-chairs", "Adjustable Chrome Bar Stool", 45000, 350000, "EVT-013", 60.0, safetyThreshold = 12.0),
        equipment("eq-014", "cat-chairs", "Ivory Chair Seat Cushion", 6000, 40000, "EVT-014", 600.0, safetyThreshold = 80.0),

        // Tables & Linens
        equipment("eq-015", "cat-tables", "Round Banquet Table 180cm (10-seat)", 140000, 950000, "EVT-015", 65.0, safetyThreshold = 12.0),
        equipment("eq-016", "cat-tables", "Rectangular Trestle Table 240cm", 110000, 700000, "EVT-016", 85.0, safetyThreshold = 15.0),
        equipment("eq-017", "cat-tables", "High-Top Cocktail Bistro Table", 120000, 820000, "EVT-017", 45.0, safetyThreshold = 10.0),
        equipment("eq-018", "cat-tables", "Satin Round Tablecloth 320cm", 45000, 220000, "EVT-018", 120.0, safetyThreshold = 25.0),
        equipment("eq-019", "cat-tables", "Spandex Cocktail Table Cover (White)", 30000, 140000, "EVT-019", 70.0, safetyThreshold = 15.0),
        equipment("eq-020", "cat-tables", "Sweetheart Velvet Head Table", 280000, 1800000, "EVT-020", 8.0),

        // Lighting & FX
        equipment("eq-021", "cat-lighting", "RGBW LED Par Can Wash Light", 85000, 520000, "EVT-021", 64.0, safetyThreshold = 10.0),
        equipment("eq-022", "cat-lighting", "230W Moving Head Beam Light", 260000, 2200000, "EVT-022", 24.0, safetyThreshold = 6.0),
        equipment("eq-023", "cat-lighting", "Warm Festoon String Lights (25m)", 65000, 350000, "EVT-023", 40.0, safetyThreshold = 8.0),
        equipment("eq-024", "cat-lighting", "Wireless Battery Uplight Bar", 110000, 680000, "EVT-024", 36.0, safetyThreshold = 8.0),
        equipment("eq-025", "cat-lighting", "DMX 512 Lighting Desk Controller", 240000, 1600000, "EVT-025", 6.0),
        equipment("eq-026", "cat-lighting", "Low-Lying Dry Ice Fog Machine", 320000, 2100000, "EVT-026", 5.0),
        equipment("eq-027", "cat-lighting", "Cold Spark Fountain Unit (Pair)", 450000, 2900000, "EVT-027", 8.0),
        equipment("eq-028", "cat-lighting", "Crystal Banquet Chandelier", 480000, 3200000, "EVT-028", 10.0),

        // Sound & AV
        equipment("eq-029", "cat-sound", "Dual 15-inch Active PA Speaker", 450000, 3400000, "EVT-029", 24.0, safetyThreshold = 4.0),
        equipment("eq-030", "cat-sound", "18-inch Powered Event Subwoofer", 550000, 4200000, "EVT-030", 12.0, safetyThreshold = 4.0),
        equipment("eq-031", "cat-sound", "Dual UHF Wireless Microphone Kit", 180000, 1200000, "EVT-031", 16.0, safetyThreshold = 4.0),
        equipment("eq-032", "cat-sound", "16-Channel Digital Audio Mixer", 420000, 3800000, "EVT-032", 6.0),
        equipment("eq-033", "cat-sound", "4K Laser Projector 6000 Lumens", 650000, 5200000, "EVT-033", 6.0),
        equipment("eq-034", "cat-sound", "Fast-Fold 3x2m Projection Screen", 290000, 1800000, "EVT-034", 8.0),
        equipment("eq-035", "cat-sound", "Podium Gooseneck Mic & Stand", 75000, 450000, "EVT-035", 10.0),

        // Staging & Truss
        equipment("eq-036", "cat-stage", "Modular Stage Deck 2x1m (Adjustable)", 160000, 1100000, "EVT-036", 44.0, safetyThreshold = 8.0),
        equipment("eq-037", "cat-stage", "Aluminum Box Truss Section 3m", 130000, 980000, "EVT-037", 36.0, safetyThreshold = 8.0),
        equipment("eq-038", "cat-stage", "Stage Steps with Safety Handrail", 85000, 550000, "EVT-038", 10.0),
        equipment("eq-039", "cat-stage", "Black Velour Stage Skirt 6m", 55000, 300000, "EVT-039", 22.0, safetyThreshold = 5.0),
        equipment("eq-040", "cat-stage", "LED White Gloss Dance Floor Tile 1x1m", 95000, 620000, "EVT-040", 64.0, safetyThreshold = 16.0),
        equipment("eq-041", "cat-stage", "Crank-Up Lighting T-Stand (4m)", 90000, 580000, "EVT-041", 14.0),

        // Power & Cooling
        equipment("eq-042", "cat-power", "Silent Diesel Generator 25kVA", 2800000, 24000000, "EVT-042", 3.0, safetyThreshold = 2.0),
        equipment("eq-043", "cat-power", "Portable Inverter Generator 5kVA", 750000, 5400000, "EVT-043", 7.0),
        equipment("eq-044", "cat-power", "Industrial Heavy Extension Cable 30m", 40000, 220000, "EVT-044", 35.0, safetyThreshold = 8.0),
        equipment("eq-045", "cat-power", "Cable Ramp Protector (5-Channel)", 35000, 210000, "EVT-045", 40.0, safetyThreshold = 10.0),
        equipment("eq-046", "cat-power", "Outdoor Mushroom Patio Heater", 220000, 1400000, "EVT-046", 20.0, safetyThreshold = 4.0),
        equipment("eq-047", "cat-power", "Evaporative Air Cooler Tent Unit", 310000, 2100000, "EVT-047", 12.0),

        // Decor & Catering
        equipment("eq-048", "cat-decor", "Red Velvet Carpet Runner 10m", 140000, 750000, "EVT-048", 12.0),
        equipment("eq-049", "cat-decor", "Gold Stanchion Pole & Velvet Rope", 55000, 320000, "EVT-049", 40.0, safetyThreshold = 8.0),
        equipment("eq-050", "cat-decor", "Floral Ceremony Brass Arch Frame", 210000, 1300000, "EVT-050", 9.0),
        equipment("eq-051", "cat-decor", "Roll-Top Stainless Chafing Dish 9L", 85000, 520000, "EVT-051", 48.0, safetyThreshold = 10.0),
        equipment("eq-052", "cat-decor", "360 Photo Booth Spinning Platform", 850000, 6200000, "EVT-052", 3.0, safetyThreshold = 2.0),
    )

    fun defaultCustomer(now: Long = nowMs()) = CustomerEntity(
        id = "cust-events",
        name = "Grand Horizon Events",
        phone = "+251 911-000102",
        email = "bookings@grandhorizonevents.example",
        note = "Corporate & wedding event planner",
        points = 25,
        createdAt = now,
        updatedAt = 0L,
    )

    fun defaultEventKits(now: Long = nowMs()): List<EventKitWithItems> {
        val byId = items.associateBy { it.id }
        fun kitLine(kitId: String, suffix: String, itemId: String, qty: Double): EventKitItemEntity {
            val product = byId[itemId]
            return EventKitItemEntity(
                id = "$kitId-$suffix",
                kitId = kitId,
                itemId = itemId,
                itemName = product?.name ?: itemId,
                qty = qty,
            )
        }
        return listOf(
            EventKitWithItems(
                kit = EventKitEntity(
                    id = "kit-wedding",
                    name = "Wedding Package",
                    category = "Wedding",
                    description = "50 Chiavari chairs, 5 round banquet tables, 5 satin tablecloths & 1 PA sound system",
                    discountPercent = 10.0,
                    createdAt = now,
                    updatedAt = now,
                ),
                items = listOf(
                    kitLine("kit-wedding", "1", "eq-008", 50.0),
                    kitLine("kit-wedding", "2", "eq-015", 5.0),
                    kitLine("kit-wedding", "3", "eq-018", 5.0),
                    kitLine("kit-wedding", "4", "eq-029", 1.0),
                ),
            ),
            EventKitWithItems(
                kit = EventKitEntity(
                    id = "kit-corporate",
                    name = "Corporate Conference Kit",
                    category = "Corporate",
                    description = "40 banquet chairs, 4 trestle tables, 4K projector, projection screen & wireless mic kit",
                    discountPercent = 5.0,
                    createdAt = now,
                    updatedAt = now,
                ),
                items = listOf(
                    kitLine("kit-corporate", "1", "eq-011", 40.0),
                    kitLine("kit-corporate", "2", "eq-016", 4.0),
                    kitLine("kit-corporate", "3", "eq-033", 1.0),
                    kitLine("kit-corporate", "4", "eq-034", 1.0),
                    kitLine("kit-corporate", "5", "eq-031", 1.0),
                ),
            ),
            EventKitWithItems(
                kit = EventKitEntity(
                    id = "kit-party",
                    name = "Outdoor Night Party Kit",
                    category = "Party",
                    description = "6x6m pyramid tent, 6 RGBW LED par lights, 2 festoon string strands & 1 PA speaker",
                    discountPercent = 8.0,
                    createdAt = now,
                    updatedAt = now,
                ),
                items = listOf(
                    kitLine("kit-party", "1", "eq-002", 1.0),
                    kitLine("kit-party", "2", "eq-021", 6.0),
                    kitLine("kit-party", "3", "eq-023", 2.0),
                    kitLine("kit-party", "4", "eq-029", 1.0),
                ),
            ),
        )
    }
}
