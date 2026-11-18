package com.daunpos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    surfaceVariant = GreenSoft,
    onSurfaceVariant = InkSoft,
    outline = Line,
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
        content = content
    )
}
