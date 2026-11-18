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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.RentalTimeframeFilter
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatCentsShort
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.nowMs
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.core.searchRentalAgreements
import com.loypos.app.core.startOfTodayMs
import com.loypos.app.data.local.DailyRevenuePoint
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.EquipmentUtilizationChartCard
import com.loypos.app.ui.components.GlobalRentalSearchBar
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.MonthlyRentalIncomeChartCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.theme.BackOfficeNavy
import com.loypos.app.ui.theme.BackOfficeTeal
import com.loypos.app.ui.theme.BackOfficeTealLight
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
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class RevenueChartMode { LINE, COLUMNS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackOfficeDashboardScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToCatalog: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReceipts: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSwitchToPos: () -> Unit,
    onLockBackOffice: () -> Unit,
    onNavigateToReceiptDetail: (String) -> Unit = {},
) {
    val sym = data.settings.currencySymbol
    val boUser = session.backOfficeUser
    val selectedBranchId = session.backOfficeBranchFilterId
    var periodIdx by remember { mutableIntStateOf(1) } // 0=Today, 1=7d, 2=30d, 3=All
    var showGoalDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchTimeframe by remember { mutableStateOf(RentalTimeframeFilter.ALL) }
    var selectedRentalId by remember { mutableStateOf<String?>(null) }

    val todaySummary = remember(data.receipts, selectedBranchId) {
        viewModel.computeTodaySummary(data, selectedBranchId)
    }

    val sinceMs = remember(periodIdx) {
        val today = startOfTodayMs()
        when (periodIdx) {
            0 -> today
            1 -> today - 6L * 86_400_000L
            2 -> today - 29L * 86_400_000L
            else -> 0L
        }
    }

    val branchFilteredReceipts = remember(data.receipts, selectedBranchId, sinceMs) {
        data.receipts.filter { r ->
            r.receipt.createdAt >= sinceMs &&
                (selectedBranchId == null || r.receipt.branchId == null || r.receipt.branchId == selectedBranchId)
        }
    }
    val completedSales = remember(branchFilteredReceipts) {
        branchFilteredReceipts.filter { it.receipt.isCompleted }
    }
    val refundedSales = remember(branchFilteredReceipts) {
        branchFilteredReceipts.filter { !it.receipt.isCompleted }
    }

    val itemById = remember(data.items) { data.items.associateBy { it.id } }

    val netRevenueCents = completedSales.sumOf { it.receipt.totalCents }
    val totalCogsCents = remember(completedSales, itemById) {
        completedSales.sumOf { sale ->
            sale.lines.sumOf { line ->
                val unitCost = itemById[line.itemId]?.costCents ?: 0L
                (unitCost * line.qty).roundToLong()
            }
        }
    }
    val grossProfitCents = netRevenueCents - totalCogsCents
    val marginPercent = if (netRevenueCents > 0L) (grossProfitCents * 100.0 / netRevenueCents) else 0.0
    val avgTicketCents = if (completedSales.isEmpty()) 0L else netRevenueCents / completedSales.size

    val trackedItems = remember(data.items, selectedBranchId) {
        data.items.filter { it.availableInBranch(selectedBranchId) }
    }
    val retailValuationCents = remember(trackedItems) {
        trackedItems.filter { it.trackStock && it.stock > 0.0 }
            .sumOf { (it.priceCents * it.stock).roundToLong() }
    }
    val costValuationCents = remember(trackedItems) {
        trackedItems.filter { it.trackStock && it.stock > 0.0 }
            .sumOf { (it.costCents * it.stock).roundToLong() }
    }
    val outOfStockItems = remember(trackedItems) {
        trackedItems.filter { it.trackStock && it.stock <= 0.0 }
    }
    val lowStockItems = remember(trackedItems, data.settings.lowStockThreshold) {
        val th = data.settings.lowStockThreshold
        trackedItems.filter { it.trackStock && it.stock > 0.0 && it.isBelowSafetyThreshold(th) }
            .sortedBy { it.stock - it.effectiveSafetyThreshold(th) }
    }

    // Branch breakdown across all completed sales in period
    val branchPerformance = remember(data.receipts, data.branches, sinceMs) {
        val periodCompleted = data.receipts.filter { it.receipt.isCompleted && it.receipt.createdAt >= sinceMs }
        data.branches.map { branch ->
            val sales = periodCompleted.filter { it.receipt.branchId == branch.id }
            Triple(branch.name, sales.size, sales.sumOf { it.receipt.totalCents })
        }.sortedByDescending { it.third }
    }

    // Cashier / seller performance in selected branch + period
    val cashierPerformance = remember(completedSales) {
        completedSales.groupBy { it.receipt.cashierName?.ifEmpty { "Owner / Terminal" } ?: "Owner / Terminal" }
            .map { (name, list) ->
                Triple(name, list.size, list.sumOf { it.receipt.totalCents })
            }
            .sortedByDescending { it.third }
            .take(5)
    }

    val trendDays by viewModel.backOfficeTrendDays.collectAsStateWithLifecycle()
    val boTrend by viewModel.backOfficeRevenueTrend.collectAsStateWithLifecycle()
    val monthlyMonths by viewModel.monthlyIncomeMonths.collectAsStateWithLifecycle()
    var utilByCategory by remember { mutableStateOf(true) }
    var utilLiveMode by remember { mutableStateOf(true) }
    val monthlyIncomePoints = remember(data.receipts, monthlyMonths, selectedBranchId) {
        viewModel.computeMonthlyRentalIncome(data, monthlyMonths, selectedBranchId)
    }
    val utilByCategoryPoints = remember(data.items, data.categories, data.receipts, selectedBranchId, utilLiveMode) {
        viewModel.computeEquipmentUtilization(data, selectedBranchId, byCategory = true, liveOutMode = utilLiveMode)
    }
    val utilTopItemPoints = remember(data.items, data.categories, data.receipts, selectedBranchId, utilLiveMode) {
        viewModel.computeEquipmentUtilization(data, selectedBranchId, byCategory = false, liveOutMode = utilLiveMode)
    }
    val activeBranchLabel = remember(data.branches, selectedBranchId) {
        if (selectedBranchId == null) "All Branches"
        else data.branches.firstOrNull { it.id == selectedBranchId }?.name ?: "Selected Branch"
    }
    val boSearchOutput = remember(
        searchQuery,
        searchTimeframe,
        data.receipts,
        data.tickets,
        data.items,
        data.customers,
        data.equipmentUnits,
        data.equipmentScanLogs,
        selectedBranchId,
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
            branchId = selectedBranchId,
            nowMs = nowMs(),
        )
    }
    val windowSize = LocalLoyWindowSize.current
    val metrics = LocalLoyMetrics.current
    val currentTimeMs = remember(data.receipts) { nowMs() }
    val branchRecentReceipts = remember(data.receipts, selectedBranchId) {
        data.receipts.filter { r ->
            selectedBranchId == null || r.receipt.branchId == null || r.receipt.branchId == selectedBranchId
        }
    }
    val activeDetail = remember(data.receipts, branchRecentReceipts, selectedRentalId) {
        data.receipts.firstOrNull { it.receipt.id == selectedRentalId } ?: branchRecentReceipts.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackOfficeNavy,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = data.settings.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = BackOfficeTeal,
                        ) {
                            Text(
                                text = "BACK OFFICE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(
                        text = "${boUser?.fullName?.ifEmpty { boUser.email } ?: "Admin"} · ${boUser?.role?.replaceFirstChar { it.uppercase() } ?: "Admin"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = onSwitchToPos,
                    modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("bo_switch_to_pos_button"),
                ) {
                    Icon(Icons.Outlined.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("POS", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                IconButton(
                    onClick = { viewModel.syncCloud() },
                    enabled = !session.syncing,
                    modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget),
                ) {
                    if (session.syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Outlined.CloudSync, contentDescription = "Sync Plesk MySQL", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                IconButton(
                    onClick = onLockBackOffice,
                    modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("bo_lock_button"),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Lock Back Office", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            },
        )

        // Branch & Period filter ribbon
        Surface(color = LoySurface) {
            Column {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedBranchId == null,
                            onClick = { viewModel.selectBackOfficeBranch(null) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Storefront, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("All Branches", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BackOfficeTealLight,
                                selectedLabelColor = BackOfficeTeal,
                                selectedLeadingIconColor = BackOfficeTeal,
                            ),
                            modifier = Modifier.height(30.dp).testTag("bo_branch_all"),
                        )
                    }
                    items(data.branches, key = { it.id }) { branch ->
                        val sel = selectedBranchId == branch.id
                        FilterChip(
                            selected = sel,
                            onClick = { viewModel.selectBackOfficeBranch(if (sel) null else branch.id) },
                            label = { Text(branch.name, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BackOfficeTealLight,
                                selectedLabelColor = BackOfficeTeal,
                            ),
                            modifier = Modifier.height(30.dp),
                        )
                    }
                }
                HorizontalDivider(color = LoyBorder)
            }
        }

        val boMainContent: @Composable (Modifier) -> Unit = { contentModifier ->
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
                        searchOutput = boSearchOutput,
                        currencySymbol = sym,
                        onSelectAgreement = { result ->
                            if (result.isHeldQuote) {
                                onSwitchToPos()
                            } else if (windowSize.useSplitPane) {
                                selectedRentalId = result.id
                            } else {
                                onNavigateToReceiptDetail(result.id)
                            }
                        },
                        onViewAllRentals = onNavigateToReceipts,
                        accentColor = BackOfficeTeal,
                        accentLight = BackOfficeTealLight,
                        testTagPrefix = "bo_dashboard_search",
                    )
                }

            // Period filter + Executive Financial Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("Financial Performance")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Today", "7D", "30D", "All").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = periodIdx == idx,
                                onClick = { periodIdx = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BackOfficeNavy,
                                    selectedLabelColor = Color.White,
                                ),
                                modifier = Modifier.height(metrics.chipHeight),
                            )
                        }
                    }
                }
                GroupCard(modifier = Modifier.testTag("bo_financial_card")) {
                    Column(modifier = Modifier.padding(metrics.cardPadding), verticalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                        if (windowSize.isCompact) {
                            Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                StatBlock(
                                    label = "Net Revenue",
                                    value = formatCents(netRevenueCents, sym),
                                    valueColor = BackOfficeTeal,
                                    modifier = Modifier.weight(1f),
                                )
                                StatBlock(
                                    label = "Gross Profit (${formatQty(marginPercent)}%)",
                                    value = formatCents(grossProfitCents, sym),
                                    valueColor = if (grossProfitCents >= 0L) LoyGreen else LoyError,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                StatBlock(
                                    label = "Bookings / Refunds",
                                    value = "${completedSales.size} / ${refundedSales.size}",
                                    modifier = Modifier.weight(1f),
                                )
                                StatBlock(
                                    label = "Avg Booking",
                                    value = formatCents(avgTicketCents, sym),
                                    valueColor = LoyInfo,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                                StatBlock(
                                    label = "Net Revenue",
                                    value = formatCents(netRevenueCents, sym),
                                    valueColor = BackOfficeTeal,
                                    modifier = Modifier.weight(1f),
                                )
                                StatBlock(
                                    label = "Gross Margin (${formatQty(marginPercent)}%)",
                                    value = formatCents(grossProfitCents, sym),
                                    valueColor = if (grossProfitCents >= 0L) LoyGreen else LoyError,
                                    modifier = Modifier.weight(1f),
                                )
                                StatBlock(
                                    label = "Bookings / Refunds",
                                    value = "${completedSales.size} / ${refundedSales.size}",
                                    modifier = Modifier.weight(1f),
                                )
                                StatBlock(
                                    label = "Avg Booking",
                                    value = formatCents(avgTicketCents, sym),
                                    valueColor = LoyInfo,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                            OutlinedButton(
                                onClick = onNavigateToReports,
                                modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget).testTag("bo_open_reports_btn"),
                            ) {
                                Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Reports & CSV")
                            }
                            OutlinedButton(
                                onClick = onNavigateToReceipts,
                                modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget).testTag("bo_open_receipts_btn"),
                            ) {
                                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Rental Ledger")
                            }
                        }

                        HorizontalDivider(color = LoyBorder)
                        // Daily Revenue Goal progress bar
                        val targetCents = data.settings.dailyRevenueTargetCents
                        val todayNet = todaySummary.netCents
                        val goalRatio = if (targetCents > 0L) (todayNet.toFloat() / targetCents.toFloat()).coerceIn(0f, 1f) else 0f
                        val goalPct = if (targetCents > 0L) ((todayNet * 100.0) / targetCents).roundToInt() else 0
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = if (targetCents > 0L) {
                                            "Today's Goal: ${formatCents(todayNet, sym)} / ${formatCents(targetCents, sym)} ($goalPct%)"
                                        } else {
                                            "Daily Revenue Goal: Not set"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = LoyTextPrimary,
                                    )
                                }
                                TextButton(
                                    onClick = { showGoalDialog = true },
                                    modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("bo_set_goal_button"),
                                ) {
                                    Text(if (targetCents > 0L) "Edit" else "Set Goal", color = BackOfficeTeal)
                                }
                            }
                            if (targetCents > 0L) {
                                LinearProgressIndicator(
                                    progress = { goalRatio },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                                    color = if (goalRatio >= 1f) LoyGreen else BackOfficeTeal,
                                    trackColor = LoySurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Daily Revenue Trend Chart (Vico)
            item {
                SectionHeader("Daily Revenue Trends")
                BackOfficeRevenueChartCard(
                    points = boTrend,
                    selectedDays = trendDays,
                    branchLabel = activeBranchLabel,
                    currencySymbol = sym,
                    onSelectDays = viewModel::setBackOfficeTrendDays,
                )
            }

            // Monthly Rental Income & Equipment Utilization Charts (Vico)
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
                                accentColor = BackOfficeTeal,
                                accentLight = BackOfficeTealLight,
                                subtitlePrefix = activeBranchLabel,
                                testTagPrefix = "bo_monthly_income",
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
                                accentColor = BackOfficeNavy,
                                accentLight = BackOfficeTealLight,
                                testTagPrefix = "bo_utilization",
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
                        accentColor = BackOfficeTeal,
                        accentLight = BackOfficeTealLight,
                        subtitlePrefix = activeBranchLabel,
                        testTagPrefix = "bo_monthly_income",
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
                        accentColor = BackOfficeNavy,
                        accentLight = BackOfficeTealLight,
                        testTagPrefix = "bo_utilization",
                    )
                }
            }

            // Inventory Valuation & Alerts
            item {
                SectionHeader("Fleet Valuation & Health")
                GroupCard(modifier = Modifier.testTag("bo_inventory_health_card")) {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            StatBlock(
                                label = "Daily Fleet Rate Value",
                                value = formatCents(retailValuationCents, sym),
                                valueColor = BackOfficeNavy,
                                modifier = Modifier.weight(1f),
                            )
                            StatBlock(
                                label = "Fleet Unit Cost Value",
                                value = formatCents(costValuationCents, sym),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (outOfStockItems.isNotEmpty() || lowStockItems.isNotEmpty()) Color(0xFFFFF3E0) else BackOfficeTealLight),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (outOfStockItems.isNotEmpty() || lowStockItems.isNotEmpty()) Icons.Outlined.WarningAmber else Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = if (outOfStockItems.isNotEmpty()) LoyWarning else BackOfficeTeal,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${trackedItems.size} active equipment SKUs · ${data.categories.size} categories",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${outOfStockItems.size} rented out · ${lowStockItems.size} below ${formatQty(data.settings.lowStockThreshold)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (outOfStockItems.isNotEmpty()) LoyError else LoyTextSecondary,
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (data.settings.lowStockAlertsEnabled) BackOfficeTeal else LoyTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (data.settings.lowStockAlertsEnabled) {
                                        "WorkManager: ${session.lowStockWorkerStatus} · Every ${data.settings.lowStockIntervalMinutes}m"
                                    } else {
                                        "WorkManager fleet monitor off"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (data.settings.lowStockLastCheckAt > 0L) {
                                    Text(
                                        text = "Last scan: ${formatDateTime(data.settings.lowStockLastCheckAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextSecondary,
                                    )
                                }
                            }
                            TextButton(
                                onClick = { viewModel.runLowStockCheckNow() },
                                modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("bo_dash_run_stock_check"),
                            ) {
                                Text("Run Check", color = BackOfficeTeal, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                            OutlinedButton(
                                onClick = onNavigateToCatalog,
                                modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget),
                            ) {
                                Text("Manage Equipment")
                            }
                            OutlinedButton(
                                onClick = onNavigateToStock,
                                modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget),
                            ) {
                                Text("Adjust Fleet")
                            }
                        }
                    }
                }
            }

            // Branch Performance + Team Activity (side-by-side on Expanded)
            if (windowSize.isExpanded) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (branchPerformance.isNotEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                SectionHeader("Branch Performance")
                                GroupCard {
                                    val maxBranchRev = branchPerformance.maxOf { it.third }.coerceAtLeast(1L)
                                    Column(modifier = Modifier.padding(metrics.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        branchPerformance.forEach { (name, count, rev) ->
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("$name ($count orders)", fontWeight = FontWeight.Medium)
                                                    Text(
                                                        formatCents(rev, sym),
                                                        style = TabularMoneyStyle,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BackOfficeNavy,
                                                    )
                                                }
                                                LinearProgressIndicator(
                                                    progress = { rev.toFloat() / maxBranchRev.toFloat() },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                                    color = BackOfficeTeal,
                                                    trackColor = LoySurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            SectionHeader("Team & Cashier Activity")
                            GroupCard {
                                if (cashierPerformance.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Outlined.Badge,
                                        title = "No cashier sales in this window",
                                        subtitle = "Completed sales from POS terminals will show seller attribution here.",
                                    )
                                } else {
                                    Column {
                                        cashierPerformance.forEachIndexed { idx, (name, count, rev) ->
                                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = metrics.cardPadding, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(Icons.Outlined.Badge, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(19.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, fontWeight = FontWeight.SemiBold)
                                                    Text("$count orders completed", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                                }
                                                Text(formatCents(rev, sym), style = TabularMoneyStyle, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Branch Performance Breakdown
                if (branchPerformance.isNotEmpty()) {
                    item {
                        SectionHeader("Branch Performance")
                        GroupCard {
                            val maxBranchRev = branchPerformance.maxOf { it.third }.coerceAtLeast(1L)
                            Column(modifier = Modifier.padding(metrics.cardPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                branchPerformance.forEach { (name, count, rev) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("$name ($count orders)", fontWeight = FontWeight.Medium)
                                            Text(
                                                formatCents(rev, sym),
                                                style = TabularMoneyStyle,
                                                fontWeight = FontWeight.Bold,
                                                color = BackOfficeNavy,
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { rev.toFloat() / maxBranchRev.toFloat() },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                            color = BackOfficeTeal,
                                            trackColor = LoySurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Staff / Seller Performance
                item {
                    SectionHeader("Team & Cashier Activity")
                    GroupCard {
                        if (cashierPerformance.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Badge,
                                title = "No cashier sales in this window",
                                subtitle = "Completed sales from POS terminals will show seller attribution here.",
                            )
                        } else {
                            Column {
                                cashierPerformance.forEachIndexed { idx, (name, count, rev) ->
                                    if (idx > 0) HorizontalDivider(color = LoyBorder)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = metrics.cardPadding, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Outlined.Badge, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontWeight = FontWeight.SemiBold)
                                            Text("$count orders completed", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                        }
                                        Text(formatCents(rev, sym), style = TabularMoneyStyle, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Restock Watchlist
            if (outOfStockItems.isNotEmpty() || lowStockItems.isNotEmpty()) {
                item {
                    SectionHeader("Restock Watchlist")
                    GroupCard {
                        val alerts = (outOfStockItems + lowStockItems).take(5)
                        Column {
                            alerts.forEachIndexed { idx, item ->
                                if (idx > 0) HorizontalDivider(color = LoyBorder)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToStock() }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val itemMin = item.effectiveSafetyThreshold(data.settings.lowStockThreshold)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${if (item.sku.isNotEmpty()) "SKU ${item.sku} · " else ""}Safety min ${formatQty(itemMin)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    PillBadge(
                                        text = if (item.stock <= 0.0) "Out" else "${formatQty(item.stock)} / ${formatQty(itemMin)}",
                                        backgroundColor = if (item.stock <= 0.0) LoyErrorLight else Color(0xFFFFF3E0),
                                        textColor = if (item.stock <= 0.0) LoyError else LoyWarning,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Rental Ledger (clickable Master-Detail selection on desktop/expanded)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader("Recent Rentals")
                    if (branchRecentReceipts.isNotEmpty()) {
                        TextButton(
                            onClick = onNavigateToReceipts,
                            modifier = Modifier.heightIn(min = metrics.minTouchTarget),
                        ) {
                            Text("View ledger", color = BackOfficeTeal)
                        }
                    }
                }
                GroupCard {
                    val recent = branchRecentReceipts.take(5)
                    if (recent.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "No rental orders yet",
                            subtitle = "Completed rentals will appear here for executive audit.",
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
                                        .background(if (isSelected) BackOfficeTealLight.copy(alpha = 0.55f) else Color.Transparent)
                                        .clickable {
                                            if (windowSize.useSplitPane) {
                                                selectedRentalId = r.id
                                            } else {
                                                onNavigateToReceiptDetail(r.id)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("bo_recent_sale_${r.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (windowSize.useSplitPane) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isSelected) BackOfficeTeal else Color.Transparent),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("#${r.number.toString().padStart(4, '0')} · ${r.rentalDays}d", fontWeight = FontWeight.SemiBold)
                                            when {
                                                !r.isCompleted -> PillBadge("Refunded", LoyErrorLight, LoyError)
                                                r.isReturned -> PillBadge("Returned", LoyGreenLight, LoyGreen)
                                                r.isOverdue(currentTimeMs) -> PillBadge("Overdue", LoyErrorLight, LoyError)
                                                else -> PillBadge("Out", BackOfficeTealLight, BackOfficeTeal)
                                            }
                                        }
                                        Text(
                                            text = "${formatDateTime(r.createdAt)}${if (!r.customerName.isNullOrEmpty()) " · ${r.customerName}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    Text(
                                        text = formatCents(r.totalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.isCompleted) BackOfficeNavy else LoyError,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = "Inspect rental",
                                        tint = if (isSelected) BackOfficeTeal else LoyTextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Back Office Management shortcuts
            item {
                SectionHeader("Back Office Shortcuts")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onNavigateToCustomers),
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.PeopleOutline, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(19.dp))
                            Column {
                                Text("Clients", fontWeight = FontWeight.SemiBold)
                                Text("${data.customers.size} profiles", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onNavigateToAdmin),
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = BackOfficeNavy, modifier = Modifier.size(19.dp))
                            Column {
                                Text("Admin & Team", fontWeight = FontWeight.SemiBold)
                                Text("${data.backOfficeAdmins.size} admins", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        }

        if (windowSize.useSplitPane) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                boMainContent(Modifier.weight(0.60f).fillMaxHeight())
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                RentalDetailInspectorPane(
                    viewModel = viewModel,
                    receiptWithLines = activeDetail,
                    data = data,
                    onOpenFullScreen = onNavigateToReceiptDetail,
                    emptyTitle = "Rental Audit Inspector",
                    emptySubtitle = "Click any rental in Recent Rentals or Global Search to view line items, agreement, and return status.",
                    modifier = Modifier.weight(0.40f),
                )
            }
        } else {
            boMainContent(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (showGoalDialog) {
        DailyGoalDialog(
            initialTargetCents = data.settings.dailyRevenueTargetCents,
            currencySymbol = sym,
            onSave = { targetCents ->
                viewModel.updateDailyRevenueTarget(targetCents)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false },
        )
    }
}

@Composable
private fun DailyGoalDialog(
    initialTargetCents: Long,
    currencySymbol: String,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember {
        mutableStateOf(
            if (initialTargetCents <= 0L) ""
            else if (initialTargetCents % 100L == 0L) (initialTargetCents / 100L).toString()
            else "%.2f".format(initialTargetCents / 100.0)
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Revenue Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set a daily sales target ($currencySymbol) to track store goal progress on the Back Office dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Target ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bo_daily_goal_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseAmountToCents(text) ?: 0L
                    onSave(cents)
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("bo_daily_goal_save"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (initialTargetCents > 0L) {
                    TextButton(onClick = { onSave(0L) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Clear", color = LoyError)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun BackOfficeRevenueChartCard(
    points: List<DailyRevenuePoint>,
    selectedDays: Int,
    branchLabel: String,
    currencySymbol: String,
    onSelectDays: (Int) -> Unit,
) {
    var chartMode by remember { mutableStateOf(RevenueChartMode.LINE) }

    val totalWindowCents = remember(points) { points.sumOf { it.totalCents } }
    val totalOrders = remember(points) { points.sumOf { it.salesCount } }
    val avgDailyCents = if (points.isEmpty()) 0L else totalWindowCents / points.size
    val peakPoint = remember(points) {
        points.filter { it.totalCents > 0L }.maxByOrNull { it.totalCents }
    }
    val momentumPercent = remember(points) {
        if (points.size < 4) return@remember null
        val mid = points.size / 2
        val firstHalf = points.take(mid).sumOf { it.totalCents }
        val secondHalf = points.drop(mid).sumOf { it.totalCents }
        if (firstHalf <= 0L) null
        else (((secondHalf - firstHalf) * 100.0) / firstHalf).roundToInt()
    }

    val lineModelProducer = remember { CartesianChartModelProducer() }
    val columnModelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            val values = points.map { it.revenueUnits }
            lineModelProducer.runTransaction {
                lineSeries { series(values) }
            }
            columnModelProducer.runTransaction {
                columnSeries { series(values) }
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
    val xLabelSpacing = remember(selectedDays) {
        when {
            selectedDays <= 7 -> 1
            selectedDays <= 14 -> 2
            else -> 5
        }
    }

    val markerLabel = rememberTextComponent(
        color = Color.White,
        background = rememberShapeComponent(
            fill = fill(BackOfficeNavy),
            shape = CorneredShape.rounded(allPercent = 25),
        ),
        padding = insets(horizontal = 10.dp, vertical = 6.dp),
    )
    val markerIndicator = rememberShapeComponent(
        fill = fill(BackOfficeTeal),
        shape = CorneredShape.Pill,
    )
    val markerGuideline = rememberLineComponent(
        fill = fill(BackOfficeTeal.copy(alpha = 0.45f)),
        thickness = 1.dp,
    )
    val markerFormatter = remember(points, currencySymbol) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val firstTarget = targets.firstOrNull()
            val idx = firstTarget?.x?.roundToInt()
            val pt = if (idx != null) points.getOrNull(idx) else null
            if (pt != null) {
                "${pt.label}: ${formatCents(pt.totalCents, currencySymbol)} (${pt.salesCount})"
            } else {
                ""
            }
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerFormatter,
        indicator = { _ -> markerIndicator },
        indicatorSize = 10.dp,
        guideline = markerGuideline,
    )

    GroupCard(modifier = Modifier.testTag("bo_revenue_chart_card")) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Last $selectedDays days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LoyTextPrimary,
                        )
                        if (momentumPercent != null) {
                            val positive = momentumPercent >= 0
                            PillBadge(
                                text = "${if (positive) "+" else ""}$momentumPercent% trend",
                                backgroundColor = if (positive) LoyGreenLight else LoyErrorLight,
                                textColor = if (positive) LoyGreen else LoyError,
                            )
                        }
                    }
                    Text(
                        text = "$branchLabel · $totalOrders completed ${if (totalOrders == 1) "order" else "orders"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = selectedDays == days,
                            onClick = { onSelectDays(days) },
                            label = { Text("${days}D", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BackOfficeTealLight,
                                selectedLabelColor = BackOfficeTeal,
                            ),
                            modifier = Modifier.height(28.dp).testTag("bo_trend_${days}d"),
                        )
                    }
                }
            }

            // KPI summary blocks (2-column row + Peak Day highlight to prevent mobile truncation)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBlock(
                    label = "${selectedDays}D Revenue",
                    value = formatCents(totalWindowCents, currencySymbol),
                    valueColor = BackOfficeTeal,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "Daily Average",
                    value = formatCents(avgDailyCents, currencySymbol),
                    valueColor = LoyInfo,
                    modifier = Modifier.weight(1f),
                )
            }
            if (peakPoint != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BackOfficeTealLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Best Day · ${peakPoint.label} (${peakPoint.salesCount} ${if (peakPoint.salesCount == 1) "order" else "orders"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = BackOfficeNavy,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = formatCents(peakPoint.totalCents, currencySymbol),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                            color = BackOfficeTeal,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Vico chart type selector (Area Line vs Daily Bars)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = chartMode == RevenueChartMode.LINE,
                    onClick = { chartMode = RevenueChartMode.LINE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.testTag("bo_chart_mode_line"),
                ) {
                    Text("Trend Line")
                }
                SegmentedButton(
                    selected = chartMode == RevenueChartMode.COLUMNS,
                    onClick = { chartMode = RevenueChartMode.COLUMNS },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.testTag("bo_chart_mode_bar"),
                ) {
                    Text("Daily Bars")
                }
            }

            if (points.isNotEmpty()) {
                val pointComponent = rememberShapeComponent(
                    fill = fill(BackOfficeTeal),
                    shape = CorneredShape.Pill,
                )
                val lineChart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(BackOfficeTeal)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill(
                                        ShaderProvider.verticalGradient(
                                            BackOfficeTeal.copy(alpha = 0.28f).toArgb(),
                                            Color.Transparent.toArgb(),
                                        )
                                    )
                                ),
                                pointProvider = if (selectedDays <= 14) {
                                    LineCartesianLayer.PointProvider.single(
                                        LineCartesianLayer.Point(
                                            component = pointComponent,
                                            sizeDp = 6f,
                                        )
                                    )
                                } else {
                                    null
                                },
                                pointConnector = LineCartesianLayer.PointConnector.cubic(0.4f),
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { xLabelSpacing }),
                    ),
                    marker = marker,
                )

                val columnChart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(
                                fill = fill(BackOfficeTeal),
                                thickness = if (selectedDays <= 7) 18.dp else if (selectedDays <= 14) 11.dp else 6.dp,
                                shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30),
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { xLabelSpacing }),
                    ),
                    marker = marker,
                )

                CartesianChartHost(
                    chart = if (chartMode == RevenueChartMode.LINE) lineChart else columnChart,
                    modelProducer = if (chartMode == RevenueChartMode.LINE) lineModelProducer else columnModelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(154.dp)
                        .testTag("bo_revenue_chart_host"),
                )
            }

            if (totalWindowCents == 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackOfficeTealLight.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(18.dp))
                    Text(
                        text = "No completed rentals in this period. Book an event rental on the Rent Desk to visualize daily revenue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            } else {
                Text(
                    text = "Tip: Touch or drag across the chart to inspect daily revenue and order count.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LoyTextSecondary,
                )
            }
        }
    }
}
