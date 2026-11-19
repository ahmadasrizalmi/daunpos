package com.daunpos.app.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Extension property to determine whether the device should render in Tablet / Wide POS layout.
 *
 * Evaluates to true if:
 * 1. smallestScreenWidthDp >= 600 (Standard 7"+ tablets in both portrait and landscape)
 * 2. OR screenWidthDp >= 600 and screenHeightDp >= 420 (Landscape tablets, LDPlayer 1280x720/960x540,
 *    cash register desktop/POS hardware terminals, and unfolded foldables).
 */
val Configuration.isTablet: Boolean
    get() = smallestScreenWidthDp >= 600 || (screenWidthDp >= 600 && screenHeightDp >= 420)

@Composable
fun isTabletDevice(): Boolean {
    return LocalConfiguration.current.isTablet
}
