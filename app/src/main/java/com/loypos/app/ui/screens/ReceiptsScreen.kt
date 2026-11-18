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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatQty
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.nowMs
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.ConfirmDialog
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.ReceiptPdfPreviewSheet
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
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
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onOpenReceipt: (String) -> Unit,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToQrTracking: () -> Unit = {},
) {
    var search by remember { mutableStateOf("") }
    var statusFilterIdx by remember { mutableIntStateOf(0) } // 0=All, 1=Out, 2=Overdue, 3=Returned, 4=Refunded
    var selectedReceiptId by remember { mutableStateOf<String?>(null) }
    var showPdfPreview by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }
    var showRefundDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }

    val sym = data.settings.currencySymbol
    val windowSize = LocalLoyWindowSize.current
    val metrics = LocalLoyMetrics.current
    val currentTimeMs = remember(data.receipts) { nowMs() }

    val filtered = remember(data.receipts, search, statusFilterIdx, currentTimeMs) {
        val q = search.trim().lowercase()
        data.receipts.filter { item ->
            val r = item.receipt
            val matchesSearch = q.isEmpty() ||
                "#${r.number}".contains(q) ||
                r.number.toString().padStart(4, '0').contains(q) ||
                (r.customerName?.lowercase()?.contains(q) == true) ||
                r.eventName.lowercase().contains(q) ||
                r.eventVenue.lowercase().contains(q)
            val matchesFilter = when (statusFilterIdx) {
                1 -> r.isRentalOut
                2 -> r.isOverdue(currentTimeMs)
                3 -> r.isReturned
                4 -> !r.isCompleted
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val activeDetail = remember(filtered, selectedReceiptId) {
        filtered.firstOrNull { it.receipt.id == selectedReceiptId } ?: filtered.firstOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            title = { Text("Rentals & Returns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            actions = {
                IconButton(
                    onClick = onNavigateToQrTracking,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("receipts_qr_button"),
                ) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = "QR Check-In & Scan",
                        tint = LoyGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onNavigateToSchedule,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("receipts_schedule_button"),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Rental Calendar",
                        tint = LoyGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
                TopSyncIconButton(syncing = session.syncing, onClick = { viewModel.syncCloud() })
            },
        )
        HorizontalDivider(color = LoyBorder)

        val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
            Column(modifier = paneModifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoySurface)
                        .padding(horizontal = metrics.screenPadding, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CompactSearchField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search #number, client, or event",
                        inputTestTag = "receipts_search_input",
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val tabs = listOf(
                            "All (${data.receipts.size})",
                            "Out (${data.receipts.count { it.receipt.isRentalOut }})",
                            "Overdue (${data.receipts.count { it.receipt.isOverdue(currentTimeMs) }})",
                            "Returned (${data.receipts.count { it.receipt.isReturned }})",
                            "Refunded (${data.receipts.count { !it.receipt.isCompleted }})",
                        )
                        tabs.forEachIndexed { idx, label ->
                            item {
                                FilterChip(
                                    selected = statusFilterIdx == idx,
                                    onClick = { statusFilterIdx = idx },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (idx == 2) Color(0xFFFFF3E0) else LoyGreenLight,
                                        selectedLabelColor = if (idx == 2) LoyWarning else LoyGreen,
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
                            icon = Icons.Outlined.ReceiptLong,
                            title = "No rental orders found",
                            subtitle = "Completed rentals will appear here for dispatch and return tracking.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(metrics.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                    ) {
                        items(filtered, key = { it.receipt.id }) { item ->
                            val r = item.receipt
                            val isSelected = windowSize.useSplitPane && activeDetail?.receipt?.id == r.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoySurface,
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) LoyGreen else LoyBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (windowSize.useSplitPane) {
                                            selectedReceiptId = r.id
                                        } else {
                                            onOpenReceipt(r.id)
                                        }
                                    }
                                    .testTag("receipt_row_${r.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(metrics.rowHorizontalPadding, metrics.rowVerticalPadding + 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (r.isCompleted) LoyGreenLight else LoyErrorLight),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (r.paymentType == "card") Icons.Outlined.CreditCard else Icons.Outlined.Payments,
                                            contentDescription = r.paymentType,
                                            tint = if (r.isCompleted) LoyGreen else LoyError,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "#${r.number.toString().padStart(4, '0')}",
                                                fontWeight = FontWeight.Bold,
                                                color = LoyTextPrimary,
                                            )
                                            PillBadge(text = "${r.rentalDays}d", backgroundColor = LoyInfoLight, textColor = LoyInfo)
                                            when {
                                                !r.isCompleted -> PillBadge(text = "Refunded", backgroundColor = LoyErrorLight, textColor = LoyError)
                                                r.isReturned -> PillBadge(text = "Returned", backgroundColor = LoyGreenLight, textColor = LoyGreen)
                                                r.isOverdue(currentTimeMs) -> PillBadge(text = "Overdue", backgroundColor = Color(0xFFFFF3E0), textColor = LoyWarning)
                                                else -> PillBadge(text = "Out", backgroundColor = LoyInfoLight, textColor = LoyInfo)
                                            }
                                        }
                                        Text(
                                            buildString {
                                                append(formatDateTime(r.createdAt))
                                                if (!r.customerName.isNullOrEmpty()) append(" · ${r.customerName}")
                                                if (r.eventName.isNotEmpty()) append(" · ${r.eventName}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    Text(
                                        text = formatCents(r.totalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                        fontWeight = FontWeight.Bold,
                                        color = if (r.isCompleted) LoyTextPrimary else LoyError,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (windowSize.useSplitPane) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                listPane(Modifier.weight(0.45f))
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                Box(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                    if (activeDetail == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(
                                icon = Icons.Outlined.ReceiptLong,
                                title = "Select a receipt",
                                subtitle = "Choose a transaction on the left to inspect line items, PDF receipt, or refund.",
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Surface(color = LoySurface) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = metrics.screenPadding, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Receipt #${activeDetail.receipt.number.toString().padStart(4, '0')}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    IconButton(
                                        onClick = { showPdfPreview = true },
                                        modifier = Modifier
                                            .accessibleTouchTarget(metrics.minTouchTarget)
                                            .testTag("receipt_detail_top_pdf_preview"),
                                    ) {
                                        Icon(
                                            Icons.Outlined.PictureAsPdf,
                                            contentDescription = "Preview Receipt PDF",
                                            tint = LoyGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = LoyBorder)
                            ReceiptDetailContent(
                                receiptWithLines = activeDetail,
                                data = data,
                                onShareText = { viewModel.shareReceiptText(activeDetail) },
                                onPreviewPdf = { showPdfPreview = true },
                                onOpenAgreement = { showAgreementDialog = true },
                                onMarkReturned = { showReturnDialog = true },
                                onRequestRefund = { showRefundDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(metrics.screenPadding),
                            )
                        }
                    }
                }
            }
        } else {
            listPane(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (showPdfPreview && activeDetail != null) {
        ReceiptPdfPreviewSheet(
            viewModel = viewModel,
            receiptWithLines = activeDetail,
            onDismiss = { showPdfPreview = false },
        )
    }

    if (showAgreementDialog && activeDetail != null) {
        PopulatedAgreementDialog(
            viewModel = viewModel,
            receiptWithLines = activeDetail,
            data = data,
            onDismiss = { showAgreementDialog = false },
        )
    }

    if (showReturnDialog && activeDetail != null) {
        val activeReceipt = activeDetail.receipt
        ConfirmDialog(
            title = "Mark rental #${activeReceipt.number.toString().padStart(4, '0')} returned?",
            message = "All tracked equipment on this rental will be returned to available fleet inventory.",
            confirmLabel = "Mark Returned",
            destructive = false,
            onConfirm = {
                viewModel.markRentalReturned(activeReceipt.id)
                showReturnDialog = false
            },
            onDismiss = { showReturnDialog = false },
        )
    }

    if (showRefundDialog && activeDetail != null) {
        val activeReceipt = activeDetail.receipt
        ConfirmDialog(
            title = "Refund rental #${activeReceipt.number.toString().padStart(4, '0')}?",
            message = "This marks the rental as refunded and restores equipment availability if not already returned.",
            confirmLabel = "Refund",
            destructive = true,
            onConfirm = {
                viewModel.refundReceipt(activeReceipt.id)
                showRefundDialog = false
            },
            onDismiss = { showRefundDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    viewModel: LoyPosViewModel,
    receiptId: String,
    data: LoyPosDataState,
    onBack: () -> Unit,
) {
    val receiptWithLines = remember(data.receipts, receiptId) {
        data.receipts.firstOrNull { it.receipt.id == receiptId }
    }
    if (receiptWithLines == null) {
        LaunchedEffect(receiptId) { onBack() }
        return
    }
    var showRefundDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showPdfPreview by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }
    val r = receiptWithLines.receipt
    val metrics = LocalLoyMetrics.current

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                title = { Text("Rental #${r.number.toString().padStart(4, '0')}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAgreementDialog = true },
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("receipt_detail_top_agreement"),
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = "Generate Rental Agreement", tint = LoyGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { showPdfPreview = true },
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("receipt_detail_top_pdf_preview"),
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Preview Receipt PDF", tint = LoyGreen, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        ReceiptDetailContent(
            receiptWithLines = receiptWithLines,
            data = data,
            onShareText = { viewModel.shareReceiptText(receiptWithLines) },
            onPreviewPdf = { showPdfPreview = true },
            onOpenAgreement = { showAgreementDialog = true },
            onMarkReturned = { showReturnDialog = true },
            onRequestRefund = { showRefundDialog = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(metrics.screenPadding),
        )
    }

    if (showPdfPreview) {
        ReceiptPdfPreviewSheet(
            viewModel = viewModel,
            receiptWithLines = receiptWithLines,
            onDismiss = { showPdfPreview = false },
        )
    }

    if (showAgreementDialog) {
        PopulatedAgreementDialog(
            viewModel = viewModel,
            receiptWithLines = receiptWithLines,
            data = data,
            onDismiss = { showAgreementDialog = false },
        )
    }

    if (showReturnDialog) {
        ConfirmDialog(
            title = "Mark rental #${r.number.toString().padStart(4, '0')} returned?",
            message = "All tracked equipment on this rental will be returned to available fleet inventory.",
            confirmLabel = "Mark Returned",
            destructive = false,
            onConfirm = {
                viewModel.markRentalReturned(r.id)
                showReturnDialog = false
            },
            onDismiss = { showReturnDialog = false },
        )
    }

    if (showRefundDialog) {
        ConfirmDialog(
            title = "Refund rental #${r.number.toString().padStart(4, '0')}?",
            message = "This marks the rental as refunded and restores equipment availability if not already returned.",
            confirmLabel = "Refund",
            destructive = true,
            onConfirm = {
                viewModel.refundReceipt(r.id)
                showRefundDialog = false
            },
            onDismiss = { showRefundDialog = false },
        )
    }
}

@Composable
private fun ReceiptDetailContent(
    receiptWithLines: ReceiptWithLines,
    data: LoyPosDataState,
    onShareText: () -> Unit,
    onPreviewPdf: () -> Unit,
    onOpenAgreement: () -> Unit,
    onMarkReturned: () -> Unit,
    onRequestRefund: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = receiptWithLines.receipt
    val sym = data.settings.currencySymbol
    val branch = data.branches.firstOrNull { it.id == r.branchId }
    val metrics = LocalLoyMetrics.current
    val now = remember(r.id, r.rentalStatus) { nowMs() }
    val overdue = r.isOverdue(now)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
    ) {
        GroupCard {
            Column(modifier = Modifier.padding(metrics.cardPadding), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDateTime(r.createdAt), color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                    PillBadge(
                        text = when {
                            !r.isCompleted -> "Refunded"
                            r.isReturned -> "Returned"
                            overdue -> "Overdue"
                            else -> "Out on Rent"
                        },
                        backgroundColor = when {
                            !r.isCompleted -> LoyErrorLight
                            r.isReturned -> LoyGreenLight
                            overdue -> Color(0xFFFFF3E0)
                            else -> LoyInfoLight
                        },
                        textColor = when {
                            !r.isCompleted -> LoyError
                            r.isReturned -> LoyGreen
                            overdue -> LoyWarning
                            else -> LoyInfo
                        },
                    )
                }
                Text(
                    "Duration: ${r.rentalDays} ${if (r.rentalDays == 1) "day" else "days"} · Return due ${formatShortDate(r.rentalEndMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) LoyWarning else LoyTextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                r.returnedAtMs?.let { returnedAt ->
                    if (returnedAt > 0L) {
                        Text("Returned on: ${formatDateTime(returnedAt)}", style = MaterialTheme.typography.bodySmall, color = LoyGreen)
                    }
                }
                if (r.eventName.isNotEmpty()) {
                    Text("Event: ${r.eventName}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
                if (r.eventVenue.isNotEmpty()) {
                    Text("Venue: ${r.eventVenue}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
                if (branch != null) {
                    Text("Branch: ${branch.name}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
                if (!r.cashierName.isNullOrEmpty()) {
                    Text("Dispatcher: ${r.cashierName}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
                if (!r.customerName.isNullOrEmpty()) {
                    Text("Client: ${r.customerName}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
                if (r.note.isNotEmpty()) {
                    Text("Note: ${r.note}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                }
            }
        }

        GroupCard {
            Column(modifier = Modifier.padding(metrics.cardPadding), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                receiptWithLines.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${formatQty(line.qty)} × ${formatCents(line.priceCents, sym)}/d × ${r.rentalDays}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                        }
                        Text(
                            formatCents(line.lineTotalCents, sym),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                HorizontalDivider(color = LoyBorder)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = LoyTextSecondary)
                    Text(formatCents(r.subtotalCents, sym), style = TabularMoneyStyle)
                }
                if (r.discountCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.discountLabel(), color = LoyGreen)
                        Text("-${formatCents(r.discountCents, sym)}", color = LoyGreen, style = TabularMoneyStyle)
                    }
                }
                if (r.taxCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax", color = LoyTextSecondary)
                        Text(formatCents(r.taxCents, sym), style = TabularMoneyStyle)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        formatCents(r.totalCents, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Paid (${r.paymentType})", color = LoyTextSecondary)
                    Text(formatCents(r.paidCents, sym), style = TabularMoneyStyle)
                }
                if (r.depositCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Security Deposit (refundable)", color = LoyTextSecondary)
                        Text(formatCents(r.depositCents, sym), style = TabularMoneyStyle)
                    }
                }
                if (r.changeCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Change", color = LoyTextSecondary)
                        Text(formatCents(r.changeCents, sym), style = TabularMoneyStyle)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onShareText,
                modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget),
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Share")
            }
            OutlinedButton(
                onClick = onPreviewPdf,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("receipt_detail_pdf_preview_button"),
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Preview PDF")
            }
        }

        OutlinedButton(
            onClick = onOpenAgreement,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = metrics.minTouchTarget)
                .testTag("receipt_detail_agreement_button"),
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Generate Populated Rental Agreement")
        }

        if (r.isRentalOut) {
            Button(
                onClick = onMarkReturned,
                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("mark_returned_button"),
            ) {
                Icon(Icons.Outlined.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark Equipment Returned")
            }
        }

        if (r.isCompleted) {
            Button(
                onClick = onRequestRefund,
                colors = ButtonDefaults.buttonColors(containerColor = LoyError, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("refund_receipt_button"),
            ) {
                Text("Refund Rental")
            }
        }
    }
}
