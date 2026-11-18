package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.DesktopQrCodeUtils
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.data.DesktopRepository
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
    var leftTab by remember { mutableIntStateOf(0) } // 0=Units, 1=Generate, 2=Scan Log
    var scanInput by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var scanMode by remember { mutableStateOf("check_out") }
    var selectedUnitId by remember { mutableStateOf<String?>(snapshot.equipmentUnits.firstOrNull()?.id) }
    var selectedItemForGenId by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var orderExpanded by remember { mutableStateOf(false) }
    var conditionNote by remember { mutableStateOf("") }

    val activeOrders = remember(snapshot.orders) { snapshot.orders.filter { it.isRentalOut } }
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
        // Left Column (56%)
        Column(modifier = Modifier.weight(0.56f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$checkedInCount Checked In · $checkedOutCount Checked Out · $maintenanceCount Maintenance",
                            style = MaterialTheme.typography.labelLarge,
                            color = LoyTextSecondary,
                        )
                        DesktopSegmentedControl(
                            options = listOf(
                                0 to "Unit Tags (${snapshot.equipmentUnits.size})",
                                1 to "Generate",
                                2 to "Scan Log (${snapshot.scanLogs.size})",
                            ),
                            selected = leftTab,
                            onSelect = { leftTab = it },
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DesktopSearchInput(
                            value = scanInput,
                            onValueChange = { scanInput = it },
                            placeholder = "Scan USB QR/barcode or type unit code (e.g. TENT-01#U01)...",
                            leadingIcon = Icons.Outlined.QrCodeScanner,
                            onSubmit = { if (scanInput.isNotBlank()) handleQrScanLookup(scanInput) },
                            modifier = Modifier.weight(1f),
                        )
                        DesktopPrimaryButton(
                            text = "Process Scan",
                            enabled = scanInput.isNotBlank(),
                            onClick = { handleQrScanLookup(scanInput) },
                        )
                        DesktopSecondaryButton(
                            text = "Scan Image",
                            icon = Icons.Outlined.ImageSearch,
                            onClick = {
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
                            },
                        )
                    }

                    if (leftTab == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            DesktopSearchInput(
                                value = search,
                                onValueChange = { search = it },
                                placeholder = "Filter units by code, gear, or client...",
                                modifier = Modifier.weight(1f),
                            )
                            DesktopSegmentedControl(
                                options = listOf(
                                    "all" to "All",
                                    "checked_in" to "In",
                                    "checked_out" to "Out",
                                    "maintenance" to "Service",
                                ),
                                selected = statusFilter,
                                onSelect = { statusFilter = it },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            when (leftTab) {
                0 -> {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("Unit Tag", Modifier.width(120.dp))
                        DesktopTableHeaderText("Equipment", Modifier.weight(1.3f))
                        DesktopTableHeaderText("Assignment / Note", Modifier.weight(1.1f))
                        DesktopTableHeaderText("Status", Modifier.width(80.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(filteredUnits, key = { it.id }) { u ->
                            val selected = selectedUnit?.id == u.id && selectedItemForGenId == null
                            DesktopTableRow(
                                selected = selected,
                                onClick = {
                                    selectedUnitId = u.id
                                    selectedItemForGenId = null
                                },
                            ) {
                                Text(
                                    text = u.unitCode,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = LoyGreenDark,
                                    modifier = Modifier.width(120.dp),
                                )
                                Text(
                                    text = u.itemName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.3f),
                                )
                                val sub = buildString {
                                    if (u.currentCustomerName.isNotEmpty()) append(u.currentCustomerName)
                                    if (u.currentEventName.isNotEmpty()) append(" (${u.currentEventName})")
                                    if (u.conditionNote.isNotEmpty()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(u.conditionNote)
                                    }
                                    if (isEmpty() && u.lastScanAt > 0L) append(formatDateTime(u.lastScanAt))
                                }.ifBlank { "Depot shelf" }
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.1f),
                                )
                                Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.CenterEnd) {
                                    UnitStatusPill(u.status)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("SKU", Modifier.width(84.dp))
                        DesktopTableHeaderText("Equipment", Modifier.weight(1.5f))
                        DesktopTableHeaderText("Tags", Modifier.width(84.dp))
                        DesktopTableHeaderText("Action", Modifier.width(120.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(snapshot.items, key = { it.id }) { equip ->
                            val unitCount = snapshot.equipmentUnits.count { it.itemId == equip.id }
                            DesktopTableRow(
                                selected = selectedItemForGenId == equip.id,
                                onClick = {
                                    selectedItemForGenId = equip.id
                                    selectedUnitId = snapshot.equipmentUnits.firstOrNull { it.itemId == equip.id }?.id
                                },
                            ) {
                                Text(
                                    text = equip.sku.ifBlank { equip.id },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = LoyTextMuted,
                                    modifier = Modifier.width(84.dp),
                                )
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(equip.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${formatQty(equip.stock)} avail in depot", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                }
                                Text(
                                    text = "$unitCount tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LoyTextSecondary,
                                    modifier = Modifier.width(84.dp),
                                )
                                DesktopSecondaryButton(
                                    text = "New Tag",
                                    icon = Icons.Outlined.Add,
                                    onClick = {
                                        repository.addEquipmentUnit(equip.id)
                                            .onSuccess { created ->
                                                selectedUnitId = created.id
                                                selectedItemForGenId = null
                                                onToast("Generated unit QR tag ${created.unitCode}")
                                            }
                                            .onFailure { onToast(it.message ?: "Error") }
                                    },
                                    modifier = Modifier.width(120.dp),
                                )
                            }
                        }
                    }
                }

                2 -> {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("Unit", Modifier.width(110.dp))
                        DesktopTableHeaderText("Equipment & Notes", Modifier.weight(1.4f))
                        DesktopTableHeaderText("Timestamp", Modifier.weight(0.9f))
                        DesktopTableHeaderText("Action", Modifier.width(78.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(snapshot.scanLogs, key = { it.id }) { log ->
                            DesktopTableRow {
                                Text(
                                    text = log.unitCode,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = LoyGreenDark,
                                    modifier = Modifier.width(110.dp),
                                )
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(log.itemName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val meta = listOfNotNull(log.customerName.takeIf { it.isNotBlank() }, log.note.takeIf { it.isNotBlank() }).joinToString(" · ")
                                    if (meta.isNotEmpty()) {
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = LoyTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text(
                                    text = formatDateTime(log.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                    modifier = Modifier.weight(0.9f),
                                )
                                Box(modifier = Modifier.width(78.dp), contentAlignment = Alignment.CenterEnd) {
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
        }

        VerticalDivider(color = LoyBorder)

        // Right Column (44%): QR Preview & Check-In / Check-Out Console
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight()
                .background(LoySurface),
        ) {
            val unit = selectedUnit
            val item = selectedItemForGen ?: unit?.let { u -> snapshot.items.firstOrNull { it.id == u.itemId } }
            if (unit == null && item == null) {
                DesktopEmptyState(
                    icon = Icons.Outlined.QrCodeScanner,
                    title = "No QR Unit Selected",
                    subtitle = "Select a unit tag or catalog item on the left to inspect or print labels.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val payload = unit?.qrPayload ?: DesktopQrCodeUtils.buildEquipmentPayload(item!!.id, item.sku.ifEmpty { item.id })
                val labelCode = unit?.unitCode ?: (item!!.sku.ifEmpty { item.id.take(8) })
                val title = unit?.itemName ?: item!!.name
                val qrImg = remember(payload) { DesktopQrCodeUtils.encodeQrImageBitmap(payload, 360) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(labelCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge, color = LoyGreenDark)
                        }
                        if (unit != null) {
                            UnitStatusPill(unit.status)
                        }
                    }

                    if (qrImg != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, LoyBorder),
                        ) {
                            Image(
                                bitmap = qrImg,
                                contentDescription = "QR code $labelCode",
                                modifier = Modifier.padding(12.dp).size(176.dp),
                            )
                        }
                    }
                    Text(payload, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)

                    DesktopSecondaryButton(
                        text = "Save Printable QR Sticker (.png)",
                        icon = Icons.Outlined.Download,
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
                    )

                    DesktopSectionCard(title = "Individual Unit Dispatch Console") {
                        DesktopSegmentedControl(
                            options = listOf(
                                "check_out" to "Check Out",
                                "check_in" to "Check In",
                                "maintenance" to "Service",
                            ),
                            selected = scanMode,
                            onSelect = { scanMode = it },
                        )

                        if (scanMode == "check_out" && activeOrders.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Link to Active Order (Optional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                                ExposedDropdownMenuBox(expanded = orderExpanded, onExpandedChange = { orderExpanded = !orderExpanded }) {
                                    val lbl = selectedOrder?.let { "#${it.number.toString().padStart(4, '0')} · ${it.customerName ?: "Client"} (${it.eventName})" }
                                        ?: "Standalone check-out (adjusts stock)"
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
                                            Text(lbl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded)
                                        }
                                    }
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
                        }

                        DesktopFormInput(
                            label = "Condition / Inspection Note",
                            value = conditionNote,
                            onValueChange = { conditionNote = it },
                            placeholder = "Clean frame, stakes verified...",
                        )

                        val actionBtnColor = when (scanMode) {
                            "check_out" -> LoyInfo
                            "maintenance" -> LoyWarning
                            else -> LoyGreen
                        }
                        DesktopPrimaryButton(
                            text = when (scanMode) {
                                "check_out" -> "Apply Check-Out"
                                "check_in" -> "Apply Check-In"
                                else -> "Mark Maintenance"
                            },
                            icon = Icons.Outlined.CheckCircleOutline,
                            containerColor = actionBtnColor,
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (unit != null && item != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesktopSecondaryButton(
                                text = "Add Another Unit Tag",
                                icon = Icons.Outlined.Add,
                                onClick = {
                                    repository.addEquipmentUnit(item.id)
                                        .onSuccess { created ->
                                            selectedUnitId = created.id
                                            onToast("Added tag ${created.unitCode}")
                                        }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            DesktopSecondaryButton(
                                text = "Delete Tag",
                                icon = Icons.Outlined.DeleteOutline,
                                contentColor = LoyError,
                                onClick = {
                                    repository.deleteEquipmentUnit(unit.id)
                                    selectedUnitId = null
                                    onToast("Removed ${unit.unitCode}")
                                },
                            )
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
    DesktopBadge(text = label, bgColor = bg, fgColor = fg)
}
