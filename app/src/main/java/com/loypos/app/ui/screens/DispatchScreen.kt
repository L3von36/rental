package com.loypos.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.dayStartMs
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.localDateFromMs
import com.loypos.app.core.nowMs
import com.loypos.app.data.local.DeliveryStopEntity
import com.loypos.app.data.local.ReceiptWithLines
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
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.LoyWarningLight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    initialReceiptId: String? = null,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val now = remember(data.deliveryStops) { nowMs() }

    var search by remember { mutableStateOf("") }
    var filterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Today's Runs, 2=Outbound, 3=Inbound, 4=Done
    var showAddDialog by remember(initialReceiptId) { mutableStateOf(initialReceiptId != null) }
    var preselectReceiptId by remember(initialReceiptId) { mutableStateOf(initialReceiptId) }
    var editingStop by remember { mutableStateOf<DeliveryStopEntity?>(null) }
    var stopToDelete by remember { mutableStateOf<DeliveryStopEntity?>(null) }

    val stops = data.deliveryStops
    val startOfToday = remember(now) { dayStartMs(LocalDate.now()) }
    val endOfToday = startOfToday + 86_400_000L

    val filtered = remember(stops, search, filterIdx, startOfToday) {
        val q = search.trim().lowercase()
        stops.filter { s ->
            val matchesSearch = q.isEmpty() ||
                s.venue.lowercase().contains(q) ||
                s.customerName.lowercase().contains(q) ||
                s.eventName.lowercase().contains(q) ||
                s.driverName.lowercase().contains(q) ||
                s.itemsSummary.lowercase().contains(q)
            val matchesFilter = when (filterIdx) {
                1 -> s.scheduledMs in startOfToday until endOfToday
                2 -> s.type == DeliveryStopEntity.TYPE_DELIVERY
                3 -> s.type == DeliveryStopEntity.TYPE_PICKUP
                4 -> s.status == DeliveryStopEntity.STATUS_DONE
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val overdueCount = remember(stops, now) { stops.count { it.isOverdue(now) } }
    val todayCount = remember(stops, startOfToday) {
        stops.count { it.scheduledMs in startOfToday until endOfToday && it.isActive }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dispatch_back_btn")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            "Dispatch Board",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Delivery & pickup runs to event venues",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            preselectReceiptId = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_dispatch_stop_btn"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Stop", tint = LoyGreen)
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
                    placeholder = "Search venue, client, event, or driver",
                    inputTestTag = "dispatch_search_input",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tabs = listOf(
                        "All (${stops.size})",
                        "Today ($todayCount)",
                        "Deliveries (${stops.count { it.type == DeliveryStopEntity.TYPE_DELIVERY }})",
                        "Pickups (${stops.count { it.type == DeliveryStopEntity.TYPE_PICKUP }})",
                        "Completed (${stops.count { it.status == DeliveryStopEntity.STATUS_DONE }})",
                    )
                    tabs.forEachIndexed { idx, label ->
                        item {
                            FilterChip(
                                selected = filterIdx == idx,
                                onClick = { filterIdx = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (idx == 1 && overdueCount > 0) Color(0xFFFDE8E8) else LoyGreenLight,
                                    selectedLabelColor = if (idx == 1 && overdueCount > 0) LoyError else LoyGreen,
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
                        icon = Icons.Outlined.LocalShipping,
                        title = "No dispatch runs found",
                        subtitle = "Schedule delivery and pickup stops for event venues — stops auto-generate when a rental with a venue is checked out.",
                        actionLabel = "Plan a Run",
                        onAction = {
                            preselectReceiptId = null
                            showAddDialog = true
                        },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(metrics.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                ) {
                    items(filtered, key = { it.id }) { stop ->
                        DispatchStopCard(
                            stop = stop,
                            nowMs = now,
                            onStart = { viewModel.advanceDeliveryStop(stop) },
                            onFail = { viewModel.markDeliveryStopFailed(stop) },
                            onEdit = { editingStop = stop },
                            onDelete = { stopToDelete = stop },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editingStop != null) {
        val outRentals = data.receipts.filter { it.receipt.isRentalOut }
        AddEditDispatchDialog(
            outRentals = outRentals,
            existing = editingStop,
            preselectReceiptId = preselectReceiptId,
            onDismiss = {
                showAddDialog = false
                preselectReceiptId = null
                editingStop = null
            },
            onSave = { type, rental, customerName, eventName, venue, phone, scheduledMs, driver, vehicle, itemsSummary, notes ->
                viewModel.saveDeliveryStop(
                    type = type,
                    receiptId = rental?.receipt?.id,
                    receiptNumber = rental?.receipt?.number,
                    customerName = customerName,
                    eventName = eventName,
                    venue = venue,
                    contactPhone = phone,
                    scheduledMs = scheduledMs,
                    driverName = driver,
                    vehicle = vehicle,
                    itemsSummary = itemsSummary,
                    notes = notes,
                    existing = editingStop,
                )
                showAddDialog = false
                preselectReceiptId = null
                editingStop = null
            },
        )
    }

    stopToDelete?.let { stop ->
        ConfirmDialog(
            title = "Delete Dispatch Stop?",
            message = "Remove the ${stop.type} run to ${stop.venue} from the dispatch board?",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteDeliveryStop(stop.id)
                stopToDelete = null
            },
            onDismiss = { stopToDelete = null },
        )
    }
}

@Composable
private fun DispatchStopCard(
    stop: DeliveryStopEntity,
    nowMs: Long,
    onStart: () -> Unit,
    onFail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOverdue = stop.isOverdue(nowMs)
    val isPickup = stop.type == DeliveryStopEntity.TYPE_PICKUP
    val (statusColor, statusBg, statusLabel) = when {
        stop.status == DeliveryStopEntity.STATUS_DONE -> Triple(LoyGreen, LoyGreenLight, "Done")
        stop.status == DeliveryStopEntity.STATUS_FAILED -> Triple(LoyError, LoyErrorLight, "Failed")
        stop.status == DeliveryStopEntity.STATUS_EN_ROUTE -> Triple(LoyWarning, LoyWarningLight, "En Route")
        isOverdue -> Triple(LoyError, LoyErrorLight, "Overdue")
        else -> Triple(LoyInfo, LoyInfoLight, "Scheduled")
    }

    GroupCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dispatch_card_${stop.id}"),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPickup) LoySurfaceVariant else LoyGreenLight,
                    ) {
                        Icon(
                            imageVector = if (isPickup) Icons.Outlined.Undo else Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = if (isPickup) LoyTextSecondary else LoyGreen,
                            modifier = Modifier.padding(8.dp).size(18.dp),
                        )
                    }
                    Column {
                        Text(
                            if (isPickup) "PICKUP" else "DELIVERY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPickup) LoyTextSecondary else LoyGreen,
                        )
                        Text(
                            text = buildString {
                                append(formatDateTime(stop.scheduledMs))
                                if (stop.receiptNumber != null) append("  •  #${stop.receiptNumber}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) LoyError else LoyTextSecondary,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                PillBadge(
                    text = statusLabel,
                    backgroundColor = statusBg,
                    textColor = statusColor,
                )
            }

            HorizontalDivider(color = LoyBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Text(
                    stop.venue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LoyTextPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CLIENT & EVENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                    )
                    Text(
                        listOf(stop.customerName, stop.eventName).filter { it.isNotBlank() }
                            .ifEmpty { listOf("Walk-in") }
                            .joinToString(" — "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = LoyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "DRIVER & VEHICLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                    )
                    Text(
                        listOf(stop.driverName, stop.vehicle).filter { it.isNotBlank() }
                            .ifEmpty { listOf("Unassigned") }
                            .joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = LoyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (stop.itemsSummary.isNotBlank()) {
                Text(
                    text = "Load: ${stop.itemsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (stop.notes.isNotBlank()) {
                Text(
                    text = "Note: ${stop.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit stop", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete stop", tint = LoyError, modifier = Modifier.size(18.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (stop.isActive) {
                        OutlinedButton(
                            onClick = onFail,
                            enabled = stop.status == DeliveryStopEntity.STATUS_EN_ROUTE,
                            modifier = Modifier.height(36.dp),
                        ) {
                            Text("Failed", style = MaterialTheme.typography.labelMedium, color = if (stop.status == DeliveryStopEntity.STATUS_EN_ROUTE) LoyError else LoyTextSecondary)
                        }
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (stop.status == DeliveryStopEntity.STATUS_SCHEDULED) LoyInfo else LoyGreen,
                            ),
                            modifier = Modifier.height(36.dp).testTag("advance_dispatch_${stop.id}"),
                        ) {
                            Text(
                                if (stop.status == DeliveryStopEntity.STATUS_SCHEDULED) "Start Run" else "Mark Delivered",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else {
                        Text(
                            text = if (stop.status == DeliveryStopEntity.STATUS_DONE && stop.completedAtMs != null) {
                                "Completed ${formatShortDate(stop.completedAtMs)}"
                            } else {
                                "Run ${statusLabel.lowercase()}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditDispatchDialog(
    outRentals: List<ReceiptWithLines>,
    existing: DeliveryStopEntity?,
    preselectReceiptId: String?,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        rental: ReceiptWithLines?,
        customerName: String,
        eventName: String,
        venue: String,
        phone: String,
        scheduledMs: Long,
        driver: String,
        vehicle: String,
        itemsSummary: String,
        notes: String,
    ) -> Unit,
) {
    val linkedRental = existing?.receiptId?.let { rid -> outRentals.firstOrNull { it.receipt.id == rid } }
        ?: preselectReceiptId?.let { rid -> outRentals.firstOrNull { it.receipt.id == rid } }

    var type by remember(existing) {
        mutableStateOf(existing?.type ?: DeliveryStopEntity.TYPE_DELIVERY)
    }
    var selectedRental by remember(existing) { mutableStateOf(linkedRental) }
    var customerName by remember(existing) { mutableStateOf(existing?.customerName ?: linkedRental?.receipt?.customerName ?: "") }
    var eventName by remember(existing) { mutableStateOf(existing?.eventName ?: linkedRental?.receipt?.eventName ?: "") }
    var venue by remember(existing) { mutableStateOf(existing?.venue ?: linkedRental?.receipt?.eventVenue ?: "") }
    var phone by remember(existing) { mutableStateOf(existing?.contactPhone ?: "") }
    var driver by remember(existing) { mutableStateOf(existing?.driverName ?: "") }
    var vehicle by remember(existing) { mutableStateOf(existing?.vehicle ?: "") }
    var itemsSummary by remember(existing) { mutableStateOf(existing?.itemsSummary ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

    val initialDate = existing?.let { localDateFromMs(it.scheduledMs) } ?: LocalDate.now()
    var scheduleDate by remember(existing) { mutableStateOf(initialDate) }
    var hourOfDay by remember(existing) { mutableStateOf(existing?.let { 9 } ?: 9) }
    var showDatePicker by remember { mutableStateOf(false) }
    var rentalMenuExpanded by remember { mutableStateOf(false) }
    var hourMenuExpanded by remember { mutableStateOf(false) }

    val isValid = venue.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Plan Dispatch Run" else "Edit Dispatch Run")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (existing == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = type == DeliveryStopEntity.TYPE_DELIVERY,
                            onClick = { type = DeliveryStopEntity.TYPE_DELIVERY },
                            label = { Text("Deliver to venue") },
                            modifier = Modifier.weight(1f).height(38.dp),
                        )
                        FilterChip(
                            selected = type == DeliveryStopEntity.TYPE_PICKUP,
                            onClick = { type = DeliveryStopEntity.TYPE_PICKUP },
                            label = { Text("Pick up") },
                            modifier = Modifier.weight(1f).height(38.dp),
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = rentalMenuExpanded,
                    onExpandedChange = { rentalMenuExpanded = !rentalMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedRental?.let { r ->
                            "#${r.receipt.number} ${r.receipt.customerName ?: "Walk-in"} — ${r.receipt.eventVenue.ifBlank { "no venue" }}"
                        } ?: "No linked rental",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Linked Rental (optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rentalMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = rentalMenuExpanded,
                        onDismissRequest = { rentalMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("No linked rental") },
                            onClick = {
                                selectedRental = null
                                rentalMenuExpanded = false
                            },
                        )
                        outRentals.forEach { r ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "#${r.receipt.number} ${r.receipt.customerName ?: "Walk-in"} — ${r.receipt.eventVenue.ifBlank { "no venue" }}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    selectedRental = r
                                    if (customerName.isBlank()) customerName = r.receipt.customerName ?: ""
                                    if (eventName.isBlank()) eventName = r.receipt.eventName
                                    if (venue.isBlank()) venue = r.receipt.eventVenue
                                    if (itemsSummary.isBlank()) {
                                        itemsSummary = r.lines.joinToString(", ") { line ->
                                            "${com.loypos.app.core.formatQty(line.qty)}× ${line.name}"
                                        }.take(180)
                                    }
                                    rentalMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Delivery Address *") },
                    placeholder = { Text("e.g. Hilton Garden Inn — Grand Ballroom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Client") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Event") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${scheduleDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${formatShortDate(dayStartMs(scheduleDate))}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ExposedDropdownMenuBox(
                        expanded = hourMenuExpanded,
                        onExpandedChange = { hourMenuExpanded = !hourMenuExpanded },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d:00", hourOfDay),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Load-out Time") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = hourMenuExpanded,
                            onDismissRequest = { hourMenuExpanded = false },
                        ) {
                            (5..21).forEach { h ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d:00", h)) },
                                    onClick = {
                                        hourOfDay = h
                                        hourMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("On-site Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = driver,
                        onValueChange = { driver = it },
                        label = { Text("Driver / Crew") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = vehicle,
                    onValueChange = { vehicle = it },
                    label = { Text("Vehicle") },
                    placeholder = { Text("e.g. Isuzu NPR") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Load / Packing List") },
                    placeholder = { Text("2× Canopy Tent, 12× Chiavari Chair...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Stop notes (gate code, dock hours, contact...)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scheduledMs = dayStartMs(scheduleDate) + hourOfDay.toLong() * 3_600_000L
                    onSave(
                        type,
                        selectedRental,
                        customerName,
                        eventName,
                        venue,
                        phone,
                        scheduledMs,
                        driver,
                        vehicle,
                        itemsSummary,
                        notes,
                    )
                },
                enabled = isValid,
                modifier = Modifier.heightIn(min = 44.dp),
            ) {
                Text(if (existing == null) "Schedule Stop" else "Update Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("Cancel")
            }
        },
    )

    if (showDatePicker) {
        val initialUtcMs = scheduleDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = datePickerState.selectedDateMillis
                        if (utcMillis != null) {
                            scheduleDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
