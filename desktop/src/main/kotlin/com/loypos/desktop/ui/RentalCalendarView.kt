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
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
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
    var scheduleFilterIdx by remember { mutableIntStateOf(0) }
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
        // Left Pane: Calendar + Schedule List
        LazyColumn(
            modifier = Modifier.weight(0.45f).fillMaxHeight(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DesktopSectionCard(
                    title = formatMonthYear(displayedMonth),
                    subtitle = "$activeOutCount active out · $overdueCount overdue · ${upcomingOrders.size} upcoming",
                    headerAction = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                                modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month", modifier = Modifier.size(18.dp))
                            }
                            if (displayedMonth != YearMonth.from(today) || selectedDate != today) {
                                DesktopSecondaryButton(
                                    text = "Today",
                                    icon = Icons.Outlined.Today,
                                    onClick = {
                                        selectedDate = today
                                        displayedMonth = YearMonth.from(today)
                                    },
                                )
                            }
                            IconButton(
                                onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                                modifier = Modifier.size(28.dp).pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month", modifier = Modifier.size(18.dp))
                            }
                            DesktopPrimaryButton(
                                text = "Book Date",
                                icon = Icons.Outlined.PointOfSale,
                                onClick = { onBookForDate(dayStartMs(selectedDate)) },
                            )
                        }
                    },
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                            Text(
                                text = d.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyTextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        cells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DesktopLegendDot(LoyInfo, "Scheduled / Out")
                        DesktopLegendDot(LoyWarning, "Overdue")
                        DesktopLegendDot(LoyGreen, "Returned")
                    }
                }
            }

            item {
                val shownOrders = if (scheduleFilterIdx == 0) selectedDayOrders else upcomingOrders
                DesktopSectionCard(
                    title = if (scheduleFilterIdx == 0) formatFullLocalDate(selectedDate) else "All Upcoming Events",
                    subtitle = "${shownOrders.size} rental booking(s)",
                    headerAction = {
                        DesktopSegmentedControl(
                            options = listOf(
                                0 to "Day (${selectedDayOrders.size})",
                                1 to "Upcoming (${upcomingOrders.size})",
                            ),
                            selected = scheduleFilterIdx,
                            onSelect = { scheduleFilterIdx = it },
                        )
                    },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    if (shownOrders.isEmpty()) {
                        DesktopEmptyState(
                            icon = Icons.Outlined.CheckCircleOutline,
                            title = "Full Fleet Open",
                            subtitle = "No active bookings overlap this selected date.",
                        )
                    } else {
                        DesktopTableHeaderRow {
                            DesktopTableHeaderText("Order / Event", Modifier.weight(1.3f))
                            DesktopTableHeaderText("Window", Modifier.weight(0.9f))
                            DesktopTableHeaderText("Status", Modifier.width(84.dp))
                        }
                        shownOrders.forEach { order ->
                            val overdue = order.isOverdue(now)
                            DesktopTableRow {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(
                                        text = order.eventName.ifBlank { "Order #${order.number.toString().padStart(4, '0')}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            "#${order.number.toString().padStart(4, '0')}",
                                            order.customerName?.takeIf { it.isNotBlank() },
                                            order.eventVenue.takeIf { it.isNotBlank() },
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(modifier = Modifier.weight(0.9f)) {
                                    Text(
                                        text = "${formatShortDate(order.rentalStartMs)} → ${formatShortDate(order.rentalEndMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextSecondary,
                                    )
                                    Text(
                                        text = formatCents(order.totalCents, sym),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.labelSmall),
                                        fontWeight = FontWeight.Bold,
                                        color = LoyGreenDark,
                                    )
                                }
                                Column(
                                    modifier = Modifier.width(84.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    DesktopBadge(
                                        text = when {
                                            order.isReturned -> "Returned"
                                            overdue -> "Overdue"
                                            else -> "Out ${order.rentalDays}d"
                                        },
                                        bgColor = when {
                                            order.isReturned -> LoyGreenLight
                                            overdue -> LoyErrorLight
                                            else -> LoyInfoLight
                                        },
                                        fgColor = when {
                                            order.isReturned -> LoyGreenDark
                                            overdue -> LoyError
                                            else -> LoyInfo
                                        },
                                    )
                                    if (order.isRentalOut) {
                                        Text(
                                            text = "Mark In",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LoyGreenDark,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    if (repository.markRentalReturned(order.id)) {
                                                        onToast("Marked #${order.number.toString().padStart(4, '0')} returned")
                                                    }
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = LoyBorder)

        // Right Pane: Availability Matrix Table
        Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
            Surface(color = LoySurface) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                                text = "Availability Matrix · ${formatFullLocalDate(selectedDate)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "$partiallyBooked partial · $fullyBooked fully booked · ${availability.size - partiallyBooked - fullyBooked} full capacity",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fullyBooked > 0) LoyWarning else LoyTextSecondary,
                            )
                        }
                        DesktopSecondaryButton(
                            text = "Rent on Date",
                            icon = Icons.Outlined.CalendarToday,
                            onClick = { onBookForDate(dayStartMs(selectedDate)) },
                        )
                    }

                    DesktopSearchInput(
                        value = equipSearch,
                        onValueChange = { equipSearch = it },
                        placeholder = "Filter availability matrix by gear name or SKU...",
                    )

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

            DesktopTableHeaderRow {
                DesktopTableHeaderText("SKU", Modifier.width(82.dp))
                DesktopTableHeaderText("Equipment", Modifier.weight(1.4f))
                DesktopTableHeaderText("Utilization", Modifier.weight(0.9f))
                DesktopTableHeaderText("Available", Modifier.width(100.dp))
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(LoySurface)) {
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

                    DesktopTableRow {
                        Text(
                            text = entry.item.sku.ifBlank { entry.item.id },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = LoyTextMuted,
                            modifier = Modifier.width(82.dp),
                        )
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(
                                text = entry.item.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (entry.reservedUnits > 0.0) {
                                    "${formatQty(entry.reservedUnits)} reserved (${entry.bookingsCount} ord)"
                                } else {
                                    "${formatCents(entry.item.priceCents, sym)}/d"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = LoyTextMuted,
                            )
                        }
                        Box(modifier = Modifier.weight(0.9f)) {
                            if (entry.item.trackStock && entry.totalCapacity > 0.0) {
                                LinearProgressIndicator(
                                    progress = { entry.utilizationRatio },
                                    color = statusColor,
                                    trackColor = LoySurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                )
                            }
                        }
                        Box(modifier = Modifier.width(100.dp), contentAlignment = Alignment.CenterEnd) {
                            DesktopBadge(
                                text = when (entry.status) {
                                    DesktopAvailabilityStatus.UNLIMITED -> "Unlimited"
                                    DesktopAvailabilityStatus.FULLY_BOOKED -> "0 / ${formatQty(entry.totalCapacity)}"
                                    else -> "${formatQty(entry.availableUnits)} / ${formatQty(entry.totalCapacity)}"
                                },
                                bgColor = statusBg,
                                fgColor = statusColor,
                            )
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
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bgColor = when {
        isSelected -> LoyGreen
        cell.overdueCount > 0 -> LoyWarningLight
        cell.activeCount > 0 -> LoyInfoLight
        isToday -> LoyGreenLight
        hovered -> LoySurfaceVariant
        else -> LoySurfaceHover
    }
    val borderColor = when {
        isSelected -> LoyGreenDark
        isToday -> LoyGreen
        hovered -> LoyBorderStrong
        else -> LoyBorder
    }
    val textColor = when {
        isSelected -> Color.White
        !cell.isCurrentMonth -> LoyTextMuted.copy(alpha = 0.45f)
        isToday -> LoyGreenDark
        else -> LoyTextPrimary
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected || isToday) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
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
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (cell.overdueCount > 0) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) Color.White else LoyWarning))
                    }
                    if (cell.activeCount - cell.overdueCount > 0) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) Color.White else LoyInfo))
                    }
                    if (cell.returnedCount > 0) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) Color.White.copy(alpha = 0.8f) else LoyGreenDark))
                    }
                    Text(
                        "${cell.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else LoyTextSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DesktopLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
    }
}
