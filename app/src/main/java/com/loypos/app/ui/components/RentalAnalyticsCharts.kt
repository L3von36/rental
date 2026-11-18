package com.loypos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loypos.app.core.formatCents
import com.loypos.app.core.formatCentsShort
import com.loypos.app.core.formatQty
import com.loypos.app.data.local.EquipmentUtilizationPoint
import com.loypos.app.data.local.MonthlyIncomePoint
import com.loypos.app.ui.theme.BackOfficeNavy
import com.loypos.app.ui.theme.BackOfficeTeal
import com.loypos.app.ui.theme.BackOfficeTealLight
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyErrorLight
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoyInfo
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class MonthlyChartStyle { BARS, LINE }

@Composable
fun MonthlyRentalIncomeChartCard(
    points: List<MonthlyIncomePoint>,
    selectedMonths: Int,
    currencySymbol: String,
    onSelectMonths: (Int) -> Unit,
    accentColor: Color = LoyGreen,
    accentLight: Color = LoyGreenLight,
    subtitlePrefix: String = "All completed rentals",
    testTagPrefix: String = "dash_monthly_income",
    modifier: Modifier = Modifier,
) {
    var chartStyle by remember { mutableStateOf(MonthlyChartStyle.BARS) }

    val totalIncomeCents = remember(points) { points.sumOf { it.totalCents } }
    val totalOrders = remember(points) { points.sumOf { it.ordersCount } }
    val avgMonthlyCents = if (points.isEmpty()) 0L else totalIncomeCents / points.size
    val peakMonth = remember(points) {
        points.filter { it.totalCents > 0L }.maxByOrNull { it.totalCents }
    }
    val momDeltaPct = remember(points) {
        if (points.size < 2) return@remember null
        val prev = points[points.size - 2].totalCents
        val curr = points.last().totalCents
        if (prev <= 0L) null else (((curr - prev) * 100.0) / prev).roundToInt()
    }

    val barProducer = remember { CartesianChartModelProducer() }
    val lineProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        if (points.isNotEmpty()) {
            val values = points.map { it.revenueUnits }
            barProducer.runTransaction {
                columnSeries { series(values) }
            }
            lineProducer.runTransaction {
                lineSeries { series(values) }
            }
        }
    }

    val startAxisFormatter = remember(currencySymbol) {
        CartesianValueFormatter { _, value, _ ->
            formatCentsShort((value * 100.0).roundToLong(), currencySymbol)
        }
    }
    val bottomAxisFormatter = remember(points) {
        CartesianValueFormatter { _, value, _ ->
            points.getOrNull(value.toInt())?.label ?: ""
        }
    }
    val labelSpacing = if (selectedMonths > 6) 2 else 1

    val markerLabel = rememberTextComponent(
        color = Color.White,
        background = rememberShapeComponent(
            fill = fill(BackOfficeNavy),
            shape = CorneredShape.rounded(allPercent = 25),
        ),
        padding = insets(horizontal = 10.dp, vertical = 6.dp),
    )
    val markerIndicator = rememberShapeComponent(
        fill = fill(accentColor),
        shape = CorneredShape.Pill,
    )
    val markerGuideline = rememberLineComponent(
        fill = fill(accentColor.copy(alpha = 0.45f)),
        thickness = 1.dp,
    )
    val markerFormatter = remember(points, currencySymbol) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val idx = targets.firstOrNull()?.x?.roundToInt()
            val pt = if (idx != null) points.getOrNull(idx) else null
            if (pt != null) {
                "${pt.label}: ${formatCents(pt.totalCents, currencySymbol)} (${pt.ordersCount} orders)"
            } else {
                ""
            }
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerFormatter,
        indicator = { _ -> markerIndicator },
        indicatorSize = 10.dp,
        guideline = markerGuideline,
    )

    GroupCard(modifier = modifier.testTag("${testTagPrefix}_card")) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Monthly Rental Income",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LoyTextPrimary,
                        )
                        if (momDeltaPct != null) {
                            val positive = momDeltaPct >= 0
                            PillBadge(
                                text = "${if (positive) "+" else ""}$momDeltaPct% MoM",
                                backgroundColor = if (positive) LoyGreenLight else LoyErrorLight,
                                textColor = if (positive) LoyGreen else LoyError,
                            )
                        }
                    }
                    Text(
                        text = "$subtitlePrefix · $totalOrders ${if (totalOrders == 1) "order" else "orders"} over ${selectedMonths}M",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(6, 12).forEach { months ->
                        FilterChip(
                            selected = selectedMonths == months,
                            onClick = { onSelectMonths(months) },
                            label = {
                                Text(
                                    text = "${months}M",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentLight,
                                selectedLabelColor = accentColor,
                            ),
                            modifier = Modifier.height(28.dp).testTag("${testTagPrefix}_${months}m"),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatBlock(
                    label = "${selectedMonths}M Income",
                    value = formatCents(totalIncomeCents, currencySymbol),
                    valueColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "Monthly Avg",
                    value = formatCents(avgMonthlyCents, currencySymbol),
                    valueColor = LoyInfo,
                    modifier = Modifier.weight(1f),
                )
            }

            if (peakMonth != null) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = accentLight.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Peak Month · ${peakMonth.label} (${peakMonth.ordersCount} orders · ${formatQty(peakMonth.rentedUnitDays)} unit-days)",
                            style = MaterialTheme.typography.bodySmall,
                            color = LoyTextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatCents(peakMonth.totalCents, currencySymbol),
                            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleSmall),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = chartStyle == MonthlyChartStyle.BARS,
                    onClick = { chartStyle = MonthlyChartStyle.BARS },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {
                        Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_mode_bars"),
                ) {
                    Text("Monthly Bars")
                }
                SegmentedButton(
                    selected = chartStyle == MonthlyChartStyle.LINE,
                    onClick = { chartStyle = MonthlyChartStyle.LINE },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_mode_line"),
                ) {
                    Text("Trend Line")
                }
            }

            if (points.isNotEmpty()) {
                val pointComponent = rememberShapeComponent(
                    fill = fill(accentColor),
                    shape = CorneredShape.Pill,
                )
                val barChart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(
                                fill = fill(accentColor),
                                thickness = if (selectedMonths <= 6) 20.dp else 11.dp,
                                shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30),
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }),
                    ),
                    marker = marker,
                )
                val lineChart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(accentColor)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill(
                                        ShaderProvider.verticalGradient(
                                            accentColor.copy(alpha = 0.28f).toArgb(),
                                            Color.Transparent.toArgb(),
                                        )
                                    )
                                ),
                                pointProvider = LineCartesianLayer.PointProvider.single(
                                    LineCartesianLayer.Point(component = pointComponent, sizeDp = 6f)
                                ),
                                pointConnector = LineCartesianLayer.PointConnector.cubic(0.4f),
                            )
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = bottomAxisFormatter,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }),
                    ),
                    marker = marker,
                )

                CartesianChartHost(
                    chart = if (chartStyle == MonthlyChartStyle.BARS) barChart else lineChart,
                    modelProducer = if (chartStyle == MonthlyChartStyle.BARS) barProducer else lineProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .testTag("${testTagPrefix}_chart"),
                )
            }

            if (totalIncomeCents == 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentLight.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Complete rental orders to populate monthly income bars.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            } else {
                Text(
                    text = "Tip: Touch a month bar or point to view exact income and booking volume.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LoyTextSecondary,
                )
            }
        }
    }
}

