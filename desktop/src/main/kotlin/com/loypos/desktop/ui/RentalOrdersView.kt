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
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.DesktopRentalReturnReminderCalculator
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun RentalOrdersView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    initialFilterIdx: Int? = null,
    onConsumeInitialFilter: () -> Unit = {},
    onReportDamage: (DesktopRentalOrder) -> Unit = {},
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    // 0=All, 1=Out, 2=Due <24h, 3=Overdue, 4=Returned, 5=Refunded
    var filterIdx by remember { mutableIntStateOf(0) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var confirmReturnId by remember { mutableStateOf<String?>(null) }
    var confirmRefundId by remember { mutableStateOf<String?>(null) }
    var previewReceiptOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    var agreementOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }

    LaunchedEffect(initialFilterIdx) {
        if (initialFilterIdx != null) {
            filterIdx = initialFilterIdx
            onConsumeInitialFilter()
        }
    }

    val now = remember(snapshot.orders) { nowMs() }
    val due24hIds = remember(snapshot.orders, now) {
        DesktopRentalReturnReminderCalculator.findRentalsDueWithin24Hours(snapshot.orders, now)
            .map { it.order.id }
            .toSet()
    }

    val filtered = remember(snapshot.orders, search, filterIdx, now, due24hIds) {
        val q = search.trim().lowercase()
        snapshot.orders.filter { o ->
            val matchesSearch = q.isEmpty() ||
                "#${o.number}".contains(q) ||
                o.number.toString().padStart(4, '0').contains(q) ||
                (o.customerName?.lowercase()?.contains(q) == true) ||
                o.eventName.lowercase().contains(q) ||
                o.eventVenue.lowercase().contains(q) ||
                o.lines.any { it.name.lowercase().contains(q) }
            val matchesStatus = when (filterIdx) {
                1 -> o.isRentalOut
                2 -> o.id in due24hIds
                3 -> o.isOverdue(now)
                4 -> o.isReturned
                5 -> !o.isCompleted
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    val activeOrder = remember(filtered, selectedOrderId) {
        filtered.firstOrNull { it.id == selectedOrderId } ?: filtered.firstOrNull()
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Master Table (56%)
        Column(modifier = Modifier.weight(0.56f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopSearchInput(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = "Search #order, customer, event, venue, or gear...",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopSecondaryButton(
                            text = "Export CSV",
                            icon = Icons.Outlined.TableView,
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    selectedFile = File("loypos-rental-history.csv")
                                    fileFilter = FileNameExtensionFilter("CSV Ledger (*.csv)", "csv")
                                }
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    val count = repository.exportRentalHistoryCsv(chooser.selectedFile)
                                    onToast("Exported $count rental history rows to ${chooser.selectedFile.name}")
                                }
                            },
                        )
                        DesktopSecondaryButton(
                            text = "Export Report",
                            icon = Icons.Outlined.Download,
                            onClick = {
                                val chooser = JFileChooser().apply {
                                    selectedFile = File("loypos-accounting-statement.txt")
                                    fileFilter = FileNameExtensionFilter("Accounting Report (*.txt, *.pdf)", "txt", "pdf")
                                }
                                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                    repository.exportRentalHistoryReportPdf(chooser.selectedFile)
                                    onToast("Exported accounting statement to ${chooser.selectedFile.name}")
                                }
                            },
                        )
                    }
                    DesktopSegmentedControl(
                        options = listOf(
                            0 to "All (${snapshot.orders.size})",
                            1 to "Out (${snapshot.orders.count { it.isRentalOut }})",
                            2 to "Due <24h (${due24hIds.size})",
                            3 to "Overdue (${snapshot.orders.count { it.isOverdue(now) }})",
                            4 to "Returned (${snapshot.orders.count { it.isReturned }})",
                            5 to "Refunded (${snapshot.orders.count { !it.isCompleted }})",
                        ),
                        selected = filterIdx,
                        onSelect = { filterIdx = it },
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (filtered.isEmpty()) {
                DesktopEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "No matching rental orders",
                    subtitle = if (filterIdx == 2) "No active rentals are due within the next 24 hours." else "Adjust your search or status filter.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                DesktopTableHeaderRow {
                    DesktopTableHeaderText("Order", Modifier.width(74.dp))
                    DesktopTableHeaderText("Client & Event", Modifier.weight(1.4f))
                    DesktopTableHeaderText("Return Due", Modifier.weight(1.0f))
                    DesktopTableHeaderText("Status", Modifier.width(88.dp))
                    DesktopTableHeaderText("Total", Modifier.width(92.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                    items(filtered, key = { it.id }) { o ->
                        val selected = activeOrder?.id == o.id
                        val overdue = o.isOverdue(now)
                        val isDue24h = o.id in due24hIds && !overdue
                        val statusLabel = when {
                            !o.isCompleted -> "Refunded"
                            o.isReturned -> "Returned"
                            overdue -> "Overdue"
                            isDue24h -> "Due <24h"
                            else -> "Out"
                        }

                        DesktopTableRow(
                            selected = selected,
                            onClick = { selectedOrderId = o.id },
                        ) {
                            Column(modifier = Modifier.width(74.dp)) {
                                Text(
                                    text = "#${o.number.toString().padStart(4, '0')}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = LoyGreenDark,
                                )
                                Text(
                                    text = "${o.rentalDays}d · ${o.lines.size} ln",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextMuted,
                                )
                            }
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(
                                    text = o.customerName?.takeIf { it.isNotBlank() } ?: "Walk-in Client",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = listOfNotNull(
                                        o.eventName.takeIf { it.isNotBlank() },
                                        o.eventVenue.takeIf { it.isNotBlank() },
                                    ).joinToString(" @ ").ifBlank { "Booked ${formatShortDate(o.createdAt)}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = formatShortDate(o.rentalEndMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (overdue) LoyError else if (isDue24h) LoyWarning else LoyTextPrimary,
                                )
                                if (o.isRentalOut) {
                                    Text(
                                        text = DesktopRentalReturnReminderCalculator.formatRemainingLabel(o.rentalEndMs - now),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overdue) LoyError else if (isDue24h) LoyWarning else LoyTextMuted,
                                    )
                                }
                            }
                            Box(modifier = Modifier.width(88.dp)) {
                                OrderStatusBadge(statusLabel, o.isCompleted, o.isReturned, overdue, isDue24h)
                            }
                            Text(
                                text = formatCents(o.totalCents, sym),
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                fontWeight = FontWeight.Bold,
                                color = if (!o.isCompleted) LoyError else LoyTextPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(92.dp),
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Order Inspector Dock (44%)
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight()
                .background(LoySurface),
        ) {
            val order = activeOrder
            if (order == null) {
                DesktopEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "No Order Selected",
                    subtitle = "Select any rental order row to inspect line items, returns, and agreements.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                DesktopRentalOrderInspectorContent(
                    order = order,
                    repository = repository,
                    snapshot = snapshot,
                    nowMs = now,
                    isDue24h = order.id in due24hIds && !order.isOverdue(now),
                    onReportDamage = onReportDamage,
                    onToast = onToast,
                )
            }
        }
    }
}

@Composable
internal fun DesktopRentalOrderInspectorContent(
    order: DesktopRentalOrder,
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    nowMs: Long = nowMs(),
    isDue24h: Boolean = false,
    onReportDamage: (DesktopRentalOrder) -> Unit = {},
    onToast: (String) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val sym = snapshot.settings.currencySymbol
    var confirmReturnId by remember { mutableStateOf<String?>(null) }
    var confirmRefundId by remember { mutableStateOf<String?>(null) }
    var previewReceiptOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }
    var agreementOrder by remember { mutableStateOf<DesktopRentalOrder?>(null) }

    val overdue = order.isOverdue(nowMs)
    val inDue24hWindow = isDue24h || (order.isRentalOut && (order.rentalEndMs - nowMs) <= 24L * 60L * 60L * 1000L)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Order #${order.number.toString().padStart(4, '0')}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    val badgeLabel = when {
                        !order.isCompleted -> "Refunded"
                        order.isReturned -> "Returned"
                        overdue -> "Overdue"
                        inDue24hWindow -> "Due <24h"
                        else -> "Active Out"
                    }
                    OrderStatusBadge(badgeLabel, order.isCompleted, order.isReturned, overdue, inDue24hWindow)
                }
                Text(
                    text = "Created ${formatDateTime(order.createdAt)} · ${order.paymentType.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }
            Text(
                text = formatCents(order.totalCents, sym),
                style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                color = LoyGreenDark,
                fontWeight = FontWeight.Bold,
            )
        }

        // 24h Return Alert Banner inside Inspector when applicable
        if (inDue24hWindow) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (overdue) LoyErrorLight else LoyWarningLight,
                border = BorderStroke(1.dp, (if (overdue) LoyError else LoyWarning).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = if (overdue) LoyError else LoyWarning,
                        modifier = Modifier.size(16.dp),
                    )
                    Column {
                        Text(
                            text = if (overdue) "Return Overdue" else "24-Hour Return Reminder Window",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (overdue) LoyError else LoyWarning,
                        )
                        Text(
                            text = "${DesktopRentalReturnReminderCalculator.formatRemainingLabel(order.rentalEndMs - nowMs)} · Scheduled return ${formatShortDate(order.rentalEndMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }
                }
            }
        }

        // Structured Metadata Card
        DesktopSectionCard(title = "Dispatch & Return Details") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InspectorField("Pickup Date", formatShortDate(order.rentalStartMs), Modifier.weight(1f))
                InspectorField("Return Due", "${formatShortDate(order.rentalEndMs)} (${order.rentalDays}d)", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InspectorField("Client", order.customerName?.ifBlank { "Walk-in" } ?: "Walk-in", Modifier.weight(1f))
                InspectorField("Event", order.eventName.ifBlank { "—" }, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InspectorField("Venue", order.eventVenue.ifBlank { "—" }, Modifier.weight(1f))
                InspectorField(
                    "Security Deposit",
                    if (order.depositCents > 0L) formatCents(order.depositCents, sym) else "None",
                    Modifier.weight(1f),
                )
            }
            order.returnedAtMs?.let { ret ->
                InspectorField("Returned At", formatDateTime(ret), Modifier.fillMaxWidth())
            }
            if (order.note.isNotBlank()) {
                InspectorField("Dispatch Note", order.note, Modifier.fillMaxWidth())
            }
        }

        // Line items table
        DesktopSectionCard(
            title = "Rented Equipment (${order.lines.size})",
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            DesktopTableHeaderRow {
                DesktopTableHeaderText("Equipment", Modifier.weight(1.4f))
                DesktopTableHeaderText("Rate × Qty", Modifier.weight(1f))
                DesktopTableHeaderText("Total", Modifier.width(84.dp))
            }
            order.lines.forEach { line ->
                DesktopTableRow {
                    Text(
                        text = line.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.4f),
                    )
                    Text(
                        text = "${formatQty(line.qty)} × ${formatCents(line.priceCents, sym)} × ${order.rentalDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatCents(line.lineTotalCents, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(84.dp),
                    )
                }
            }
        }

        // Primary Actions
        if (order.isCompleted && !order.isReturned) {
            DesktopPrimaryButton(
                text = "Mark Equipment Returned (Restore Fleet)",
                icon = Icons.AutoMirrored.Outlined.AssignmentReturn,
                onClick = { confirmReturnId = order.id },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DesktopSecondaryButton(
                text = "Receipt",
                icon = Icons.Outlined.Print,
                onClick = { previewReceiptOrder = order },
                modifier = Modifier.weight(1f),
            )
            DesktopSecondaryButton(
                text = "Agreement",
                icon = Icons.Outlined.Description,
                onClick = { agreementOrder = order },
                modifier = Modifier.weight(1f),
            )
            if (order.isCompleted) {
                DesktopSecondaryButton(
                    text = "Refund",
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    contentColor = LoyError,
                    onClick = { confirmRefundId = order.id },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DesktopSecondaryButton(
            text = "Document Equipment Damage",
            icon = Icons.Outlined.ReportProblem,
            contentColor = LoyWarning,
            onClick = { onReportDamage(order) },
            modifier = Modifier.fillMaxWidth(),
        )
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

    previewReceiptOrder?.let { rec ->
        val txt = remember(rec, snapshot.settings) { repository.buildPlainTextReceipt(rec, snapshot.settings) }
        AlertDialog(
            onDismissRequest = { previewReceiptOrder = null },
            title = { Text("Receipt Copy · #${rec.number.toString().padStart(4, '0')}") },
            text = {
                Surface(color = LoyBackground, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, LoyBorder)) {
                    Text(txt, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            },
            confirmButton = { Button(onClick = { previewReceiptOrder = null }) { Text("Close") } },
        )
    }

    agreementOrder?.let { agr ->
        DesktopPopulatedAgreementDialog(
            order = agr,
            snapshot = snapshot,
            onToast = onToast,
            onDismiss = { agreementOrder = null },
        )
    }
}

@Composable
private fun InspectorField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LoyTextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = LoyTextPrimary,
        )
    }
}

@Composable
private fun OrderStatusBadge(
    label: String,
    isCompleted: Boolean,
    isReturned: Boolean,
    overdue: Boolean,
    isDue24h: Boolean,
) {
    val (bg, fg) = when {
        !isCompleted -> LoyErrorLight to LoyError
        isReturned -> LoyGreenLight to LoyGreenDark
        overdue -> LoyErrorLight to LoyError
        isDue24h -> LoyWarningLight to LoyWarning
        else -> LoyInfoLight to LoyInfo
    }
    DesktopBadge(text = label, bgColor = bg, fgColor = fg)
}
