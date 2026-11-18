package com.loypos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.parseQty
import com.loypos.app.data.local.BackOfficeUserEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.CompactBottomNavBar
import com.loypos.app.ui.components.CompactNavEntry
import com.loypos.app.ui.components.ConfirmDialog
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.InventoryCsvSheet
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.navigation.BackOfficeAdminRoute
import com.loypos.app.ui.navigation.BackOfficeCustomersRoute
import com.loypos.app.ui.navigation.BackOfficeInventoryRoute
import com.loypos.app.ui.navigation.BackOfficeOverviewRoute
import com.loypos.app.ui.navigation.BackOfficeReceiptDetailRoute
import com.loypos.app.ui.navigation.BackOfficeReceiptsRoute
import com.loypos.app.ui.navigation.BackOfficeReportsRoute
import com.loypos.app.ui.navigation.BackOfficeStockRoute
import com.loypos.app.ui.theme.BackOfficeNavy
import com.loypos.app.ui.theme.BackOfficeTeal
import com.loypos.app.ui.theme.BackOfficeTealLight
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.accessibleTouchTarget
import kotlin.reflect.KClass

private data class BackOfficeNavItem(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
    val tag: String,
)

@Composable
fun BackOfficeMainScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(session.message) {
        val msg = session.message
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val topDestinations = remember {
        listOf(
            BackOfficeNavItem(BackOfficeOverviewRoute, BackOfficeOverviewRoute::class, "Overview", Icons.Outlined.Analytics, "bo_nav_overview"),
            BackOfficeNavItem(BackOfficeInventoryRoute, BackOfficeInventoryRoute::class, "Catalog", Icons.Outlined.Inventory2, "bo_nav_catalog"),
            BackOfficeNavItem(BackOfficeStockRoute, BackOfficeStockRoute::class, "Stock", Icons.Outlined.Warehouse, "bo_nav_stock"),
            BackOfficeNavItem(BackOfficeReportsRoute, BackOfficeReportsRoute::class, "Reports", Icons.Outlined.Assessment, "bo_nav_reports"),
            BackOfficeNavItem(BackOfficeAdminRoute, BackOfficeAdminRoute::class, "Admin", Icons.Outlined.AdminPanelSettings, "bo_nav_admin"),
        )
    }

    val showTopChrome = topDestinations.any { entry ->
        currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
    }

    val navigateToTab: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(session.navigateToLowStockTrigger) {
        if (session.navigateToLowStockTrigger > 0) {
            navigateToTab(BackOfficeStockRoute)
            viewModel.consumeLowStockNavigation()
        }
    }

    val windowSize = LocalLoyWindowSize.current
    val useRail = windowSize.useNavigationRail

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
                if (showTopChrome && !useRail) {
                    CompactBottomNavBar(
                        entries = topDestinations.map { entry ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
                            CompactNavEntry(
                                label = entry.label,
                                icon = entry.icon,
                                selected = selected,
                                tag = entry.tag,
                                onClick = { navigateToTab(entry.route) },
                            )
                        },
                        selectedColor = BackOfficeTeal,
                        indicatorColor = BackOfficeTealLight,
                    )
                }
            },
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showTopChrome && useRail) {
                    NavigationRail(
                        containerColor = LoySurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        topDestinations.forEach { entry ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(entry.route) },
                                icon = { Icon(entry.icon, contentDescription = entry.label) },
                                label = { Text(entry.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BackOfficeTeal,
                                    selectedTextColor = BackOfficeTeal,
                                    indicatorColor = BackOfficeTealLight,
                                ),
                                modifier = Modifier.testTag(entry.tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = BackOfficeOverviewRoute,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable<BackOfficeOverviewRoute> {
                            BackOfficeDashboardScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onNavigateToCatalog = { navigateToTab(BackOfficeInventoryRoute) },
                                onNavigateToStock = { navigateToTab(BackOfficeStockRoute) },
                                onNavigateToReports = { navigateToTab(BackOfficeReportsRoute) },
                                onNavigateToCustomers = { navController.navigate(BackOfficeCustomersRoute) },
                                onNavigateToReceipts = { navController.navigate(BackOfficeReceiptsRoute) },
                                onNavigateToAdmin = { navigateToTab(BackOfficeAdminRoute) },
                                onSwitchToPos = { viewModel.switchToPos() },
                                onLockBackOffice = { viewModel.signOutBackOffice() },
                            )
                        }

                        composable<BackOfficeInventoryRoute> {
                            InventoryScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onNavigateToStock = { navigateToTab(BackOfficeStockRoute) },
                            )
                        }

                        composable<BackOfficeStockRoute> {
                            StockScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = null,
                                session = session,
                            )
                        }

                        composable<BackOfficeReportsRoute> {
                            ReportsScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = null,
                            )
                        }

                        composable<BackOfficeAdminRoute> {
                            BackOfficeAdminScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onNavigateToCustomers = { navController.navigate(BackOfficeCustomersRoute) },
                                onNavigateToReceipts = { navController.navigate(BackOfficeReceiptsRoute) },
                                onSwitchToPos = { viewModel.switchToPos() },
                            )
                        }

                        composable<BackOfficeCustomersRoute> {
                            CustomersScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<BackOfficeReceiptsRoute> {
                            ReceiptsScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onOpenReceipt = { receiptId ->
                                    navController.navigate(BackOfficeReceiptDetailRoute(receiptId))
                                },
                            )
                        }

                        composable<BackOfficeReceiptDetailRoute> { entry ->
                            val args = entry.toRoute<BackOfficeReceiptDetailRoute>()
                            ReceiptDetailScreen(
                                viewModel = viewModel,
                                receiptId = args.receiptId,
                                data = data,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackOfficeAdminScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReceipts: () -> Unit,
    onSwitchToPos: () -> Unit,
) {
    val context = LocalContext.current
    val boUser = session.backOfficeUser
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showLockConfirm by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showInventoryCsvSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.updateLowStockAlertSettings(
                enabled = true,
                threshold = data.settings.lowStockThreshold,
                intervalMinutes = data.settings.lowStockIntervalMinutes,
            )
        } else {
            viewModel.showMessage("Notification permission is required for WorkManager alerts")
        }
    }

    val metrics = LocalLoyMetrics.current
    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackOfficeNavy,
                titleContentColor = Color.White,
            ),
            title = { Text("Back Office Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(metrics.screenPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
        ) {
            // Back Office Authenticated User & Lock
            item {
                SectionHeader("Back Office Session")
                GroupCard {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    boUser?.fullName?.ifEmpty { boUser.email } ?: "Back Office Admin",
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    boUser?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            PillBadge(
                                text = boUser?.role?.replaceFirstChar { it.uppercase() } ?: "Admin",
                                backgroundColor = BackOfficeTealLight,
                                textColor = BackOfficeTeal,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onSwitchToPos,
                                modifier = Modifier.weight(1f).heightIn(min = 42.dp).testTag("bo_admin_switch_pos"),
                            ) {
                                Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Open POS")
                            }
                            Button(
                                onClick = { showLockConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LoyError, contentColor = Color.White),
                                modifier = Modifier.weight(1f).heightIn(min = 42.dp).testTag("bo_admin_lock"),
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Lock / Sign Out")
                            }
                        }
                    }
                }
            }

            // Back Office Team Accounts
            item {
                SectionHeader("Back Office Accounts")
                GroupCard {
                    data.backOfficeAdmins.forEachIndexed { idx, admin ->
                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                        val isSelf = admin.id == boUser?.id
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(admin.fullName.ifEmpty { admin.email }, fontWeight = FontWeight.Medium)
                                    if (isSelf) {
                                        PillBadge("You", backgroundColor = BackOfficeTealLight, textColor = BackOfficeTeal)
                                    }
                                }
                                Text(
                                    "${admin.email} · ${admin.role.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            Switch(
                                checked = admin.active,
                                enabled = !isSelf,
                                onCheckedChange = { viewModel.toggleBackOfficeAdminActive(admin.id, it) },
                            )
                            if (!isSelf) {
                                IconButton(
                                    onClick = { viewModel.deleteBackOfficeAdmin(admin.id) },
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove Back Office admin", tint = LoyError, modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { showAddAdminDialog = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("bo_add_admin_row"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Back Office admin", color = BackOfficeTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // WorkManager Low-Stock Monitor Controls
            item {
                SectionHeader("Inventory Monitor (WorkManager)")
                GroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Low-stock notifications", fontWeight = FontWeight.Medium)
                                PillBadge(
                                    text = if (data.settings.lowStockAlertsEnabled) session.lowStockWorkerStatus else "Off",
                                    backgroundColor = BackOfficeTealLight,
                                    textColor = BackOfficeTeal,
                                )
                            }
                            Text(
                                text = if (data.settings.lowStockAlertsEnabled) {
                                    "Monitoring Room every ${data.settings.lowStockIntervalMinutes} min (< ${formatQty(data.settings.lowStockThreshold)})"
                                } else {
                                    "Enable background Room stock checks"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                            if (data.settings.lowStockLastCheckAt > 0L) {
                                Text(
                                    text = "Last scan: ${formatDateTime(data.settings.lowStockLastCheckAt)} · ${data.settings.lowStockLastCount} low/out",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                )
                            }
                        }
                        Switch(
                            checked = data.settings.lowStockAlertsEnabled,
                            onCheckedChange = { enable ->
                                if (!enable) {
                                    viewModel.updateLowStockAlertSettings(false, data.settings.lowStockThreshold)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.updateLowStockAlertSettings(true, data.settings.lowStockThreshold)
                                }
                            },
                            modifier = Modifier.testTag("bo_admin_low_stock_switch"),
                        )
                    }
                    HorizontalDivider(color = LoyBorder)
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Scan interval",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                        Spacer(Modifier.height(5.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15L to "15 min", 30L to "30 min", 60L to "1 hr", 360L to "6 hr").forEach { (mins, label) ->
                                FilterChip(
                                    selected = data.settings.lowStockIntervalMinutes == mins,
                                    onClick = {
                                        viewModel.updateLowStockAlertSettings(
                                            enabled = data.settings.lowStockAlertsEnabled,
                                            threshold = data.settings.lowStockThreshold,
                                            intervalMinutes = mins,
                                        )
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.height(30.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    AdminNavRow(
                        icon = Icons.Outlined.Tune,
                        title = "Low-stock threshold",
                        subtitle = "Alert when tracked stock is below ${formatQty(data.settings.lowStockThreshold)}",
                        onClick = { showThresholdDialog = true },
                    )
                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.runLowStockCheckNow()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("bo_admin_run_check_now"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Run WorkManager stock check now", color = BackOfficeTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Operations & Directory
            item {
                SectionHeader("Operations & Directory")
                GroupCard {
                    AdminNavRow(
                        icon = Icons.Outlined.PeopleOutline,
                        title = "Customer Directory",
                        subtitle = "${data.customers.size} customers & loyalty points",
                        onClick = onNavigateToCustomers,
                    )
                    HorizontalDivider(color = LoyBorder)
                    AdminNavRow(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "Receipts & Refund Audit",
                        subtitle = "${data.receipts.size} transactions recorded across branches",
                        onClick = onNavigateToReceipts,
                    )
                    HorizontalDivider(color = LoyBorder)
                    AdminNavRow(
                        icon = Icons.Outlined.TableView,
                        title = "Bulk Inventory (CSV Import / Export)",
                        subtitle = "Export ${data.items.size} items or bulk-import SKU lists from CSV",
                        onClick = { showInventoryCsvSheet = true },
                        modifier = Modifier.testTag("bo_admin_inventory_csv_row"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    AdminNavRow(
                        icon = Icons.Outlined.CloudSync,
                        title = if (session.syncing) "Syncing with Plesk MySQL..." else "Plesk MySQL Cloud Sync",
                        subtitle = session.lastSyncMessage ?: "Offline-first Room · novelsapps.com.et",
                        onClick = { viewModel.syncCloud() },
                        modifier = Modifier.testTag("bo_admin_plesk_sync_row"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    AdminNavRow(
                        icon = Icons.Outlined.Tune,
                        title = "Store, Branches & POS Staff",
                        subtitle = "${data.branches.size} branches · ${data.staff.size} POS sellers · Tax ${data.settings.taxRatePercent}%",
                        onClick = onSwitchToPos,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showAddAdminDialog) {
        AddBackOfficeAdminDialog(
            onSave = { name, email, pwd, role, onDone ->
                viewModel.addBackOfficeAdmin(name, email, pwd, role) { err ->
                    if (err == null) showAddAdminDialog = false
                    onDone(err)
                }
            },
            onDismiss = { showAddAdminDialog = false },
        )
    }

    if (showInventoryCsvSheet) {
        InventoryCsvSheet(
            viewModel = viewModel,
            data = data,
            onDismiss = { showInventoryCsvSheet = false },
        )
    }

    if (showThresholdDialog) {
        BackOfficeThresholdDialog(
            initialThreshold = data.settings.lowStockThreshold,
            onSave = { threshold ->
                viewModel.updateLowStockAlertSettings(
                    enabled = data.settings.lowStockAlertsEnabled,
                    threshold = threshold,
                    intervalMinutes = data.settings.lowStockIntervalMinutes,
                )
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false },
        )
    }

    if (showLockConfirm) {
        ConfirmDialog(
            title = "Lock Back Office?",
            message = "You will need Back Office credentials to re-enter the Back Office dashboard.",
            confirmLabel = "Lock",
            destructive = true,
            onConfirm = {
                showLockConfirm = false
                viewModel.signOutBackOffice()
            },
            onDismiss = { showLockConfirm = false },
        )
    }
}

@Composable
private fun BackOfficeThresholdDialog(
    initialThreshold: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(formatQty(initialThreshold)) }
    val parsed = parseQty(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Low-Stock Alert Threshold") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "WorkManager will trigger a local notification whenever tracked stock falls below this quantity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Minimum quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(parsed ?: 3.0) },
                enabled = parsed != null,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AdminNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = metrics.minTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = metrics.cardPadding, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BackOfficeTeal, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBackOfficeAdminDialog(
    onSave: (String, String, String, String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(BackOfficeUserEntity.ROLE_MANAGER) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Back Office Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (min 8 chars)") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = role.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(BackOfficeUserEntity.ROLE_ADMIN, BackOfficeUserEntity.ROLE_MANAGER).forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.replaceFirstChar { it.uppercase() }) },
                                onClick = { role = r; expanded = false },
                            )
                        }
                    }
                }
                if (error != null) Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email, password, role) { err -> error = err } },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Add admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}
