package com.loypos.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.loypos.desktop.core.DesktopRentalReturnReminderCalculator
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.ui.AgreementTemplatesView
import com.loypos.desktop.ui.ClientsView
import com.loypos.desktop.ui.DesktopBadge
import com.loypos.desktop.ui.DesktopDamageReportsView
import com.loypos.desktop.ui.DesktopDashboardView
import com.loypos.desktop.ui.DesktopEventKitsView
import com.loypos.desktop.ui.DesktopPrimaryButton
import com.loypos.desktop.ui.DesktopSettingsView
import com.loypos.desktop.ui.EquipmentView
import com.loypos.desktop.ui.LoyBackground
import com.loypos.desktop.ui.LoyBorder
import com.loypos.desktop.ui.LoyBorderStrong
import com.loypos.desktop.ui.LoyDesktopTheme
import com.loypos.desktop.ui.LoyError
import com.loypos.desktop.ui.LoyErrorLight
import com.loypos.desktop.ui.LoyGreen
import com.loypos.desktop.ui.LoyGreenDark
import com.loypos.desktop.ui.LoyGreenLight
import com.loypos.desktop.ui.LoyInfo
import com.loypos.desktop.ui.LoyInfoLight
import com.loypos.desktop.ui.LoySidebarAccent
import com.loypos.desktop.ui.LoySidebarBg
import com.loypos.desktop.ui.LoySidebarBorder
import com.loypos.desktop.ui.LoySidebarHover
import com.loypos.desktop.ui.LoySidebarMuted
import com.loypos.desktop.ui.LoySidebarSurface
import com.loypos.desktop.ui.LoySidebarText
import com.loypos.desktop.ui.LoySurface
import com.loypos.desktop.ui.LoySurfaceHover
import com.loypos.desktop.ui.LoyTextMuted
import com.loypos.desktop.ui.LoyTextSecondary
import com.loypos.desktop.ui.LoyWarning
import com.loypos.desktop.ui.LoyWarningLight
import com.loypos.desktop.ui.QrTrackingView
import com.loypos.desktop.ui.RentDeskView
import com.loypos.desktop.ui.RentalCalendarView
import com.loypos.desktop.ui.RentalOrdersView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DesktopTabGroup(val title: String) {
    OPERATIONS("OPERATIONS"),
    FLEET("FLEET & LOGS"),
    SYSTEM("DIRECTORY"),
}

private enum class DesktopTab(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val group: DesktopTabGroup,
) {
    DASHBOARD("Dashboard", "Executive overview, return alerts & analytics", Icons.Outlined.Dashboard, DesktopTabGroup.OPERATIONS),
    RENT_DESK("Rent Desk", "Create multi-day bookings, kits & quotes", Icons.Outlined.PointOfSale, DesktopTabGroup.OPERATIONS),
    RENTALS("Rentals", "Active dispatches, 24h due returns & refunds", Icons.AutoMirrored.Outlined.ReceiptLong, DesktopTabGroup.OPERATIONS),
    SCHEDULE("Schedule", "Monthly dispatch calendar & capacity matrix", Icons.Outlined.CalendarMonth, DesktopTabGroup.OPERATIONS),
    EQUIPMENT("Equipment", "Fleet inventory, daily rates & safety minimums", Icons.Outlined.Inventory2, DesktopTabGroup.FLEET),
    EVENT_KITS("Event Kits", "Pre-bundled equipment packages & discounts", Icons.Outlined.Layers, DesktopTabGroup.FLEET),
    QR_TRACKING("QR Track", "Serialized unit tags, scanner & printable labels", Icons.Outlined.QrCodeScanner, DesktopTabGroup.FLEET),
    DAMAGE_LOG("Damage Log", "Return inspection notes, photos & repair queue", Icons.Outlined.ReportProblem, DesktopTabGroup.FLEET),
    AGREEMENTS("Contracts", "Digital rental agreement templates & exports", Icons.Outlined.Description, DesktopTabGroup.FLEET),
    CLIENTS("Clients", "Event organizers, venues & booking history", Icons.Outlined.PeopleOutline, DesktopTabGroup.SYSTEM),
    SETTINGS("Settings", "Company profile, JSON/CSV backups & cloud sync", Icons.Outlined.Settings, DesktopTabGroup.SYSTEM),
}

