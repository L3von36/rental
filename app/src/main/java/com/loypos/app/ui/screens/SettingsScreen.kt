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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.loypos.app.core.LocalBackup
import com.loypos.app.core.formatBytes
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.parseQty
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.ConfirmDialog
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.InventoryCsvSheet
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.theme.BackOfficeTeal
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierAccountScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
) {
    val user = session.currentUser
    val branch = data.branches.firstOrNull { it.id == session.activeBranchId }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = 50.dp,
            title = {
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
        )
        HorizontalDivider(color = LoyBorder)

        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GroupCard {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user?.ownerName?.ifEmpty { user.email } ?: "Seller",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                        PillBadge("Seller")
                    }
                    if (branch != null) {
                        Text("Assigned branch: ${branch.name}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                    }
                }
            }

            Button(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = LoyError, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("cashier_sign_out_button"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sign out")
            }
        }
    }

    if (showSignOutConfirm) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You will return to the sign-in screen.",
            confirmLabel = "Sign out",
            destructive = true,
            onConfirm = {
                showSignOutConfirm = false
                viewModel.signOut()
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToStock: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToShift: () -> Unit,
    onNavigateToQrTracking: () -> Unit = {},
    onNavigateToAgreements: () -> Unit = {},
) {
    val context = LocalContext.current
    val user = session.currentUser
    var showStoreDialog by remember { mutableStateOf(false) }
    var showTaxDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingBranch by remember { mutableStateOf<BranchEntity?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var pendingRestoreBackup by remember { mutableStateOf<LocalBackup?>(null) }
    var showInventoryCsvSheet by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.updateLowStockAlertSettings(true, data.settings.lowStockThreshold)
        } else {
            viewModel.showMessage("Notification permission is required for low-stock alerts")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    val metrics = LocalLoyMetrics.current
    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            title = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            actions = {
                TopSyncIconButton(syncing = session.syncing, onClick = { viewModel.syncCloud() })
            },
        )
        HorizontalDivider(color = LoyBorder)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(metrics.screenPadding),
            verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
        ) {
            // Account
            item {
                SectionHeader("Account")
                GroupCard {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    user?.ownerName?.ifEmpty { data.settings.storeName } ?: data.settings.storeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                            PillBadge("Owner")
                        }
                        OutlinedButton(
                            onClick = { showSignOutConfirm = true },
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("owner_sign_out_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = LoyError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign out", color = LoyError)
                        }
                        HorizontalDivider(color = LoyBorder)
                        SettingsNavRow(
                            icon = Icons.Outlined.HelpOutline,
                            title = "Getting Started Walkthrough",
                            subtitle = "Interactive guide to manage equipment and book rentals",
                            onClick = { viewModel.showWalkthrough() },
                            modifier = Modifier.testTag("settings_walkthrough_row"),
                        )
                    }
                }
            }

            // Back Office
            item {
                SectionHeader("Back Office")
                GroupCard {
                    SettingsNavRow(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "Open Back Office Dashboard",
                        subtitle = if (session.backOfficeUser != null) {
                            "Signed in as ${session.backOfficeUser.email} · Analytics, margins & stock"
                        } else {
                            "Separate Back Office authentication · Analytics, margins & stock"
                        },
                        iconTint = BackOfficeTeal,
                        onClick = { viewModel.openBackOffice() },
                        modifier = Modifier.testTag("settings_open_backoffice_portal"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.AccessTime,
                        title = "Shift & Cash Drawer",
                        subtitle = "Open/close cash register shift and movements",
                        onClick = onNavigateToShift,
                        modifier = Modifier.testTag("nav_backoffice_shift"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Inventory2,
                        title = "Equipment & Fleet Stock",
                        subtitle = "${data.items.count { it.trackStock && it.stock < data.settings.lowStockThreshold }} below threshold (< ${formatQty(data.settings.lowStockThreshold)})",
                        onClick = onNavigateToStock,
                        modifier = Modifier.testTag("nav_backoffice_stock"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.PeopleOutline,
                        title = "Clients & Organizers",
                        subtitle = "${data.customers.size} registered clients",
                        onClick = onNavigateToCustomers,
                        modifier = Modifier.testTag("nav_backoffice_customers"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Assessment,
                        title = "Rental Reports",
                        subtitle = "${data.receipts.size} rental orders recorded",
                        onClick = onNavigateToReports,
                        modifier = Modifier.testTag("nav_backoffice_reports"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.QrCodeScanner,
                        title = "QR Equipment Tracking",
                        subtitle = "${data.equipmentUnits.size} serialized QR unit tags · Check-in/out scanner",
                        onClick = onNavigateToQrTracking,
                        modifier = Modifier.testTag("nav_backoffice_qr_tracking"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Description,
                        title = "Digital Rental Agreement Templates",
                        subtitle = "${data.agreementTemplates.size} reusable contract templates with merge fields",
                        onClick = onNavigateToAgreements,
                        modifier = Modifier.testTag("nav_backoffice_agreements"),
                    )
                }
            }

            // Branches
            item {
                SectionHeader("Branches")
                GroupCard {
                    data.branches.forEachIndexed { idx, branch ->
                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingBranch = branch }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(branch.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (branch.address.isNotEmpty()) {
                                    Text(branch.address, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                            }
                            if (branch.id == session.activeBranchId) {
                                PillBadge("Active")
                            }
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit branch", tint = LoyTextSecondary, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable {
                                editingBranch = BranchEntity(id = newId(), name = "", createdAt = nowMs())
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add branch", color = LoyGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Staff
            item {
                SectionHeader("Staff & Sellers")
                GroupCard {
                    if (data.staff.isEmpty()) {
                        Text(
                            "No seller accounts yet. Add cashiers pinned to a specific branch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                            modifier = Modifier.padding(11.dp),
                        )
                    } else {
                        data.staff.forEachIndexed { idx, seller ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            val branchName = data.branches.firstOrNull { it.id == seller.branchId }?.name ?: "Branch"
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(seller.ownerName.ifEmpty { seller.email }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${seller.email} · $branchName", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                Switch(
                                    checked = seller.active,
                                    onCheckedChange = { viewModel.toggleStaffActive(seller.id, it) },
                                )
                                IconButton(onClick = { viewModel.deleteStaff(seller.id) }, modifier = Modifier.size(38.dp)) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete seller", tint = LoyError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { showAddStaffDialog = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add seller account", color = LoyGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Company & Tax
            item {
                SectionHeader("Rental Company & Taxes")
                GroupCard {
                    SettingsNavRow(
                        icon = Icons.Outlined.Storefront,
                        title = "Company profile & receipt footer",
                        subtitle = "${data.settings.storeName} · ${data.settings.currencySymbol}",
                        onClick = { showStoreDialog = true },
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Assessment,
                        title = "Currency & Tax",
                        subtitle = "${data.settings.currencySymbol} · ${data.settings.taxRatePercent}% (${if (data.settings.taxIncluded) "included" else "added"})",
                        onClick = { showTaxDialog = true },
                    )
                }
            }

            // Low-Stock Periodic Alerts (WorkManager)
            item {
                SectionHeader("Inventory Alerts (WorkManager)")
                GroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("Periodic low-stock alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                PillBadge(
                                    text = if (data.settings.lowStockAlertsEnabled) session.lowStockWorkerStatus else "Off",
                                )
                            }
                            Text(
                                text = if (data.settings.lowStockAlertsEnabled) {
                                    "Checks Room every ${data.settings.lowStockIntervalMinutes} min for items < ${formatQty(data.settings.lowStockThreshold)}"
                                } else {
                                    "Background WorkManager notifications turned off"
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
                            modifier = Modifier.testTag("settings_low_stock_switch"),
                        )
                    }
                    HorizontalDivider(color = LoyBorder)
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Worker check interval",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                        Spacer(Modifier.height(5.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    modifier = Modifier.height(30.dp).testTag("settings_interval_${mins}m"),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Tune,
                        title = "Low-stock threshold",
                        subtitle = "Notify when tracked quantity is below ${formatQty(data.settings.lowStockThreshold)}",
                        onClick = { showThresholdDialog = true },
                        modifier = Modifier.testTag("settings_low_stock_threshold_button"),
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
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                            .testTag("settings_low_stock_check_now"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Check low stock now", color = LoyGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Data Export & Import
            item {
                SectionHeader("Data & Safety Backups")
                GroupCard {
                    SettingsNavRow(
                        icon = Icons.Outlined.TableView,
                        title = "Bulk fleet inventory (CSV import / export)",
                        subtitle = "Export ${data.items.size} tools or bulk-import from CSV spreadsheets",
                        onClick = { showInventoryCsvSheet = true },
                        modifier = Modifier.testTag("settings_inventory_csv_row"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Upload,
                        title = "Export rental data (JSON)",
                        subtitle = "Share portable loypos-export v1 file with equipment photos",
                        onClick = { viewModel.exportData() },
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Download,
                        title = "Import rental data (JSON)",
                        subtitle = "Replaces fleet & rentals (creates safety backup first)",
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Backup,
                        title = "Save local safety copy",
                        subtitle = "${session.localBackups.size} saved on device",
                        onClick = { viewModel.saveLocalSafetyBackup() },
                    )
                    if (session.localBackups.isNotEmpty()) {
                        session.localBackups.forEach { backup ->
                            HorizontalDivider(color = LoyBorder)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingRestoreBackup = backup }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Restore, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Restore: ${backup.label}", style = MaterialTheme.typography.bodyMedium)
                                    Text(backup.sizeLabel, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Storage & Health + Cloud Sync
            item {
                SectionHeader("Storage, Health & Cloud")
                GroupCard {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val s = session.dbStats
                        Text(
                            "${s.items} items · ${s.categories} categories · ${s.receipts} receipts · ${s.customers} customers",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                        Text(
                            "Database size: ${formatBytes(s.fileBytes)}${if (session.integrityResult != null) " · Integrity: ${session.integrityResult}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.checkIntegrity() }, modifier = Modifier.weight(1f).height(38.dp)) {
                                Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Verify")
                            }
                            OutlinedButton(onClick = { viewModel.optimiseDatabase() }, modifier = Modifier.weight(1f).height(38.dp)) {
                                Text("Optimise")
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.CloudSync,
                        title = if (session.syncing) "Syncing with Plesk MySQL..." else "Sync with Plesk MySQL",
                        subtitle = session.lastSyncMessage ?: "Offline-first Room store · novelsapps.com.et",
                        onClick = { viewModel.syncCloud() },
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "Configure Plesk Sync Token",
                        subtitle = "Store auth token securely in Android Keystore",
                        iconTint = BackOfficeTeal,
                        onClick = { showTokenDialog = true },
                        modifier = Modifier.testTag("settings_configure_token_row"),
                    )
                    HorizontalDivider(color = LoyBorder)
                    SettingsNavRow(
                        icon = Icons.Outlined.Download,
                        title = "Check for GitHub App Updates",
                        subtitle = "Query GitHub Releases API for newer APK version",
                        iconTint = LoyGreen,
                        onClick = { viewModel.checkForAppUpdatesNow() },
                        modifier = Modifier.testTag("settings_check_github_updates"),
                    )
                }
            }

            // Danger Zone
            item {
                SectionHeader("Danger Zone")
                GroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clickable { showResetConfirm = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = LoyError, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset all rental data", style = MaterialTheme.typography.bodyMedium, color = LoyError, fontWeight = FontWeight.SemiBold)
                            Text("Restores the 52-item event rental demo fleet after saving a safety copy", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showInventoryCsvSheet) {
        InventoryCsvSheet(
            viewModel = viewModel,
            data = data,
            onDismiss = { showInventoryCsvSheet = false },
        )
    }

    if (showStoreDialog) {
        StoreSettingsDialog(
            initialName = data.settings.storeName,
            initialFooter = data.settings.receiptFooter,
            onSave = { name, footer ->
                viewModel.saveShopSettings(storeName = name, receiptFooter = footer)
                showStoreDialog = false
            },
            onDismiss = { showStoreDialog = false },
        )
    }

    if (showTaxDialog) {
        TaxSettingsDialog(
            initialCurrency = data.settings.currencySymbol,
            initialRate = data.settings.taxRatePercent,
            initialIncluded = data.settings.taxIncluded,
            onSave = { cur, rate, inc ->
                viewModel.saveShopSettings(currencySymbol = cur, taxRatePercent = rate, taxIncluded = inc)
                showTaxDialog = false
            },
            onDismiss = { showTaxDialog = false },
        )
    }

    if (showThresholdDialog) {
        LowStockThresholdDialog(
            initialThreshold = data.settings.lowStockThreshold,
            onSave = { threshold ->
                viewModel.updateLowStockAlertSettings(data.settings.lowStockAlertsEnabled, threshold)
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false },
        )
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            branches = data.branches,
            onSave = { name, email, pin, branchId, onDone ->
                viewModel.addStaff(name, email, pin, branchId) { err ->
                    if (err == null) showAddStaffDialog = false
                    onDone(err)
                }
            },
            onDismiss = { showAddStaffDialog = false },
        )
    }

    editingBranch?.let { branch ->
        val isNew = data.branches.none { it.id == branch.id }
        BranchEditorDialog(
            branch = branch,
            isNew = isNew,
            canArchive = !isNew && data.branches.size > 1,
            onSave = {
                viewModel.saveBranch(it)
                editingBranch = null
            },
            onArchive = {
                viewModel.archiveBranch(branch.id)
                editingBranch = null
            },
            onDismiss = { editingBranch = null },
        )
    }

    if (showSignOutConfirm) {
        ConfirmDialog(
            title = "Sign out?",
            message = "Your offline catalog and receipts stay saved on this device.",
            confirmLabel = "Sign out",
            destructive = true,
            onConfirm = {
                showSignOutConfirm = false
                viewModel.signOut()
            },
            onDismiss = { showSignOutConfirm = false },
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "Reset all shop data?",
            message = "A local safety backup will be created first, then the catalog and receipts will be reset to defaults.",
            confirmLabel = "Reset",
            destructive = true,
            onConfirm = {
                showResetConfirm = false
                viewModel.resetAllData()
            },
            onDismiss = { showResetConfirm = false },
        )
    }

    pendingRestoreBackup?.let { backup ->
        ConfirmDialog(
            title = "Restore safety backup?",
            message = "Restore snapshot from ${backup.label} (${backup.sizeLabel})? Current shop data will be backed up first.",
            confirmLabel = "Restore",
            onConfirm = {
                val target = backup
                pendingRestoreBackup = null
                viewModel.restoreSafetyBackup(target)
            },
            onDismiss = { pendingRestoreBackup = null },
        )
    }

    if (showTokenDialog) {
        PleskTokenDialog(
            initialToken = com.loypos.app.core.SecurityManager.getSecureToken(context),
            onSave = { token ->
                viewModel.savePleskSyncToken(token)
                showTokenDialog = false
            },
            onDismiss = { showTokenDialog = false },
        )
    }
}

@Composable
private fun PleskTokenDialog(
    initialToken: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var token by remember { mutableStateOf(initialToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plesk Secure Sync Token") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the sync token generated on your Plesk backend (novelsapps.com.et). It will be stored securely using Android Keystore (EncryptedSharedPreferences) with hardware-level AES-256 encryption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Sync Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_plesk_token_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(token.trim()) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("settings_plesk_token_save"),
            ) {
                Text("Save & Sync")
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
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = LoyGreen,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = LoyTextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StoreSettingsDialog(
    initialName: String,
    initialFooter: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var footer by remember { mutableStateOf(initialFooter) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Store & Receipt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store name") }, singleLine = true)
                OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Receipt footer") }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim().ifEmpty { "My Store" }, footer.trim()) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        },
    )
}

@Composable
private fun TaxSettingsDialog(
    initialCurrency: String,
    initialRate: Double,
    initialIncluded: Boolean,
    onSave: (String, Double, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var cur by remember { mutableStateOf(initialCurrency) }
    var rateText by remember { mutableStateOf(if (initialRate == 0.0) "0" else initialRate.toString()) }
    var included by remember { mutableStateOf(initialIncluded) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency & Tax") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = cur, onValueChange = { cur = it }, label = { Text("Currency symbol") }, singleLine = true)
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Tax rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tax included in prices")
                    Switch(checked = included, onCheckedChange = { included = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateText.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                    onSave(cur.trim().ifEmpty { "Br" }, rate, included)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(
    branches: List<BranchEntity>,
    onSave: (String, String, String, String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var branchId by remember { mutableStateOf(branches.firstOrNull()?.id ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Seller Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Seller name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Password / PIN (min 6 chars)") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    val bName = branches.firstOrNull { it.id == branchId }?.name ?: "Select branch"
                    OutlinedTextField(
                        value = bName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Branch") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        branches.forEach { b ->
                            DropdownMenuItem(text = { Text(b.name) }, onClick = { branchId = b.id; expanded = false })
                        }
                    }
                }
                if (error != null) Text(error!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, email, pin, branchId) { err -> error = err }
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Add seller") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        },
    )
}

@Composable
private fun BranchEditorDialog(
    branch: BranchEntity,
    isNew: Boolean,
    canArchive: Boolean,
    onSave: (BranchEntity) -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(branch.name) }
    var address by remember { mutableStateOf(branch.address) }
    var phone by remember { mutableStateOf(branch.phone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Branch" else "Edit Branch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch name *") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(branch.copy(name = name.trim(), address = address.trim(), phone = phone.trim()))
                    }
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (canArchive) {
                    TextButton(onClick = onArchive, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Delete", color = LoyError)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun LowStockThresholdDialog(
    initialThreshold: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var thresholdText by remember { mutableStateOf(formatQty(initialThreshold)) }
    val parsed = parseQty(thresholdText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Low-Stock Threshold") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Send a periodic notification whenever a tracked product falls below this quantity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Minimum quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("low_stock_threshold_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = parsed ?: 3.0
                    onSave(value)
                },
                enabled = parsed != null,
                modifier = Modifier.heightIn(min = 48.dp).testTag("low_stock_threshold_save"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        },
    )
}
