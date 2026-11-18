package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.parseAmountToCents
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopDamageReport
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopEventKitItem
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToLong

// ============================================================
// EVENT KITS & BUNDLES
// ============================================================

@Composable
fun DesktopEventKitsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onAddKitToOrder: (DesktopEventKit) -> Unit,
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var selectedKitId by remember { mutableStateOf<String?>(snapshot.eventKits.firstOrNull()?.id) }
    var editingKit by remember { mutableStateOf<DesktopEventKit?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val catalogMap = remember(snapshot.items) { snapshot.items.associateBy { it.id } }

    val filteredKits = remember(snapshot.eventKits, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) snapshot.eventKits
        else snapshot.eventKits.filter { kit ->
            kit.name.lowercase().contains(q) ||
                kit.category.lowercase().contains(q) ||
                kit.description.lowercase().contains(q) ||
                kit.items.any { it.itemName.lowercase().contains(q) }
        }
    }

    val activeKit = remember(filteredKits, selectedKitId) {
        filteredKits.firstOrNull { it.id == selectedKitId } ?: filteredKits.firstOrNull()
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Master List (42%)
        Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopSearchInput(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search packages, categories, or included gear...",
                        modifier = Modifier.weight(1f),
                    )
                    DesktopPrimaryButton(
                        text = "New Kit",
                        icon = Icons.Outlined.Add,
                        onClick = { showCreateDialog = true },
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (filteredKits.isEmpty()) {
                DesktopEmptyState(
                    icon = Icons.Outlined.Layers,
                    title = "No Event Kits Match",
                    subtitle = "Create a reusable bundle package to speed up Rent Desk checkouts.",
                    action = {
                        DesktopPrimaryButton(
                            text = "Create Event Kit",
                            icon = Icons.Outlined.Add,
                            onClick = { showCreateDialog = true },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                DesktopTableHeaderRow {
                    DesktopTableHeaderText("Package / Event Kit", Modifier.weight(1.5f))
                    DesktopTableHeaderText("Items", Modifier.width(64.dp))
                    DesktopTableHeaderText("Rate / Day", Modifier.width(96.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                    items(filteredKits, key = { it.id }) { kit ->
                        val rawDailyTotalCents = kit.items.sumOf { ki ->
                            val equip = catalogMap[ki.itemId]
                            if (equip != null) (equip.priceCents * ki.qty).roundToLong() else 0L
                        }
                        val bundleDailyTotalCents = if (kit.discountPercent > 0.0) {
                            (rawDailyTotalCents * (1.0 - kit.discountPercent / 100.0)).roundToLong()
                        } else rawDailyTotalCents
                        val isSelected = activeKit?.id == kit.id

                        DesktopTableRow(
                            selected = isSelected,
                            onClick = { selectedKitId = kit.id },
                        ) {
                            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = kit.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    DesktopBadge(kit.category, LoyGreenLight, LoyGreenDark)
                                    if (kit.discountPercent > 0.0) {
                                        DesktopBadge("${formatQty(kit.discountPercent)}% Off", LoyInfoLight, LoyInfo)
                                    }
                                }
                            }
                            Text(
                                text = "${kit.items.size} sku",
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextSecondary,
                                modifier = Modifier.width(64.dp),
                            )
                            Text(
                                text = formatCents(bundleDailyTotalCents, sym),
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                fontWeight = FontWeight.Bold,
                                color = LoyGreenDark,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(96.dp),
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Detail Inspector Pane (58%)
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .background(LoySurface),
        ) {
            val kit = activeKit
            if (kit == null) {
                DesktopEmptyState(
                    icon = Icons.Outlined.Layers,
                    title = "Select an Event Kit",
                    subtitle = "Choose any equipment bundle from the list to inspect pricing, stock readiness, and itemized gear.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val rawDailyTotalCents = kit.items.sumOf { ki ->
                    val equip = catalogMap[ki.itemId]
                    if (equip != null) (equip.priceCents * ki.qty).roundToLong() else 0L
                }
                val bundleDailyTotalCents = if (kit.discountPercent > 0.0) {
                    (rawDailyTotalCents * (1.0 - kit.discountPercent / 100.0)).roundToLong()
                } else rawDailyTotalCents
                val savingsCents = (rawDailyTotalCents - bundleDailyTotalCents).coerceAtLeast(0L)
                val shortCount = kit.items.count { ki ->
                    val eq = catalogMap[ki.itemId]
                    eq == null || (eq.trackStock && eq.stock < ki.qty)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(kit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                DesktopBadge(kit.category, LoyGreenLight, LoyGreenDark)
                                if (kit.discountPercent > 0.0) {
                                    DesktopBadge("${formatQty(kit.discountPercent)}% Bundle Off", LoyInfoLight, LoyInfo)
                                }
                            }
                            if (kit.description.isNotBlank()) {
                                Text(kit.description, style = MaterialTheme.typography.bodyMedium, color = LoyTextSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${formatCents(bundleDailyTotalCents, sym)}/day",
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                                color = LoyGreenDark,
                                fontWeight = FontWeight.Bold,
                            )
                            if (savingsCents > 0L) {
                                Text(
                                    "Std ${formatCents(rawDailyTotalCents, sym)}/d · Save ${formatCents(savingsCents, sym)}/d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyInfo,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (shortCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LoyWarningLight,
                            border = BorderStroke(1.dp, LoyWarning.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = LoyWarning, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "$shortCount included equipment item(s) currently below kit quantity in available depot stock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyWarning,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    DesktopSectionCard(
                        title = "Bundled Equipment Manifest (${kit.items.size} SKUs)",
                        subtitle = "Quantities automatically added per booking day",
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        DesktopTableHeaderRow {
                            DesktopTableHeaderText("Qty", Modifier.width(64.dp))
                            DesktopTableHeaderText("Equipment", Modifier.weight(1.4f))
                            DesktopTableHeaderText("Availability", Modifier.weight(1.0f))
                            DesktopTableHeaderText("Subtotal/d", Modifier.width(92.dp))
                        }
                        kit.items.forEach { line ->
                            val equip = catalogMap[line.itemId]
                            val stockShort = equip != null && equip.trackStock && equip.stock < line.qty
                            val lineSub = if (equip != null) (equip.priceCents * line.qty).roundToLong() else 0L
                            DesktopTableRow {
                                Text(
                                    text = "${formatQty(line.qty)} ×",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LoyGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(64.dp),
                                )
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(
                                        text = equip?.name ?: line.itemName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = if (equip != null) "${equip.sku.ifBlank { equip.id }} · ${formatCents(equip.priceCents, sym)}/d each" else line.itemId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextMuted,
                                    )
                                }
                                Box(modifier = Modifier.weight(1.0f)) {
                                    when {
                                        equip == null -> DesktopBadge("Missing SKU", LoyErrorLight, LoyError)
                                        !equip.trackStock -> DesktopBadge("Unlimited", LoySurfaceVariant, LoyTextSecondary)
                                        stockShort -> DesktopBadge("${formatQty(equip.stock)} avail (short)", LoyWarningLight, LoyWarning)
                                        else -> DesktopBadge("${formatQty(equip.stock)} avail", LoyGreenLight, LoyGreenDark)
                                    }
                                }
                                Text(
                                    text = formatCents(lineSub, sym),
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(92.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier.fillMaxWidth().background(LoySurfaceHover).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopSecondaryButton(
                        text = "Edit",
                        icon = Icons.Outlined.Edit,
                        onClick = { editingKit = kit },
                    )
                    DesktopSecondaryButton(
                        text = "Delete",
                        icon = Icons.Outlined.DeleteOutline,
                        contentColor = LoyError,
                        onClick = {
                            repository.deleteEventKit(kit.id)
                            selectedKitId = null
                            onToast("Deleted ${kit.name}")
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    DesktopPrimaryButton(
                        text = "Add Kit to Order (1-Click)",
                        icon = Icons.Outlined.AddShoppingCart,
                        onClick = { onAddKitToOrder(kit) },
                    )
                }
            }
        }
    }

    if (showCreateDialog || editingKit != null) {
        DesktopEventKitEditorDialog(
            initial = editingKit,
            snapshot = snapshot,
            onSave = { kit ->
                repository.saveEventKit(kit)
                selectedKitId = kit.id
                onToast("Saved event kit \"${kit.name}\"")
                showCreateDialog = false
                editingKit = null
            },
            onDismiss = {
                showCreateDialog = false
                editingKit = null
            },
        )
    }
}

@Composable
private fun DesktopEventKitEditorDialog(
    initial: DesktopEventKit?,
    snapshot: DesktopStoreSnapshot,
    onSave: (DesktopEventKit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Event Package") }
    var discountText by remember {
        mutableStateOf(
            if (initial == null || initial.discountPercent <= 0.0) "" else formatQty(initial.discountPercent)
        )
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var pickerFilter by remember { mutableStateOf("") }

    val selectedQuantities = remember(initial) {
        mutableStateMapOf<String, Double>().apply {
            initial?.items?.forEach { put(it.itemId, it.qty) }
        }
    }

    val filteredEquipment = remember(snapshot.items, pickerFilter) {
        val q = pickerFilter.trim().lowercase()
        if (q.isEmpty()) snapshot.items
        else snapshot.items.filter {
            it.name.lowercase().contains(q) || it.sku.lowercase().contains(q) || it.id.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Event Kit" else "Edit Event Kit", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.width(560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DesktopFormInput("Kit / Package Name * (e.g. Wedding Package)", name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Category", category, { category = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Bundle % Off", discountText, { discountText = it }, modifier = Modifier.weight(0.7f))
                }
                DesktopFormInput("Description / Event Notes", description, { description = it })
                HorizontalDivider(color = LoyBorder)
                Text(
                    "Included Equipment (${selectedQuantities.size} items selected)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                DesktopSearchInput(
                    value = pickerFilter,
                    onValueChange = { pickerFilter = it },
                    placeholder = "Filter equipment by name or SKU...",
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredEquipment, key = { it.id }) { equip ->
                        val qty = selectedQuantities[equip.id]
                        val checked = qty != null && qty > 0.0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (checked) LoyGreenLight.copy(alpha = 0.45f) else LoyBackground,
                            border = BorderStroke(1.dp, if (checked) LoyGreen else LoyBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        if (checked) selectedQuantities.remove(equip.id)
                                        else selectedQuantities[equip.id] = 1.0
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedQuantities[equip.id] = 1.0
                                        else selectedQuantities.remove(equip.id)
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(equip.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${equip.sku} · ${formatQty(equip.stock)} avail", style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
                                }
                                if (checked) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val next = (qty ?: 1.0) - 1.0
                                                if (next <= 0.0) selectedQuantities.remove(equip.id) else selectedQuantities[equip.id] = next
                                            },
                                            modifier = Modifier.size(26.dp),
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                        }
                                        Text(formatQty(qty ?: 1.0), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(
                                            onClick = { selectedQuantities[equip.id] = (qty ?: 1.0) + 1.0 },
                                            modifier = Modifier.size(26.dp),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(
                                            onClick = { selectedQuantities[equip.id] = (qty ?: 1.0) + 10.0 },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Text("+10", style = MaterialTheme.typography.labelSmall, color = LoyGreenDark, fontWeight = FontWeight.Bold)
                                        }
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
                    val catalogMap = snapshot.items.associateBy { it.id }
                    val itemsList = selectedQuantities.entries.mapNotNull { (itemId, q) ->
                        if (q <= 0.0) null
                        else DesktopEventKitItem(
                            itemId = itemId,
                            itemName = catalogMap[itemId]?.name ?: itemId,
                            qty = q,
                        )
                    }
                    onSave(
                        DesktopEventKit(
                            id = initial?.id ?: newId(),
                            name = name.trim(),
                            description = description.trim(),
                            category = category.trim().ifEmpty { "Event Package" },
                            discountPercent = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 90.0),
                            createdAt = initial?.createdAt ?: 0L,
                            items = itemsList,
                        )
                    )
                },
                enabled = name.trim().isNotEmpty() && selectedQuantities.isNotEmpty(),
            ) {
                Text("Save Kit")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ============================================================
// EQUIPMENT DAMAGE REPORT LOG & PHOTO DOCUMENTATION
// ============================================================

@Composable
fun DesktopDamageReportsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    prefillOrder: DesktopRentalOrder? = null,
    onConsumePrefillOrder: () -> Unit = {},
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") } // all | open | repairing | resolved
    var selectedReportId by remember { mutableStateOf<String?>(snapshot.damageReports.firstOrNull()?.id) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingReport by remember { mutableStateOf<DesktopDamageReport?>(null) }
    var initialOrderForDialog by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefillOrder) {
        if (prefillOrder != null) {
            initialOrderForDialog = prefillOrder
            showCreateDialog = true
            onConsumePrefillOrder()
        }
    }

    val openCount = remember(snapshot.damageReports) { snapshot.damageReports.count { it.status == "open" } }
    val repairingCount = remember(snapshot.damageReports) { snapshot.damageReports.count { it.status == "repairing" } }
    val resolvedCount = remember(snapshot.damageReports) { snapshot.damageReports.count { it.status == "resolved" } }
    val totalEstCostCents = remember(snapshot.damageReports) { snapshot.damageReports.sumOf { it.repairCostCents } }

    val filteredReports = remember(snapshot.damageReports, search, statusFilter) {
        val q = search.trim().lowercase()
        snapshot.damageReports.filter { r ->
            val matchStatus = statusFilter == "all" || r.status == statusFilter
            val matchQuery = q.isEmpty() ||
                r.itemId.lowercase().contains(q) ||
                r.itemSku.lowercase().contains(q) ||
                r.itemName.lowercase().contains(q) ||
                r.unitCode.lowercase().contains(q) ||
                r.customerName.lowercase().contains(q) ||
                r.notes.lowercase().contains(q) ||
                (r.orderNumber != null && "#${r.orderNumber}".contains(q))
            matchStatus && matchQuery
        }
    }

    val activeReport = remember(filteredReports, selectedReportId) {
        filteredReports.firstOrNull { it.id == selectedReportId } ?: filteredReports.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopKpiTile("Open", openCount.toString(), if (openCount > 0) LoyError else LoyGreen, Modifier.weight(1f))
                    DesktopKpiTile("Repairing", repairingCount.toString(), LoyWarning, Modifier.weight(1f))
                    DesktopKpiTile("Resolved", resolvedCount.toString(), LoyGreen, Modifier.weight(1f))
                    DesktopKpiTile("Est. Repair Total", formatCents(totalEstCostCents, sym), LoyInfo, Modifier.weight(1.2f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DesktopSearchInput(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search by equipment ID, SKU, QR unit tag, client, or inspection notes...",
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSegmentedControl(
                        options = listOf(
                            "all" to "All (${snapshot.damageReports.size})",
                            "open" to "Open ($openCount)",
                            "repairing" to "Repair ($repairingCount)",
                            "resolved" to "Resolved ($resolvedCount)",
                        ),
                        selected = statusFilter,
                        onSelect = { statusFilter = it },
                    )
                    DesktopPrimaryButton(
                        text = "Log Damage",
                        icon = Icons.Outlined.AddAPhoto,
                        containerColor = LoyWarning,
                        onClick = {
                            initialOrderForDialog = null
                            editingReport = null
                            showCreateDialog = true
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Master Table (48%)
            Column(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
                if (filteredReports.isEmpty()) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.ReportProblem,
                        title = if (snapshot.damageReports.isEmpty()) "No Damage Reports Logged" else "No Matching Reports",
                        subtitle = "Attach equipment IDs, inspection notes, repair estimates, and damage photos.",
                        action = {
                            DesktopPrimaryButton(
                                text = "Document Returned Damage",
                                icon = Icons.Outlined.AddAPhoto,
                                containerColor = LoyWarning,
                                onClick = {
                                    initialOrderForDialog = null
                                    editingReport = null
                                    showCreateDialog = true
                                },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("Equipment / ID", Modifier.weight(1.4f))
                        DesktopTableHeaderText("Order / Client", Modifier.weight(1.0f))
                        DesktopTableHeaderText("Status", Modifier.width(104.dp))
                        DesktopTableHeaderText("Repair", Modifier.width(82.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(filteredReports, key = { it.id }) { report ->
                            val statusColors = when (report.status) {
                                "resolved" -> LoyGreenLight to LoyGreenDark
                                "repairing" -> LoyWarningLight to LoyWarning
                                else -> LoyErrorLight to LoyError
                            }
                            DesktopTableRow(
                                selected = activeReport?.id == report.id,
                                onClick = { selectedReportId = report.id },
                            ) {
                                Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = report.itemName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            append(report.itemId)
                                            if (report.unitCode.isNotBlank()) append(" · ${report.unitCode}")
                                            if (report.photoPaths.isNotEmpty()) append(" · ${report.photoPaths.size} photo(s)")
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = LoyTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = report.orderNumber?.let { "#${it.toString().padStart(4, '0')}" } ?: "Depot",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = report.customerName.ifBlank { "Standalone check" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Box(modifier = Modifier.width(104.dp)) {
                                    DesktopBadge(
                                        text = report.status.replaceFirstChar { it.uppercase() },
                                        bgColor = statusColors.first,
                                        fgColor = statusColors.second,
                                    )
                                }
                                Text(
                                    text = if (report.repairCostCents > 0L) formatCents(report.repairCostCents, sym) else "—",
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.repairCostCents > 0L) LoyError else LoyTextMuted,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(82.dp),
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(color = LoyBorder)

            // Right Detail Inspector Pane (52%)
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .background(LoySurface),
            ) {
                val report = activeReport
                if (report == null) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.ReportProblem,
                        title = "Select a Damage Report",
                        subtitle = "Choose any log entry on the left to inspect photos, notes, equipment IDs, and repair status.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val statusColors = when (report.status) {
                        "resolved" -> LoyGreenLight to LoyGreenDark
                        "repairing" -> LoyWarningLight to LoyWarning
                        else -> LoyErrorLight to LoyError
                    }
                    val severityColors = when (report.severity) {
                        "severe" -> LoyErrorLight to LoyError
                        "moderate" -> LoyWarningLight to LoyWarning
                        else -> LoyInfoLight to LoyInfo
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    DesktopBadge(report.status.replaceFirstChar { it.uppercase() }, statusColors.first, statusColors.second)
                                    DesktopBadge("${report.severity.replaceFirstChar { it.uppercase() }} Severity", severityColors.first, severityColors.second)
                                }
                                Text(report.itemName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = LoySurfaceVariant) {
                                        Text(
                                            "ID: ${report.itemId}${if (report.itemSku.isNotBlank()) " · SKU: ${report.itemSku}" else ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                    if (report.unitCode.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = LoySurfaceVariant) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(12.dp), tint = LoyTextSecondary)
                                                Text(report.unitCode, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                            }
                                        }
                                    }
                                }
                            }
                            if (report.repairCostCents > 0L) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("EST. REPAIR", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        formatCents(report.repairCostCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                                        color = LoyError,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        DesktopSectionCard(title = "Return & Audit Metadata") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LINKED ORDER", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        report.orderNumber?.let { "#${it.toString().padStart(4, '0')}" } ?: "Standalone Depot Check",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CLIENT / RENTER", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        report.customerName.ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LOGGED AT", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(formatDateTime(report.createdAt), style = MaterialTheme.typography.bodySmall)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("RESOLVED AT", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(report.resolvedAt?.let { formatDateTime(it) } ?: "Pending", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        DesktopSectionCard(title = "Inspection Notes") {
                            Text(
                                text = report.notes.ifBlank { "No written inspection notes attached." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (report.notes.isBlank()) LoyTextMuted else LoyTextPrimary,
                            )
                        }

                        DesktopSectionCard(
                            title = "Photo Evidence (${report.photoPaths.size})",
                            subtitle = if (report.photoPaths.isEmpty()) "Click Edit to attach damage photos" else "Click any photo thumbnail to inspect full size",
                        ) {
                            if (report.photoPaths.isEmpty()) {
                                Text("No photos attached to this report.", style = MaterialTheme.typography.bodySmall, color = LoyTextMuted)
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(report.photoPaths, key = { it }) { path ->
                                        DamagePhotoThumbnail(
                                            path = path,
                                            sizeDp = 112,
                                            onClick = { zoomedPhotoPath = path },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(LoySurfaceHover).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (report.status == "open") {
                            DesktopSecondaryButton(
                                text = "Start Repair",
                                icon = Icons.Outlined.Build,
                                onClick = {
                                    repository.updateDamageReportStatus(report.id, "repairing")
                                    onToast("Marked as under repair")
                                },
                            )
                        }
                        if (report.status != "resolved") {
                            DesktopPrimaryButton(
                                text = "Mark Resolved",
                                icon = Icons.Outlined.CheckCircle,
                                onClick = {
                                    repository.updateDamageReportStatus(report.id, "resolved")
                                    onToast("Marked damage report resolved")
                                },
                            )
                        } else {
                            DesktopSecondaryButton(
                                text = "Reopen",
                                onClick = {
                                    repository.updateDamageReportStatus(report.id, "open")
                                    onToast("Reopened damage report")
                                },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        DesktopSecondaryButton(
                            text = "Edit",
                            icon = Icons.Outlined.Edit,
                            onClick = { editingReport = report },
                        )
                        DesktopSecondaryButton(
                            text = "Delete",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = {
                                repository.deleteDamageReport(report.id)
                                selectedReportId = null
                                onToast("Deleted damage report")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingReport != null) {
        DesktopDamageReportDialog(
            repository = repository,
            snapshot = snapshot,
            initialReport = editingReport,
            prefillOrder = initialOrderForDialog,
            onSave = { report, flagUnitMaintenance ->
                repository.saveDamageReport(report, markUnitMaintenance = flagUnitMaintenance)
                selectedReportId = report.id
                onToast("Saved damage report for ${report.itemName}")
                showCreateDialog = false
                editingReport = null
                initialOrderForDialog = null
            },
            onDismiss = {
                showCreateDialog = false
                editingReport = null
                initialOrderForDialog = null
            },
        )
    }

    zoomedPhotoPath?.let { path ->
        val bmp = rememberDesktopImageBitmap(path)
        AlertDialog(
            onDismissRequest = { zoomedPhotoPath = null },
            title = { Text("Damage Inspection Photo") },
            text = {
                Box(modifier = Modifier.width(620.dp).heightIn(max = 460.dp), contentAlignment = Alignment.Center) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Zoomed damage photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Text("Image file not found on disk:\n$path", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { Button(onClick = { zoomedPhotoPath = null }) { Text("Close") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopDamageReportDialog(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    initialReport: DesktopDamageReport?,
    prefillOrder: DesktopRentalOrder?,
    onSave: (DesktopDamageReport, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    val initialEquip = remember(initialReport, prefillOrder, snapshot.items) {
        initialReport?.let { rep -> snapshot.items.firstOrNull { it.id == rep.itemId } }
            ?: prefillOrder?.lines?.firstOrNull()?.itemId?.let { id -> snapshot.items.firstOrNull { it.id == id } }
            ?: snapshot.items.firstOrNull()
    }
    var selectedOrder by remember(initialReport, prefillOrder, snapshot.orders) {
        mutableStateOf(
            prefillOrder
                ?: initialReport?.orderId?.let { id -> snapshot.orders.firstOrNull { it.id == id } }
        )
    }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var equipMenuExpanded by remember { mutableStateOf(false) }

    var itemIdText by remember { mutableStateOf(initialReport?.itemId ?: initialEquip?.id ?: "") }
    var itemSkuText by remember { mutableStateOf(initialReport?.itemSku ?: initialEquip?.sku ?: "") }
    var itemNameText by remember { mutableStateOf(initialReport?.itemName ?: initialEquip?.name ?: "") }
    var unitCodeText by remember { mutableStateOf(initialReport?.unitCode ?: "") }
    var customerNameText by remember {
        mutableStateOf(initialReport?.customerName ?: prefillOrder?.customerName.orEmpty())
    }
    var severity by remember { mutableStateOf(initialReport?.severity ?: "minor") }
    var repairCostText by remember {
        mutableStateOf(
            if (initialReport == null || initialReport.repairCostCents <= 0L) ""
            else "%.2f".format(initialReport.repairCostCents / 100.0)
        )
    }
    var notesText by remember { mutableStateOf(initialReport?.notes ?: "") }
    var photoPaths by remember { mutableStateOf(initialReport?.photoPaths ?: emptyList()) }
    var flagUnitMaintenance by remember { mutableStateOf(initialReport == null) }

    val matchingUnits = remember(snapshot.equipmentUnits, itemIdText) {
        snapshot.equipmentUnits.filter { it.itemId == itemIdText }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialReport == null) "Document Equipment Damage" else "Edit Damage Report", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.width(560.dp).heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (snapshot.orders.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Linked Rental Order", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                        ExposedDropdownMenuBox(
                            expanded = orderMenuExpanded,
                            onExpandedChange = { orderMenuExpanded = !orderMenuExpanded },
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LoySurface,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        selectedOrder?.let { o -> "#${o.number.toString().padStart(4, '0')} · ${o.customerName ?: "Walk-in"}" }
                                            ?: "No linked rental order (Depot inspection)",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuExpanded)
                                }
                            }
                            ExposedDropdownMenu(expanded = orderMenuExpanded, onDismissRequest = { orderMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("None (Standalone depot inspection)") },
                                    onClick = { selectedOrder = null; orderMenuExpanded = false },
                                )
                                snapshot.orders.take(20).forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text("#${order.number.toString().padStart(4, '0')} · ${order.customerName ?: "Walk-in"} (${order.eventName})") },
                                        onClick = {
                                            selectedOrder = order
                                            if (customerNameText.isBlank()) customerNameText = order.customerName.orEmpty()
                                            order.lines.firstOrNull()?.let { l ->
                                                val eq = l.itemId?.let { id -> snapshot.items.firstOrNull { it.id == id } }
                                                itemIdText = l.itemId ?: eq?.id ?: itemIdText
                                                itemSkuText = eq?.sku ?: ""
                                                itemNameText = l.name
                                            }
                                            orderMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    val ord = selectedOrder
                    if (ord != null && ord.lines.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ord.lines) { line ->
                                val isSel = line.itemId != null && line.itemId == itemIdText
                                DesktopFilterPill(
                                    label = line.name,
                                    selected = isSel,
                                    onClick = {
                                        val eq = line.itemId?.let { id -> snapshot.items.firstOrNull { it.id == id } }
                                        itemIdText = line.itemId ?: ""
                                        itemSkuText = eq?.sku ?: ""
                                        itemNameText = line.name
                                    },
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Damaged Equipment *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                    ExposedDropdownMenuBox(
                        expanded = equipMenuExpanded,
                        onExpandedChange = { equipMenuExpanded = !equipMenuExpanded },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LoySurface,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .height(36.dp)
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(itemNameText.ifBlank { "Select equipment..." }, style = MaterialTheme.typography.bodySmall)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipMenuExpanded)
                            }
                        }
                        ExposedDropdownMenu(expanded = equipMenuExpanded, onDismissRequest = { equipMenuExpanded = false }) {
                            snapshot.items.forEach { eq ->
                                DropdownMenuItem(
                                    text = { Text("${eq.name} (${eq.id} · ${eq.sku})") },
                                    onClick = {
                                        itemIdText = eq.id
                                        itemSkuText = eq.sku
                                        itemNameText = eq.name
                                        equipMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Equipment ID *", itemIdText, { itemIdText = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("SKU", itemSkuText, { itemSkuText = it }, modifier = Modifier.weight(1f))
                }

                DesktopFormInput("Serialized QR Unit Tag (Optional)", unitCodeText, { unitCodeText = it })
                if (matchingUnits.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingUnits, key = { it.id }) { u ->
                            DesktopFilterPill(
                                label = u.unitCode,
                                selected = unitCodeText.equals(u.unitCode, ignoreCase = true),
                                onClick = { unitCodeText = if (unitCodeText == u.unitCode) "" else u.unitCode },
                            )
                        }
                    }
                }
                if (unitCodeText.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Flag QR unit as Maintenance", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = flagUnitMaintenance, onCheckedChange = { flagUnitMaintenance = it })
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Severity", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                    DesktopSegmentedControl(
                        options = listOf("minor" to "Minor", "moderate" to "Moderate", "severe" to "Severe"),
                        selected = severity,
                        onSelect = { severity = it },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Customer / Renter", customerNameText, { customerNameText = it }, modifier = Modifier.weight(1.2f))
                    DesktopFormInput("Est. Repair ($sym)", repairCostText, { repairCostText = it }, modifier = Modifier.weight(0.8f))
                }

                DesktopFormInput(
                    label = "Damage Inspection Notes *",
                    value = notesText,
                    onValueChange = { notesText = it },
                    singleLine = false,
                    minLines = 3,
                    placeholder = "Describe tear, bent frame, missing hardware, cleaning needed...",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Damage Photos (${photoPaths.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    DesktopSecondaryButton(
                        text = "Attach Photo...",
                        icon = Icons.Outlined.AddAPhoto,
                        onClick = {
                            val chooser = JFileChooser().apply {
                                dialogTitle = "Attach Equipment Damage Photos"
                                isMultiSelectionEnabled = true
                                fileFilter = FileNameExtensionFilter("Images (jpg, png, webp, gif)", "jpg", "jpeg", "png", "webp", "gif")
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val picked: List<File> = chooser.selectedFiles.toList().ifEmpty {
                                    listOfNotNull(chooser.selectedFile)
                                }
                                val saved = picked.mapNotNull { file ->
                                    runCatching { repository.saveDamagePhoto(file) }.getOrNull()
                                }
                                photoPaths = (photoPaths + saved).distinct()
                            }
                        },
                    )
                }
                if (photoPaths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoPaths, key = { it }) { path ->
                            Box {
                                DamagePhotoThumbnail(path = path, sizeDp = 74, onClick = {})
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = LoyError,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { photoPaths = photoPaths.filterNot { it == path } },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(12.dp))
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
                    val rep = DesktopDamageReport(
                        id = initialReport?.id ?: newId(),
                        itemId = itemIdText.trim(),
                        itemSku = itemSkuText.trim(),
                        itemName = itemNameText.trim().ifEmpty { itemIdText.trim() },
                        unitCode = unitCodeText.trim(),
                        orderId = selectedOrder?.id,
                        orderNumber = selectedOrder?.number,
                        customerName = customerNameText.trim(),
                        severity = severity,
                        status = initialReport?.status ?: "open",
                        repairCostCents = parseAmountToCents(repairCostText) ?: 0L,
                        notes = notesText.trim(),
                        photoPaths = photoPaths,
                        createdAt = initialReport?.createdAt ?: 0L,
                        resolvedAt = initialReport?.resolvedAt,
                    )
                    onSave(rep, flagUnitMaintenance)
                },
                enabled = itemIdText.trim().isNotEmpty() && (notesText.trim().isNotEmpty() || photoPaths.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = LoyWarning),
            ) {
                Text("Save Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun rememberDesktopImageBitmap(path: String): ImageBitmap? = remember(path) {
    runCatching {
        val file = File(path)
        if (!file.exists()) null else ImageIO.read(file)?.toComposeImageBitmap()
    }.getOrNull()
}

@Composable
private fun DamagePhotoThumbnail(
    path: String,
    sizeDp: Int,
    onClick: () -> Unit,
) {
    val bmp = rememberDesktopImageBitmap(path)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LoyBackground,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = Modifier.size(sizeDp.dp).pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick),
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "Equipment damage photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = LoyTextSecondary)
            }
        }
    }
}
