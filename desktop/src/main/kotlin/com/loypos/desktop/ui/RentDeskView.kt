package com.loypos.desktop.ui

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
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
        // Left Equipment Catalog Panel
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search tents, chairs, lights, sound, SKU...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                if (search.isNotEmpty()) {
                                    IconButton(onClick = { search = "" }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (snapshot.heldQuotes.isNotEmpty()) {
                            OutlinedButton(onClick = { showQuotesDialog = true }) {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Held Quotes (${snapshot.heldQuotes.size})")
                            }
                        }
                        OutlinedButton(onClick = onOpenSchedule) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = LoyGreenDark)
                            Spacer(Modifier.width(6.dp))
                            Text("Calendar")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("All (${snapshot.items.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LoyGreenLight,
                                    selectedLabelColor = LoyGreenDark,
                                ),
                            )
                        }
                        items(snapshot.categories, key = { it.id }) { cat ->
                            val sel = selectedCategoryId == cat.id
                            FilterChip(
                                selected = sel,
                                onClick = { selectedCategoryId = if (sel) null else cat.id },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LoyGreenLight,
                                    selectedLabelColor = LoyGreenDark,
                                ),
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
                                Icon(Icons.Outlined.Layers, contentDescription = null, tint = LoyGreenDark, modifier = Modifier.size(16.dp))
                                Text("Event Kits:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LoyGreenDark)
                            }
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(snapshot.eventKits, key = { it.id }) { kit ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LoyGreenLight.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, LoyGreen.copy(alpha = 0.35f)),
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { addEventKit(kit) },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, tint = LoyGreenDark, modifier = Modifier.size(14.dp))
                                            Text(kit.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyGreenDark)
                                            if (kit.discountPercent > 0.0) {
                                                Text("-${formatQty(kit.discountPercent)}%", style = MaterialTheme.typography.labelSmall, color = LoyInfo, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = onOpenEventKits, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("Manage Kits", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 205.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredEquipment, key = { it.id }) { item ->
                    val inCartQty = cartLines.firstOrNull { it.itemId == item.id }?.qty ?: 0.0
                    val remaining = (item.stock - inCartQty).coerceAtLeast(0.0)
                    val out = item.trackStock && remaining <= 0.0
                    val accent = categoryTileAccent(item.categoryId)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, if (inCartQty > 0.0) LoyGreen else LoyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !out) { addEquipment(item) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accent.copy(alpha = 0.14f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = item.sku.ifEmpty { "EQUIP" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (inCartQty > 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(LoyGreen)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "${formatQty(inCartQty)} added",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
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
                                    text = if (out) "Rented out" else "${formatQty(remaining)} avail",
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

        // Right Rental Booking Panel
        Surface(
            color = LoySurface,
            border = BorderStroke(1.dp, LoyBorder),
            modifier = Modifier.width(420.dp).fillMaxHeight(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header + Client & Event chips
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Rental Booking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (cartLines.isNotEmpty()) {
                            TextButton(onClick = { clearDesk() }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = LoyError)
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", color = LoyError)
                            }
                        }
                    }

                    // Duration & Pickup Date Bar
                    val dueStr = formatShortDate(rentalStartMs + rentalDays * 86_400_000L)
                    val startStr = formatShortDate(rentalStartMs)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoyGreenLight.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, LoyGreen.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$startStr → $dueStr · $rentalDays ${if (rentalDays == 1) "day" else "days"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LoyGreenDark,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (rentalDays > 1) rentalDays-- },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease days", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { rentalDays = (rentalDays + 1).coerceAtMost(365) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = "Increase days", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 2, 3, 7, 14).forEach { preset ->
                                    FilterChip(
                                        selected = rentalDays == preset,
                                        onClick = { rentalDays = preset },
                                        label = { Text("${preset}d") },
                                        modifier = Modifier.height(28.dp),
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showClientDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedClient?.name ?: "Select Client", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { showEventDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(eventName.ifEmpty { "Event & Deposit" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                HorizontalDivider(color = LoyBorder)

                // Cart Lines
                if (syncedLines.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(34.dp))
                            Text("No equipment selected", style = MaterialTheme.typography.titleSmall, color = LoyTextSecondary)
                            Text("Click tools on the left to add them to this rental.", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(syncedLines, key = { it.itemId }) { line ->
                            val equip = snapshot.items.firstOrNull { it.id == line.itemId }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoyBackground,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${formatQty(line.qty)} × ${formatCents(line.dailyRateCents, sym)}/d × ${line.rentalDays}d",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                cartLines = if (line.qty <= 1.0) {
                                                    cartLines.filterNot { it.itemId == line.itemId }
                                                } else {
                                                    cartLines.map { if (it.itemId == line.itemId) it.copy(qty = it.qty - 1.0) else it }
                                                }
                                            },
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease qty", modifier = Modifier.size(16.dp))
                                        }
                                        Text(formatQty(line.qty), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(
                                            onClick = {
                                                val max = equip?.stock ?: 9999.0
                                                if (equip?.trackStock == true && line.qty + 1.0 > max) {
                                                    onToast("Max available units: ${formatQty(max)}")
                                                } else {
                                                    cartLines = cartLines.map {
                                                        if (it.itemId == line.itemId) it.copy(qty = it.qty + 1.0) else it
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = "Increase qty", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = formatCents(line.lineTotalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Summary & Action Buttons
                HorizontalDivider(color = LoyBorder)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rental Subtotal", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(formatCents(calc.subtotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                    if (calc.discountCents > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount", color = LoyGreen, style = MaterialTheme.typography.bodySmall)
                            Text("-${formatCents(calc.discountCents, sym)}", color = LoyGreen, style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
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
                            Text("Security Deposit (Refundable)", color = LoyInfo, style = MaterialTheme.typography.bodySmall)
                            Text(formatCents(depositCents, sym), color = LoyInfo, style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            formatCents(calc.totalCents, sym),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                            color = LoyGreenDark,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDiscountDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = cartLines.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Discount")
                        }
                        OutlinedButton(
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
                            enabled = cartLines.isNotEmpty(),
                        ) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hold Quote")
                        }
                    }

                    Button(
                        onClick = { showCheckoutDialog = true },
                        enabled = cartLines.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) {
                        Text("Book & Collect Payment · ${formatCents(calc.totalCents, sym)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialogs
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
                            shape = RoundedCornerShape(10.dp),
                            color = LoyBackground,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier.fillMaxWidth().clickable {
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
                                IconButton(onClick = { repository.deleteQuote(q.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete quote", tint = LoyError)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuotesDialog = false }) { Text("Close") }
            },
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
                Surface(color = LoyBackground, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, LoyBorder)) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            },
            confirmButton = {
                Button(onClick = { completedOrderReceipt = null }) { Text("New Rental") }
            },
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(380.dp)) {
                TextButton(onClick = { onSelect(null) }) {
                    Text("Walk-in Client (None)")
                }
                clients.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (c.id == selectedClientId) LoyGreenLight else LoyBackground,
                        border = BorderStroke(1.dp, if (c.id == selectedClientId) LoyGreen else LoyBorder),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(c) },
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(400.dp)) {
                OutlinedTextField(value = ev, onValueChange = { ev = it }, label = { Text("Event name (e.g. Summer Gala)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = vn, onValueChange = { vn = it }, label = { Text("Venue / Delivery location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = dep,
                    onValueChange = { dep = it },
                    label = { Text("Refundable deposit ($currencySymbol)") },
                    leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = nt, onValueChange = { nt = it }, label = { Text("Dispatch note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ev, vn, parseAmountToCents(dep) ?: 0L, nt) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(360.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5.0, 10.0, 15.0, 20.0).forEach { p ->
                        FilterChip(
                            selected = pct.toDoubleOrNull() == p,
                            onClick = { pct = formatQty(p) },
                            label = { Text("${p.toInt()}%") },
                        )
                    }
                }
                OutlinedTextField(value = pct, onValueChange = { pct = it }, label = { Text("Percentage (%)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fix, onValueChange = { fix = it }, label = { Text("Fixed amount ($currencySymbol)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                modifier = Modifier.width(620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Rental Duration & Pickup Date
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Rental Duration & Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Pickup: ${formatFullLocalDate(pickupDate)}", fontWeight = FontWeight.SemiBold)
                                Text("Return Due: ${formatFullLocalDate(pickupDate.plusDays(days.toLong()))} ($days ${if (days == 1) "day" else "days"})", style = MaterialTheme.typography.bodySmall, color = LoyGreenDark)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { pickupDate = pickupDate.minusDays(1) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("-1d") }
                                OutlinedButton(onClick = { pickupDate = today }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Today") }
                                OutlinedButton(onClick = { pickupDate = pickupDate.plusDays(1) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+1d") }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration:", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            listOf(1, 2, 3, 5, 7, 14).forEach { preset ->
                                FilterChip(
                                    selected = days == preset,
                                    onClick = { days = preset },
                                    label = { Text("${preset}d") },
                                    modifier = Modifier.height(28.dp),
                                )
                            }
                            IconButton(onClick = { if (days > 1) days-- }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Remove, contentDescription = "Decrease rental days", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { days = (days + 1).coerceAtMost(365) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Add, contentDescription = "Increase rental days", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (conflicts.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LoyWarningLight,
                                border = BorderStroke(1.dp, LoyWarning.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LoyWarning, modifier = Modifier.size(18.dp))
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
                }

                // 2. Customer & Event Details
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("2. Customer & Event Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showNewClientForm = !showNewClientForm }) {
                                Text(if (showNewClientForm) "Pick Existing Client" else "+ New Client")
                            }
                        }
                        if (showNewClientForm) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = newClientName, onValueChange = { newClientName = it }, label = { Text("Client / Company Name") }, singleLine = true, modifier = Modifier.weight(1.2f))
                                OutlinedTextField(value = newClientPhone, onValueChange = { newClientPhone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        val created = DesktopClient(id = newId(), name = newClientName.trim(), phone = newClientPhone.trim(), createdAt = nowMs())
                                        repository.saveClient(created)
                                        clientId = created.id
                                        newClientName = ""
                                        newClientPhone = ""
                                        showNewClientForm = false
                                    },
                                    enabled = newClientName.trim().isNotEmpty(),
                                ) {
                                    Text("Add")
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = clientId == null,
                                        onClick = { clientId = null },
                                        label = { Text("Walk-in") },
                                    )
                                }
                                items(snapshot.clients, key = { it.id }) { client ->
                                    FilterChip(
                                        selected = clientId == client.id,
                                        onClick = { clientId = client.id },
                                        label = { Text(client.name) },
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = evName, onValueChange = { evName = it }, label = { Text("Event Name") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = evVenue, onValueChange = { evVenue = it }, label = { Text("Venue / Delivery Address") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = depText, onValueChange = { depText = it }, label = { Text("Refundable Deposit ($sym)") }, singleLine = true, modifier = Modifier.weight(0.7f))
                            OutlinedTextField(value = orderNote, onValueChange = { orderNote = it }, label = { Text("Delivery / Setup Note") }, singleLine = true, modifier = Modifier.weight(1.3f))
                        }
                    }
                }

                // 3. Equipment Costs & Payment
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoyBackground,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Equipment Costs (${activeLines.size} items)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        activeLines.forEach { l ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${l.name} (${formatQty(l.qty)} × ${formatCents(l.dailyRateCents, sym)}/d × ${l.rentalDays}d)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                Text(formatCents(l.lineTotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = LoyBorder)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal ($days days)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            Text(formatCents(calc.subtotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                        }
                        if (calc.discountCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", style = MaterialTheme.typography.bodySmall, color = LoyGreenDark)
                                Text("-${formatCents(calc.discountCents, sym)}", style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), color = LoyGreenDark)
                            }
                        }
                        if (calc.taxCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tax", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                Text(formatCents(calc.taxCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                            }
                        }
                        if (depCents > 0L) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Refundable Security Deposit", style = MaterialTheme.typography.bodySmall, color = LoyInfo)
                                Text(formatCents(depCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), color = LoyInfo)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Rental Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(formatCents(calc.totalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge), fontWeight = FontWeight.Bold, color = LoyGreenDark)
                        }

                        HorizontalDivider(color = LoyBorder)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = method == "cash",
                                onClick = { method = "cash" },
                                leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Cash") },
                            )
                            FilterChip(
                                selected = method == "card",
                                onClick = { method = "card" },
                                leadingIcon = { Icon(Icons.Outlined.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Card / Transfer") },
                            )
                            if (method == "cash") {
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = tenderedText,
                                    onValueChange = { tenderedText = it },
                                    label = { Text("Received ($sym)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("Change ${formatCents(changeCents, sym)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
