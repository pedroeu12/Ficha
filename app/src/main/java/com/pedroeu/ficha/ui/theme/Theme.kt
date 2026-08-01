package com.pedroeu.ficha.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The app's look, in daylight or candlelight.
 *
 * [mode] is the player's choice; whether that actually means dark is only known once the
 * phone's own setting is folded in, which is why the resolved answer travels down through
 * [LocalThemeController] rather than being recomputed by each screen that cares.
 */
@Composable
fun FichaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    onModeChange: (ThemeMode) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val colors = if (dark) CandlelightColors else ParchmentColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // The status bar sits under the app bar's crimson band and the navigation bar
            // under the page, so each takes the tone of what it abuts in the active scheme.
            window.statusBarColor = colors.primaryContainer.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Both crimsons are dark enough to want light icons either way; what changes
            // between the schemes is the page sitting behind the navigation bar.
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    val themeController = remember(mode, dark, onModeChange) {
        ThemeController(mode = mode, isDark = dark, setMode = onModeChange)
    }

    CompositionLocalProvider(LocalThemeController provides themeController) {
        MaterialTheme(
            colorScheme = colors,
            typography = FichaTypography,
        ) {
            PageBackdrop(dark = dark) { content() }
        }
    }
}

/**
 * The base page tone plus a soft wash, so the paper reads as aged rather than flat. Screens
 * layered on top keep transparent containers to let this show through.
 *
 * Daylight washes from light at the top down to a deeper tone. Candlelight runs the other
 * way — brightest where the page meets the app bar, falling off toward the bottom, as though
 * lit from above.
 */
@Composable
private fun PageBackdrop(dark: Boolean, content: @Composable () -> Unit) {
    val stops = if (dark) CandlelightBackdropStops else ParchmentBackdropStops
    val bandColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(stops))
    ) {
        // A crimson band behind the status bar so the app bar reads as one piece with it.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(bandColor)
                .align(Alignment.TopCenter)
        )
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            content()
        }
    }
}
