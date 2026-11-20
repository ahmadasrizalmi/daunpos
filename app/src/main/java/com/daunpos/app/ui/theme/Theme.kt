package com.daunpos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

private val DaunShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(50),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

private val DaunColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenSoft,
    onPrimaryContainer = GreenDeep,
    secondary = GreenDeep,
    onSecondary = Color.White,
    secondaryContainer = Mint,
    onSecondaryContainer = Ink,
    tertiary = Amber,
    onTertiary = Color.White,
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = Surface,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Surface,
    surfaceVariant = GreenSoft,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD9DDE2),
    outlineVariant = Line,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger
)

@Composable
fun DaunPOSTheme(content: @Composable () -> Unit) {
    // DaunPOS selalu memakai tema terang sesuai desain (clean & cozy).
    MaterialTheme(
        colorScheme = DaunColorScheme,
        typography = DaunTypography,
        shapes = DaunShapes,
        content = content
    )
}
