package com.loypos.desktop.model

import kotlinx.serialization.Serializable

@Serializable
data class DesktopCategory(
    val id: String,
    val name: String,
    val position: Int = 0,
)

@Serializable
data class DesktopEquipment(
    val id: String,
    val categoryId: String? = null,
    val name: String,
    val priceCents: Long,
    val costCents: Long = 0L,
    val sku: String = "",
    val barcode: String = "",
    val trackStock: Boolean = true,
    val stock: Double = 0.0,
    val createdAt: Long = 0L,
)

@Serializable
data class DesktopClient(
    val id: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val note: String = "",
    val points: Long = 0L,
    val ordersCount: Int = 0,
    val createdAt: Long = 0L,
)

@Serializable
data class DesktopRentalLine(
    val itemId: String? = null,
    val name: String,
    val qty: Double,
    val priceCents: Long,
    val lineTotalCents: Long,
    val taxPercent: Double = 0.0,
)

@Serializable
data class DesktopRentalOrder(
    val id: String,
    val number: Int,
    val createdAt: Long,
    val subtotalCents: Long,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long,
    val paymentType: String = "cash",
    val paidCents: Long = 0L,
    val changeCents: Long = 0L,
    val customerId: String? = null,
    val customerName: String? = null,
    val dispatcherName: String = "Desktop Operator",
    val branchName: String = "Main Depot",
    val note: String = "",
    val status: String = "completed",
    val rentalDays: Int = 1,
    val rentalStartMs: Long = createdAt,
    val rentalEndMs: Long = createdAt + 86_400_000L,
    val depositCents: Long = 0L,
    val rentalStatus: String = "out",
    val returnedAtMs: Long? = null,
    val eventName: String = "",
    val eventVenue: String = "",
    val lines: List<DesktopRentalLine> = emptyList(),
) {
    val isCompleted: Boolean get() = status == "completed"
    val isReturned: Boolean get() = isCompleted && rentalStatus == "returned"
    val isRentalOut: Boolean get() = isCompleted && rentalStatus != "returned"
    fun isOverdue(nowMs: Long): Boolean = isRentalOut && rentalEndMs in 1 until nowMs
}

@Serializable
data class DesktopHeldQuote(
    val id: String,
    val name: String,
    val createdAt: Long,
    val rentalDays: Int = 1,
    val customerId: String? = null,
    val customerName: String? = null,
    val eventName: String = "",
    val eventVenue: String = "",
    val depositCents: Long = 0L,
    val discountPercent: Double = 0.0,
    val discountCents: Long = 0L,
    val note: String = "",
    val lines: List<DesktopCartLine> = emptyList(),
)

@Serializable
data class DesktopCartLine(
    val itemId: String,
    val name: String,
    val dailyRateCents: Long,
    val qty: Double = 1.0,
    val rentalDays: Int = 1,
) {
    val lineTotalCents: Long
        get() = kotlin.math.round(qty * dailyRateCents * rentalDays.coerceAtLeast(1)).toLong()
}

@Serializable
data class DesktopSettings(
    val storeName: String = "EventGear Rentals",
    val receiptFooter: String = "Thank you for renting with EventGear! Please return equipment on time.",
    val currencySymbol: String = "$",
    val taxRatePercent: Double = 0.0,
    val taxIncluded: Boolean = true,
    val lowStockThreshold: Double = 4.0,
    val cloudApiUrl: String = "https://novelsapps.com.et/api/v1",
    val cloudSyncToken: String = "",
    val nextReceiptNumber: Int = 1,
)

@Serializable
data class DesktopEquipmentUnit(
    val id: String,
    val itemId: String,
    val itemName: String,
    val unitCode: String,
    val qrPayload: String,
    val status: String = "checked_in", // "checked_in" | "checked_out" | "maintenance"
    val currentOrderId: String? = null,
    val currentEventName: String = "",
    val currentCustomerName: String = "",
    val conditionNote: String = "",
    val lastScanAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isCheckedOut: Boolean get() = status == "checked_out"
    val isMaintenance: Boolean get() = status == "maintenance"
}

@Serializable
data class DesktopEquipmentScanLog(
    val id: String,
    val unitId: String? = null,
    val unitCode: String = "",
    val itemId: String,
    val itemName: String,
    val action: String, // "check_out" | "check_in" | "maintenance" | "inspect"
    val orderId: String? = null,
    val eventName: String = "",
    val customerName: String = "",
    val note: String = "",
    val createdAt: Long,
)

@Serializable
data class DesktopAgreementTemplate(
    val id: String,
    val title: String,
    val category: String = "General Event",
    val body: String,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class DesktopStoreSnapshot(
    val categories: List<DesktopCategory> = emptyList(),
    val items: List<DesktopEquipment> = emptyList(),
    val clients: List<DesktopClient> = emptyList(),
    val orders: List<DesktopRentalOrder> = emptyList(),
    val heldQuotes: List<DesktopHeldQuote> = emptyList(),
    val settings: DesktopSettings = DesktopSettings(),
    val equipmentUnits: List<DesktopEquipmentUnit> = emptyList(),
    val scanLogs: List<DesktopEquipmentScanLog> = emptyList(),
    val agreementTemplates: List<DesktopAgreementTemplate> = emptyList(),
)

// Android-compatible export envelope (loypos-export v1) for seamless Android <-> Desktop transfer
@Serializable
data class LoyPosExportPayload(
    val categories: List<DesktopCategory> = emptyList(),
    val items: List<DesktopEquipment> = emptyList(),
    val customers: List<DesktopClient> = emptyList(),
    val sales: List<ExportSaleRecord> = emptyList(),
    val receipts: List<DesktopRentalOrder> = emptyList(),
)

@Serializable
data class ExportSaleRecord(
    val id: String,
    val number: Int,
    val createdAt: Long,
    val subtotalCents: Long,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long,
    val paymentType: String = "cash",
    val paidCents: Long = 0L,
    val changeCents: Long = 0L,
    val customerId: String? = null,
    val customerName: String? = null,
    val note: String = "",
    val status: String = "completed",
    val rentalDays: Int = 1,
    val rentalStartMs: Long = createdAt,
    val rentalEndMs: Long = createdAt + 86_400_000L,
    val depositCents: Long = 0L,
    val rentalStatus: String = "out",
    val returnedAtMs: Long? = null,
    val eventName: String = "",
    val eventVenue: String = "",
    val lines: List<DesktopRentalLine> = emptyList(),
)

@Serializable
data class LoyPosExportV1(
    val app: String = "loypos-export",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val payload: LoyPosExportPayload,
)
