package com.loypos.desktop.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Refined Desktop Pro Palette (Slate + Deep Emerald Workstation)
val LoyGreen = Color(0xFF0F766E)
val LoyGreenDark = Color(0xFF115E59)
val LoyGreenLight = Color(0xFFE6F6F4)
val LoyBackground = Color(0xFFF6F8FA)
val LoySurface = Color(0xFFFFFFFF)
val LoySurfaceVariant = Color(0xFFF1F5F9)
val LoySurfaceHover = Color(0xFFF8FAFC)
val LoyBorder = Color(0xFFE2E8F0)
val LoyBorderStrong = Color(0xFFCBD5E1)
val LoyTextPrimary = Color(0xFF0F172A)
val LoyTextSecondary = Color(0xFF475569)
val LoyTextMuted = Color(0xFF64748B)
val LoyError = Color(0xFFDC2626)
val LoyErrorLight = Color(0xFFFEF2F2)
val LoyWarning = Color(0xFFD97706)
val LoyWarningLight = Color(0xFFFFFBEB)
val LoyInfo = Color(0xFF2563EB)
val LoyInfoLight = Color(0xFFEFF6FF)

// Desktop Sidebar Slate Chrome Tokens
val LoySidebarBg = Color(0xFF0F172A)
val LoySidebarSurface = Color(0xFF1E293B)
val LoySidebarHover = Color(0xFF162032)
val LoySidebarBorder = Color(0xFF1E293B)
val LoySidebarText = Color(0xFFF1F5F9)
val LoySidebarMuted = Color(0xFF94A3B8)
val LoySidebarAccent = Color(0xFF10B981)

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

private val DesktopTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = LoyTextPrimary,
    ),
    titleLarge = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        color = LoyTextPrimary,
    ),
    titleMedium = TextStyle(
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        color = LoyTextPrimary,
    ),
    titleSmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoyTextPrimary,
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = LoyTextPrimary,
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        color = LoyTextPrimary,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        color = LoyTextSecondary,
    ),
    labelLarge = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoyTextPrimary,
    ),
    labelMedium = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        color = LoyTextSecondary,
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
        color = LoyTextMuted,
    ),
)

private val DesktopShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
)

@Composable
fun LoyDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesktopPalette,
        typography = DesktopTypography,
        shapes = DesktopShapes,
        content = content,
    )
}

fun categoryTileAccent(catId: String?): Color = when (catId) {
    "cat-tents" -> Color(0xFF0F766E)
    "cat-chairs" -> Color(0xFF2563EB)
    "cat-tables" -> Color(0xFF7C3AED)
    "cat-lighting" -> Color(0xFFD97706)
    "cat-sound" -> Color(0xFFDC2626)
    "cat-stage" -> Color(0xFF0891B2)
    "cat-power" -> Color(0xFF4F46E5)
    "cat-decor" -> Color(0xFFDB2777)
    else -> LoyGreen
}
