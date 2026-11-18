package com.loypos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.loypos.app.ui.AuthStage
import com.loypos.app.ui.components.GettingStartedOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.navigation.AccountRoute
import com.loypos.app.ui.navigation.AgreementsRoute
import com.loypos.app.ui.navigation.CalendarRoute
import com.loypos.app.ui.navigation.CartRoute
import com.loypos.app.ui.navigation.CheckoutRoute
import com.loypos.app.ui.navigation.CustomersRoute
import com.loypos.app.ui.navigation.DamageReportsRoute
import com.loypos.app.ui.navigation.DashboardRoute
import com.loypos.app.ui.navigation.EventKitsRoute
import com.loypos.app.ui.navigation.InventoryRoute
import com.loypos.app.ui.navigation.MaintenanceRoute
import com.loypos.app.ui.navigation.PaymentRoute
import com.loypos.app.ui.navigation.QrTrackingRoute
import com.loypos.app.ui.navigation.ReceiptDetailRoute
import com.loypos.app.ui.navigation.ReceiptSuccessRoute
import com.loypos.app.ui.navigation.ReceiptsRoute
import com.loypos.app.ui.navigation.ReportsRoute
import com.loypos.app.ui.navigation.SettingsRoute
import com.loypos.app.ui.navigation.ShiftRoute
import com.loypos.app.ui.navigation.StockRoute
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val group: String = "OPERATIONS",
    val matchClasses: List<KClass<*>> = listOf(routeClass),
)

