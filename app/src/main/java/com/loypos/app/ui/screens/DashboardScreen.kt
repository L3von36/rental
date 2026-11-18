package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.RentalTimeframeFilter
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatCentsShort
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.nowMs
import com.loypos.app.core.searchRentalAgreements
import com.loypos.app.data.local.DailyRevenuePoint
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import kotlin.math.roundToLong
import com.loypos.app.ui.components.BranchPickerSheet
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.EquipmentUtilizationChartCard
import com.loypos.app.ui.components.GlobalRentalSearchBar
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.MonthlyRentalIncomeChartCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget

private data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToCheckout: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToShift: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToQrTracking: () -> Unit = {},
    onNavigateToAgreements: () -> Unit = {},
    onNavigateToEventKits: () -> Unit = {},
    onNavigateToDamageReports: () -> Unit = {},
    onReportDamage: (String, String?) -> Unit = { _, _ -> },
    onNavigateToReceipts: () -> Unit,
    onNavigateToReceiptDetail: (String) -> Unit,
    onOpenHeldQuote: (String) -> Unit = {},
) {
    var showBranchSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchTimeframe by remember { mutableStateOf(RentalTimeframeFilter.ALL) }
    var selectedRentalId by remember { mutableStateOf<String?>(null) }

    val sym = data.settings.currencySymbol
    val branchId = session.activeBranchId
    val activeBranch = data.branches.firstOrNull { it.id == branchId }
    val isCashier = session.currentUser?.isCashier == true

    val todaySummary = remember(data.receipts, branchId) {
        viewModel.computeTodaySummary(data, branchId)
    }
    val avgTicketCents = if (todaySummary.salesCount == 0) 0L else todaySummary.netCents / todaySummary.salesCount

    val lowThreshold = data.settings.lowStockThreshold
    val outOfStockCount = remember(data.items) {
        data.items.count { it.trackStock && it.stock <= 0.0 }
    }
    val lowStockCount = remember(data.items, lowThreshold) {
        data.items.count { it.trackStock && it.stock > 0.0 && it.isBelowSafetyThreshold(lowThreshold) }
    }

    val branchReceipts = remember(data.receipts, branchId) {
        data.receipts.filter { r ->
            branchId == null || r.receipt.branchId == null || r.receipt.branchId == branchId
        }
    }
    val currentTimeMs = remember(branchReceipts) { nowMs() }
    val activeOutCount = remember(branchReceipts) {
        branchReceipts.count { it.receipt.isRentalOut }
    }
    val overdueCount = remember(branchReceipts, currentTimeMs) {
        branchReceipts.count { it.receipt.isOverdue(currentTimeMs) }
    }

    val agreementSearchOutput = remember(
        searchQuery,
        searchTimeframe,
        data.receipts,
        data.tickets,
        data.items,
        data.customers,
        data.equipmentUnits,
        data.equipmentScanLogs,
        branchId,
        currentTimeMs,
    ) {
        searchRentalAgreements(
            query = searchQuery,
            timeframeFilter = searchTimeframe,
            receipts = data.receipts,
            tickets = data.tickets,
            products = data.items,
            customers = data.customers,
            equipmentUnits = data.equipmentUnits,
            scanLogs = data.equipmentScanLogs,
            branchId = branchId,
            nowMs = currentTimeMs,
        )
    }

    val trendDays by viewModel.revenueTrendDays.collectAsStateWithLifecycle()
    val revenueTrend by viewModel.dailyRevenueTrend.collectAsStateWithLifecycle()
    val monthlyMonths by viewModel.monthlyIncomeMonths.collectAsStateWithLifecycle()
    var utilByCategory by remember { mutableStateOf(true) }
    var utilLiveMode by remember { mutableStateOf(true) }
    val monthlyIncomePoints = remember(data.receipts, monthlyMonths, branchId) {
        viewModel.computeMonthlyRentalIncome(data, monthlyMonths, branchId)
    }
    val utilByCategoryPoints = remember(data.items, data.categories, data.receipts, branchId, utilLiveMode) {
        viewModel.computeEquipmentUtilization(data, branchId, byCategory = true, liveOutMode = utilLiveMode)
    }
    val utilTopItemPoints = remember(data.items, data.categories, data.receipts, branchId, utilLiveMode) {
        viewModel.computeEquipmentUtilization(data, branchId, byCategory = false, liveOutMode = utilLiveMode)
    }

    val quickActions = remember(
        viewModel,
        onNavigateToCheckout,
        onNavigateToInventory,
        onNavigateToSchedule,
        onNavigateToQrTracking,
        onNavigateToAgreements,
        onNavigateToEventKits,
        onNavigateToDamageReports,
        onNavigateToShift,
        onNavigateToCustomers,
        onNavigateToReports,
    ) {
        listOf(
            QuickActionItem("Rent Desk", Icons.Outlined.PointOfSale, "dash_action_checkout", onNavigateToCheckout),
            QuickActionItem("Event Kits", Icons.Outlined.Layers, "dash_action_event_kits", onNavigateToEventKits),
            QuickActionItem("Damage Log", Icons.Outlined.ReportProblem, "dash_action_damage_reports", onNavigateToDamageReports),
            QuickActionItem("QR Track", Icons.Outlined.QrCodeScanner, "dash_action_qr_tracking", onNavigateToQrTracking),
            QuickActionItem("Contracts", Icons.Outlined.Description, "dash_action_agreements", onNavigateToAgreements),
            QuickActionItem("Schedule", Icons.Outlined.CalendarMonth, "dash_action_schedule", onNavigateToSchedule),
            QuickActionItem("Back Office", Icons.Outlined.AdminPanelSettings, "dash_action_backoffice") {
                viewModel.openBackOffice()
            },
            QuickActionItem("Equipment", Icons.Outlined.Inventory2, "dash_action_inventory", onNavigateToInventory),
            QuickActionItem("Shift", Icons.Outlined.AccessTime, "dash_action_shift", onNavigateToShift),
            QuickActionItem("Clients", Icons.Outlined.PeopleOutline, "dash_action_customers", onNavigateToCustomers),
            QuickActionItem("Reports", Icons.Outlined.Assessment, "dash_action_reports", onNavigateToReports),
        )
    }

    val windowSize = LocalLoyWindowSize.current
    val metrics = LocalLoyMetrics.current
    val activeDetail = remember(data.receipts, branchReceipts, selectedRentalId) {
        data.receipts.firstOrNull { it.receipt.id == selectedRentalId } ?: branchReceipts.firstOrNull()
    }
    val quickActionCols = remember(windowSize) {
        when {
            windowSize.isDesktop -> 6
            windowSize.isExpanded -> 4
            windowSize.isMedium -> 4
            else -> 3
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            title = {
                Column {
                    Text(
                        text = data.settings.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (activeBranch != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = !isCashier && data.branches.size > 1) {
                                    showBranchSheet = true
                                }
                                .testTag("dashboard_branch_selector"),
                        ) {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = "Active branch",
                                tint = LoyGreen,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = activeBranch.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyGreen,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onNavigateToSchedule,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("dashboard_schedule_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Schedule & Availability Calendar",
                        tint = LoyGreen,
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.openBackOffice() },
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("dashboard_open_backoffice_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Open Back Office",
                        tint = LoyGreen,
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(
                    onClick = { viewModel.showWalkthrough() },
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("dashboard_walkthrough_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Getting Started Walkthrough",
                        tint = LoyTextSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                TopSyncIconButton(
                    syncing = session.syncing,
                    onClick = { viewModel.syncCloud() },
                )
            },
        )
        HorizontalDivider(color = LoyBorder)

        val dashboardContent: @Composable (Modifier) -> Unit = { contentModifier ->
            LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(metrics.screenPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                // Global Rental Agreement Search Bar
                item {
                    GlobalRentalSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedTimeframe = searchTimeframe,
                        onSelectTimeframe = { searchTimeframe = it },
                        searchOutput = agreementSearchOutput,
                        currencySymbol = sym,
                        onSelectAgreement = { result ->
                            if (result.isHeldQuote) {
                                onOpenHeldQuote(result.id)
                            } else if (windowSize.useSplitPane) {
                                selectedRentalId = result.id
                            } else {
                                onNavigateToReceiptDetail(result.id)
                            }
                        },
                        onViewAllRentals = onNavigateToReceipts,
                        testTagPrefix = "dashboard_search",
                    )
                }

                // Today's Overview
                item {
                    SectionHeader("Today's Overview")
                    GroupCard {
                        Column(
                            modifier = Modifier.padding(metrics.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                        ) {
                            if (windowSize.isCompact) {
                                Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                    StatBlock(
                                        label = "Net rentals",
                                        value = formatCents(todaySummary.netCents, sym),
                                        valueColor = LoyGreen,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Rentals / Refunds",
                                        value = "${todaySummary.salesCount} / ${todaySummary.refundsCount}",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                    StatBlock(
                                        label = "Avg booking",
                                        value = formatCents(avgTicketCents, sym),
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Out now / Overdue",
                                        value = "$activeOutCount / $overdueCount",
                                        valueColor = if (overdueCount > 0) LoyError else LoyInfo,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                    StatBlock(
                                        label = "Net rentals",
                                        value = formatCents(todaySummary.netCents, sym),
                                        valueColor = LoyGreen,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Rentals / Refunds",
                                        value = "${todaySummary.salesCount} / ${todaySummary.refundsCount}",
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Avg booking",
                                        value = formatCents(avgTicketCents, sym),
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatBlock(
                                        label = "Out now / Overdue",
                                        value = "$activeOutCount / $overdueCount",
                                        valueColor = if (overdueCount > 0) LoyError else LoyInfo,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToCheckout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = metrics.minTouchTarget)
                                    .testTag("dashboard_start_checkout_button"),
                            ) {
                                Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Rent Desk", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Responsive Quick Actions Grid
                item {
                    SectionHeader("Quick Actions")
                    val rows = remember(quickActions, quickActionCols) {
                        quickActions.chunked(quickActionCols)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowItems.forEach { action ->
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = LoySurface,
                                        border = BorderStroke(1.dp, LoyBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .clickable(onClick = action.onClick)
                                            .testTag(action.tag),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 66.dp)
                                                .padding(horizontal = 6.dp, vertical = 9.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(LoyGreenLight),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = action.icon,
                                                    contentDescription = action.label,
                                                    tint = LoyGreen,
                                                    modifier = Modifier.size(17.dp),
                                                )
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(
                                                text = action.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoyTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                repeat(quickActionCols - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

            // Monthly Rental Income & Equipment Utilization Rate Charts (Vico)
            if (windowSize.isExpanded) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("Monthly Rental Income")
                            MonthlyRentalIncomeChartCard(
                                points = monthlyIncomePoints,
                                selectedMonths = monthlyMonths,
                                currencySymbol = sym,
                                onSelectMonths = viewModel::setMonthlyIncomeMonths,
                                subtitlePrefix = activeBranch?.name ?: "All branches",
                                testTagPrefix = "dash_monthly_income",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("Equipment Utilization Rate")
                            EquipmentUtilizationChartCard(
                                byCategoryPoints = utilByCategoryPoints,
                                topItemPoints = utilTopItemPoints,
                                liveOutMode = utilLiveMode,
                                byCategory = utilByCategory,
                                onToggleLiveMode = { utilLiveMode = it },
                                onToggleByCategory = { utilByCategory = it },
                                testTagPrefix = "dash_utilization",
                            )
                        }
                    }
                }
            } else {
                item {
                    SectionHeader("Monthly Rental Income")
                    MonthlyRentalIncomeChartCard(
                        points = monthlyIncomePoints,
                        selectedMonths = monthlyMonths,
                        currencySymbol = sym,
                        onSelectMonths = viewModel::setMonthlyIncomeMonths,
                        subtitlePrefix = activeBranch?.name ?: "All branches",
                        testTagPrefix = "dash_monthly_income",
                    )
                }
                item {
                    SectionHeader("Equipment Utilization Rate")
                    EquipmentUtilizationChartCard(
                        byCategoryPoints = utilByCategoryPoints,
                        topItemPoints = utilTopItemPoints,
                        liveOutMode = utilLiveMode,
                        byCategory = utilByCategory,
                        onToggleLiveMode = { utilLiveMode = it },
                        onToggleByCategory = { utilByCategory = it },
                        testTagPrefix = "dash_utilization",
                    )
                }
            }

            // Inventory Alerts + Trend Card (side-by-side on Expanded screens)
            if (windowSize.isExpanded) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("Daily Revenue Trend")
                            DailyRevenueTrendCard(
                                points = revenueTrend,
                                selectedDays = trendDays,
                                currencySymbol = sym,
                                onSelectDays = viewModel::setRevenueTrendDays,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("Equipment Fleet Alerts")
                            GroupCard {
                                Column(
                                    modifier = Modifier.padding(metrics.cardPadding),
                                    verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (outOfStockCount + lowStockCount > 0) Color(0xFFFFF3E0) else LoyGreenLight),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (outOfStockCount + lowStockCount > 0) Icons.Outlined.WarningAmber else Icons.Outlined.Inventory2,
                                                contentDescription = "Equipment fleet status",
                                                tint = if (outOfStockCount + lowStockCount > 0) LoyWarning else LoyGreen,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${data.items.size} tools · ${data.categories.size} categories",
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoyTextPrimary,
                                            )
                                            Text(
                                                text = "$outOfStockCount rented out · $lowStockCount low availability",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (outOfStockCount > 0) LoyError else LoyTextSecondary,
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = onNavigateToInventory,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = metrics.minTouchTarget)
                                                .testTag("dashboard_manage_catalog_button"),
                                        ) {
                                            Text("Equipment", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = onNavigateToStock,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = metrics.minTouchTarget)
                                                .testTag("dashboard_adjust_stock_button"),
                                        ) {
                                            Text("Adjust Fleet", maxLines = 1)
                                        }
                                    }
                                    if (outOfStockCount + lowStockCount > 0) {
                                        OutlinedButton(
                                            onClick = { viewModel.runLowStockCheckNow() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = metrics.minTouchTarget)
                                                .testTag("dashboard_check_stock_now_expanded"),
                                        ) {
                                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Run Background Stock Check")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Equipment Fleet Alerts
                item {
                    SectionHeader("Equipment Fleet Alerts")
                    GroupCard {
                        Column(
                            modifier = Modifier.padding(metrics.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (outOfStockCount + lowStockCount > 0) Color(0xFFFFF3E0) else LoyGreenLight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (outOfStockCount + lowStockCount > 0) Icons.Outlined.WarningAmber else Icons.Outlined.Inventory2,
                                        contentDescription = "Equipment fleet status",
                                        tint = if (outOfStockCount + lowStockCount > 0) LoyWarning else LoyGreen,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${data.items.size} tools · ${data.categories.size} categories",
                                        fontWeight = FontWeight.SemiBold,
                                        color = LoyTextPrimary,
                                    )
                                    Text(
                                        text = "$outOfStockCount rented out · $lowStockCount low availability",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (outOfStockCount > 0) LoyError else LoyTextSecondary,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onNavigateToInventory,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = metrics.minTouchTarget)
                                        .testTag("dashboard_manage_catalog_button"),
                                ) {
                                    Text("Equipment", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToStock,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = metrics.minTouchTarget)
                                        .testTag("dashboard_adjust_stock_button"),
                                ) {
                                    Text("Adjust Fleet", maxLines = 1)
                                }
                            }
                            if (outOfStockCount + lowStockCount > 0) {
                                OutlinedButton(
                                    onClick = { viewModel.runLowStockCheckNow() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = metrics.minTouchTarget)
                                        .testTag("dashboard_check_stock_now_compact"),
                                ) {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Run Background Stock Check")
                                }
                            }
                        }
                    }
                }

                // Daily Revenue Trend Chart (Vico powered by Room SaleEntity)
                item {
                    SectionHeader("Daily Revenue Trend")
                    DailyRevenueTrendCard(
                        points = revenueTrend,
                        selectedDays = trendDays,
                        currencySymbol = sym,
                        onSelectDays = viewModel::setRevenueTrendDays,
                    )
                }
            }

            // Recent Rentals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("Recent Rentals")
                    if (branchReceipts.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateToReceipts,
                            modifier = Modifier.heightIn(min = metrics.minTouchTarget),
                        ) {
                            Text("View all")
                        }
                    }
                }
                GroupCard {
                    val recent = branchReceipts.take(5)
                    if (recent.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "No rentals recorded yet",
                            subtitle = "Book your first rental on the Rent Desk.",
                            actionLabel = "Go to Rent Desk",
                            onAction = onNavigateToCheckout,
                        )
                    } else {
                        Column {
                            recent.forEachIndexed { idx, item ->
                                if (idx > 0) HorizontalDivider(color = LoyBorder)
                                val r = item.receipt
                                val isSelected = windowSize.useSplitPane && activeDetail?.receipt?.id == r.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .background(if (isSelected) LoyGreenLight.copy(alpha = 0.5f) else Color.Transparent)
                                        .clickable {
                                            if (windowSize.useSplitPane) {
                                                selectedRentalId = r.id
                                            } else {
                                                onNavigateToReceiptDetail(r.id)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("dashboard_recent_sale_${r.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (windowSize.useSplitPane) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isSelected) LoyGreen else Color.Transparent),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                "#${r.number.toString().padStart(4, '0')} · ${r.rentalDays}d",
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoyTextPrimary,
                                            )
                                            when {
                                                !r.isCompleted -> PillBadge(
                                                    text = "Refunded",
                                                    backgroundColor = LoyErrorLight,
                                                    textColor = LoyError,
                                                )
                                                r.isReturned -> PillBadge(
                                                    text = "Returned",
                                                    backgroundColor = LoyGreenLight,
                                                    textColor = LoyGreen,
                                                )
                                                r.isOverdue(currentTimeMs) -> PillBadge(
                                                    text = "Overdue",
                                                    backgroundColor = LoyErrorLight,
                                                    textColor = LoyError,
                                                )
                                                else -> PillBadge(
                                                    text = "Out",
                                                    backgroundColor = Color(0xFFFFF3E0),
                                                    textColor = LoyWarning,
                                                )
                                            }
                                        }
                                        val clientEvent = listOfNotNull(
                                            r.customerName?.takeIf { it.isNotEmpty() },
                                            r.eventName.takeIf { it.isNotEmpty() },
                                        ).joinToString(" · ")
                                        Text(
                                            text = "${formatDateTime(r.createdAt)}${if (clientEvent.isNotEmpty()) " · $clientEvent" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    Text(
                                        text = formatCents(r.totalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.isCompleted) LoyTextPrimary else LoyError,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = "Inspect rental",
                                        tint = if (isSelected) LoyGreen else LoyTextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if (windowSize.useSplitPane) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                dashboardContent(Modifier.weight(0.60f).fillMaxHeight())
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                RentalDetailInspectorPane(
                    viewModel = viewModel,
                    receiptWithLines = activeDetail,
                    data = data,
                    onReportDamage = onReportDamage,
                    onOpenFullScreen = onNavigateToReceiptDetail,
                    emptyTitle = "Rental Inspector",
                    emptySubtitle = "Select any rental record or global search result on the left to inspect equipment, contract, and return actions.",
                    modifier = Modifier.weight(0.40f),
                )
            }
        } else {
            dashboardContent(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (showBranchSheet) {
        BranchPickerSheet(
            branches = data.branches,
            activeBranchId = session.activeBranchId,
            onSelect = { viewModel.selectBranch(it.id) },
            onDismiss = { showBranchSheet = false },
        )
    }
}

@Composable
private fun DailyRevenueTrendCard(
    points: List<DailyRevenuePoint>,
    selectedDays: Int,
    currencySymbol: String,
    onSelectDays: (Int) -> Unit,
) {
    val totalWindowCents = remember(points) { points.sumOf { it.totalCents } }
    val totalOrders = remember(points) { points.sumOf { it.salesCount } }
    val avgDailyCents = if (points.isEmpty()) 0L else totalWindowCents / points.size

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(points.map { it.revenueUnits })
                }
            }
        }
    }

    val startAxisFormatter = remember(currencySymbol) {
        CartesianValueFormatter { _, value, _ ->
            formatCentsShort((value * 100.0).roundToLong(), currencySymbol)
        }
    }
    val bottomAxisFormatter = remember(points) {
        CartesianValueFormatter { _, value, _ ->
            points.getOrNull(value.toInt())?.label ?: ""
        }
    }

    GroupCard(modifier = Modifier.testTag("dashboard_revenue_trend_card")) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last $selectedDays days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LoyTextPrimary,
                    )
                    Text(
                        text = "$totalOrders completed ${if (totalOrders == 1) "rental" else "rentals"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { onSelectDays(days) },
                            label = {
                                Text(
                                    text = "${days}D",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreenLight,
                                selectedLabelColor = LoyGreen,
                            ),
                            modifier = Modifier.height(28.dp).testTag("dashboard_trend_${days}d"),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatBlock(
                    label = "${selectedDays}D Revenue",
                    value = formatCents(totalWindowCents, currencySymbol),
                    valueColor = LoyGreen,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "Daily Average",
                    value = formatCents(avgDailyCents, currencySymbol),
                    valueColor = LoyInfo,
                    modifier = Modifier.weight(1f),
                )
            }

            if (points.isNotEmpty()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(fill(LoyGreen)),
                                    areaFill = LineCartesianLayer.AreaFill.single(
                                        fill(
                                            ShaderProvider.verticalGradient(
                                                LoyGreen.copy(alpha = 0.28f).toArgb(),
                                                Color.Transparent.toArgb(),
                                            )
                                        )
                                    ),
                                    pointConnector = LineCartesianLayer.PointConnector.cubic(0.4f),
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = startAxisFormatter,
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomAxisFormatter,
                        ),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .testTag("dashboard_revenue_chart"),
                )
            }

            if (totalWindowCents == 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LoyGreenLight.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = LoyGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "No completed rentals in this period yet. Complete a booking on the Rent Desk to populate the daily revenue trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            }
        }
    }
}
