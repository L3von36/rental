package com.loypos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatQty
import com.loypos.app.core.newId
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.core.parseQty
import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.ProductEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.UiSessionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.InventoryCsvSheet
import com.loypos.app.ui.components.InventoryStockAlertSettingsDialog
import com.loypos.app.ui.components.InventoryStockMonitorBanner
import com.loypos.app.ui.components.ItemThumbnail
import com.loypos.app.ui.components.StockDotBadge
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.components.categoryIconFor
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.tileColorFor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: LoyPosViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToStock: () -> Unit,
    onNavigateToQrTracking: (String?) -> Unit = {},
    onNavigateToMaintenance: (String?) -> Unit = {},
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val search by viewModel.inventorySearchQuery.collectAsStateWithLifecycle()
    val roomProducts by viewModel.inventoryProducts.collectAsStateWithLifecycle()
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ProductEntity?>(null) }
    var isCreatingItem by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var isCreatingCategory by remember { mutableStateOf(false) }
    var showCsvSheet by remember { mutableStateOf(false) }
    var showStockAlertSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateLowStockAlertSettings(
                enabled = true,
                threshold = data.settings.lowStockThreshold,
                intervalMinutes = data.settings.lowStockIntervalMinutes,
            )
        } else {
            viewModel.showMessage("Notification permission is needed for background stock alerts")
        }
    }

    LaunchedEffect(session.initialFilterLowStock) {
        if (session.initialFilterLowStock) {
            lowStockOnly = true
            tabIndex = 0
            viewModel.consumeLowStockNavigation()
        }
    }

    val sym = data.settings.currencySymbol
    val threshold = data.settings.lowStockThreshold
    val outOfStockCount = remember(roomProducts) {
        roomProducts.count { it.trackStock && it.stock <= 0.0 }
    }
    val lowStockCount = remember(roomProducts, threshold) {
        roomProducts.count { it.trackStock && it.stock > 0.0 && it.isBelowSafetyThreshold(threshold) }
    }
    val totalLowCount = outOfStockCount + lowStockCount

    val filteredProducts = remember(roomProducts, selectedCategoryFilter, lowStockOnly, threshold) {
        roomProducts.filter { product ->
            val matchesCat = selectedCategoryFilter == null || product.categoryId == selectedCategoryFilter
            val matchesLow = !lowStockOnly || product.isBelowSafetyThreshold(threshold)
            matchesCat && matchesLow
        }
    }
    val windowSize = LocalLoyWindowSize.current
    val activeSplitProduct = remember(editingItem, isCreatingItem, filteredProducts) {
        if (isCreatingItem) editingItem
        else editingItem?.let { cur -> filteredProducts.firstOrNull { it.id == cur.id } ?: cur }
            ?: filteredProducts.firstOrNull()
    }
    val activeSplitCategory = remember(editingCategory, isCreatingCategory, data.categories) {
        if (isCreatingCategory) editingCategory
        else editingCategory?.let { cur -> data.categories.firstOrNull { it.id == cur.id } ?: cur }
            ?: data.categories.firstOrNull()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    expandedHeight = 50.dp,
                    title = {
                        Text(
                            "Equipment & Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
                    actions = {
                        IconButton(
                            onClick = { showStockAlertSettingsDialog = true },
                            modifier = Modifier.size(42.dp).testTag("inventory_stock_alert_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalLowCount > 0) {
                                        Badge(containerColor = if (outOfStockCount > 0) LoyError else LoyWarning) {
                                            Text("$totalLowCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (data.settings.lowStockAlertsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                    contentDescription = "Stock Alert Settings",
                                    tint = if (totalLowCount > 0) (if (outOfStockCount > 0) LoyError else LoyWarning) else LoyGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onNavigateToQrTracking(null) },
                            modifier = Modifier.size(42.dp).testTag("inventory_qr_button"),
                        ) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = "QR Check-In and Tagging",
                                tint = LoyGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = { showCsvSheet = true },
                            modifier = Modifier.size(42.dp).testTag("inventory_csv_button"),
                        ) {
                            Icon(
                                Icons.Outlined.ImportExport,
                                contentDescription = "CSV Import and Export",
                                tint = LoyGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        TextButton(
                            onClick = onNavigateToStock,
                            modifier = Modifier.heightIn(min = 40.dp).testTag("inventory_stock_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(
                                "Fleet",
                                color = LoyGreen,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TopSyncIconButton(syncing = session.syncing, onClick = { viewModel.syncCloud() })
                    },
                )
                TabRow(selectedTabIndex = tabIndex, containerColor = LoySurface) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        modifier = Modifier.height(38.dp),
                        text = { Text("Tools (${data.items.size})", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        modifier = Modifier.height(38.dp),
                        text = { Text("Categories (${data.categories.size})", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        modifier = Modifier.height(38.dp),
                        text = { Text("Discounts", style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tabIndex == 0 || tabIndex == 1) {
                SmallFloatingActionButton(
                    onClick = {
                        if (tabIndex == 0) {
                            isCreatingItem = true
                            editingItem = ProductEntity(
                                id = newId(),
                                categoryId = data.categories.firstOrNull()?.id,
                                name = "",
                                priceCents = 0L,
                                trackStock = true,
                                stock = 0.0,
                            )
                        } else {
                            isCreatingCategory = true
                            editingCategory = CategoryEntity(
                                id = newId(),
                                name = "",
                                position = data.categories.size,
                            )
                        }
                    },
                    containerColor = LoyGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("inventory_fab_add"),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = if (tabIndex == 0) "New equipment" else "New category",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tabIndex) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LoySurface)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        CompactSearchField(
                            value = search,
                            onValueChange = { viewModel.setInventorySearch(it) },
                            placeholder = "Search equipment name or SKU",
                            inputTestTag = "inventory_search_input",
                            clearTestTag = "inventory_search_clear",
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LoySurface)
                            .padding(bottom = 7.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = !lowStockOnly && selectedCategoryFilter == null,
                                onClick = {
                                    lowStockOnly = false
                                    selectedCategoryFilter = null
                                },
                                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.height(30.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = LoyGreen,
                                ),
                            )
                        }
                        item {
                            val lowCount = data.items.count { it.isBelowSafetyThreshold(threshold) }
                            FilterChip(
                                selected = lowStockOnly,
                                onClick = { lowStockOnly = !lowStockOnly },
                                label = { Text("Low / Out ($lowCount)", style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.height(30.dp).testTag("inventory_filter_low_stock"),
                            )
                        }
                        items(data.categories, key = { it.id }) { cat ->
                            val sel = selectedCategoryFilter == cat.id
                            FilterChip(
                                selected = sel,
                                onClick = {
                                    selectedCategoryFilter = if (sel) null else cat.id
                                },
                                label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.height(30.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = LoyBorder)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        InventoryStockMonitorBanner(
                            alertsEnabled = data.settings.lowStockAlertsEnabled,
                            threshold = threshold,
                            lowCount = lowStockCount,
                            outOfStockCount = outOfStockCount,
                            workerStatus = session.lowStockWorkerStatus,
                            onConfigureClick = { showStockAlertSettingsDialog = true },
                            onCheckNowClick = { viewModel.runLowStockCheckNow() },
                            onFilterLowStockClick = { lowStockOnly = !lowStockOnly },
                            isFilterActive = lowStockOnly,
                        )
                    }

                    val productListPane: @Composable (Modifier) -> Unit = { paneMod ->
                        if (filteredProducts.isEmpty()) {
                            Box(paneMod.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    icon = Icons.Outlined.Inventory2,
                                    title = if (search.isBlank() && !lowStockOnly) "No equipment found" else "No matching equipment",
                                    subtitle = when {
                                        lowStockOnly -> "No equipment items are below their safety threshold."
                                        search.isBlank() -> "Tap + to add event tools to your fleet."
                                        else -> "No equipment matches \"$search\" by name or SKU."
                                    },
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = paneMod.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 76.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(filteredProducts, key = { it.id }) { product ->
                                    val catName = data.categories.firstOrNull { it.id == product.categoryId }?.name
                                        ?: "Uncategorized"
                                    val isSelected = windowSize.useSplitPane && !isCreatingItem && activeSplitProduct?.id == product.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) LoyGreenLight.copy(alpha = 0.25f) else LoySurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) LoyGreen else LoyBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                isCreatingItem = false
                                                editingItem = product
                                            }
                                            .testTag("inventory_product_${product.id}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            ItemThumbnail(item = product, size = 36.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    product.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    "$catName${if (product.sku.isNotEmpty()) " · SKU ${product.sku}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LoyTextSecondary,
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    StockDotBadge(item = product, threshold = threshold)
                                                    val rentCount = data.itemRentalCount(product.id)
                                                    Text(
                                                        "· Rented ${rentCount}x",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LoyTextSecondary,
                                                    )
                                                }
                                            }
                                            Text(
                                                "${formatCents(product.priceCents, sym)}/d",
                                                style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (windowSize.useSplitPane) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            productListPane(Modifier.weight(0.48f).fillMaxHeight())
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                            Box(modifier = Modifier.weight(0.52f).fillMaxHeight().background(LoySurface)) {
                                val targetProduct = activeSplitProduct
                                if (targetProduct == null) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        EmptyState(
                                            icon = Icons.Outlined.Inventory2,
                                            title = "Select equipment",
                                            subtitle = "Click any tool on the left to edit daily rates, stock, and QR asset tags.",
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        ProductEditorFormContent(
                                            product = targetProduct,
                                            isNew = isCreatingItem,
                                            data = data,
                                            onOpenQrUnits = {
                                                editingItem = null
                                                isCreatingItem = false
                                                onNavigateToQrTracking(targetProduct.id)
                                            },
                                            onOpenMaintenance = {
                                                editingItem = null
                                                isCreatingItem = false
                                                onNavigateToMaintenance(targetProduct.id)
                                            },
                                            onSave = { updated, pickedUri, removePhoto ->
                                                viewModel.saveItem(updated, pickedUri, removePhoto)
                                                isCreatingItem = false
                                                editingItem = updated
                                            },
                                            onDelete = {
                                                viewModel.deleteItem(targetProduct.id)
                                                editingItem = null
                                                isCreatingItem = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        productListPane(Modifier.weight(1f).fillMaxWidth())
                    }
                }

                1 -> {
                    val categoryListPane: @Composable (Modifier) -> Unit = { paneMod ->
                        if (data.categories.isEmpty()) {
                            Box(paneMod.fillMaxSize(), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    icon = Icons.Outlined.Category,
                                    title = "No categories",
                                    subtitle = "Tap + to create an equipment category.",
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = paneMod.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 76.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(data.categories, key = { it.id }) { cat ->
                                    val count = data.items.count { it.categoryId == cat.id }
                                    val tint = tileColorFor(cat.id)
                                    val isSelected = windowSize.useSplitPane && !isCreatingCategory && activeSplitCategory?.id == cat.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) LoyGreenLight.copy(alpha = 0.25f) else LoySurface,
                                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) LoyGreen else LoyBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                isCreatingCategory = false
                                                editingCategory = cat
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(tint.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    categoryIconFor(cat.id),
                                                    contentDescription = null,
                                                    tint = tint,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    cat.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text("$count tools", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (windowSize.useSplitPane) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            categoryListPane(Modifier.weight(0.48f).fillMaxHeight())
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder))
                            Box(modifier = Modifier.weight(0.52f).fillMaxHeight().background(LoySurface).padding(16.dp)) {
                                val targetCat = activeSplitCategory
                                if (targetCat == null) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        EmptyState(
                                            icon = Icons.Outlined.Category,
                                            title = "Select a category",
                                            subtitle = "Choose a category on the left or tap + to add one.",
                                        )
                                    }
                                } else {
                                    var catName by remember(targetCat.id, isCreatingCategory) { mutableStateOf(targetCat.name) }
                                    val toolCount = data.items.count { it.categoryId == targetCat.id }
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                if (isCreatingCategory) "New Category" else "Edit Category",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            if (!isCreatingCategory) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteCategory(targetCat.id)
                                                        editingCategory = null
                                                    },
                                                    modifier = Modifier.size(44.dp),
                                                ) {
                                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete category", tint = LoyError)
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = catName,
                                            onValueChange = { catName = it },
                                            label = { Text("Category name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        if (!isCreatingCategory) {
                                            Text(
                                                text = "$toolCount equipment items assigned to this category",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LoyTextSecondary,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val clean = catName.trim()
                                                if (clean.isNotEmpty()) {
                                                    viewModel.saveCategory(targetCat.id, clean, targetCat.position)
                                                    isCreatingCategory = false
                                                    editingCategory = targetCat.copy(name = clean)
                                                }
                                            },
                                            enabled = catName.trim().isNotEmpty(),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        ) {
                                            Text("Save Category")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        categoryListPane(Modifier.weight(1f).fillMaxWidth())
                    }
                }

                2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GroupCard {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                                    Text("Booking Discounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "LoyPOS supports whole-rental percentage discounts (5%, 10%, 15%, 20%, 25%, or custom %) and fixed currency discounts directly on the Rent Desk.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                                Text(
                                    "Tap the Discount chip inside any active booking or held quote to apply or remove a discount before payment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (!windowSize.useSplitPane) {
        editingItem?.let { item ->
            ProductEditorBottomSheet(
                product = item,
                isNew = isCreatingItem,
                data = data,
                onOpenQrUnits = {
                    editingItem = null
                    onNavigateToQrTracking(item.id)
                },
                onOpenMaintenance = {
                    editingItem = null
                    onNavigateToMaintenance(item.id)
                },
                onSave = { updated, pickedUri, removePhoto ->
                    viewModel.saveItem(updated, pickedUri, removePhoto)
                    editingItem = null
                },
                onDelete = {
                    viewModel.deleteItem(item.id)
                    editingItem = null
                },
                onDismiss = { editingItem = null },
            )
        }

        editingCategory?.let { cat ->
            CategoryEditorDialog(
                category = cat,
                isNew = isCreatingCategory,
                onSave = { name ->
                    viewModel.saveCategory(cat.id, name, cat.position)
                    editingCategory = null
                },
                onDelete = {
                    viewModel.deleteCategory(cat.id)
                    editingCategory = null
                },
                onDismiss = { editingCategory = null },
            )
        }
    }

    if (showCsvSheet) {
        InventoryCsvSheet(
            viewModel = viewModel,
            data = data,
            onDismiss = { showCsvSheet = false },
        )
    }

    if (showStockAlertSettingsDialog) {
        InventoryStockAlertSettingsDialog(
            initialEnabled = data.settings.lowStockAlertsEnabled,
            initialThreshold = data.settings.lowStockThreshold,
            initialIntervalMinutes = data.settings.lowStockIntervalMinutes,
            trackedItems = data.items.filter { it.trackStock && !it.archived },
            onUpdateItemThreshold = viewModel::updateItemSafetyThreshold,
            onSave = { enabled, newThreshold, interval ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.updateLowStockAlertSettings(enabled, newThreshold, interval)
                showStockAlertSettingsDialog = false
            },
            onTestCheckNow = {
                viewModel.runLowStockCheckNow()
            },
            onDismiss = { showStockAlertSettingsDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProductEditorBottomSheet(
    product: ProductEntity,
    isNew: Boolean,
    data: LoyPosDataState,
    onOpenQrUnits: () -> Unit,
    onOpenMaintenance: () -> Unit = {},
    onSave: (ProductEntity, Uri?, Boolean) -> Unit,
    onDelete: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductEditorFormContent(
                product = product,
                isNew = isNew,
                data = data,
                onOpenQrUnits = onOpenQrUnits,
                onOpenMaintenance = onOpenMaintenance,
                onSave = onSave,
                onDelete = onDelete,
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProductEditorFormContent(
    product: ProductEntity,
    isNew: Boolean,
    data: LoyPosDataState,
    onOpenQrUnits: () -> Unit,
    onOpenMaintenance: () -> Unit = {},
    onSave: (ProductEntity, Uri?, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(product.id, isNew) { mutableStateOf(product.name) }
    var categoryId by remember(product.id, isNew) { mutableStateOf(product.categoryId) }
    var catExpanded by remember(product.id, isNew) { mutableStateOf(false) }
    var priceText by remember(product.id, isNew) {
        mutableStateOf(
            if (product.priceCents == 0L) "" else (product.priceCents / 100.0).let {
                if (product.priceCents % 100L == 0L) (product.priceCents / 100L).toString() else "%.2f".format(it)
            }
        )
    }
    var costText by remember(product.id, isNew) {
        mutableStateOf(
            if (product.costCents == 0L) "" else (product.costCents / 100.0).let {
                if (product.costCents % 100L == 0L) (product.costCents / 100L).toString() else "%.2f".format(it)
            }
        )
    }
    var sku by remember(product.id, isNew) { mutableStateOf(product.sku) }
    var barcode by remember(product.id, isNew) { mutableStateOf(product.barcode) }
    var trackStock by remember(product.id, isNew) { mutableStateOf(product.trackStock) }
    var stockText by remember(product.id, isNew) { mutableStateOf(formatQty(product.stock)) }
    var safetyThresholdText by remember(product.id, isNew) {
        mutableStateOf(product.safetyThreshold?.let { formatQty(it) } ?: "")
    }
    var selectedBranches by remember(product.id, isNew) {
        mutableStateOf(
            if (product.branchIds.isEmpty()) emptySet() else product.branchIds.split(",").filter { it.isNotEmpty() }.toSet()
        )
    }
    var pickedPhotoUri by remember(product.id, isNew) { mutableStateOf<Uri?>(null) }
    var removeExistingPhoto by remember(product.id, isNew) { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            removeExistingPhoto = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (isNew) "New Equipment" else "Edit Equipment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (!isNew) {
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete equipment", tint = LoyError, modifier = Modifier.size(20.dp))
            }
        }
    }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val model: Any? = when {
                        pickedPhotoUri != null -> pickedPhotoUri
                        !removeExistingPhoto && product.hasPhoto && File(product.imagePath).exists() -> File(product.imagePath)
                        else -> null
                    }
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Product photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.height(38.dp),
                ) {
                    Text("Choose photo", style = MaterialTheme.typography.labelLarge)
                }
                if (pickedPhotoUri != null || (!removeExistingPhoto && product.hasPhoto)) {
                    TextButton(onClick = {
                        pickedPhotoUri = null
                        removeExistingPhoto = true
                    }) {
                        Text("Remove", color = LoyError)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Equipment name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
            )

            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded },
            ) {
                val catName = data.categories.firstOrNull { it.id == categoryId }?.name ?: "Uncategorized"
                OutlinedTextField(
                    value = catName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Uncategorized") },
                        onClick = { categoryId = null; catExpanded = false },
                    )
                    data.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { categoryId = cat.id; catExpanded = false },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Daily rate (${data.settings.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("item_price_input"),
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Unit cost (${data.settings.currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Track fleet units", style = MaterialTheme.typography.titleMedium)
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }

            if (trackStock) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = {
                            val cur = parseQty(stockText) ?: 0.0
                            stockText = formatQty((cur - 1.0).coerceAtLeast(0.0))
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity")
                    }
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Available units") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            val cur = parseQty(stockText) ?: 0.0
                            stockText = formatQty(cur + 1.0)
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase quantity")
                    }
                }

                OutlinedTextField(
                    value = safetyThresholdText,
                    onValueChange = { safetyThresholdText = it },
                    label = {
                        Text("Safety alert min (blank = global ${formatQty(data.settings.lowStockThreshold)})")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_safety_threshold_input"),
                )

                val curStock = parseQty(stockText) ?: 0.0
                val effectiveThreshold = parseQty(safetyThresholdText)?.coerceAtLeast(0.0)
                    ?: data.settings.lowStockThreshold
                if (curStock < effectiveThreshold) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (curStock <= 0.0) Color(0xFFFFF3F0) else Color(0xFFFFF8E1),
                        border = BorderStroke(1.dp, if (curStock <= 0.0) LoyError.copy(alpha = 0.4f) else LoyWarning.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = if (curStock <= 0.0) LoyError else LoyWarning,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (curStock <= 0.0) {
                                    "All units rented out! Safety threshold is < ${formatQty(effectiveThreshold)}. WorkManager will notify."
                                } else {
                                    "Below safety threshold (< ${formatQty(effectiveThreshold)}). WorkManager will trigger an alert."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (curStock <= 0.0) LoyError else Color(0xFFB78103),
                            )
                        }
                    }
                }
            }

            if (data.branches.size > 1) {
                Text("Available branches (none selected = all)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.branches.forEach { branch ->
                        val sel = branch.id in selectedBranches
                        FilterChip(
                            selected = sel,
                            onClick = {
                                selectedBranches = if (sel) selectedBranches - branch.id else selectedBranches + branch.id
                            },
                            label = { Text(branch.name) },
                        )
                    }
                }
            }

            if (!isNew) {
                val count = data.itemRentalCount(product.id)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Rental Wear & Frequency", style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
                            Text("Rented $count times", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onOpenMaintenance,
                            modifier = Modifier.height(34.dp).testTag("item_schedule_service_btn"),
                        ) {
                            Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Service Schedule", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenQrUnits,
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("item_qr_units_button"),
                ) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("QR Units & Asset Tags")
                }
            }

            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isEmpty()) return@Button
                    val parsedSafety = if (safetyThresholdText.isBlank()) {
                        null
                    } else {
                        parseQty(safetyThresholdText)?.coerceAtLeast(0.0)
                    }
                    val updated = product.copy(
                        categoryId = categoryId,
                        name = cleanName,
                        priceCents = parseAmountToCents(priceText) ?: 0L,
                        costCents = parseAmountToCents(costText) ?: 0L,
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        trackStock = trackStock,
                        stock = parseQty(stockText) ?: 0.0,
                        safetyThreshold = parsedSafety,
                        branchIds = selectedBranches.joinToString(","),
                    )
                    onSave(updated, pickedPhotoUri, removeExistingPhoto)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("item_save_button"),
            ) {
                Text("Save equipment")
            }
}

@Composable
private fun CategoryEditorDialog(
    category: CategoryEntity,
    isNew: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Category" else "Edit Category") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.trim().isNotEmpty()) onSave(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Delete", color = LoyError)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
            }
        },
    )
}
