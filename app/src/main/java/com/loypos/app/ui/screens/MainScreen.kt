package com.loypos.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.loypos.app.ui.AuthStage
import com.loypos.app.ui.components.CompactBottomNavBar
import com.loypos.app.ui.components.CompactNavEntry
import com.loypos.app.ui.components.GettingStartedOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
    val tag: String,
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
                TopLevelDestination(CheckoutRoute, CheckoutRoute::class, "Rent Desk", Icons.Outlined.PointOfSale, "nav_checkout"),
                TopLevelDestination(InventoryRoute, InventoryRoute::class, "Equipment", Icons.Outlined.Inventory2, "nav_inventory"),
                TopLevelDestination(ReceiptsRoute, ReceiptsRoute::class, "Rentals", Icons.Outlined.ReceiptLong, "nav_receipts"),
                TopLevelDestination(SettingsRoute, SettingsRoute::class, "Settings", Icons.Outlined.Settings, "nav_settings"),
            )
        } else {
            listOf(
                TopLevelDestination(CheckoutRoute, CheckoutRoute::class, "Rent Desk", Icons.Outlined.PointOfSale, "nav_checkout"),
                TopLevelDestination(ReceiptsRoute, ReceiptsRoute::class, "Rentals", Icons.Outlined.ReceiptLong, "nav_receipts"),
                TopLevelDestination(ShiftRoute, ShiftRoute::class, "Shift", Icons.Outlined.AccessTime, "nav_shift"),
                TopLevelDestination(AccountRoute, AccountRoute::class, "Account", Icons.Outlined.PersonOutline, "nav_account"),
            )
        }
    }

    val showTopLevelChrome = topLevelDestinations.any { entry ->
        currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
    }

    val navigateToTopLevel: (Any) -> Unit = { route ->
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
                    CompactBottomNavBar(
                        entries = topLevelDestinations.map { entry ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
                            CompactNavEntry(
                                label = entry.label,
                                icon = entry.icon,
                                selected = selected,
                                tag = entry.tag,
                                onClick = { navigateToTopLevel(entry.route) },
                            )
                        },
                        selectedColor = LoyGreen,
                        indicatorColor = LoyGreenLight,
                    )
                }
            },
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showTopLevelChrome && useRail) {
                    NavigationRail(
                        containerColor = LoySurface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        topLevelDestinations.forEach { entry ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(entry.routeClass) } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(entry.route) },
                                icon = { Icon(entry.icon, contentDescription = entry.label) },
                                label = { Text(entry.label) },
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
                                onNavigateToCheckout = { navigateToTopLevel(CheckoutRoute) },
                                onNavigateToInventory = { navigateToTopLevel(InventoryRoute) },
                                onNavigateToStock = { navController.navigate(StockRoute) },
                                onNavigateToShift = { navController.navigate(ShiftRoute) },
                                onNavigateToCustomers = { navController.navigate(CustomersRoute) },
                                onNavigateToReports = { navController.navigate(ReportsRoute) },
                                onNavigateToSchedule = { navController.navigate(CalendarRoute) },
                                onNavigateToQrTracking = { navController.navigate(QrTrackingRoute()) },
                                onNavigateToAgreements = { navController.navigate(AgreementsRoute) },
                                onNavigateToEventKits = { navController.navigate(EventKitsRoute) },
                                onNavigateToDamageReports = { navController.navigate(DamageReportsRoute()) },
                                onNavigateToReceipts = { navigateToTopLevel(ReceiptsRoute) },
                                onNavigateToReceiptDetail = { receiptId ->
                                    navController.navigate(ReceiptDetailRoute(receiptId))
                                },
                                onOpenHeldQuote = { ticketId ->
                                    data.tickets.firstOrNull { it.ticket.id == ticketId }?.let { ticket ->
                                        transactionViewModel.loadTicket(ticket)
                                        navigateToTopLevel(CheckoutRoute)
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
                                    navigateToTopLevel(CheckoutRoute)
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
                                onAddedToOrder = { navigateToTopLevel(CheckoutRoute) },
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
                navigateToTopLevel(InventoryRoute)
            },
            onNavigateToCheckout = {
                viewModel.setWalkthroughCompleted(true)
                navigateToTopLevel(CheckoutRoute)
            },
        )
    }
}
