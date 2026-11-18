package com.loypos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Festival
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.loypos.app.core.formatQty
import com.loypos.app.core.parseQty
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.loypos.app.data.local.ItemEntity
import com.loypos.app.ui.theme.LoyBorder
import com.loypos.app.ui.theme.LoyError
import com.loypos.app.ui.theme.LoyGreen
import com.loypos.app.ui.theme.LoyGreenLight
import com.loypos.app.ui.theme.LoySurface
import com.loypos.app.ui.theme.LoySurfaceVariant
import com.loypos.app.ui.theme.LoyTextPrimary
import com.loypos.app.ui.theme.LoyTextSecondary
import com.loypos.app.ui.theme.LoyWarning
import com.loypos.app.ui.theme.TabularMoneyStyle
import com.loypos.app.ui.theme.LocalLoyMetrics
import com.loypos.app.ui.theme.accessibleTouchTarget
import com.loypos.app.ui.theme.tileColorFor
import java.io.File

data class CompactNavEntry(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val tag: String,
    val onClick: () -> Unit,
)

@Composable
fun CompactBottomNavBar(
    entries: List<CompactNavEntry>,
    selectedColor: Color,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LoySurface,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = LoyBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = metrics.minTouchTarget.coerceAtLeast(52.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                entries.forEach { entry ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = metrics.minTouchTarget)
                            .clickable(onClick = entry.onClick)
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                            .testTag(entry.tag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (entry.selected) indicatorColor else Color.Transparent)
                                .padding(horizontal = 11.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = entry.label,
                                tint = if (entry.selected) selectedColor else LoyTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (entry.selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (entry.selected) selectedColor else LoyTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    inputTestTag: String? = null,
    clearTestTag: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LoySurfaceVariant,
        border = BorderStroke(1.dp, LoyBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = LoyTextSecondary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LoyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LoyTextPrimary),
                    cursorBrush = SolidColor(LoyGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier),
                )
            }
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") }
                        .then(if (clearTestTag != null) Modifier.testTag(clearTestTag) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = LoyTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

fun categoryIconFor(categoryId: String?): ImageVector = when (categoryId) {
    "cat-tents" -> Icons.Outlined.Festival
    "cat-chairs" -> Icons.Outlined.EventSeat
    "cat-tables" -> Icons.Outlined.TableRestaurant
    "cat-lighting" -> Icons.Outlined.Lightbulb
    "cat-sound" -> Icons.Outlined.Speaker
    "cat-stage" -> Icons.Outlined.Layers
    "cat-power" -> Icons.Outlined.Bolt
    "cat-decor" -> Icons.Outlined.Celebration
    else -> Icons.Outlined.Inventory2
}

@Composable
fun ItemThumbnail(
    item: ItemEntity,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val tint = tileColorFor(item.categoryId ?: item.name)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        if (item.hasPhoto && File(item.imagePath).exists()) {
            AsyncImage(
                model = File(item.imagePath),
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = categoryIconFor(item.categoryId),
                contentDescription = item.name,
                tint = tint,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
fun StockDotBadge(
    item: ItemEntity,
    modifier: Modifier = Modifier,
    threshold: Double = 3.0,
) {
    if (!item.trackStock) return
    val (dotColor, label) = when {
        item.stock <= 0.0 -> LoyError to "Rented out"
        item.stock < threshold -> LoyWarning to "Low · ${if (item.stock == item.stock.toLong().toDouble()) item.stock.toLong() else item.stock} avail"
        else -> LoyGreen to "${if (item.stock == item.stock.toLong().toDouble()) item.stock.toLong() else item.stock} avail"
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.stock <= 0.0) LoyError else LoyTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    backgroundColor: Color = LoyGreenLight,
    textColor: Color = LoyGreen,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LoySurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, LoyBorder),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LoyTextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    valueColor: Color = LoyTextPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(LoySurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LoyTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
            color = valueColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(LoyGreenLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LoyGreen,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = LoyTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LoyTextSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            val metrics = LocalLoyMetrics.current
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = metrics.minTouchTarget),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun CurrencyNumPad(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    allowDecimal: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (allowDecimal) "." else "C", "0", "BACK"),
    )
    val metrics = LocalLoyMetrics.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val tag = when (key) {
                        "BACK" -> "numpad_backspace"
                        "C" -> "numpad_clear"
                        "." -> "numpad_dot"
                        else -> "numpad_$key"
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = metrics.minTouchTarget)
                            .testTag(tag)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                when (key) {
                                    "BACK" -> onBackspace()
                                    "C" -> onClear()
                                    else -> onKey(key)
                                }
                            },
                        color = LoySurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LoyBorder),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == "BACK") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = "Backspace",
                                    tint = LoyTextPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = TabularMoneyStyle.merge(MaterialTheme.typography.titleMedium),
                                    color = LoyTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
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
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val metrics = LocalLoyMetrics.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) LoyError else LoyGreen,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("confirm_dialog_ok"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("confirm_dialog_cancel"),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun TopSyncIconButton(
    syncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalLoyMetrics.current
    IconButton(
        onClick = onClick,
        enabled = !syncing,
        modifier = modifier
            .accessibleTouchTarget(metrics.minTouchTarget)
            .testTag("sync_button"),
    ) {
        if (syncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LoyGreen,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.CloudSync,
                contentDescription = "Sync with Plesk MySQL",
                tint = LoyTextPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
fun InventoryStockAlertSettingsDialog(
    initialEnabled: Boolean,
    initialThreshold: Double,
    initialIntervalMinutes: Long,
    onSave: (enabled: Boolean, threshold: Double, intervalMinutes: Long) -> Unit,
    onTestCheckNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var thresholdText by remember { mutableStateOf(formatQty(initialThreshold)) }
    var selectedInterval by remember { mutableLongStateOf(initialIntervalMinutes.coerceAtLeast(15L)) }
    val parsedThreshold = parseQty(thresholdText)
    val metrics = LocalLoyMetrics.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = LoyGreen,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    "Stock Alerts & Monitor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "WorkManager automatically checks inventory in the background and sends an Android system notification when products drop below your threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoyTextSecondary,
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (enabled) LoyGreenLight.copy(alpha = 0.5f) else LoySurfaceVariant,
                    border = BorderStroke(1.dp, if (enabled) LoyGreen else LoyBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (enabled) "Periodic scan active" else "Alerts turned off",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (enabled) LoyGreen else LoyTextSecondary,
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            modifier = Modifier.testTag("stock_alerts_dialog_switch"),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Low Stock Threshold (units)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { thresholdText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("stock_threshold_input"),
                        label = { Text("Minimum quantity") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0, 3.0, 5.0, 10.0).forEach { qty ->
                            FilterChip(
                                selected = parsedThreshold == qty,
                                onClick = { thresholdText = formatQty(qty) },
                                label = { Text(formatQty(qty)) },
                                modifier = Modifier.height(30.dp),
                            )
                        }
                    }
                }

                if (enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Check Frequency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15L to "15m", 30L to "30m", 60L to "1h", 240L to "4h").forEach { (mins, label) ->
                                FilterChip(
                                    selected = selectedInterval == mins,
                                    onClick = { selectedInterval = mins },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(30.dp),
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onTestCheckNow,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).testTag("stock_alerts_test_now"),
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run Stock Check Now", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = parsedThreshold ?: 3.0
                    onSave(enabled, threshold, selectedInterval)
                },
                enabled = parsedThreshold != null,
                colors = ButtonDefaults.buttonColors(containerColor = LoyGreen),
                modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("stock_alerts_dialog_save"),
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = metrics.minTouchTarget),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun InventoryStockMonitorBanner(
    alertsEnabled: Boolean,
    threshold: Double,
    lowCount: Int,
    outOfStockCount: Int,
    workerStatus: String,
    onConfigureClick: () -> Unit,
    onCheckNowClick: () -> Unit,
    onFilterLowStockClick: () -> Unit,
    isFilterActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasAlert = lowCount > 0 || outOfStockCount > 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            outOfStockCount > 0 -> Color(0xFFFFF3F0)
            hasAlert -> Color(0xFFFFF8E1)
            else -> LoySurface
        },
        border = BorderStroke(
            1.dp,
            when {
                outOfStockCount > 0 -> LoyError.copy(alpha = 0.5f)
                hasAlert -> LoyWarning.copy(alpha = 0.6f)
                else -> LoyBorder
            }
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                outOfStockCount > 0 -> LoyError.copy(alpha = 0.15f)
                                hasAlert -> LoyWarning.copy(alpha = 0.2f)
                                else -> LoyGreenLight
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            outOfStockCount > 0 -> Icons.Outlined.WarningAmber
                            hasAlert -> Icons.Outlined.NotificationsActive
                            else -> Icons.Outlined.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            outOfStockCount > 0 -> LoyError
                            hasAlert -> LoyWarning
                            else -> LoyGreen
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            outOfStockCount > 0 && lowCount > 0 -> "$outOfStockCount all out · $lowCount low availability"
                            outOfStockCount > 0 -> "$outOfStockCount tools fully rented out"
                            hasAlert -> "$lowCount ${if (lowCount == 1) "tool" else "tools"} below ${formatQty(threshold)} available"
                            else -> "All tracked equipment healthy (≥ ${formatQty(threshold)})"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            outOfStockCount > 0 -> LoyError
                            hasAlert -> Color(0xFFB78103)
                            else -> LoyGreen
                        },
                    )
                    Text(
                        text = "Background check: $workerStatus · ${if (alertsEnabled) "Active" else "Disabled"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LoyTextSecondary,
                    )
                }

                IconButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.size(36.dp).testTag("inventory_stock_monitor_tune"),
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Configure Stock Alerts",
                        tint = LoyTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasAlert) {
                    FilterChip(
                        selected = isFilterActive,
                        onClick = onFilterLowStockClick,
                        label = {
                            Text(
                                if (isFilterActive) "Showing Low Stock" else "Filter Low (${lowCount + outOfStockCount})",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (outOfStockCount > 0) Color(0xFFFFEBEE) else Color(0xFFFFF8E1),
                            selectedLabelColor = if (outOfStockCount > 0) LoyError else Color(0xFFB78103),
                        ),
                        modifier = Modifier.height(28.dp).testTag("inventory_show_low_chip"),
                    )
                }

                OutlinedButton(
                    onClick = onCheckNowClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("inventory_run_check_btn"),
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Check Stock Now", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = onConfigureClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("< ${formatQty(threshold)} threshold", style = MaterialTheme.typography.labelSmall, color = LoyGreen)
                }
            }
        }
    }
}

