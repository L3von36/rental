package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.DesktopRentalAgreementEngine
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopAgreementTemplate
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgreementTemplatesView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val templates = snapshot.agreementTemplates
    var selectedTemplateId by remember { mutableStateOf(templates.firstOrNull { it.isDefault }?.id ?: templates.firstOrNull()?.id) }

    val selectedTemplate = remember(templates, selectedTemplateId) {
        templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    }

    var title by remember(selectedTemplate?.id) { mutableStateOf(selectedTemplate?.title ?: "") }
    var category by remember(selectedTemplate?.id) { mutableStateOf(selectedTemplate?.category ?: "General Event") }
    var body by remember(selectedTemplate?.id) { mutableStateOf(selectedTemplate?.body ?: "") }
    var isDefault by remember(selectedTemplate?.id) { mutableStateOf(selectedTemplate?.isDefault == true) }

    LaunchedEffect(selectedTemplate?.updatedAt) {
        if (selectedTemplate != null) {
            title = selectedTemplate.title
            category = selectedTemplate.category
            body = selectedTemplate.body
            isDefault = selectedTemplate.isDefault
        }
    }

    var selectedOrderId by remember(snapshot.orders) {
        mutableStateOf(snapshot.orders.firstOrNull()?.id)
    }
    var orderExpanded by remember { mutableStateOf(false) }

    val selectedOrder = remember(snapshot.orders, selectedOrderId) {
        snapshot.orders.firstOrNull { it.id == selectedOrderId } ?: snapshot.orders.firstOrNull()
    }
    val selectedClient = remember(selectedOrder, snapshot.clients) {
        selectedOrder?.customerId?.let { cid -> snapshot.clients.firstOrNull { it.id == cid } }
    }

    val populatedText = remember(body, selectedOrder, selectedClient, snapshot.settings) {
        if (selectedOrder != null) {
            DesktopRentalAgreementEngine.populateTemplate(body, selectedOrder, selectedClient, snapshot.settings)
        } else {
            DesktopRentalAgreementEngine.populateSamplePreview(body, snapshot.settings)
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Pane: Template Library + Editor
        Column(modifier = Modifier.weight(0.50f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Agreement Templates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${templates.size} templates with dynamic order merge fields", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                    }
                    DesktopPrimaryButton(
                        text = "New Template",
                        icon = Icons.Outlined.Add,
                        onClick = {
                            val newTpl = DesktopAgreementTemplate(
                                id = newId(),
                                title = "Custom Event Rental Contract",
                                category = "Custom",
                                body = DesktopRentalAgreementEngine.defaultTemplates(nowMs()).first().body,
                                isDefault = templates.isEmpty(),
                            )
                            repository.saveAgreementTemplate(newTpl)
                            selectedTemplateId = newTpl.id
                            onToast("Created new agreement template")
                        },
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Template Table
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LoyBorder),
                    color = LoySurface,
                ) {
                    Column {
                        DesktopTableHeaderRow {
                            DesktopTableHeaderText("Template Title", Modifier.weight(1.4f))
                            DesktopTableHeaderText("Category", Modifier.weight(0.7f))
                            DesktopTableHeaderText("Actions", Modifier.width(100.dp))
                        }
                        templates.forEach { tpl ->
                            DesktopTableRow(
                                selected = tpl.id == selectedTemplate?.id,
                                onClick = { selectedTemplateId = tpl.id },
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.4f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = tpl.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (tpl.isDefault) {
                                        DesktopBadge("Default", LoyGreenLight, LoyGreenDark)
                                    }
                                }
                                Text(
                                    text = tpl.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                    modifier = Modifier.weight(0.7f),
                                )
                                Row(modifier = Modifier.width(100.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(
                                        onClick = { repository.setDefaultAgreementTemplate(tpl.id) },
                                        modifier = Modifier.size(26.dp).pointerHoverIcon(PointerIcon.Hand),
                                    ) {
                                        Icon(
                                            if (tpl.isDefault) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Set default",
                                            tint = if (tpl.isDefault) LoyGreenDark else LoyTextMuted,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val copy = repository.duplicateAgreementTemplate(tpl)
                                            selectedTemplateId = copy.id
                                            onToast("Duplicated template")
                                        },
                                        modifier = Modifier.size(26.dp).pointerHoverIcon(PointerIcon.Hand),
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate", tint = LoyTextMuted, modifier = Modifier.size(15.dp))
                                    }
                                    if (templates.size > 1) {
                                        IconButton(
                                            onClick = {
                                                repository.deleteAgreementTemplate(tpl.id)
                                                selectedTemplateId = null
                                                onToast("Template deleted")
                                            },
                                            modifier = Modifier.size(26.dp).pointerHoverIcon(PointerIcon.Hand),
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = LoyError, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTemplate != null) {
                    DesktopSectionCard(title = "Edit Selected Template") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DesktopFormInput("Template Title", title, { title = it }, modifier = Modifier.weight(2f))
                            DesktopFormInput("Category", category, { category = it }, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Use as Default Agreement Template", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                        }
                        Text("Click to insert order merge field:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            DesktopRentalAgreementEngine.placeholders.forEach { ph ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LoySurfaceVariant,
                                    border = BorderStroke(1.dp, LoyBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            val sep = if (body.isEmpty() || body.endsWith("\n") || body.endsWith(" ")) "" else " "
                                            body += "$sep${ph.token}"
                                        },
                                ) {
                                    Text(
                                        text = ph.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                        DesktopFormInput(
                            label = "Contract Body & Placeholders",
                            value = body,
                            onValueChange = { body = it },
                            singleLine = false,
                            minLines = 12,
                        )
                        DesktopPrimaryButton(
                            text = "Save Template Changes",
                            onClick = {
                                repository.saveAgreementTemplate(
                                    selectedTemplate.copy(
                                        title = title.trim().ifEmpty { "Rental Agreement" },
                                        category = category.trim().ifEmpty { "General Event" },
                                        body = body,
                                        isDefault = isDefault,
                                    )
                                )
                                onToast("Saved template \"$title\"")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Pane: Auto-Populated Contract Sheet Preview
        Column(
            modifier = Modifier.weight(0.50f).fillMaxHeight().background(LoySurface),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Populated Contract Document Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Live merge preview for printable dispatch contracts", style = MaterialTheme.typography.labelSmall, color = LoyTextMuted)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Populate With Rental Order Data", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                    ExposedDropdownMenuBox(expanded = orderExpanded, onExpandedChange = { orderExpanded = !orderExpanded }) {
                        val label = if (selectedOrder != null) {
                            "#${selectedOrder.number.toString().padStart(4, '0')} · ${selectedOrder.customerName ?: "Walk-in"} · ${selectedOrder.eventName.ifEmpty { formatCents(selectedOrder.totalCents, snapshot.settings.currencySymbol) }}"
                        } else {
                            "Sample Booking Preview (#0042 · Gala Night)"
                        }
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
                                Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded)
                            }
                        }
                        ExposedDropdownMenu(expanded = orderExpanded, onDismissRequest = { orderExpanded = false }) {
                            if (snapshot.orders.isEmpty()) {
                                DropdownMenuItem(text = { Text("Sample Booking Preview (#0042)") }, onClick = { orderExpanded = false })
                            } else {
                                snapshot.orders.take(30).forEach { o ->
                                    DropdownMenuItem(
                                        text = { Text("#${o.number.toString().padStart(4, '0')} · ${o.customerName ?: "Client"} · ${o.eventName}") },
                                        onClick = { selectedOrderId = o.id; orderExpanded = false },
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorderStrong),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Text(
                        text = populatedText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DesktopSecondaryButton(
                        text = "Copy Text",
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            clipboard.setText(AnnotatedString(populatedText))
                            onToast("Populated rental agreement copied to clipboard")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSecondaryButton(
                        text = "Save .TXT",
                        icon = Icons.Outlined.Download,
                        onClick = {
                            val orderSlug = selectedOrder?.number?.toString()?.padStart(4, '0') ?: "sample"
                            val chooser = JFileChooser().apply {
                                selectedFile = File("rental-agreement-$orderSlug.txt")
                                fileFilter = FileNameExtensionFilter("Text agreement (*.txt)", "txt")
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                chooser.selectedFile.writeText(populatedText, Charsets.UTF_8)
                                onToast("Saved ${chooser.selectedFile.name}")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    DesktopPrimaryButton(
                        text = "Export Printable HTML",
                        icon = Icons.Outlined.Download,
                        onClick = {
                            val orderSlug = selectedOrder?.number?.toString()?.padStart(4, '0') ?: "sample"
                            val chooser = JFileChooser().apply {
                                selectedFile = File("rental-agreement-$orderSlug.html")
                                fileFilter = FileNameExtensionFilter("Printable HTML (*.html)", "html")
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                DesktopRentalAgreementEngine.exportHtmlAgreement(
                                    targetFile = chooser.selectedFile,
                                    title = title.ifEmpty { "Rental Agreement" },
                                    populatedText = populatedText,
                                )
                                onToast("Exported printable agreement to ${chooser.selectedFile.name}")
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPopulatedAgreementDialog(
    order: DesktopRentalOrder,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val templates = snapshot.agreementTemplates
    var selectedTemplateId by remember(templates) {
        mutableStateOf((templates.firstOrNull { it.isDefault } ?: templates.firstOrNull())?.id)
    }
    var expanded by remember { mutableStateOf(false) }

    val selectedTpl = templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    val client = remember(order, snapshot.clients) {
        order.customerId?.let { cid -> snapshot.clients.firstOrNull { it.id == cid } }
    }
    val populatedText = remember(selectedTpl, order, client, snapshot.settings) {
        val tplBody = selectedTpl?.body ?: DesktopRentalAgreementEngine.defaultTemplates(nowMs()).first().body
        DesktopRentalAgreementEngine.populateTemplate(tplBody, order, client, snapshot.settings)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digital Rental Agreement · #${order.number.toString().padStart(4, '0')}") },
        text = {
            Column(modifier = Modifier.width(620.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (templates.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
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
                                Text(selectedTpl?.title ?: "Standard Agreement", style = MaterialTheme.typography.bodySmall)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        }
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            templates.forEach { tpl ->
                                DropdownMenuItem(text = { Text(tpl.title) }, onClick = { selectedTemplateId = tpl.id; expanded = false })
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorderStrong),
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                ) {
                    Text(
                        text = populatedText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopSecondaryButton(
                    text = "Copy",
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        clipboard.setText(AnnotatedString(populatedText))
                        onToast("Agreement copied to clipboard")
                    },
                )
                DesktopPrimaryButton(
                    text = "Export Printable HTML",
                    icon = Icons.Outlined.Download,
                    onClick = {
                        val orderSlug = order.number.toString().padStart(4, '0')
                        val chooser = JFileChooser().apply {
                            selectedFile = File("rental-agreement-$orderSlug.html")
                            fileFilter = FileNameExtensionFilter("Printable HTML (*.html)", "html")
                        }
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            DesktopRentalAgreementEngine.exportHtmlAgreement(
                                targetFile = chooser.selectedFile,
                                title = selectedTpl?.title ?: "Rental Agreement",
                                populatedText = populatedText,
                            )
                            onToast("Saved ${chooser.selectedFile.name}")
                        }
                    },
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
