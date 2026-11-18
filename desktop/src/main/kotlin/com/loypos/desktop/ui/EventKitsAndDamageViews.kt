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
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Layers, contentDescription = null, tint = LoyGreenDark)
                            Text("Event Kits & Bundles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Group tents, chairs, tables, linens, and AV sound into reusable packages with 1-click Rent Desk ordering.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                    }
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Event Kit")
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search kits by package name, category, or included equipment...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HorizontalDivider(color = LoyBorder)

        if (filteredKits.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Layers, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(38.dp))
                    Text("No Event Kits match", style = MaterialTheme.typography.titleSmall, color = LoyTextSecondary)
                    Button(onClick = { showCreateDialog = true }) { Text("Create Event Kit") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredKits, key = { it.id }) { kit ->
                    val rawDailyTotalCents = kit.items.sumOf { ki ->
                        val equip = catalogMap[ki.itemId]
                        if (equip != null) (equip.priceCents * ki.qty).roundToLong() else 0L
                    }
                    val bundleDailyTotalCents = if (kit.discountPercent > 0.0) {
                        (rawDailyTotalCents * (1.0 - kit.discountPercent / 100.0)).roundToLong()
                    } else rawDailyTotalCents

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(kit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(shape = RoundedCornerShape(999.dp), color = LoyGreenLight) {
                                            Text(
                                                kit.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LoyGreenDark,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                        if (kit.discountPercent > 0.0) {
                                            Surface(shape = RoundedCornerShape(999.dp), color = LoyInfoLight) {
                                                Text(
                                                    "${formatQty(kit.discountPercent)}% Bundle Off",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LoyInfo,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    if (kit.description.isNotBlank()) {
                                        Text(kit.description, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${formatCents(bundleDailyTotalCents, sym)}/day",
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                        color = LoyGreenDark,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (kit.discountPercent > 0.0) {
                                        Text(
                                            "Standard ${formatCents(rawDailyTotalCents, sym)}/d",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoyBackground,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    kit.items.forEachIndexed { idx, line ->
                                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                                        val equip = catalogMap[line.itemId]
                                        val stockShort = equip != null && equip.trackStock && equip.stock < line.qty
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${formatQty(line.qty)} ×", fontWeight = FontWeight.Bold, color = LoyGreenDark)
                                                Text(equip?.name ?: line.itemName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            }
                                            if (equip != null) {
                                                Text(
                                                    text = "${formatQty(equip.stock)} avail · ${formatCents(equip.priceCents, sym)}/d",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (stockShort) LoyWarning else LoyTextSecondary,
                                                    fontWeight = if (stockShort) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            } else {
                                                Text("Not in fleet", style = MaterialTheme.typography.labelSmall, color = LoyError)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onAddKitToOrder(kit) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                                ) {
                                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Kit to Order (1-Click)", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(onClick = { editingKit = kit }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                IconButton(onClick = {
                                    repository.deleteEventKit(kit.id)
                                    onToast("Deleted ${kit.name}")
                                }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete kit", tint = LoyError)
                                }
                            }
                        }
                    }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kit / Package Name * (e.g. Wedding Package)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Bundle % Off") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Event notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(color = LoyBorder)
                Text(
                    "Included Equipment (${selectedQuantities.size} items selected)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = pickerFilter,
                    onValueChange = { pickerFilter = it },
                    placeholder = { Text("Filter equipment by name or SKU...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredEquipment, key = { it.id }) { equip ->
                        val qty = selectedQuantities[equip.id]
                        val checked = qty != null && qty > 0.0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (checked) LoyGreenLight.copy(alpha = 0.55f) else LoyBackground,
                            border = BorderStroke(1.dp, if (checked) LoyGreen else LoyBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease", modifier = Modifier.size(15.dp))
                                        }
                                        Text(formatQty(qty ?: 1.0), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(
                                            onClick = { selectedQuantities[equip.id] = (qty ?: 1.0) + 1.0 },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = "Increase", modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { selectedQuantities[equip.id] = (qty ?: 1.0) + 10.0 },
                                            modifier = Modifier.size(34.dp),
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

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = LoyWarning)
                            Text("Equipment Damage Report Log", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Document & photograph damaged returns with equipment IDs, inspection notes, repair costs, and maintenance status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                    }
                    Button(
                        onClick = {
                            initialOrderForDialog = null
                            editingReport = null
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LoyWarning),
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Damage Report")
                    }
                }

                // KPI Strip
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DamageKpiChip("Open", openCount.toString(), if (openCount > 0) LoyError else LoyTextPrimary, Modifier.weight(1f))
                    DamageKpiChip("Repairing", repairingCount.toString(), LoyWarning, Modifier.weight(1f))
                    DamageKpiChip("Resolved", resolvedCount.toString(), LoyGreenDark, Modifier.weight(1f))
                    DamageKpiChip("Est. Repair Total", formatCents(totalEstCostCents, sym), LoyInfo, Modifier.weight(1.3f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search by equipment ID, SKU, QR unit tag, client, or inspection notes...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    listOf(
                        "all" to "All (${snapshot.damageReports.size})",
                        "open" to "Open ($openCount)",
                        "repairing" to "Repairing ($repairingCount)",
                        "resolved" to "Resolved ($resolvedCount)",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreenLight,
                                selectedLabelColor = LoyGreenDark,
                            ),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(38.dp))
                    Text(
                        if (snapshot.damageReports.isEmpty()) "No damage reports logged yet" else "No matching damage reports",
                        style = MaterialTheme.typography.titleSmall,
                        color = LoyTextSecondary,
                    )
                    Button(onClick = {
                        initialOrderForDialog = null
                        editingReport = null
                        showCreateDialog = true
                    }) { Text("Document Returned Damage") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredReports, key = { it.id }) { report ->
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(999.dp), color = statusColors.first) {
                                            Text(
                                                report.status.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColors.second,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(999.dp), color = severityColors.first) {
                                            Text(
                                                report.severity.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = severityColors.second,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = LoyBackground) {
                                            Text(
                                                "ID: ${report.itemId}${if (report.itemSku.isNotBlank()) " · SKU: ${report.itemSku}" else ""}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            )
                                        }
                                        if (report.unitCode.isNotBlank()) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = LoyBackground) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                ) {
                                                    Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(12.dp), tint = LoyTextSecondary)
                                                    Text(report.unitCode, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                                }
                                            }
                                        }
                                    }
                                    Text(report.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val metaParts = listOfNotNull(
                                        report.orderNumber?.let { "Order #${it.toString().padStart(4, '0')}" },
                                        report.customerName.takeIf { it.isNotBlank() },
                                        "Logged ${formatDateTime(report.createdAt)}",
                                    )
                                    Text(metaParts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                if (report.repairCostCents > 0L) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Est. Repair", style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
                                        Text(
                                            formatCents(report.repairCostCents, sym),
                                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                            color = LoyError,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            if (report.notes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LoyBackground,
                                    border = BorderStroke(1.dp, LoyBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(report.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                                }
                            }

                            if (report.photoPaths.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(report.photoPaths, key = { it }) { path ->
                                        DamagePhotoThumbnail(
                                            path = path,
                                            sizeDp = 78,
                                            onClick = { zoomedPhotoPath = path },
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (report.status == "open") {
                                    OutlinedButton(onClick = {
                                        repository.updateDamageReportStatus(report.id, "repairing")
                                        onToast("Marked as under repair")
                                    }) {
                                        Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Start Repair")
                                    }
                                }
                                if (report.status != "resolved") {
                                    Button(
                                        onClick = {
                                            repository.updateDamageReportStatus(report.id, "resolved")
                                            onToast("Marked damage report resolved")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                                    ) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Mark Resolved")
                                    }
                                } else {
                                    OutlinedButton(onClick = {
                                        repository.updateDamageReportStatus(report.id, "open")
                                        onToast("Reopened damage report")
                                    }) { Text("Reopen") }
                                }
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(onClick = { editingReport = report }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                IconButton(onClick = {
                                    repository.deleteDamageReport(report.id)
                                    onToast("Deleted damage report")
                                }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete report", tint = LoyError)
                                }
                            }
                        }
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

@Composable
private fun DamageKpiChip(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LoyBackground,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
            Text(value, style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium), color = color, fontWeight = FontWeight.Bold)
        }
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Linked Rental Order dropdown
                if (snapshot.orders.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = orderMenuExpanded,
                        onExpandedChange = { orderMenuExpanded = !orderMenuExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedOrder?.let { o ->
                                "#${o.number.toString().padStart(4, '0')} · ${o.customerName ?: "Walk-in"}"
                            } ?: "No linked rental order (Depot inspection)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Linked Rental Order") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
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

                    val ord = selectedOrder
                    if (ord != null && ord.lines.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ord.lines) { line ->
                                val isSel = line.itemId != null && line.itemId == itemIdText
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        val eq = line.itemId?.let { id -> snapshot.items.firstOrNull { it.id == id } }
                                        itemIdText = line.itemId ?: ""
                                        itemSkuText = eq?.sku ?: ""
                                        itemNameText = line.name
                                    },
                                    label = { Text(line.name, maxLines = 1) },
                                )
                            }
                        }
                    }
                }

                // Equipment Picker Dropdown
                ExposedDropdownMenuBox(
                    expanded = equipMenuExpanded,
                    onExpandedChange = { equipMenuExpanded = !equipMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = itemNameText,
                        onValueChange = { itemNameText = it },
                        label = { Text("Damaged Equipment *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipMenuExpanded) },
                        singleLine = true,
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itemIdText,
                        onValueChange = { itemIdText = it },
                        label = { Text("Equipment ID *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = itemSkuText,
                        onValueChange = { itemSkuText = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = unitCodeText,
                    onValueChange = { unitCodeText = it },
                    label = { Text("Serialized QR Unit Tag (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (matchingUnits.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matchingUnits, key = { it.id }) { u ->
                            FilterChip(
                                selected = unitCodeText.equals(u.unitCode, ignoreCase = true),
                                onClick = { unitCodeText = if (unitCodeText == u.unitCode) "" else u.unitCode },
                                label = { Text(u.unitCode) },
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

                Text("Severity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("minor" to "Minor", "moderate" to "Moderate", "severe" to "Severe").forEach { (key, label) ->
                        FilterChip(
                            selected = severity == key,
                            onClick = { severity = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (key) {
                                    "severe" -> LoyErrorLight
                                    "moderate" -> LoyWarningLight
                                    else -> LoyInfoLight
                                },
                            ),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerNameText,
                        onValueChange = { customerNameText = it },
                        label = { Text("Customer / Renter") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                    )
                    OutlinedTextField(
                        value = repairCostText,
                        onValueChange = { repairCostText = it },
                        label = { Text("Est. Repair ($sym)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Damage Inspection Notes *") },
                    placeholder = { Text("Describe tear, bent frame, missing hardware, cleaning needed...") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Photo attachments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Damage Photos (${photoPaths.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = {
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
                    }) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Attach Photo...")
                    }
                }
                if (photoPaths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoPaths, key = { it }) { path ->
                            Box {
                                DamagePhotoThumbnail(path = path, sizeDp = 76, onClick = {})
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = LoyError,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
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
        shape = RoundedCornerShape(8.dp),
        color = LoyBackground,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = Modifier.size(sizeDp.dp).clickable(onClick = onClick),
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
