package com.loypos.desktop.ui

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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        // Left Pane: Template List + Editor
        Column(modifier = Modifier.weight(0.50f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Digital Rental Agreement Templates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${templates.size} stored templates with auto-populate merge fields", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                    }
                    Button(onClick = {
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
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("New Template")
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Template Cards
                templates.forEach { tpl ->
                    val sel = tpl.id == selectedTemplate?.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel) LoyGreenLight.copy(alpha = 0.5f) else LoySurface,
                        border = BorderStroke(1.dp, if (sel) LoyGreen else LoyBorder),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            selectedTemplateId = tpl.id
                        },
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tpl.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (tpl.isDefault) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(LoyGreenLight).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("Default", style = MaterialTheme.typography.labelSmall, color = LoyGreenDark, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                Text(tpl.category, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                            IconButton(onClick = { repository.setDefaultAgreementTemplate(tpl.id) }) {
                                Icon(if (tpl.isDefault) Icons.Outlined.Star else Icons.Outlined.StarOutline, contentDescription = "Set default", tint = if (tpl.isDefault) LoyGreenDark else LoyTextSecondary)
                            }
                            IconButton(onClick = {
                                val copy = repository.duplicateAgreementTemplate(tpl)
                                selectedTemplateId = copy.id
                                onToast("Duplicated template")
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate", tint = LoyTextSecondary)
                            }
                            if (templates.size > 1) {
                                IconButton(onClick = {
                                    repository.deleteAgreementTemplate(tpl.id)
                                    selectedTemplateId = null
                                    onToast("Template deleted")
                                }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = LoyError)
                                }
                            }
                        }
                    }
                }

                if (selectedTemplate != null) {
                    HorizontalDivider(color = LoyBorder)
                    Text("Edit Selected Template", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Template Title") }, singleLine = true, modifier = Modifier.weight(2f))
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Use as Default Agreement Template")
                        Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                    }
                    Text("Insert Order Merge Token:", style = MaterialTheme.typography.labelMedium, color = LoyTextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DesktopRentalAgreementEngine.placeholders.forEach { ph ->
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
                        label = { Text("Contract Body & Placeholders") },
                        minLines = 12,
                        maxLines = 22,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
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
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                    ) {
                        Text("Save Template Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Right Pane: Auto-Populated Order Preview & Export
        Surface(
            color = LoySurface,
            border = BorderStroke(1.dp, LoyBorder),
            modifier = Modifier.weight(0.50f).fillMaxHeight(),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Auto-Populated Agreement Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                ExposedDropdownMenuBox(expanded = orderExpanded, onExpandedChange = { orderExpanded = !orderExpanded }) {
                    val label = if (selectedOrder != null) {
                        "#${selectedOrder.number.toString().padStart(4, '0')} · ${selectedOrder.customerName ?: "Walk-in"} · ${selectedOrder.eventName.ifEmpty { formatCents(selectedOrder.totalCents, snapshot.settings.currencySymbol) }}"
                    } else {
                        "Sample Booking Preview (#0042 · Gala Night)"
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Populate With Rental Order Data") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
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

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Text(
                        text = populatedText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(populatedText))
                            onToast("Populated rental agreement copied to clipboard")
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copy Text")
                    }
                    OutlinedButton(
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
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Save .TXT")
                    }
                    Button(
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
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Export Printable HTML")
                    }
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
                        OutlinedTextField(
                            value = selectedTpl?.title ?: "Standard Agreement",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Template") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            templates.forEach { tpl ->
                                DropdownMenuItem(text = { Text(tpl.title) }, onClick = { selectedTemplateId = tpl.id; expanded = false })
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                ) {
                    Text(
                        text = populatedText,
                        fontFamily = FontFamily.Serif,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    clipboard.setText(AnnotatedString(populatedText))
                    onToast("Agreement copied to clipboard")
                }) { Text("Copy") }
                Button(onClick = {
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
                }) { Text("Export Printable HTML") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
