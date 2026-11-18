package com.loypos.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.RentalAgreementSearchOutput
import com.loypos.app.core.RentalAgreementSearchResult
import com.loypos.app.core.RentalAgreementTimeframe
import com.loypos.app.core.RentalTimeframeFilter
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatShortDate
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.accessibleTouchTarget

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalRentalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedTimeframe: RentalTimeframeFilter,
    onSelectTimeframe: (RentalTimeframeFilter) -> Unit,
    searchOutput: RentalAgreementSearchOutput,
    currencySymbol: String,
    onSelectAgreement: (RentalAgreementSearchResult) -> Unit,
    onViewAllRentals: () -> Unit,
    accentColor: Color = LoyGreen,
    accentLight: Color = LoyGreenLight,
    testTagPrefix: String = "dashboard_search",
) {
    var showAllMatches by remember(query, selectedTimeframe) { mutableStateOf(false) }
    val showResultsPanel = query.isNotBlank() || selectedTimeframe != RentalTimeframeFilter.ALL

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LoySurface,
        border = BorderStroke(1.dp, if (showResultsPanel) accentColor.copy(alpha = 0.45f) else LoyBorder),
        shadowElevation = if (showResultsPanel) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentLight.copy(alpha = 0.48f),
                                LoySurface,
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Find Rental Agreements",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = LoyTextPrimary,
                            )
                        }
                        Text(
                            text = "${searchOutput.allCount} ${if (searchOutput.allCount == 1) "agreement" else "agreements"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = "Search by customer name or equipment ID (e.g. eq-001, TENT-2020)...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search rental agreements",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier
                                        .accessibleTouchTarget(48.dp)
                                        .testTag("${testTagPrefix}_clear"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                        tint = LoyTextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = LoySurface,
                            unfocusedContainerColor = LoySurface,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = LoyBorder,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${testTagPrefix}_input"),
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                    ) {
                        val chipEntries = listOf(
                            RentalTimeframeFilter.ALL to searchOutput.allCount,
                            RentalTimeframeFilter.CURRENT to searchOutput.currentCount,
                            RentalTimeframeFilter.FUTURE to searchOutput.futureCount,
                            RentalTimeframeFilter.PAST to searchOutput.pastCount,
                        )
                        items(chipEntries, key = { it.first.name }) { (filter, count) ->
                            val selected = selectedTimeframe == filter
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onSelectTimeframe(
                                        if (selected && filter != RentalTimeframeFilter.ALL) RentalTimeframeFilter.ALL else filter
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${filter.label} ($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentLight,
                                    selectedLabelColor = accentColor,
                                ),
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .testTag("${testTagPrefix}_filter_${filter.name.lowercase()}"),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showResultsPanel,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = LoyBorder)
                    if (searchOutput.results.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = LoyTextSecondary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "No matching rental agreements",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LoyTextPrimary,
                            )
                            Text(
                                text = "Try another customer name, equipment ID (e.g. eq-001), SKU, or switch timeframe filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LoyTextSecondary,
                            )
                            TextButton(
                                onClick = {
                                    onQueryChange("")
                                    onSelectTimeframe(RentalTimeframeFilter.ALL)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("${testTagPrefix}_reset"),
                            ) {
                                Text("Reset search & filters", color = accentColor)
                            }
                        }
                    } else {
                        val visibleLimit = if (showAllMatches) searchOutput.results.size else 6
                        val displayed = searchOutput.results.take(visibleLimit)

                        displayed.forEachIndexed { idx, result ->
                            if (idx > 0) HorizontalDivider(color = LoyBorder)
                            RentalAgreementSearchResultRow(
                                result = result,
                                currencySymbol = currencySymbol,
                                accentColor = accentColor,
                                onClick = { onSelectAgreement(result) },
                                testTag = "${testTagPrefix}_result_${result.id}",
                            )
                        }

                        if (searchOutput.results.size > 6 || searchOutput.results.size > 1) {
                            HorizontalDivider(color = LoyBorder)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (searchOutput.results.size > 6) {
                                    TextButton(
                                        onClick = { showAllMatches = !showAllMatches },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Text(
                                            text = if (showAllMatches) "Show fewer" else "Show all ${searchOutput.results.size} matches",
                                            color = accentColor,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${searchOutput.results.size} matches",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LoyTextSecondary,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                                TextButton(
                                    onClick = onViewAllRentals,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text("Open Rental Ledger", color = accentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RentalAgreementSearchResultRow(
    result: RentalAgreementSearchResult,
    currencySymbol: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val timeframeBadgeColors = when {
        result.isRefunded -> LoyErrorLight to LoyError
        result.isOverdue -> LoyErrorLight to LoyError
        result.timeframe == RentalAgreementTimeframe.CURRENT -> Color(0xFFFFF3E0) to LoyWarning
        result.timeframe == RentalAgreementTimeframe.FUTURE -> Color(0xFFE3F2FD) to LoyInfo
        else -> LoyGreenLight to LoyGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = result.agreementNumberLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                PillBadge(
                    text = "${result.timeframe.label} · ${result.statusBadge}",
                    backgroundColor = timeframeBadgeColors.first,
                    textColor = timeframeBadgeColors.second,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = LoyTextPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = result.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LoyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (result.eventName.isNotBlank()) {
                    Text(
                        text = "· ${result.eventName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = LoyTextSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "${formatShortDate(result.rentalStartMs)} → ${formatShortDate(result.rentalEndMs)} (${result.rentalDays}d)",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )
            }

            if (result.equipmentSummaries.isNotEmpty()) {
                Text(
                    text = result.equipmentSummaries.take(3).joinToString(" · ") +
                        if (result.equipmentSummaries.size > 3) " +${result.equipmentSummaries.size - 3} more" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = LoyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (result.equipmentIds.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    result.equipmentIds.take(4).forEach { eqId ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LoySurfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    tint = LoyTextSecondary,
                                    modifier = Modifier.size(11.dp),
                                )
                                Text(
                                    text = eqId,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = LoyTextPrimary,
                                )
                            }
                        }
                    }
                    if (result.equipmentIds.size > 4) {
                        Text(
                            text = "+${result.equipmentIds.size - 4} IDs",
                            style = MaterialTheme.typography.labelSmall,
                            color = LoyTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCents(result.totalCents, currencySymbol),
                style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                fontWeight = FontWeight.Bold,
                color = if (result.isRefunded) LoyError else LoyTextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Open agreement",
                tint = LoyTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