@Composable
fun MainScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
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
    val txMessage by transactionViewModel.transactionMessage.collectAsStateWithLifecycle()
    LaunchedEffect(txMessage) {
        val msg = txMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            transactionViewModel.consumeMessage()
        }
    }

    val isOwner = session.currentUser?.isOwner != false
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(session.navigateToLowStockTrigger, isOwner) {
        if (session.navigateToLowStockTrigger > 0) {
            if (isOwner) {
                navController.navigate(InventoryRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(session.navigateToReturnsTrigger, session.navigateToReturnReceiptId) {
        if (session.navigateToReturnsTrigger > 0) {
            val targetId = session.navigateToReturnReceiptId
            if (!targetId.isNullOrBlank()) {
                navController.navigate(ReceiptDetailRoute(targetId)) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(ReceiptsRoute) {
                    launchSingleTop = true
                }
            }
            viewModel.consumeReturnReminderNavigation()
        }
    }

    val topLevelDestinations = remember(isOwner) {
        if (isOwner) {
            listOf(
                TopLevelDestination(DashboardRoute, DashboardRoute::class, "Dashboard", Icons.Outlined.Dashboard, "nav_dashboard"),
                TopLevelDestination(
                    route = CheckoutRoute,
                    routeClass = CheckoutRoute::class,
                    label = "Rent Desk",
                    icon = Icons.Outlined.PointOfSale,
                    tag = "nav_checkout",
                    matchClasses = listOf(CheckoutRoute::class, CartRoute::class, PaymentRoute::class, ReceiptSuccessRoute::class),
                ),
                TopLevelDestination(InventoryRoute, InventoryRoute::class, "Equipment", Icons.Outlined.Inventory2, "nav_inventory"),
                TopLevelDestination(
                    route = ReceiptsRoute,
                    routeClass = ReceiptsRoute::class,
                    label = "Rentals",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = "nav_receipts",
                    matchClasses = listOf(ReceiptsRoute::class, ReceiptDetailRoute::class),
                ),
                TopLevelDestination(SettingsRoute, SettingsRoute::class, "Settings", Icons.Outlined.Settings, "nav_settings"),
            )
        } else {
            listOf(
                TopLevelDestination(
                    route = CheckoutRoute,
                    routeClass = CheckoutRoute::class,
                    label = "Rent Desk",
                    icon = Icons.Outlined.PointOfSale,
                    tag = "nav_checkout",
                    matchClasses = listOf(CheckoutRoute::class, CartRoute::class, PaymentRoute::class, ReceiptSuccessRoute::class),
                ),
                TopLevelDestination(
                    route = ReceiptsRoute,
                    routeClass = ReceiptsRoute::class,
                    label = "Rentals",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = "nav_receipts",
                    matchClasses = listOf(ReceiptsRoute::class, ReceiptDetailRoute::class),
                ),
                TopLevelDestination(ShiftRoute, ShiftRoute::class, "Shift", Icons.Outlined.AccessTime, "nav_shift"),
                TopLevelDestination(AccountRoute, AccountRoute::class, "Account", Icons.Outlined.PersonOutline, "nav_account"),
            )
        }
    }

    val sidebarDestinations = remember(isOwner) {
        if (isOwner) {
            listOf(
                TopLevelDestination(DashboardRoute, DashboardRoute::class, "Dashboard", Icons.Outlined.Dashboard, "nav_dashboard", "OPERATIONS"),
                TopLevelDestination(
                    route = CheckoutRoute,
                    routeClass = CheckoutRoute::class,
                    label = "Rent Desk",
                    icon = Icons.Outlined.PointOfSale,
                    tag = "nav_checkout",
                    group = "OPERATIONS",
                    matchClasses = listOf(CheckoutRoute::class, CartRoute::class, PaymentRoute::class, ReceiptSuccessRoute::class),
                ),
                TopLevelDestination(
                    route = ReceiptsRoute,
                    routeClass = ReceiptsRoute::class,
                    label = "Rentals",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = "nav_receipts",
                    group = "OPERATIONS",
                    matchClasses = listOf(ReceiptsRoute::class, ReceiptDetailRoute::class),
                ),
                TopLevelDestination(CalendarRoute, CalendarRoute::class, "Schedule", Icons.Outlined.CalendarMonth, "nav_schedule", "OPERATIONS"),
                TopLevelDestination(InventoryRoute, InventoryRoute::class, "Equipment", Icons.Outlined.Inventory2, "nav_inventory", "FLEET & LOGS"),
                TopLevelDestination(StockRoute, StockRoute::class, "Fleet Stock", Icons.Outlined.Warehouse, "nav_stock", "FLEET & LOGS"),
                TopLevelDestination(EventKitsRoute, EventKitsRoute::class, "Event Kits", Icons.Outlined.Layers, "nav_event_kits", "FLEET & LOGS"),
                TopLevelDestination(QrTrackingRoute(), QrTrackingRoute::class, "QR Track", Icons.Outlined.QrCodeScanner, "nav_qr_track", "FLEET & LOGS"),
                TopLevelDestination(DamageReportsRoute(), DamageReportsRoute::class, "Damage Log", Icons.Outlined.ReportProblem, "nav_damage_log", "FLEET & LOGS"),
                TopLevelDestination(MaintenanceRoute(), MaintenanceRoute::class, "Maintenance", Icons.Outlined.Build, "nav_maintenance", "FLEET & LOGS"),
                TopLevelDestination(AgreementsRoute, AgreementsRoute::class, "Contracts", Icons.Outlined.Description, "nav_contracts", "FLEET & LOGS"),
                TopLevelDestination(CustomersRoute, CustomersRoute::class, "Clients", Icons.Outlined.PeopleOutline, "nav_clients", "DIRECTORY"),
                TopLevelDestination(ReportsRoute, ReportsRoute::class, "Reports", Icons.Outlined.Assessment, "nav_reports", "DIRECTORY"),
                TopLevelDestination(ShiftRoute, ShiftRoute::class, "Shift", Icons.Outlined.AccessTime, "nav_shift", "DIRECTORY"),
                TopLevelDestination(SettingsRoute, SettingsRoute::class, "Settings", Icons.Outlined.Settings, "nav_settings", "DIRECTORY"),
            )
        } else {
            listOf(
                TopLevelDestination(
                    route = CheckoutRoute,
                    routeClass = CheckoutRoute::class,
                    label = "Rent Desk",
                    icon = Icons.Outlined.PointOfSale,
                    tag = "nav_checkout",
                    group = "OPERATIONS",
                    matchClasses = listOf(CheckoutRoute::class, CartRoute::class, PaymentRoute::class, ReceiptSuccessRoute::class),
                ),
                TopLevelDestination(
                    route = ReceiptsRoute,
                    routeClass = ReceiptsRoute::class,
                    label = "Rentals",
                    icon = Icons.Outlined.ReceiptLong,
                    tag = "nav_receipts",
                    group = "OPERATIONS",
                    matchClasses = listOf(ReceiptsRoute::class, ReceiptDetailRoute::class),
                ),
                TopLevelDestination(CalendarRoute, CalendarRoute::class, "Schedule", Icons.Outlined.CalendarMonth, "nav_schedule", "OPERATIONS"),
                TopLevelDestination(EventKitsRoute, EventKitsRoute::class, "Event Kits", Icons.Outlined.Layers, "nav_event_kits", "OPERATIONS"),
                TopLevelDestination(ShiftRoute, ShiftRoute::class, "Shift", Icons.Outlined.AccessTime, "nav_shift", "STAFF"),
                TopLevelDestination(AccountRoute, AccountRoute::class, "Account", Icons.Outlined.PersonOutline, "nav_account", "STAFF"),
            )
        }
    }

    val showTopLevelChrome = topLevelDestinations.any { entry ->
        currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
    }

    val navigateToDestination: (Any) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val windowSize = LocalLoyWindowSize.current
    val useRail = windowSize.useNavigationRail

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showTopLevelChrome && !useRail) {
                NavigationBar(
                    containerColor = LoySurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    topLevelDestinations.forEach { entry ->
                        val selected = currentDestination?.hierarchy?.any { dest ->
                            entry.matchClasses.any { dest.hasRoute(it) }
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToDestination(entry.route) },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LoyGreen,
                                selectedTextColor = LoyGreen,
                                indicatorColor = LoyGreenLight,
                                unselectedIconColor = LoyTextSecondary,
                                unselectedTextColor = LoyTextSecondary,
                            ),
                            modifier = Modifier.testTag(entry.tag),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (useRail) {
                if (windowSize.isDesktop) {
                    PersistentDesktopSidebar(
                        storeName = data.settings.storeName,
                        destinations = sidebarDestinations,
                        isSelected = { entry ->
                            currentDestination?.hierarchy?.any { dest ->
                                entry.matchClasses.any { dest.hasRoute(it) }
                            } == true
                        },
                        onSelect = navigateToDestination,
                        onOpenBackOffice = if (isOwner) {
                            { viewModel.openBackOffice() }
                        } else {
                            null
                        },
                    )
                } else {
                    NavigationRail(
                        containerColor = LoySurface,
                        modifier = Modifier.fillMaxHeight().testTag("persistent_navigation_rail"),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            topLevelDestinations.forEach { entry ->
                                val selected = currentDestination?.hierarchy?.any { dest ->
                                    entry.matchClasses.any { dest.hasRoute(it) }
                                } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToDestination(entry.route) },
                                    icon = { Icon(entry.icon, contentDescription = entry.label) },
                                    label = { Text(entry.label, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = LoyGreen,
                                        selectedTextColor = LoyGreen,
                                        indicatorColor = LoyGreenLight,
                                    ),
                                    modifier = Modifier.testTag(entry.tag),
                                )
                            }
                            if (isOwner) {
                                HorizontalDivider(
                                    color = LoyBorder,
                                    modifier = Modifier.width(44.dp).padding(vertical = 6.dp),
                                )
                                sidebarDestinations
                                    .filter { topLevelDestinations.none { t -> t.tag == it.tag } }
                                    .forEach { entry ->
                                        val selected = currentDestination?.hierarchy?.any { dest ->
                                            entry.matchClasses.any { dest.hasRoute(it) }
                                        } == true
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateToDestination(entry.route) },
                                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                                            label = { Text(entry.label, maxLines = 1) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = LoyGreen,
                                                selectedTextColor = LoyGreen,
                                                indicatorColor = LoyGreenLight,
                                            ),
                                            modifier = Modifier.testTag(entry.tag),
                                        )
                                    }
                            }
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = if (isOwner) DashboardRoute else CheckoutRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen(
                            viewModel = viewModel,
                            session = session,
                            data = data,
                            onNavigateToCheckout = { navigateToDestination(CheckoutRoute) },
                            onNavigateToInventory = { navigateToDestination(InventoryRoute) },
                            onNavigateToStock = { navigateToDestination(StockRoute) },
                            onNavigateToShift = { navigateToDestination(ShiftRoute) },
                            onNavigateToCustomers = { navigateToDestination(CustomersRoute) },
                            onNavigateToReports = { navigateToDestination(ReportsRoute) },
                            onNavigateToSchedule = { navigateToDestination(CalendarRoute) },
                            onNavigateToQrTracking = { navigateToDestination(QrTrackingRoute()) },
                            onNavigateToAgreements = { navigateToDestination(AgreementsRoute) },
                            onNavigateToEventKits = { navigateToDestination(EventKitsRoute) },
                            onNavigateToDamageReports = { navigateToDestination(DamageReportsRoute()) },
                            onReportDamage = { receiptId, itemId ->
                                navController.navigate(
                                    DamageReportsRoute(
                                        initialReceiptId = receiptId,
                                        initialItemId = itemId,
                                    )
                                )
                            },
                            onNavigateToReceipts = { navigateToDestination(ReceiptsRoute) },
                            onNavigateToReceiptDetail = { receiptId ->
                                navController.navigate(ReceiptDetailRoute(receiptId))
                            },
                            onOpenHeldQuote = { ticketId ->
                                data.tickets.firstOrNull { it.ticket.id == ticketId }?.let { ticket ->
                                    transactionViewModel.loadTicket(ticket)
                                    navigateToDestination(CheckoutRoute)
                                }
                            },
                        )
                    }

                        composable<CheckoutRoute> {
                            SellScreen(
                                viewModel = viewModel,
                                transactionViewModel = transactionViewModel,
                                session = session,
                                data = data,
                                onNavigateToCart = { navController.navigate(CartRoute) },
                                onNavigateToPayment = { navController.navigate(PaymentRoute) },
                                onNavigateToSchedule = { navController.navigate(CalendarRoute) },
                                onNavigateToEventKits = { navController.navigate(EventKitsRoute) },
                            )
                        }

                        composable<InventoryRoute> {
                            InventoryScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onNavigateToStock = { navController.navigate(StockRoute) },
                                onNavigateToQrTracking = { itemId ->
                                    navController.navigate(QrTrackingRoute(initialItemId = itemId))
                                },
                                onNavigateToMaintenance = { itemId ->
                                    navController.navigate(MaintenanceRoute(initialItemId = itemId))
                                },
                            )
                        }

                        composable<ReceiptsRoute> {
                            ReceiptsScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onOpenReceipt = { receiptId ->
                                    navController.navigate(ReceiptDetailRoute(receiptId))
                                },
                                onNavigateToSchedule = { navController.navigate(CalendarRoute) },
                                onNavigateToQrTracking = { navController.navigate(QrTrackingRoute()) },
                                onReportDamage = { receiptId, itemId ->
                                    navController.navigate(
                                        DamageReportsRoute(
                                            initialReceiptId = receiptId,
                                            initialItemId = itemId,
                                        )
                                    )
                                },
                            )
                        }

                        composable<ShiftRoute> {
                            ShiftScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onBack = if (isOwner) {
                                    { navController.popBackStack() }
                                } else {
                                    null
                                },
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                                onNavigateToStock = { navController.navigate(StockRoute) },
                                onNavigateToCustomers = { navController.navigate(CustomersRoute) },
                                onNavigateToReports = { navController.navigate(ReportsRoute) },
                                onNavigateToShift = { navController.navigate(ShiftRoute) },
                                onNavigateToQrTracking = { navController.navigate(QrTrackingRoute()) },
                                onNavigateToAgreements = { navController.navigate(AgreementsRoute) },
                                onNavigateToMaintenance = { navController.navigate(MaintenanceRoute()) },
                            )
                        }

                        composable<AccountRoute> {
                            CashierAccountScreen(
                                viewModel = viewModel,
                                session = session,
                                data = data,
                            )
                        }

                        composable<CartRoute> {
                            CartScreen(
                                viewModel = viewModel,
                                transactionViewModel = transactionViewModel,
                                session = session,
                                data = data,
                                onBack = { navController.popBackStack() },
                                onNavigateToPayment = { navController.navigate(PaymentRoute) },
                            )
                        }

                        composable<PaymentRoute> {
                            PaymentScreen(
                                viewModel = viewModel,
                                transactionViewModel = transactionViewModel,
                                session = session,
                                data = data,
                                onBack = { navController.popBackStack() },
                                onOpenSchedule = { navController.navigate(CalendarRoute) },
                                onPaymentCompleted = {
                                    navController.navigate(ReceiptSuccessRoute) {
                                        popUpTo<CheckoutRoute> {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }

                        composable<ReceiptSuccessRoute> {
                            ReceiptSuccessScreen(
                                viewModel = viewModel,
                                transactionViewModel = transactionViewModel,
                                data = data,
                                onDone = {
                                    navController.popBackStack(CheckoutRoute, inclusive = false)
                                },
                            )
                        }

                        composable<ReceiptDetailRoute> { entry ->
                            val args = entry.toRoute<ReceiptDetailRoute>()
                            ReceiptDetailScreen(
                                viewModel = viewModel,
                                receiptId = args.receiptId,
                                data = data,
                                onReportDamage = { receiptId, itemId ->
                                    navController.navigate(
                                        DamageReportsRoute(
                                            initialReceiptId = receiptId,
                                            initialItemId = itemId,
                                        )
                                    )
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<StockRoute> {
                            StockScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = { navController.popBackStack() },
                                session = session,
                            )
                        }

                        composable<CustomersRoute> {
                            CustomersScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<ReportsRoute> {
                            ReportsScreen(
                                viewModel = viewModel,
                                data = data,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<CalendarRoute> {
                            ScheduleCalendarScreen(
                                transactionViewModel = transactionViewModel,
                                session = session,
                                data = data,
                                onBack = { navController.popBackStack() },
                                onOpenReceipt = { receiptId ->
                                    navController.navigate(ReceiptDetailRoute(receiptId))
                                },
                            onBookOnDate = {
                                navigateToDestination(CheckoutRoute)
                            },
                        )
                    }

                    composable<QrTrackingRoute> { entry ->
                        val args = entry.toRoute<QrTrackingRoute>()
                        QrTrackingScreen(
                            viewModel = viewModel,
                            data = data,
                            initialItemId = args.initialItemId,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AgreementsRoute> {
                        AgreementTemplatesScreen(
                            viewModel = viewModel,
                            data = data,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<EventKitsRoute> {
                        EventKitsScreen(
                            viewModel = viewModel,
                            transactionViewModel = transactionViewModel,
                            data = data,
                            onAddedToOrder = { navigateToDestination(CheckoutRoute) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<DamageReportsRoute> { entry ->
                        val args = entry.toRoute<DamageReportsRoute>()
                        DamageReportsScreen(
                            viewModel = viewModel,
                            data = data,
                            initialReceiptId = args.initialReceiptId,
                            initialItemId = args.initialItemId,
                            initialUnitCode = args.initialUnitCode,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<MaintenanceRoute> { entry ->
                        val args = entry.toRoute<MaintenanceRoute>()
                        MaintenanceScreen(
                            viewModel = viewModel,
                            data = data,
                            initialItemId = args.initialItemId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    if (session.showWalkthrough || (!data.settings.hasSeenWalkthrough && session.authStage == AuthStage.LOGGED_IN)) {
        GettingStartedOverlay(
            onDismiss = {
                viewModel.setWalkthroughCompleted(true)
            },
            onNavigateToInventory = {
                viewModel.setWalkthroughCompleted(true)
                navigateToDestination(InventoryRoute)
            },
            onNavigateToCheckout = {
                viewModel.setWalkthroughCompleted(true)
                navigateToDestination(CheckoutRoute)
            },
        )
    }
}

@Composable
private fun PersistentDesktopSidebar(
    storeName: String,
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onSelect: (Any) -> Unit,
    onOpenBackOffice: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(LoySurface)
            .testTag("persistent_desktop_sidebar"),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Workspace Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(LoyGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = storeName.ifBlank { "LoyRent" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LoyTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Event Rental Suite",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            // Scrollable Grouped Navigation
            val grouped = remember(destinations) { destinations.groupBy { it.group } }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                grouped.entries.forEachIndexed { groupIdx, (groupTitle, items) ->
                    if (groupIdx > 0) {
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = groupTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LoyTextSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    items.forEach { entry ->
                        val selected = isSelected(entry)
                        Surface(
                            color = if (selected) LoyGreenLight else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(entry.route) }
                                .testTag(entry.tag),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(18.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (selected) LoyGreen else Color.Transparent),
                                )
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = entry.label,
                                    tint = if (selected) LoyGreen else LoyTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) LoyGreen else LoyTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            if (onOpenBackOffice != null) {
                HorizontalDivider(color = LoyBorder)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(onClick = onOpenBackOffice)
                        .testTag("sidebar_open_back_office"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Back Office",
                            tint = LoyTextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Back Office",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LoyTextPrimary,
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
    }
}
