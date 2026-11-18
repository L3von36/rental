package com.loypos.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object CheckoutRoute

@Serializable
data object InventoryRoute

@Serializable
data object ReceiptsRoute

@Serializable
data object ShiftRoute

@Serializable
data object SettingsRoute

@Serializable
data object AccountRoute

@Serializable
data object CartRoute

@Serializable
data object PaymentRoute

@Serializable
data object ReceiptSuccessRoute

@Serializable
data class ReceiptDetailRoute(val receiptId: String)

@Serializable
data object StockRoute

@Serializable
data object CustomersRoute

@Serializable
data object ReportsRoute

@Serializable
data object CalendarRoute

@Serializable
data object BackOfficeOverviewRoute

@Serializable
data object BackOfficeInventoryRoute

@Serializable
data object BackOfficeStockRoute

@Serializable
data object BackOfficeReportsRoute

@Serializable
data object BackOfficeAdminRoute

@Serializable
data object BackOfficeCustomersRoute

@Serializable
data object BackOfficeReceiptsRoute

@Serializable
data class BackOfficeReceiptDetailRoute(val receiptId: String)

@Serializable
data class QrTrackingRoute(val initialItemId: String? = null)

@Serializable
data object AgreementsRoute

@Serializable
data object EventKitsRoute

@Serializable
data class DamageReportsRoute(
    val initialReceiptId: String? = null,
    val initialItemId: String? = null,
    val initialUnitCode: String? = null,
)

@Serializable
data class MaintenanceRoute(val initialItemId: String? = null)


