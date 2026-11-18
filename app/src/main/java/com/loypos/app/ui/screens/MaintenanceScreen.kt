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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.nowMs
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.data.local.MaintenanceScheduleEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.ConfirmDialog
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    initialItemId: String? = null,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val now = remember { nowMs() }

    var search by remember { mutableStateOf("") }
    var filterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Due/Overdue, 2=Scheduled, 3=Completed
    var showAddDialog by remember { mutableStateOf(initialItemId != null) }
    var editingSchedule by remember { mutableStateOf<MaintenanceScheduleEntity?>(null) }
    var scheduleToDelete by remember { mutableStateOf<MaintenanceScheduleEntity?>(null) }

    val schedules = data.maintenanceSchedules
    val filtered = remember(schedules, search, filterIdx, now) {
        val q = search.trim().lowercase()
        schedules.filter { s ->
            val matchesSearch = q.isEmpty() ||
                s.itemName.lowercase().contains(q) ||
                s.serviceTitle.lowercase().contains(q) ||
                s.technicianName.lowercase().contains(q) ||
                s.notes.lowercase().contains(q)
            val matchesFilter = when (filterIdx) {
                1 -> s.isDue(now)
                2 -> s.status == MaintenanceScheduleEntity.STATUS_SCHEDULED && !s.isDue(now)
                3 -> s.status == MaintenanceScheduleEntity.STATUS_COMPLETED
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val dueCount = remember(schedules, now) { schedules.count { it.isDue(now) } }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("maintenance_back_btn")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            "Equipment Maintenance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Recurring service schedules for sound, lighting & rigs",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_maintenance_schedule_btn"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Schedule", tint = LoyGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LoyBackground)
                .padding(padding),
        ) {
            // Search & Status filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoySurface)
                    .padding(horizontal = metrics.screenPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search equipment, title, or technician",
                    inputTestTag = "maintenance_search_input",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tabs = listOf(
                        "All (${schedules.size})",
                        "Due / Overdue ($dueCount)",
                        "Scheduled (${schedules.count { it.status == MaintenanceScheduleEntity.STATUS_SCHEDULED && !it.isDue(now) }})",
                        "Completed (${schedules.count { it.status == MaintenanceScheduleEntity.STATUS_COMPLETED }})",
                    )
                    tabs.forEachIndexed { idx, label ->
                        item {
                            FilterChip(
                                selected = filterIdx == idx,
                                onClick = { filterIdx = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (idx == 1 && dueCount > 0) Color(0xFFFDE8E8) else LoyGreenLight,
                                    selectedLabelColor = if (idx == 1 && dueCount > 0) LoyError else LoyGreen,
                                ),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.Build,
                        title = "No maintenance schedules found",
                        subtitle = "Track recurrent safety inspections and repairs for high-value equipment.",
                        actionLabel = "Schedule Service",
                        onAction = { showAddDialog = true },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(metrics.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                ) {
                    items(filtered, key = { it.id }) { schedule ->
                        val itemRentalCount = data.itemRentalCount(schedule.itemId)
                        MaintenanceScheduleCard(
                            schedule = schedule,
                            nowMs = now,
                            rentalCount = itemRentalCount,
                            onComplete = { viewModel.completeMaintenance(schedule.id) },
                            onEdit = { editingSchedule = schedule },
                            onDelete = { scheduleToDelete = schedule },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editingSchedule != null) {
        val targetSchedule = editingSchedule
        AddEditMaintenanceDialog(
            items = data.items.filter { !it.archived },
            existing = targetSchedule,
            initialItemId = initialItemId,
            onDismiss = {
                showAddDialog = false
                editingSchedule = null
            },
            onSave = { itemId, itemName, title, intervalDays, intervalRentals, priority, tech, notes ->
                viewModel.saveMaintenanceSchedule(
                    itemId = itemId,
                    itemName = itemName,
                    serviceTitle = title,
                    intervalDays = intervalDays,
                    intervalRentals = intervalRentals,
                    priority = priority,
                    technicianName = tech,
                    notes = notes,
                    id = targetSchedule?.id,
                )
                showAddDialog = false
                editingSchedule = null
            },
        )
    }

    scheduleToDelete?.let { schedule ->
        ConfirmDialog(
            title = "Delete Service Schedule?",
            message = "Remove recurring maintenance schedule for ${schedule.itemName} (${schedule.serviceTitle})?",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteMaintenanceSchedule(schedule.id)
                scheduleToDelete = null
            },
            onDismiss = { scheduleToDelete = null },
        )
    }
}

@Composable
private fun MaintenanceScheduleCard(
    schedule: MaintenanceScheduleEntity,
    nowMs: Long,
    rentalCount: Int,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDue = schedule.isDue(nowMs)
    val statusColor = when {
        isDue -> LoyError
        schedule.status == MaintenanceScheduleEntity.STATUS_COMPLETED -> LoyGreen
        else -> LoyInfo
    }
    val statusBg = when {
        isDue -> LoyErrorLight
        schedule.status == MaintenanceScheduleEntity.STATUS_COMPLETED -> LoyGreenLight
        else -> LoyInfoLight
    }
    val statusLabel = when {
        isDue -> "Service Due"
        schedule.status == MaintenanceScheduleEntity.STATUS_COMPLETED -> "Completed"
        else -> "Scheduled"
    }

    GroupCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("maintenance_card_${schedule.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        schedule.serviceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LoyTextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        schedule.itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoyTextSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillBadge(
                        text = statusLabel,
                        backgroundColor = statusBg,
                        textColor = statusColor,
                    )
                    if (schedule.priority == MaintenanceScheduleEntity.PRIORITY_URGENT) {
                        PillBadge("Urgent", backgroundColor = Color(0xFFFFEBEE), textColor = LoyError)
                    } else if (schedule.priority == MaintenanceScheduleEntity.PRIORITY_HIGH) {
                        PillBadge("High", backgroundColor = Color(0xFFFFF3E0), textColor = LoyWarning)
                    }
                }
            }

            HorizontalDivider(color = LoyBorder)

            // Recurrence & Wear stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(16.dp))
                    Text(
                        "Every ${schedule.intervalDays} days (or ${schedule.intervalRentals} rentals)",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }

                PillBadge(
                    text = "Rented $rentalCount times",
                    backgroundColor = Color(0xFFF1F5F9),
                    textColor = Color(0xFF334155),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (schedule.nextDueMs > 0L) "Next due: ${formatShortDate(schedule.nextDueMs)}" else "No next date",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDue) LoyError else LoyTextSecondary,
                )
                Text(
                    text = "Tech: ${schedule.technicianName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }

            if (schedule.notes.isNotBlank()) {
                Text(
                    text = "Note: ${schedule.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }

            HorizontalDivider(color = LoyBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit schedule", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete schedule", tint = LoyError, modifier = Modifier.size(18.dp))
                    }
                }

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                    modifier = Modifier.height(36.dp).testTag("complete_maint_${schedule.id}"),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Serviced", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditMaintenanceDialog(
    items: List<ItemEntity>,
    existing: MaintenanceScheduleEntity?,
    initialItemId: String?,
    onDismiss: () -> Unit,
    onSave: (
        itemId: String,
        itemName: String,
        title: String,
        intervalDays: Int,
        intervalRentals: Int,
        priority: String,
        tech: String,
        notes: String,
    ) -> Unit,
) {
    var selectedItem by remember {
        mutableStateOf(
            existing?.let { e -> items.firstOrNull { it.id == e.itemId } }
                ?: initialItemId?.let { id -> items.firstOrNull { it.id == id } }
                ?: items.firstOrNull()
        )
    }
    var title by remember { mutableStateOf(existing?.serviceTitle ?: "") }
    var intervalDaysStr by remember { mutableStateOf(existing?.intervalDays?.toString() ?: "30") }
    var intervalRentalsStr by remember { mutableStateOf(existing?.intervalRentals?.toString() ?: "10") }
    var priority by remember { mutableStateOf(existing?.priority ?: MaintenanceScheduleEntity.PRIORITY_ROUTINE) }
    var tech by remember { mutableStateOf(existing?.technicianName ?: "Depot Technician") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var itemMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    val isValid = (selectedItem != null) && title.isNotBlank() &&
        (intervalDaysStr.toIntOrNull() != null) && (intervalRentalsStr.toIntOrNull() != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Schedule Equipment Maintenance" else "Edit Maintenance Schedule")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Item Selector
                ExposedDropdownMenuBox(
                    expanded = itemMenuExpanded,
                    onExpandedChange = { itemMenuExpanded = !itemMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedItem?.name ?: "Select equipment...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipment Item") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = itemMenuExpanded,
                        onDismissRequest = { itemMenuExpanded = false },
                    ) {
                        items.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} (${item.sku})") },
                                onClick = {
                                    selectedItem = item
                                    itemMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service Title") },
                    placeholder = { Text("e.g. Acoustic calibration & amp test") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = intervalDaysStr,
                        onValueChange = { intervalDaysStr = it },
                        label = { Text("Every X Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = intervalRentalsStr,
                        onValueChange = { intervalRentalsStr = it },
                        label = { Text("Every Y Rentals") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = priorityMenuExpanded,
                    onExpandedChange = { priorityMenuExpanded = !priorityMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = priority.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false },
                    ) {
                        listOf(
                            MaintenanceScheduleEntity.PRIORITY_ROUTINE to "Routine Service",
                            MaintenanceScheduleEntity.PRIORITY_HIGH to "High Priority",
                            MaintenanceScheduleEntity.PRIORITY_URGENT to "Urgent Safety Check",
                        ).forEach { (pKey, pLabel) ->
                            DropdownMenuItem(
                                text = { Text(pLabel) },
                                onClick = {
                                    priority = pKey
                                    priorityMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = tech,
                    onValueChange = { tech = it },
                    label = { Text("Technician / Crew") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Service notes & checklist") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedItem ?: return@Button
                    val days = intervalDaysStr.toIntOrNull() ?: 30
                    val rentals = intervalRentalsStr.toIntOrNull() ?: 10
                    onSave(target.id, target.name, title, days, rentals, priority, tech, notes)
                },
                enabled = isValid,
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text(if (existing == null) "Schedule Service" else "Update Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("Cancel")
            }
        },
    )
}