fun main() = application {
    val repository = remember { DesktopRepository() }
    val windowState = rememberWindowState(width = 1360.dp, height = 860.dp)

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
    var activeTab by remember { mutableStateOf(DesktopTab.DASHBOARD) }
    var pendingRentDeskStartMs by remember { mutableStateOf<Long?>(null) }
    var pendingKitToAdd by remember { mutableStateOf<DesktopEventKit?>(null) }
    var pendingDamageOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    var pendingRentalsFilterIdx by remember { mutableStateOf<Int?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastNonce by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val now = remember(snapshot.orders) { nowMs() }
    val activeOutCount = remember(snapshot.orders) { snapshot.orders.count { it.isRentalOut } }
    val dueWithin24h = remember(snapshot.orders, now) {
        DesktopRentalReturnReminderCalculator.findRentalsDueWithin24Hours(snapshot.orders, now)
    }
    val lowFleetCount = remember(snapshot.items, snapshot.settings.lowStockThreshold) {
        snapshot.items.count { it.isBelowSafetyThreshold(snapshot.settings.lowStockThreshold) }
    }
    val openDamageCount = remember(snapshot.damageReports) {
        snapshot.damageReports.count { it.status != "resolved" }
    }

    fun showToast(msg: String) {
        toastMessage = msg
        val myNonce = ++toastNonce
        scope.launch {
            delay(3400L)
            if (toastNonce == myNonce) {
                toastMessage = null
            }
        }
    }

    fun openRentalsDue24h() {
        pendingRentalsFilterIdx = 2
        activeTab = DesktopTab.RENTALS
    }

    Box(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Executive Slate Sidebar
            Column(
                modifier = Modifier
                    .width(228.dp)
                    .fillMaxHeight()
                    .background(LoySidebarBg),
            ) {
                // Brand Identity Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LoySidebarAccent,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "LP",
                                color = LoySidebarBg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = snapshot.settings.storeName.ifBlank { "EventGear" },
                            color = LoySidebarText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "RENTAL WORKSTATION",
                            color = LoySidebarMuted,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, letterSpacing = 0.8.sp),
                        )
                    }
                }
                HorizontalDivider(color = LoySidebarBorder)

                // Grouped Navigation List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    DesktopTabGroup.entries.forEachIndexed { groupIndex, group ->
                        if (groupIndex > 0) {
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(
                            text = group.title,
                            color = LoySidebarMuted.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.9.sp),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        DesktopTab.entries.filter { it.group == group }.forEach { tab ->
                            val badgeText: String? = when (tab) {
                                DesktopTab.RENTALS -> when {
                                    dueWithin24h.isNotEmpty() -> "${dueWithin24h.size} due"
                                    activeOutCount > 0 -> "$activeOutCount"
                                    else -> null
                                }
                                DesktopTab.EQUIPMENT -> if (lowFleetCount > 0) "$lowFleetCount" else null
                                DesktopTab.DAMAGE_LOG -> if (openDamageCount > 0) "$openDamageCount" else null
                                else -> null
                            }
                            val badgeWarning = (tab == DesktopTab.RENTALS && dueWithin24h.isNotEmpty()) ||
                                tab == DesktopTab.EQUIPMENT ||
                                tab == DesktopTab.DAMAGE_LOG

                            SidebarNavRow(
                                tab = tab,
                                selected = activeTab == tab,
                                badgeText = badgeText,
                                badgeIsAlert = badgeWarning,
                                onClick = { activeTab = tab },
                            )
                        }
                    }
                }

                // Sidebar Footer Action
                HorizontalDivider(color = LoySidebarBorder)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LoyGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { activeTab = DesktopTab.RENT_DESK },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "New Rental Order",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // Main Workspace Area
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Top 50.dp Command Header Bar
                Surface(color = LoySurface, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = activeTab.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "/",
                                    color = LoyBorderStrong,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = activeTab.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Clickable 24h Return Reminder Status Pill
                                val hasOverdue = dueWithin24h.any { it.isOverdue }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        hasOverdue -> LoyErrorLight
                                        dueWithin24h.isNotEmpty() -> LoyWarningLight
                                        else -> LoyGreenLight
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            hasOverdue -> LoyError.copy(alpha = 0.3f)
                                            dueWithin24h.isNotEmpty() -> LoyWarning.copy(alpha = 0.35f)
                                            else -> LoyGreen.copy(alpha = 0.25f)
                                        },
                                    ),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { openRentalsDue24h() },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (hasOverdue) Icons.Outlined.WarningAmber else Icons.Outlined.NotificationsActive,
                                            contentDescription = "24-hour rental return monitor",
                                            tint = when {
                                                hasOverdue -> LoyError
                                                dueWithin24h.isNotEmpty() -> LoyWarning
                                                else -> LoyGreenDark
                                            },
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = if (dueWithin24h.isEmpty()) {
                                                "0 returns due <24h"
                                            } else {
                                                "${dueWithin24h.size} return${if (dueWithin24h.size == 1) "" else "s"} due <24h"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                hasOverdue -> LoyError
                                                dueWithin24h.isNotEmpty() -> LoyWarning
                                                else -> LoyGreenDark
                                            },
                                        )
                                    }
                                }

                                if (activeOutCount > 0) {
                                    DesktopBadge(
                                        text = "$activeOutCount Active Out",
                                        bgColor = LoyInfoLight,
                                        fgColor = LoyInfo,
                                        modifier = Modifier
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable { activeTab = DesktopTab.RENTALS },
                                    )
                                }

                                if (activeTab != DesktopTab.RENT_DESK) {
                                    DesktopPrimaryButton(
                                        text = "Open Rent Desk",
                                        icon = Icons.Outlined.PointOfSale,
                                        onClick = { activeTab = DesktopTab.RENT_DESK },
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = LoyBorder)
                    }
                }

                // Active Workspace View
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        DesktopTab.DASHBOARD -> DesktopDashboardView(
                            repository = repository,
                            snapshot = snapshot,
                            onOpenRentDesk = { activeTab = DesktopTab.RENT_DESK },
                            onOpenRentals = { activeTab = DesktopTab.RENTALS },
                            onOpenDueRentals = { openRentalsDue24h() },
                            onOpenEquipment = { activeTab = DesktopTab.EQUIPMENT },
                            onOpenSchedule = { activeTab = DesktopTab.SCHEDULE },
                            onOpenQrTracking = { activeTab = DesktopTab.QR_TRACKING },
                            onOpenAgreements = { activeTab = DesktopTab.AGREEMENTS },
                            onOpenEventKits = { activeTab = DesktopTab.EVENT_KITS },
                            onOpenDamageLog = { activeTab = DesktopTab.DAMAGE_LOG },
                            onReportDamage = { order ->
                                pendingDamageOrder = order
                                activeTab = DesktopTab.DAMAGE_LOG
                            },
                            onToast = ::showToast,
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
                            initialFilterIdx = pendingRentalsFilterIdx,
                            onConsumeInitialFilter = { pendingRentalsFilterIdx = null },
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

                // Bottom 26.dp Desktop Status Bar
                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(LoySurfaceHover)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(LoySidebarAccent))
                            Text(
                                text = "Local Depot Store Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextSecondary,
                            )
                        }
                        Text("·", color = LoyBorderStrong, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${snapshot.items.size} Fleet SKUs",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextMuted,
                        )
                        Text("·", color = LoyBorderStrong, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${snapshot.orders.size} Rental Orders",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextMuted,
                        )
                        Text("·", color = LoyBorderStrong, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "24h Return Alert Monitor (${dueWithin24h.size} active)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dueWithin24h.isNotEmpty()) LoyWarning else LoyTextMuted,
                        )
                    }

                    Text(
                        text = "${snapshot.settings.currencySymbol} Currency · Tax ${snapshot.settings.taxRatePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextMuted,
                    )
                }
            }
        }

        // Floating Bottom-Right Desktop Notification Toast
        toastMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LoySidebarBg,
                border = BorderStroke(1.dp, LoySidebarAccent.copy(alpha = 0.4f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 40.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = LoySidebarAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavRow(
    tab: DesktopTab,
    selected: Boolean,
    badgeText: String?,
    badgeIsAlert: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bgColor = when {
        selected -> LoySidebarSurface
        hovered -> LoySidebarHover
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (selected) 20.dp else 0.dp)
                .background(if (selected) LoySidebarAccent else Color.Transparent),
        )
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = if (selected) LoySidebarAccent else if (hovered) LoySidebarText else LoySidebarMuted,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = tab.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected || hovered) LoySidebarText else LoySidebarMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (badgeIsAlert) LoyWarning.copy(alpha = 0.22f) else LoySidebarAccent.copy(alpha = 0.20f),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (badgeIsAlert) Color(0xFFFBBF24) else LoySidebarAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
