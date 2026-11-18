package com.loypos.app.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val LoyLightColorScheme = lightColorScheme(
    primary = LoyGreen,
    onPrimary = Color.White,
    primaryContainer = LoyGreenLight,
    onPrimaryContainer = LoyGreenDarkText,
    secondary = LoyGreenAccent,
    onSecondary = Color.White,
    secondaryContainer = LoyGreenLight,
    onSecondaryContainer = LoyGreenDarkText,
    background = LoyBackground,
    onBackground = LoyTextPrimary,
    surface = LoySurface,
    onSurface = LoyTextPrimary,
    surfaceVariant = LoySurfaceVariant,
    onSurfaceVariant = LoyTextSecondary,
    outline = LoyBorder,
    outlineVariant = Color(0xFFEEEEEE),
    error = LoyError,
    onError = Color.White,
    errorContainer = LoyErrorLight,
    onErrorContainer = Color(0xFFB71C1C),
)

private val LoyShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(13.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun LoyPosTheme(content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSize = remember(maxWidth, maxHeight) {
            calculateLoyWindowSize(maxWidth, maxHeight)
        }
        val metrics = remember(windowSize) {
            loyMetricsFor(windowSize)
        }
        val scaledDensity = remember(baseDensity, metrics.uiScale) {
            Density(
                density = baseDensity.density * metrics.uiScale,
                fontScale = baseDensity.fontScale,
            )
        }
        CompositionLocalProvider(
            LocalLoyWindowSize provides windowSize,
            LocalLoyMetrics provides metrics,
            LocalDensity provides scaledDensity,
            LocalMinimumInteractiveComponentSize provides metrics.minTouchTarget,
        ) {
            MaterialTheme(
                colorScheme = LoyLightColorScheme,
                typography = LoyPosTypography,
                shapes = LoyShapes,
                content = content,
            )
        }
    }
}
