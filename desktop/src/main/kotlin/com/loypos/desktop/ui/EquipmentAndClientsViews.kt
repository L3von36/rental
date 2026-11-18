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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.font.FontFamily
import com.loypos.desktop.core.DesktopRentalAgreementSearchResult
import com.loypos.desktop.core.DesktopRentalAgreementTimeframe
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
    snapshot: DesktopStoreSnapshot,
    onOpenRentDesk: () -> Unit,
    onOpenRentals: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenSchedule: () -> Unit = {},
    onOpenQrTracking: () -> Unit = {},
    onOpenAgreements: () -> Unit = {},
    onOpenEventKits: () -> Unit = {},
    onOpenDamageLog: () -> Unit = {},
) {
    val sym = snapshot.settings.currencySymbol
    var agreementSearchQuery by remember { mutableStateOf("") }
    var agreementTimeframeFilter by remember { mutableStateOf(DesktopRentalTimeframeFilter.ALL) }
    var selectedSearchResult by remember { mutableStateOf<DesktopRentalAgreementSearchResult?>(null) }
    val now = remember(snapshot.orders) { nowMs() }
    val completed = remember(snapshot.orders) { snapshot.orders.filter { it.isCompleted } }
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
            Triple(catNames[catId] ?: "Other", pct, "${formatQty(out)}/${formatQty(cap)}")
        }.sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LoyBackground).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${snapshot.settings.storeName} · Overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Multi-day event equipment rental management for Desktop & Mobile", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenEventKits) {
                    Text("Event Kits")
                }
                OutlinedButton(onClick = onOpenDamageLog) {
                    Text("Damage Log")
                }
                OutlinedButton(onClick = onOpenQrTracking) {
                    Text("QR Track")
                }
                OutlinedButton(onClick = onOpenAgreements) {
                    Text("Contracts")
                }
                OutlinedButton(onClick = onOpenSchedule) {
                    Text("Schedule")
                }
                Button(onClick = onOpenRentDesk) {
                    Text("Open Rent Desk")
                }
            }
        }

        // Global Rental Agreement Search Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LoySurface,
            border = BorderStroke(
                1.dp,
                if (agreementSearchQuery.isNotBlank() || agreementTimeframeFilter != DesktopRentalTimeframeFilter.ALL) LoyGreenDark.copy(alpha = 0.5f) else LoyBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = LoyGreenDark, modifier = Modifier.size(18.dp))
                        Text("Find Rental Agreements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${agreementSearchOutput.allCount} total · ${agreementSearchOutput.currentCount} current · ${agreementSearchOutput.futureCount} future · ${agreementSearchOutput.pastCount} past",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoyTextSecondary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = agreementSearchQuery,
                        onValueChange = { agreementSearchQuery = it },
                        placeholder = { Text("Search past, current, or future agreements by customer name or equipment ID (e.g. eq-001, TENT-2020)...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search rental agreements", tint = LoyGreenDark) },
                        trailingIcon = {
                            if (agreementSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { agreementSearchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            DesktopRentalTimeframeFilter.ALL to agreementSearchOutput.allCount,
                            DesktopRentalTimeframeFilter.CURRENT to agreementSearchOutput.currentCount,
                            DesktopRentalTimeframeFilter.FUTURE to agreementSearchOutput.futureCount,
                            DesktopRentalTimeframeFilter.PAST to agreementSearchOutput.pastCount,
                        ).forEach { (tf, cnt) ->
                            val selected = agreementTimeframeFilter == tf
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    agreementTimeframeFilter = if (selected && tf != DesktopRentalTimeframeFilter.ALL) {
                                        DesktopRentalTimeframeFilter.ALL
                                    } else tf
                                },
                                label = { Text("${tf.label} ($cnt)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LoyGreenLight,
                                    selectedLabelColor = LoyGreenDark,
                                ),
                            )
                        }
                    }
                }

                val showResults = agreementSearchQuery.isNotBlank() || agreementTimeframeFilter != DesktopRentalTimeframeFilter.ALL
                if (showResults) {
                    HorizontalDivider(color = LoyBorder)
                    if (agreementSearchOutput.results.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "No matching rental agreements. Try another customer name, equipment ID/SKU, or timeframe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                            TextButton(onClick = {
                                agreementSearchQuery = ""
                                agreementTimeframeFilter = DesktopRentalTimeframeFilter.ALL
                            }) { Text("Reset") }
                        }
                    } else {
                        agreementSearchOutput.results.take(8).forEachIndexed { idx, res ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSearchResult = res }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(res.agreementNumberLabel, fontWeight = FontWeight.Bold, color = LoyGreenDark)
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = when {
                                                res.isRefunded || res.isOverdue -> LoyErrorLight
                                                res.timeframe == DesktopRentalAgreementTimeframe.CURRENT -> LoyWarningLight
                                                res.timeframe == DesktopRentalAgreementTimeframe.FUTURE -> Color(0xFFE3F2FD)
                                                else -> LoyGreenLight
                                            },
                                        ) {
                                            Text(
                                                "${res.timeframe.label} · ${res.statusBadge}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    res.isRefunded || res.isOverdue -> LoyError
                                                    res.timeframe == DesktopRentalAgreementTimeframe.CURRENT -> LoyWarning
                                                    res.timeframe == DesktopRentalAgreementTimeframe.FUTURE -> LoyInfo
                                                    else -> LoyGreenDark
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                        Text(res.customerName, fontWeight = FontWeight.SemiBold)
                                        if (res.eventName.isNotBlank()) {
                                            Text("· ${res.eventName}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${formatShortDate(res.rentalStartMs)} → ${formatShortDate(res.rentalEndMs)} (${res.rentalDays}d)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyTextSecondary,
                                        )
                                        if (res.equipmentIds.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                res.equipmentIds.take(4).forEach { eqId ->
                                                    Surface(shape = RoundedCornerShape(6.dp), color = LoyBackground) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                        ) {
                                                            Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(11.dp), tint = LoyTextSecondary)
                                                            Text(eqId, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatCents(res.totalCents, sym),
                                        style = TabularMoneyStyle,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.isRefunded) LoyError else LoyTextPrimary,
                                    )
                                    Text("View details", style = MaterialTheme.typography.labelSmall, color = LoyGreenDark)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard("Net Rental Revenue", formatCents(netRevenueCents, sym), LoyGreenDark, Modifier.weight(1f))
            KpiCard("Rentals Out Now", activeOutCount.toString(), LoyInfo, Modifier.weight(1f))
            KpiCard("Overdue Returns", overdueCount.toString(), if (overdueCount > 0) LoyError else LoyGreenDark, Modifier.weight(1f))
            KpiCard("Low Fleet Alerts", lowOrOutItems.size.toString(), if (lowOrOutItems.isNotEmpty()) LoyWarning else LoyGreenDark, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Rental Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onOpenRentals) { Text("View all") }
                    }
                    if (snapshot.orders.isEmpty()) {
                        Text("No rental bookings recorded yet. Create your first order on the Rent Desk.", color = LoyTextSecondary, style = MaterialTheme.typography.bodySmall)
                    } else {
                        snapshot.orders.take(6).forEachIndexed { idx, o ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("#${o.number.toString().padStart(4, '0')} · ${o.rentalDays}d · ${o.customerName ?: "Walk-in"}", fontWeight = FontWeight.SemiBold)
                                    Text("${formatDateTime(o.createdAt)}${if (o.eventName.isNotEmpty()) " · ${o.eventName}" else ""}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                Text(formatCents(o.totalCents, sym), style = TabularMoneyStyle, fontWeight = FontWeight.Bold, color = if (o.isCompleted) LoyTextPrimary else LoyError)
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Fleet Availability Watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onOpenEquipment) { Text("Manage Fleet") }
                    }
                    if (lowOrOutItems.isEmpty()) {
                        Text("All ${snapshot.items.size} equipment items are well stocked.", color = LoyGreenDark, style = MaterialTheme.typography.bodySmall)
                    } else {
                        lowOrOutItems.take(6).forEachIndexed { idx, item ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Medium)
                                    Text("SKU ${item.sku} · ${formatCents(item.priceCents, sym)}/d", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                }
                                Text(
                                    text = if (item.stock <= 0.0) "Rented out" else "${formatQty(item.stock)} / min ${formatQty(item.effectiveSafetyThreshold(lowThreshold))}",
                                    color = if (item.stock <= 0.0) LoyError else LoyWarning,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Rental Income & Equipment Utilization Bars
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Monthly Rental Income (6M)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val maxCents = monthlyPoints.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                    Row(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        monthlyPoints.forEach { (label, cents, count) ->
                            val frac = (cents.toFloat() / maxCents.toFloat()).coerceIn(0.05f, 1f)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                Text(
                                    if (cents > 0L) formatCents(cents, sym) else "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LoyTextSecondary,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height((90f * frac).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (cents > 0L) LoyGreenDark else LoyBorder),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyBorder),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Equipment Utilization by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    utilizationRows.take(5).forEach { (cat, pct, ratioText) ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("${pct.roundToInt()}% ($ratioText out)", style = MaterialTheme.typography.labelSmall, color = LoyGreenDark, fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                                color = if (pct >= 80.0) LoyWarning else LoyGreenDark,
                                trackColor = LoyBorder,
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSearchResult?.let { res ->
        AlertDialog(
            onDismissRequest = { selectedSearchResult = null },
            title = { Text("${res.agreementNumberLabel} · ${res.customerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status: ${res.timeframe.label} (${res.statusBadge})", fontWeight = FontWeight.SemiBold)
                    if (res.eventName.isNotBlank()) Text("Event: ${res.eventName}${if (res.eventVenue.isNotBlank()) " @ ${res.eventVenue}" else ""}")
                    Text("Dates: ${formatShortDate(res.rentalStartMs)} → ${formatShortDate(res.rentalEndMs)} (${res.rentalDays} days)")
                    Text("Total: ${formatCents(res.totalCents, sym)}", fontWeight = FontWeight.Bold)
                    if (res.equipmentIds.isNotEmpty()) {
                        Text("Equipment IDs: ${res.equipmentIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (res.equipmentSummaries.isNotEmpty()) {
                        HorizontalDivider(color = LoyBorder)
                        res.equipmentSummaries.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedSearchResult = null
                    if (res.isHeldQuote) onOpenRentDesk() else onOpenRentals()
                }) {
                    Text(if (res.isHeldQuote) "Open Rent Desk" else "Open in Rentals")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSearchResult = null }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun KpiCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LoySurface,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = LoyTextSecondary)
            Text(value, style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall), color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

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
    var editing by remember { mutableStateOf<DesktopEquipment?>(null) }
    var isNew by remember { mutableStateOf(false) }

    val threshold = snapshot.settings.lowStockThreshold
    val filtered = remember(snapshot.items, search, selectedCatId, lowOnly, threshold) {
        val q = search.trim().lowercase()
        snapshot.items.filter { item ->
            val mCat = selectedCatId == null || item.categoryId == selectedCatId
            val mLow = !lowOnly || item.isBelowSafetyThreshold(threshold)
            val mQ = q.isEmpty() || item.name.lowercase().contains(q) || item.sku.lowercase().contains(q)
            mCat && mLow && mQ
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search fleet equipment or SKU...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = onOpenQrTracking) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("QR Tags")
                    }
                    Button(onClick = {
                        isNew = true
                        editing = DesktopEquipment(
                            id = newId(),
                            categoryId = snapshot.categories.firstOrNull()?.id,
                            name = "",
                            priceCents = 0L,
                            createdAt = nowMs(),
                        )
                    }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Equipment")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = !lowOnly && selectedCatId == null, onClick = { lowOnly = false; selectedCatId = null }, label = { Text("All (${snapshot.items.size})") })
                    }
                    item {
                        FilterChip(selected = lowOnly, onClick = { lowOnly = !lowOnly }, label = { Text("Low / Out") })
                    }
                    items(snapshot.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = if (selectedCatId == cat.id) null else cat.id },
                            label = { Text(cat.name) },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.id }) { item ->
                val catName = snapshot.categories.firstOrNull { it.id == item.categoryId }?.name ?: "General"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoySurface,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth().clickable { isNew = false; editing = item },
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold)
                            Text("$catName · SKU ${item.sku.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                        Text("${formatCents(item.priceCents, sym)}/day", style = TabularMoneyStyle, fontWeight = FontWeight.Bold, color = LoyGreenDark)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { repository.adjustEquipmentStock(item.id, -1.0) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Outlined.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                            }
                            val itemMin = item.effectiveSafetyThreshold(threshold)
                            Text(
                                text = if (item.safetyThreshold != null) "${formatQty(item.stock)} avail (min ${formatQty(itemMin)})" else "${formatQty(item.stock)} avail",
                                color = if (item.stock <= 0.0) LoyError else if (item.isBelowSafetyThreshold(threshold)) LoyWarning else LoyTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 6.dp),
                            )
                            IconButton(onClick = { repository.adjustEquipmentStock(item.id, 1.0) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Outlined.Add, contentDescription = "Increase stock", modifier = Modifier.size(16.dp))
                            }
                        }
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit equipment", tint = LoyTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    editing?.let { item ->
        EquipmentEditDialog(
            equipment = item,
            isNew = isNew,
            snapshot = snapshot,
            onSave = { updated ->
                repository.saveEquipment(updated)
                editing = null
                onToast("Saved ${updated.name}")
            },
            onDelete = {
                repository.deleteEquipment(item.id)
                editing = null
                onToast("Deleted ${item.name}")
            },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentEditDialog(
    equipment: DesktopEquipment,
    isNew: Boolean,
    snapshot: DesktopStoreSnapshot,
    onSave: (DesktopEquipment) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(equipment.name) }
    var categoryId by remember { mutableStateOf(equipment.categoryId) }
    var catExpanded by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf(if (equipment.priceCents == 0L) "" else (equipment.priceCents / 100.0).toString()) }
    var costText by remember { mutableStateOf(if (equipment.costCents == 0L) "" else (equipment.costCents / 100.0).toString()) }
    var sku by remember { mutableStateOf(equipment.sku) }
    var stockText by remember { mutableStateOf(formatQty(equipment.stock)) }
    var safetyText by remember { mutableStateOf(equipment.safetyThreshold?.let { formatQty(it) } ?: "") }
    var trackStock by remember { mutableStateOf(equipment.trackStock) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New Equipment" else "Edit Equipment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(420.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Equipment name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    val catName = snapshot.categories.firstOrNull { it.id == categoryId }?.name ?: "Uncategorized"
                    OutlinedTextField(
                        value = catName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        snapshot.categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryId = cat.id; catExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Daily rate (${snapshot.settings.currencySymbol})") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Unit cost (${snapshot.settings.currencySymbol})") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("Available units") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = safetyText,
                    onValueChange = { safetyText = it },
                    label = { Text("Safety alert threshold (blank = global ${formatQty(snapshot.settings.lowStockThreshold)})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Track fleet units")
                    Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        onSave(
                            equipment.copy(
                                name = clean,
                                categoryId = categoryId,
                                priceCents = parseAmountToCents(priceText) ?: 0L,
                                costCents = parseAmountToCents(costText) ?: 0L,
                                sku = sku.trim(),
                                trackStock = trackStock,
                                stock = stockText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                safetyThreshold = if (safetyText.isBlank()) null else safetyText.toDoubleOrNull()?.coerceAtLeast(0.0),
                            )
                        )
                    }
                },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = onDelete) { Text("Delete", color = LoyError) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun ClientsView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onToast: (String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<DesktopClient?>(null) }
    var isNew by remember { mutableStateOf(false) }

    val filtered = remember(snapshot.clients, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) snapshot.clients else snapshot.clients.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        Surface(color = LoySurface) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search event clients or organizers...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    isNew = true
                    editing = DesktopClient(id = newId(), name = "", createdAt = nowMs())
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Client")
                }
            }
        }
        HorizontalDivider(color = LoyBorder)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.id }) { client ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LoySurface,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth().clickable { isNew = false; editing = client },
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.PeopleOutline, contentDescription = null, tint = LoyGreen)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(client.name, fontWeight = FontWeight.SemiBold)
                            val sub = listOf(client.phone, client.email, client.note).filter { it.isNotEmpty() }.joinToString(" · ")
                            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                        Text("${client.ordersCount} rentals · ${client.points} pts", style = MaterialTheme.typography.labelMedium, color = LoyGreenDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    editing?.let { client ->
        var name by remember { mutableStateOf(client.name) }
        var phone by remember { mutableStateOf(client.phone) }
        var email by remember { mutableStateOf(client.email) }
        var note by remember { mutableStateOf(client.note) }

        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isNew) "New Client" else "Edit Client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(380.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Client / Company *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Venue / Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isNotEmpty()) {
                            repository.saveClient(client.copy(name = name.trim(), phone = phone.trim(), email = email.trim(), note = note.trim()))
                            editing = null
                            onToast("Saved client ${name.trim()}")
                        }
                    },
                    enabled = name.trim().isNotEmpty(),
                ) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (!isNew) {
                        TextButton(onClick = {
                            repository.deleteClient(client.id)
                            editing = null
                            onToast("Deleted client")
                        }) { Text("Delete", color = LoyError) }
                    }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            },
        )
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

    Column(
        modifier = Modifier.fillMaxSize().background(LoyBackground).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Desktop Settings, Data & Cloud Sync", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Surface(shape = RoundedCornerShape(12.dp), color = LoySurface, border = BorderStroke(1.dp, LoyBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Company & Rental Billing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Rental company name") }, singleLine = true, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency symbol") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = taxRate, onValueChange = { taxRate = it }, label = { Text("Tax %") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = footer, onValueChange = { footer = it }, label = { Text("Receipt footer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    repository.updateSettings {
                        it.copy(
                            storeName = storeName.trim().ifEmpty { "EventGear Rentals" },
                            currencySymbol = currency.trim().ifEmpty { "$" },
                            taxRatePercent = taxRate.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0,
                            receiptFooter = footer.trim(),
                        )
                    }
                    onToast("Company settings saved")
                }) { Text("Save Profile Settings") }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = LoySurface, border = BorderStroke(1.dp, LoyBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Portable Data & Android Interoperability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Export or import `loypos-export` v1 JSON files directly compatible with the Android app, or export a CSV rental ledger.", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        val chooser = JFileChooser().apply {
                            selectedFile = File("loypos-rentals-backup.json")
                            fileFilter = FileNameExtensionFilter("JSON backup (*.json)", "json")
                        }
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            repository.exportBackupJson(chooser.selectedFile)
                            onToast("Exported backup to ${chooser.selectedFile.name}")
                        }
                    }) {
                        Icon(Icons.Outlined.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Backup (JSON)")
                    }
                    OutlinedButton(onClick = {
                        val chooser = JFileChooser().apply {
                            fileFilter = FileNameExtensionFilter("JSON backup (*.json)", "json")
                        }
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            runCatching { repository.importBackupJson(chooser.selectedFile) }
                                .onSuccess { onToast(it) }
                                .onFailure { onToast("Import failed: ${it.message}") }
                        }
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Backup (JSON)")
                    }
                    OutlinedButton(onClick = {
                        val chooser = JFileChooser().apply {
                            selectedFile = File("loypos-rentals-report.csv")
                            fileFilter = FileNameExtensionFilter("CSV ledger (*.csv)", "csv")
                        }
                        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                            val count = repository.exportRentalsCsv(chooser.selectedFile)
                            onToast("Exported $count rentals to ${chooser.selectedFile.name}")
                        }
                    }) {
                        Icon(Icons.Outlined.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Rentals (CSV)")
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(12.dp), color = LoySurface, border = BorderStroke(1.dp, LoyBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plesk Cloud Backend Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = cloudUrl, onValueChange = { cloudUrl = it }, label = { Text("API Base URL") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = cloudToken, onValueChange = { cloudToken = it }, label = { Text("Sync Token") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        repository.updateSettings {
                            it.copy(cloudApiUrl = cloudUrl.trim(), cloudSyncToken = cloudToken.trim())
                        }
                        scope.launch {
                            val res = withContext(Dispatchers.IO) { repository.syncCloudHealth() }
                            res.onSuccess { onToast(it) }.onFailure { onToast("Sync check failed: ${it.message}") }
                        }
                    }) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save & Test Cloud Connection")
                    }
                    OutlinedButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = LoyError)
                        Spacer(Modifier.width(6.dp))
                        Text("Reset Demo Fleet", color = LoyError)
                    }
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
