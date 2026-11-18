package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loypos.app.core.SalesCsvReportResult
import com.loypos.app.core.SalesReportPeriod
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.parseQty
import com.loypos.app.core.startOfTodayMs
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.data.local.CategoryReportRow
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.DailySalesPoint
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.data.local.TopItemReportRow
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.CurrencyNumPad
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.ItemThumbnail
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.components.StockDotBadge
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StockScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    onBack: (() -> Unit)? = null,
    session: UiSessionState? = null,
) {
    var search by remember { mutableStateOf("") }
    var filterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Low, 2=Out
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var showBatchRestockDialog by remember { mutableStateOf(false) }

    val threshold = data.settings.lowStockThreshold

    val allTracked = remember(data.items) {
        data.items.filter { it.trackStock }
    }
    val outCount = remember(allTracked) { allTracked.count { it.stock <= 0.0 } }
    val lowOnlyCount = remember(allTracked, threshold) {
        allTracked.count { it.stock > 0.0 && it.isBelowSafetyThreshold(threshold) }
    }
    val attentionItems = remember(allTracked, threshold) {
        allTracked.filter { it.isBelowSafetyThreshold(threshold) }
    }

    val trackedItems = remember(allTracked, search, filterIdx, threshold) {
        val q = search.trim().lowercase()
        allTracked
            .filter { item ->
                val matchesSearch = q.isEmpty() ||
                    item.name.lowercase().contains(q) ||
                    item.sku.lowercase().contains(q)
                val matchesFilter = when (filterIdx) {
                    1 -> item.stock > 0.0 && item.isBelowSafetyThreshold(threshold)
                    2 -> item.stock <= 0.0
                    else -> true
                }
                matchesSearch && matchesFilter
            }
            .sortedWith(compareBy<ItemEntity> {
                when {
                    it.stock <= 0.0 -> 0
                    it.isBelowSafetyThreshold(threshold) -> 1
                    else -> 2
                }
            }.thenBy { it.stock - it.effectiveSafetyThreshold(threshold) }.thenBy { it.name })
    }
    val itemNameById = remember(data.items) { data.items.associate { it.id to it.name } }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 50.dp,
                title = {
                    Text(
                        "Fleet & Availability",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    if (attentionItems.isNotEmpty()) {
                        TextButton(
                            onClick = { showBatchRestockDialog = true },
                            modifier = Modifier.heightIn(min = 40.dp).testTag("stock_batch_restock_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(
                                "Restock (${attentionItems.size})",
                                color = LoyGreen,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoySurface)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search tracked items or SKU",
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoySurface)
                    .padding(bottom = 7.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    FilterChip(
                        selected = filterIdx == 0,
                        onClick = { filterIdx = 0 },
                        label = { Text("All (${allTracked.size})", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreenLight,
                            selectedLabelColor = LoyGreen,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = filterIdx == 1,
                        onClick = { filterIdx = 1 },
                        label = { Text("Low Stock ($lowOnlyCount)", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFF3E0),
                            selectedLabelColor = LoyWarning,
                        ),
                        modifier = Modifier.height(30.dp).testTag("stock_filter_low"),
                    )
                }
                item {
                    FilterChip(
                        selected = filterIdx == 2,
                        onClick = { filterIdx = 2 },
                        label = { Text("Out of Stock ($outCount)", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = LoyError,
                        ),
                        modifier = Modifier.height(30.dp).testTag("stock_filter_out"),
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // WorkManager Stock Monitor Summary Banner
                item {
                    GroupCard(modifier = Modifier.testTag("stock_workmanager_banner")) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (data.settings.lowStockAlertsEnabled) LoyGreenLight else LoySurfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = "Stock alert monitor",
                                    tint = if (data.settings.lowStockAlertsEnabled) LoyGreen else LoyTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val workerLabel = if (data.settings.lowStockAlertsEnabled) {
                                    "${session?.lowStockWorkerStatus ?: "Active"} · Every ${data.settings.lowStockIntervalMinutes}m"
                                } else {
                                    "Background monitor off"
                                }
                                Text(
                                    text = "Monitor (< ${formatQty(threshold)}): $workerLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (data.settings.lowStockLastCheckAt > 0L) {
                                        "Last scan: ${formatDateTime(data.settings.lowStockLastCheckAt)} · ${data.settings.lowStockLastCount} flagged"
                                    } else {
                                        "${attentionItems.size} items currently below threshold"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.runLowStockCheckNow() },
                                modifier = Modifier.height(36.dp).testTag("stock_run_check_now"),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Text("Scan", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                items(trackedItems, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedItem = item },
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ItemThumbnail(item = item, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                StockDotBadge(item = item, threshold = threshold)
                            }
                            val isBelow = item.isBelowSafetyThreshold(threshold)
                            PillBadge(
                                text = formatQty(item.stock),
                                backgroundColor = when {
                                    item.stock <= 0.0 -> Color(0xFFFFEBEE)
                                    isBelow -> Color(0xFFFFF3E0)
                                    else -> LoyGreenLight
                                },
                                textColor = when {
                                    item.stock <= 0.0 -> LoyError
                                    isBelow -> LoyWarning
                                    else -> LoyGreen
                                },
                            )
                        }
                    }
                }

                if (data.movements.isNotEmpty()) {
                    item { SectionHeader("Recent Stock Movements") }
                    items(data.movements.take(25), key = { it.id }) { mv ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurface,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(itemNameById[mv.itemId] ?: "Item", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${mv.type.replaceFirstChar { it.uppercase() }} · ${formatDateTime(mv.createdAt)}${if (mv.note.isNotEmpty()) " · ${mv.note}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                    )
                                }
                                Text(
                                    text = "${if (mv.qty > 0) "+" else ""}${formatQty(mv.qty)}",
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                    fontWeight = FontWeight.Bold,
                                    color = if (mv.qty >= 0) LoyGreen else LoyError,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        StockAdjustBottomSheet(
            item = item,
            onReceive = { delta, note ->
                viewModel.receiveStock(item.id, delta, note)
                selectedItem = null
            },
            onSetExact = { count, note ->
                viewModel.setStockCount(item.id, count, note)
                selectedItem = null
            },
            onDismiss = { selectedItem = null },
        )
    }

    if (showBatchRestockDialog) {
        BatchRestockDialog(
            lowItemsCount = attentionItems.size,
            threshold = threshold,
            onConfirm = { targetCount ->
                viewModel.batchRestockLowItems(attentionItems.map { it.id }, targetCount)
                showBatchRestockDialog = false
            },
            onDismiss = { showBatchRestockDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRestockDialog(
    lowItemsCount: Int,
    threshold: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultTarget = (threshold * 3.0).coerceAtLeast(10.0)
    var targetText by remember { mutableStateOf(formatQty(defaultTarget)) }
    val parsed = parseQty(targetText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Restock Low Items") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Replenish all $lowItemsCount low or out-of-stock SKUs (< ${formatQty(threshold)}) up to a target stock count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10.0, 25.0, 50.0).forEach { preset ->
                        FilterChip(
                            selected = parsed == preset,
                            onClick = { targetText = formatQty(preset) },
                            label = { Text(formatQty(preset)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target stock count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("batch_restock_target_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = parsed ?: defaultTarget
                    if (v > 0.0) onConfirm(v)
                },
                enabled = parsed != null && parsed > 0.0,
                modifier = Modifier.heightIn(min = 48.dp).testTag("batch_restock_confirm"),
            ) {
                Text("Restock All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockAdjustBottomSheet(
    item: ItemEntity,
    onReceive: (Double, String) -> Unit,
    onSetExact: (Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf("receive") }
    var qtyText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Current stock: ${formatQty(item.stock)}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == "receive",
                    onClick = { mode = "receive"; qtyText = "" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Receive (+)", style = MaterialTheme.typography.labelLarge) }
                SegmentedButton(
                    selected = mode == "set",
                    onClick = { mode = "set"; qtyText = formatQty(item.stock) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Set count", style = MaterialTheme.typography.labelLarge) }
            }

            if (mode == "receive") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 25, 50).forEach { preset ->
                        FilterChip(
                            selected = qtyText == preset.toString(),
                            onClick = { qtyText = preset.toString() },
                            label = { Text("+$preset", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f).height(30.dp),
                        )
                    }
                }
            }

            Surface(color = LoySurfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (mode == "receive") "+${qtyText.ifEmpty { "0" }}" else qtyText.ifEmpty { "0" },
                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CurrencyNumPad(
                onKey = { d ->
                    if (d == "." && qtyText.contains(".")) return@CurrencyNumPad
                    qtyText += d
                },
                onBackspace = { if (qtyText.isNotEmpty()) qtyText = qtyText.dropLast(1) },
                onClear = { qtyText = "" },
                allowDecimal = true,
            )

            Button(
                onClick = {
                    val v = parseQty(qtyText) ?: 0.0
                    if (mode == "receive") {
                        if (v > 0.0) onReceive(v, note)
                    } else {
                        onSetExact(v, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(42.dp),
            ) {
                Text(if (mode == "receive") "Add to stock" else "Save stock count")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    onBack: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var isNew by remember { mutableStateOf(false) }

    val filtered = remember(data.customers, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) data.customers else data.customers.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 50.dp,
                title = {
                    Text(
                        "Clients (${data.customers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    isNew = true
                    editingCustomer = CustomerEntity(id = newId(), name = "", createdAt = nowMs())
                },
                containerColor = LoyGreen,
                contentColor = Color.White,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add customer", modifier = Modifier.size(20.dp))
            }
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoySurface)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search name, phone or email",
                )
            }
            HorizontalDivider(color = LoyBorder)

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.PeopleOutline,
                        title = "No clients yet",
                        subtitle = "Tap + to add an event client or organizer.",
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 76.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filtered, key = { it.id }) { cust ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurface,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    isNew = false
                                    editingCustomer = cust
                                },
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cust.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    val sub = listOf(cust.phone, cust.email).filter { it.isNotEmpty() }.joinToString(" · ")
                                    if (sub.isNotEmpty()) {
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                }
                                PillBadge("${cust.points} pts")
                            }
                        }
                    }
                }
            }
        }
    }

    editingCustomer?.let { cust ->
        CustomerEditorSheet(
            customer = cust,
            isNew = isNew,
            onSave = {
                viewModel.saveCustomer(it)
                editingCustomer = null
            },
            onDelete = {
                viewModel.deleteCustomer(cust.id)
                editingCustomer = null
            },
            onDismiss = { editingCustomer = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerEditorSheet(
    customer: CustomerEntity,
    isNew: Boolean,
    onSave: (CustomerEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var email by remember { mutableStateOf(customer.email) }
    var note by remember { mutableStateOf(customer.note) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNew) "New Client" else "Edit Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!isNew) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete client", tint = LoyError, modifier = Modifier.size(20.dp))
                    }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client / Organizer *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Venue / Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(customer.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), note = note.trim()))
                    }
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(42.dp),
            ) {
                Text("Save client")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    onBack: (() -> Unit)? = null,
) {
    var periodIdx by remember { mutableIntStateOf(1) } // 0=Today, 1=7d, 2=30d, 3=All
    val sym = data.settings.currencySymbol

    val sinceMs = remember(periodIdx) {
        val today = startOfTodayMs()
        when (periodIdx) {
            0 -> today
            1 -> today - 6L * 86_400_000L
            2 -> today - 29L * 86_400_000L
            else -> 0L
        }
    }

    val filteredReceipts = remember(data.receipts, sinceMs) {
        data.receipts.filter { it.receipt.createdAt >= sinceMs }
    }
    val completed = remember(filteredReceipts) { filteredReceipts.filter { it.receipt.isCompleted } }
    val refunded = remember(filteredReceipts) { filteredReceipts.filter { !it.receipt.isCompleted } }

    val grossCents = completed.sumOf { it.receipt.subtotalCents }
    val discountCents = completed.sumOf { it.receipt.discountCents }
    val netCents = completed.sumOf { it.receipt.totalCents }
    val taxCents = completed.sumOf { it.receipt.taxCents }
    val avgTicketCents = if (completed.isEmpty()) 0L else netCents / completed.size
    val cashCents = completed.filter { it.receipt.paymentType != "card" }.sumOf { it.receipt.totalCents }
    val cardCents = completed.filter { it.receipt.paymentType == "card" }.sumOf { it.receipt.totalCents }

    val itemById = remember(data.items) { data.items.associateBy { it.id } }
    val catById = remember(data.categories) { data.categories.associateBy { it.id } }

    val categoryRows = remember(completed, itemById, catById) {
        val totals = mutableMapOf<String, Pair<Long, Double>>()
        for (rwl in completed) {
            for (line in rwl.lines) {
                val catId = itemById[line.itemId]?.categoryId
                val catName = catById[catId]?.name ?: "Other"
                val cur = totals[catName] ?: (0L to 0.0)
                totals[catName] = (cur.first + line.lineTotalCents) to (cur.second + line.qty)
            }
        }
        totals.entries.map { CategoryReportRow(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.totalCents }
    }

    val topItems = remember(completed) {
        val map = mutableMapOf<String, Pair<Long, Double>>()
        for (rwl in completed) {
            for (line in rwl.lines) {
                val cur = map[line.name] ?: (0L to 0.0)
                map[line.name] = (cur.first + line.lineTotalCents) to (cur.second + line.qty)
            }
        }
        map.entries.map { TopItemReportRow(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.totalCents }
            .take(8)
    }

    // 7-day daily sales series for Bar Chart
    val last7Days = remember(data.receipts) {
        val todayStart = startOfTodayMs()
        val cal = Calendar.getInstance()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        (6 downTo 0).map { offset ->
            val start = todayStart - offset * 86_400_000L
            val end = start + 86_400_000L
            cal.timeInMillis = start
            val label = dayLabels[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val sum = data.receipts
                .filter { it.receipt.isCompleted && it.receipt.createdAt in start until end }
                .sumOf { it.receipt.totalCents }
            DailySalesPoint(label, sum)
        }
    }

    var showCsvExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 50.dp,
                title = {
                    Text(
                        "Rental Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showCsvExportSheet = true },
                        modifier = Modifier.heightIn(min = 40.dp).testTag("reports_top_export_csv_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = LoyGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("CSV", color = LoyGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Today", "7 days", "30 days", "All").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = periodIdx == idx,
                        onClick = { periodIdx = idx },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.height(30.dp),
                    )
                }
            }

            GroupCard {
                Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatBlock("Net rentals", formatCents(netCents, sym), LoyGreen, Modifier.weight(1f))
                        StatBlock("Gross rentals", formatCents(grossCents, sym), LoyTextPrimary, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatBlock("Discounts", formatCents(discountCents, sym), LoyTextPrimary, Modifier.weight(1f))
                        StatBlock("Tax", formatCents(taxCents, sym), LoyTextPrimary, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatBlock("Rentals / Refunds", "${completed.size} / ${refunded.size}", LoyTextPrimary, Modifier.weight(1f))
                        StatBlock("Avg booking", formatCents(avgTicketCents, sym), LoyTextPrimary, Modifier.weight(1f))
                    }
                }
            }

            // Back-office accounting CSV export card
            GroupCard(modifier = Modifier.testTag("reports_export_csv_card")) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LoyGreenLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TableView,
                            contentDescription = "Accounting CSV export",
                            tint = LoyGreen,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Accounting Export (CSV)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LoyTextPrimary,
                        )
                        Text(
                            "Export daily or monthly sales summaries and receipt ledgers from Room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                    }
                    Button(
                        onClick = { showCsvExportSheet = true },
                        modifier = Modifier.height(38.dp).testTag("reports_export_csv_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Export", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Last 7 days bar chart
            SectionHeader("Last 7 Days")
            GroupCard {
                Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val maxBar = (last7Days.maxOfOrNull { it.totalCents } ?: 1L).coerceAtLeast(1L)
                    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        val barCount = last7Days.size
                        val slot = size.width / barCount
                        val barWidth = slot * 0.54f
                        last7Days.forEachIndexed { i, point ->
                            val frac = point.totalCents.toFloat() / maxBar.toFloat()
                            val barHeight = (size.height - 8f) * frac.coerceAtLeast(0.04f)
                            val left = i * slot + (slot - barWidth) / 2f
                            val top = size.height - barHeight
                            drawRoundRect(
                                color = if (point.totalCents > 0L) LoyGreen else Color(0xFFE0E0E0),
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8f, 8f),
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        last7Days.forEach { pt ->
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextSecondary,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Cash vs Card
            SectionHeader("Payment Methods")
            GroupCard {
                Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalPay = (cashCents + cardCents).coerceAtLeast(1L)
                    val cashFrac = cashCents.toFloat() / totalPay.toFloat()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cash: ${formatCents(cashCents, sym)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Card: ${formatCents(cardCents, sym)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = LoyInfo)
                    }
                    LinearProgressIndicator(
                        progress = { if (cashCents + cardCents == 0L) 0f else cashFrac },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                        color = LoyGreen,
                        trackColor = if (cardCents > 0L) LoyInfo else LoySurfaceVariant,
                    )
                }
            }

            // Category share
            if (categoryRows.isNotEmpty()) {
                SectionHeader("Rentals by Category")
                GroupCard {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val maxCat = categoryRows.maxOf { it.totalCents }.coerceAtLeast(1L)
                        categoryRows.forEach { row ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(row.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(formatCents(row.totalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.SemiBold)
                                }
                                LinearProgressIndicator(
                                    progress = { row.totalCents.toFloat() / maxCat.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                    color = LoyGreen,
                                    trackColor = LoySurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Top rented equipment
            if (topItems.isNotEmpty()) {
                SectionHeader("Top Rented Equipment")
                GroupCard {
                    Column {
                        topItems.forEachIndexed { idx, row ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${idx + 1}.", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary, modifier = Modifier.width(22.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${formatQty(row.qty)} units rented", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                Text(formatCents(row.totalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCsvExportSheet) {
        SalesCsvExportSheet(
            viewModel = viewModel,
            branches = data.branches,
            currencySymbol = sym,
            onDismiss = { showCsvExportSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesCsvExportSheet(
    viewModel: LoyPosViewModel,
    branches: List<BranchEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
) {
    val todayCal = remember { Calendar.getInstance() }
    var period by remember { mutableStateOf(SalesReportPeriod.DAILY) }
    var selectedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }
    var branchFilterId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var previewReport by remember { mutableStateOf<SalesCsvReportResult?>(null) }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val report = previewReport
        if (uri != null && report != null) {
            viewModel.writeSalesCsvToUri(uri, report)
            onDismiss()
        }
    }

    LaunchedEffect(period, selectedYear, selectedMonth, selectedDay, branchFilterId) {
        viewModel.loadSalesCsvPreview(
            period = period,
            year = selectedYear,
            monthZeroBased = selectedMonth,
            dayOfMonth = selectedDay,
            branchFilterId = branchFilterId,
        ) { result ->
            previewReport = result
        }
    }

    val monthNames = remember {
        arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val dateDisplayLabel = if (period == SalesReportPeriod.DAILY) {
        "${monthNames[selectedMonth]} $selectedDay, $selectedYear"
    } else {
        "${monthNames[selectedMonth]} $selectedYear"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Export Accounting Report (CSV)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = period == SalesReportPeriod.DAILY,
                    onClick = { period = SalesReportPeriod.DAILY },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("csv_period_daily"),
                ) {
                    Text("Daily Report", style = MaterialTheme.typography.labelLarge)
                }
                SegmentedButton(
                    selected = period == SalesReportPeriod.MONTHLY,
                    onClick = { period = SalesReportPeriod.MONTHLY },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("csv_period_monthly"),
                ) {
                    Text("Monthly Report", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Date or Month stepper + calendar picker
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay)
                                if (period == SalesReportPeriod.DAILY) {
                                    add(Calendar.DAY_OF_MONTH, -1)
                                } else {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    add(Calendar.MONTH, -1)
                                }
                            }
                            selectedYear = cal.get(Calendar.YEAR)
                            selectedMonth = cal.get(Calendar.MONTH)
                            selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                        },
                        modifier = Modifier.size(40.dp).testTag("csv_period_prev"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous period", modifier = Modifier.size(20.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = period == SalesReportPeriod.DAILY) { showDatePicker = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("csv_period_label"),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = "Select date",
                            tint = LoyGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(dateDisplayLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LoyTextPrimary)
                    }

                    IconButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay)
                                if (period == SalesReportPeriod.DAILY) {
                                    add(Calendar.DAY_OF_MONTH, 1)
                                } else {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    add(Calendar.MONTH, 1)
                                }
                            }
                            selectedYear = cal.get(Calendar.YEAR)
                            selectedMonth = cal.get(Calendar.MONTH)
                            selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                        },
                        modifier = Modifier.size(40.dp).testTag("csv_period_next"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next period", modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (branches.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = branchFilterId == null,
                        onClick = { branchFilterId = null },
                        label = { Text("All branches", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreenLight,
                            selectedLabelColor = LoyGreen,
                        ),
                    )
                    branches.take(2).forEach { branch ->
                        FilterChip(
                            selected = branchFilterId == branch.id,
                            onClick = { branchFilterId = branch.id },
                            label = { Text(branch.name, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreenLight,
                                selectedLabelColor = LoyGreen,
                            ),
                        )
                    }
                }
            }

            // Live Room preview of selected daily/monthly window
            val preview = previewReport
            GroupCard {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatBlock(
                            label = "Net Revenue",
                            value = formatCents(preview?.netTotalCents ?: 0L, currencySymbol),
                            valueColor = LoyGreen,
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Sales / Refunds",
                            value = "${preview?.completedCount ?: 0} / ${preview?.refundedCount ?: 0}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = if (preview != null) {
                            "File: ${preview.filename} · Includes summary${if (period == SalesReportPeriod.MONTHLY) ", daily breakdown," else ""} and transaction ledger."
                        } else {
                            "Loading Room sales data..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val fileName = preview?.filename ?: "loypos-sales-report.csv"
                        createCsvLauncher.launch(fileName)
                    },
                    enabled = preview != null,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("csv_save_button"),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Save CSV")
                }
                Button(
                    onClick = {
                        viewModel.shareSalesCsvReport(
                            period = period,
                            year = selectedYear,
                            monthZeroBased = selectedMonth,
                            dayOfMonth = selectedDay,
                            branchFilterId = branchFilterId,
                        )
                        onDismiss()
                    },
                    enabled = preview != null,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("csv_share_button"),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share CSV")
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showDatePicker) {
        val initialUtcMillis = remember(selectedYear, selectedMonth, selectedDay) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            selectedYear = utc.get(Calendar.YEAR)
                            selectedMonth = utc.get(Calendar.MONTH)
                            selectedDay = utc.get(Calendar.DAY_OF_MONTH)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
