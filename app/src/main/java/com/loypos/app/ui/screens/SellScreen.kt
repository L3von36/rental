package com.loypos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatQty
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.data.local.CartLine
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.ui.CartSessionState
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.BranchPickerSheet
import com.loypos.app.ui.components.CompactSearchField
import com.loypos.app.ui.components.CustomerPickerSheet
import com.loypos.app.ui.components.DiscountBottomSheet
import com.loypos.app.ui.components.EmptyState
import com.loypos.app.ui.components.ItemThumbnail
import com.loypos.app.ui.components.LineEditorBottomSheet
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.components.StockDotBadge
import com.loypos.app.ui.components.TicketsBottomSheet
import com.loypos.app.ui.components.TopSyncIconButton
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onNavigateToCart: () -> Unit,
    onNavigateToPayment: () -> Unit,
    onNavigateToSchedule: () -> Unit = {},
) {
    var searchOpen by remember { mutableStateOf(false) }
    var showBranchSheet by remember { mutableStateOf(false) }
    var showTicketsSheet by remember { mutableStateOf(false) }
    var editingLine by remember { mutableStateOf<CartLine?>(null) }
    var showDiscountSheet by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    var showSaveTicketDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }

    val cartSession by transactionViewModel.cartSession.collectAsStateWithLifecycle()
    val totals by transactionViewModel.cartTotals.collectAsStateWithLifecycle()

    val activeBranch = data.branches.firstOrNull { it.id == session.activeBranchId }
    val branchTickets = remember(data.tickets, session.activeBranchId) {
        data.tickets.filter { it.ticket.branchId == null || it.ticket.branchId == session.activeBranchId }
    }
    val filteredItems = remember(data.items, session.activeBranchId, session.sellCategoryId, session.sellSearchQuery) {
        val q = session.sellSearchQuery.trim().lowercase()
        data.items.filter { item ->
            item.availableInBranch(session.activeBranchId) &&
                (session.sellCategoryId == null || item.categoryId == session.sellCategoryId) &&
                (q.isEmpty() ||
                    item.name.lowercase().contains(q) ||
                    item.sku.lowercase().contains(q) ||
                    item.barcode.lowercase().contains(q))
        }
    }
    val sym = data.settings.currencySymbol
    val isCashier = session.currentUser?.isCashier == true
    val windowSize = LocalLoyWindowSize.current
    val metrics = LocalLoyMetrics.current

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        TopAppBar(
            expandedHeight = metrics.topBarHeight,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            title = {
                if (searchOpen) {
                    CompactSearchField(
                        value = session.sellSearchQuery,
                        onValueChange = { viewModel.setSellSearch(it) },
                        placeholder = "Search name, SKU or barcode",
                        inputTestTag = "sell_search_input",
                    )
                } else {
                    Column {
                        Text(
                            text = data.settings.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (activeBranch != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable(enabled = !isCashier && data.branches.size > 1) {
                                        showBranchSheet = true
                                    }
                                    .testTag("sell_branch_selector"),
                            ) {
                                Icon(
                                    Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = LoyGreen,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = activeBranch.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyGreen,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onNavigateToSchedule,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("sell_schedule_button"),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Equipment Availability Calendar",
                        tint = LoyGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = {
                        if (searchOpen) {
                            searchOpen = false
                            viewModel.setSellSearch("")
                        } else {
                            searchOpen = true
                        }
                    },
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("sell_search_toggle"),
                ) {
                    Icon(
                        if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = if (searchOpen) "Close search" else "Search catalog",
                        modifier = Modifier.size(20.dp),
                    )
                }
                TopSyncIconButton(
                    syncing = session.syncing,
                    onClick = { viewModel.syncCloud() },
                )
            },
        )
        HorizontalDivider(color = LoyBorder)

        // Rental / Quote Tabs Bar
        Surface(color = LoySurface) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = metrics.screenPadding, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item {
                    val activeSelected = cartSession.openTicketId == null
                    FilterChip(
                        selected = activeSelected,
                        onClick = { if (!activeSelected) transactionViewModel.clearCart() },
                        label = {
                            Text(
                                "Rental${if (cartSession.items.isNotEmpty() && activeSelected) " (${cartSession.items.size})" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreenLight,
                            selectedLabelColor = LoyGreen,
                        ),
                        modifier = Modifier.height(30.dp),
                    )
                }
                items(branchTickets, key = { it.ticket.id }) { t ->
                    val isSel = cartSession.openTicketId == t.ticket.id
                    FilterChip(
                        selected = isSel,
                        onClick = { transactionViewModel.loadTicket(t) },
                        label = {
                            Text(
                                "${t.ticket.name.ifEmpty { "Quote" }} (${t.lines.size})",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreenLight,
                            selectedLabelColor = LoyGreen,
                        ),
                        modifier = Modifier.height(30.dp),
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showTicketsSheet = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("open_tickets_button"),
                    ) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Quotes (${branchTickets.size})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        val isSplitPane = windowSize.useSplitPane
        val columns = windowSize.gridColumns

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Category Filter Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = metrics.screenPadding, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
                ) {
                    item {
                        FilterChip(
                            selected = session.sellCategoryId == null,
                            onClick = { viewModel.setSellCategory(null) },
                            label = {
                                Text(
                                    "All (${data.items.count { it.availableInBranch(session.activeBranchId) }})",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.height(metrics.chipHeight),
                        )
                    }
                    items(data.categories, key = { it.id }) { cat ->
                        val selected = session.sellCategoryId == cat.id
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSellCategory(if (selected) null else cat.id) },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.height(metrics.chipHeight),
                        )
                    }
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Outlined.ShoppingBag,
                            title = "No matching items",
                            subtitle = if (session.sellSearchQuery.isNotEmpty()) "Try clearing your search filter." else "Add items from the Items tab.",
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = metrics.screenPadding,
                            end = metrics.screenPadding,
                            top = 2.dp,
                            bottom = 10.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(metrics.itemGap),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
                    ) {
                        gridItems(filteredItems, key = { it.id }) { item ->
                            val qtyInCart = cartSession.items.firstOrNull { it.itemId == item.id }?.qty ?: 0.0
                            ProductSaleRow(
                                item = item,
                                currencySymbol = sym,
                                qtyInCart = qtyInCart,
                                onAdd = { transactionViewModel.addProductToCart(item) },
                                onDecrement = {
                                    transactionViewModel.updateCartItemQty(item.id, (qtyInCart - 1.0).coerceAtLeast(0.0))
                                },
                            )
                        }
                    }
                }

                if (!isSplitPane) {
                    CheckoutCartSummaryCard(
                        cartSession = cartSession,
                        totals = totals,
                        currencySymbol = sym,
                        onRentalDaysChange = { transactionViewModel.setRentalDays(it) },
                        onIncrement = { line ->
                            val product = data.items.firstOrNull { it.id == line.itemId }
                            if (product != null) {
                                transactionViewModel.addProductToCart(product)
                            } else {
                                transactionViewModel.updateCartItemQty(line.itemId, line.qty + 1.0)
                            }
                        },
                        onDecrement = { line ->
                            transactionViewModel.updateCartItemQty(line.itemId, (line.qty - 1.0).coerceAtLeast(0.0))
                        },
                        onEditLine = { editingLine = it },
                        onOpenCart = onNavigateToCart,
                        onCharge = onNavigateToPayment,
                    )
                }
            }

            if (isSplitPane) {
                Surface(
                    modifier = Modifier.width(if (windowSize.isDesktop) 380.dp else 340.dp).fillMaxHeight(),
                    color = LoySurface,
                    border = BorderStroke(1.dp, LoyBorder),
                ) {
                    ReceiptPanelContent(
                        cartSession = cartSession,
                        data = data,
                        totals = totals,
                        onRentalDaysChange = { transactionViewModel.setRentalDays(it) },
                        onOpenEventDialog = { showEventDialog = true },
                        onOpenDepositDialog = { showDepositDialog = true },
                        onEditLine = { editingLine = it },
                        onOpenDiscount = { showDiscountSheet = true },
                        onOpenCustomer = { showCustomerSheet = true },
                        onOpenNote = { showNoteDialog = true },
                        onSaveTicket = { showSaveTicketDialog = true },
                        onClear = { transactionViewModel.clearCart() },
                        onCharge = onNavigateToPayment,
                    )
                }
            }
        }
    }

    if (showBranchSheet) {
        BranchPickerSheet(
            branches = data.branches,
            activeBranchId = session.activeBranchId,
            onSelect = { viewModel.selectBranch(it.id) },
            onDismiss = { showBranchSheet = false },
        )
    }

    if (showTicketsSheet) {
        TicketsBottomSheet(
            tickets = branchTickets,
            currencySymbol = sym,
            onLoad = { transactionViewModel.loadTicket(it) },
            onDelete = { transactionViewModel.deleteTicket(it) },
            onDismiss = { showTicketsSheet = false },
        )
    }

    editingLine?.let { line ->
        LineEditorBottomSheet(
            line = line,
            currencySymbol = sym,
            onUpdateQty = { transactionViewModel.updateCartItemQty(line.itemId, it) },
            onDelete = { transactionViewModel.removeFromCart(line.itemId) },
            onDismiss = { editingLine = null },
        )
    }

    if (showDiscountSheet) {
        DiscountBottomSheet(
            current = cartSession.discount,
            currencySymbol = sym,
            onApply = { transactionViewModel.setDiscount(it) },
            onDismiss = { showDiscountSheet = false },
        )
    }

    if (showCustomerSheet) {
        CustomerPickerSheet(
            customers = data.customers,
            selectedId = cartSession.customerId,
            onSelect = { transactionViewModel.setCustomer(it?.id) },
            onCreateCustomer = { name, phone ->
                val c = CustomerEntity(id = newId(), name = name, phone = phone, createdAt = nowMs())
                viewModel.saveCustomer(c)
                transactionViewModel.setCustomer(c.id)
            },
            onDismiss = { showCustomerSheet = false },
        )
    }

    if (showSaveTicketDialog) {
        val currentCustomer = data.customers.firstOrNull { it.id == cartSession.customerId }
        SaveTicketDialog(
            initialName = branchTickets.firstOrNull { it.ticket.id == cartSession.openTicketId }?.ticket?.name
                ?: cartSession.eventName.ifBlank { "Event #${branchTickets.size + 1}" },
            onSave = { name ->
                transactionViewModel.saveCurrentTicket(name, currentCustomer, session.activeBranchId)
                showSaveTicketDialog = false
            },
            onDismiss = { showSaveTicketDialog = false },
        )
    }

    if (showNoteDialog) {
        NoteInputDialog(
            initialNote = cartSession.note,
            onSave = { note ->
                transactionViewModel.setCartNote(note)
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false },
        )
    }

    if (showEventDialog) {
        EventDetailsDialog(
            initialEventName = cartSession.eventName,
            initialVenue = cartSession.eventVenue,
            onSave = { evName, venue ->
                transactionViewModel.setEventDetails(evName, venue)
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }

    if (showDepositDialog) {
        DepositDialog(
            initialDepositCents = cartSession.depositCents,
            currencySymbol = sym,
            onSave = { deposit ->
                transactionViewModel.setDepositCents(deposit)
                showDepositDialog = false
            },
            onDismiss = { showDepositDialog = false },
        )
    }
}

@Composable
private fun ProductSaleRow(
    item: ItemEntity,
    currencySymbol: String,
    qtyInCart: Double,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
) {
    val outOfStock = item.trackStock && item.stock <= 0.0
    val metrics = LocalLoyMetrics.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LoySurface,
        border = BorderStroke(1.dp, if (qtyInCart > 0) LoyGreen else LoyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (outOfStock) 0.52f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !outOfStock, onClick = onAdd)
            .testTag("product_row_${item.id}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = metrics.rowHorizontalPadding, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemThumbnail(item = item, size = 34.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LoyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (qtyInCart > 0) {
                        PillBadge(text = "×${formatQty(qtyInCart)}")
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (item.sku.isNotEmpty()) {
                        Text(
                            text = "SKU ${item.sku}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                            maxLines = 1,
                        )
                    }
                    StockDotBadge(item = item)
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (item.priceCents > 0L) "${formatCents(item.priceCents, currencySymbol)}/d" else "Set rate",
                style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                fontWeight = FontWeight.Bold,
                color = if (item.priceCents > 0L) LoyTextPrimary else LoyTextSecondary,
            )
            Spacer(Modifier.width(2.dp))
            if (qtyInCart > 0) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("product_dec_${item.id}"),
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = LoySurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Remove,
                                contentDescription = "Remove one ${item.name}",
                                tint = LoyTextPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onAdd,
                enabled = !outOfStock,
                modifier = Modifier
                    .accessibleTouchTarget(metrics.minTouchTarget)
                    .testTag("product_inc_${item.id}"),
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (outOfStock) LoySurfaceVariant else LoyGreenLight,
                    modifier = Modifier.size(26.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Add ${item.name}",
                            tint = if (outOfStock) LoyTextSecondary else LoyGreen,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutCartSummaryCard(
    cartSession: CartSessionState,
    totals: com.loypos.app.core.SaleTotals,
    currencySymbol: String,
    onRentalDaysChange: (Int) -> Unit,
    onIncrement: (CartLine) -> Unit,
    onDecrement: (CartLine) -> Unit,
    onEditLine: (CartLine) -> Unit,
    onOpenCart: () -> Unit,
    onCharge: () -> Unit,
) {
    val cartItems = cartSession.items
    var expanded by remember { mutableStateOf(false) }
    val totalUnits = cartItems.sumOf { it.qty }
    val metrics = LocalLoyMetrics.current

    Surface(
        color = LoySurface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = Modifier.fillMaxWidth().testTag("checkout_cart_summary"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Quick rental duration bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LoySurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${cartSession.rentalDays}d rental · Return ${formatShortDate(cartSession.rentalEndMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LoyTextPrimary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 3, 7).forEach { d ->
                        FilterChip(
                            selected = cartSession.rentalDays == d,
                            onClick = { onRentalDaysChange(d) },
                            label = { Text("${d}d", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(26.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            // Rental Cart Summary header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = cartItems.isNotEmpty()) { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = if (cartItems.isNotEmpty()) LoyGreen else LoyTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rental Booking Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LoyTextPrimary,
                    )
                    Text(
                        text = if (cartItems.isEmpty()) {
                            "Tap equipment above to add to booking"
                        } else {
                            "${cartItems.size} ${if (cartItems.size == 1) "tool" else "tools"} · ${formatQty(totalUnits)} units · ${cartSession.rentalDays}d"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Rental",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                    )
                    Text(
                        text = formatCents(totals.totalCents, currencySymbol),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                        fontWeight = FontWeight.Bold,
                        color = if (cartItems.isNotEmpty()) LoyGreen else LoyTextPrimary,
                        modifier = Modifier.testTag("checkout_total_cost"),
                    )
                }
                if (cartItems.isNotEmpty()) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("cart_summary_expand"),
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                            contentDescription = if (expanded) "Collapse cart summary" else "Expand cart summary",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded && cartItems.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = LoyBorder)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        items(cartItems, key = { it.itemId }) { line ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LoySurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEditLine(line) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = line.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${formatQty(line.qty)} × ${formatCents(line.priceCents, currencySymbol)}/d × ${line.rentalDays}d",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDecrement(line) },
                                        modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget),
                                    ) {
                                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease ${line.name}", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onIncrement(line) },
                                        modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget),
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Increase ${line.name}", tint = LoyGreen, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = formatCents(line.lineTotalCents, currencySymbol),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal (${cartSession.rentalDays}d)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            Text(formatCents(totals.subtotalCents, currencySymbol), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                        }
                        if (totals.discountCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", style = MaterialTheme.typography.bodySmall, color = LoyGreen)
                                Text("-${formatCents(totals.discountCents, currencySymbol)}", style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), color = LoyGreen)
                            }
                        }
                        if (totals.taxCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                Text(formatCents(totals.taxCents, currencySymbol), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                            }
                        }
                        if (cartSession.depositCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Refundable deposit", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                Text(formatCents(cartSession.depositCents, currencySymbol), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = LoyBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = metrics.screenPadding, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenCart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = metrics.minTouchTarget)
                        .testTag("view_ticket_button"),
                ) {
                    Text(
                        text = if (cartItems.isEmpty()) "Booking (0)" else "Details (${cartItems.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onCharge,
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier
                        .weight(1.35f)
                        .heightIn(min = metrics.minTouchTarget)
                        .testTag("charge_bar_button"),
                ) {
                    Text("Book ${formatCents(totals.totalCents, currencySymbol)}", maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onBack: () -> Unit,
    onNavigateToPayment: () -> Unit,
) {
    var editingLine by remember { mutableStateOf<CartLine?>(null) }
    var showDiscountSheet by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    var showSaveTicketDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }

    val cartSession by transactionViewModel.cartSession.collectAsStateWithLifecycle()
    val totals by transactionViewModel.cartTotals.collectAsStateWithLifecycle()
    val sym = data.settings.currencySymbol
    val metrics = LocalLoyMetrics.current

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                title = { Text("Rental Booking (${cartSession.items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to catalog", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ReceiptPanelContent(
                cartSession = cartSession,
                data = data,
                totals = totals,
                onRentalDaysChange = { transactionViewModel.setRentalDays(it) },
                onOpenEventDialog = { showEventDialog = true },
                onOpenDepositDialog = { showDepositDialog = true },
                onEditLine = { editingLine = it },
                onOpenDiscount = { showDiscountSheet = true },
                onOpenCustomer = { showCustomerSheet = true },
                onOpenNote = { showNoteDialog = true },
                onSaveTicket = { showSaveTicketDialog = true },
                onClear = { transactionViewModel.clearCart() },
                onCharge = onNavigateToPayment,
            )
        }
    }

    editingLine?.let { line ->
        LineEditorBottomSheet(
            line = line,
            currencySymbol = sym,
            onUpdateQty = { transactionViewModel.updateCartItemQty(line.itemId, it) },
            onDelete = { transactionViewModel.removeFromCart(line.itemId) },
            onDismiss = { editingLine = null },
        )
    }
    if (showDiscountSheet) {
        DiscountBottomSheet(
            current = cartSession.discount,
            currencySymbol = sym,
            onApply = { transactionViewModel.setDiscount(it) },
            onDismiss = { showDiscountSheet = false },
        )
    }
    if (showCustomerSheet) {
        CustomerPickerSheet(
            customers = data.customers,
            selectedId = cartSession.customerId,
            onSelect = { transactionViewModel.setCustomer(it?.id) },
            onCreateCustomer = { name, phone ->
                val c = CustomerEntity(id = newId(), name = name, phone = phone, createdAt = nowMs())
                viewModel.saveCustomer(c)
                transactionViewModel.setCustomer(c.id)
            },
            onDismiss = { showCustomerSheet = false },
        )
    }
    if (showSaveTicketDialog) {
        val currentCustomer = data.customers.firstOrNull { it.id == cartSession.customerId }
        SaveTicketDialog(
            initialName = cartSession.eventName.ifBlank { "Quote ${data.tickets.size + 1}" },
            onSave = { name ->
                transactionViewModel.saveCurrentTicket(name, currentCustomer, session.activeBranchId)
                showSaveTicketDialog = false
                onBack()
            },
            onDismiss = { showSaveTicketDialog = false },
        )
    }
    if (showNoteDialog) {
        NoteInputDialog(
            initialNote = cartSession.note,
            onSave = { note ->
                transactionViewModel.setCartNote(note)
                showNoteDialog = false
            },
            onDismiss = { showNoteDialog = false },
        )
    }
    if (showEventDialog) {
        EventDetailsDialog(
            initialEventName = cartSession.eventName,
            initialVenue = cartSession.eventVenue,
            onSave = { evName, venue ->
                transactionViewModel.setEventDetails(evName, venue)
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }
    if (showDepositDialog) {
        DepositDialog(
            initialDepositCents = cartSession.depositCents,
            currencySymbol = sym,
            onSave = { deposit ->
                transactionViewModel.setDepositCents(deposit)
                showDepositDialog = false
            },
            onDismiss = { showDepositDialog = false },
        )
    }
}

@Composable
fun ReceiptPanelContent(
    cartSession: CartSessionState,
    data: LoyPosDataState,
    totals: com.loypos.app.core.SaleTotals,
    onRentalDaysChange: (Int) -> Unit,
    onOpenEventDialog: () -> Unit,
    onOpenDepositDialog: () -> Unit,
    onEditLine: (CartLine) -> Unit,
    onOpenDiscount: () -> Unit,
    onOpenCustomer: () -> Unit,
    onOpenNote: () -> Unit,
    onSaveTicket: () -> Unit,
    onClear: () -> Unit,
    onCharge: () -> Unit,
) {
    val sym = data.settings.currencySymbol
    val customer = data.customers.firstOrNull { it.id == cartSession.customerId }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                Text(
                    text = "Rental Booking Cart",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            PillBadge(text = "${cartSession.rentalDays}d · ${cartSession.items.size} ${if (cartSession.items.size == 1) "tool" else "tools"}")
        }
        HorizontalDivider(color = LoyBorder)

        // Rental Duration Stepper row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LoySurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rental Duration: ${cartSession.rentalDays} ${if (cartSession.rentalDays == 1) "day" else "days"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatShortDate(cartSession.rentalStartMs)} → ${formatShortDate(cartSession.rentalEndMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { onRentalDaysChange((cartSession.rentalDays - 1).coerceAtLeast(1)) },
                    modifier = Modifier.size(34.dp).testTag("rental_days_minus"),
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease days", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${cartSession.rentalDays}d",
                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                    fontWeight = FontWeight.Bold,
                    color = LoyGreen,
                )
                IconButton(
                    onClick = { onRentalDaysChange(cartSession.rentalDays + 1) },
                    modifier = Modifier.size(34.dp).testTag("rental_days_plus"),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase days", modifier = Modifier.size(16.dp))
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        // Quick action chips row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                FilterChip(
                    selected = customer != null,
                    onClick = onOpenCustomer,
                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text(customer?.name ?: "Client", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.height(30.dp),
                )
            }
            item {
                FilterChip(
                    selected = cartSession.eventName.isNotEmpty(),
                    onClick = onOpenEventDialog,
                    leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = {
                        Text(
                            cartSession.eventName.ifEmpty { "Event" },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    },
                    modifier = Modifier.height(30.dp).testTag("rental_event_chip"),
                )
            }
            item {
                FilterChip(
                    selected = cartSession.depositCents > 0L,
                    onClick = onOpenDepositDialog,
                    leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = {
                        Text(
                            if (cartSession.depositCents > 0L) "Dep ${formatCents(cartSession.depositCents, sym)}" else "Deposit",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    modifier = Modifier.height(30.dp).testTag("rental_deposit_chip"),
                )
            }
            item {
                FilterChip(
                    selected = cartSession.discount != null,
                    onClick = onOpenDiscount,
                    leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = {
                        Text(
                            cartSession.discount?.let {
                                if (it.type == "percent") "${formatQty(it.value)}% off" else "-${formatCents((it.value * 100).toLong(), sym)}"
                            } ?: "Discount",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    modifier = Modifier.height(30.dp),
                )
            }
            item {
                FilterChip(
                    selected = cartSession.note.isNotEmpty(),
                    onClick = onOpenNote,
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text(if (cartSession.note.isEmpty()) "Note" else "Note ✓", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.height(30.dp),
                )
            }
            if (cartSession.items.isNotEmpty()) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = onClear,
                        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = LoyError, modifier = Modifier.size(14.dp)) },
                        label = { Text("Clear", color = LoyError, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.height(30.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        if (cartSession.items.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.ShoppingBag,
                    title = "Rental list is empty",
                    subtitle = "Tap tents, chairs, lighting, or sound gear to build an event booking.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(cartSession.items, key = { it.itemId }) { line ->
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = LoySurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditLine(line) }
                            .testTag("cart_line_${line.itemId}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatQty(line.qty)} × ${formatCents(line.priceCents, sym)}/d × ${line.rentalDays}d",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            Text(
                                text = formatCents(line.lineTotalCents, sym),
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = LoyBorder)
        Surface(color = LoySurface) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal (${cartSession.rentalDays}d)", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(formatCents(totals.subtotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                }
                if (totals.discountCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount", color = LoyGreen, style = MaterialTheme.typography.bodySmall)
                        Text("-${formatCents(totals.discountCents, sym)}", color = LoyGreen, style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                if (totals.taxCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (data.settings.taxIncluded) "Tax (included)" else "Tax (${formatQty(data.settings.taxRatePercent)}%)",
                            color = LoyTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(formatCents(totals.taxCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                if (cartSession.depositCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Security Deposit (refundable)", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(formatCents(cartSession.depositCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        formatCents(totals.totalCents, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSaveTicket,
                        enabled = cartSession.items.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("save_ticket_button"),
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Hold Quote")
                    }
                    Button(
                        onClick = onCharge,
                        enabled = cartSession.items.isNotEmpty(),
                        modifier = Modifier.weight(1.6f).height(42.dp).testTag("panel_charge_button"),
                    ) {
                        Text("Book ${formatCents(totals.totalCents, sym)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveTicketDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Rental Quote") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Event / quote name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun NoteInputDialog(
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch / Delivery Note") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Setup / pickup instructions") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onSave(note.trim()) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EventDetailsDialog(
    initialEventName: String,
    initialVenue: String,
    onSave: (eventName: String, venue: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var eventName by remember { mutableStateOf(initialEventName) }
    var venue by remember { mutableStateOf(initialVenue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Event & Venue Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Event name (e.g. Gala Reception)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_name_input"),
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Delivery Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("event_venue_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(eventName.trim(), venue.trim()) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("event_save_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DepositDialog(
    initialDepositCents: Long,
    currencySymbol: String,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var depositText by remember {
        mutableStateOf(
            if (initialDepositCents <= 0L) "" else (initialDepositCents / 100L).toString()
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refundable Security Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Recorded separately on the rental receipt and refunded after equipment inspection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 50, 100, 250).forEach { amt ->
                        FilterChip(
                            selected = depositText == amt.toString() || (amt == 0 && depositText.isBlank()),
                            onClick = { depositText = if (amt == 0) "" else amt.toString() },
                            label = { Text(if (amt == 0) "None" else "$currencySymbol$amt") },
                        )
                    }
                }
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it },
                    label = { Text("Deposit amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = parseAmountToCents(depositText) ?: 0L
                    onSave(cents.coerceAtLeast(0L))
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("deposit_save_button"),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        },
    )
}
