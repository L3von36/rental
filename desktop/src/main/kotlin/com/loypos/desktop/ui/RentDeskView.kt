package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.checkCartAvailabilityConflicts
import com.loypos.desktop.core.computeRentalTotals
import com.loypos.desktop.core.dayStartMs
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatFullLocalDate
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.localDateFromMs
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.core.parseAmountToCents
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopCartLine
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopEventKit
import com.loypos.desktop.model.DesktopHeldQuote
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.time.LocalDate

@Composable
fun RentDeskView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    selectedStartDateMs: Long? = null,
    onConsumeSelectedStartDate: () -> Unit = {},
    pendingKitToAdd: DesktopEventKit? = null,
    onConsumePendingKit: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    onOpenEventKits: () -> Unit = {},
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableIntStateOf(0) } // 0 = Grid, 1 = Table

    var cartLines by remember { mutableStateOf<List<DesktopCartLine>>(emptyList()) }
    var rentalDays by remember { mutableIntStateOf(1) }
    var rentalStartMs by remember { mutableLongStateOf(dayStartMs(LocalDate.now())) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var eventName by remember { mutableStateOf("") }
    var eventVenue by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var depositCents by remember { mutableLongStateOf(0L) }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var fixedDiscountCents by remember { mutableLongStateOf(0L) }

    LaunchedEffect(selectedStartDateMs) {
        if (selectedStartDateMs != null && selectedStartDateMs > 0L) {
            rentalStartMs = selectedStartDateMs
            onConsumeSelectedStartDate()
        }
    }

    var showClientDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showQuotesDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var completedOrderReceipt by remember { mutableStateOf<DesktopRentalOrder?>(null) }

    val selectedClient = remember(snapshot.clients, selectedClientId) {
        snapshot.clients.firstOrNull { it.id == selectedClientId }
    }

    val filteredEquipment = remember(snapshot.items, search, selectedCategoryId) {
        val q = search.trim().lowercase()
        snapshot.items.filter { item ->
            val matchCat = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val matchSearch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.sku.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q)
            matchCat && matchSearch
        }
    }

    val syncedLines = remember(cartLines, rentalDays) {
        cartLines.map { it.copy(rentalDays = rentalDays) }
    }
    val rawSubtotal = syncedLines.sumOf { it.lineTotalCents }
    val calc = remember(rawSubtotal, discountPercent, fixedDiscountCents, snapshot.settings) {
        computeRentalTotals(
            rawSubtotalCents = rawSubtotal,
            discountPercent = discountPercent,
            fixedDiscountCents = fixedDiscountCents,
            taxRatePercent = snapshot.settings.taxRatePercent,
            taxIncluded = snapshot.settings.taxIncluded,
        )
    }

    fun addEquipment(item: DesktopEquipment) {
        val existing = cartLines.firstOrNull { it.itemId == item.id }
        val nextQty = (existing?.qty ?: 0.0) + 1.0
        if (item.trackStock && nextQty > item.stock) {
            onToast("Only ${formatQty(item.stock)} units of ${item.name} available")
            return
        }
        cartLines = if (existing == null) {
            cartLines + DesktopCartLine(
                itemId = item.id,
                name = item.name,
                dailyRateCents = item.priceCents,
                qty = 1.0,
                rentalDays = rentalDays,
            )
        } else {
            cartLines.map { if (it.itemId == item.id) it.copy(qty = nextQty) else it }
        }
    }

    fun addEventKit(kit: DesktopEventKit) {
        if (kit.items.isEmpty()) {
            onToast("\"${kit.name}\" has no equipment items")
            return
        }
        val catalogById = snapshot.items.associateBy { it.id }
        var nextCart = cartLines
        var addedCount = 0
        var partialCount = 0
        kit.items.forEach { kitItem ->
            val equip = catalogById[kitItem.itemId] ?: return@forEach
            val existingQty = nextCart.firstOrNull { it.itemId == equip.id }?.qty ?: 0.0
            val addQty = if (equip.trackStock) {
                val room = (equip.stock - existingQty).coerceAtLeast(0.0)
                if (room < kitItem.qty) partialCount++
                room.coerceAtMost(kitItem.qty)
            } else {
                kitItem.qty
            }
            if (addQty > 0.0) {
                addedCount++
                nextCart = if (existingQty <= 0.0) {
                    nextCart + DesktopCartLine(
                        itemId = equip.id,
                        name = equip.name,
                        dailyRateCents = equip.priceCents,
                        qty = addQty,
                        rentalDays = rentalDays,
                    )
                } else {
                    nextCart.map {
                        if (it.itemId == equip.id) it.copy(qty = existingQty + addQty) else it
                    }
                }
            }
        }
        cartLines = nextCart
        if (kit.discountPercent > 0.0 && discountPercent == 0.0 && fixedDiscountCents == 0L) {
            discountPercent = kit.discountPercent
        }
        if (note.isBlank()) note = "Kit: ${kit.name}"
        when {
            addedCount == 0 -> onToast("No available depot stock for ${kit.name}")
            partialCount > 0 -> onToast("Added ${kit.name} ($partialCount item(s) capped by available stock)")
            else -> onToast("Added kit \"${kit.name}\" ($addedCount items)")
        }
    }

    LaunchedEffect(pendingKitToAdd) {
        if (pendingKitToAdd != null) {
            addEventKit(pendingKitToAdd)
            onConsumePendingKit()
        }
    }

    fun clearDesk() {
        cartLines = emptyList()
        rentalDays = 1
        rentalStartMs = dayStartMs(LocalDate.now())
        selectedClientId = null
        eventName = ""
        eventVenue = ""
        note = ""
        depositCents = 0L
        discountPercent = 0.0
        fixedDiscountCents = 0L
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Equipment Catalog Workspace
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopSearchInput(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = "Search tents, chairs, lighting, sound, SKU...",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopSegmentedControl(
                            options = listOf(0 to "Grid", 1 to "Table"),
                            selected = viewMode,
                            onSelect = { viewMode = it },
                        )
                        if (snapshot.heldQuotes.isNotEmpty()) {
                            DesktopSecondaryButton(
                                text = "Quotes (${snapshot.heldQuotes.size})",
                                icon = Icons.Outlined.BookmarkBorder,
                                onClick = { showQuotesDialog = true },
                            )
                        }
                        DesktopSecondaryButton(
                            text = "Calendar",
                            icon = Icons.Outlined.CalendarMonth,
                            onClick = onOpenSchedule,
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            DesktopFilterPill(
                                label = "All (${snapshot.items.size})",
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                            )
                        }
                        items(snapshot.categories, key = { it.id }) { cat ->
                            val sel = selectedCategoryId == cat.id
                            DesktopFilterPill(
                                label = cat.name,
                                selected = sel,
                                onClick = { selectedCategoryId = if (sel) null else cat.id },
                            )
                        }
                    }

                    if (snapshot.eventKits.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(Icons.Outlined.Layers, contentDescription = null, tint = LoyGreenDark, modifier = Modifier.size(14.dp))
                                Text("Quick Kits:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LoyGreenDark)
                            }
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(snapshot.eventKits, key = { it.id }) { kit ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LoyGreenLight,
                                        border = BorderStroke(1.dp, LoyGreen.copy(alpha = 0.28f)),
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable { addEventKit(kit) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, tint = LoyGreenDark, modifier = Modifier.size(13.dp))
                                            Text(kit.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyGreenDark)
                                            if (kit.discountPercent > 0.0) {
                                                Text("-${formatQty(kit.discountPercent)}%", style = MaterialTheme.typography.labelSmall, color = LoyInfo, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Manage Kits",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyGreenDark,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(onClick = onOpenEventKits),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (viewMode == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 198.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredEquipment, key = { it.id }) { item ->
                        val inCartQty = cartLines.firstOrNull { it.itemId == item.id }?.qty ?: 0.0
                        val remaining = (item.stock - inCartQty).coerceAtLeast(0.0)
                        val out = item.trackStock && remaining <= 0.0
                        val accent = categoryTileAccent(item.categoryId)
                        val interaction = remember { MutableInteractionSource() }
                        val hovered by interaction.collectIsHoveredAsState()

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (inCartQty > 0.0) LoyGreenLight.copy(alpha = 0.25f) else LoySurface,
                            border = BorderStroke(
                                width = if (inCartQty > 0.0) 1.5.dp else 1.dp,
                                color = when {
                                    inCartQty > 0.0 -> LoyGreen
                                    hovered && !out -> LoyBorderStrong
                                    else -> LoyBorder
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .hoverable(interaction)
                                .then(if (!out) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                                .clickable(enabled = !out) { addEquipment(item) },
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(accent))
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = item.sku.ifEmpty { "EQUIP" },
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = LoyTextMuted,
                                        )
                                        if (inCartQty > 0.0) {
                                            DesktopBadge("${formatQty(inCartQty)} in order", LoyGreenLight, LoyGreenDark)
                                        }
                                    }
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LoyTextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.height(36.dp),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${formatCents(item.priceCents, sym)}/d",
                                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                            color = LoyGreenDark,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = if (out) "Out" else if (!item.trackStock) "Unlim." else "${formatQty(remaining)} free",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (out) LoyError else LoyTextSecondary,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // High-Speed Dense Table Mode
                DesktopTableHeaderRow {
                    DesktopTableHeaderText("SKU", Modifier.width(86.dp))
                    DesktopTableHeaderText("Equipment", Modifier.weight(1.6f))
                    DesktopTableHeaderText("Daily Rate", Modifier.width(88.dp))
                    DesktopTableHeaderText("Free", Modifier.width(80.dp))
                    DesktopTableHeaderText("In Cart", Modifier.width(72.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                    items(filteredEquipment, key = { it.id }) { item ->
                        val inCartQty = cartLines.firstOrNull { it.itemId == item.id }?.qty ?: 0.0
                        val remaining = (item.stock - inCartQty).coerceAtLeast(0.0)
                        val out = item.trackStock && remaining <= 0.0
                        DesktopTableRow(
                            selected = inCartQty > 0.0,
                            onClick = if (!out) ({ addEquipment(item) }) else null,
                        ) {
                            Text(
                                text = item.sku.ifBlank { item.id },
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = LoyTextMuted,
                                modifier = Modifier.width(86.dp),
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.6f),
                            )
                            Text(
                                text = "${formatCents(item.priceCents, sym)}/d",
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                fontWeight = FontWeight.Bold,
                                color = LoyGreenDark,
                                modifier = Modifier.width(88.dp),
                            )
                            Text(
                                text = if (out) "0" else if (!item.trackStock) "Unlim." else formatQty(remaining),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (out) LoyError else LoyTextSecondary,
                                modifier = Modifier.width(80.dp),
                            )
                            Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterEnd) {
                                if (inCartQty > 0.0) {
                                    DesktopBadge("${formatQty(inCartQty)}×", LoyGreenLight, LoyGreenDark)
                                } else if (!out) {
                                    Text("+ Add", style = MaterialTheme.typography.labelSmall, color = LoyGreenDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right 396.dp Rental Booking Docket
        Column(
            modifier = Modifier.width(396.dp).fillMaxHeight().background(LoySurface),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Booking Docket", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (cartLines.isNotEmpty()) {
                        DesktopSecondaryButton(
                            text = "Clear",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = { clearDesk() },
                        )
                    }
                }

                // Pickup & Duration Control Box
                val dueStr = formatShortDate(rentalStartMs + rentalDays * 86_400_000L)
                val startStr = formatShortDate(rentalStartMs)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LoySurfaceHover,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$startStr → $dueStr ($rentalDays ${if (rentalDays == 1) "day" else "days"})",
                                style = MaterialTheme.typography.labelLarge,
                                color = LoyGreenDark,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DocketStepButton(Icons.Outlined.Remove) { if (rentalDays > 1) rentalDays-- }
                                DocketStepButton(Icons.Outlined.Add) { rentalDays = (rentalDays + 1).coerceAtMost(365) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 2, 3, 7, 14).forEach { preset ->
                                DesktopFilterPill(
                                    label = "${preset}d",
                                    selected = rentalDays == preset,
                                    onClick = { rentalDays = preset },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopSecondaryButton(
                        text = selectedClient?.name ?: "Select Client",
                        icon = Icons.Outlined.PersonOutline,
                        onClick = { showClientDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSecondaryButton(
                        text = eventName.ifEmpty { "Event / Deposit" },
                        icon = Icons.Outlined.Event,
                        onClick = { showEventDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            // Cart Lines Table
            if (syncedLines.isEmpty()) {
                DesktopEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = "Empty Booking Docket",
                    subtitle = "Click equipment cards or add a quick kit to begin.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                DesktopTableHeaderRow {
                    DesktopTableHeaderText("Item", Modifier.weight(1.3f))
                    DesktopTableHeaderText("Qty", Modifier.width(74.dp))
                    DesktopTableHeaderText("Subtotal", Modifier.width(76.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(syncedLines, key = { it.itemId }) { line ->
                        val equip = snapshot.items.firstOrNull { it.id == line.itemId }
                        DesktopTableRow {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${formatCents(line.dailyRateCents, sym)}/d × ${line.rentalDays}d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextMuted,
                                )
                            }
                            Row(
                                modifier = Modifier.width(74.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                DocketStepButton(Icons.Outlined.Remove) {
                                    cartLines = if (line.qty <= 1.0) {
                                        cartLines.filterNot { it.itemId == line.itemId }
                                    } else {
                                        cartLines.map { if (it.itemId == line.itemId) it.copy(qty = it.qty - 1.0) else it }
                                    }
                                }
                                Text(
                                    text = formatQty(line.qty),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(20.dp),
                                )
                                DocketStepButton(Icons.Outlined.Add) {
                                    val max = equip?.stock ?: 9999.0
                                    if (equip?.trackStock == true && line.qty + 1.0 > max) {
                                        onToast("Max available units: ${formatQty(max)}")
                                    } else {
                                        cartLines = cartLines.map {
                                            if (it.itemId == line.itemId) it.copy(qty = it.qty + 1.0) else it
                                        }
                                    }
                                }
                            }
                            Text(
                                text = formatCents(line.lineTotalCents, sym),
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(76.dp),
                            )
                        }
                    }
                }
            }

            // Financial Footer
            HorizontalDivider(color = LoyBorder)
            Column(
                modifier = Modifier.fillMaxWidth().background(LoySurfaceHover).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rental Subtotal", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(formatCents(calc.subtotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                }
                if (calc.discountCents > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount", color = LoyGreenDark, style = MaterialTheme.typography.bodySmall)
                        Text("-${formatCents(calc.discountCents, sym)}", color = LoyGreenDark, style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                if (calc.taxCents > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax (${formatQty(snapshot.settings.taxRatePercent)}%)", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(formatCents(calc.taxCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                if (depositCents > 0L) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Security Deposit", color = LoyInfo, style = MaterialTheme.typography.bodySmall)
                        Text(formatCents(depositCents, sym), color = LoyInfo, style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Total Due", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        formatCents(calc.totalCents, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        color = LoyGreenDark,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopSecondaryButton(
                        text = "Discount",
                        icon = Icons.Outlined.LocalOffer,
                        enabled = cartLines.isNotEmpty(),
                        onClick = { showDiscountDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    DesktopSecondaryButton(
                        text = "Hold Quote",
                        icon = Icons.Outlined.BookmarkBorder,
                        enabled = cartLines.isNotEmpty(),
                        onClick = {
                            val title = eventName.ifBlank { selectedClient?.name ?: "Quote ${formatShortDate(nowMs())}" }
                            repository.holdQuote(
                                DesktopHeldQuote(
                                    id = newId(),
                                    name = title,
                                    createdAt = nowMs(),
                                    rentalDays = rentalDays,
                                    customerId = selectedClient?.id,
                                    customerName = selectedClient?.name,
                                    eventName = eventName,
                                    eventVenue = eventVenue,
                                    depositCents = depositCents,
                                    discountPercent = discountPercent,
                                    discountCents = fixedDiscountCents,
                                    note = note,
                                    lines = syncedLines,
                                )
                            )
                            clearDesk()
                            onToast("Held quote \"$title\"")
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                DesktopPrimaryButton(
                    text = "Book & Collect Payment · ${formatCents(calc.totalCents, sym)}",
                    enabled = cartLines.isNotEmpty(),
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                )
            }
        }
    }

    if (showClientDialog) {
        ClientPickerDialog(
            clients = snapshot.clients,
            selectedClientId = selectedClientId,
            onSelect = { selectedClientId = it?.id; showClientDialog = false },
            onDismiss = { showClientDialog = false },
        )
    }

    if (showEventDialog) {
        EventAndDepositDialog(
            initialEvent = eventName,
            initialVenue = eventVenue,
            initialDepositCents = depositCents,
            initialNote = note,
            currencySymbol = sym,
            onSave = { ev, vn, dep, nt ->
                eventName = ev
                eventVenue = vn
                depositCents = dep
                note = nt
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }

    if (showDiscountDialog) {
        DiscountDialog(
            initialPercent = discountPercent,
            initialFixedCents = fixedDiscountCents,
            currencySymbol = sym,
            onApply = { pct, fix ->
                discountPercent = pct
                fixedDiscountCents = fix
                showDiscountDialog = false
            },
            onDismiss = { showDiscountDialog = false },
        )
    }

    if (showQuotesDialog) {
        AlertDialog(
            onDismissRequest = { showQuotesDialog = false },
            title = { Text("Held Rental Quotes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(420.dp)) {
                    snapshot.heldQuotes.forEach { q ->
                        val qTotal = q.lines.sumOf { it.lineTotalCents }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LoyBackground,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable {
                                    cartLines = q.lines
                                    rentalDays = q.rentalDays
                                    selectedClientId = q.customerId
                                    eventName = q.eventName
                                    eventVenue = q.eventVenue
                                    depositCents = q.depositCents
                                    discountPercent = q.discountPercent
                                    fixedDiscountCents = q.discountCents
                                    note = q.note
                                    repository.deleteQuote(q.id)
                                    showQuotesDialog = false
                                    onToast("Resumed \"${q.name}\"")
                                },
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(q.name, fontWeight = FontWeight.SemiBold)
                                    Text("${q.lines.size} items · ${q.rentalDays}d · ${formatCents(qTotal, sym)}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyError,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable { repository.deleteQuote(q.id) }
                                        .padding(6.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQuotesDialog = false }) { Text("Close") } },
        )
    }

    if (showCheckoutDialog) {
        DesktopCheckoutDialog(
            repository = repository,
            snapshot = snapshot,
            lines = syncedLines,
            initialStartMs = rentalStartMs,
            initialDays = rentalDays,
            initialClientId = selectedClientId,
            initialEventName = eventName,
            initialVenue = eventVenue,
            initialDepositCents = depositCents,
            initialNote = note,
            discountPercent = discountPercent,
            fixedDiscountCents = fixedDiscountCents,
            onConfirm = { startMs, days, client, evName, evVenue, depCents, orderNote, method, paid ->
                val order = repository.checkoutRental(
                    lines = linesWithDays(syncedLines, days),
                    rentalDays = days,
                    paymentType = method,
                    paidCents = paid,
                    discountPercent = discountPercent,
                    fixedDiscountCents = fixedDiscountCents,
                    depositCents = depCents,
                    client = client,
                    eventName = evName,
                    eventVenue = evVenue,
                    note = orderNote,
                    rentalStartMs = startMs,
                )
                showCheckoutDialog = false
                clearDesk()
                completedOrderReceipt = order
                onToast("Booked rental #${order.number.toString().padStart(4, '0')}")
            },
            onDismiss = { showCheckoutDialog = false },
        )
    }

    completedOrderReceipt?.let { order ->
        val receiptText = remember(order, snapshot.settings) {
            repository.buildPlainTextReceipt(order, snapshot.settings)
        }
        AlertDialog(
            onDismissRequest = { completedOrderReceipt = null },
            title = { Text("Rental Confirmed · #${order.number.toString().padStart(4, '0')}") },
            text = {
                Surface(color = LoyBackground, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, LoyBorder)) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            },
            confirmButton = { Button(onClick = { completedOrderReceipt = null }) { Text("Done") } },
        )
    }
}

@Composable
private fun DocketStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LoySurface,
        border = BorderStroke(1.dp, LoyBorderStrong),
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun ClientPickerDialog(
    clients: List<DesktopClient>,
    selectedClientId: String?,
    onSelect: (DesktopClient?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Client / Organizer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(380.dp).heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelect(null) }) { Text("Walk-in Client (None)") }
                clients.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (c.id == selectedClientId) LoyGreenLight else LoyBackground,
                        border = BorderStroke(1.dp, if (c.id == selectedClientId) LoyGreen else LoyBorder),
                        modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand).clickable { onSelect(c) },
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            if (c.phone.isNotEmpty() || c.email.isNotEmpty()) {
                                Text(listOf(c.phone, c.email).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun EventAndDepositDialog(
    initialEvent: String,
    initialVenue: String,
    initialDepositCents: Long,
    initialNote: String,
    currencySymbol: String,
    onSave: (String, String, Long, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var ev by remember { mutableStateOf(initialEvent) }
    var vn by remember { mutableStateOf(initialVenue) }
    var dep by remember { mutableStateOf(if (initialDepositCents == 0L) "" else (initialDepositCents / 100.0).toString()) }
    var nt by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Event Details & Security Deposit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(400.dp)) {
                DesktopFormInput("Event Name", ev, { ev = it }, placeholder = "e.g. Summer Gala")
                DesktopFormInput("Venue / Delivery Location", vn, { vn = it })
                DesktopFormInput("Refundable Deposit ($currencySymbol)", dep, { dep = it })
                DesktopFormInput("Dispatch Note", nt, { nt = it })
            }
        },
        confirmButton = { Button(onClick = { onSave(ev, vn, parseAmountToCents(dep) ?: 0L, nt) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DiscountDialog(
    initialPercent: Double,
    initialFixedCents: Long,
    currencySymbol: String,
    onApply: (Double, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var pct by remember { mutableStateOf(if (initialPercent == 0.0) "" else formatQty(initialPercent)) }
    var fix by remember { mutableStateOf(if (initialFixedCents == 0L) "" else (initialFixedCents / 100.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rental Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(360.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5.0, 10.0, 15.0, 20.0).forEach { p ->
                        DesktopFilterPill(
                            label = "${p.toInt()}%",
                            selected = pct.toDoubleOrNull() == p,
                            onClick = { pct = formatQty(p) },
                        )
                    }
                }
                DesktopFormInput("Percentage (%)", pct, { pct = it })
                DesktopFormInput("Fixed Amount ($currencySymbol)", fix, { fix = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(pct.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0, parseAmountToCents(fix) ?: 0L)
            }) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onApply(0.0, 0L) }) { Text("Clear", color = LoyError) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun linesWithDays(lines: List<DesktopCartLine>, days: Int): List<DesktopCartLine> =
    lines.map { it.copy(rentalDays = days.coerceAtLeast(1)) }

@Composable
private fun DesktopCheckoutDialog(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    lines: List<DesktopCartLine>,
    initialStartMs: Long,
    initialDays: Int,
    initialClientId: String?,
    initialEventName: String,
    initialVenue: String,
    initialDepositCents: Long,
    initialNote: String,
    discountPercent: Double,
    fixedDiscountCents: Long,
    onConfirm: (
        startMs: Long,
        days: Int,
        client: DesktopClient?,
        eventName: String,
        venue: String,
        depositCents: Long,
        note: String,
        paymentType: String,
        paidCents: Long,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    val today = remember { LocalDate.now() }
    var pickupDate by remember { mutableStateOf(localDateFromMs(initialStartMs)) }
    var days by remember { mutableIntStateOf(initialDays.coerceAtLeast(1)) }
    var clientId by remember { mutableStateOf(initialClientId) }
    var showNewClientForm by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientPhone by remember { mutableStateOf("") }
    var evName by remember { mutableStateOf(initialEventName) }
    var evVenue by remember { mutableStateOf(initialVenue) }
    var depText by remember {
        mutableStateOf(if (initialDepositCents <= 0L) "" else (initialDepositCents / 100L).toString())
    }
    var orderNote by remember { mutableStateOf(initialNote) }

    val activeLines = remember(lines, days) { linesWithDays(lines, days) }
    val rawSubtotal = activeLines.sumOf { it.lineTotalCents }
    val calc = remember(rawSubtotal, discountPercent, fixedDiscountCents, snapshot.settings) {
        computeRentalTotals(
            rawSubtotalCents = rawSubtotal,
            discountPercent = discountPercent,
            fixedDiscountCents = fixedDiscountCents,
            taxRatePercent = snapshot.settings.taxRatePercent,
            taxIncluded = snapshot.settings.taxIncluded,
        )
    }
    val depCents = parseAmountToCents(depText) ?: 0L
    val startMs = dayStartMs(pickupDate)
    val endMs = startMs + days * 86_400_000L
    val conflicts = remember(activeLines, startMs, endMs, snapshot.items, snapshot.orders) {
        checkCartAvailabilityConflicts(activeLines, snapshot.items, snapshot.orders, startMs, endMs)
    }

    var method by remember { mutableStateOf("cash") }
    var tenderedText by remember(calc.totalCents) {
        mutableStateOf(
            if (calc.totalCents % 100L == 0L) (calc.totalCents / 100L).toString() else "%.2f".format(calc.totalCents / 100.0)
        )
    }
    val tenderedCents = parseAmountToCents(tenderedText) ?: 0L
    val changeCents = (tenderedCents - calc.totalCents).coerceAtLeast(0L)
    val selectedClient = snapshot.clients.firstOrNull { it.id == clientId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Order Checkout & Confirmation", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.width(620.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DesktopSectionCard(title = "1. Rental Duration & Schedule") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Pickup: ${formatFullLocalDate(pickupDate)}", fontWeight = FontWeight.SemiBold)
                            Text("Return Due: ${formatFullLocalDate(pickupDate.plusDays(days.toLong()))} ($days ${if (days == 1) "day" else "days"})", style = MaterialTheme.typography.bodySmall, color = LoyGreenDark)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DesktopSecondaryButton("-1d", onClick = { pickupDate = pickupDate.minusDays(1) })
                            DesktopSecondaryButton("Today", onClick = { pickupDate = today })
                            DesktopSecondaryButton("+1d", onClick = { pickupDate = pickupDate.plusDays(1) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Duration:", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        listOf(1, 2, 3, 5, 7, 14).forEach { preset ->
                            DesktopFilterPill("${preset}d", days == preset, { days = preset })
                        }
                        DocketStepButton(Icons.Outlined.Remove) { if (days > 1) days-- }
                        DocketStepButton(Icons.Outlined.Add) { days = (days + 1).coerceAtMost(365) }
                    }
                    if (conflicts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LoyWarningLight,
                            border = BorderStroke(1.dp, LoyWarning.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LoyWarning, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("Availability Conflict", fontWeight = FontWeight.Bold, color = LoyWarning, style = MaterialTheme.typography.labelMedium)
                                    conflicts.forEach { c ->
                                        Text("${c.itemName}: need ${formatQty(c.requestedQty)}, ${formatQty(c.availableQty)} free on these dates", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                DesktopSectionCard(
                    title = "2. Customer & Event Details",
                    headerAction = {
                        DesktopSecondaryButton(
                            text = if (showNewClientForm) "Pick Existing" else "+ New Client",
                            onClick = { showNewClientForm = !showNewClientForm },
                        )
                    },
                ) {
                    if (showNewClientForm) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            DesktopFormInput("Client / Company *", newClientName, { newClientName = it }, modifier = Modifier.weight(1.2f))
                            DesktopFormInput("Phone", newClientPhone, { newClientPhone = it }, modifier = Modifier.weight(1f))
                            DesktopPrimaryButton(
                                text = "Add",
                                enabled = newClientName.trim().isNotEmpty(),
                                onClick = {
                                    val created = DesktopClient(id = newId(), name = newClientName.trim(), phone = newClientPhone.trim(), createdAt = nowMs())
                                    repository.saveClient(created)
                                    clientId = created.id
                                    newClientName = ""
                                    newClientPhone = ""
                                    showNewClientForm = false
                                },
                            )
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                DesktopFilterPill("Walk-in", clientId == null, { clientId = null })
                            }
                            items(snapshot.clients, key = { it.id }) { client ->
                                DesktopFilterPill(client.name, clientId == client.id, { clientId = client.id })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopFormInput("Event Name", evName, { evName = it }, modifier = Modifier.weight(1f))
                        DesktopFormInput("Venue / Delivery Address", evVenue, { evVenue = it }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopFormInput("Refundable Deposit ($sym)", depText, { depText = it }, modifier = Modifier.weight(0.7f))
                        DesktopFormInput("Delivery / Setup Note", orderNote, { orderNote = it }, modifier = Modifier.weight(1.3f))
                    }
                }

                DesktopSectionCard(title = "3. Equipment Costs (${activeLines.size} items)") {
                    activeLines.forEach { l ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${l.name} (${formatQty(l.qty)} × ${formatCents(l.dailyRateCents, sym)}/d × ${l.rentalDays}d)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            Text(formatCents(l.lineTotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = LoyBorder)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Rental Cost", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(formatCents(calc.totalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge), fontWeight = FontWeight.Bold, color = LoyGreenDark)
                    }
                    HorizontalDivider(color = LoyBorder)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DesktopSegmentedControl(
                            options = listOf("cash" to "Cash", "card" to "Card / Transfer"),
                            selected = method,
                            onSelect = { method = it },
                        )
                        if (method == "cash") {
                            Spacer(Modifier.width(8.dp))
                            DesktopFormInput("Received ($sym)", tenderedText, { tenderedText = it }, modifier = Modifier.weight(1f))
                            Text("Change ${formatCents(changeCents, sym)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = if (method == "card") calc.totalCents else tenderedCents
                    onConfirm(
                        startMs,
                        days,
                        selectedClient,
                        evName.trim(),
                        evVenue.trim(),
                        depCents,
                        orderNote.trim(),
                        method,
                        paid,
                    )
                },
                enabled = activeLines.isNotEmpty() && (method == "card" || tenderedCents >= calc.totalCents),
                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
            ) {
                Text("Confirm & Charge ${formatCents(calc.totalCents, sym)}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
