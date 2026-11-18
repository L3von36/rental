package com.loypos.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.loypos.app.core.DiscountInput
import com.loypos.app.core.formatQty
import kotlin.math.roundToLong

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    @ColumnInfo(name = "shop_name") val shopName: String = "",
    @ColumnInfo(name = "owner_name") val ownerName: String = "",
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val salt: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    val role: String = ROLE_OWNER,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    val active: Boolean = true,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
) {
    val isOwner: Boolean get() = role != ROLE_CASHIER
    val isCashier: Boolean get() = role == ROLE_CASHIER

    companion object {
        const val ROLE_OWNER = "owner"
        const val ROLE_CASHIER = "cashier"
    }
}

@Entity(
    tableName = "backoffice_users",
    indices = [Index(value = ["email"], unique = true)]
)
data class BackOfficeUserEntity(
    @PrimaryKey val id: String,
    val email: String,
    @ColumnInfo(name = "full_name") val fullName: String = "",
    @ColumnInfo(name = "shop_name") val shopName: String = "",
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val salt: String = "",
    val role: String = ROLE_ADMIN,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_MANAGER = "manager"
    }
}

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String = "",
    val phone: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    val archived: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["sku"]),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    val name: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    val sku: String = "",
    val barcode: String = "",
    @ColumnInfo(name = "track_stock") val trackStock: Boolean = false,
    val stock: Double = 0.0,
    val archived: Boolean = false,
    @ColumnInfo(name = "branch_ids") val branchIds: String = "",
    @ColumnInfo(name = "image_path") val imagePath: String = "",
    @ColumnInfo(name = "safety_threshold") val safetyThreshold: Double? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
) {
    val hasPhoto: Boolean get() = imagePath.isNotEmpty()
    val stockQuantity: Double get() = stock
    val priceAmount: Double get() = priceCents / 100.0

    fun effectiveSafetyThreshold(globalThreshold: Double): Double =
        (safetyThreshold ?: globalThreshold).coerceAtLeast(0.0)

    fun isBelowSafetyThreshold(globalThreshold: Double): Boolean =
        trackStock && !archived && stock < effectiveSafetyThreshold(globalThreshold)

    fun availableInBranch(branchId: String?): Boolean {
        if (branchIds.isEmpty() || branchId.isNullOrEmpty()) return true
        return branchIds.split(",").contains(branchId)
    }
}

typealias ItemEntity = ProductEntity

data class CategoryWithProducts(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id",
    )
    val products: List<ProductEntity>,
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val note: String = "",
    val points: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["created_at"]),
    ],
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val number: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long,
    @ColumnInfo(name = "discount_cents") val discountCents: Long,
    @ColumnInfo(name = "discount_type") val discountType: String? = null,
    @ColumnInfo(name = "discount_value") val discountValue: Double? = null,
    @ColumnInfo(name = "tax_cents") val taxCents: Long,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    @ColumnInfo(name = "paid_cents") val paidCents: Long,
    @ColumnInfo(name = "change_cents") val changeCents: Long,
    @ColumnInfo(name = "payment_type") val paymentType: String, // "cash" | "card"
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "customer_name") val customerName: String? = null,
    val status: String = "completed", // "completed" | "refunded"
    val note: String = "",
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "cashier_id") val cashierId: String? = null,
    @ColumnInfo(name = "cashier_name") val cashierName: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
    @ColumnInfo(name = "rental_days") val rentalDays: Int = 1,
    @ColumnInfo(name = "rental_start_ms") val rentalStartMs: Long = createdAt,
    @ColumnInfo(name = "rental_end_ms") val rentalEndMs: Long = createdAt + 86_400_000L,
    @ColumnInfo(name = "deposit_cents") val depositCents: Long = 0L,
    @ColumnInfo(name = "rental_status") val rentalStatus: String = "out", // "out" | "returned" | "refunded"
    @ColumnInfo(name = "returned_at_ms") val returnedAtMs: Long? = null,
    @ColumnInfo(name = "event_name") val eventName: String = "",
    @ColumnInfo(name = "event_venue") val eventVenue: String = "",
) {
    val timestamp: Long get() = createdAt
    val totalPriceCents: Long get() = totalCents
    val isCompleted: Boolean get() = status == "completed"
    val isReturned: Boolean get() = rentalStatus == "returned"
    val isRentalOut: Boolean get() = isCompleted && rentalStatus == "out"

    fun isOverdue(nowMs: Long = System.currentTimeMillis()): Boolean =
        isRentalOut && rentalEndMs > 0L && nowMs > rentalEndMs

    fun discountLabel(): String {
        if (discountType == null || discountValue == null || discountCents == 0L) return "Discount"
        return if (discountType == "percent") "Discount (${formatQty(discountValue)}%)" else "Discount"
    }
}

