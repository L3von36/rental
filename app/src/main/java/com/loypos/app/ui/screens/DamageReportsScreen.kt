package com.loypos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.data.local.DamageReportEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
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
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DamageReportsScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    initialReceiptId: String? = null,
    initialItemId: String? = null,
    initialUnitCode: String? = null,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val sym = data.settings.currencySymbol

    var searchQuery by remember { mutableStateOf("") }
    var statusFilterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Open, 2=Repairing, 3=Resolved
    var editingReport by remember { mutableStateOf<DamageReportEntity?>(null) }
    var showFormDialog by remember(initialReceiptId, initialItemId, initialUnitCode) {
        mutableStateOf(initialReceiptId != null || initialItemId != null || initialUnitCode != null)
    }
    var deleteCandidate by remember { mutableStateOf<DamageReportEntity?>(null) }
    var zoomPhotoPath by remember { mutableStateOf<String?>(null) }

    val openCount = remember(data.damageReports) {
        data.damageReports.count { it.status == DamageReportEntity.STATUS_OPEN }
    }
    val repairingCount = remember(data.damageReports) {
        data.damageReports.count { it.status == DamageReportEntity.STATUS_REPAIRING }
    }
    val resolvedCount = remember(data.damageReports) {
        data.damageReports.count { it.status == DamageReportEntity.STATUS_RESOLVED }
    }
    val totalRepairCents = remember(data.damageReports) {
        data.damageReports.sumOf { it.repairCostCents }
    }

    val filteredReports = remember(data.damageReports, searchQuery, statusFilterIdx) {
        val q = searchQuery.trim().lowercase()
        data.damageReports.filter { report ->
            val matchesStatus = when (statusFilterIdx) {
                1 -> report.status == DamageReportEntity.STATUS_OPEN
                2 -> report.status == DamageReportEntity.STATUS_REPAIRING
                3 -> report.status == DamageReportEntity.STATUS_RESOLVED
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                report.itemId.lowercase().contains(q) ||
                report.itemSku.lowercase().contains(q) ||
                report.itemName.lowercase().contains(q) ||
                report.unitCode.lowercase().contains(q) ||
                report.customerName.lowercase().contains(q) ||
                report.notes.lowercase().contains(q) ||
                (report.receiptNumber?.let { "#${it.toString().padStart(4, '0')}".contains(q) || it.toString().contains(q) } == true)
            matchesStatus && matchesQuery
        }
    }
    val windowSize = LocalLoyWindowSize.current
    var selectedReportId by remember { mutableStateOf<String?>(null) }
    val activeReport = remember(filteredReports, selectedReportId) {
        filteredReports.firstOrNull { it.id == selectedReportId } ?: filteredReports.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
                title = {
                    Column {
                        Text(
                            text = "Damage Report Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Inspection photos, notes & equipment IDs",
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
                    editingReport = null
                    showFormDialog = true
                },
                containerColor = LoyError,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_damage_report_button"),
            ) {
                Icon(Icons.Outlined.AddAPhoto, contentDescription = "Log Equipment Damage")
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
                Column(
                    modifier = Modifier.padding(horizontal = metrics.screenPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
                        StatBlock(
                            label = "Open",
                            value = openCount.toString(),
                            valueColor = if (openCount > 0) LoyError else LoyTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Repairing",
                            value = repairingCount.toString(),
                            valueColor = if (repairingCount > 0) LoyWarning else LoyTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Resolved",
                            value = resolvedCount.toString(),
                            valueColor = LoyGreen,
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Est. Cost",
                            value = formatCents(totalRepairCents, sym),
                            modifier = Modifier.weight(1.3f),
                        )
                    }
                    CompactSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search item ID, SKU, unit code, client, or notes",
                        inputTestTag = "damage_search_input",
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val tabs = listOf(
                            "All (${data.damageReports.size})",
                            "Open ($openCount)",
                            "Repairing ($repairingCount)",
                            "Resolved ($resolvedCount)",
                        )
                        tabs.forEachIndexed { idx, label ->
                            item {
                                FilterChip(
                                    selected = statusFilterIdx == idx,
                                    onClick = { statusFilterIdx = idx },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LoyErrorLight,
                                        selectedLabelColor = LoyError,
                                    ),
                                    modifier = Modifier.height(28.dp),
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (filteredReports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.ReportProblem,
                        title = "No damage reports logged",
                        subtitle = "Document returned equipment condition with inspection photos, item IDs, and notes.",
                        actionLabel = "Log Damage Report",
                        onAction = {
                            editingReport = null
                            showFormDialog = true
                        },
                    )
                }
            } else if (windowSize.useSplitPane) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.weight(0.44f).fillMaxHeight(),
                        contentPadding = PaddingValues(metrics.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            val isSelected = activeReport?.id == report.id
                            val (statusLabel, statusBg, statusFg) = when (report.status) {
                                DamageReportEntity.STATUS_REPAIRING -> Triple("Under Repair", LoyWarningLight, LoyWarning)
                                DamageReportEntity.STATUS_RESOLVED -> Triple("Resolved", LoyGreenLight, LoyGreenDarkText)
                                else -> Triple("Open", LoyErrorLight, LoyError)
                            }
                            val (sevLabel, sevBg, sevFg) = when (report.severity) {
                                DamageReportEntity.SEVERITY_SEVERE -> Triple("Severe", LoyErrorLight, LoyError)
                                DamageReportEntity.SEVERITY_MODERATE -> Triple("Moderate", LoyWarningLight, LoyWarning)
                                else -> Triple("Minor", LoyInfoLight, LoyInfo)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) LoyErrorLight.copy(alpha = 0.25f) else LoySurface,
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) LoyError else LoyBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedReportId = report.id }
                                    .testTag("damage_report_card_${report.id}"),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = report.itemName,
                                            fontWeight = FontWeight.Bold,
                                            color = LoyTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (report.repairCostCents > 0L) {
                                            Text(
                                                text = formatCents(report.repairCostCents, sym),
                                                style = TabularMoneyStyle,
                                                fontWeight = FontWeight.Bold,
                                                color = LoyError,
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PillBadge(text = statusLabel, backgroundColor = statusBg, textColor = statusFg)
                                        PillBadge(text = sevLabel, backgroundColor = sevBg, textColor = sevFg)
                                        if (report.unitCode.isNotBlank()) {
                                            PillBadge(text = report.unitCode, backgroundColor = LoyInfoLight, textColor = LoyInfo)
                                        }
                                    }
                                    Text(
                                        text = "${formatDateTime(report.createdAt)}${if (report.customerName.isNotBlank()) " · ${report.customerName}" else ""} · ${report.photoPathList().size} photos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                    Box(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(metrics.screenPadding),
                    ) {
                        val targetReport = activeReport
                        if (targetReport == null) {
                            EmptyState(
                                icon = Icons.Outlined.ReportProblem,
                                title = "Select a damage report",
                                subtitle = "Pick any damage entry on the left to review inspection notes, attached photos, and repair actions.",
                            )
                        } else {
                            DamageReportCard(
                                report = targetReport,
                                currencySymbol = sym,
                                onZoomPhoto = { zoomPhotoPath = it },
                                onUpdateStatus = { newStatus ->
                                    viewModel.updateDamageReportStatus(targetReport.id, newStatus)
                                },
                                onEdit = {
                                    editingReport = targetReport
                                    showFormDialog = true
                                },
                                onDelete = { deleteCandidate = targetReport },
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
                    items(filteredReports, key = { it.id }) { report ->
                        DamageReportCard(
                            report = report,
                            currencySymbol = sym,
                            onZoomPhoto = { zoomPhotoPath = it },
                            onUpdateStatus = { newStatus ->
                                viewModel.updateDamageReportStatus(report.id, newStatus)
                            },
                            onEdit = {
                                editingReport = report
                                showFormDialog = true
                            },
                            onDelete = { deleteCandidate = report },
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        DamageReportEditorDialog(
            viewModel = viewModel,
            data = data,
            existing = editingReport,
            initialReceiptId = initialReceiptId,
            initialItemId = initialItemId,
            initialUnitCode = initialUnitCode,
            onDismiss = {
                showFormDialog = false
                editingReport = null
            },
        )
    }

    deleteCandidate?.let { target ->
        ConfirmDialog(
            title = "Delete damage report for ${target.itemName}?",
            message = "This removes the damage log entry and any attached inspection photos from device storage.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteDamageReport(target)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null },
        )
    }

    zoomPhotoPath?.let { path ->
        AlertDialog(
            onDismissRequest = { zoomPhotoPath = null },
            title = { Text("Inspection Photo") },
            text = {
                AsyncImage(
                    model = File(path),
                    contentDescription = "Zoomed equipment damage photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            },
            confirmButton = {
                TextButton(onClick = { zoomPhotoPath = null }) {
                    Text("Close")
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DamageReportCard(
    report: DamageReportEntity,
    currencySymbol: String,
    onZoomPhoto: (String) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val photos = remember(report.photoPaths) { report.photoPathList() }

    val (statusLabel, statusBg, statusFg) = when (report.status) {
        DamageReportEntity.STATUS_REPAIRING -> Triple("Under Repair", LoyWarningLight, LoyWarning)
        DamageReportEntity.STATUS_RESOLVED -> Triple("Resolved", LoyGreenLight, LoyGreenDarkText)
        else -> Triple("Open", LoyErrorLight, LoyError)
    }
    val (sevLabel, sevBg, sevFg) = when (report.severity) {
        DamageReportEntity.SEVERITY_SEVERE -> Triple("Severe", LoyErrorLight, LoyError)
        DamageReportEntity.SEVERITY_MODERATE -> Triple("Moderate", LoyWarningLight, LoyWarning)
        else -> Triple("Minor", LoyInfoLight, LoyInfo)
    }

    GroupCard(modifier = Modifier.testTag("damage_report_card_${report.id}")) {
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
                    Text(
                        text = report.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LoyTextPrimary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PillBadge(text = statusLabel, backgroundColor = statusBg, textColor = statusFg)
                        PillBadge(text = sevLabel, backgroundColor = sevBg, textColor = sevFg)
                        if (report.itemId.isNotBlank()) {
                            PillBadge(
                                text = "ID: ${report.itemId}",
                                backgroundColor = LoySurfaceVariant,
                                textColor = LoyTextPrimary,
                            )
                        }
                        if (report.unitCode.isNotBlank()) {
                            PillBadge(
                                text = "Unit: ${report.unitCode}",
                                backgroundColor = LoyInfoLight,
                                textColor = LoyInfo,
                            )
                        }
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("edit_damage_${report.id}"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit report", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("delete_damage_${report.id}"),
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete report", tint = LoyError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (report.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LoySurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = report.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoyTextPrimary,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            val metaParts = buildList {
                report.receiptNumber?.let { add("Order #${it.toString().padStart(4, '0')}") }
                if (report.customerName.isNotBlank()) add("Client: ${report.customerName}")
                if (report.itemSku.isNotBlank()) add("SKU: ${report.itemSku}")
                if (report.reportedBy.isNotBlank()) add("By: ${report.reportedBy}")
                add(formatDateTime(report.createdAt))
            }
            Text(
                text = metaParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = LoyTextSecondary,
            )

            if (photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos) { path ->
                        val file = File(path)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Equipment damage photo for ${report.itemName}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onZoomPhoto(path) },
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LoyBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (report.repairCostCents > 0L) {
                        "Est. Repair: ${formatCents(report.repairCostCents, currencySymbol)}"
                    } else {
                        "No repair charge"
                    },
                    style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                    fontWeight = FontWeight.SemiBold,
                    color = if (report.repairCostCents > 0L) LoyError else LoyTextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.status == DamageReportEntity.STATUS_OPEN) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(DamageReportEntity.STATUS_REPAIRING) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp),
                        ) {
                            Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start Repair", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (report.status != DamageReportEntity.STATUS_RESOLVED) {
                        Button(
                            onClick = { onUpdateStatus(DamageReportEntity.STATUS_RESOLVED) },
                            colors = ButtonDefaults.buttonColors(containerColor = LoyGreen, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp),
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Resolve", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onUpdateStatus(DamageReportEntity.STATUS_OPEN) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp),
                        ) {
                            Text("Reopen", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DamageReportEditorDialog(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    existing: DamageReportEntity?,
    initialReceiptId: String?,
    initialItemId: String?,
    initialUnitCode: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val preReceipt = remember(existing, initialReceiptId, data.receipts) {
        val targetId = existing?.receiptId ?: initialReceiptId
        data.receipts.firstOrNull { it.receipt.id == targetId }
    }
    val initialProduct = remember(existing, initialItemId, preReceipt, data.items) {
        val id = existing?.itemId
            ?: initialItemId
            ?: preReceipt?.lines?.firstOrNull()?.itemId
            ?: data.items.firstOrNull()?.id
        data.items.firstOrNull { it.id == id }
    }

    var selectedReceiptId by remember(existing, preReceipt) {
        mutableStateOf(existing?.receiptId ?: preReceipt?.receipt?.id)
    }
    var receiptNumber by remember(existing, preReceipt) {
        mutableStateOf(existing?.receiptNumber ?: preReceipt?.receipt?.number)
    }
    var customerName by remember(existing, preReceipt) {
        mutableStateOf(existing?.customerName ?: preReceipt?.receipt?.customerName ?: "")
    }
    var selectedItemId by remember(existing, initialProduct) {
        mutableStateOf(existing?.itemId ?: initialProduct?.id ?: "")
    }
    var itemName by remember(existing, initialProduct) {
        mutableStateOf(existing?.itemName ?: initialProduct?.name ?: "")
    }
    var itemSku by remember(existing, initialProduct) {
        mutableStateOf(existing?.itemSku ?: initialProduct?.sku ?: "")
    }
    var unitCode by remember(existing, initialUnitCode) {
        mutableStateOf(existing?.unitCode ?: initialUnitCode ?: "")
    }
    var severity by remember(existing) {
        mutableStateOf(existing?.severity ?: DamageReportEntity.SEVERITY_MINOR)
    }
    var status by remember(existing) {
        mutableStateOf(existing?.status ?: DamageReportEntity.STATUS_OPEN)
    }
    var repairCostText by remember(existing) {
        mutableStateOf(
            existing?.repairCostCents?.takeIf { it > 0L }?.let { (it / 100.0).toString() } ?: ""
        )
    }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var markUnitMaintenance by remember { mutableStateOf(existing == null) }

    val photos = remember(existing) {
        mutableStateListOf<String>().apply {
            existing?.photoPathList()?.let { addAll(it) }
        }
    }

    var itemMenuExpanded by remember { mutableStateOf(false) }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        val rawFile = pendingCameraFile
        if (success && uri != null) {
            scope.launch {
                viewModel.saveDamagePhotoFromUri(uri)
                    .onSuccess { savedPath -> photos.add(savedPath) }
                    .onFailure { viewModel.showMessage("Could not save captured photo: ${it.message}") }
                rawFile?.delete()
            }
        } else {
            rawFile?.delete()
        }
    }

    val launchCameraCapture: () -> Unit = {
        runCatching {
            val tmpFile = File.createTempFile("damage_capture_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
            pendingCameraFile = tmpFile
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        }.onFailure {
            viewModel.showMessage("Could not start camera: ${it.message}")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            viewModel.showMessage("Camera permission denied — you can attach a photo from gallery")
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.saveDamagePhotoFromUri(uri)
                    .onSuccess { savedPath -> photos.add(savedPath) }
                    .onFailure { viewModel.showMessage("Could not save photo: ${it.message}") }
            }
        }
    }

    val availableUnitsForItem = remember(data.equipmentUnits, selectedItemId) {
        data.equipmentUnits.filter { it.itemId == selectedItemId }
    }
    val selectedOrderLines = remember(selectedReceiptId, data.receipts) {
        data.receipts.firstOrNull { it.receipt.id == selectedReceiptId }?.lines.orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Document Equipment Damage" else "Edit Damage Report",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Linked Rental Order dropdown
                val orderLabel = selectedReceiptId?.let { id ->
                    val rec = data.receipts.firstOrNull { it.receipt.id == id }?.receipt
                    if (rec != null) "#${rec.number.toString().padStart(4, '0')} · ${rec.customerName ?: "Walk-in"}" else "None"
                } ?: "None (Stand-alone inspection)"

                ExposedDropdownMenuBox(
                    expanded = orderMenuExpanded,
                    onExpandedChange = { orderMenuExpanded = !orderMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = orderLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Linked Rental Order") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = orderMenuExpanded,
                        onDismissRequest = { orderMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Stand-alone inspection)") },
                            onClick = {
                                selectedReceiptId = null
                                receiptNumber = null
                                orderMenuExpanded = false
                            },
                        )
                        data.receipts.take(25).forEach { entry ->
                            val r = entry.receipt
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Walk-in"} (${entry.lines.size} tools)",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    selectedReceiptId = r.id
                                    receiptNumber = r.number
                                    if (!r.customerName.isNullOrBlank()) customerName = r.customerName
                                    entry.lines.firstOrNull()?.let { firstLine ->
                                        selectedItemId = firstLine.itemId
                                        val match = data.items.firstOrNull { it.id == firstLine.itemId }
                                        itemName = match?.name ?: firstLine.name
                                        itemSku = match?.sku ?: ""
                                    }
                                    orderMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                if (selectedOrderLines.isNotEmpty()) {
                    Text(
                        text = "Quick pick returned tool from order:",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedOrderLines, key = { it.id }) { line ->
                            FilterChip(
                                selected = selectedItemId == line.itemId,
                                onClick = {
                                    selectedItemId = line.itemId
                                    val match = data.items.firstOrNull { it.id == line.itemId }
                                    itemName = match?.name ?: line.name
                                    itemSku = match?.sku ?: ""
                                },
                                label = { Text(line.name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                // Catalog Equipment Picker
                ExposedDropdownMenuBox(
                    expanded = itemMenuExpanded,
                    onExpandedChange = { itemMenuExpanded = !itemMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = itemName.ifBlank { "Select Equipment" },
                        onValueChange = { itemName = it },
                        label = { Text("Equipment Name *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false },
                    ) {
                        data.items.filter { !it.archived }.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Text("${item.name} (${item.id}${if (item.sku.isNotBlank()) " · ${item.sku}" else ""})")
                                },
                                onClick = {
                                    selectedItemId = item.id
                                    itemName = item.name
                                    itemSku = item.sku
                                    itemMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedItemId,
                        onValueChange = { selectedItemId = it },
                        label = { Text("Equipment ID *") },
                        placeholder = { Text("eq-008") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("damage_item_id_input"),
                    )
                    OutlinedTextField(
                        value = itemSku,
                        onValueChange = { itemSku = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = unitCode,
                    onValueChange = { unitCode = it },
                    label = { Text("QR Unit Tag / Serial (optional)") },
                    placeholder = { Text("e.g. CHAIR-001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (availableUnitsForItem.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableUnitsForItem.take(8), key = { it.id }) { unit ->
                            FilterChip(
                                selected = unitCode == unit.unitCode,
                                onClick = { unitCode = if (unitCode == unit.unitCode) "" else unit.unitCode },
                                label = { Text(unit.unitCode, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                Text("Severity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        DamageReportEntity.SEVERITY_MINOR to "Minor",
                        DamageReportEntity.SEVERITY_MODERATE to "Moderate",
                        DamageReportEntity.SEVERITY_SEVERE to "Severe",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = severity == value,
                            onClick = { severity = value },
                            label = { Text(label) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Client Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                    )
                    OutlinedTextField(
                        value = repairCostText,
                        onValueChange = { repairCostText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Repair (${data.settings.currencySymbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Inspection Notes *") },
                    placeholder = { Text("Describe tear, bent frame, missing clamp, moisture damage...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("damage_notes_input"),
                )

                if (unitCode.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Flag unit ${unitCode} under Maintenance",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = markUnitMaintenance,
                            onCheckedChange = { markUnitMaintenance = it },
                        )
                    }
                }

                HorizontalDivider(color = LoyBorder)
                Text("Damage Photos (${photos.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val hasCamera = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCamera) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("damage_take_photo_button"),
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Camera", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("damage_pick_photo_button"),
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Gallery", maxLines = 1)
                    }
                }

                if (photos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos.toList()) { path ->
                            Box(modifier = Modifier.size(76.dp)) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Attached damage photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(22.dp)
                                        .clickable {
                                            photos.remove(path)
                                            if (existing == null || path !in existing.photoPathList()) {
                                                viewModel.deleteLocalPhotoQuietly(path)
                                            }
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
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
                    viewModel.saveDamageReport(
                        reportId = existing?.id,
                        itemId = selectedItemId.ifBlank { itemSku.ifBlank { itemName } },
                        itemSku = itemSku,
                        itemName = itemName.ifBlank { selectedItemId },
                        unitCode = unitCode,
                        receiptId = selectedReceiptId,
                        receiptNumber = receiptNumber,
                        customerName = customerName,
                        severity = severity,
                        status = status,
                        repairCostCents = parseAmountToCents(repairCostText) ?: 0L,
                        notes = notes,
                        photoPaths = photos.toList(),
                        markUnitMaintenance = markUnitMaintenance,
                    ) {
                        onDismiss()
                    }
                },
                enabled = (selectedItemId.isNotBlank() || itemName.isNotBlank()) && notes.isNotBlank(),
                modifier = Modifier.testTag("save_damage_report_button"),
            ) {
                Text("Save Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
