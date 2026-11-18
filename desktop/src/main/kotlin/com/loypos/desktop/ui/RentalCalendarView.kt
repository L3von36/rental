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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.desktop.core.DesktopAvailabilityStatus
import com.loypos.desktop.core.DesktopCalendarCell
import com.loypos.desktop.core.DesktopEquipmentAvailability
import com.loypos.desktop.core.buildMonthCalendarGrid
import com.loypos.desktop.core.computeEquipmentAvailabilityForRange
import com.loypos.desktop.core.dayBoundsFor
import com.loypos.desktop.core.dayStartMs
import com.loypos.desktop.core.formatCents
import com.loypos.desktop.core.formatFullLocalDate
import com.loypos.desktop.core.formatMonthYear
import com.loypos.desktop.core.formatQty
import com.loypos.desktop.core.formatShortDate
import com.loypos.desktop.core.nowMs
import com.loypos.desktop.core.schedulesForDate
import com.loypos.desktop.core.upcomingRentalsFrom
import com.loypos.desktop.data.DesktopRepository
import com.loypos.desktop.model.DesktopStoreSnapshot
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RentalCalendarView(
    repository: DesktopRepository,
    snapshot: DesktopStoreSnapshot,
    onBookForDate: (startDateMs: Long) -> Unit,
    onToast: (String) -> Unit,
) {
    val sym = snapshot.settings.currencySymbol
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var scheduleFilterIdx by remember { mutableIntStateOf(0) } // 0 = Selected Date, 1 = Upcoming
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var equipSearch by remember { mutableStateOf("") }

    val now = remember(snapshot.orders) { nowMs() }
    val cells = remember(displayedMonth, snapshot.orders, now) {
        buildMonthCalendarGrid(displayedMonth, snapshot.orders, now)
    }
    val selectedDayOrders = remember(selectedDate, snapshot.orders) {
        schedulesForDate(selectedDate, snapshot.orders)
    }
    val upcomingOrders = remember(today, snapshot.orders) {
        upcomingRentalsFrom(today, snapshot.orders)
    }
    val dayBounds = remember(selectedDate) { dayBoundsFor(selectedDate) }
    val availability = remember(snapshot.items, snapshot.orders, dayBounds, snapshot.settings.lowStockThreshold) {
        computeEquipmentAvailabilityForRange(
            items = snapshot.items,
            orders = snapshot.orders,
            rangeStartMs = dayBounds.startMs,
            rangeEndMs = dayBounds.endMs,
            lowThreshold = snapshot.settings.lowStockThreshold,
        )
    }

    val activeOutCount = remember(snapshot.orders) { snapshot.orders.count { it.isRentalOut } }
    val overdueCount = remember(snapshot.orders, now) { snapshot.orders.count { it.isOverdue(now) } }

    Row(modifier = Modifier.fillMaxSize().background(LoyBackground)) {
        // Left Pane: Monthly Calendar & Upcoming Overview
        LazyColumn(
            modifier = Modifier.weight(0.46f).fillMaxHeight(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Equipment Schedule & Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "$activeOutCount active out · $overdueCount overdue · ${upcomingOrders.size} upcoming",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                    }
                    Button(
                        onClick = { onBookForDate(dayStartMs(selectedDate)) },
                        colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                    ) {
                        Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Book for ${selectedDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.dayOfMonth}")
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LoySurface,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    formatMonthYear(displayedMonth),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (displayedMonth != YearMonth.from(today) || selectedDate != today) {
                                    TextButton(onClick = {
                                        selectedDate = today
                                        displayedMonth = YearMonth.from(today)
                                    }) {
                                        Icon(Icons.Outlined.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Today")
                                    }
                                }
                            }
                            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                                Text(
                                    text = d,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LoyTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cells.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    week.forEach { cell ->
                                        DesktopCalendarDayCell(
                                            cell = cell,
                                            isToday = cell.date == today,
                                            isSelected = cell.date == selectedDate,
                                            onClick = { selectedDate = cell.date },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = LoyBorder)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DesktopLegendDot(LoyInfo, "Scheduled / Out")
                            DesktopLegendDot(LoyWarning, "Overdue")
                            DesktopLegendDot(LoyGreenDark, "Returned")
                        }
                    }
                }
            }

            // Scheduled Orders Section
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LoySurface,
                    border = BorderStroke(1.dp, LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = if (scheduleFilterIdx == 0) "Schedules for ${formatFullLocalDate(selectedDate)}" else "All Upcoming Schedules",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${if (scheduleFilterIdx == 0) selectedDayOrders.size else upcomingOrders.size} bookings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LoyTextSecondary,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = scheduleFilterIdx == 0,
                                    onClick = { scheduleFilterIdx = 0 },
                                    label = { Text("Day (${selectedDayOrders.size})") },
                                )
                                FilterChip(
                                    selected = scheduleFilterIdx == 1,
                                    onClick = { scheduleFilterIdx = 1 },
                                    label = { Text("Upcoming (${upcomingOrders.size})") },
                                )
                            }
                        }

                        val shownOrders = if (scheduleFilterIdx == 0) selectedDayOrders else upcomingOrders
                        if (shownOrders.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoyBackground,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = LoyGreenDark)
                                    Column {
                                        Text("No bookings overlapping this date", fontWeight = FontWeight.SemiBold)
                                        Text("Full equipment fleet capacity is open for booking.", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                shownOrders.forEach { order ->
                                    val overdue = order.isOverdue(now)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = LoyBackground,
                                        border = BorderStroke(1.dp, if (overdue) LoyWarning.copy(alpha = 0.45f) else LoyBorder),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(11.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = order.eventName.ifBlank { "Order #${order.number.toString().padStart(4, '0')}" },
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                Text(
                                                    formatCents(order.totalCents, sym),
                                                    style = TabularMoneyStyle,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LoyGreenDark,
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    if (!order.customerName.isNullOrBlank()) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(14.dp))
                                                            Text(order.customerName, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                                        }
                                                    }
                                                    if (order.eventVenue.isNotBlank()) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(14.dp))
                                                            Text(order.eventVenue, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                                        }
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(
                                                            when {
                                                                order.isReturned -> LoyGreenLight
                                                                overdue -> LoyWarningLight
                                                                else -> LoyInfoLight
                                                            }
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        text = when {
                                                            order.isReturned -> "Returned"
                                                            overdue -> "Overdue"
                                                            else -> "Out ${order.rentalDays}d"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = when {
                                                            order.isReturned -> LoyGreenDark
                                                            overdue -> LoyWarning
                                                            else -> LoyInfo
                                                        },
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "#${order.number.toString().padStart(4, '0')} · ${formatShortDate(order.rentalStartMs)} → ${formatShortDate(order.rentalEndMs)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LoyTextPrimary,
                                                )
                                                if (order.isRentalOut) {
                                                    TextButton(
                                                        onClick = {
                                                            if (repository.markRentalReturned(order.id)) {
                                                                onToast("Marked #${order.number.toString().padStart(4, '0')} returned")
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Outlined.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Mark Returned", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Pane: Equipment Availability Forecast Matrix for Selected Date
        Column(modifier = Modifier.weight(0.54f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val fullyBooked = availability.count { it.status == DesktopAvailabilityStatus.FULLY_BOOKED }
                    val partiallyBooked = availability.count { it.reservedUnits > 0.0 && it.status != DesktopAvailabilityStatus.FULLY_BOOKED }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Fleet Availability on ${formatFullLocalDate(selectedDate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "$partiallyBooked partial · $fullyBooked fully booked · ${availability.size - partiallyBooked - fullyBooked} full capacity",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (fullyBooked > 0) LoyWarning else LoyTextSecondary,
                            )
                        }
                        OutlinedButton(onClick = { onBookForDate(dayStartMs(selectedDate)) }) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Rent on Date")
                        }
                    }

                    OutlinedTextField(
                        value = equipSearch,
                        onValueChange = { equipSearch = it },
                        placeholder = { Text("Filter equipment availability by name or SKU...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (equipSearch.isNotEmpty()) {
                                IconButton(onClick = { equipSearch = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

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
                }
            }
            HorizontalDivider(color = LoyBorder)

            val filteredAvailability = remember(availability, selectedCategoryId, equipSearch) {
                val q = equipSearch.trim().lowercase()
                availability
                    .filter { a ->
                        (selectedCategoryId == null || a.item.categoryId == selectedCategoryId) &&
                            (q.isEmpty() || a.item.name.lowercase().contains(q) || a.item.sku.lowercase().contains(q))
                    }
                    .sortedWith(compareByDescending<DesktopEquipmentAvailability> { it.reservedUnits }.thenBy { it.item.name })
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredAvailability, key = { it.item.id }) { entry ->
                    val statusColor = when (entry.status) {
                        DesktopAvailabilityStatus.AVAILABLE, DesktopAvailabilityStatus.UNLIMITED -> LoyGreenDark
                        DesktopAvailabilityStatus.LOW -> LoyWarning
                        DesktopAvailabilityStatus.FULLY_BOOKED -> LoyError
                    }
                    val statusBg = when (entry.status) {
                        DesktopAvailabilityStatus.AVAILABLE, DesktopAvailabilityStatus.UNLIMITED -> LoyGreenLight
                        DesktopAvailabilityStatus.LOW -> LoyWarningLight
                        DesktopAvailabilityStatus.FULLY_BOOKED -> LoyErrorLight
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LoySurface,
                        border = BorderStroke(1.dp, LoyBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            append("${entry.item.sku} · ${formatCents(entry.item.priceCents, sym)}/d")
                                            if (entry.reservedUnits > 0.0) {
                                                append(" · ${formatQty(entry.reservedUnits)} reserved across ${entry.bookingsCount} ${if (entry.bookingsCount == 1) "order" else "orders"}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = when (entry.status) {
                                            DesktopAvailabilityStatus.UNLIMITED -> "Unlimited"
                                            DesktopAvailabilityStatus.FULLY_BOOKED -> "0 / ${formatQty(entry.totalCapacity)} avail"
                                            else -> "${formatQty(entry.availableUnits)} / ${formatQty(entry.totalCapacity)} avail"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (entry.item.trackStock && entry.totalCapacity > 0.0) {
                                LinearProgressIndicator(
                                    progress = { entry.utilizationRatio },
                                    color = statusColor,
                                    trackColor = LoyBorder,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopCalendarDayCell(
    cell: DesktopCalendarCell,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        isSelected -> LoyGreen
        cell.overdueCount > 0 -> LoyWarningLight
        cell.activeCount > 0 -> LoyInfoLight
        isToday -> LoyGreenLight
        else -> LoyBackground
    }
    val borderColor = when {
        isSelected -> LoyGreenDark
        isToday -> LoyGreen
        else -> LoyBorder
    }
    val textColor = when {
        isSelected -> Color.White
        !cell.isCurrentMonth -> LoyTextSecondary.copy(alpha = 0.45f)
        isToday -> LoyGreenDark
        else -> LoyTextPrimary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected || isToday) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            )
            if (cell.totalCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cell.overdueCount > 0) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) Color.White else LoyWarning))
                    }
                    if (cell.activeCount - cell.overdueCount > 0) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) Color.White else LoyInfo))
                    }
                    if (cell.returnedCount > 0) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) Color.White.copy(alpha = 0.8f) else LoyGreenDark))
                    }
                    Text(
                        "${cell.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else LoyTextSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DesktopLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
    }
}
