package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.RentalAgreementEngine
import com.loypos.app.core.formatCents
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.data.local.AgreementTemplateEntity
import com.loypos.app.data.local.ReceiptWithLines
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.SectionHeader
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.accessibleTouchTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgreementTemplatesScreen(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    onBack: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    val clipboard = LocalClipboardManager.current
    var editingTemplate by remember { mutableStateOf<AgreementTemplateEntity?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var selectedOrderId by remember { mutableStateOf<String?>(data.receipts.firstOrNull()?.receipt?.id) }
    var orderPickerExpanded by remember { mutableStateOf(false) }
    var previewTemplate by remember(data.agreementTemplates) {
        mutableStateOf(data.agreementTemplates.firstOrNull { it.isDefault } ?: data.agreementTemplates.firstOrNull())
    }

    val selectedOrder = remember(data.receipts, selectedOrderId) {
        data.receipts.firstOrNull { it.receipt.id == selectedOrderId } ?: data.receipts.firstOrNull()
    }
    val selectedCustomer = remember(selectedOrder, data.customers) {
        selectedOrder?.receipt?.customerId?.let { cid -> data.customers.firstOrNull { it.id == cid } }
    }

    val activePreviewTemplate = previewTemplate ?: data.agreementTemplates.firstOrNull()
    val populatedPreviewText = remember(activePreviewTemplate, selectedOrder, selectedCustomer, data.settings) {
        if (activePreviewTemplate == null) {
            ""
        } else if (selectedOrder != null) {
            RentalAgreementEngine.populateTemplate(
                templateBody = activePreviewTemplate.body,
                receipt = selectedOrder,
                customer = selectedCustomer,
                settings = data.settings,
            )
        } else {
            RentalAgreementEngine.populateSamplePreview(
                templateBody = activePreviewTemplate.body,
                settings = data.settings,
            )
        }
    }
    val windowSize = LocalLoyWindowSize.current

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                title = {
                    Column {
                        Text(
                            "Rental Agreement Templates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${data.agreementTemplates.size} templates · auto-populates from orders",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("agreements_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = {
                    isCreating = true
                    editingTemplate = AgreementTemplateEntity(
                        id = newId(),
                        title = "",
                        category = "General Event",
                        body = RentalAgreementEngine.defaultTemplates(nowMs()).first().body,
                        isDefault = data.agreementTemplates.isEmpty(),
                    )
                },
                containerColor = LoyGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("agreements_add_button"),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New agreement template")
            }
        },
        containerColor = LoyBackground,
    ) { padding ->
        val templatesListContent: @Composable (Modifier) -> Unit = { listModifier ->
            LazyColumn(
                modifier = listModifier,
                contentPadding = PaddingValues(metrics.screenPadding, metrics.screenPadding, metrics.screenPadding, 80.dp),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                item {
                    SectionHeader("Stored Digital Contract Templates")
                }

                if (data.agreementTemplates.isEmpty()) {
                    item {
                        GroupCard {
                            EmptyState(
                                icon = Icons.Outlined.Description,
                                title = "No agreement templates yet",
                                subtitle = "Create reusable digital rental contracts with auto-filled order fields.",
                            )
                        }
                    }
                } else {
                    items(data.agreementTemplates, key = { it.id }) { tpl ->
                        val isSelectedPreview = activePreviewTemplate?.id == tpl.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelectedPreview) LoyGreenLight.copy(alpha = 0.22f) else LoySurface,
                            border = BorderStroke(if (isSelectedPreview) 1.5.dp else 1.dp, if (isSelectedPreview) LoyGreen else LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { previewTemplate = tpl }
                                .testTag("agreement_template_card_${tpl.id}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(metrics.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(tpl.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (tpl.isDefault) {
                                                PillBadge("Default", backgroundColor = LoyGreenLight, textColor = LoyGreen)
                                            }
                                        }
                                        Text(tpl.category, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.setDefaultAgreementTemplate(tpl.id) },
                                            modifier = Modifier.accessibleTouchTarget(44.dp),
                                        ) {
                                            Icon(
                                                if (tpl.isDefault) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                                contentDescription = "Set default template",
                                                tint = if (tpl.isDefault) LoyGreen else LoyTextSecondary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.duplicateAgreementTemplate(tpl) },
                                            modifier = Modifier.accessibleTouchTarget(44.dp),
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate template", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                isCreating = false
                                                editingTemplate = tpl
                                            },
                                            modifier = Modifier.accessibleTouchTarget(44.dp).testTag("agreement_edit_${tpl.id}"),
                                        ) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit template", tint = LoyGreen, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = tpl.body.lineSequence().take(3).joinToString(" ").take(140),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        val livePreviewPanel: @Composable (Modifier) -> Unit = { panelModifier ->
            if (activePreviewTemplate == null) {
                Box(panelModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        title = "Select an agreement template",
                        subtitle = "Choose a contract template on the left to inspect its auto-populated order preview.",
                    )
                }
            } else {
                Column(
                    modifier = panelModifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(metrics.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader("Live Auto-Populated Order Preview · ${activePreviewTemplate.title}")
                    GroupCard {
                        Column(
                            modifier = Modifier.padding(metrics.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = orderPickerExpanded,
                                onExpandedChange = { orderPickerExpanded = !orderPickerExpanded },
                            ) {
                                val currentLabel = if (selectedOrder != null) {
                                    val r = selectedOrder.receipt
                                    "#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Walk-in"} (${formatCents(r.totalCents, data.settings.currencySymbol)})"
                                } else {
                                    "Sample Order Preview (#0042 · Gala Night)"
                                }
                                OutlinedTextField(
                                    value = currentLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Populate With Rental Order") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderPickerExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("agreement_order_selector"),
                                )
                                ExposedDropdownMenu(
                                    expanded = orderPickerExpanded,
                                    onDismissRequest = { orderPickerExpanded = false },
                                ) {
                                    if (data.receipts.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Sample Order Preview (#0042)") },
                                            onClick = { orderPickerExpanded = false },
                                        )
                                    } else {
                                        data.receipts.take(25).forEach { rec ->
                                            val r = rec.receipt
                                            DropdownMenuItem(
                                                text = { Text("#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Client"} · ${r.eventName}") },
                                                onClick = {
                                                    selectedOrderId = r.id
                                                    orderPickerExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoyBackground,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = populatedPreviewText,
                                    fontFamily = FontFamily.Serif,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextPrimary,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(populatedPreviewText))
                                        viewModel.showMessage("Populated agreement copied to clipboard")
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.shareAgreementText(activePreviewTemplate.title, populatedPreviewText)
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share", maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        val orderTag = selectedOrder?.receipt?.number?.let { "order-${it.toString().padStart(4, '0')}" } ?: "sample"
                                        viewModel.shareAgreementPdf(activePreviewTemplate.title, orderTag, populatedPreviewText)
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("agreement_export_pdf_button"),
                                ) {
                                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (windowSize.useSplitPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
            ) {
                templatesListContent(Modifier.weight(0.42f).fillMaxHeight())
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                livePreviewPanel(Modifier.weight(0.58f).fillMaxHeight())
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(metrics.screenPadding, metrics.screenPadding, metrics.screenPadding, 80.dp),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                item {
                    SectionHeader("Stored Digital Contract Templates")
                }

                if (data.agreementTemplates.isEmpty()) {
                    item {
                        GroupCard {
                            EmptyState(
                                icon = Icons.Outlined.Description,
                                title = "No agreement templates yet",
                                subtitle = "Create reusable digital rental contracts with auto-filled order fields.",
                            )
                        }
                    }
                } else {
                    items(data.agreementTemplates, key = { it.id }) { tpl ->
                        val isSelectedPreview = activePreviewTemplate?.id == tpl.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurface,
                            border = BorderStroke(if (isSelectedPreview) 1.5.dp else 1.dp, if (isSelectedPreview) LoyGreen else LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { previewTemplate = tpl }
                                .testTag("agreement_template_card_${tpl.id}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(metrics.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(tpl.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (tpl.isDefault) {
                                                PillBadge("Default", backgroundColor = LoyGreenLight, textColor = LoyGreen)
                                            }
                                        }
                                        Text(tpl.category, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.setDefaultAgreementTemplate(tpl.id) },
                                            modifier = Modifier.accessibleTouchTarget(44.dp),
                                        ) {
                                            Icon(
                                                if (tpl.isDefault) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                                contentDescription = "Set default template",
                                                tint = if (tpl.isDefault) LoyGreen else LoyTextSecondary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.duplicateAgreementTemplate(tpl) },
                                            modifier = Modifier.accessibleTouchTarget(44.dp),
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate template", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                isCreating = false
                                                editingTemplate = tpl
                                            },
                                            modifier = Modifier.accessibleTouchTarget(44.dp).testTag("agreement_edit_${tpl.id}"),
                                        ) {
                                            Icon(Icons.Outlined.Edit, contentDescription = "Edit template", tint = LoyGreen, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = tpl.body.lineSequence().take(3).joinToString(" ").take(140),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (activePreviewTemplate != null) {
                    item {
                        SectionHeader("Live Auto-Populated Order Preview")
                        GroupCard {
                            Column(
                                modifier = Modifier.padding(metrics.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = orderPickerExpanded,
                                    onExpandedChange = { orderPickerExpanded = !orderPickerExpanded },
                                ) {
                                    val currentLabel = if (selectedOrder != null) {
                                        val r = selectedOrder.receipt
                                        "#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Walk-in"} (${formatCents(r.totalCents, data.settings.currencySymbol)})"
                                    } else {
                                        "Sample Order Preview (#0042 · Gala Night)"
                                    }
                                    OutlinedTextField(
                                        value = currentLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Populate With Rental Order") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderPickerExpanded) },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                            .testTag("agreement_order_selector"),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = orderPickerExpanded,
                                        onDismissRequest = { orderPickerExpanded = false },
                                    ) {
                                        if (data.receipts.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Sample Order Preview (#0042)") },
                                                onClick = { orderPickerExpanded = false },
                                            )
                                        } else {
                                            data.receipts.take(25).forEach { rec ->
                                                val r = rec.receipt
                                                DropdownMenuItem(
                                                    text = { Text("#${r.number.toString().padStart(4, '0')} · ${r.customerName ?: "Client"} · ${r.eventName}") },
                                                    onClick = {
                                                        selectedOrderId = r.id
                                                        orderPickerExpanded = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LoyBackground,
                                    border = BorderStroke(1.dp, LoyBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = populatedPreviewText,
                                        fontFamily = FontFamily.Serif,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextPrimary,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(populatedPreviewText))
                                            viewModel.showMessage("Populated agreement copied to clipboard")
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.shareAgreementText(activePreviewTemplate.title, populatedPreviewText)
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share", maxLines = 1)
                                    }
                                    Button(
                                        onClick = {
                                            val orderTag = selectedOrder?.receipt?.number?.let { "order-${it.toString().padStart(4, '0')}" } ?: "sample"
                                            viewModel.shareAgreementPdf(activePreviewTemplate.title, orderTag, populatedPreviewText)
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("agreement_export_pdf_button"),
                                    ) {
                                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("PDF", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingTemplate?.let { tpl ->
        AgreementTemplateEditorSheet(
            template = tpl,
            isNew = isCreating,
            onSave = { updated ->
                viewModel.saveAgreementTemplate(updated)
                previewTemplate = updated
                editingTemplate = null
            },
            onDelete = {
                viewModel.deleteAgreementTemplate(tpl.id)
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AgreementTemplateEditorSheet(
    template: AgreementTemplateEntity,
    isNew: Boolean,
    onSave: (AgreementTemplateEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(template.title) }
    var category by remember { mutableStateOf(template.category) }
    var body by remember { mutableStateOf(template.body) }
    var isDefault by remember { mutableStateOf(template.isDefault) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (isNew) "New Rental Agreement Template" else "Edit Agreement Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!isNew) {
                    IconButton(onClick = onDelete, modifier = Modifier.accessibleTouchTarget(48.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete template", tint = LoyError)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Template Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("agreement_editor_title_input"),
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g. General Event, Wedding, AV)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Set as Default Agreement Template", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }

            Text(
                "Tap any merge field below to append it to your template:",
                style = MaterialTheme.typography.labelSmall,
                color = LoyTextSecondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RentalAgreementEngine.placeholders.forEach { ph ->
                    AssistChip(
                        onClick = {
                            val sep = if (body.isEmpty() || body.endsWith("\n") || body.endsWith(" ")) "" else " "
                            body += "$sep${ph.token}"
                        },
                        label = { Text(ph.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Agreement Terms & Merge Tokens") },
                minLines = 10,
                maxLines = 18,
                modifier = Modifier.fillMaxWidth().testTag("agreement_editor_body_input"),
            )

            Button(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        onSave(
                            template.copy(
                                title = title.trim(),
                                category = category.trim().ifEmpty { "General Event" },
                                body = body,
                                isDefault = isDefault,
                            )
                        )
                    }
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("agreement_editor_save_button"),
            ) {
                Text("Save Template", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopulatedAgreementDialog(
    viewModel: LoyPosViewModel,
    receiptWithLines: ReceiptWithLines,
    data: LoyPosDataState,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val templates = data.agreementTemplates
    var selectedTemplateId by remember(templates) {
        mutableStateOf(
            (templates.firstOrNull { it.isDefault } ?: templates.firstOrNull())?.id
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedTemplate = templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    val customer = remember(receiptWithLines, data.customers) {
        receiptWithLines.receipt.customerId?.let { cid -> data.customers.firstOrNull { it.id == cid } }
    }
    val populatedText = remember(selectedTemplate, receiptWithLines, customer, data.settings) {
        val tplBody = selectedTemplate?.body ?: RentalAgreementEngine.defaultTemplates(nowMs()).first().body
        RentalAgreementEngine.populateTemplate(tplBody, receiptWithLines, customer, data.settings)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Rental Agreement · #${receiptWithLines.receipt.number.toString().padStart(4, '0')}")
                Text(
                    receiptWithLines.receipt.customerName ?: "Walk-in Client",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (templates.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.title ?: "Standard Agreement",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Template") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            templates.forEach { tpl ->
                                DropdownMenuItem(
                                    text = { Text(tpl.title) },
                                    onClick = {
                                        selectedTemplateId = tpl.id
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                ) {
                    Text(
                        text = populatedText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(populatedText))
                        viewModel.showMessage("Copied contract to clipboard")
                    },
                ) {
                    Text("Copy")
                }
                TextButton(
                    onClick = {
                        viewModel.shareAgreementText(
                            selectedTemplate?.title ?: "Rental Agreement",
                            populatedText,
                        )
                    },
                ) {
                    Text("Share")
                }
                Button(
                    onClick = {
                        val orderTag = "order-${receiptWithLines.receipt.number.toString().padStart(4, '0')}"
                        viewModel.shareAgreementPdf(
                            selectedTemplate?.title ?: "Rental Agreement",
                            orderTag,
                            populatedText,
                        )
                    },
                ) {
                    Text("PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
