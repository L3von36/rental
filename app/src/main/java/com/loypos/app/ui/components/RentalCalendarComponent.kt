package com.loypos.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.AvailabilityStatus
import com.loypos.app.core.EquipmentDayAvailability
import com.loypos.app.core.RentalCalendarCell
import com.loypos.app.core.buildMonthCalendarGrid
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatFullLocalDate
import com.loypos.app.core.formatMonthYear
import com.loypos.app.core.formatQty
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.nowMs
import com.loypos.app.data.local.CategoryEntity
import com.loypos.app.data.local.SaleWithItems
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoyInfoLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun RentalCalendarGridCard(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    receipts: List<SaleWithItems>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    val today = remember { LocalDate.now() }
    val currentTimeMs = remember(receipts) { nowMs() }
    val cells = remember(displayedMonth, receipts, currentTimeMs) {
        buildMonthCalendarGrid(displayedMonth, receipts, currentTimeMs)
    }

    GroupCard(modifier = modifier.testTag("rental_calendar_card")) {
        Column(
            modifier = Modifier.padding(metrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("calendar_prev_month"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = formatMonthYear(displayedMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LoyTextPrimary,
                    )
                    if (displayedMonth != YearMonth.from(today) || selectedDate != today) {
                        TextButton(
                            onClick = onJumpToToday,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp).testTag("calendar_today_button"),
                        ) {
                            Icon(Icons.Outlined.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Today", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .accessibleTouchTarget(metrics.minTouchTarget)
                        .testTag("calendar_next_month"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Next month",
                    )
                }
            }

            // Weekday labels (Mon .. Sun)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LoyTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // 6-Week Matrix
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        week.forEach { cell ->
                            CalendarDayTile(
                                cell = cell,
                                isToday = cell.date == today,
                                isSelected = cell.date == selectedDate,
                                onClick = { onSelectDate(cell.date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LoyBorder)

            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalendarLegendItem(color = LoyInfo, label = "Scheduled / Out")
                CalendarLegendItem(color = LoyWarning, label = "Overdue")
                CalendarLegendItem(color = LoyGreen, label = "Returned")
            }
        }
    }
}

@Composable
private fun CalendarDayTile(
    cell: RentalCalendarCell,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> LoyGreen
            cell.overdueCount > 0 -> Color(0xFFFFF3E0)
            cell.activeCount > 0 -> LoyInfoLight.copy(alpha = 0.65f)
            isToday -> LoyGreenLight.copy(alpha = 0.65f)
            else -> Color.Transparent
        },
        label = "calendarDayBg",
    )
    val borderColor = when {
        isSelected -> LoyGreen
        isToday -> LoyGreen
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        !cell.isCurrentMonth -> LoyTextSecondary.copy(alpha = 0.42f)
        isToday -> LoyGreen
        else -> LoyTextPrimary
    }

    Surface(
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        border = if (borderColor != Color.Transparent) BorderStroke(1.2.dp, borderColor) else null,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .testTag("calendar_day_${cell.date}"),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            )
            if (cell.totalCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cell.overdueCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else LoyWarning),
                        )
                    }
                    if (cell.activeCount - cell.overdueCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else LoyInfo),
                        )
                    }
                    if (cell.returnedCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White.copy(alpha = 0.8f) else LoyGreen),
                        )
                    }
                    Text(
                        text = "${cell.totalCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else LoyTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
    }
}

