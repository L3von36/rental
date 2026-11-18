package com.loypos.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.ui.AgreementTemplatesView
import com.loypos.desktop.ui.ClientsView
import com.loypos.desktop.ui.DesktopDamageReportsView
import com.loypos.desktop.ui.DesktopDashboardView
import com.loypos.desktop.ui.DesktopEventKitsView
import com.loypos.desktop.ui.DesktopSettingsView
import com.loypos.desktop.ui.EquipmentView
import com.loypos.desktop.ui.LoyBorder
import com.loypos.desktop.ui.LoyDesktopTheme
import com.loypos.desktop.ui.LoyGreen
import com.loypos.desktop.ui.LoyGreenDark
import com.loypos.desktop.ui.LoyGreenLight
import com.loypos.desktop.ui.LoySurface
import com.loypos.desktop.ui.QrTrackingView
import com.loypos.desktop.ui.RentDeskView
import com.loypos.desktop.ui.RentalCalendarView
import com.loypos.desktop.ui.RentalOrdersView
import kotlinx.coroutines.launch

private enum class DesktopTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Outlined.Dashboard),
    RENT_DESK("Rent Desk", Icons.Outlined.PointOfSale),
    EVENT_KITS("Event Kits", Icons.Outlined.Layers),
    SCHEDULE("Schedule", Icons.Outlined.CalendarMonth),
    RENTALS("Rentals", Icons.AutoMirrored.Outlined.ReceiptLong),
    DAMAGE_LOG("Damage Log", Icons.Outlined.ReportProblem),
    QR_TRACKING("QR Track", Icons.Outlined.QrCodeScanner),
    AGREEMENTS("Contracts", Icons.Outlined.Description),
    EQUIPMENT("Equipment", Icons.Outlined.Inventory2),
    CLIENTS("Clients", Icons.Outlined.PeopleOutline),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

fun main() = application {
    val repository = remember { DesktopRepository() }
    val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "LoyPOS — Event Equipment Rental Desktop",
    ) {
        LoyDesktopTheme {
            DesktopShell(repository)
        }
    }
}

@Composable
private fun DesktopShell(repository: DesktopRepository) {
    val snapshot by repository.state.collectAsState()
    var activeTab by remember { mutableStateOf(DesktopTab.RENT_DESK) }
    var pendingRentDeskStartMs by remember { mutableStateOf<Long?>(null) }
    var pendingKitToAdd by remember { mutableStateOf<DesktopEventKit?>(null) }
    var pendingDamageOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeOutCount = remember(snapshot.orders) {
        snapshot.orders.count { it.isRentalOut }
    }
    val openDamageCount = remember(snapshot.damageReports) {
        snapshot.damageReports.count { it.status != "resolved" }
    }

    fun showToast(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavigationRail(
                containerColor = LoySurface,
                header = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LoyGreen,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "LP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Rental",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LoyGreenDark,
                        )
                    }
                },
                modifier = Modifier.fillMaxHeight(),
            ) {
                DesktopTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            when {
                                tab == DesktopTab.RENTALS && activeOutCount > 0 -> {
                                    BadgedBox(badge = { Badge { Text("$activeOutCount") } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                }
                                tab == DesktopTab.DAMAGE_LOG && openDamageCount > 0 -> {
                                    BadgedBox(badge = { Badge { Text("$openDamageCount") } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                }
                                else -> Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = LoyGreenDark,
                            selectedTextColor = LoyGreenDark,
                            indicatorColor = LoyGreenLight,
                        ),
                    )
                }
            }
            VerticalDivider(color = LoyBorder)

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (activeTab) {
                    DesktopTab.DASHBOARD -> DesktopDashboardView(
                        snapshot = snapshot,
                        onOpenRentDesk = { activeTab = DesktopTab.RENT_DESK },
                        onOpenRentals = { activeTab = DesktopTab.RENTALS },
                        onOpenEquipment = { activeTab = DesktopTab.EQUIPMENT },
                        onOpenSchedule = { activeTab = DesktopTab.SCHEDULE },
                        onOpenQrTracking = { activeTab = DesktopTab.QR_TRACKING },
                        onOpenAgreements = { activeTab = DesktopTab.AGREEMENTS },
                        onOpenEventKits = { activeTab = DesktopTab.EVENT_KITS },
                        onOpenDamageLog = { activeTab = DesktopTab.DAMAGE_LOG },
                    )
                    DesktopTab.RENT_DESK -> RentDeskView(
                        repository = repository,
                        snapshot = snapshot,
                        selectedStartDateMs = pendingRentDeskStartMs,
                        onConsumeSelectedStartDate = { pendingRentDeskStartMs = null },
                        pendingKitToAdd = pendingKitToAdd,
                        onConsumePendingKit = { pendingKitToAdd = null },
                        onOpenSchedule = { activeTab = DesktopTab.SCHEDULE },
                        onOpenEventKits = { activeTab = DesktopTab.EVENT_KITS },
                        onToast = ::showToast,
                    )
                    DesktopTab.EVENT_KITS -> DesktopEventKitsView(
                        repository = repository,
                        snapshot = snapshot,
                        onAddKitToOrder = { kit ->
                            pendingKitToAdd = kit
                            activeTab = DesktopTab.RENT_DESK
                        },
                        onToast = ::showToast,
                    )
                    DesktopTab.SCHEDULE -> RentalCalendarView(
                        repository = repository,
                        snapshot = snapshot,
                        onBookForDate = { startMs ->
                            pendingRentDeskStartMs = startMs
                            activeTab = DesktopTab.RENT_DESK
                        },
                        onToast = ::showToast,
                    )
                    DesktopTab.RENTALS -> RentalOrdersView(
                        repository = repository,
                        snapshot = snapshot,
                        onReportDamage = { order ->
                            pendingDamageOrder = order
                            activeTab = DesktopTab.DAMAGE_LOG
                        },
                        onToast = ::showToast,
                    )
                    DesktopTab.DAMAGE_LOG -> DesktopDamageReportsView(
                        repository = repository,
                        snapshot = snapshot,
                        prefillOrder = pendingDamageOrder,
                        onConsumePrefillOrder = { pendingDamageOrder = null },
                        onToast = ::showToast,
                    )
                    DesktopTab.QR_TRACKING -> QrTrackingView(
                        repository = repository,
                        snapshot = snapshot,
                        onToast = ::showToast,
                    )
                    DesktopTab.AGREEMENTS -> AgreementTemplatesView(
                        repository = repository,
                        snapshot = snapshot,
                        onToast = ::showToast,
                    )
                    DesktopTab.EQUIPMENT -> EquipmentView(
                        repository = repository,
                        snapshot = snapshot,
                        onOpenQrTracking = { activeTab = DesktopTab.QR_TRACKING },
                        onToast = ::showToast,
                    )
                    DesktopTab.CLIENTS -> ClientsView(
                        repository = repository,
                        snapshot = snapshot,
                        onToast = ::showToast,
                    )
                    DesktopTab.SETTINGS -> DesktopSettingsView(
                        repository = repository,
                        snapshot = snapshot,
                        onToast = ::showToast,
                    )
                }
            }
        }
    }
}