typealias ReceiptEntity = SaleEntity

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["receipt_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["receipt_id"]),
        Index(value = ["item_id"]),
    ],
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "receipt_id") val receiptId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val name: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
    val qty: Double,
    @ColumnInfo(name = "line_total_cents") val lineTotalCents: Long,
) {
    val saleId: String get() = receiptId
    val productId: String get() = itemId
}

typealias SaleLineEntity = SaleItemEntity
typealias ReceiptLineEntity = SaleItemEntity

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val name: String = "",
    @ColumnInfo(name = "discount_type") val discountType: String? = null,
    @ColumnInfo(name = "discount_value") val discountValue: Double? = null,
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "customer_name") val customerName: String? = null,
    val note: String = "",
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "rental_days") val rentalDays: Int = 1,
    @ColumnInfo(name = "rental_start_ms") val rentalStartMs: Long = createdAt,
    @ColumnInfo(name = "rental_end_ms") val rentalEndMs: Long = createdAt + 86_400_000L,
    @ColumnInfo(name = "deposit_cents") val depositCents: Long = 0L,
    @ColumnInfo(name = "event_name") val eventName: String = "",
    @ColumnInfo(name = "event_venue") val eventVenue: String = "",
)

@Entity(
    tableName = "ticket_lines",
    indices = [Index(value = ["ticket_id"])]
)
data class TicketLineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ticket_id") val ticketId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val name: String,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
    val qty: Double,
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["item_id"])]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val qty: Double,
    val type: String, // "rental" | "return" | "sale" | "receive" | "adjust" | "refund"
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val note: String = "",
    @ColumnInfo(name = "receipt_id") val receiptId: String? = null,
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "opened_at") val openedAt: Long,
    @ColumnInfo(name = "opening_float_cents") val openingFloatCents: Long = 0L,
    @ColumnInfo(name = "branch_id") val branchId: String? = null,
    @ColumnInfo(name = "closed_at") val closedAt: Long? = null,
    @ColumnInfo(name = "counted_cents") val countedCents: Long? = null,
    @ColumnInfo(name = "expected_cents") val expectedCents: Long? = null,
) {
    val isOpen: Boolean get() = closedAt == null

    fun expectedDrawerCents(cashSalesCents: Long, cashInCents: Long, cashOutCents: Long): Long =
        openingFloatCents + cashSalesCents + cashInCents - cashOutCents

    fun overShortCents(): Long? {
        val c = countedCents ?: return null
        val e = expectedCents ?: return null
        return c - e
    }
}

