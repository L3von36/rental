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
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot

@Composable
fun RentalOrdersView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var filterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Out, 2=Overdue, 3=Returned, 4=Refunded
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var confirmReturnId by remember { mutableStateOf<String?>(null) }
    var confirmRefundId by remember { mutableStateOf<String?>(null) }
    var previewReceiptOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    var agreementOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }

    val now = remember(snapshot.orders) { nowMs() }
    val filtered = remember(snapshot.orders, search, filterIdx, now) {
        val q = search.trim().lowercase()
        snapshot.orders.filter { o ->
            val matchesSearch = q.isEmpty() ||
                "#${o.number}".contains(q) ||
                o.number.toString().padStart(4, '0').contains(q) ||
                (o.customerName?.lowercase()?.contains(q) == true) ||
                o.eventName.lowercase().contains(q) ||
                o.eventVenue.lowercase().contains(q)
            val matchesStatus = when (filterIdx) {
                1 -> o.isRentalOut
                2 -> o.isOverdue(now)
                3 -> o.isReturned
                4 -> !o.isCompleted
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    val activeOrder = remember(filtered, selectedOrderId) {
        filtered.firstOrNull { it.id == selectedOrderId } ?: filtered.firstOrNull()
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Column(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search #order, client, event, or venue") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val tabs = listOf(
                            "All (${snapshot.orders.size})",
                            "Out (${snapshot.orders.count { it.isRentalOut }})",
                            "Overdue (${snapshot.orders.count { it.isOverdue(now) }})",
                            "Returned (${snapshot.orders.count { it.isReturned }})",
                            "Refunded (${snapshot.orders.count { !it.isCompleted }})",
                        )
                        tabs.forEachIndexed { idx, label ->
                            FilterChip(
                                selected = filterIdx == idx,
                                onClick = { filterIdx = idx },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (idx == 2) LoyWarningLight else LoyGreenLight,
                                    selectedLabelColor = if (idx == 2) LoyWarning else LoyGreenDark,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(34.dp))
                        Text("No rental orders found", style = MaterialTheme.typography.titleSmall, color = LoyTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { o ->
                        val selected = activeOrder?.id == o.id
                        val overdue = o.isOverdue(now)
                        val statusLabel = when {
                            !o.isCompleted -> "Refunded"
                            o.isReturned -> "Returned"
                            overdue -> "Overdue"
                            else -> "Out"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) LoyGreenLight.copy(alpha = 0.5f) else LoySurface,
                            border = BorderStroke(1.dp, if (selected) LoyGreen else LoyBorder),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                selectedOrderId = o.id
                            },
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${o.number.toString().padStart(4, '0')} · ${o.rentalDays}d", fontWeight = FontWeight.Bold)
                                        StatusBadge(statusLabel, o.isCompleted, o.isReturned, overdue)
                                    }
                                    val sub = listOfNotNull(
                                        o.customerName?.takeIf { it.isNotEmpty() },
                                        o.eventName.takeIf { it.isNotEmpty() },
                                    ).joinToString(" · ")
                                    Text(
                                        if (sub.isNotEmpty()) "$sub · Due ${formatShortDate(o.rentalEndMs)}" else "Due ${formatShortDate(o.rentalEndMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overdue) LoyWarning else LoyTextSecondary,
                                    )
                                }
                                Text(
                                    formatCents(o.totalCents, sym),
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Detail Inspector
        Surface(
            color = LoySurface,
            border = BorderStroke(1.dp, LoyBorder),
            modifier = Modifier.weight(0.52f).fillMaxHeight(),
        ) {
            val order = activeOrder
            if (order == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a rental order to inspect dispatch, equipment, and returns.", color = LoyTextSecondary)
                }
            } else {
                val overdue = order.isOverdue(now)
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Rental #${order.number.toString().padStart(4, '0')}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Booked ${formatDateTime(order.createdAt)}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                        Text(
                            formatCents(order.totalCents, sym),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                            color = LoyGreenDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoyBackground,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Duration: ${order.rentalDays} ${if (order.rentalDays == 1) "day" else "days"} · Return Due ${formatShortDate(order.rentalEndMs)}", fontWeight = FontWeight.SemiBold, color = if (overdue) LoyWarning else LoyTextPrimary)
                            order.returnedAtMs?.let { ret ->
                                Text("Returned: ${formatDateTime(ret)}", color = LoyGreenDark, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!order.customerName.isNullOrEmpty()) Text("Client: ${order.customerName}", style = MaterialTheme.typography.bodySmall)
                            if (order.eventName.isNotEmpty()) Text("Event: ${order.eventName}", style = MaterialTheme.typography.bodySmall)
                            if (order.eventVenue.isNotEmpty()) Text("Venue: ${order.eventVenue}", style = MaterialTheme.typography.bodySmall)
                            if (order.depositCents > 0L) Text("Refundable Security Deposit: ${formatCents(order.depositCents, sym)}", style = MaterialTheme.typography.bodySmall, color = LoyInfo)
                            if (order.note.isNotEmpty()) Text("Note: ${order.note}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                    }

                    Text("Rented Equipment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoyBackground,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            order.lines.forEachIndexed { idx, line ->
                                if (idx > 0) HorizontalDivider(color = LoyBorder)
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.name, fontWeight = FontWeight.Medium)
                                        Text("${formatQty(line.qty)} × ${formatCents(line.priceCents, sym)}/d × ${order.rentalDays}d", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                    Text(formatCents(line.lineTotalCents, sym), style = TabularMoneyStyle, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (order.isCompleted && !order.isReturned) {
                        Button(
                            onClick = { confirmReturnId = order.id },
                            colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mark Equipment Returned (Restore Fleet)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { previewReceiptOrder = order },
                            modifier = Modifier.weight(1f).height(42.dp),
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Receipt Copy")
                        }
                        OutlinedButton(
                            onClick = { agreementOrder = order },
                            modifier = Modifier.weight(1f).height(42.dp),
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Agreement")
                        }
                        if (order.isCompleted) {
                            OutlinedButton(
                                onClick = { confirmRefundId = order.id },
                                modifier = Modifier.weight(1f).height(42.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = LoyError)
                                Spacer(Modifier.width(6.dp))
                                Text("Refund", color = LoyError)
                            }
                        }
                    }
                }
            }
        }
    }

    confirmReturnId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmReturnId = null },
            title = { Text("Confirm Equipment Return?") },
            text = { Text("All rented tools on this order will be restored back to available depot inventory.") },
            confirmButton = {
                Button(onClick = {
                    if (repository.markRentalReturned(id)) onToast("Equipment returned and fleet stock restored")
                    confirmReturnId = null
                }) { Text("Mark Returned") }
            },
            dismissButton = { TextButton(onClick = { confirmReturnId = null }) { Text("Cancel") } },
        )
    }

    confirmRefundId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmRefundId = null },
            title = { Text("Refund Rental Booking?") },
            text = { Text("Marks this rental refunded and restores equipment if it has not already been returned.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (repository.refundRental(id)) onToast("Rental booking refunded")
                        confirmRefundId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoyError),
                ) { Text("Refund") }
            },
            dismissButton = { TextButton(onClick = { confirmRefundId = null }) { Text("Cancel") } },
        )
    }

    previewReceiptOrder?.let { order ->
        val txt = remember(order, snapshot.settings) { repository.buildPlainTextReceipt(order, snapshot.settings) }
        AlertDialog(
            onDismissRequest = { previewReceiptOrder = null },
            title = { Text("Receipt Copy · #${order.number.toString().padStart(4, '0')}") },
            text = {
                Surface(color = LoyBackground, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, LoyBorder)) {
                    Text(txt, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = { previewReceiptOrder = null }) { Text("Close") }
            },
        )
    }

    agreementOrder?.let { order ->
        DesktopPopulatedAgreementDialog(
            order = order,
            snapshot = snapshot,
            onToast = onToast,
            onDismiss = { agreementOrder = null },
        )
    }
}

@Composable
private fun StatusBadge(label: String, isCompleted: Boolean, isReturned: Boolean, overdue: Boolean) {
    val (bg, fg) = when {
        !isCompleted -> LoyErrorLight to LoyError
        isReturned -> LoyGreenLight to LoyGreenDark
        overdue -> LoyWarningLight to LoyWarning
        else -> LoyInfoLight to LoyInfo
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
