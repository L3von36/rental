package com.loypos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loypos.app.core.checkCartAvailabilityConflicts
import com.loypos.app.core.dayStartMs
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatQty
import com.loypos.app.core.formatShortDate
import com.loypos.app.core.localDateFromMs
import com.loypos.app.core.newId
import com.loypos.app.core.nowMs
import com.loypos.app.core.parseAmountToCents
import com.loypos.app.data.local.CustomerEntity
import com.loypos.app.ui.LoyPosDataState
import com.loypos.app.ui.LoyPosViewModel
import com.loypos.app.ui.TransactionViewModel
import com.loypos.app.ui.UiSessionState
import com.loypos.app.ui.components.CurrencyNumPad
import com.loypos.app.ui.components.CustomerPickerSheet
import com.loypos.app.ui.components.DiscountBottomSheet
import com.loypos.app.ui.components.GroupCard
import com.loypos.app.ui.components.PillBadge
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.LocalLoyWindowSize
import com.loypos.app.ui.theme.LoyBackground
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: LoyPosViewModel,
    transactionViewModel: TransactionViewModel,
    session: UiSessionState,
    data: LoyPosDataState,
    onBack: () -> Unit,
    onOpenSchedule: () -> Unit = {},
    onPaymentCompleted: () -> Unit,
) {
    val cartSession by transactionViewModel.cartSession.collectAsStateWithLifecycle()
    val totals by transactionViewModel.cartTotals.collectAsStateWithLifecycle()
    val sym = data.settings.currencySymbol
    val totalCents = totals.totalCents
    val selectedCustomer = data.customers.firstOrNull { it.id == cartSession.customerId }
    val windowSize = LocalLoyWindowSize.current
    val metrics = LocalLoyMetrics.current

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showCustomerSheet by remember { mutableStateOf(false) }
    var showDiscountSheet by remember { mutableStateOf(false) }

    var eventNameInput by remember(cartSession.eventName) { mutableStateOf(cartSession.eventName) }
    var venueInput by remember(cartSession.eventVenue) { mutableStateOf(cartSession.eventVenue) }
    var noteInput by remember(cartSession.note) { mutableStateOf(cartSession.note) }
    var depositInput by remember(cartSession.depositCents) {
        mutableStateOf(if (cartSession.depositCents <= 0L) "" else (cartSession.depositCents / 100L).toString())
    }

    var paymentMethod by remember { mutableStateOf("cash") }
    var tenderedText by remember(totalCents) {
        mutableStateOf((totalCents / 100.0).let {
            if (totalCents % 100L == 0L) (totalCents / 100L).toString() else "%.2f".format(it)
        })
    }

    val conflicts = remember(
        cartSession.items,
        cartSession.rentalStartMs,
        cartSession.rentalEndMs,
        data.items,
        data.receipts,
    ) {
        checkCartAvailabilityConflicts(
            cartItems = cartSession.items,
            catalog = data.items,
            receipts = data.receipts,
            rangeStartMs = cartSession.rentalStartMs,
            rangeEndMs = cartSession.rentalEndMs,
        )
    }

    val paidCents = if (paymentMethod == "card") {
        totalCents
    } else {
        parseAmountToCents(tenderedText) ?: 0L
    }
    val canCharge = cartSession.items.isNotEmpty() && (paymentMethod == "card" || paidCents >= totalCents)
    val diffCents = paidCents - totalCents

    val quickAmounts = remember(totalCents) {
        val exact = totalCents
        val units = totalCents / 100.0
        val next50 = (ceil(units / 50.0) * 50.0).toLong() * 100L
        val next100 = (ceil(units / 100.0) * 100.0).toLong() * 100L
        listOf(exact, next50, next100).distinct().filter { it >= exact }.take(3)
    }

    val onCompletePayment: () -> Unit = {
        transactionViewModel.setEventDetails(eventNameInput, venueInput)
        transactionViewModel.setCartNote(noteInput)
        transactionViewModel.setDepositCents(parseAmountToCents(depositInput) ?: 0L)
        transactionViewModel.finalizeSale(
            paymentType = paymentMethod,
            paidCents = paidCents,
            settings = data.settings,
            customer = selectedCustomer,
            branchId = session.activeBranchId,
            cashier = session.currentUser,
        ) {
            viewModel.refreshHealthAndBackups()
            onPaymentCompleted()
        }
    }

    val rentalPeriodCard: @Composable () -> Unit = {
        val today = remember { LocalDate.now() }
        val startLocalDate = remember(cartSession.rentalStartMs) { localDateFromMs(cartSession.rentalStartMs) }
        GroupCard(modifier = Modifier.testTag("checkout_rental_duration_card")) {
            Column(
                modifier = Modifier.padding(metrics.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Event, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                        Text("Rental Duration & Dates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    PillBadge(
                        text = "${cartSession.rentalDays} ${if (cartSession.rentalDays == 1) "day" else "days"}",
                        backgroundColor = LoyGreenLight,
                        textColor = LoyGreen,
                    )
                }

                // Pickup Date row + Calendar link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = metrics.minTouchTarget)
                            .testTag("checkout_start_date_button"),
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pickup ${formatShortDate(cartSession.rentalStartMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = LoySurfaceVariant,
                        modifier = Modifier.weight(1f).heightIn(min = metrics.minTouchTarget),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Return due", style = MaterialTheme.typography.labelSmall, color = LoyTextSecondary)
                            Text(
                                formatShortDate(cartSession.rentalEndMs),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Quick start date pills
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quickStarts = listOf(
                        "Today" to today,
                        "Tomorrow" to today.plusDays(1),
                        "Next Week" to today.plusDays(7),
                    )
                    items(quickStarts) { (label, date) ->
                        FilterChip(
                            selected = startLocalDate == date,
                            onClick = { transactionViewModel.setRentalPeriod(dayStartMs(date), cartSession.rentalDays) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(30.dp),
                        )
                    }
                }

                // Duration stepper + presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Number of rental days", style = MaterialTheme.typography.bodyMedium, color = LoyTextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { transactionViewModel.setRentalDays((cartSession.rentalDays - 1).coerceAtLeast(1)) },
                            modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("checkout_days_minus"),
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Decrease rental days")
                        }
                        Text(
                            "${cartSession.rentalDays}d",
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                            fontWeight = FontWeight.Bold,
                            color = LoyGreen,
                        )
                        IconButton(
                            onClick = { transactionViewModel.setRentalDays(cartSession.rentalDays + 1) },
                            modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget).testTag("checkout_days_plus"),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Increase rental days")
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(1, 2, 3, 5, 7, 14)) { d ->
                        FilterChip(
                            selected = cartSession.rentalDays == d,
                            onClick = { transactionViewModel.setRentalDays(d) },
                            label = { Text("${d}d", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LoyGreen,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.height(30.dp).testTag("checkout_duration_${d}d"),
                        )
                    }
                }

                if (conflicts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFFFF3E0),
                        border = BorderStroke(1.dp, LoyWarning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("checkout_conflict_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LoyWarning, modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Schedule conflict detected",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LoyWarning,
                                )
                                conflicts.forEach { c ->
                                    Text(
                                        "${c.itemName}: requested ${formatQty(c.requestedQty)}, ${formatQty(c.availableQty)} available on selected dates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoyTextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val customerAndEventCard: @Composable () -> Unit = {
        GroupCard(modifier = Modifier.testTag("checkout_customer_event_card")) {
            Column(
                modifier = Modifier.padding(metrics.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = LoyGreen, modifier = Modifier.size(18.dp))
                        Text("Customer & Event Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showCustomerSheet = true },
                        modifier = Modifier.heightIn(min = 40.dp).testTag("checkout_select_customer_button"),
                    ) {
                        Text(if (selectedCustomer == null) "Select / + New" else "Change")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (selectedCustomer != null) LoyGreenLight.copy(alpha = 0.55f) else LoySurfaceVariant,
                    border = BorderStroke(1.dp, if (selectedCustomer != null) LoyGreen.copy(alpha = 0.35f) else LoyBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { showCustomerSheet = true }
                        .testTag("checkout_customer_summary"),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCustomer?.name ?: "Walk-in Customer (Tap to assign or create)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyTextPrimary,
                            )
                            val subtitle = listOfNotNull(
                                selectedCustomer?.phone?.takeIf { it.isNotBlank() },
                                selectedCustomer?.email?.takeIf { it.isNotBlank() },
                            ).joinToString(" · ")
                            if (subtitle.isNotEmpty()) {
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                            }
                        }
                        if (selectedCustomer != null) {
                            PillBadge(text = "${selectedCustomer.points} pts")
                        }
                    }
                }

                OutlinedTextField(
                    value = eventNameInput,
                    onValueChange = {
                        eventNameInput = it
                        transactionViewModel.setEventDetails(it, venueInput)
                    },
                    label = { Text("Event name (e.g. Summer Gala)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checkout_event_name_input"),
                )
                OutlinedTextField(
                    value = venueInput,
                    onValueChange = {
                        venueInput = it
                        transactionViewModel.setEventDetails(eventNameInput, it)
                    },
                    label = { Text("Venue / delivery address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("checkout_venue_input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = depositInput,
                        onValueChange = {
                            depositInput = it
                            transactionViewModel.setDepositCents(parseAmountToCents(it) ?: 0L)
                        },
                        label = { Text("Deposit ($sym)") },
                        leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("checkout_deposit_input"),
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = {
                            noteInput = it
                            transactionViewModel.setCartNote(it)
                        },
                        label = { Text("Delivery note") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("checkout_note_input"),
                    )
                }
            }
        }
    }

    val equipmentCostsCard: @Composable () -> Unit = {
        val totalUnits = cartSession.items.sumOf { it.qty }
        val depositCents = parseAmountToCents(depositInput) ?: cartSession.depositCents
        GroupCard(modifier = Modifier.testTag("checkout_cost_breakdown_card")) {
            Column(
                modifier = Modifier.padding(metrics.cardPadding),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Equipment Costs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${cartSession.items.size} items · ${formatQty(totalUnits)} units",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }

                cartSession.items.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                line.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${formatQty(line.qty)} × ${formatCents(line.priceCents, sym)}/d × ${line.rentalDays}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                        }
                        Text(
                            formatCents(line.lineTotalCents, sym),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.bodyMedium),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                HorizontalDivider(color = LoyBorder)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Equipment subtotal (${cartSession.rentalDays}d)", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                    Text(formatCents(totals.subtotalCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Discount", style = MaterialTheme.typography.bodySmall, color = if (totals.discountCents > 0L) LoyGreen else LoyTextSecondary)
                        TextButton(
                            onClick = { showDiscountSheet = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp).testTag("checkout_edit_discount"),
                        ) {
                            Icon(Icons.Outlined.LocalOffer, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(if (totals.discountCents > 0L) "Edit" else "Add", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        if (totals.discountCents > 0L) "-${formatCents(totals.discountCents, sym)}" else formatCents(0L, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall),
                        color = if (totals.discountCents > 0L) LoyGreen else LoyTextSecondary,
                    )
                }
                if (totals.taxCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (data.settings.taxIncluded) "Tax (included)" else "Tax (${formatQty(data.settings.taxRatePercent)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextSecondary,
                        )
                        Text(formatCents(totals.taxCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall))
                    }
                }
                if (depositCents > 0L) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Refundable security deposit", style = MaterialTheme.typography.bodySmall, color = LoyInfo)
                        Text(formatCents(depositCents, sym), style = TabularMoneyStyle.merge(MaterialTheme.typography.bodySmall), color = LoyInfo)
                    }
                }
                HorizontalDivider(color = LoyBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Total Rental Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        formatCents(totalCents, sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.headlineSmall),
                        fontWeight = FontWeight.Bold,
                        color = LoyGreen,
                        modifier = Modifier.testTag("checkout_order_total_value"),
                    )
                }
            }
        }
    }

    val methodSelectorRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
        ) {
            MethodCard(
                selected = paymentMethod == "cash",
                icon = Icons.Outlined.Payments,
                label = "Cash",
                onClick = { paymentMethod = "cash" },
                modifier = Modifier.weight(1f).testTag("method_cash"),
            )
            MethodCard(
                selected = paymentMethod == "card",
                icon = Icons.Outlined.CreditCard,
                label = "Card",
                onClick = { paymentMethod = "card" },
                modifier = Modifier.weight(1f).testTag("method_card"),
            )
        }
    }

    val tenderStatusRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LoySurface,
                border = BorderStroke(1.dp, LoyGreen),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(metrics.cardPadding)) {
                    Text("Cash received", style = MaterialTheme.typography.bodySmall, color = LoyTextSecondary)
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = "$sym${tenderedText.ifEmpty { "0" }}",
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (diffCents >= 0L) LoyGreenLight else LoySurfaceVariant,
                border = BorderStroke(1.dp, if (diffCents >= 0L) LoyGreen else LoyBorder),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(metrics.cardPadding)) {
                    Text(
                        if (diffCents >= 0L) "Change" else "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diffCents >= 0L) LoyGreen else LoyError,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = formatCents(kotlin.math.abs(diffCents), sym),
                        style = TabularMoneyStyle.merge(MaterialTheme.typography.titleLarge),
                        fontWeight = FontWeight.Bold,
                        color = if (diffCents >= 0L) LoyGreen else LoyError,
                    )
                }
            }
        }
    }

    val chargeSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(metrics.itemGap)) {
            if (!canCharge && paymentMethod == "cash") {
                Text(
                    text = "Enter at least ${formatCents(totalCents, sym)} to complete cash payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyError,
                )
            }
            Button(
                onClick = onCompletePayment,
                enabled = canCharge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("payment_charge_button"),
            ) {
                Text("Confirm & Charge ${formatCents(totalCents, sym)}", fontWeight = FontWeight.Bold)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = metrics.topBarHeight,
                title = { Text("Order Checkout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.accessibleTouchTarget(metrics.minTouchTarget)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSchedule,
                        modifier = Modifier
                            .accessibleTouchTarget(metrics.minTouchTarget)
                            .testTag("checkout_open_calendar_button"),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = "View Equipment Calendar",
                            tint = LoyGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LoySurface),
            )
        },
        containerColor = LoyBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(metrics.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!windowSize.isCompact) {
                Row(
                    modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.screenPadding),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                    ) {
                        rentalPeriodCard()
                        customerAndEventCard()
                        equipmentCostsCard()
                    }
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                    ) {
                        methodSelectorRow()
                        if (paymentMethod == "cash") {
                            tenderStatusRow()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                quickAmounts.forEach { amt ->
                                    FilterChip(
                                        selected = paidCents == amt,
                                        onClick = {
                                            tenderedText = if (amt % 100L == 0L) (amt / 100L).toString() else "%.2f".format(amt / 100.0)
                                        },
                                        label = { Text(formatCents(amt, sym), style = MaterialTheme.typography.labelMedium) },
                                        modifier = Modifier.height(metrics.chipHeight),
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { tenderedText = "" },
                                    modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("payment_clear_cash"),
                                ) {
                                    Text("Clear")
                                }
                            }
                            CurrencyNumPad(
                                onKey = { d ->
                                    if (d == "." && tenderedText.contains(".")) return@CurrencyNumPad
                                    tenderedText = if (tenderedText == "0" && d != ".") d else tenderedText + d
                                },
                                onBackspace = {
                                    if (tenderedText.isNotEmpty()) tenderedText = tenderedText.dropLast(1)
                                },
                                onClear = { tenderedText = "" },
                                allowDecimal = true,
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = LoySurface,
                                border = BorderStroke(1.dp, LoyBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Confirm card terminal approval for ${formatCents(totalCents, sym)} before confirming the rental.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LoyTextSecondary,
                                    modifier = Modifier.padding(metrics.cardPadding),
                                )
                            }
                        }
                        chargeSection()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionGap),
                ) {
                    rentalPeriodCard()
                    customerAndEventCard()
                    equipmentCostsCard()
                    methodSelectorRow()
                    if (paymentMethod == "cash") {
                        tenderStatusRow()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            quickAmounts.forEach { amt ->
                                FilterChip(
                                    selected = paidCents == amt,
                                    onClick = {
                                        tenderedText = if (amt % 100L == 0L) (amt / 100L).toString() else "%.2f".format(amt / 100.0)
                                    },
                                    label = { Text(formatCents(amt, sym), style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.height(metrics.chipHeight),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { tenderedText = "" },
                                modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("payment_clear_cash"),
                            ) {
                                Text("Clear")
                            }
                        }
                        CurrencyNumPad(
                            onKey = { d ->
                                if (d == "." && tenderedText.contains(".")) return@CurrencyNumPad
                                tenderedText = if (tenderedText == "0" && d != ".") d else tenderedText + d
                            },
                            onBackspace = {
                                if (tenderedText.isNotEmpty()) tenderedText = tenderedText.dropLast(1)
                            },
                            onClear = { tenderedText = "" },
                            allowDecimal = true,
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LoySurface,
                            border = BorderStroke(1.dp, LoyBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Confirm card terminal approval for ${formatCents(totalCents, sym)} before confirming the rental.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LoyTextSecondary,
                                modifier = Modifier.padding(metrics.cardPadding),
                            )
                        }
                    }
                    chargeSection()
                }
            }
        }
    }

    if (showStartDatePicker) {
        val currentStartDate = localDateFromMs(cartSession.rentalStartMs)
        val initialUtcMs = currentStartDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMs)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val utcMillis = datePickerState.selectedDateMillis
                        if (utcMillis != null) {
                            val chosenDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            transactionViewModel.setRentalPeriod(
                                startMs = dayStartMs(chosenDate),
                                days = cartSession.rentalDays,
                            )
                        }
                        showStartDatePicker = false
                    },
                    modifier = Modifier.testTag("checkout_date_confirm"),
                ) {
                    Text("Set Pickup Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
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

    if (showDiscountSheet) {
        DiscountBottomSheet(
            current = cartSession.discount,
            currencySymbol = sym,
            onApply = { transactionViewModel.setDiscount(it) },
            onDismiss = { showDiscountSheet = false },
        )
    }
}

@Composable
private fun MethodCard(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) LoyGreenLight else LoySurface,
        border = BorderStroke(1.5.dp, if (selected) LoyGreen else LoyBorder),
        modifier = modifier
            .heightIn(min = metrics.minTouchTarget)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) LoyGreen else LoyTextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) LoyGreen else LoyTextPrimary,
            )
        }
    }
}
