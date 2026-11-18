package com.loypos.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LoyGreen = Color(0xFF4CAF50)
val LoyGreenDark = Color(0xFF388E3C)
val LoyGreenLight = Color(0xFFE8F5E9)
val LoyBackground = Color(0xFFF5F6F8)
val LoySurface = Color(0xFFFFFFFF)
val LoySurfaceVariant = Color(0xFFF0F2F5)
val LoyBorder = Color(0xFFE2E6EB)
val LoyTextPrimary = Color(0xFF1A1D21)
val LoyTextSecondary = Color(0xFF5F6672)
val LoyError = Color(0xFFD32F2F)
val LoyErrorLight = Color(0xFFFFEBEE)
val LoyWarning = Color(0xFFE65100)
val LoyWarningLight = Color(0xFFFFF3E0)
val LoyInfo = Color(0xFF1976D2)
val LoyInfoLight = Color(0xFFE3F2FD)

val TabularMoneyStyle = TextStyle(
    fontFeatureSettings = "tnum",
)

private val DesktopPalette = lightColorScheme(
    primary = LoyGreen,
    onPrimary = Color.White,
    primaryContainer = LoyGreenLight,
    onPrimaryContainer = LoyGreenDark,
    secondary = LoyInfo,
    onSecondary = Color.White,
    background = LoyBackground,
    onBackground = LoyTextPrimary,
    surface = LoySurface,
    onSurface = LoyTextPrimary,
    surfaceVariant = LoySurfaceVariant,
    onSurfaceVariant = LoyTextSecondary,
    error = LoyError,
    onError = Color.White,
    outline = LoyBorder,
)

private val DesktopShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun LoyDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopPalette,
        shapes = DesktopShapes,
        content = content,
    )
}

fun categoryTileAccent(catId: String?): Color = when (catId) {
    "cat-tents" -> Color(0xFF2E7D32)
    "cat-chairs" -> Color(0xFF1565C0)
    "cat-tables" -> Color(0xFF6A1B9A)
    "cat-lighting" -> Color(0xFFEF6C00)
    "cat-sound" -> Color(0xFFC62828)
    "cat-stage" -> Color(0xFF00838F)
    "cat-power" -> Color(0xFF4527A0)
    "cat-decor" -> Color(0xFFAD1457)
    else -> LoyGreen
}
