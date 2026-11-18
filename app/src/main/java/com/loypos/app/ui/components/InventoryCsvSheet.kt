package com.loypos.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.CsvImportPreview
import com.loypos.app.core.InventoryCsvExportResult
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatQty
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenDarkText
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoyInfoLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.LoyWarningLight
import com.loypos.app.ui.theme.TabularMoneyStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryCsvSheet(
    viewModel: LoyPosViewModel,
    data: LoyPosDataState,
    onDismiss: () -> Unit,
) {
    var includeArchived by remember { mutableStateOf(false) }
    var exportPreview by remember { mutableStateOf<InventoryCsvExportResult?>(null) }
    var importPreview by remember { mutableStateOf<CsvImportPreview?>(null) }
    var skipExisting by remember { mutableStateOf(false) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val pending = exportPreview
        if (uri != null && pending != null) {
            viewModel.writeInventoryCsvToUri(uri, pending)
        }
    }

    val saveTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.writeInventoryTemplateCsvToUri(uri)
        }
    }

    val openCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importErrorMessage = null
            viewModel.previewInventoryCsvFromUri(uri) { result ->
                result.onSuccess { preview ->
                    importPreview = preview
                }.onFailure { err ->
                    importErrorMessage = err.message ?: "Could not parse CSV file"
                }
            }
        }
    }

    LaunchedEffect(includeArchived, data.items, data.categories) {
        viewModel.buildInventoryCsvExport(includeArchived) { result ->
            exportPreview = result
        }
    }

    val sym = data.settings.currencySymbol

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LoyGreenLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TableView,
                        contentDescription = "Inventory CSV management",
                        tint = LoyGreen,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventory CSV Import & Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${data.items.size} active items · ${data.categories.size} categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            }

            // Export Section
            SectionHeader("Export Inventory")
            GroupCard(modifier = Modifier.testTag("inventory_csv_export_card")) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatBlock(
                            label = "Products",
                            value = (exportPreview?.itemCount ?: data.items.size).toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Tracked SKUs",
                            value = (exportPreview?.trackedCount ?: 0).toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = "Total Units",
                            value = formatQty(exportPreview?.totalStockUnits ?: 0.0),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { includeArchived = !includeArchived }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include archived products", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "Columns: Name, SKU, Barcode, Category, Price, Cost, Track Stock, Stock",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                        }
                        Switch(
                            checked = includeArchived,
                            onCheckedChange = { includeArchived = it },
                            modifier = Modifier.testTag("inventory_csv_include_archived"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fileName = exportPreview?.filename ?: "loypos-inventory.csv"
                                saveCsvLauncher.launch(fileName)
                            },
                            enabled = exportPreview != null,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("inventory_csv_save_button"),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Save CSV")
                        }
                        Button(
                            onClick = { viewModel.shareInventoryCsv(includeArchived) },
                            enabled = exportPreview != null,
                            colors = ButtonDefaults.buttonColors(containerColor = LoyGreen, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("inventory_csv_share_button"),
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Share CSV")
                        }
                    }
                }
            }

            // Import Section
            SectionHeader("Import Product List (CSV)")
            GroupCard(modifier = Modifier.testTag("inventory_csv_import_card")) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Upload a spreadsheet (.csv) to bulk-create new products and categories or update prices, SKUs, and stock quantities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                openCsvLauncher.launch(
                                    arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "*/*")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LoyInfo, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("inventory_csv_pick_file_button"),
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Choose CSV")
                        }
                        OutlinedButton(
                            onClick = { saveTemplateLauncher.launch("loypos-inventory-template.csv") },
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("inventory_csv_template_button"),
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Template")
                        }
                    }

                    if (importErrorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoyErrorLight,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LoyError, modifier = Modifier.size(18.dp))
                                Text(importErrorMessage!!, color = LoyError, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    importPreview?.let { preview ->
                        HorizontalDivider(color = LoyBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preview.filenameLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${preview.rows.size} valid rows · ${preview.errors.size} errors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            TextButton(
                                onClick = { importPreview = null },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Clear")
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PillBadge(
                                text = "${preview.newItemsCount} new",
                                backgroundColor = LoyGreenLight,
                                textColor = LoyGreenDarkText,
                            )
                            PillBadge(
                                text = "${preview.updateItemsCount} updates",
                                backgroundColor = LoyInfoLight,
                                textColor = LoyInfo,
                            )
                            if (preview.newCategoriesCount > 0) {
                                PillBadge(
                                    text = "${preview.newCategoriesCount} new categories",
                                    backgroundColor = LoyWarningLight,
                                    textColor = LoyWarning,
                                )
                            }
                            if (preview.errors.isNotEmpty()) {
                                PillBadge(
                                    text = "${preview.errors.size} invalid",
                                    backgroundColor = LoyErrorLight,
                                    textColor = LoyError,
                                )
                            }
                        }

                        if (preview.updateItemsCount > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "When SKU, barcode, or product name matches an existing item:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LoyTextSecondary,
                                )
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = !skipExisting,
                                        onClick = { skipExisting = false },
                                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                                        modifier = Modifier.testTag("inventory_csv_mode_update"),
                                    ) {
                                        Text("Update existing")
                                    }
                                    SegmentedButton(
                                        selected = skipExisting,
                                        onClick = { skipExisting = true },
                                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                                        modifier = Modifier.testTag("inventory_csv_mode_skip"),
                                    ) {
                                        Text("Skip existing")
                                    }
                                }
                            }
                        }

                        if (preview.errors.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoyErrorLight,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    preview.errors.take(4).forEach { err ->
                                        Text(
                                            text = "Row ${err.rowNumber}: ${err.reason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyError,
                                        )
                                    }
                                    if (preview.errors.size > 4) {
                                        Text(
                                            text = "+ ${preview.errors.size - 4} more row errors skipped",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyError,
                                        )
                                    }
                                }
                            }
                        }

                        if (preview.rows.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoySurfaceVariant,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    preview.rows.take(5).forEachIndexed { idx, row ->
                                        if (idx > 0) HorizontalDivider(color = LoyBorder)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = row.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false),
                                                    )
                                                    val tagText = when {
                                                        row.isUpdate && skipExisting -> "SKIP"
                                                        row.isUpdate -> "UPDATE"
                                                        else -> "NEW"
                                                    }
                                                    PillBadge(
                                                        text = tagText,
                                                        backgroundColor = when (tagText) {
                                                            "NEW" -> LoyGreenLight
                                                            "UPDATE" -> LoyInfoLight
                                                            else -> LoySurface
                                                        },
                                                        textColor = when (tagText) {
                                                            "NEW" -> LoyGreenDarkText
                                                            "UPDATE" -> LoyInfo
                                                            else -> LoyTextSecondary
                                                        },
                                                    )
                                                }
                                                val sub = buildList {
                                                    if (row.sku.isNotEmpty()) add("SKU ${row.sku}")
                                                    if (row.categoryName.isNotEmpty()) add(row.categoryName)
                                                    if (row.trackStock) add("Stock ${formatQty(row.stock)}")
                                                }.joinToString(" · ")
                                                if (sub.isNotEmpty()) {
                                                    Text(
                                                        text = sub,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = LoyTextSecondary,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = formatCents(row.priceCents, sym),
                                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                                fontWeight = FontWeight.SemiBold,
                                                color = LoyTextPrimary,
                                            )
                                        }
                                    }
                                    if (preview.rows.size > 5) {
                                        Text(
                                            text = "+ ${preview.rows.size - 5} more rows ready to import",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                }
                            }

                            val importableCount = if (skipExisting) preview.newItemsCount else preview.rows.size
                            Button(
                                onClick = {
                                    isImporting = true
                                    viewModel.commitInventoryCsvImport(preview, skipExisting) {
                                        isImporting = false
                                        importPreview = null
                                        onDismiss()
                                    }
                                },
                                enabled = importableCount > 0 && !isImporting,
                                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("inventory_csv_commit_button"),
                            ) {
                                Text(
                                    if (isImporting) "Importing..." else "Import $importableCount ${if (importableCount == 1) "Product" else "Products"}",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}
