package com.loypos.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.LowStockNotifier
import com.loypos.app.core.RentalReturnNotifier
import com.loypos.app.ui.AppWorkspace
import com.loypos.app.ui.AuthStage
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.screens.AuthGateScreen
import com.loypos.app.ui.screens.BackOfficeAuthScreen
import com.loypos.app.ui.screens.BackOfficeMainScreen
import com.loypos.app.ui.screens.MainScreen
import com.loypos.app.ui.screens.SplashScreen
import com.loypos.app.ui.theme.LoyPosTheme

class MainActivity : ComponentActivity() {
    private val vm: LoyPosViewModel by viewModels()
    private val txVm: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            LoyPosTheme {
                val session by vm.session.collectAsStateWithLifecycle()
                val data by vm.dataState.collectAsStateWithLifecycle()

                LaunchedEffect(session.authStage) {
                    if (session.authStage == AuthStage.LOGGED_OUT) {
                        txVm.clearCart()
                    }
                }

                when (session.authStage) {
                    AuthStage.SPLASH -> SplashScreen()
                    AuthStage.LOGGED_OUT -> AuthGateScreen(
                        viewModel = vm,
                        hasAnyUser = session.hasAnyUser,
                        hasAnyBackOfficeUser = session.hasAnyBackOfficeUser,
                        initialWorkspace = session.activeWorkspace,
                    )
                    AuthStage.LOGGED_IN -> {
                        when (session.activeWorkspace) {
                            AppWorkspace.BACK_OFFICE -> {
                                if (session.backOfficeUser == null) {
                                    BackOfficeAuthScreen(
                                        viewModel = vm,
                                        hasAnyBackOfficeUser = session.hasAnyBackOfficeUser,
                                        canReturnToPos = session.currentUser != null,
                                        storeName = data.settings.storeName,
                                    )
                                } else {
                                    BackOfficeMainScreen(
                                        viewModel = vm,
                                        session = session,
                                        data = data,
                                    )
                                }
                            }
                            AppWorkspace.POS -> {
                                if (session.currentUser != null) {
                                    MainScreen(
                                        viewModel = vm,
                                        transactionViewModel = txVm,
                                        session = session,
                                        data = data,
                                    )
                                } else {
                                    AuthGateScreen(
                                        viewModel = vm,
                                        hasAnyUser = session.hasAnyUser,
                                        hasAnyBackOfficeUser = session.hasAnyBackOfficeUser,
                                        initialWorkspace = AppWorkspace.POS,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(LowStockNotifier.EXTRA_OPEN_LOW_STOCK, false)) {
            vm.triggerLowStockNavigation()
        }
        val rentalId = intent.getStringExtra(RentalReturnNotifier.EXTRA_OPEN_RENTAL_ID)
        if (!rentalId.isNullOrBlank()) {
            vm.triggerReturnReminderNavigation(rentalId)
        } else if (intent.getBooleanExtra(RentalReturnNotifier.EXTRA_OPEN_RETURNS, false)) {
            vm.triggerReturnReminderNavigation(null)
        }
    }
}
