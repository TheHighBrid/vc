package com.kenza.callsim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iOS system palette used across the call UI.
object IOSColors {
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val ControlGrey = Color(0xFF3A3A3C)          // idle round button
    val ControlGreyActive = Color(0xFFE9E9EA)    // toggled-on button (white-ish)
    val Label = Color(0xFFFFFFFF)
    val SecondaryLabel = Color(0xCCFFFFFF)
    val KeypadDigit = Color(0xFFFFFFFF)
    val CallScreenTop = Color(0xFF1C1C1E)
    val CallScreenBottom = Color(0xFF000000)
    val Blue = Color(0xFF0A84FF)
}

private val DarkColors = darkColorScheme(
    primary = IOSColors.Green,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun KenzaCallTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The iOS call screen is always dark.
    MaterialTheme(colorScheme = DarkColors, content = content)
}
