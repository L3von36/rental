package com.loypos.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

val LoyGreen = Color(0xFF4CAF50)
val LoyGreenPressed = Color(0xFF43A047)
val LoyGreenLight = Color(0xFFE8F5E9)
val LoyGreenDarkText = Color(0xFF1B5E20)
val LoyGreenAccent = Color(0xFF7CB342)
val LoyError = Color(0xFFE53935)
val LoyErrorLight = Color(0xFFFFEBEE)
val LoyWarning = Color(0xFFFF9800)
val LoyWarningLight = Color(0xFFFFF3E0)
val LoyInfo = Color(0xFF1976D2)
val LoyInfoLight = Color(0xFFE3F2FD)

val BackOfficeNavy = Color(0xFF162436)
val BackOfficeSlate = Color(0xFF243B55)
val BackOfficeTeal = Color(0xFF0D9488)
val BackOfficeTealLight = Color(0xFFE6FFFA)

val LoyBackground = Color(0xFFF6F7F8)
val LoySurface = Color(0xFFFFFFFF)
val LoySurfaceVariant = Color(0xFFF1F3F5)
val LoyTileBg = Color(0xFFF1F1F1)
val LoyBorder = Color(0xFFE5E7EB)
val LoyTextPrimary = Color(0xFF1C1B1F)
val LoyTextSecondary = Color(0xFF6B7280)

val TilePalette = listOf(
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF009688),
    Color(0xFFE91E63),
    Color(0xFF3F51B5),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFFFF5722),
)

fun tileColorFor(key: String): Color {
    if (key.isEmpty()) return TilePalette.first()
    val idx = abs(key.hashCode()) % TilePalette.size
    return TilePalette[idx]
}