@Composable
fun EquipmentUtilizationChartCard(
    byCategoryPoints: List<EquipmentUtilizationPoint>,
    topItemPoints: List<EquipmentUtilizationPoint>,
    liveOutMode: Boolean,
    byCategory: Boolean,
    onToggleLiveMode: (Boolean) -> Unit,
    onToggleByCategory: (Boolean) -> Unit,
    accentColor: Color = BackOfficeTeal,
    accentLight: Color = BackOfficeTealLight,
    testTagPrefix: String = "dash_utilization",
    modifier: Modifier = Modifier,
) {
    val activePoints = if (byCategory) byCategoryPoints else topItemPoints
    val chartPoints = remember(activePoints) { activePoints.take(6) }

    val totalOut = remember(byCategoryPoints) { byCategoryPoints.sumOf { it.activeOutUnits } }
    val totalCapacity = remember(byCategoryPoints) { byCategoryPoints.sumOf { it.totalCapacityUnits } }
    val overallPct = remember(byCategoryPoints, totalOut, totalCapacity, liveOutMode) {
        if (liveOutMode) {
            if (totalCapacity <= 0.0) 0.0 else (totalOut / totalCapacity * 100.0).coerceIn(0.0, 100.0)
        } else {
            val weighted = byCategoryPoints.sumOf { it.utilizationPercent * it.totalCapacityUnits }
            if (totalCapacity <= 0.0) 0.0 else (weighted / totalCapacity).coerceIn(0.0, 100.0)
        }
    }
    val topLeader = remember(activePoints) {
        activePoints.firstOrNull { it.utilizationPercent > 0.0 }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartPoints) {
        if (chartPoints.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(chartPoints.map { it.utilizationPercent })
                }
            }
        }
    }

    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            "${value.roundToInt()}%"
        }
    }
    val bottomAxisFormatter = remember(chartPoints) {
        CartesianValueFormatter { _, value, _ ->
            chartPoints.getOrNull(value.toInt())?.shortLabel ?: ""
        }
    }

    val markerLabel = rememberTextComponent(
        color = Color.White,
        background = rememberShapeComponent(
            fill = fill(BackOfficeNavy),
            shape = CorneredShape.rounded(allPercent = 25),
        ),
        padding = insets(horizontal = 10.dp, vertical = 6.dp),
    )
    val markerIndicator = rememberShapeComponent(
        fill = fill(accentColor),
        shape = CorneredShape.Pill,
    )
    val markerGuideline = rememberLineComponent(
        fill = fill(accentColor.copy(alpha = 0.45f)),
        thickness = 1.dp,
    )
    val markerFormatter = remember(chartPoints) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val idx = targets.firstOrNull()?.x?.roundToInt()
            val pt = if (idx != null) chartPoints.getOrNull(idx) else null
            if (pt != null) {
                "${pt.label}: ${formatQty(pt.utilizationPercent)}% (${formatQty(pt.activeOutUnits)}/${formatQty(pt.totalCapacityUnits)} out)"
            } else {
                ""
            }
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerFormatter,
        indicator = { _ -> markerIndicator },
        indicatorSize = 10.dp,
        guideline = markerGuideline,
    )

    GroupCard(modifier = modifier.testTag("${testTagPrefix}_card")) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equipment Utilization Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LoyTextPrimary,
                    )
                    Text(
                        text = if (liveOutMode) {
                            "Live fleet units currently rented out"
                        } else {
                            "30-day unit-day occupancy across fleet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = liveOutMode,
                        onClick = { onToggleLiveMode(true) },
                        label = { Text("Live Out", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentLight,
                            selectedLabelColor = accentColor,
                        ),
                        modifier = Modifier.height(28.dp).testTag("${testTagPrefix}_mode_live"),
                    )
                    FilterChip(
                        selected = !liveOutMode,
                        onClick = { onToggleLiveMode(false) },
                        label = { Text("30D", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentLight,
                            selectedLabelColor = accentColor,
                        ),
                        modifier = Modifier.height(28.dp).testTag("${testTagPrefix}_mode_30d"),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatBlock(
                    label = if (liveOutMode) "Live Fleet Rate" else "30D Utilization",
                    value = "${formatQty(overallPct)}%",
                    valueColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
                StatBlock(
                    label = "Rented / Fleet Units",
                    value = "${formatQty(totalOut)} / ${formatQty(totalCapacity)}",
                    valueColor = if (overallPct >= 80.0) LoyWarning else LoyTextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = byCategory,
                    onClick = { onToggleByCategory(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("${testTagPrefix}_scope_category"),
                ) {
                    Text("By Category")
                }
                SegmentedButton(
                    selected = !byCategory,
                    onClick = { onToggleByCategory(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("${testTagPrefix}_scope_gear"),
                ) {
                    Text("Top Gear")
                }
            }

            if (chartPoints.isNotEmpty()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = fill(accentColor),
                                    thickness = 18.dp,
                                    shape = CorneredShape.rounded(topLeftPercent = 30, topRightPercent = 30),
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(valueFormatter = startAxisFormatter),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomAxisFormatter,
                            itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }),
                        ),
                        marker = marker,
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .testTag("${testTagPrefix}_chart"),
                )
            }

            val breakdown = activePoints.take(4)
            if (breakdown.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    breakdown.forEach { row ->
                        val ratio = (row.utilizationPercent / 100.0).toFloat().coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = row.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${formatQty(row.utilizationPercent)}% · ${formatQty(row.activeOutUnits)}/${formatQty(row.totalCapacityUnits)} out",
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.labelSmall),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (row.utilizationPercent >= 80.0) LoyWarning else accentColor,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                color = if (row.utilizationPercent >= 80.0) LoyWarning else accentColor,
                                trackColor = LoySurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (topLeader == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentLight.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BuildCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Check out equipment on Rent Desk or QR Track to view live fleet utilization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }
            }
        }
    }
}
