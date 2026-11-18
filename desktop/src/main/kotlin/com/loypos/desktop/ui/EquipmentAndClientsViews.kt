package com.loypos.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.loypos.desktop.core.DesktopRentalAgreementSearchResult
import com.loypos.desktop.core.DesktopRentalAgreementTimeframe
import com.loypos.desktop.core.DesktopRentalReturnReminderCalculator
import com.loypos.desktop.core.DesktopRentalTimeframeFilter
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatDateTime
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.newId
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.core.parseAmountToCents
import com.loypos.desktop.core.searchDesktopRentalAgreements
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopClient
import com.loypos.desktop.model.DesktopEquipment
import com.loypos.desktop.model.DesktopRentalOrder
import com.loypos.desktop.model.DesktopStoreSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

@Composable
fun DesktopDashboardView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onOpenRentDesk: () -> Unit,
    onOpenRentals: () -> Unit,
    onOpenDueRentals: () -> Unit = onOpenRentals,
    onOpenEquipment: () -> Unit,
    onOpenSchedule: () -> Unit = {},
    onOpenQrTracking: () -> Unit = {},
    onOpenAgreements: () -> Unit = {},
    onOpenEventKits: () -> Unit = {},
    onOpenDamageLog: () -> Unit = {},
    onReportDamage: (DesktopRentalOrder) -> Unit = {},
    onToast: (String) -> Unit = {},
) {
    val sym = snapshot.settings.currencySymbol
    var agreementSearchQuery by remember { mutableStateOf("") }
    var agreementTimeframeFilter by remember { mutableStateOf(DesktopRentalTimeframeFilter.ALL) }
    var inspectedOrderId by remember { mutableStateOf<String?>(null) }
    var inspectedQuoteResult by remember { mutableStateOf<DesktopRentalAgreementSearchResult?>(null) }
    val now = remember(snapshot.orders) { nowMs() }
    val completed = remember(snapshot.orders) { snapshot.orders.filter { it.isCompleted } }
    val dueWithin24h = remember(snapshot.orders, now) {
        DesktopRentalReturnReminderCalculator.findRentalsDueWithin24Hours(snapshot.orders, now)
    }
    val due24hIds = remember(dueWithin24h) { dueWithin24h.map { it.order.id }.toSet() }
    val inspectedOrder = remember(snapshot.orders, inspectedOrderId) {
        inspectedOrderId?.let { id -> snapshot.orders.firstOrNull { it.id == id } }
    }
    val showInspector = inspectedOrder != null || inspectedQuoteResult != null

    val agreementSearchOutput = remember(
        agreementSearchQuery,
        agreementTimeframeFilter,
        snapshot.orders,
        snapshot.heldQuotes,
        snapshot.items,
        snapshot.clients,
        snapshot.equipmentUnits,
        snapshot.scanLogs,
        now,
    ) {
        searchDesktopRentalAgreements(
            query = agreementSearchQuery,
            timeframeFilter = agreementTimeframeFilter,
            orders = snapshot.orders,
            heldQuotes = snapshot.heldQuotes,
            items = snapshot.items,
            clients = snapshot.clients,
            equipmentUnits = snapshot.equipmentUnits,
            scanLogs = snapshot.scanLogs,
            nowMs = now,
        )
    }
    val netRevenueCents = completed.sumOf { it.totalCents }
    val activeOutCount = completed.count { it.isRentalOut }
    val overdueCount = completed.count { it.isOverdue(now) }
    val lowThreshold = snapshot.settings.lowStockThreshold
    val lowOrOutItems = remember(snapshot.items, lowThreshold) {
        snapshot.items.filter { it.isBelowSafetyThreshold(lowThreshold) }
            .sortedBy { it.stock - it.effectiveSafetyThreshold(lowThreshold) }
    }
    val monthlyPoints = remember(snapshot.orders) {
        val zone = java.time.ZoneId.systemDefault()
        val nowYm = java.time.YearMonth.now(zone)
        val grouped = completed.groupBy {
            java.time.YearMonth.from(java.time.Instant.ofEpochMilli(it.createdAt).atZone(zone))
        }
        (5 downTo 0).map { offset ->
            val ym = nowYm.minusMonths(offset.toLong())
            val orders = grouped[ym].orEmpty()
            Triple(
                ym.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                orders.sumOf { it.totalCents },
                orders.size,
            )
        }
    }
    val utilizationRows = remember(snapshot.items, snapshot.categories, completed) {
        val outById = mutableMapOf<String, Double>()
        completed.filter { it.isRentalOut }.forEach { o ->
            o.lines.forEach { l -> l.itemId?.let { outById[it] = (outById[it] ?: 0.0) + l.qty } }
        }
        val catNames = snapshot.categories.associate { it.id to it.name }
        snapshot.items.filter { it.trackStock }.groupBy { it.categoryId ?: "other" }.map { (catId, group) ->
            val out = group.sumOf { outById[it.id] ?: 0.0 }
            val cap = group.sumOf { (it.stock + (outById[it.id] ?: 0.0)).coerceAtLeast(0.0) }
            val pct = if (cap <= 0.0) 0.0 else (out / cap * 100.0).coerceIn(0.0, 100.0)
            Triple(catId, catNames[catId] ?: "Other", pct to "${formatQty(out)}/${formatQty(cap)}")
        }.sortedByDescending { it.third.first }
    }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val useFourColKpis = maxWidth >= 800.dp
            val useTwoColPanels = maxWidth >= 820.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Responsive KPI Grid (4 columns when wide, 2x2 when docked inspector is open)
                if (useFourColKpis) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        DesktopKpiTile(
                            label = "Net Rental Revenue",
                            value = formatCents(netRevenueCents, sym),
                            accentColor = LoyGreen,
                            caption = "${completed.size} completed agreements",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopKpiTile(
                            label = "Active Rentals Out",
                            value = activeOutCount.toString(),
                            accentColor = LoyInfo,
                            caption = "${snapshot.heldQuotes.size} held quotes open",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopKpiTile(
                            label = "Due <24h / Overdue",
                            value = "${dueWithin24h.size} / $overdueCount",
                            accentColor = if (overdueCount > 0) LoyError else if (dueWithin24h.isNotEmpty()) LoyWarning else LoyGreen,
                            caption = if (dueWithin24h.isEmpty()) "All returns on schedule" else "Click top monitor to filter",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopKpiTile(
                            label = "Low Fleet Alerts",
                            value = lowOrOutItems.size.toString(),
                            accentColor = if (lowOrOutItems.isNotEmpty()) LoyWarning else LoyGreen,
                            caption = "Global threshold: ${formatQty(lowThreshold)} units",
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            DesktopKpiTile(
                                label = "Net Rental Revenue",
                                value = formatCents(netRevenueCents, sym),
                                accentColor = LoyGreen,
                                caption = "${completed.size} completed agreements",
                                modifier = Modifier.weight(1f),
                            )
                            DesktopKpiTile(
                                label = "Active Rentals Out",
                                value = activeOutCount.toString(),
                                accentColor = LoyInfo,
                                caption = "${snapshot.heldQuotes.size} held quotes open",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            DesktopKpiTile(
                                label = "Due <24h / Overdue",
                                value = "${dueWithin24h.size} / $overdueCount",
                                accentColor = if (overdueCount > 0) LoyError else if (dueWithin24h.isNotEmpty()) LoyWarning else LoyGreen,
                                caption = if (dueWithin24h.isEmpty()) "All returns on schedule" else "Click top monitor to filter",
                                modifier = Modifier.weight(1f),
                            )
                            DesktopKpiTile(
                                label = "Low Fleet Alerts",
                                value = lowOrOutItems.size.toString(),
                                accentColor = if (lowOrOutItems.isNotEmpty()) LoyWarning else LoyGreen,
                                caption = "Global threshold: ${formatQty(lowThreshold)} units",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Global Rental Agreement Search Card
                DesktopSectionCard(
                    title = "Find Rental Agreements",
                    subtitle = "${agreementSearchOutput.allCount} total · ${agreementSearchOutput.currentCount} current · ${agreementSearchOutput.futureCount} future · ${agreementSearchOutput.pastCount} past",
                    headerAction = {
                        DesktopSegmentedControl(
                            options = listOf(
                                DesktopRentalTimeframeFilter.ALL to "All (${agreementSearchOutput.allCount})",
                                DesktopRentalTimeframeFilter.CURRENT to "Current (${agreementSearchOutput.currentCount})",
                                DesktopRentalTimeframeFilter.FUTURE to "Future (${agreementSearchOutput.futureCount})",
                                DesktopRentalTimeframeFilter.PAST to "Past (${agreementSearchOutput.pastCount})",
                            ),
                            selected = agreementTimeframeFilter,
                            onSelect = { agreementTimeframeFilter = it },
                        )
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopSearchInput(
                            value = agreementSearchQuery,
                            onValueChange = { agreementSearchQuery = it },
                            placeholder = "Search past, current, or future agreements by customer name or equipment ID (e.g. eq-001, TENT-2020)...",
                            modifier = Modifier.weight(1f),
                        )
                        if (agreementSearchQuery.isNotBlank() || agreementTimeframeFilter != DesktopRentalTimeframeFilter.ALL) {
                            DesktopSecondaryButton(
                                text = "Reset",
                                onClick = {
                                    agreementSearchQuery = ""
                                    agreementTimeframeFilter = DesktopRentalTimeframeFilter.ALL
                                },
                            )
                        }
                    }

                    val showResults = agreementSearchQuery.isNotBlank() || agreementTimeframeFilter != DesktopRentalTimeframeFilter.ALL
                    if (showResults) {
                        if (agreementSearchOutput.results.isEmpty()) {
                            Text(
                                text = "No matching rental agreements. Try another customer name, equipment ID/SKU, or timeframe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextMuted,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column {
                                    DesktopTableHeaderRow {
                                        DesktopTableHeaderText("Agreement", Modifier.width(90.dp))
                                        DesktopTableHeaderText("Customer & Event", Modifier.weight(1.3f))
                                        DesktopTableHeaderText("Rental Window", Modifier.weight(1.1f))
                                        DesktopTableHeaderText("Equipment IDs", Modifier.weight(1.0f))
                                        DesktopTableHeaderText("Status", Modifier.width(120.dp))
                                        DesktopTableHeaderText("Total", Modifier.width(86.dp))
                                    }
                                    agreementSearchOutput.results.take(8).forEach { res ->
                                        val isRowSelected = if (res.isHeldQuote) {
                                            inspectedQuoteResult?.id == res.id
                                        } else {
                                            inspectedOrderId == res.id
                                        }
                                        DesktopTableRow(
                                            selected = isRowSelected,
                                            onClick = {
                                                if (res.isHeldQuote) {
                                                    inspectedOrderId = null
                                                    inspectedQuoteResult = res
                                                } else {
                                                    inspectedQuoteResult = null
                                                    inspectedOrderId = res.id
                                                }
                                            },
                                        ) {
                                            Text(
                                                text = res.agreementNumberLabel,
                                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                                color = LoyGreenDark,
                                                modifier = Modifier.width(90.dp),
                                            )
                                            Column(modifier = Modifier.weight(1.3f)) {
                                                Text(
                                                    text = res.customerName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (res.eventName.isNotBlank()) {
                                                    Text(
                                                        text = res.eventName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LoyTextMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${formatShortDate(res.rentalStartMs)} → ${formatShortDate(res.rentalEndMs)} (${res.rentalDays}d)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LoyTextSecondary,
                                                modifier = Modifier.weight(1.1f),
                                            )
                                            Text(
                                                text = res.equipmentIds.take(3).joinToString(", ").ifBlank { "—" },
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = LoyTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1.0f),
                                            )
                                            Box(modifier = Modifier.width(120.dp)) {
                                                DesktopBadge(
                                                    text = "${res.timeframe.label} · ${res.statusBadge}",
                                                    bgColor = when {
                                                        res.isRefunded || res.isOverdue -> LoyErrorLight
                                                        res.timeframe == DesktopRentalAgreementTimeframe.CURRENT -> LoyWarningLight
                                                        res.timeframe == DesktopRentalAgreementTimeframe.FUTURE -> LoyInfoLight
                                                        else -> LoyGreenLight
                                                    },
                                                    fgColor = when {
                                                        res.isRefunded || res.isOverdue -> LoyError
                                                        res.timeframe == DesktopRentalAgreementTimeframe.CURRENT -> LoyWarning
                                                        res.timeframe == DesktopRentalAgreementTimeframe.FUTURE -> LoyInfo
                                                        else -> LoyGreenDark
                                                    },
                                                )
                                            }
                                            Text(
                                                text = formatCents(res.totalCents, sym),
                                                style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                                fontWeight = FontWeight.Bold,
                                                color = if (res.isRefunded) LoyError else LoyTextPrimary,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.width(86.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Operations Tables: Return Alerts / Recent Orders & Fleet Watchlist
                val recentOrdersCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DesktopSectionCard(
                        title = if (dueWithin24h.isNotEmpty()) "Returns Due Within 24h & Recent Orders" else "Recent Rental Orders",
                        subtitle = if (dueWithin24h.isNotEmpty()) "${dueWithin24h.size} alert(s) · click any row to inspect inline" else "Click any rental row to inspect details",
                        headerAction = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (dueWithin24h.isNotEmpty()) {
                                    DesktopSecondaryButton(
                                        text = "Due <24h (${dueWithin24h.size})",
                                        icon = Icons.Outlined.NotificationsActive,
                                        contentColor = LoyWarning,
                                        onClick = onOpenDueRentals,
                                    )
                                }
                                DesktopSecondaryButton(text = "View All", onClick = onOpenRentals)
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = cardModifier,
                    ) {
                        if (snapshot.orders.isEmpty()) {
                            DesktopEmptyState(
                                icon = Icons.Outlined.Inventory2,
                                title = "No Rental Orders Yet",
                                subtitle = "Create a booking from Rent Desk to populate dispatch tracking.",
                            )
                        } else {
                            DesktopTableHeaderRow {
                                DesktopTableHeaderText("Order", Modifier.width(66.dp))
                                DesktopTableHeaderText("Client / Event", Modifier.weight(1.3f))
                                DesktopTableHeaderText("Return", Modifier.weight(1f))
                                DesktopTableHeaderText("Total", Modifier.width(82.dp))
                            }
                            val featuredOrders = remember(dueWithin24h, snapshot.orders) {
                                val dueOrders = dueWithin24h.map { it.order }
                                val dueIds = dueOrders.map { it.id }.toSet()
                                (dueOrders + snapshot.orders.filterNot { it.id in dueIds }).take(6)
                            }
                            featuredOrders.forEach { o ->
                                val dueInfo = dueWithin24h.firstOrNull { it.order.id == o.id }
                                DesktopTableRow(
                                    selected = inspectedOrderId == o.id,
                                    onClick = {
                                        inspectedQuoteResult = null
                                        inspectedOrderId = o.id
                                    },
                                ) {
                                    Text(
                                        text = "#${o.number.toString().padStart(4, '0')}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                        color = LoyGreenDark,
                                        modifier = Modifier.width(66.dp),
                                    )
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = o.customerName?.ifBlank { "Walk-in" } ?: "Walk-in",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = o.eventName.ifBlank { formatDateTime(o.createdAt) },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        when {
                                            dueInfo != null -> DesktopBadge(
                                                text = DesktopRentalReturnReminderCalculator.formatRemainingLabel(dueInfo.msUntilDue),
                                                bgColor = if (dueInfo.isOverdue) LoyErrorLight else LoyWarningLight,
                                                fgColor = if (dueInfo.isOverdue) LoyError else LoyWarning,
                                            )
                                            o.isReturned -> DesktopBadge("Returned", LoyGreenLight, LoyGreenDark)
                                            !o.isCompleted -> DesktopBadge("Refunded", LoyErrorLight, LoyError)
                                            else -> Text(
                                                text = "Due ${formatShortDate(o.rentalEndMs)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LoyTextSecondary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatCents(o.totalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                                        fontWeight = FontWeight.Bold,
                                        color = if (o.isCompleted) LoyTextPrimary else LoyError,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(82.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                val fleetWatchlistCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DesktopSectionCard(
                        title = "Fleet Availability Watchlist",
                        subtitle = if (lowOrOutItems.isEmpty()) "All ${snapshot.items.size} items healthy" else "${lowOrOutItems.size} item(s) below safety level",
                        headerAction = {
                            DesktopSecondaryButton(text = "Manage Fleet", onClick = onOpenEquipment)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = cardModifier,
                    ) {
                        if (lowOrOutItems.isEmpty()) {
                            Text(
                                text = "All ${snapshot.items.size} equipment items are above safety stock levels.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyGreenDark,
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            DesktopTableHeaderRow {
                                DesktopTableHeaderText("SKU", Modifier.width(86.dp))
                                DesktopTableHeaderText("Equipment", Modifier.weight(1.4f))
                                DesktopTableHeaderText("Depot / Min", Modifier.width(110.dp))
                            }
                            lowOrOutItems.take(6).forEach { item ->
                                val min = item.effectiveSafetyThreshold(lowThreshold)
                                DesktopTableRow(onClick = onOpenEquipment) {
                                    Text(
                                        text = item.sku.ifBlank { item.id },
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = LoyTextSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.width(86.dp),
                                    )
                                    Column(modifier = Modifier.weight(1.4f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${formatCents(item.priceCents, sym)}/day",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyTextMuted,
                                        )
                                    }
                                    Box(modifier = Modifier.width(110.dp), contentAlignment = Alignment.CenterEnd) {
                                        DesktopBadge(
                                            text = if (item.stock <= 0.0) "0 / ${formatQty(min)}" else "${formatQty(item.stock)} / ${formatQty(min)}",
                                            bgColor = if (item.stock <= 0.0) LoyErrorLight else LoyWarningLight,
                                            fgColor = if (item.stock <= 0.0) LoyError else LoyWarning,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (useTwoColPanels) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        recentOrdersCard(Modifier.weight(1f))
                        fleetWatchlistCard(Modifier.weight(1f))
                    }
                } else {
                    recentOrdersCard(Modifier.fillMaxWidth())
                    fleetWatchlistCard(Modifier.fillMaxWidth())
                }

                // Analytics Row
                val revenueChartCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DesktopSectionCard(
                        title = "Monthly Rental Income (6 Months)",
                        subtitle = "Completed bookings revenue by month",
                        modifier = cardModifier,
                    ) {
                        val maxCents = monthlyPoints.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                        Row(
                            modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            monthlyPoints.forEach { (label, cents, count) ->
                                val frac = (cents.toFloat() / maxCents.toFloat()).coerceIn(0.06f, 1f)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Text(
                                        text = if (cents > 0L) formatCents(cents, sym) else "$count",
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.labelSmall),
                                        color = LoyTextSecondary,
                                        maxLines = 1,
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.58f)
                                            .height((98f * frac).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (cents > 0L) LoyGreen else LoyBorder),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                val utilizationChartCard: @Composable (Modifier) -> Unit = { cardModifier ->
                    DesktopSectionCard(
                        title = "Fleet Utilization by Category",
                        subtitle = "Active checked-out ratio",
                        headerAction = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DesktopSecondaryButton("Schedule", onClick = onOpenSchedule)
                                DesktopSecondaryButton("Event Kits", onClick = onOpenEventKits)
                            }
                        },
                        modifier = cardModifier,
                    ) {
                        utilizationRows.take(5).forEach { (catId, cat, metrics) ->
                            val (pct, ratioText) = metrics
                            val accent = categoryTileAccent(catId)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${pct.roundToInt()}% ($ratioText out)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                    color = if (pct >= 80.0) LoyWarning else accent,
                                    trackColor = LoySurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (useTwoColPanels) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        revenueChartCard(Modifier.weight(1f))
                        utilizationChartCard(Modifier.weight(1f))
                    }
                } else {
                    revenueChartCard(Modifier.fillMaxWidth())
                    utilizationChartCard(Modifier.fillMaxWidth())
                }
            }
        }

        // Docked Right Master-Detail Inspector Pane
        if (showInspector) {
            VerticalDivider(color = LoyBorder)
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .fillMaxHeight()
                    .background(LoySurface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoySurfaceHover)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (inspectedOrder != null) "Rental Agreement Detail" else "Held Quote Detail",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopSecondaryButton(
                            text = if (inspectedOrder != null) "All Rentals" else "Rent Desk",
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = { if (inspectedOrder != null) onOpenRentals() else onOpenRentDesk() },
                        )
                        IconButton(
                            onClick = {
                                inspectedOrderId = null
                                inspectedQuoteResult = null
                            },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close inspector",
                                tint = LoyTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = LoyBorder)

                if (inspectedOrder != null) {
                    DesktopRentalOrderInspectorContent(
                        order = inspectedOrder,
                        repository = repository,
                        snapshot = snapshot,
                        nowMs = now,
                        isDue24h = inspectedOrder.id in due24hIds && !inspectedOrder.isOverdue(now),
                        onReportDamage = onReportDamage,
                        onToast = onToast,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    inspectedQuoteResult?.let { quote ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = quote.agreementNumberLabel,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                DesktopBadge("${quote.timeframe.label} · ${quote.statusBadge}", LoyInfoLight, LoyInfo)
                            }
                            Text(
                                text = formatCents(quote.totalCents, sym),
                                style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                                color = LoyGreenDark,
                                fontWeight = FontWeight.Bold,
                            )
                            DesktopSectionCard(title = "Quote Details") {
                                Text("Client: ${quote.customerName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                if (quote.eventName.isNotBlank()) {
                                    Text(
                                        text = "Event: ${quote.eventName}${if (quote.eventVenue.isNotBlank()) " @ ${quote.eventVenue}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    text = "Window: ${formatShortDate(quote.rentalStartMs)} → ${formatShortDate(quote.rentalEndMs)} (${quote.rentalDays} days)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (quote.equipmentIds.isNotEmpty()) {
                                    Text(
                                        text = "Equipment IDs: ${quote.equipmentIds.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = LoyTextSecondary,
                                    )
                                }
                            }
                            if (quote.equipmentSummaries.isNotEmpty()) {
                                DesktopSectionCard(title = "Quoted Gear (${quote.equipmentSummaries.size})") {
                                    quote.equipmentSummaries.forEach { summary ->
                                        Text("• $summary", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            DesktopPrimaryButton(
                                text = "Resume Quote in Rent Desk",
                                onClick = onOpenRentDesk,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onOpenQrTracking: () -> Unit = {},
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    var search by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<String?>(null) }
    var lowOnly by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(snapshot.items.firstOrNull()?.id) }
    var creatingNew by remember { mutableStateOf(false) }
    var confirmDeleteTarget by remember { mutableStateOf<DesktopEquipment?>(null) }

    val threshold = snapshot.settings.lowStockThreshold
    val filtered = remember(snapshot.items, search, selectedCatId, lowOnly, threshold) {
        val q = search.trim().lowercase()
        snapshot.items.filter { item ->
            val mCat = selectedCatId == null || item.categoryId == selectedCatId
            val mLow = !lowOnly || item.isBelowSafetyThreshold(threshold)
            val mQ = q.isEmpty() || item.name.lowercase().contains(q) || item.sku.lowercase().contains(q) || item.id.lowercase().contains(q)
            mCat && mLow && mQ
        }
    }

    val activeItem = remember(filtered, selectedId, creatingNew) {
        if (creatingNew) null else filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()
    }

    val formSource = remember(activeItem, creatingNew, snapshot.categories) {
        if (creatingNew) {
            DesktopEquipment(
                id = newId(),
                categoryId = snapshot.categories.firstOrNull()?.id,
                name = "",
                priceCents = 0L,
                createdAt = nowMs(),
            )
        } else {
            activeItem
        }
    }

    var name by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.name ?: "") }
    var categoryId by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.categoryId) }
    var catExpanded by remember { mutableStateOf(false) }
    var priceText by remember(formSource?.id, creatingNew) {
        mutableStateOf(if (formSource == null || formSource.priceCents == 0L) "" else (formSource.priceCents / 100.0).toString())
    }
    var costText by remember(formSource?.id, creatingNew) {
        mutableStateOf(if (formSource == null || formSource.costCents == 0L) "" else (formSource.costCents / 100.0).toString())
    }
    var sku by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.sku ?: "") }
    var stockText by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.let { formatQty(it.stock) } ?: "1") }
    var safetyText by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.safetyThreshold?.let { formatQty(it) } ?: "") }
    var trackStock by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.trackStock ?: true) }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Fleet Data Table
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        DesktopSearchInput(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = "Search equipment fleet by name, SKU, or ID...",
                            modifier = Modifier.weight(1f),
                        )
                        DesktopSecondaryButton(
                            text = "QR Tags",
                            icon = Icons.Outlined.QrCodeScanner,
                            onClick = onOpenQrTracking,
                        )
                        DesktopPrimaryButton(
                            text = "Add Equipment",
                            icon = Icons.Outlined.Add,
                            onClick = { creatingNew = true },
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            DesktopFilterPill(
                                label = "All (${snapshot.items.size})",
                                selected = !lowOnly && selectedCatId == null,
                                onClick = { lowOnly = false; selectedCatId = null },
                            )
                        }
                        item {
                            DesktopFilterPill(
                                label = "Low / Out",
                                selected = lowOnly,
                                onClick = { lowOnly = !lowOnly },
                                selectedBg = LoyWarningLight,
                                selectedFg = LoyWarning,
                            )
                        }
                        items(snapshot.categories, key = { it.id }) { cat ->
                            DesktopFilterPill(
                                label = cat.name,
                                selected = selectedCatId == cat.id,
                                onClick = { selectedCatId = if (selectedCatId == cat.id) null else cat.id },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = LoyBorder)

            DesktopTableHeaderRow {
                DesktopTableHeaderText("SKU", Modifier.width(88.dp))
                DesktopTableHeaderText("Equipment Name", Modifier.weight(1.5f))
                DesktopTableHeaderText("Category", Modifier.weight(0.85f))
                DesktopTableHeaderText("Rate/Day", Modifier.width(88.dp))
                DesktopTableHeaderText("Stock", Modifier.width(96.dp))
                DesktopTableHeaderText("Adjust", Modifier.width(68.dp))
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                items(filtered, key = { it.id }) { item ->
                    val selected = !creatingNew && activeItem?.id == item.id
                    val catName = snapshot.categories.firstOrNull { it.id == item.categoryId }?.name ?: "General"
                    val itemMin = item.effectiveSafetyThreshold(threshold)
                    DesktopTableRow(
                        selected = selected,
                        onClick = {
                            creatingNew = false
                            selectedId = item.id
                        },
                    ) {
                        Text(
                            text = item.sku.ifBlank { item.id.take(8) },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = LoyTextSecondary,
                            maxLines = 1,
                            modifier = Modifier.width(88.dp),
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.5f),
                        )
                        Box(modifier = Modifier.weight(0.85f)) {
                            DesktopBadge(
                                text = catName,
                                bgColor = categoryTileAccent(item.categoryId).copy(alpha = 0.10f),
                                fgColor = categoryTileAccent(item.categoryId),
                            )
                        }
                        Text(
                            text = formatCents(item.priceCents, sym),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                            fontWeight = FontWeight.Bold,
                            color = LoyGreenDark,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(88.dp),
                        )
                        Box(modifier = Modifier.width(96.dp), contentAlignment = Alignment.CenterEnd) {
                            DesktopBadge(
                                text = if (!item.trackStock) "Unlim." else "${formatQty(item.stock)} / ${formatQty(itemMin)}",
                                bgColor = when {
                                    !item.trackStock -> LoySurfaceVariant
                                    item.stock <= 0.0 -> LoyErrorLight
                                    item.isBelowSafetyThreshold(threshold) -> LoyWarningLight
                                    else -> LoyGreenLight
                                },
                                fgColor = when {
                                    !item.trackStock -> LoyTextSecondary
                                    item.stock <= 0.0 -> LoyError
                                    item.isBelowSafetyThreshold(threshold) -> LoyWarning
                                    else -> LoyGreenDark
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.width(68.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SmallSquareStepBtn(Icons.Outlined.Remove, "Decrease stock") {
                                repository.adjustEquipmentStock(item.id, -1.0)
                            }
                            SmallSquareStepBtn(Icons.Outlined.Add, "Increase stock") {
                                repository.adjustEquipmentStock(item.id, 1.0)
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Docked Equipment Inspector (360dp)
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(LoySurface),
        ) {
            if (formSource == null) {
                DesktopEmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "Select Equipment",
                    subtitle = "Click any fleet row to edit pricing, SKU, or safety thresholds.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoySurfaceHover)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = if (creatingNew) "New Equipment SKU" else "Equipment Inspector",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (creatingNew) "Add tool to depot fleet" else "ID: ${formSource.id}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = LoyTextMuted,
                        )
                    }
                    if (!creatingNew) {
                        DesktopBadge("Editing", LoyGreenLight, LoyGreenDark)
                    }
                }
                HorizontalDivider(color = LoyBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DesktopFormInput("Equipment Name *", name, { name = it }, placeholder = "e.g. 20x20 Canopy Tent")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = LoyTextSecondary)
                        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                            val catName = snapshot.categories.firstOrNull { it.id == categoryId }?.name ?: "Uncategorized"
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
                                    Text(catName, style = MaterialTheme.typography.bodyMedium)
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded)
                                }
                            }
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                snapshot.categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryId = cat.id; catExpanded = false })
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopFormInput("Daily Rate ($sym)", priceText, { priceText = it }, modifier = Modifier.weight(1f))
                        DesktopFormInput("Unit Cost ($sym)", costText, { costText = it }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopFormInput("SKU", sku, { sku = it }, modifier = Modifier.weight(1f))
                        DesktopFormInput("Available Units", stockText, { stockText = it }, modifier = Modifier.weight(1f))
                    }
                    DesktopFormInput(
                        label = "Safety Alert Threshold (blank = ${formatQty(threshold)})",
                        value = safetyText,
                        onValueChange = { safetyText = it },
                        placeholder = formatQty(threshold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Track depot inventory count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                    }
                }

                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!creatingNew) {
                        DesktopSecondaryButton(
                            text = "Delete",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = { confirmDeleteTarget = formSource },
                        )
                    } else {
                        DesktopSecondaryButton(
                            text = "Cancel",
                            onClick = { creatingNew = false },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    DesktopPrimaryButton(
                        text = if (creatingNew) "Create Equipment" else "Save Changes",
                        enabled = name.trim().isNotEmpty(),
                        onClick = {
                            val clean = name.trim()
                            if (clean.isNotEmpty()) {
                                val updated = formSource.copy(
                                    name = clean,
                                    categoryId = categoryId,
                                    priceCents = parseAmountToCents(priceText) ?: 0L,
                                    costCents = parseAmountToCents(costText) ?: 0L,
                                    sku = sku.trim(),
                                    trackStock = trackStock,
                                    stock = stockText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                    safetyThreshold = if (safetyText.isBlank()) null else safetyText.toDoubleOrNull()?.coerceAtLeast(0.0),
                                )
                                repository.saveEquipment(updated)
                                creatingNew = false
                                selectedId = updated.id
                                onToast("Saved ${updated.name}")
                            }
                        },
                    )
                }
            }
        }
    }

    confirmDeleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text("Delete Equipment?") },
            text = { Text("Remove \"${target.name}\" (${target.sku}) from fleet inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteEquipment(target.id)
                        confirmDeleteTarget = null
                        onToast("Deleted ${target.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoyError),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SmallSquareStepBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (hovered) LoySurfaceVariant else LoySurface,
        border = BorderStroke(1.dp, if (hovered) LoyBorderStrong else LoyBorder),
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = LoyTextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun ClientsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(snapshot.clients.firstOrNull()?.id) }
    var creatingNew by remember { mutableStateOf(false) }

    val filtered = remember(snapshot.clients, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) snapshot.clients else snapshot.clients.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.email.lowercase().contains(q) || it.note.lowercase().contains(q)
        }
    }

    val activeClient = remember(filtered, selectedId, creatingNew) {
        if (creatingNew) null else filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()
    }
    val formSource = remember(activeClient, creatingNew) {
        if (creatingNew) DesktopClient(id = newId(), name = "", createdAt = nowMs()) else activeClient
    }

    var name by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.name ?: "") }
    var phone by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.phone ?: "") }
    var email by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.email ?: "") }
    var note by remember(formSource?.id, creatingNew) { mutableStateOf(formSource?.note ?: "") }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Client Directory Table
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopSearchInput(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search event clients, phones, emails, or venues...",
                        modifier = Modifier.weight(1f),
                    )
                    DesktopPrimaryButton(
                        text = "Add Client",
                        icon = Icons.Outlined.Add,
                        onClick = { creatingNew = true },
                    )
                }
            }
            HorizontalDivider(color = LoyBorder)

            if (filtered.isEmpty()) {
                DesktopEmptyState(
                    icon = Icons.Outlined.PeopleOutline,
                    title = "No matching clients",
                    subtitle = "Add event organizers or venues to speed up Rent Desk checkout.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                DesktopTableHeaderRow {
                    DesktopTableHeaderText("Client / Organizer", Modifier.weight(1.2f))
                    DesktopTableHeaderText("Phone", Modifier.weight(0.9f))
                    DesktopTableHeaderText("Email", Modifier.weight(1.1f))
                    DesktopTableHeaderText("Venue / Note", Modifier.weight(1.0f))
                    DesktopTableHeaderText("Activity", Modifier.width(105.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
                    items(filtered, key = { it.id }) { client ->
                        DesktopTableRow(
                            selected = !creatingNew && activeClient?.id == client.id,
                            onClick = {
                                creatingNew = false
                                selectedId = client.id
                            },
                        ) {
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.2f),
                            )
                            Text(
                                text = client.phone.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                                modifier = Modifier.weight(0.9f),
                            )
                            Text(
                                text = client.email.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.1f),
                            )
                            Text(
                                text = client.note.ifBlank { "—" },
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.0f),
                            )
                            Box(modifier = Modifier.width(105.dp), contentAlignment = Alignment.CenterEnd) {
                                DesktopBadge(
                                    text = "${client.ordersCount} ord · ${client.points} pt",
                                    bgColor = LoyGreenLight,
                                    fgColor = LoyGreenDark,
                                )
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Client Inspector Dock (340dp)
        Column(
            modifier = Modifier.width(340.dp).fillMaxHeight().background(LoySurface),
        ) {
            if (formSource == null) {
                DesktopEmptyState(
                    icon = Icons.Outlined.PeopleOutline,
                    title = "Select a Client",
                    subtitle = "Choose a client row or click Add Client to edit contact records.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoySurfaceHover)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (creatingNew) "New Event Client" else "Client Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!creatingNew) {
                        DesktopBadge("${formSource.ordersCount} rentals", LoyInfoLight, LoyInfo)
                    }
                }
                HorizontalDivider(color = LoyBorder)

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DesktopFormInput("Client / Organization Name *", name, { name = it }, placeholder = "e.g. Grand Horizon Events")
                    DesktopFormInput("Phone Number", phone, { phone = it }, placeholder = "+1 555-0192")
                    DesktopFormInput("Email Address", email, { email = it }, placeholder = "events@client.com")
                    DesktopFormInput(
                        label = "Default Venue / Account Notes",
                        value = note,
                        onValueChange = { note = it },
                        singleLine = false,
                        minLines = 3,
                        placeholder = "Loading bay details, NET terms...",
                    )
                }

                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!creatingNew) {
                        DesktopSecondaryButton(
                            text = "Delete",
                            icon = Icons.Outlined.DeleteOutline,
                            contentColor = LoyError,
                            onClick = {
                                repository.deleteClient(formSource.id)
                                selectedId = null
                                onToast("Deleted client ${formSource.name}")
                            },
                        )
                    } else {
                        DesktopSecondaryButton(text = "Cancel", onClick = { creatingNew = false })
                    }
                    Spacer(Modifier.weight(1f))
                    DesktopPrimaryButton(
                        text = if (creatingNew) "Create Client" else "Save Profile",
                        enabled = name.trim().isNotEmpty(),
                        onClick = {
                            val updated = formSource.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                note = note.trim(),
                            )
                            repository.saveClient(updated)
                            creatingNew = false
                            selectedId = updated.id
                            onToast("Saved client ${updated.name}")
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopSettingsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var storeName by remember(snapshot.settings.storeName) { mutableStateOf(snapshot.settings.storeName) }
    var footer by remember(snapshot.settings.receiptFooter) { mutableStateOf(snapshot.settings.receiptFooter) }
    var currency by remember(snapshot.settings.currencySymbol) { mutableStateOf(snapshot.settings.currencySymbol) }
    var taxRate by remember(snapshot.settings.taxRatePercent) { mutableStateOf(formatQty(snapshot.settings.taxRatePercent)) }
    var cloudUrl by remember(snapshot.settings.cloudApiUrl) { mutableStateOf(snapshot.settings.cloudApiUrl) }
    var cloudToken by remember(snapshot.settings.cloudSyncToken) { mutableStateOf(snapshot.settings.cloudSyncToken) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoyBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DesktopSectionCard(
                title = "Company & Rental Billing Profile",
                subtitle = "Shown on receipts, printable rental agreements, and workspace sidebar",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopFormInput("Rental Company Name", storeName, { storeName = it }, modifier = Modifier.weight(2f))
                    DesktopFormInput("Currency", currency, { currency = it }, modifier = Modifier.weight(0.7f))
                    DesktopFormInput("Tax %", taxRate, { taxRate = it }, modifier = Modifier.weight(0.7f))
                }
                DesktopFormInput("Receipt & Contract Footer", footer, { footer = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DesktopPrimaryButton(
                        text = "Save Profile Settings",
                        onClick = {
                            repository.updateSettings {
                                it.copy(
                                    storeName = storeName.trim().ifEmpty { "EventGear Rentals" },
                                    currencySymbol = currency.trim().ifEmpty { "$" },
                                    taxRatePercent = taxRate.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0,
                                    receiptFooter = footer.trim(),
                                )
                            }
                            onToast("Company settings saved")
                        },
                    )
                }
            }

            DesktopSectionCard(
                title = "Portable Data & Android Interoperability",
                subtitle = "Export/import `loypos-export` v1 JSON compatible with Android or download CSV ledgers",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopSecondaryButton(
                        text = "Export Backup (JSON)",
                        icon = Icons.Outlined.Upload,
                        onClick = {
                            val chooser = JFileChooser().apply {
                                selectedFile = File("loypos-rentals-backup.json")
                                fileFilter = FileNameExtensionFilter("JSON backup (*.json)", "json")
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                repository.exportBackupJson(chooser.selectedFile)
                                onToast("Exported backup to ${chooser.selectedFile.name}")
                            }
                        },
                    )
                    DesktopSecondaryButton(
                        text = "Import Backup (JSON)",
                        icon = Icons.Outlined.Download,
                        onClick = {
                            val chooser = JFileChooser().apply {
                                fileFilter = FileNameExtensionFilter("JSON backup (*.json)", "json")
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                runCatching { repository.importBackupJson(chooser.selectedFile) }
                                    .onSuccess { onToast(it) }
                                    .onFailure { onToast("Import failed: ${it.message}") }
                            }
                        },
                    )
                    DesktopSecondaryButton(
                        text = "Export Rentals (CSV)",
                        icon = Icons.Outlined.TableView,
                        onClick = {
                            val chooser = JFileChooser().apply {
                                selectedFile = File("loypos-rentals-report.csv")
                                fileFilter = FileNameExtensionFilter("CSV ledger (*.csv)", "csv")
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                val count = repository.exportRentalsCsv(chooser.selectedFile)
                                onToast("Exported $count rentals to ${chooser.selectedFile.name}")
                            }
                        },
                    )
                }
            }

            DesktopSectionCard(
                title = "Plesk Cloud Backend Sync & Maintenance",
                subtitle = "Connect desktop workstation to remote sync endpoint or reset demo fleet",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DesktopFormInput("API Base URL", cloudUrl, { cloudUrl = it }, modifier = Modifier.weight(1f))
                    DesktopFormInput("Sync Token", cloudToken, { cloudToken = it }, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopSecondaryButton(
                        text = "Reset Demo Fleet",
                        icon = Icons.Outlined.RestartAlt,
                        contentColor = LoyError,
                        onClick = { showResetConfirm = true },
                    )
                    DesktopPrimaryButton(
                        text = "Save & Test Cloud Connection",
                        icon = Icons.Outlined.CloudSync,
                        onClick = {
                            repository.updateSettings {
                                it.copy(cloudApiUrl = cloudUrl.trim(), cloudSyncToken = cloudToken.trim())
                            }
                            scope.launch {
                                val res = withContext(Dispatchers.IO) { repository.syncCloudHealth() }
                                res.onSuccess { onToast(it) }.onFailure { onToast("Sync check failed: ${it.message}") }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Desktop Fleet to Demo Data?") },
            text = { Text("Restores all 52 event equipment tools and 8 categories to defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetDemoFleet()
                        showResetConfirm = false
                        onToast("Demo fleet restored")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoyError),
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
        )
    }
}
