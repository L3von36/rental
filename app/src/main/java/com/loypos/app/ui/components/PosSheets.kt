package com.loypos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loypos.app.core.DiscountInput
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.parseQty
import com.loypos.app.data.local.BranchEntity
import com.loypos.app.data.local.CartLine
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.TicketWithLines
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.TabularMoneyStyle
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineEditorBottomSheet(
    line: CartLine,
    currencySymbol: String,
    onUpdateQty: (Double) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var qtyText by remember(line.itemId) { mutableStateOf(formatQty(line.qty)) }
    val parsedQty = parseQty(qtyText) ?: 0.0
    val days = line.rentalDays.coerceAtLeast(1)
    val lineTotal = (line.priceCents * parsedQty * days).roundToLong()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(line.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatCents(line.priceCents, currencySymbol)}/day × ${days}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                IconButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("line_editor_delete"),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove item", tint = LoyError, modifier = Modifier.size(20.dp))
                }
            }

            Surface(
                color = LoySurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = {
                            val next = (parsedQty - 1.0).coerceAtLeast(0.0)
                            qtyText = formatQty(next)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("line_editor_minus"),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(18.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (qtyText.isEmpty()) "0" else qtyText,
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Line total: ${formatCents(lineTotal, currencySymbol)}",
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                            color = LoyGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(
                        onClick = {
                            qtyText = formatQty(parsedQty + 1.0)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("line_editor_plus"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase quantity", modifier = Modifier.size(18.dp))
                    }
                }
            }

            CurrencyNumPad(
                onKey = { digit ->
                    if (digit == "." && qtyText.contains(".")) return@CurrencyNumPad
                    qtyText = if (qtyText == "0" && digit != ".") digit else qtyText + digit
                },
                onBackspace = {
                    qtyText = if (qtyText.length <= 1) "0" else qtyText.dropLast(1)
                },
                onClear = { qtyText = "0" },
                allowDecimal = true,
            )

            Button(
                onClick = {
                    if (parsedQty <= 0.0) onDelete() else onUpdateQty(parsedQty)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("line_editor_save"),
            ) {
                Text("Apply quantity")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountBottomSheet(
    current: DiscountInput?,
    currencySymbol: String,
    onApply: (DiscountInput?) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(current?.type ?: "percent") }
    var valueText by remember {
        mutableStateOf(current?.let { formatQty(it.value) } ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rental Discount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (current != null) {
                    TextButton(
                        onClick = {
                            onApply(null)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("discount_clear"),
                    ) {
                        Text("Remove", color = LoyError)
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == "percent",
                    onClick = { mode = "percent" },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Percentage (%)", style = MaterialTheme.typography.labelLarge) }
                SegmentedButton(
                    selected = mode == "amount",
                    onClick = { mode = "amount" },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Fixed ($currencySymbol)", style = MaterialTheme.typography.labelLarge) }
            }

            if (mode == "percent") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(5, 10, 15, 20, 25).forEach { pct ->
                        FilterChip(
                            selected = valueText == pct.toString(),
                            onClick = { valueText = pct.toString() },
                            label = { Text("$pct%", style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f).height(30.dp),
                        )
                    }
                }
            }

            Surface(
                color = LoySurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (mode == "percent") "${valueText.ifEmpty { "0" }}%" else "$currencySymbol${valueText.ifEmpty { "0" }}",
                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp),
                )
            }

            CurrencyNumPad(
                onKey = { d ->
                    if (d == "." && valueText.contains(".")) return@CurrencyNumPad
                    valueText += d
                },
                onBackspace = { if (valueText.isNotEmpty()) valueText = valueText.dropLast(1) },
                onClear = { valueText = "" },
                allowDecimal = true,
            )

            Button(
                onClick = {
                    val v = parseQty(valueText) ?: 0.0
                    if (v <= 0.0) {
                        onApply(null)
                    } else {
                        val clamped = if (mode == "percent") v.coerceAtMost(100.0) else v
                        onApply(DiscountInput(mode, clamped))
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("discount_apply"),
            ) {
                Text("Apply discount")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPickerSheet(
    customers: List<CustomerEntity>,
    selectedId: String?,
    onSelect: (CustomerEntity?) -> Unit,
    onCreateCustomer: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }

    val filtered = remember(customers, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) customers else customers.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Client / Organizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    if (selectedId != null) {
                        TextButton(onClick = {
                            onSelect(null)
                            onDismiss()
                        }) {
                            Text("Clear", color = LoyError)
                        }
                    }
                    TextButton(onClick = { showCreate = !showCreate }) {
                        Text(if (showCreate) "List" else "+ New")
                    }
                }
            }

            if (showCreate) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Client / company name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (newName.trim().isNotEmpty()) {
                            onCreateCustomer(newName.trim(), newPhone.trim())
                            onDismiss()
                        }
                    },
                    enabled = newName.trim().isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                ) {
                    Text("Save & Attach Client")
                }
            } else {
                CompactSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Search clients & organizers",
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                ) {
                    items(filtered, key = { it.id }) { cust ->
                        val isSel = cust.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .clickable {
                                    onSelect(cust)
                                    onDismiss()
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                tint = if (isSel) LoyGreen else LoyTextSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cust.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (cust.phone.isNotEmpty()) {
                                    Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                            }
                            PillBadge(text = "${cust.points} pts")
                            if (isSel) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = LoyGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                        HorizontalDivider(color = LoyBorder)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsBottomSheet(
    tickets: List<TicketWithLines>,
    currencySymbol: String,
    onLoad: (TicketWithLines) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Held Quotes & Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (tickets.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "No held quotes",
                    subtitle = "Save a rental cart to hold an event quote here.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(tickets, key = { it.ticket.id }) { t ->
                        val days = t.ticket.rentalDays.coerceAtLeast(1)
                        val total = t.lines.sumOf { (it.priceCents * it.qty * days).roundToLong() }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLoad(t)
                                    onDismiss()
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        t.ticket.name.ifEmpty { "Saved quote" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val eventSuffix = if (t.ticket.eventName.isNotBlank()) " · ${t.ticket.eventName}" else ""
                                    Text(
                                        "${t.lines.size} items · ${days}d$eventSuffix · ${formatDateTime(t.ticket.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                    )
                                }
                                Text(
                                    formatCents(total, currencySymbol),
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(onClick = { onDelete(t.ticket.id) }, modifier = Modifier.size(38.dp)) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ticket", tint = LoyError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchPickerSheet(
    branches: List<BranchEntity>,
    activeBranchId: String?,
    onSelect: (BranchEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Select Branch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            branches.forEach { branch ->
                val selected = branch.id == activeBranchId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) LoyGreenLight else Color.Transparent,
                    border = BorderStroke(1.dp, if (selected) LoyGreen else LoyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(branch)
                            onDismiss()
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = if (selected) LoyGreen else LoyTextSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(branch.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LoyTextPrimary)
                            if (branch.address.isNotEmpty()) {
                                Text(branch.address, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Outlined.Check, contentDescription = "Active branch", tint = LoyGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
