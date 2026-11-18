package com.loypos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.computeEquipmentAvailabilityForRange
import com.loypos.app.core.dayBoundsFor
import com.loypos.app.core.dayStartMs
import com.loypos.app.core.localDateFromMs
import com.loypos.app.core.nowMs
import com.loypos.app.core.schedulesForDate
import com.loypos.app.core.upcomingRentalsFrom
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.EquipmentAvailabilityCard
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.RentalCalendarGridCard
import com.loypos.app.ui.components.RentalScheduleListCard
import com.loypos.app.ui.components.StatBlock
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.accessibleTouchTarget
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    transactionViewModel: TransactionViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onBack: () -> Unit,
    onOpenReceipt: (String) -> Unit,
    onBookOnDate: () -> Unit,
) {
    val cartSession by transactionViewModel.cartSession.collectAsStateWithLifecycle()
    val metrics = LocalLoyMetrics.current
    val windowSize = LocalLoyWindowSize.current
    val sym = data.settings.currencySymbol

    val initialDate = remember(cartSession.rentalStartMs) {
        localDateFromMs(cartSession.rentalStartMs)
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var scheduleModeIdx by remember { mutableIntStateOf(0) } // 0 = Selected Date, 1 = Upcoming
    var selectedCatId by remember { mutableStateOf<String?>(null) }
    var equipSearch by remember { mutableStateOf("") }

    val branchId = session.activeBranchId
    val branchReceipts = remember(data.receipts, branchId) {
        data.receipts.filter { r ->
            branchId == null || r.receipt.branchId == null || r.receipt.branchId == branchId
        }
    }
    val branchItems = remember(data.items, branchId) {
        data.items.filter { it.availableInBranch(branchId) }
    }

    val currentTimeMs = remember(branchReceipts) { nowMs() }
    val today = remember { LocalDate.now() }
    val activeOutCount = remember(branchReceipts) { branchReceipts.count { it.receipt.isRentalOut } }
    val overdueCount = remember(branchReceipts, currentTimeMs) { branchReceipts.count { it.receipt.isOverdue(currentTimeMs) } }
    val upcomingList = remember(today, branchReceipts) { upcomingRentalsFrom(today, branchReceipts) }
    val selectedDaySchedules = remember(selectedDate, branchReceipts) { schedulesForDate(selectedDate, branchReceipts) }
    val dayBounds = remember(selectedDate) { dayBoundsFor(selectedDate) }
    val equipmentAvailability = remember(branchItems, branchReceipts, dayBounds, data.settings.lowStockThreshold) {
        computeEquipmentAvailabilityForRange(
            items = branchItems,
            receipts = branchReceipts,
            rangeStartMs = dayBounds.startMs,
            rangeEndMs = dayBounds.endMs,
            lowThreshold = data.settings.lowStockThreshold,
        )
    }

    val startBookingForSelectedDate: () -> Unit = {
        transactionViewModel.setRentalPeriod(
            startMs = dayStartMs(selectedDate),
            days = cartSession.rentalDays.coerceAtLeast(1),
        )
        onBookOnDate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                title = {
                    Column {
                        Text(
                            "Schedule & Availability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${upcomingList.size} upcoming rentals · ${branchItems.size} tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyGreen,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("calendar_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        val scheduleAndAvailabilityContent: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = scheduleModeIdx == 0,
                    onClick = { scheduleModeIdx = 0 },
                    label = { Text("Day Schedule (${selectedDaySchedules.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LoyGreenLight,
                        selectedLabelColor = LoyGreen,
                    ),
                    modifier = Modifier.testTag("calendar_tab_selected_day"),
                )
                FilterChip(
                    selected = scheduleModeIdx == 1,
                    onClick = { scheduleModeIdx = 1 },
                    label = { Text("Upcoming (${upcomingList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LoyGreenLight,
                        selectedLabelColor = LoyGreen,
                    ),
                    modifier = Modifier.testTag("calendar_tab_upcoming"),
                )
            }

            RentalScheduleListCard(
                title = if (scheduleModeIdx == 0) "Schedules on Date" else "Upcoming Event Schedules",
                selectedDate = selectedDate,
                schedules = if (scheduleModeIdx == 0) selectedDaySchedules else upcomingList,
                currencySymbol = sym,
                onOpenOrder = onOpenReceipt,
                onBookForDate = startBookingForSelectedDate,
            )

            EquipmentAvailabilityCard(
                availabilityList = equipmentAvailability,
                categories = data.categories,
                selectedCategoryId = selectedCatId,
                onSelectCategory = { selectedCatId = it },
                searchQuery = equipSearch,
                onSearchChange = { equipSearch = it },
                currencySymbol = sym,
            )
        }

        if (windowSize.useSplitPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.46f).fillMaxHeight(),
                    contentPadding = PaddingValues(metrics.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                ) {
                    item {
                        GroupCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(metrics.cardPadding),
                                horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
                            ) {
                                StatBlock("Active Out", "$activeOutCount", valueColor = LoyInfo, modifier = Modifier.weight(1f))
                                StatBlock("Overdue", "$overdueCount", valueColor = if (overdueCount > 0) LoyError else LoyGreen, modifier = Modifier.weight(1f))
                                StatBlock("Upcoming", "${upcomingList.size}", valueColor = LoyGreen, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        RentalCalendarGridCard(
                            displayedMonth = displayedMonth,
                            selectedDate = selectedDate,
                            receipts = branchReceipts,
                            onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                            onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                            onJumpToToday = {
                                selectedDate = today
                                displayedMonth = YearMonth.from(today)
                            },
                            onSelectDate = { d ->
                                selectedDate = d
                                displayedMonth = YearMonth.from(d)
                            },
                        )
                    }
                    item {
                        Button(
                            onClick = startBookingForSelectedDate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = metrics.minTouchTarget)
                                .testTag("calendar_start_rental_cta"),
                        ) {
                            Icon(Icons.Outlined.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Book Equipment for $selectedDate")
                        }
                    }
                }

                HorizontalDivider(
                    color = LoyBorder,
                    modifier = Modifier.width(1.dp).fillMaxHeight().background(LoyBorder),
                )

                LazyColumn(
                    modifier = Modifier.weight(0.54f).fillMaxHeight(),
                    contentPadding = PaddingValues(metrics.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                ) {
                    item { scheduleAndAvailabilityContent() }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(metrics.screenPadding),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
            ) {
                item {
                    GroupCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(metrics.cardPadding),
                            horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
                        ) {
                            StatBlock("Out", "$activeOutCount", valueColor = LoyInfo, modifier = Modifier.weight(1f))
                            StatBlock("Overdue", "$overdueCount", valueColor = if (overdueCount > 0) LoyWarning else LoyGreen, modifier = Modifier.weight(1f))
                            StatBlock("Upcoming", "${upcomingList.size}", valueColor = LoyGreen, modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    RentalCalendarGridCard(
                        displayedMonth = displayedMonth,
                        selectedDate = selectedDate,
                        receipts = branchReceipts,
                        onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                        onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                        onJumpToToday = {
                            selectedDate = today
                            displayedMonth = YearMonth.from(today)
                        },
                        onSelectDate = { d ->
                            selectedDate = d
                            displayedMonth = YearMonth.from(d)
                        },
                    )
                }
                item { scheduleAndAvailabilityContent() }
            }
        }
    }
}
