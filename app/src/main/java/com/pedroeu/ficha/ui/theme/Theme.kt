package com.pedroeu.ficha.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * One warm scheme for every screen. There is no dark variant on purpose: a parchment sheet
 * that flipped to grey in dark mode would defeat the look the app is going for.
 */
private val ParchmentColors = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = ParchmentPage,
    primaryContainer = CrimsonPrimary,
    onPrimaryContainer = ParchmentPage,
    inversePrimary = CrimsonSoft,

    secondary = GoldAccentDeep,
    onSecondary = ParchmentPage,
    secondaryContainer = ParchmentDeep,
    onSecondaryContainer = InkBrown,

    tertiary = CrimsonDeep,
    onTertiary = ParchmentPage,
    tertiaryContainer = ParchmentDeep,
    onTertiaryContainer = InkBrown,

    background = ParchmentPage,
    onBackground = InkBrown,
    surface = ParchmentCard,
    onSurface = InkBrown,
    surfaceVariant = ParchmentDeep,
    onSurfaceVariant = InkBrownSoft,
    surfaceTint = GoldAccent,

    // Material pulls these for menus, dialogs, and text-field containers; all must stay warm
    // or a stray white panel shows up on top of the parchment.
    surfaceContainerLowest = ParchmentPage,
    surfaceContainerLow = ParchmentPage,
    surfaceContainer = ParchmentCard,
    surfaceContainerHigh = ParchmentCard,
    surfaceContainerHighest = ParchmentDeep,
    surfaceBright = ParchmentPage,
    surfaceDim = ParchmentDeep,
    inverseSurface = InkBrown,
    inverseOnSurface = ParchmentPage,

    outline = ParchmentEdge,
    outlineVariant = ParchmentEdge,

    error = DangerRed,
    onError = ParchmentPage,
    errorContainer = ParchmentDeep,
    onErrorContainer = DangerRed,

    scrim = InkBrown,
)

@Composable
fun FichaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CrimsonDeep.toArgb()
            window.navigationBarColor = ParchmentPage.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Crimson status bar wants light icons; the parchment nav bar wants dark ones.
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = ParchmentColors,
        typography = FichaTypography,
    ) {
        ParchmentBackdrop(content)
    }
}

/**
 * The base page tone plus a soft top-to-bottom wash, so the paper reads as aged rather than
 * flat. Screens layered on top keep transparent containers to let this show through.
 */
@Composable
private fun ParchmentBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ParchmentPage,
                        ParchmentCard.copy(alpha = 0.65f),
                        ParchmentDeep.copy(alpha = 0.55f),
                    )
                )
            )
    ) {
        // A crimson band behind the status bar so the app bar reads as one piece with it.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(CrimsonDeep)
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
