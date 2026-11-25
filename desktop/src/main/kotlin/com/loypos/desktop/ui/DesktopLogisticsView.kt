package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Undo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.dayStartMs
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopDeliveryStop
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DesktopLogisticsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") } // all | today | delivery | pickup | done
    var selectedId by remember { mutableStateOf<String?>(snapshot.deliveryStops.firstOrNull()?.id) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingStop by remember { mutableStateOf<DesktopDeliveryStop?>(null) }

    val now = remember(snapshot.deliveryStops) { nowMs() }
    val startOfToday = remember(now) { dayStartMs(LocalDate.now()) }
    val endOfToday = startOfToday + 86_400_000L

    val todayCount = remember(snapshot.deliveryStops, startOfToday) {
        snapshot.deliveryStops.count { it.scheduledMs in startOfToday until endOfToday && it.isActive }
    }
    val overdueCount = remember(snapshot.deliveryStops, now) {
        snapshot.deliveryStops.count { it.isOverdue(now) }
    }
    val activeCount = remember(snapshot.deliveryStops) {
        snapshot.deliveryStops.count { it.isActive }
    }
    val doneCount = remember(snapshot.deliveryStops) {
        snapshot.deliveryStops.count { it.status == "done" }
    }

    val filteredStops = remember(snapshot.deliveryStops, search, filterStatus, startOfToday, endOfToday) {
        val q = search.trim().lowercase()
        snapshot.deliveryStops.filter { s ->
            val matchStatus = when (filterStatus) {
                "today" -> s.scheduledMs in startOfToday until endOfToday
                "delivery" -> s.type == "delivery"
                "pickup" -> s.type == "pickup"
                "done" -> s.status == "done"
                else -> true
            }
            val matchQuery = q.isEmpty() ||
                s.venue.lowercase().contains(q) ||
                s.customerName.lowercase().contains(q) ||
                s.eventName.lowercase().contains(q) ||
                s.driverName.lowercase().contains(q) ||
                s.itemsSummary.lowercase().contains(q)
            matchStatus && matchQuery
        }.sortedBy { it.scheduledMs }
    }

    val activeStop = remember(filteredStops, selectedId) {
        filteredStops.firstOrNull { it.id == selectedId } ?: filteredStops.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopKpiTile(
                        "Today's Runs",
                        todayCount.toString(),
                        if (overdueCount > 0) LoyError else LoyGreen,
                        Modifier.weight(1f),
                        caption = "Scheduled stop window",
                    )
                    DesktopKpiTile("Active Runs", activeCount.toString(), LoyInfo, Modifier.weight(1f), caption = "Awaiting driver action")
                    DesktopKpiTile("Overdue", overdueCount.toString(), if (overdueCount > 0) LoyError else LoyGreenDark, Modifier.weight(1f), caption = "Past scheduled time")
                    DesktopKpiTile("Completed", doneCount.toString(), LoyGreen, Modifier.weight(1f), caption = "Signed off runs")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DesktopSearchInput(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search venue, client, event, driver, or load list...",
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSegmentedControl(
                        options = listOf(
                            "all" to "All (${snapshot.deliveryStops.size})",
                            "today" to "Today ($todayCount)",
                            "delivery" to "Deliveries (${snapshot.deliveryStops.count { it.type == "delivery" }})",
                            "pickup" to "Pickups (${snapshot.deliveryStops.count { it.type == "pickup" }})",
                            "done" to "Done ($doneCount)",
                        ),
                        selected = filterStatus,
                        onSelect = { filterStatus = it },
                    )
                    DesktopPrimaryButton(
                        text = "Plan Run",
                        icon = Icons.Outlined.LocalShipping,
                        onClick = { showCreateDialog = true },
                    )
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Master Table (48%)
            Column(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
                if (filteredStops.isEmpty()) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.LocalShipping,
                        title = "No Dispatch Runs",
                        subtitle = "Plan delivery and pickup stops to event venues. Runs can also be generated straight from a rental order with a venue.",
                        action = {
                            DesktopPrimaryButton(
                                text = "Plan First Run",
                                icon = Icons.Outlined.Add,
                                onClick = { showCreateDialog = true },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    DesktopTableHeaderRow {
                        DesktopTableHeaderText("Run / Venue", Modifier.weight(1.5f))
                        DesktopTableHeaderText("Scheduled", Modifier.width(120.dp))
                        DesktopTableHeaderText("Status", Modifier.width(96.dp))
                    }
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                        items(filteredStops, key = { it.id }) { stop ->
                            val isOverdue = stop.isOverdue(now)
                            val statusColors = when {
                                stop.status == "done" -> LoyGreenLight to LoyGreenDark
                                stop.status == "failed" -> LoyErrorLight to LoyError
                                stop.status == "en_route" -> LoyWarningLight to LoyWarning
                                isOverdue -> LoyErrorLight to LoyError
                                else -> LoyInfoLight to LoyInfo
                            }
                            DesktopTableRow(
                                selected = activeStop?.id == stop.id,
                                onClick = { selectedId = stop.id },
                            ) {
                                Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (stop.isDelivery) Icons.Outlined.LocalShipping else Icons.Outlined.Undo,
                                            contentDescription = null,
                                            tint = if (stop.isDelivery) LoyGreen else LoyTextMuted,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            text = if (stop.isDelivery) "DELIVERY" else "PICKUP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (stop.isDelivery) LoyGreen else LoyTextMuted,
                                        )
                                        if (stop.orderNumber != null) {
                                            Text(
                                                text = "#${stop.orderNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LoyTextMuted,
                                            )
                                        }
                                    }
                                    Text(
                                        text = stop.venue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = formatDateTime(stop.scheduledMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOverdue) LoyError else LoyTextPrimary,
                                    modifier = Modifier.width(120.dp),
                                )
                                Box(modifier = Modifier.width(96.dp)) {
                                    DesktopBadge(
                                        text = when {
                                            stop.status == "done" -> "Done"
                                            stop.status == "failed" -> "Failed"
                                            stop.status == "en_route" -> "En Route"
                                            isOverdue -> "Overdue"
                                            else -> "Scheduled"
                                        },
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
                val stop = activeStop
                if (stop == null) {
                    DesktopEmptyState(
                        icon = Icons.Outlined.LocalShipping,
                        title = "Select a Dispatch Run",
                        subtitle = "Choose any delivery or pickup run on the left to inspect the load list, driver assignment, and venue notes.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val isOverdue = stop.isOverdue(now)
                    val statusColors = when {
                        stop.status == "done" -> LoyGreenLight to LoyGreenDark
                        stop.status == "failed" -> LoyErrorLight to LoyError
                        stop.status == "en_route" -> LoyWarningLight to LoyWarning
                        isOverdue -> LoyErrorLight to LoyError
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DesktopBadge(
                                if (stop.isDelivery) "Delivery Run" else "Pickup Run",
                                if (stop.isDelivery) LoyGreenLight else LoySurfaceVariant,
                                if (stop.isDelivery) LoyGreenDark else LoyTextSecondary,
                            )
                            DesktopBadge(
                                when {
                                    stop.status == "done" -> "Done"
                                    stop.status == "failed" -> "Failed"
                                    stop.status == "en_route" -> "En Route"
                                    isOverdue -> "Overdue"
                                    else -> "Scheduled"
                                },
                                statusColors.first,
                                statusColors.second,
                            )
                            if (stop.orderNumber != null) {
                                DesktopBadge("Order #${stop.orderNumber}", LoySurfaceVariant, LoyTextSecondary)
                            }
                        }

                        Text(stop.venue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (stop.eventName.isNotBlank() || stop.customerName.isNotBlank()) {
                            Text(
                                text = listOf(stop.eventName, stop.customerName).filter { it.isNotBlank() }.joinToString(" — "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyGreenDark,
                            )
                        }

                        DesktopSectionCard(title = "Schedule & Crew Assignment") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SCHEDULED FOR", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        formatDateTime(stop.scheduledMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOverdue) LoyError else LoyTextPrimary,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CONTACT", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(
                                        stop.contactPhone.ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DRIVER / CREW", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(stop.driverName.ifBlank { "Unassigned" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("VEHICLE", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                                    Text(stop.vehicle.ifBlank { "— " }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (stop.completedAtMs != null) {
                                Text(
                                    text = "Closed at ${formatDateTime(stop.completedAtMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextMuted,
                                )
                            }
                        }

                        DesktopSectionCard(title = "Load / Packing List") {
                            Text(
                                text = stop.itemsSummary.ifBlank { "No load list recorded." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (stop.itemsSummary.isBlank()) LoyTextMuted else LoyTextPrimary,
                            )
                        }

                        DesktopSectionCard(title = "Stop Notes") {
                            Text(
                                text = stop.notes.ifBlank { "No special venue instructions provided." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (stop.notes.isBlank()) LoyTextMuted else LoyTextPrimary,
                            )
                        }
                    }

                    HorizontalDivider(color = LoyBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().background(LoySurfaceHover).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when {
                            stop.status == "scheduled" -> DesktopPrimaryButton(
                                text = "Start Run",
                                icon = Icons.Outlined.LocalShipping,
                                containerColor = LoyInfo,
                                onClick = {
                                    repository.advanceDeliveryStop(stop.id)
                                    onToast("Driver en route to ${stop.venue}")
                                },
                            )
                            stop.status == "en_route" -> DesktopPrimaryButton(
                                text = "Mark Delivered",
                                icon = Icons.Outlined.CheckCircle,
                                onClick = {
                                    repository.advanceDeliveryStop(stop.id)
                                    onToast("Run completed — ${stop.venue}")
                                },
                            )
                            else -> DesktopBadge(
                                when (stop.status) {
                                    "done" -> "Completed ${stop.completedAtMs?.let { formatShortDate(it) } ?: ""}"
                                    else -> "Run failed"
                                },
                                LoySurfaceVariant,
                                LoyTextSecondary,
                            )
                        }
                        if (stop.isActive) {
                            DesktopSecondaryButton(
                                text = "Failed",
                                icon = Icons.Outlined.ReportProblem,
                                contentColor = LoyError,
                                onClick = {
                                    repository.failDeliveryStop(stop.id)
                                    onToast("Stop marked failed — ${stop.venue}")
                                },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        DesktopSecondaryButton(
                            text = "Edit",
                            icon = Icons.Outlined.Edit,
                            onClick = { editingStop = stop },
                        )
                        DesktopSecondaryButton(
                            text = "Delete",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = {
                                repository.deleteDeliveryStop(stop.id)
                                selectedId = null
                                onToast("Deleted dispatch run")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingStop != null) {
        DesktopDispatchEditorDialog(
            initial = editingStop,
            snapshot = snapshot,
            onSave = { stop ->
                repository.saveDeliveryStop(stop)
                selectedId = stop.id
                onToast(if (stop.isDelivery) "Delivery run saved — ${stop.venue}" else "Pickup run saved — ${stop.venue}")
                showCreateDialog = false
                editingStop = null
            },
            onDismiss = {
                showCreateDialog = false
                editingStop = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopDispatchEditorDialog(
    initial: DesktopDeliveryStop?,
    snapshot: DesktopStoreSnapshot,
    onSave: (DesktopDeliveryStop) -> Unit,
    onDismiss: () -> Unit,
) {
    val venueOrders = remember(snapshot.orders) {
        snapshot.orders.filter { it.eventVenue.isNotBlank() && !it.isReturned }.sortedByDescending { it.createdAt }
    }
    val initialOrder = initial?.orderId?.let { oid -> venueOrders.firstOrNull { it.id == oid } }

    var type by remember(initial) { mutableStateOf(initial?.type ?: "delivery") }
    var selectedOrder by remember(initial) { mutableStateOf(initialOrder) }
    var customerName by remember(initial) { mutableStateOf(initial?.customerName ?: initialOrder?.customerName ?: "") }
    var eventName by remember(initial) { mutableStateOf(initial?.eventName ?: initialOrder?.eventName ?: "") }
    var venue by remember(initial) { mutableStateOf(initial?.venue ?: initialOrder?.eventVenue ?: "") }
    var contactPhone by remember(initial) { mutableStateOf(initial?.contactPhone ?: "") }
    var driverName by remember(initial) { mutableStateOf(initial?.driverName ?: "") }
    var vehicle by remember(initial) { mutableStateOf(initial?.vehicle ?: "") }
    var itemsSummary by remember(initial) {
        mutableStateOf(
            initial?.itemsSummary ?: initialOrder?.lines?.joinToString(", ") { l -> "${l.qty.toInt().coerceAtLeast(1)}× ${l.name}" }?.take(180) ?: ""
        )
    }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    var dateText by remember(initial) {
        mutableStateOf(
            initial?.scheduledMs?.let { ms ->
                java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            } ?: LocalDate.now().format(dateFormatter)
        )
    }
    var hourText by remember(initial) {
        mutableStateOf(
            initial?.scheduledMs?.let { ms ->
                java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).hour.toString()
            } ?: "9"
        )
    }
    var orderMenuExpanded by remember { mutableStateOf(false) }

    val scheduledMs = remember(dateText, hourText) {
        try {
            val date = LocalDate.parse(dateText.trim(), dateFormatter)
            dayStartMs(date) + (hourText.trim().toIntOrNull()?.coerceIn(0, 23) ?: 9) * 3_600_000L
        } catch (_: Exception) {
            0L
        }
    }
    val isValid = venue.trim().isNotEmpty() && scheduledMs > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Plan Dispatch Run" else "Edit Dispatch Run", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.width(560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Run Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                    DesktopSegmentedControl(
                        options = listOf("delivery" to "Deliver to Venue", "pickup" to "Pick Up from Venue"),
                        selected = type,
                        onSelect = { type = it },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Linked Rental Order (optional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
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
                                    text = selectedOrder?.let { "#${it.number} ${it.customerName ?: "Walk-in"} — ${it.eventVenue}" } ?: "No linked order",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderMenuExpanded)
                            }
                        }
                        ExposedDropdownMenu(expanded = orderMenuExpanded, onDismissRequest = { orderMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("No linked order") },
                                onClick = {
                                    selectedOrder = null
                                    orderMenuExpanded = false
                                },
                            )
                            venueOrders.forEach { ord ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "#${ord.number} ${ord.customerName ?: "Walk-in"} — ${ord.eventVenue}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        selectedOrder = ord
                                        customerName = ord.customerName ?: customerName
                                        eventName = ord.eventName.ifBlank { eventName }
                                        venue = ord.eventVenue
                                        if (itemsSummary.isBlank()) {
                                            itemsSummary = ord.lines.joinToString(", ") { l -> "${l.qty.toInt().coerceAtLeast(1)}× ${l.name}" }.take(180)
                                        }
                                        orderMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                DesktopFormInput("Venue / Delivery Address *", venue, { venue = it }, placeholder = "e.g. Hilton Garden Inn — Grand Ballroom")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Client", customerName, { customerName = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Event", eventName, { eventName = it }, modifier = Modifier.weight(1f))
                }
                DesktopFormInput("On-site Contact Phone", contactPhone, { contactPhone = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Date (yyyy-MM-dd)", dateText, { dateText = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Hour (0-23)", hourText, { hourText = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopFormInput("Driver / Crew", driverName, { driverName = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Vehicle", vehicle, { vehicle = it }, modifier = Modifier.weight(1f), placeholder = "e.g. Isuzu NPR")
                }
                DesktopFormInput(
                    label = "Load / Packing List",
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    singleLine = false,
                    minLines = 2,
                    placeholder = "20× Gold Chiavari Chair, 2× 60\" Round Table, 1× PA Speaker...",
                )
                DesktopFormInput(
                    label = "Stop Notes (gate code, dock hours, contact...)",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false,
                    minLines = 2,
                    placeholder = "Service elevator access after 08:30; ask for venue manager.",
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = nowMs()
                    onSave(
                        DesktopDeliveryStop(
                            id = initial?.id ?: newId(),
                            type = type,
                            orderId = selectedOrder?.id ?: initial?.orderId,
                            orderNumber = selectedOrder?.number ?: initial?.orderNumber,
                            customerName = customerName.trim(),
                            eventName = eventName.trim(),
                            venue = venue.trim(),
                            contactPhone = contactPhone.trim(),
                            scheduledMs = scheduledMs,
                            driverName = driverName.trim(),
                            vehicle = vehicle.trim(),
                            itemsSummary = itemsSummary.trim(),
                            notes = notes.trim(),
                            status = initial?.status ?: "scheduled",
                            completedAtMs = initial?.completedAtMs,
                            createdAt = initial?.createdAt ?: now,
                        )
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
            ) {
                Text("Save Run", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
