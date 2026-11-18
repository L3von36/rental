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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatQty
import com.loypos.app.data.local.EventKitWithItems
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.ConfirmDialog
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenDarkText
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoyInfoLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.LoyWarningLight
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventKitsScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
    data: LoyPosDataState,
    onAddedToOrder: () -> Unit,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val sym = data.settings.currencySymbol
    val windowSize = LocalLoyWindowSize.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedKitId by remember { mutableStateOf<String?>(null) }
    var editingKit by remember { mutableStateOf<EventKitWithItems?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<EventKitWithItems?>(null) }

    val catalogById = remember(data.items) { data.items.associateBy { it.id } }

    val filteredKits = remember(data.eventKits, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            data.eventKits
        } else {
            data.eventKits.filter { entry ->
                entry.kit.name.lowercase().contains(q) ||
                    entry.kit.category.lowercase().contains(q) ||
                    entry.kit.description.lowercase().contains(q) ||
                    entry.items.any { it.itemName.lowercase().contains(q) }
            }
        }
    }
    val activeKit = remember(filteredKits, selectedKitId) {
        filteredKits.firstOrNull { it.kit.id == selectedKitId } ?: filteredKits.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
                title = {
                    Column {
                        Text(
                            text = "Event Kits & Bundles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${data.eventKits.size} packages ready for 1-click rental",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingKit = null
                    showEditor = true
                },
                containerColor = LoyGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_event_kit_button"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Create Event Kit")
            }
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Surface(color = LoySurface) {
                Column {
                    CompactSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search kits, categories, or included tools",
                        modifier = Modifier.padding(horizontal = metrics.screenPadding, vertical = 8.dp),
                        inputTestTag = "kits_search_input",
                    )
                    HorizontalDivider(color = LoyBorder)
                }
            }

            if (filteredKits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.Layers,
                        title = if (searchQuery.isNotBlank()) "No matching event kits" else "No event kits created yet",
                        subtitle = "Bundle chairs, tables, tents, and AV gear to add whole packages with one click.",
                        actionLabel = "Create Event Kit",
                        onAction = {
                            editingKit = null
                            showEditor = true
                        },
                    )
                }
            } else if (windowSize.useSplitPane) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                        contentPadding = PaddingValues(metrics.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredKits, key = { it.kit.id }) { kitWithItems ->
                            val kit = kitWithItems.kit
                            val isSelected = activeKit?.kit?.id == kit.id
                            val rawDaily = kitWithItems.items.sumOf { line ->
                                ((catalogById[line.itemId]?.priceCents ?: 0L) * line.qty).roundToLong()
                            }
                            val finalDaily = if (kit.discountPercent > 0.0) {
                                (rawDaily * (1.0 - kit.discountPercent / 100.0)).roundToLong()
                            } else {
                                rawDaily
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LoyGreenLight.copy(alpha = 0.25f) else LoySurface,
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) LoyGreen else LoyBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedKitId = kit.id }
                                    .testTag("event_kit_card_${kit.id}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = kit.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = LoyTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "${formatCents(finalDaily, sym)}/d",
                                            style = TabularMoneyStyle,
                                            fontWeight = FontWeight.Bold,
                                            color = LoyGreen,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PillBadge(
                                            text = kit.category.ifBlank { "Event Kit" },
                                            backgroundColor = LoyInfoLight,
                                            textColor = LoyInfo,
                                        )
                                        if (kit.discountPercent > 0.0) {
                                            PillBadge(
                                                text = "${kit.discountPercent.toInt()}% Off",
                                                backgroundColor = LoyGreenLight,
                                                textColor = LoyGreenDarkText,
                                            )
                                        }
                                        Text(
                                            text = "${kitWithItems.items.size} tools",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyTextSecondary,
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                    Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                        val kitDetail = activeKit
                        if (kitDetail == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    icon = Icons.Outlined.Layers,
                                    title = "Select an Event Kit",
                                    subtitle = "Pick a bundle on the left to inspect its included gear and add it to a rental order.",
                                )
                            }
                        } else {
                            EventKitInspectorPane(
                                kitWithItems = kitDetail,
                                catalogById = catalogById,
                                currencySymbol = sym,
                                onAddToOrder = {
                                    val outcome = transactionViewModel.addEventKit(kitDetail, data.items)
                                    if (outcome.addedLines > 0) {
                                        onAddedToOrder()
                                    }
                                },
                                onEdit = {
                                    editingKit = kitDetail
                                    showEditor = true
                                },
                                onDelete = { deleteCandidate = kitDetail },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = metrics.screenPadding,
                        end = metrics.screenPadding,
                        top = metrics.sectionGap,
                        bottom = 84.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                ) {
                    items(filteredKits, key = { it.kit.id }) { kitWithItems ->
                        EventKitCard(
                            kitWithItems = kitWithItems,
                            catalogById = catalogById,
                            currencySymbol = sym,
                            onAddToOrder = {
                                val outcome = transactionViewModel.addEventKit(kitWithItems, data.items)
                                if (outcome.addedLines > 0) {
                                    onAddedToOrder()
                                }
                            },
                            onEdit = {
                                editingKit = kitWithItems
                                showEditor = true
                            },
                            onDelete = { deleteCandidate = kitWithItems },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        EventKitEditorDialog(
            existing = editingKit,
            catalog = data.items.filter { !it.archived },
            currencySymbol = sym,
            onSave = { name, desc, category, discountPct, selectedLines ->
                viewModel.saveEventKit(
                    kitId = editingKit?.kit?.id,
                    name = name,
                    description = desc,
                    category = category,
                    discountPercent = discountPct,
                    lines = selectedLines,
                ) {
                    showEditor = false
                    editingKit = null
                }
            },
            onDismiss = {
                showEditor = false
                editingKit = null
            },
        )
    }

    deleteCandidate?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.kit.name}?",
            message = "This removes the bundle template. Existing rental orders will not be affected.",
            confirmLabel = "Delete Kit",
            destructive = true,
            onConfirm = {
                viewModel.deleteEventKit(target.kit.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null },
        )
    }
}

