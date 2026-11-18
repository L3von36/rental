package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatDateTime
import com.loypos.app.core.formatShortDate
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.components.ReceiptPdfPreviewSheet
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.TabularMoneyStyle

@Composable
fun ReceiptSuccessScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
    data: LoyPosDataState,
    onDone: () -> Unit,
) {
    val receiptWithLines by transactionViewModel.lastCompletedSale.collectAsStateWithLifecycle()
    val currentReceipt = receiptWithLines
    if (currentReceipt == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val r = currentReceipt.receipt
    val sym = data.settings.currencySymbol
    var showPdfPreview by remember { mutableStateOf(false) }
    var showAgreementDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(LoyGreenLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Sale completed",
                    tint = LoyGreen,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatCents(r.totalCents, sym),
                    style = TabularMoneyStyle.merge(MaterialTheme.typography.displaySmall),
                    fontWeight = FontWeight.Bold,
                    color = LoyTextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Total paid (${r.paymentType})",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }

            Surface(
                shape = RoundedCornerShape(11.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rental #", color = LoyTextSecondary)
                        Text("#${r.number.toString().padStart(4, '0')}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Duration", color = LoyTextSecondary)
                        Text("${r.rentalDays}d · Due ${formatShortDate(r.rentalEndMs)}", fontWeight = FontWeight.Medium)
                    }
                    if (r.eventName.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Event", color = LoyTextSecondary)
                            Text(r.eventName, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date", color = LoyTextSecondary)
                        Text(formatDateTime(r.createdAt))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid", color = LoyTextSecondary)
                        Text(formatCents(r.paidCents, sym), style = TabularMoneyStyle)
                    }
                    if (r.depositCents > 0L) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Security Deposit", color = LoyTextSecondary)
                            Text(formatCents(r.depositCents, sym), style = TabularMoneyStyle)
                        }
                    }
                    if (r.changeCents > 0L) {
                        HorizontalDivider(color = LoyBorder)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Change", color = LoyGreen, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatCents(r.changeCents, sym),
                                style = TabularMoneyStyle,
                                color = LoyGreen,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.shareReceiptText(currentReceipt) },
                    modifier = Modifier.weight(1f).height(42.dp).testTag("receipt_share_text_button"),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share")
                }
                OutlinedButton(
                    onClick = { showPdfPreview = true },
                    modifier = Modifier.weight(1f).height(42.dp).testTag("receipt_share_pdf_button"),
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Preview PDF")
                }
            }

            OutlinedButton(
                onClick = { showAgreementDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("receipt_agreement_button"),
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Rental Agreement")
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("receipt_done_button"),
            ) {
                Text("New Rental")
            }
        }
    }

    if (showPdfPreview) {
        ReceiptPdfPreviewSheet(
            viewModel = viewModel,
            receiptWithLines = currentReceipt,
            onDismiss = { showPdfPreview = false },
        )
    }

    if (showAgreementDialog) {
        PopulatedAgreementDialog(
            viewModel = viewModel,
            receiptWithLines = currentReceipt,
            data = data,
            onDismiss = { showAgreementDialog = false },
        )
    }
}
