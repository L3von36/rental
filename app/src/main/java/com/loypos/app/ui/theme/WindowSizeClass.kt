package com.loypos.app.ui.theme

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LoyWidthClass { Compact, Medium, Expanded }
enum class LoyHeightClass { Compact, Medium, Expanded }

@Immutable
data class LoyWindowSize(
    val widthClass: LoyWidthClass,
    val heightClass: LoyHeightClass,
    val widthDp: Dp,
    val heightDp: Dp,
) {
    val isCompact: Boolean get() = widthClass == LoyWidthClass.Compact
    val isMedium: Boolean get() = widthClass == LoyWidthClass.Medium
    val isExpanded: Boolean get() = widthClass == LoyWidthClass.Expanded
    val isDesktop: Boolean get() = widthDp >= 1100.dp
    val useNavigationRail: Boolean get() = widthClass != LoyWidthClass.Compact
    val useSplitPane: Boolean get() = widthClass == LoyWidthClass.Expanded
    val gridColumns: Int
        get() = when {
            widthClass == LoyWidthClass.Compact -> 1
            widthClass == LoyWidthClass.Medium -> 2
            widthDp >= 1400.dp -> 4
            widthDp >= 1100.dp -> 3
            else -> 2
        }
}

@Immutable
data class LoyThemeMetrics(
    val uiScale: Float,
    val screenPadding: Dp,
    val cardPadding: Dp,
    val rowHorizontalPadding: Dp,
    val rowVerticalPadding: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val topBarHeight: Dp,
    val buttonHeight: Dp,
    val chipHeight: Dp,
    val minTouchTarget: Dp,
)

fun calculateLoyWindowSize(widthDp: Dp, heightDp: Dp): LoyWindowSize {
    val wClass = when {
        widthDp < 600.dp -> LoyWidthClass.Compact
        widthDp < 840.dp -> LoyWidthClass.Medium
        else -> LoyWidthClass.Expanded
    }
    val hClass = when {
        heightDp < 480.dp -> LoyHeightClass.Compact
        heightDp < 900.dp -> LoyHeightClass.Medium
        else -> LoyHeightClass.Expanded
    }
    return LoyWindowSize(
        widthClass = wClass,
        heightClass = hClass,
        widthDp = widthDp,
        heightDp = heightDp,
    )
}

fun loyMetricsFor(windowSize: LoyWindowSize): LoyThemeMetrics {
    val scale = when (windowSize.widthClass) {
        LoyWidthClass.Compact -> 0.90f
        LoyWidthClass.Medium -> 0.96f
        LoyWidthClass.Expanded -> 1.0f
    }
    // Keep physical touch targets >= 48.dp even when LocalDensity scales down on mobile screens.
    val touchTarget = (48f / scale).dp
    return when (windowSize.widthClass) {
        LoyWidthClass.Compact -> LoyThemeMetrics(
            uiScale = scale,
            screenPadding = 10.dp,
            cardPadding = 10.dp,
            rowHorizontalPadding = 10.dp,
            rowVerticalPadding = 7.dp,
            sectionGap = 7.dp,
            itemGap = 6.dp,
            topBarHeight = 48.dp,
            buttonHeight = 44.dp,
            chipHeight = 30.dp,
            minTouchTarget = touchTarget,
        )
        LoyWidthClass.Medium -> LoyThemeMetrics(
            uiScale = scale,
            screenPadding = 12.dp,
            cardPadding = 11.dp,
            rowHorizontalPadding = 12.dp,
            rowVerticalPadding = 8.dp,
            sectionGap = 8.dp,
            itemGap = 7.dp,
            topBarHeight = 50.dp,
            buttonHeight = 44.dp,
            chipHeight = 32.dp,
            minTouchTarget = touchTarget,
        )
        LoyWidthClass.Expanded -> LoyThemeMetrics(
            uiScale = scale,
            screenPadding = 16.dp,
            cardPadding = 14.dp,
            rowHorizontalPadding = 14.dp,
            rowVerticalPadding = 10.dp,
            sectionGap = 10.dp,
            itemGap = 8.dp,
            topBarHeight = 54.dp,
            buttonHeight = 46.dp,
            chipHeight = 32.dp,
            minTouchTarget = touchTarget,
        )
    }
}

val LocalLoyWindowSize = staticCompositionLocalOf {
    LoyWindowSize(
        widthClass = LoyWidthClass.Compact,
        heightClass = LoyHeightClass.Medium,
        widthDp = 390.dp,
        heightDp = 844.dp,
    )
}

val LocalLoyMetrics = staticCompositionLocalOf {
    loyMetricsFor(
        LoyWindowSize(
            widthClass = LoyWidthClass.Compact,
            heightClass = LoyHeightClass.Medium,
            widthDp = 390.dp,
            heightDp = 844.dp,
        )
    )
}

fun Modifier.accessibleTouchTarget(minSize: Dp = 48.dp): Modifier =
    sizeIn(minWidth = minSize, minHeight = minSize)
