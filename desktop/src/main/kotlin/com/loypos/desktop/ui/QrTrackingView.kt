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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.DesktopQrCodeUtils
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopEquipmentUnit
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTrackingView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    var leftTab by remember { mutableIntStateOf(0) } // 0=Units, 1=Catalog Generate, 2=Scan Log
    var scanInput by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var scanMode by remember { mutableStateOf("check_out") }
    var selectedUnitId by remember { mutableStateOf<String?>(snapshot.equipmentUnits.firstOrNull()?.id) }
    var selectedItemForGenId by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var orderExpanded by remember { mutableStateOf(false) }
    var conditionNote by remember { mutableStateOf("") }

    val activeOrders = remember(snapshot.orders) {
        snapshot.orders.filter { it.isRentalOut }
    }
    val selectedOrder = remember(activeOrders, selectedOrderId) {
        activeOrders.firstOrNull { it.id == selectedOrderId }
    }

    val checkedInCount = snapshot.equipmentUnits.count { !it.isCheckedOut && !it.isMaintenance }
    val checkedOutCount = snapshot.equipmentUnits.count { it.isCheckedOut }
    val maintenanceCount = snapshot.equipmentUnits.count { it.isMaintenance }

    val filteredUnits = remember(snapshot.equipmentUnits, search, statusFilter) {
        val q = search.trim().lowercase()
        snapshot.equipmentUnits.filter { u ->
            val mStatus = statusFilter == "all" || u.status == statusFilter
            val mSearch = q.isEmpty() || u.itemName.lowercase().contains(q) || u.unitCode.lowercase().contains(q) || u.currentCustomerName.lowercase().contains(q)
            mStatus && mSearch
        }
    }

    val selectedUnit = remember(snapshot.equipmentUnits, selectedUnitId) {
        snapshot.equipmentUnits.firstOrNull { it.id == selectedUnitId } ?: snapshot.equipmentUnits.firstOrNull()
    }
    val selectedItemForGen = remember(snapshot.items, selectedItemForGenId, selectedUnit) {
        snapshot.items.firstOrNull { it.id == (selectedItemForGenId ?: selectedUnit?.itemId) }
    }

    fun handleQrScanLookup(raw: String) {
        val target = repository.resolveQrTarget(raw)
        if (target == null) {
            onToast("No equipment or unit matches \"$raw\"")
            return
        }
        val unit = target.unit
        if (unit != null) {
            selectedUnitId = unit.id
            selectedItemForGenId = null
        } else {
            selectedItemForGenId = target.item.id
            selectedUnitId = snapshot.equipmentUnits.firstOrNull { it.itemId == target.item.id }?.id
        }
        repository.processEquipmentScan(
            unitId = unit?.id,
            itemId = target.item.id,
            action = scanMode,
            order = if (scanMode == "check_out") selectedOrder else null,
            note = conditionNote,
            adjustStock = selectedOrder == null,
        ).onSuccess { updated ->
            selectedUnitId = updated.id
            selectedItemForGenId = null
            conditionNote = ""
            val verb = when (scanMode) {
                "check_out" -> "Checked out"
                "check_in" -> "Checked in"
                else -> "Updated"
            }
            onToast("$verb ${updated.unitCode} (${updated.itemName})")
        }.onFailure {
            onToast(it.message ?: "Could not process scan")
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Column: Scanner Input + Units / Catalog / Logs
        Column(modifier = Modifier.weight(0.54f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("QR Equipment Check-In / Check-Out", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "$checkedInCount Checked In · $checkedOutCount Checked Out · $maintenanceCount Maintenance",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = leftTab == 0, onClick = { leftTab = 0 }, label = { Text("Unit Tags") })
                            FilterChip(selected = leftTab == 1, onClick = { leftTab = 1 }, label = { Text("Generate") })
                            FilterChip(selected = leftTab == 2, onClick = { leftTab = 2 }, label = { Text("Scan Log (${snapshot.scanLogs.size})") })
                        }
                    }

                    // Instant USB Scanner / Code Input + Image Scan
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = scanInput,
                            onValueChange = { scanInput = it },
                            placeholder = { Text("Scan USB QR/barcode or type unit code (e.g. TENT-01#U01)...") },
                            leadingIcon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = LoyGreen) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (scanInput.isNotBlank()) handleQrScanLookup(scanInput)
                            }),
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { handleQrScanLookup(scanInput) }, enabled = scanInput.isNotBlank()) {
                            Text("Process Scan")
                        }
                        OutlinedButton(onClick = {
                            val chooser = JFileChooser().apply {
                                fileFilter = FileNameExtensionFilter("PNG / JPG image (*.png, *.jpg)", "png", "jpg", "jpeg")
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val decoded = DesktopQrCodeUtils.decodeQrFromFile(chooser.selectedFile)
                                if (decoded != null) {
                                    scanInput = decoded
                                    handleQrScanLookup(decoded)
                                } else {
                                    onToast("No QR code found in ${chooser.selectedFile.name}")
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.ImageSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Scan Image")
                        }
                    }

                    if (leftTab == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                placeholder = { Text("Filter units by code, gear, or client...") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            listOf(
                                "all" to "All",
                                "checked_in" to "In",
                                "checked_out" to "Out",
                                "maintenance" to "Service",
                            ).forEach { (key, lbl) ->
                                FilterChip(
                                    selected = statusFilter == key,
                                    onClick = { statusFilter = key },
                                    label = { Text(lbl) },
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            when (leftTab) {
                0 -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredUnits, key = { it.id }) { u ->
                        val selected = selectedUnit?.id == u.id && selectedItemForGenId == null
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) LoyGreenLight.copy(alpha = 0.55f) else LoySurface,
                            border = BorderStroke(1.dp, if (selected) LoyGreen else LoyBorder),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                selectedUnitId = u.id
                                selectedItemForGenId = null
                            },
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = if (u.isCheckedOut) LoyInfo else if (u.isMaintenance) LoyWarning else LoyGreenDark)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${u.unitCode} · ${u.itemName}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sub = buildString {
                                        if (u.currentCustomerName.isNotEmpty()) append("Client: ${u.currentCustomerName}")
                                        if (u.currentEventName.isNotEmpty()) append(" (${u.currentEventName})")
                                        if (u.conditionNote.isNotEmpty()) {
                                            if (isNotEmpty()) append(" · ")
                                            append(u.conditionNote)
                                        }
                                        if (isEmpty() && u.lastScanAt > 0L) append("Last scan ${formatDateTime(u.lastScanAt)}")
                                    }
                                    if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                UnitStatusPill(u.status)
                            }
                        }
                    }
                }

                1 -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(snapshot.items, key = { it.id }) { equip ->
                        val unitCount = snapshot.equipmentUnits.count { it.itemId == equip.id }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedItemForGenId == equip.id) LoyGreenLight.copy(alpha = 0.55f) else LoySurface,
                            border = BorderStroke(1.dp, if (selectedItemForGenId == equip.id) LoyGreen else LoyBorder),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                selectedItemForGenId = equip.id
                                selectedUnitId = snapshot.equipmentUnits.firstOrNull { it.itemId == equip.id }?.id
                            },
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(equip.name, fontWeight = FontWeight.SemiBold)
                                    Text("SKU ${equip.sku} · ${formatQty(equip.stock)} avail · $unitCount QR units", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                OutlinedButton(onClick = {
                                    repository.addEquipmentUnit(equip.id)
                                        .onSuccess { created ->
                                            selectedUnitId = created.id
                                            selectedItemForGenId = null
                                            onToast("Generated unit QR tag ${created.unitCode}")
                                        }
                                        .onFailure { onToast(it.message ?: "Error") }
                                }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("New Unit Tag")
                                }
                            }
                        }
                    }
                }

                2 -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(snapshot.scanLogs, key = { it.id }) { log ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurface,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${log.unitCode} · ${log.itemName}", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${formatDateTime(log.createdAt)}${if (log.customerName.isNotEmpty()) " · ${log.customerName}" else ""}${if (log.note.isNotEmpty()) " · ${log.note}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                    )
                                }
                                UnitStatusPill(
                                    when (log.action) {
                                        "check_out" -> "checked_out"
                                        "maintenance" -> "maintenance"
                                        else -> "checked_in"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Selected Unit / QR Preview & Check-In / Check-Out Console
        Surface(
            color = LoySurface,
            border = BorderStroke(1.dp, LoyBorder),
            modifier = Modifier.weight(0.46f).fillMaxHeight(),
        ) {
            val unit = selectedUnit
            val item = selectedItemForGen ?: unit?.let { u -> snapshot.items.firstOrNull { it.id == u.itemId } }
            if (unit == null && item == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a unit tag or equipment item to generate QR codes & scan status.", color = LoyTextSecondary)
                }
            } else {
                val payload = unit?.qrPayload ?: DesktopQrCodeUtils.buildEquipmentPayload(item!!.id, item.sku.ifEmpty { item.id })
                val labelCode = unit?.unitCode ?: (item!!.sku.ifEmpty { item.id.take(8) })
                val title = unit?.itemName ?: item!!.name
                val qrImg = remember(payload) { DesktopQrCodeUtils.encodeQrImageBitmap(payload, 360) }

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(labelCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleSmall, color = LoyGreenDark)
                        }
                        if (unit != null) {
                            UnitStatusPill(unit.status)
                        }
                    }

                    if (qrImg != null) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, LoyBorder)) {
                            Image(bitmap = qrImg, contentDescription = "QR code $labelCode", modifier = Modifier.padding(12.dp).size(190.dp))
                        }
                    }
                    Text(payload, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)

                    OutlinedButton(
                        onClick = {
                            val chooser = JFileChooser().apply {
                                val safe = labelCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
                                selectedFile = File("qr-$safe.png")
                                fileFilter = FileNameExtensionFilter("PNG image (*.png)", "png")
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val sub = item?.let { "${formatCents(it.priceCents, snapshot.settings.currencySymbol)}/day · SKU ${it.sku}" } ?: title
                                DesktopQrCodeUtils.saveStickerPng(
                                    targetFile = chooser.selectedFile,
                                    storeName = snapshot.settings.storeName,
                                    itemName = title,
                                    codeLabel = labelCode,
                                    subtitle = sub,
                                    qrPayload = payload,
                                )
                                onToast("Saved QR label PNG to ${chooser.selectedFile.name}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Printable QR Sticker (.png)")
                    }

                    HorizontalDivider(color = LoyBorder)

                    Text("Individual Unit Check-In / Check-Out", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple("check_out", "Check Out", Icons.Outlined.Output),
                            Triple("check_in", "Check In", Icons.AutoMirrored.Outlined.AssignmentReturn),
                            Triple("maintenance", "Service", Icons.Outlined.Build),
                        ).forEach { (mode, lbl, ic) ->
                            FilterChip(
                                selected = scanMode == mode,
                                onClick = { scanMode = mode },
                                leadingIcon = { Icon(ic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(lbl) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (mode == "check_out") LoyInfoLight else LoyGreenLight,
                                    selectedLabelColor = if (mode == "check_out") LoyInfo else LoyGreenDark,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (scanMode == "check_out" && activeOrders.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = orderExpanded, onExpandedChange = { orderExpanded = !orderExpanded }) {
                            val lbl = selectedOrder?.let { "#${it.number.toString().padStart(4, '0')} · ${it.customerName ?: "Client"} (${it.eventName})" }
                                ?: "Standalone check-out (no linked order)"
                            OutlinedTextField(
                                value = lbl,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Link to Active Order (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = orderExpanded, onDismissRequest = { orderExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Standalone check-out (adjusts stock)") },
                                    onClick = { selectedOrderId = null; orderExpanded = false },
                                )
                                activeOrders.forEach { o ->
                                    DropdownMenuItem(
                                        text = { Text("#${o.number.toString().padStart(4, '0')} · ${o.customerName ?: "Walk-in"} · ${o.eventName}") },
                                        onClick = { selectedOrderId = o.id; orderExpanded = false },
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = conditionNote,
                        onValueChange = { conditionNote = it },
                        label = { Text("Condition / inspection note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val actionBtnColor = when (scanMode) {
                        "check_out" -> LoyInfo
                        "maintenance" -> LoyWarning
                        else -> LoyGreen
                    }
                    Button(
                        onClick = {
                            if (item != null) {
                                repository.processEquipmentScan(
                                    unitId = unit?.id,
                                    itemId = item.id,
                                    action = scanMode,
                                    order = if (scanMode == "check_out") selectedOrder else null,
                                    note = conditionNote,
                                    adjustStock = selectedOrder == null,
                                ).onSuccess { updated ->
                                    selectedUnitId = updated.id
                                    conditionNote = ""
                                    onToast("Updated ${updated.unitCode} -> ${updated.status}")
                                }.onFailure { onToast(it.message ?: "Scan update failed") }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = actionBtnColor),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                    ) {
                        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (scanMode) {
                                "check_out" -> "Apply Check-Out"
                                "check_in" -> "Apply Check-In"
                                else -> "Mark Maintenance"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (unit != null && item != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                repository.addEquipmentUnit(item.id)
                                    .onSuccess { created ->
                                        selectedUnitId = created.id
                                        onToast("Added tag ${created.unitCode}")
                                    }
                            }) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Another Unit Tag")
                            }
                            TextButton(onClick = {
                                repository.deleteEquipmentUnit(unit.id)
                                selectedUnitId = null
                                onToast("Removed ${unit.unitCode}")
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = LoyError, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Tag", color = LoyError)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitStatusPill(status: String) {
    val (label, bg, fg) = when (status) {
        "checked_out" -> Triple("Out", LoyInfoLight, LoyInfo)
        "maintenance" -> Triple("Service", LoyWarningLight, LoyWarning)
        else -> Triple("In", LoyGreenLight, LoyGreenDark)
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