@Entity(
    tableName = "cash_movements",
    indices = [Index(value = ["shift_id"])]
)
data class CashMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    val type: String, // "in" | "out"
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val isCashIn: Boolean get() = type == "in"
}

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(
    tableName = "equipment_units",
    indices = [
        Index(value = ["item_id"]),
        Index(value = ["unit_code"], unique = true),
    ],
)
data class EquipmentUnitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "unit_code") val unitCode: String,
    @ColumnInfo(name = "qr_payload") val qrPayload: String,
    val status: String = STATUS_CHECKED_IN,
    @ColumnInfo(name = "current_receipt_id") val currentReceiptId: String? = null,
    @ColumnInfo(name = "current_event_name") val currentEventName: String = "",
    @ColumnInfo(name = "current_customer_name") val currentCustomerName: String = "",
    @ColumnInfo(name = "condition_note") val conditionNote: String = "",
    @ColumnInfo(name = "last_scan_at") val lastScanAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
) {
    val isCheckedOut: Boolean get() = status == STATUS_CHECKED_OUT
    val isMaintenance: Boolean get() = status == STATUS_MAINTENANCE

    companion object {
        const val STATUS_CHECKED_IN = "checked_in"
        const val STATUS_CHECKED_OUT = "checked_out"
        const val STATUS_MAINTENANCE = "maintenance"
    }
}

@Entity(
    tableName = "equipment_scan_logs",
    indices = [
        Index(value = ["unit_id"]),
        Index(value = ["item_id"]),
        Index(value = ["created_at"]),
    ],
)
data class EquipmentScanLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "unit_id") val unitId: String? = null,
    @ColumnInfo(name = "unit_code") val unitCode: String = "",
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "item_name") val itemName: String,
    val action: String, // "check_out" | "check_in" | "maintenance" | "inspect"
    @ColumnInfo(name = "receipt_id") val receiptId: String? = null,
    @ColumnInfo(name = "event_name") val eventName: String = "",
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "operator_name") val operatorName: String = "",
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "agreement_templates")
data class AgreementTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String = "General Event",
    val body: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

@Entity(tableName = "event_kits")
data class EventKitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "Wedding",
    val description: String = "",
    @ColumnInfo(name = "discount_percent") val discountPercent: Double = 0.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

@Entity(
    tableName = "event_kit_items",
    foreignKeys = [
        ForeignKey(
            entity = EventKitEntity::class,
            parentColumns = ["id"],
            childColumns = ["kit_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["kit_id"]),
        Index(value = ["item_id"]),
    ],
)
data class EventKitItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kit_id") val kitId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "item_name") val itemName: String,
    val qty: Double,
)

data class EventKitWithItems(
    @Embedded val kit: EventKitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "kit_id",
    )
    val items: List<EventKitItemEntity>,
)