@Composable
private fun EventKitInspectorPane(
    kitWithItems: EventKitWithItems,
    catalogById: Map<String, ItemEntity>,
    currencySymbol: String,
    onAddToOrder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val kit = kitWithItems.kit
    val metrics = LocalLoyMetrics.current
    val rawDailyCents = remember(kitWithItems, catalogById) {
        kitWithItems.items.sumOf { line ->
            ((catalogById[line.itemId]?.priceCents ?: 0L) * line.qty).roundToLong()
        }
    }
    val discountedDailyCents = remember(rawDailyCents, kit.discountPercent) {
        if (kit.discountPercent > 0.0) {
            (rawDailyCents * (1.0 - kit.discountPercent / 100.0)).roundToLong()
        } else {
            rawDailyCents
        }
    }
    val totalUnits = remember(kitWithItems) { kitWithItems.items.sumOf { it.qty } }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = LoySurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.screenPadding, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(kit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${kit.category.ifBlank { "Event Kit" }} · ${kitWithItems.items.size} equipment types",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("edit_kit_${kit.id}")) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit Kit", tint = LoyGreen)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("delete_kit_${kit.id}")) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Kit", tint = LoyError)
                    }
                }
            }
        }
        HorizontalDivider(color = LoyBorder)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(metrics.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBlock(
                    label = "Bundle Rate / Day",
                    value = formatCents(discountedDailyCents, currencySymbol),
                    valueColor = LoyGreen,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "A La Carte Value",
                    value = formatCents(rawDailyCents, currencySymbol),
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "Total Units",
                    value = formatQty(totalUnits),
                    valueColor = LoyInfo,
                    modifier = Modifier.weight(1f),
                )
            }
            if (kit.description.isNotBlank()) {
                GroupCard {
                    Text(
                        text = kit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoyTextPrimary,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            GroupCard {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Included Equipment Manifest", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = LoyBorder)
                    kitWithItems.items.forEachIndexed { idx, line ->
                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                        val catalogItem = catalogById[line.itemId]
                        val stockOk = catalogItem == null || !catalogItem.trackStock || catalogItem.stock >= line.qty
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PillBadge(
                                text = "${formatQty(line.qty)}×",
                                backgroundColor = LoyGreenLight,
                                textColor = LoyGreenDarkText,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(catalogItem?.name ?: line.itemName, fontWeight = FontWeight.Medium)
                                if (catalogItem != null && catalogItem.trackStock) {
                                    Text(
                                        "${formatQty(catalogItem.stock)} available in warehouse",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (stockOk) LoyTextSecondary else LoyWarning,
                                    )
                                }
                            }
                            if (catalogItem != null) {
                                Text(
                                    "${formatCents((catalogItem.priceCents * line.qty).roundToLong(), currencySymbol)}/d",
                                    style = TabularMoneyStyle,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onAddToOrder,
                enabled = kitWithItems.items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_kit_to_order_${kit.id}"),
            ) {
                Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Entire Kit to Order", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EventKitCard(
    kitWithItems: EventKitWithItems,
    catalogById: Map<String, ItemEntity>,
    currencySymbol: String,
    onAddToOrder: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val kit = kitWithItems.kit
    val metrics = LocalLoyMetrics.current

    val rawDailyCents = remember(kitWithItems, catalogById) {
        kitWithItems.items.sumOf { line ->
            val unitPrice = catalogById[line.itemId]?.priceCents ?: 0L
            (unitPrice * line.qty).roundToLong()
        }
    }
    val discountedDailyCents = remember(rawDailyCents, kit.discountPercent) {
        if (kit.discountPercent > 0.0) {
            (rawDailyCents * (1.0 - kit.discountPercent / 100.0)).roundToLong()
        } else {
            rawDailyCents
        }
    }
    val hasShortage = remember(kitWithItems, catalogById) {
        kitWithItems.items.any { line ->
            val item = catalogById[line.itemId]
            item == null || (item.trackStock && item.stock < line.qty)
        }
    }
    val totalUnitCount = remember(kitWithItems) {
        kitWithItems.items.sumOf { it.qty }
    }

    GroupCard(modifier = Modifier.testTag("event_kit_card_${kit.id}")) {
        Column(
            modifier = Modifier.padding(metrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = kit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LoyTextPrimary,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillBadge(
                            text = kit.category.ifBlank { "Event Kit" },
                            backgroundColor = LoyInfoLight,
                            textColor = LoyInfo,
                        )
                        if (kit.discountPercent > 0.0) {
                            PillBadge(
                                text = "${kit.discountPercent.toInt()}% Bundle Off",
                                backgroundColor = LoyGreenLight,
                                textColor = LoyGreenDarkText,
                            )
                        }
                        if (hasShortage) {
                            PillBadge(
                                text = "Partial Stock",
                                backgroundColor = LoyWarningLight,
                                textColor = LoyWarning,
                            )
                        }
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("edit_kit_${kit.id}"),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit ${kit.name}",
                            tint = LoyTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("delete_kit_${kit.id}"),
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete ${kit.name}",
                            tint = LoyError,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (kit.description.isNotBlank()) {
                Text(
                    text = kit.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LoySurfaceVariant,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    kitWithItems.items.forEachIndexed { idx, line ->
                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                        val catalogItem = catalogById[line.itemId]
                        val stockOk = catalogItem == null || !catalogItem.trackStock || catalogItem.stock >= line.qty
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LoyGreenLight,
                            ) {
                                Text(
                                    text = "${formatQty(line.qty)}×",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LoyGreenDarkText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = catalogItem?.name ?: line.itemName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (catalogItem != null && catalogItem.trackStock) {
                                    Text(
                                        text = "${formatQty(catalogItem.stock)} available now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (stockOk) LoyTextSecondary else LoyWarning,
                                    )
                                }
                            }
                            if (catalogItem != null) {
                                Text(
                                    text = "${formatCents((catalogItem.priceCents * line.qty).roundToLong(), currencySymbol)}/d",
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                    color = LoyTextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${kitWithItems.items.size} tool types · ${formatQty(totalUnitCount)} total units",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                    Text(
                        text = "${formatCents(discountedDailyCents, currencySymbol)} / day bundle",
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                        fontWeight = FontWeight.Bold,
                        color = LoyGreen,
                    )
                }
                Button(
                    onClick = onAddToOrder,
                    enabled = kitWithItems.items.isNotEmpty(),
                    modifier = Modifier
                        .heightIn(min = metrics.minTouchTarget)
                        .testTag("add_kit_to_order_${kit.id}"),
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Kit to Order", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EventKitEditorDialog(
    existing: EventKitWithItems?,
    catalog: List<ItemEntity>,
    currencySymbol: String,
    onSave: (name: String, description: String, category: String, discountPercent: Double, lines: List<Triple<String, String, Double>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.kit?.name ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.kit?.description ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.kit?.category ?: "Wedding") }
    var discountInput by remember(existing) {
        mutableStateOf(
            existing?.kit?.discountPercent?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var itemSearch by remember { mutableStateOf("") }

    val selectedQuantities = remember(existing) {
        mutableStateMapOf<String, Double>().apply {
            existing?.items?.forEach { line ->
                put(line.itemId, line.qty.coerceAtLeast(1.0))
            }
        }
    }

    val filteredCatalog = remember(catalog, itemSearch) {
        val q = itemSearch.trim().lowercase()
        if (q.isEmpty()) catalog else catalog.filter {
            it.name.lowercase().contains(q) || it.sku.lowercase().contains(q)
        }
    }

    val canSave = name.isNotBlank() && selectedQuantities.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Create Event Kit" else "Edit Event Kit",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kit Name *") },
                    placeholder = { Text("e.g. Wedding Package (50 Guests)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("kit_name_input"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                    )
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Bundle % Off") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Package Summary") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                HorizontalDivider(color = LoyBorder)
                Text(
                    text = "Included Equipment (${selectedQuantities.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                CompactSearchField(
                    value = itemSearch,
                    onValueChange = { itemSearch = it },
                    placeholder = "Filter catalog equipment...",
                )

                filteredCatalog.take(30).forEach { item ->
                    val qty = selectedQuantities[item.id]
                    val isSelected = qty != null
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) LoyGreenLight else LoySurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) LoyGreen else LoyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (isSelected) {
                                    selectedQuantities.remove(item.id)
                                } else {
                                    selectedQuantities[item.id] = 1.0
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedQuantities[item.id] = qty ?: 1.0
                                    else selectedQuantities.remove(item.id)
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${formatCents(item.priceCents, currencySymbol)}/d · Stock ${formatQty(item.stock)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            if (isSelected && qty != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val next = (qty - 1.0).coerceAtLeast(1.0)
                                            selectedQuantities[item.id] = next
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease qty", modifier = Modifier.size(16.dp))
                                    }
                                    var qtyText by remember(item.id, qty) {
                                        mutableStateOf(formatQty(qty))
                                    }
                                    OutlinedTextField(
                                        value = qtyText,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }
                                            qtyText = digits
                                            digits.toDoubleOrNull()?.takeIf { it > 0.0 }?.let {
                                                selectedQuantities[item.id] = it
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(58.dp),
                                    )
                                    IconButton(
                                        onClick = { selectedQuantities[item.id] = qty + 1.0 },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Increase qty", tint = LoyGreen, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catalogMap = catalog.associateBy { it.id }
                    val lines = selectedQuantities.entries.mapNotNull { (itemId, q) ->
                        val item = catalogMap[itemId] ?: return@mapNotNull null
                        Triple(item.id, item.name, q.coerceAtLeast(1.0))
                    }
                    val disc = discountInput.toDoubleOrNull()?.coerceIn(0.0, 90.0) ?: 0.0
                    onSave(name, description, category, disc, lines)
                },
                enabled = canSave,
                modifier = Modifier.testTag("save_event_kit_button"),
            ) {
                Text("Save Kit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
