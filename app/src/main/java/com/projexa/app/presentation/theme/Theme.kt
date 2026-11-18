package com.projexa.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.projexa.app.domain.models.ProximityLevel

object ProjexaColors {
    /** Backed by Compose state so changing it recomposes every screen that reads a color. */
    var highContrast by mutableStateOf(false)

    val Background: Color get() = if (highContrast) Color.Black else Color(0xFF090D14)
    val Surface: Color get() = if (highContrast) Color.Black else Color(0xFF111925)
    val Card: Color get() = if (highContrast) Color(0xFF14181F) else Color(0xFF172333)
    val Blue: Color get() = if (highContrast) Color(0xFF6FB0FF) else Color(0xFF398BFF)
    val Cyan: Color get() = if (highContrast) Color(0xFF7FE8FF) else Color(0xFF39C6E8)
    val TextPrimary: Color get() = if (highContrast) Color.White else Color(0xFFF4F7FC)
    val TextSecondary: Color get() = if (highContrast) Color(0xFFE0E6EE) else Color(0xFF9BA9BA)
    val Outline: Color get() = if (highContrast) Color(0xFF8899AA) else Color(0xFF22324A)
    val Track: Color get() = if (highContrast) Color(0xFF2A3038) else Color(0xFF1C2A3D)
    val Success = Color(0xFF32C98A)
    val Warning = Color(0xFFF5B942)
    val Orange = Color(0xFFF58A42)
    val Danger = Color(0xFFF05B64)
    val Critical = Color(0xFFFF3D6E)

    fun forLevel(level: ProximityLevel): Color = when (level) {
        ProximityLevel.CLEAR -> Success
        ProximityLevel.CAUTION -> Warning
        ProximityLevel.WARNING -> Orange
        ProximityLevel.DANGER -> Danger
        ProximityLevel.CRITICAL -> Critical
        ProximityLevel.NO_READING -> TextSecondary
    }
}

private fun scheme() = darkColorScheme(
    primary = ProjexaColors.Blue,
    onPrimary = if (ProjexaColors.highContrast) Color.Black else Color.White,
    secondary = ProjexaColors.Cyan,
    onSecondary = Color(0xFF00222B),
    background = ProjexaColors.Background,
    onBackground = ProjexaColors.TextPrimary,
    surface = ProjexaColors.Surface,
    onSurface = ProjexaColors.TextPrimary,
    surfaceVariant = ProjexaColors.Card,
    onSurfaceVariant = ProjexaColors.TextSecondary,
    error = ProjexaColors.Danger,
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, lineHeight = 48.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

/**
 * @param fontScale extra multiplier on top of the phone's own font size setting.
 */
@Composable
fun ProjexaTheme(highContrast: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
    ProjexaColors.highContrast = highContrast
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme(), typography = typography, content = content)
    }
}