@Entity(
    tableName = "damage_reports",
    indices = [
        Index(value = ["item_id"]),
        Index(value = ["receipt_id"]),
        Index(value = ["created_at"]),
    ],
)
data class DamageReportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "item_sku") val itemSku: String = "",
    @ColumnInfo(name = "item_name") val itemName: String,
    @ColumnInfo(name = "unit_code") val unitCode: String = "",
    @ColumnInfo(name = "receipt_id") val receiptId: String? = null,
    @ColumnInfo(name = "receipt_number") val receiptNumber: Int? = null,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    val severity: String = SEVERITY_MINOR, // "minor" | "moderate" | "severe"
    val status: String = STATUS_OPEN, // "open" | "repairing" | "resolved"
    @ColumnInfo(name = "repair_cost_cents") val repairCostCents: Long = 0L,
    val notes: String = "",
    @ColumnInfo(name = "photo_paths") val photoPaths: String = "", // JSON array of local paths
    @ColumnInfo(name = "reported_by") val reportedBy: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
) {
    fun photoPathList(): List<String> {
        if (photoPaths.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(photoPaths)
            (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
        } catch (_: Exception) {
            photoPaths.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    companion object {
        const val SEVERITY_MINOR = "minor"
        const val SEVERITY_MODERATE = "moderate"
        const val SEVERITY_SEVERE = "severe"

        const val STATUS_OPEN = "open"
        const val STATUS_REPAIRING = "repairing"
        const val STATUS_RESOLVED = "resolved"

        fun encodePhotoPaths(paths: List<String>): String {
            val arr = org.json.JSONArray()
            paths.filter { it.isNotBlank() }.distinct().forEach { arr.put(it) }
            return arr.toString()
        }
    }
}

data class CartLine(
    val itemId: String,
    val name: String,
    val priceCents: Long,
    val qty: Double,
    val rentalDays: Int = 1,
) {
    val dailyTotalCents: Long get() = (priceCents * qty).roundToLong()
    val lineTotalCents: Long get() = (priceCents * qty * rentalDays.coerceAtLeast(1)).roundToLong()
}

data class ReceiptWithLines(
    @Embedded val receipt: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "receipt_id",
    )
    val lines: List<SaleItemEntity>,
)

typealias SaleWithItems = ReceiptWithLines

data class TicketWithLines(
    val ticket: TicketEntity,
    val lines: List<TicketLineEntity>,
) {
    val discount: DiscountInput?
        get() = if (ticket.discountType != null && ticket.discountValue != null) {
            DiscountInput(ticket.discountType, ticket.discountValue)
        } else null

    fun toCartLines(): List<CartLine> {
        val days = ticket.rentalDays.coerceAtLeast(1)
        return lines.map {
            CartLine(
                itemId = it.itemId,
                name = it.name,
                priceCents = it.priceCents,
                qty = it.qty,
                rentalDays = days,
            )
        }
    }
}

data class SettingsModel(
    val storeName: String = "EventGear Rentals",
    val currencySymbol: String = "Br",
    val taxRatePercent: Double = 0.0,
    val taxIncluded: Boolean = false,
    val receiptFooter: String = "Thank you for renting with us! Inspect equipment on return.",
    val nextReceiptNumber: Int = 1,
    val lowStockAlertsEnabled: Boolean = false,
    val lowStockThreshold: Double = 3.0,
    val lowStockIntervalMinutes: Long = 15L,
    val lowStockLastCheckAt: Long = 0L,
    val lowStockLastCount: Int = 0,
    val returnReminderAlertsEnabled: Boolean = true,
    val returnReminderLastCheckAt: Long = 0L,
    val returnReminderLastCount: Int = 0,
    val dailyRevenueTargetCents: Long = 0L,
    val hasSeenWalkthrough: Boolean = false,
)

data class ShiftStats(
    val cashSalesCents: Long = 0L,
    val cardSalesCents: Long = 0L,
    val salesCount: Int = 0,
    val refundsCount: Int = 0,
    val cashInCents: Long = 0L,
    val cashOutCents: Long = 0L,
    val movements: List<CashMovementEntity> = emptyList(),
)

data class DaySalesSummary(
    val netCents: Long = 0L,
    val salesCount: Int = 0,
    val refundsCount: Int = 0,
    val cashCents: Long = 0L,
    val cardCents: Long = 0L,
)

data class DbStats(
    val branches: Int = 0,
    val categories: Int = 0,
    val items: Int = 0,
    val customers: Int = 0,
    val receipts: Int = 0,
    val tickets: Int = 0,
    val movements: Int = 0,
    val shifts: Int = 0,
    val fileBytes: Long = 0L,
)

data class CategoryReportRow(
    val categoryName: String,
    val totalCents: Long,
    val qty: Double,
)

data class TopItemReportRow(
    val itemName: String,
    val totalCents: Long,
    val qty: Double,
)

data class DailySalesPoint(
    val label: String,
    val totalCents: Long,
)

data class DailyRevenuePoint(
    val dayStartMs: Long,
    val label: String,
    val totalCents: Long,
    val salesCount: Int,
) {
    val revenueUnits: Double get() = totalCents / 100.0
}

data class MonthlyIncomePoint(
    val yearMonthKey: String,
    val label: String,
    val totalCents: Long,
    val ordersCount: Int,
    val rentedUnitDays: Double,
) {
    val revenueUnits: Double get() = totalCents / 100.0
}

data class EquipmentUtilizationPoint(
    val id: String,
    val label: String,
    val shortLabel: String,
    val utilizationPercent: Double,
    val activeOutUnits: Double,
    val totalCapacityUnits: Double,
    val revenueCents: Long,
)