@Composable
fun RentalScheduleListCard(
    title: String,
    selectedDate: LocalDate,
    schedules: List<SaleWithItems>,
    currencySymbol: String,
    onOpenOrder: (String) -> Unit,
    onBookForDate: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    val now = remember(schedules) { nowMs() }

    GroupCard(modifier = modifier.testTag("rental_schedule_list_card")) {
        Column(
            modifier = Modifier.padding(metrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatFullLocalDate(selectedDate)} · ${schedules.size} ${if (schedules.size == 1) "event" else "events"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                if (onBookForDate != null) {
                    OutlinedButton(
                        onClick = onBookForDate,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 40.dp).testTag("calendar_book_date_button"),
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Use Date", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (schedules.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = LoySurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(20.dp))
                        Column {
                            Text("All fleet equipment open", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("No overlapping rentals scheduled on this date.", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    schedules.forEach { entry ->
                        val r = entry.receipt
                        val overdue = r.isOverdue(now)
                        val toolsSummary = entry.lines.take(3).joinToString(", ") { "${formatQty(it.qty)}× ${it.name}" } +
                            if (entry.lines.size > 3) " +${entry.lines.size - 3} more" else ""

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurfaceVariant,
                            border = BorderStroke(1.dp, if (overdue) LoyWarning.copy(alpha = 0.45f) else LoyBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenOrder(r.id) }
                                .testTag("calendar_schedule_item_${r.id}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            text = r.eventName.ifBlank { "Order #${r.number.toString().padStart(4, '0')}" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LoyTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        PillBadge(
                                            text = when {
                                                r.isReturned -> "Returned"
                                                overdue -> "Overdue"
                                                else -> "Out ${r.rentalDays}d"
                                            },
                                            backgroundColor = when {
                                                r.isReturned -> LoyGreenLight
                                                overdue -> Color(0xFFFFF3E0)
                                                else -> LoyInfoLight
                                            },
                                            textColor = when {
                                                r.isReturned -> LoyGreen
                                                overdue -> LoyWarning
                                                else -> LoyInfo
                                            },
                                        )
                                    }
                                    Text(
                                        text = formatCents(r.totalCents, currencySymbol),
                                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!r.customerName.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(13.dp))
                                            Text(r.customerName, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                                        }
                                    }
                                    if (r.eventVenue.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = LoyTextSecondary, modifier = Modifier.size(13.dp))
                                            Text(r.eventVenue, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.Event, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "#${r.number.toString().padStart(4, '0')} · ${formatShortDate(r.rentalStartMs)} → ${formatShortDate(r.rentalEndMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextPrimary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }

                                if (toolsSummary.isNotBlank()) {
                                    Text(
                                        text = toolsSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EquipmentAvailabilityCard(
    availabilityList: List<EquipmentDayAvailability>,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    val fullyBookedCount = remember(availabilityList) {
        availabilityList.count { it.status == AvailabilityStatus.FULLY_BOOKED }
    }
    val reservedItemsCount = remember(availabilityList) {
        availabilityList.count { it.reservedUnits > 0.0 }
    }

    GroupCard(modifier = modifier.testTag("equipment_availability_card")) {
        Column(
            modifier = Modifier.padding(metrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Equipment Availability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "$reservedItemsCount tools booked · $fullyBookedCount fully booked",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (fullyBookedCount > 0) LoyWarning else LoyTextSecondary,
                    )
                }
            }

            CompactSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search tents, chairs, lighting...",
                inputTestTag = "calendar_equipment_search",
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All (${availabilityList.size})", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreen,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    val isSel = selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelectCategory(if (isSel) null else cat.id) },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LoyGreen,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                }
            }

            val visible = remember(availabilityList, selectedCategoryId, searchQuery) {
                val q = searchQuery.trim().lowercase()
                availabilityList
                    .filter { entry ->
                        (selectedCategoryId == null || entry.item.categoryId == selectedCategoryId) &&
                            (q.isEmpty() || entry.item.name.lowercase().contains(q) || entry.item.sku.lowercase().contains(q))
                    }
                    .sortedWith(compareByDescending<EquipmentDayAvailability> { it.reservedUnits }.thenBy { it.item.name })
            }

            if (visible.isEmpty()) {
                Text(
                    "No matching equipment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visible.forEach { entry ->
                        val statusColor = when (entry.status) {
                            AvailabilityStatus.AVAILABLE, AvailabilityStatus.UNLIMITED -> LoyGreen
                            AvailabilityStatus.LOW -> LoyWarning
                            AvailabilityStatus.FULLY_BOOKED -> LoyError
                        }
                        val statusBg = when (entry.status) {
                            AvailabilityStatus.AVAILABLE, AvailabilityStatus.UNLIMITED -> LoyGreenLight
                            AvailabilityStatus.LOW -> Color(0xFFFFF3E0)
                            AvailabilityStatus.FULLY_BOOKED -> LoyErrorLight
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = LoySurfaceVariant,
                            modifier = Modifier.fillMaxWidth().testTag("calendar_avail_${entry.item.id}"),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        ItemThumbnail(item = entry.item, size = 30.dp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                entry.item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${formatCents(entry.item.priceCents, currencySymbol)}/d" +
                                                    if (entry.reservedUnits > 0.0) " · ${formatQty(entry.reservedUnits)} reserved" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LoyTextSecondary,
                                            )
                                        }
                                    }
                                    PillBadge(
                                        text = when (entry.status) {
                                            AvailabilityStatus.UNLIMITED -> "Open"
                                            AvailabilityStatus.FULLY_BOOKED -> "0 avail"
                                            else -> "${formatQty(entry.availableUnits)} / ${formatQty(entry.totalCapacity)} avail"
                                        },
                                        backgroundColor = statusBg,
                                        textColor = statusColor,
                                    )
                                }
                                if (entry.item.trackStock && entry.totalCapacity > 0.0) {
                                    LinearProgressIndicator(
                                        progress = { entry.utilizationRatio },
                                        color = statusColor,
                                        trackColor = LoyBorder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
