package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.data.local.ShiftEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.CurrencyNumPad
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onBack: (() -> Unit)? = null,
) {
    val sym = data.settings.currencySymbol
    val branchId = session.activeBranchId
    val openShift = remember(data.shifts, branchId) {
        viewModel.currentBranchShift(data, branchId)
    }
    val openStats = remember(openShift, data.receipts, data.cashMovements) {
        openShift?.let { viewModel.shiftStatsFor(it, data) }
    }
    val todaySummary = remember(data.receipts, branchId) {
        viewModel.computeTodaySummary(data, branchId)
    }
    val closedShifts = remember(data.shifts, branchId) {
        data.shifts.filter { !it.isOpen && (branchId == null || it.branchId == null || it.branchId == branchId) }
    }

    var showOpenSheet by remember { mutableStateOf(false) }
    var cashMoveMode by remember { mutableStateOf<String?>(null) } // "in" | "out"
    var showCloseSheet by remember { mutableStateOf(false) }
    var closedSummary by remember { mutableStateOf<Pair<Long, Long>?>(null) } // expected, counted

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = 50.dp,
            title = {
                Text(
                    "Shift & Cash Drawer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            actions = {
                TopSyncIconButton(syncing = session.syncing, onClick = { viewModel.syncCloud() })
            },
        )
        HorizontalDivider(color = LoyBorder)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionHeader("Today's Sales")
                GroupCard {
                    Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBlock(
                                label = "Net sales",
                                value = formatCents(todaySummary.netCents, sym),
                                valueColor = LoyGreen,
                                modifier = Modifier.weight(1f),
                            )
                            StatBlock(
                                label = "Sales / Refunds",
                                value = "${todaySummary.salesCount} / ${todaySummary.refundsCount}",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBlock(
                                label = "Cash",
                                value = formatCents(todaySummary.cashCents, sym),
                                modifier = Modifier.weight(1f),
                            )
                            StatBlock(
                                label = "Card",
                                value = formatCents(todaySummary.cardCents, sym),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("Current Shift")
                if (openShift == null || openStats == null) {
                    GroupCard {
                        Column(modifier = Modifier.padding(11.dp)) {
                            EmptyState(
                                icon = Icons.Outlined.LockOpen,
                                title = "No shift open",
                                subtitle = "Open a shift with your starting cash float to track cash in, cash out, and drawer balance.",
                                actionLabel = "Open shift",
                                onAction = { showOpenSheet = true },
                            )
                        }
                    }
                } else {
                    val expected = openShift.expectedDrawerCents(
                        openStats.cashSalesCents,
                        openStats.cashInCents,
                        openStats.cashOutCents,
                    )
                    GroupCard {
                        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Opened ${formatDateTime(openShift.openedAt)}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                PillBadge("Open")
                            }
                            ShiftRow("Opening float", formatCents(openShift.openingFloatCents, sym))
                            ShiftRow("Cash sales (${openStats.salesCount})", formatCents(openStats.cashSalesCents, sym))
                            ShiftRow("Cash in", "+${formatCents(openStats.cashInCents, sym)}", LoyGreen)
                            ShiftRow("Cash out", "-${formatCents(openStats.cashOutCents, sym)}", LoyError)
                            HorizontalDivider(color = LoyBorder)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Expected cash drawer", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    formatCents(expected, sym),
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                    fontWeight = FontWeight.Bold,
                                    color = LoyGreen,
                                )
                            }
                            ShiftRow("Card sales", formatCents(openStats.cardSalesCents, sym))

                            Spacer(Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { cashMoveMode = "in" },
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("cash_in_button"),
                                ) {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cash in")
                                }
                                OutlinedButton(
                                    onClick = { cashMoveMode = "out" },
                                    modifier = Modifier.weight(1f).height(40.dp).testTag("cash_out_button"),
                                ) {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cash out")
                                }
                            }
                            Button(
                                onClick = { showCloseSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LoyError, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("close_shift_button"),
                            ) {
                                Text("Close shift")
                            }
                        }
                    }

                    if (openStats.movements.isNotEmpty()) {
                        SectionHeader("Drawer Movements")
                        GroupCard {
                            Column {
                                openStats.movements.forEachIndexed { idx, move ->
                                    if (idx > 0) HorizontalDivider(color = LoyBorder)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            if (move.isCashIn) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (move.isCashIn) LoyGreen else LoyError,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(if (move.isCashIn) "Cash in" else "Cash out", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${formatDateTime(move.createdAt)}${if (move.note.isNotEmpty()) " · ${move.note}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LoyTextSecondary,
                                            )
                                        }
                                        Text(
                                            text = "${if (move.isCashIn) "+" else "-"}${formatCents(move.amountCents, sym)}",
                                            style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                            fontWeight = FontWeight.Bold,
                                            color = if (move.isCashIn) LoyGreen else LoyError,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (closedShifts.isNotEmpty()) {
                item { SectionHeader("Previous Shifts") }
                items(closedShifts, key = { it.id }) { shift ->
                    val diff = shift.overShortCents() ?: 0L
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = LoyTextSecondary)
                                    Text(formatDateTime(shift.openedAt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                PillBadge(
                                    text = when {
                                        diff == 0L -> "Even"
                                        diff > 0L -> "Over ${formatCents(diff, sym)}"
                                        else -> "Short ${formatCents(-diff, sym)}"
                                    },
                                    backgroundColor = when {
                                        diff == 0L -> LoyGreenLight
                                        diff > 0L -> LoyGreenLight
                                        else -> LoyErrorLight
                                    },
                                    textColor = if (diff < 0L) LoyError else LoyGreen,
                                )
                            }
                            Text(
                                "Expected ${formatCents(shift.expectedCents ?: 0L, sym)} · Counted ${formatCents(shift.countedCents ?: 0L, sym)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOpenSheet) {
        ShiftAmountSheet(
            title = "Open Shift",
            label = "Opening cash float ($sym)",
            confirmLabel = "Open shift",
            requireNote = false,
            onConfirm = { cents, _ ->
                viewModel.openShift(cents)
                showOpenSheet = false
            },
            onDismiss = { showOpenSheet = false },
        )
    }

    cashMoveMode?.let { mode ->
        openShift?.let { shift ->
            ShiftAmountSheet(
                title = if (mode == "in") "Cash In" else "Cash Out",
                label = "Amount ($sym)",
                confirmLabel = if (mode == "in") "Record Cash In" else "Record Cash Out",
                requireNote = true,
                onConfirm = { cents, note ->
                    if (cents > 0L) viewModel.addCashMovement(shift.id, mode, cents, note)
                    cashMoveMode = null
                },
                onDismiss = { cashMoveMode = null },
            )
        }
    }

    if (showCloseSheet && openShift != null && openStats != null) {
        val expected = openShift.expectedDrawerCents(
            openStats.cashSalesCents,
            openStats.cashInCents,
            openStats.cashOutCents,
        )
        ShiftAmountSheet(
            title = "Close Shift",
            label = "Counted drawer cash ($sym)",
            subtitle = "Expected in drawer: ${formatCents(expected, sym)}",
            confirmLabel = "Close shift",
            requireNote = false,
            initialText = if (expected % 100L == 0L) (expected / 100L).toString() else "%.2f".format(expected / 100.0),
            onConfirm = { countedCents, _ ->
                viewModel.closeShift(openShift.id, countedCents, expected) {
                    showCloseSheet = false
                    closedSummary = expected to countedCents
                }
            },
            onDismiss = { showCloseSheet = false },
        )
    }

    closedSummary?.let { (expected, counted) ->
        val diff = counted - expected
        AlertDialog(
            onDismissRequest = { closedSummary = null },
            title = { Text("Shift Closed", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Expected: ${formatCents(expected, sym)}")
                    Text("Counted: ${formatCents(counted, sym)}")
                    Text(
                        text = when {
                            diff == 0L -> "Drawer is even."
                            diff > 0L -> "Over by ${formatCents(diff, sym)}"
                            else -> "Short by ${formatCents(-diff, sym)}"
                        },
                        color = if (diff < 0L) LoyError else LoyGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { closedSummary = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Done")
                }
            },
        )
    }
}

@Composable
private fun ShiftRow(label: String, value: String, valueColor: Color = LoyTextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LoyTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = TabularMoneyStyle, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftAmountSheet(
    title: String,
    label: String,
    confirmLabel: String,
    requireNote: Boolean,
    subtitle: String? = null,
    initialText: String = "",
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(initialText) }
    var note by remember { mutableStateOf("") }

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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
            }
            Surface(
                color = LoySurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                    Text(
                        amountText.ifEmpty { "0" },
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (requireNote) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Reason / note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CurrencyNumPad(
                onKey = { d ->
                    if (d == "." && amountText.contains(".")) return@CurrencyNumPad
                    amountText += d
                },
                onBackspace = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) },
                onClear = { amountText = "" },
                allowDecimal = true,
            )
            Button(
                onClick = {
                    val cents = parseAmountToCents(amountText) ?: 0L
                    onConfirm(cents, note)
                },
                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("shift_sheet_confirm"),
            ) {
                Text(confirmLabel)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
