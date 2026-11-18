package com.loypos.desktop.ui

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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopMaintenanceSchedule
import com.loypos.desktop.model.DesktopStoreSnapshot

@Composable
fun DesktopMaintenanceView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") } // all | due | scheduled | completed
    var selectedId by remember { mutableStateOf<String?>(snapshot.maintenanceSchedules.firstOrNull()?.id) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<DesktopMaintenanceSchedule?>(null) }

    val now = remember(snapshot.maintenanceSchedules) { nowMs() }
    val catalogMap = remember(snapshot.items) { snapshot.items.associateBy { it.id } }

    val dueCount = remember(snapshot.maintenanceSchedules, now) {
        snapshot.maintenanceSchedules.count { it.isDue(now) }
    }
    val scheduledCount = remember(snapshot.maintenanceSchedules) {
        snapshot.maintenanceSchedules.count { it.status == "scheduled" }
    }
    val completedCount = remember(snapshot.maintenanceSchedules) {
        snapshot.maintenanceSchedules.count { it.status == "completed" }
    }

    val filteredSchedules = remember(snapshot.maintenanceSchedules, search, filterStatus, now) {
        val q = search.trim().lowercase()
        snapshot.maintenanceSchedules.filter { m ->
            val matchStatus = when (filterStatus) {
                "due" -> m.isDue(now)
                "scheduled" -> m.status == "scheduled"
                "completed" -> m.status == "completed"
                else -> true
            }
            val matchQuery = q.isEmpty() ||
                m.itemName.lowercase().contains(q) ||
                m.serviceTitle.lowercase().contains(q) ||
                m.technicianName.lowercase().contains(q) ||
                m.notes.lowercase().contains(q) ||
                m.itemId.lowercase().contains(q)
            matchStatus && matchQuery
        }
    }

    val activeSchedule = remember(filteredSchedules, selectedId) {
        filteredSchedules.firstOrNull { it.id == selectedId } ?: filteredSchedules.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopKpiTile("Due / Overdue", dueCount.toString(), if (dueCount > 0) LoyError else LoyGreen, Modifier.weight(1f))
                    DesktopKpiTile("Scheduled", scheduledCount.toString(), LoyInfo, Modifier.weight(1f))
                    DesktopKpiTile("Completed", completedCount.toString(), LoyGreen, Modifier.weight(1f))
                    DesktopKpiTile("Total Assets", snapshot.maintenanceSchedules.size.toString(), LoyTextPrimary, Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DesktopSearchInput(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search sound systems, light rigs, service title, or technician...",
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSegmentedControl(
                        options = listOf(
                            "all" to "All (${snapshot.maintenanceSchedules.size})",
                            "due" to "Due ($dueCount)",
                            "scheduled" to "Scheduled ($scheduledCount)",
                            "completed" to "Completed ($completedCount)",
                        ),
                        selected = filterStatus,
                        onSelect = { filterStatus = it },
                    )
                    DesktopPrimaryButton(
                        text = "Schedule Service",
                        icon = Icons.Outlined.Build,
                        containerColor = LoyGreen,
                        onClick = { showCreateDialog = true },
                    )
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Master Table (48%)
            Column(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
                if (filteredSchedules.isEmpty()) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.Build,
                        title = "No Maintenance Schedules",
                        subtitle = "Track routine service intervals for high-value equipment like sound systems and light rigs.",
                        action = {
                            DesktopPrimaryButton(
                                text = "Schedule Routine Service",
                                icon = Icons.Outlined.Build,
                                onClick = { showCreateDialog = true },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("Equipment / Service Title", Modifier.weight(1.5f))
                        DesktopTableHeaderText("Next Due", Modifier.width(96.dp))
                        DesktopTableHeaderText("Status", Modifier.width(96.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(filteredSchedules, key = { it.id }) { schedule ->
                            val isDue = schedule.isDue(now)
                            val statusColors = when {
                                isDue -> LoyErrorLight to LoyError
                                schedule.status == "completed" -> LoyGreenLight to LoyGreenDark
                                schedule.status == "in_service" -> LoyWarningLight to LoyWarning
                                else -> LoyInfoLight to LoyInfo
                            }
                            DesktopTableRow(
                                selected = activeSchedule?.id == schedule.id,
                                onClick = { selectedId = schedule.id },
                            ) {
                                Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = schedule.itemName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = schedule.serviceTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = formatShortDate(schedule.nextDueMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isDue) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDue) LoyError else LoyTextPrimary,
                                    modifier = Modifier.width(96.dp),
                                )
                                Box(modifier = Modifier.width(96.dp)) {
                                    DesktopBadge(
                                        text = if (isDue) "Due Now" else schedule.status.replaceFirstChar { it.uppercase() },
                                        bgColor = statusColors.first,
                                        fgColor = statusColors.second,
                                    )
                                }
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
                val schedule = activeSchedule
                if (schedule == null) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.Build,
                        title = "Select a Maintenance Task",
                        subtitle = "Choose any routine service schedule on the left to inspect technician notes, intervals, and status.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val isDue = schedule.isDue(now)
                    val statusColors = when {
                        isDue -> LoyErrorLight to LoyError
                        schedule.status == "completed" -> LoyGreenLight to LoyGreenDark
                        schedule.status == "in_service" -> LoyWarningLight to LoyWarning
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
                                    DesktopBadge(if (isDue) "Service Due" else schedule.status.replaceFirstChar { it.uppercase() }, statusColors.first, statusColors.second)
                                    DesktopBadge("${schedule.priority.replaceFirstChar { it.uppercase() }} Priority", LoySurfaceVariant, LoyTextSecondary)
                                }
                                Text(schedule.serviceTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = schedule.itemName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LoyGreenDark,
                                )
                            }
                        }

                        DesktopSectionCard(title = "Service Interval & Schedule Metadata") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("INTERVAL", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text("Every ${schedule.intervalDays} days / ${schedule.intervalRentals} rentals", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ASSIGNED TECHNICIAN", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(schedule.technicianName.ifBlank { "Depot Technician" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LAST SERVICED", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(formatShortDate(schedule.lastServicedMs), style = MaterialTheme.typography.bodySmall)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("NEXT DUE DATE", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        formatShortDate(schedule.nextDueMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDue) LoyError else LoyTextPrimary,
                                    )
                                }
                            }
                        }

                        DesktopSectionCard(title = "Service Instructions & Notes") {
                            Text(
                                text = schedule.notes.ifBlank { "No special service instructions provided." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (schedule.notes.isBlank()) LoyTextMuted else LoyTextPrimary,
                            )
                        }
                    }

                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(LoySurfaceHover).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopPrimaryButton(
                            text = "Mark Serviced & Reset",
                            icon = Icons.Outlined.CheckCircle,
                            onClick = {
                                repository.completeMaintenance(schedule.id)
                                onToast("Marked routine service completed for ${schedule.itemName}")
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        DesktopSecondaryButton(
                            text = "Edit",
                            icon = Icons.Outlined.Edit,
                            onClick = { editingSchedule = schedule },
                        )
                        DesktopSecondaryButton(
                            text = "Delete",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = {
                                repository.deleteMaintenanceSchedule(schedule.id)
                                selectedId = null
                                onToast("Deleted maintenance schedule")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingSchedule != null) {
        DesktopMaintenanceEditorDialog(
            initial = editingSchedule,
            snapshot = snapshot,
            onSave = { sched ->
                repository.saveMaintenanceSchedule(sched)
                selectedId = sched.id
                onToast("Saved maintenance schedule for ${sched.itemName}")
                showCreateDialog = false
                editingSchedule = null
            },
            onDismiss = {
                showCreateDialog = false
                editingSchedule = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopMaintenanceEditorDialog(
    initial: DesktopMaintenanceSchedule?,
    snapshot: DesktopStoreSnapshot,
    onSave: (DesktopMaintenanceSchedule) -> Unit,
    onDismiss: () -> Unit,
) {
    val itemsList = snapshot.items
    var selectedItemId by remember(initial) { mutableStateOf(initial?.itemId ?: itemsList.firstOrNull()?.id ?: "") }
    var serviceTitle by remember(initial) { mutableStateOf(initial?.serviceTitle ?: "Acoustic & Rigging Check") }
    var intervalDaysText by remember(initial) { mutableStateOf(initial?.intervalDays?.toString() ?: "30") }
    var intervalRentalsText by remember(initial) { mutableStateOf(initial?.intervalRentals?.toString() ?: "10") }
    var technicianName by remember(initial) { mutableStateOf(initial?.technicianName ?: "Depot Technician") }
    var priority by remember(initial) { mutableStateOf(initial?.priority ?: "routine") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var itemMenuExpanded by remember { mutableStateOf(false) }

    val selectedItem = remember(snapshot.items, selectedItemId) {
        snapshot.items.firstOrNull { it.id == selectedItemId } ?: snapshot.items.firstOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule Routine Maintenance" else "Edit Maintenance Schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.width(520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Equipment Asset *", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                    ExposedDropdownMenuBox(
                        expanded = itemMenuExpanded,
                        onExpandedChange = { itemMenuExpanded = !itemMenuExpanded },
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
                                Text(selectedItem?.name ?: "Select equipment...", style = MaterialTheme.typography.bodySmall)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuExpanded)
                            }
                        }
                        ExposedDropdownMenu(expanded = itemMenuExpanded, onDismissRequest = { itemMenuExpanded = false }) {
                            snapshot.items.forEach { eq ->
                                DropdownMenuItem(
                                    text = { Text("${eq.name} (${eq.sku.ifBlank { eq.id }})") },
                                    onClick = {
                                        selectedItemId = eq.id
                                        itemMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                DesktopFormInput("Service Title * (e.g. Acoustic Calibration)", serviceTitle, { serviceTitle = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Interval Days", intervalDaysText, { intervalDaysText = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Interval Rentals", intervalRentalsText, { intervalRentalsText = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Assigned Technician", technicianName, { technicianName = it }, modifier = Modifier.weight(1.2f))
                    Column(modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                        DesktopSegmentedControl(
                            options = listOf("routine" to "Routine", "high" to "High", "urgent" to "Urgent"),
                            selected = priority,
                            onSelect = { priority = it },
                        )
                    }
                }
                DesktopFormInput(
                    label = "Service Instructions / Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false,
                    minLines = 3,
                    placeholder = "Inspect voice coils, check rigging hardware, grease moving parts...",
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val eq = selectedItem
                    if (eq != null && serviceTitle.trim().isNotEmpty()) {
                        val now = nowMs()
                        val days = intervalDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                        val rentals = intervalRentalsText.toIntOrNull()?.coerceAtLeast(1) ?: 10
                        val nextDue = initial?.nextDueMs ?: (now + days * 86_400_000L)
                        onSave(
                            DesktopMaintenanceSchedule(
                                id = initial?.id ?: newId(),
                                itemId = eq.id,
                                itemName = eq.name,
                                serviceTitle = serviceTitle.trim(),
                                intervalDays = days,
                                intervalRentals = rentals,
                                lastServicedMs = initial?.lastServicedMs ?: now,
                                nextDueMs = nextDue,
                                status = initial?.status ?: "scheduled",
                                priority = priority,
                                technicianName = technicianName.trim().ifEmpty { "Depot Technician" },
                                notes = notes.trim(),
                                createdAt = initial?.createdAt ?: now,
                            )
                        )
                    }
                },
                enabled = serviceTitle.trim().isNotEmpty() && selectedItemId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
            ) {
                Text("Save Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
