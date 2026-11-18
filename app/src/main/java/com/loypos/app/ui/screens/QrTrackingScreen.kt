package com.loypos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.loypos.app.core.QrCodeUtils
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.data.LoyPosRepository
import com.loypos.app.data.local.EquipmentUnitEntity
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoyInfoLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.accessibleTouchTarget
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QrTrackingScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    initialItemId: String? = null,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val context = LocalContext.current
    var tabIndex by remember(initialItemId) { mutableIntStateOf(if (initialItemId != null) 1 else 0) }
    var scanMode by remember { mutableStateOf("check_out") } // check_out | check_in | maintenance
    var manualCode by remember { mutableStateOf("") }
    var conditionNote by remember { mutableStateOf("") }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var orderDropdownExpanded by remember { mutableStateOf(false) }
    var resolvedTarget by remember { mutableStateOf<LoyPosRepository.ResolvedQrTarget?>(null) }
    var unitSearch by remember { mutableStateOf("") }
    var unitStatusFilter by remember { mutableStateOf("all") }
    var previewUnit by remember { mutableStateOf<EquipmentUnitEntity?>(null) }
    var previewCatalogQr by remember { mutableStateOf<ProductEntity?>(null) }
    var cameraActive by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            viewModel.showMessage("Camera permission denied. You can still scan from an image or enter codes manually.")
        }
    }

    val imageQrPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = QrCodeUtils.decodeQrFromUri(context, uri)
            if (decoded != null) {
                manualCode = decoded
                viewModel.resolveQrCode(decoded) { target ->
                    resolvedTarget = target
                    if (target == null) {
                        viewModel.showMessage("Decoded QR \"$decoded\" did not match any equipment in fleet")
                    }
                }
            } else {
                viewModel.showMessage("No readable QR code found in the selected image")
            }
        }
    }

    LaunchedEffect(initialItemId, data.equipmentUnits, data.items) {
        if (initialItemId != null && previewUnit == null && previewCatalogQr == null) {
            val firstUnit = data.equipmentUnits.firstOrNull { it.itemId == initialItemId }
            if (firstUnit != null) {
                previewUnit = firstUnit
            } else {
                previewCatalogQr = data.items.firstOrNull { it.id == initialItemId }
            }
        }
    }

    val activeOrders = remember(data.receipts) {
        data.receipts.filter { it.receipt.isRentalOut }
    }
    val selectedOrder = remember(activeOrders, selectedOrderId) {
        activeOrders.firstOrNull { it.receipt.id == selectedOrderId }?.receipt
    }

    val checkedOutCount = remember(data.equipmentUnits) {
        data.equipmentUnits.count { it.isCheckedOut }
    }
    val checkedInCount = remember(data.equipmentUnits) {
        data.equipmentUnits.count { !it.isCheckedOut && !it.isMaintenance }
    }
    val maintenanceCount = remember(data.equipmentUnits) {
        data.equipmentUnits.count { it.isMaintenance }
    }

    val handleDetectedCode: (String) -> Unit = { code ->
        val clean = code.trim()
        if (clean.isNotEmpty() && clean != manualCode) {
            manualCode = clean
            viewModel.resolveQrCode(clean) { target ->
                resolvedTarget = target
                if (target == null) {
                    viewModel.showMessage("Unrecognized QR code: $clean")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    expandedHeight = metrics.topBarHeight,
                    title = {
                        Column {
                            Text(
                                "QR Equipment Tracking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "$checkedInCount in · $checkedOutCount checked out · $maintenanceCount service",
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextSecondary,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .accessibleTouchTarget(metrics.minTouchTarget)
                                .testTag("qr_back_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
                )
                TabRow(selectedTabIndex = tabIndex, containerColor = LoySurface) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        modifier = Modifier.height(42.dp).testTag("qr_tab_scanner"),
                        text = { Text("Scan Check-In / Out", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        modifier = Modifier.height(42.dp).testTag("qr_tab_labels"),
                        text = { Text("QR Tags (${data.equipmentUnits.size})", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        modifier = Modifier.height(42.dp).testTag("qr_tab_history"),
                        text = { Text("Scan Log (${data.equipmentScanLogs.size})", style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            when (tabIndex) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(metrics.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                    ) {
                        // Scan Action Mode & Active Order Linker
                        item {
                            GroupCard {
                                Column(
                                    modifier = Modifier.padding(metrics.cardPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text("Scan Workflow Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        listOf(
                                            Triple("check_out", "Check Out", Icons.Outlined.Output),
                                            Triple("check_in", "Check In", Icons.AutoMirrored.Outlined.AssignmentReturn),
                                            Triple("maintenance", "Service", Icons.Outlined.Build),
                                        ).forEach { (mode, label, icon) ->
                                            val selected = scanMode == mode
                                            FilterChip(
                                                selected = selected,
                                                onClick = { scanMode = mode },
                                                leadingIcon = {
                                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                                },
                                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = when (mode) {
                                                        "check_out" -> LoyInfoLight
                                                        "maintenance" -> Color(0xFFFFF3E0)
                                                        else -> LoyGreenLight
                                                    },
                                                    selectedLabelColor = when (mode) {
                                                        "check_out" -> LoyInfo
                                                        "maintenance" -> LoyWarning
                                                        else -> LoyGreen
                                                    },
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 38.dp)
                                                    .testTag("qr_mode_$mode"),
                                            )
                                        }
                                    }

                                    if (scanMode == "check_out" && activeOrders.isNotEmpty()) {
                                        ExposedDropdownMenuBox(
                                            expanded = orderDropdownExpanded,
                                            onExpandedChange = { orderDropdownExpanded = !orderDropdownExpanded },
                                        ) {
                                            val orderLabel = if (selectedOrder != null) {
                                                "#${selectedOrder.number.toString().padStart(4, '0')} · ${selectedOrder.customerName ?: "Client"} (${selectedOrder.eventName.ifEmpty { "${selectedOrder.rentalDays}d" }})"
                                            } else {
                                                "Standalone dispatch (no linked order)"
                                            }
                                            OutlinedTextField(
                                                value = orderLabel,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Link to Active Rental Order (Optional)") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderDropdownExpanded) },
                                                modifier = Modifier
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                    .fillMaxWidth()
                                                    .testTag("qr_order_picker"),
                                            )
                                            ExposedDropdownMenu(
                                                expanded = orderDropdownExpanded,
                                                onDismissRequest = { orderDropdownExpanded = false },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Standalone dispatch (adjusts available stock)") },
                                                    onClick = {
                                                        selectedOrderId = null
                                                        orderDropdownExpanded = false
                                                    },
                                                )
                                                activeOrders.forEach { orderWithLines ->
                                                    val r = orderWithLines.receipt
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text("#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Walk-in"} · ${r.eventName}")
                                                        },
                                                        onClick = {
                                                            selectedOrderId = r.id
                                                            orderDropdownExpanded = false
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // CameraX + ML Kit Live QR Scanner Card
                        item {
                            GroupCard {
                                Column(
                                    modifier = Modifier.padding(metrics.cardPadding),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                                            Text("Live Camera QR Scanner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        if (hasCameraPermission) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { torchEnabled = !torchEnabled },
                                                    modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("qr_torch_button"),
                                                ) {
                                                    Icon(
                                                        if (torchEnabled) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                                                        contentDescription = "Toggle flashlight",
                                                        tint = if (torchEnabled) LoyWarning else LoyTextSecondary,
                                                    )
                                                }
                                                TextButton(onClick = { cameraActive = !cameraActive }) {
                                                    Text(if (cameraActive) "Pause" else "Resume")
                                                }
                                            }
                                        }
                                    }

                                    if (hasCameraPermission && cameraActive) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(210.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CameraQrScannerPreview(
                                                torchEnabled = torchEnabled,
                                                onQrDetected = handleDetectedCode,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.Transparent,
                                                border = BorderStroke(2.dp, LoyGreen),
                                                modifier = Modifier.size(145.dp),
                                            ) {}
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = LoyBackground,
                                            border = BorderStroke(1.dp, LoyBorder),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(34.dp))
                                                Text(
                                                    text = if (!hasCameraPermission) {
                                                        "Grant camera access to scan equipment QR tags directly."
                                                    } else {
                                                        "Camera scanner paused."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LoyTextSecondary,
                                                )
                                                Button(
                                                    onClick = {
                                                        if (!hasCameraPermission) {
                                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                        } else {
                                                            cameraActive = true
                                                        }
                                                    },
                                                    modifier = Modifier.heightIn(min = 48.dp).testTag("qr_enable_camera_button"),
                                                ) {
                                                    Text(if (!hasCameraPermission) "Enable Camera Scanner" else "Resume Camera")
                                                }
                                            }
                                        }
                                    }

                                    // Manual Code Lookup / Image Scan Fallback
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = manualCode,
                                            onValueChange = { manualCode = it },
                                            label = { Text("Unit code, SKU, or QR payload") },
                                            placeholder = { Text("e.g. TENT-01#U01") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    viewModel.resolveQrCode(manualCode) { target ->
                                                        resolvedTarget = target
                                                        if (target == null) viewModel.showMessage("No matching equipment or unit found")
                                                    }
                                                }
                                            ),
                                            modifier = Modifier.weight(1f).testTag("qr_manual_code_input"),
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.resolveQrCode(manualCode) { target ->
                                                    resolvedTarget = target
                                                    if (target == null) viewModel.showMessage("No matching equipment or unit found")
                                                }
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("qr_lookup_button"),
                                        ) {
                                            Text("Lookup")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            imageQrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("qr_scan_image_button"),
                                    ) {
                                        Icon(Icons.Outlined.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Scan QR from Photo / Label Image")
                                    }
                                }
                            }
                        }

                        // Resolved Target Confirmation Card
                        resolvedTarget?.let { target ->
                            item {
                                val unit = target.unit
                                val item = target.item
                                val statusText = when {
                                    unit?.isCheckedOut == true -> "Checked Out"
                                    unit?.isMaintenance == true -> "Service / Maintenance"
                                    unit != null -> "Checked In"
                                    else -> "Catalog Equipment (${formatQty(item.stock)} avail)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LoySurface,
                                    border = BorderStroke(1.5.dp, LoyGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("qr_resolved_card"),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(metrics.cardPadding),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (unit != null) "Unit Tag: ${unit.unitCode}" else "SKU: ${item.sku.ifEmpty { target.matchedCode }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LoyTextSecondary,
                                                )
                                            }
                                            PillBadge(
                                                text = statusText,
                                                backgroundColor = when {
                                                    unit?.isCheckedOut == true -> LoyInfoLight
                                                    unit?.isMaintenance == true -> Color(0xFFFFF3E0)
                                                    else -> LoyGreenLight
                                                },
                                                textColor = when {
                                                    unit?.isCheckedOut == true -> LoyInfo
                                                    unit?.isMaintenance == true -> LoyWarning
                                                    else -> LoyGreen
                                                },
                                            )
                                        }

                                        if (unit != null && unit.currentCustomerName.isNotEmpty()) {
                                            Text(
                                                "Assigned to: ${unit.currentCustomerName}${if (unit.currentEventName.isNotEmpty()) " (${unit.currentEventName})" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LoyInfo,
                                            )
                                        }

                                        OutlinedTextField(
                                            value = conditionNote,
                                            onValueChange = { conditionNote = it },
                                            label = { Text("Condition / inspection note (optional)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("qr_condition_note_input"),
                                        )

                                        val actionLabel = when (scanMode) {
                                            "check_out" -> "Confirm Check-Out"
                                            "check_in" -> "Confirm Check-In"
                                            else -> "Flag Maintenance"
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.processEquipmentScan(
                                                    unitId = unit?.id,
                                                    itemId = item.id,
                                                    action = scanMode,
                                                    receipt = if (scanMode == "check_out") selectedOrder else null,
                                                    note = conditionNote,
                                                    adjustStock = selectedOrder == null,
                                                ) { updated ->
                                                    if (updated != null) {
                                                        resolvedTarget = target.copy(unit = updated)
                                                        conditionNote = ""
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = when (scanMode) {
                                                    "check_out" -> LoyInfo
                                                    "maintenance" -> LoyWarning
                                                    else -> LoyGreen
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("qr_confirm_scan_button"),
                                        ) {
                                            Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(actionLabel, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Tab 2: QR Labels & Serialized Unit Tracker
                    val filteredUnits = remember(data.equipmentUnits, unitSearch, unitStatusFilter, initialItemId) {
                        val q = unitSearch.trim().lowercase()
                        data.equipmentUnits.filter { unit ->
                            val matchesStatus = unitStatusFilter == "all" || unit.status == unitStatusFilter
                            val matchesSearch = if (q.isEmpty()) true else {
                                unit.itemName.lowercase().contains(q) ||
                                    unit.unitCode.lowercase().contains(q) ||
                                    unit.currentCustomerName.lowercase().contains(q) ||
                                    unit.currentEventName.lowercase().contains(q)
                            }
                            matchesStatus && matchesSearch
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LoySurface)
                                .padding(horizontal = metrics.screenPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CompactSearchField(
                                value = unitSearch,
                                onValueChange = { unitSearch = it },
                                placeholder = "Filter unit code, equipment, event...",
                                inputTestTag = "qr_units_search_input",
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "all" to "All (${data.equipmentUnits.size})",
                                    EquipmentUnitEntity.STATUS_CHECKED_IN to "In ($checkedInCount)",
                                    EquipmentUnitEntity.STATUS_CHECKED_OUT to "Out ($checkedOutCount)",
                                    EquipmentUnitEntity.STATUS_MAINTENANCE to "Service ($maintenanceCount)",
                                ).forEach { (key, label) ->
                                    item {
                                        FilterChip(
                                            selected = unitStatusFilter == key,
                                            onClick = { unitStatusFilter = key },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(30.dp),
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = LoyBorder)

                        if (filteredUnits.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    icon = Icons.Outlined.QrCode2,
                                    title = "No matching unit QR tags",
                                    subtitle = "Select an equipment item below to generate serialized QR tags.",
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(metrics.screenPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Quick Catalog Tag Generator Row
                                item {
                                    SectionHeader("Generate QR Tag for Equipment")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(data.items.take(20), key = { it.id }) { equip ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = LoySurface,
                                                border = BorderStroke(1.dp, LoyBorder),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { previewCatalogQr = equip }
                                                    .testTag("qr_catalog_card_${equip.id}"),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                                                    Column {
                                                        Text(equip.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                        Text(
                                                            "${data.equipmentUnits.count { it.itemId == equip.id }} units · ${formatCents(equip.priceCents, data.settings.currencySymbol)}/d",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = LoyTextSecondary,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    SectionHeader("Individual Equipment Units")
                                }

                                items(filteredUnits, key = { it.id }) { unit ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LoySurface,
                                        border = BorderStroke(1.dp, LoyBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { previewUnit = unit }
                                            .testTag("qr_unit_row_${unit.id}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when {
                                                            unit.isCheckedOut -> LoyInfoLight
                                                            unit.isMaintenance -> Color(0xFFFFF3E0)
                                                            else -> LoyGreenLight
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Outlined.QrCode2,
                                                    contentDescription = "View QR code",
                                                    tint = when {
                                                        unit.isCheckedOut -> LoyInfo
                                                        unit.isMaintenance -> LoyWarning
                                                        else -> LoyGreen
                                                    },
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(unit.itemName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    buildString {
                                                        append(unit.unitCode)
                                                        if (unit.currentCustomerName.isNotEmpty()) append(" · ${unit.currentCustomerName}")
                                                        if (unit.conditionNote.isNotEmpty()) append(" · ${unit.conditionNote}")
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LoyTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            PillBadge(
                                                text = when {
                                                    unit.isCheckedOut -> "Out"
                                                    unit.isMaintenance -> "Service"
                                                    else -> "In"
                                                },
                                                backgroundColor = when {
                                                    unit.isCheckedOut -> LoyInfoLight
                                                    unit.isMaintenance -> Color(0xFFFFF3E0)
                                                    else -> LoyGreenLight
                                                },
                                                textColor = when {
                                                    unit.isCheckedOut -> LoyInfo
                                                    unit.isMaintenance -> LoyWarning
                                                    else -> LoyGreen
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Tab 3: Scan History Log
                    if (data.equipmentScanLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Outlined.History,
                                title = "No QR scans logged yet",
                                subtitle = "Scanned check-outs and check-ins will appear here with timestamps.",
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(metrics.screenPadding),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(data.equipmentScanLogs, key = { it.id }) { log ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LoySurface,
                                    border = BorderStroke(1.dp, LoyBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(log.unitCode.ifEmpty { log.itemName }, fontWeight = FontWeight.Bold)
                                                PillBadge(
                                                    text = when (log.action) {
                                                        "check_out" -> "Checked Out"
                                                        "check_in" -> "Checked In"
                                                        "maintenance" -> "Maintenance"
                                                        else -> "Inspected"
                                                    },
                                                    backgroundColor = when (log.action) {
                                                        "check_out" -> LoyInfoLight
                                                        "maintenance" -> Color(0xFFFFF3E0)
                                                        else -> LoyGreenLight
                                                    },
                                                    textColor = when (log.action) {
                                                        "check_out" -> LoyInfo
                                                        "maintenance" -> LoyWarning
                                                        else -> LoyGreen
                                                    },
                                                )
                                            }
                                            Text(log.itemName, style = MaterialTheme.typography.bodySmall, color = LoyTextPrimary)
                                            Text(
                                                buildString {
                                                    append(formatDateTime(log.createdAt))
                                                    if (log.customerName.isNotEmpty()) append(" · ${log.customerName}")
                                                    if (log.note.isNotEmpty()) append(" · ${log.note}")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LoyTextSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet for inspecting a specific serialized EquipmentUnit QR Code
    previewUnit?.let { currentUnit ->
        val freshUnit = data.equipmentUnits.firstOrNull { it.id == currentUnit.id } ?: currentUnit
        val item = data.items.firstOrNull { it.id == freshUnit.itemId }
        val qrBitmap = remember(freshUnit.qrPayload) {
            QrCodeUtils.encodeQrImageBitmap(freshUnit.qrPayload, 512)
        }
        var unitNote by remember(freshUnit.id) { mutableStateOf(freshUnit.conditionNote) }

        ModalBottomSheet(
            onDismissRequest = { previewUnit = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(freshUnit.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(freshUnit.unitCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleSmall, color = LoyGreen)

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LoyBorder),
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "QR code for ${freshUnit.unitCode}",
                            modifier = Modifier.padding(12.dp).size(200.dp),
                        )
                    }
                }

                Text(
                    "Payload: ${freshUnit.qrPayload}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = LoyTextSecondary,
                )

                OutlinedTextField(
                    value = unitNote,
                    onValueChange = { unitNote = it },
                    label = { Text("Condition note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.processEquipmentScan(
                                unitId = freshUnit.id,
                                itemId = freshUnit.itemId,
                                action = if (freshUnit.isCheckedOut) "check_in" else "check_out",
                                note = unitNote,
                                adjustStock = freshUnit.currentReceiptId == null,
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (freshUnit.isCheckedOut) LoyGreen else LoyInfo
                        ),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("qr_sheet_toggle_status"),
                    ) {
                        Text(if (freshUnit.isCheckedOut) "Check In Unit" else "Check Out Unit")
                    }
                    OutlinedButton(
                        onClick = {
                            val sub = item?.let { "${formatCents(it.priceCents, data.settings.currencySymbol)}/day · SKU ${it.sku}" }
                                ?: freshUnit.itemName
                            viewModel.shareEquipmentQrSticker(
                                itemName = freshUnit.itemName,
                                codeLabel = freshUnit.unitCode,
                                subtitle = sub,
                                qrPayload = freshUnit.qrPayload,
                            )
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("qr_sheet_share_png"),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Share QR Tag")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = {
                            viewModel.processEquipmentScan(
                                unitId = freshUnit.id,
                                itemId = freshUnit.itemId,
                                action = "maintenance",
                                note = unitNote.ifEmpty { "Flagged for inspection" },
                                adjustStock = false,
                            )
                        },
                    ) {
                        Text("Mark Maintenance", color = LoyWarning)
                    }
                    TextButton(
                        onClick = {
                            viewModel.deleteEquipmentUnit(freshUnit.id)
                            previewUnit = null
                        },
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = LoyError, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete Tag", color = LoyError)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Bottom sheet for viewing Equipment-level QR & managing serialized units for an item
    previewCatalogQr?.let { equip ->
        val unitsForItem = data.equipmentUnits.filter { it.itemId == equip.id }
        val catalogPayload = remember(equip.id, equip.sku) {
            QrCodeUtils.buildEquipmentPayload(equip.id, equip.sku.ifEmpty { equip.id })
        }
        val qrBitmap = remember(catalogPayload) { QrCodeUtils.encodeQrImageBitmap(catalogPayload, 512) }

        ModalBottomSheet(
            onDismissRequest = { previewCatalogQr = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(equip.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("SKU ${equip.sku.ifEmpty { equip.id.take(8) }} · ${formatCents(equip.priceCents, data.settings.currencySymbol)}/day", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LoyBorder),
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Catalog QR for ${equip.name}",
                            modifier = Modifier.padding(12.dp).size(180.dp),
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.shareEquipmentQrSticker(
                                itemName = equip.name,
                                codeLabel = equip.sku.ifEmpty { equip.id.take(8) },
                                subtitle = "${formatCents(equip.priceCents, data.settings.currencySymbol)}/day",
                                qrPayload = catalogPayload,
                            )
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Share Master QR")
                    }
                    Button(
                        onClick = {
                            viewModel.addEquipmentUnit(equip.id)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("qr_add_serialized_unit_button"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("New Unit Tag")
                    }
                }

                HorizontalDivider(color = LoyBorder)
                Text("Serialized Individual Units (${unitsForItem.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                unitsForItem.forEach { u ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LoyBackground,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth().clickable {
                            previewCatalogQr = null
                            previewUnit = u
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(u.unitCode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            PillBadge(
                                text = when {
                                    u.isCheckedOut -> "Checked Out"
                                    u.isMaintenance -> "Service"
                                    else -> "Checked In"
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrScannerPreview(
    torchEnabled: Boolean,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val opts = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(opts)
    }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(torchEnabled, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { barcodeScanner.close() }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(input)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                if (raw != null) {
                                    onQrDetected(raw)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                runCatching {
                    provider.unbindAll()
                    cameraRef = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
