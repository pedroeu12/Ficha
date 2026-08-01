package com.pedroeu.ficha.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * The two colour schemes, kept side by side so a role that gets a colour in one always gets
 * one in the other.
 *
 * Every screen in the app reads its colours from the scheme rather than naming them, which
 * is what lets a second scheme reach the whole app. The long tail of `surfaceContainer*`
 * roles matters more than it looks: Material pulls those for menus, dialogs, and text-field
 * containers, and a single unset one shows up as a stray grey panel floating over the page.
 */

/** Daylight: ink on aged paper. */
val ParchmentColors: ColorScheme = lightColorScheme(
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

/**
 * Candlelight: cream on dark leather.
 *
 * The roles are mapped the same way as the daylight scheme rather than following Material's
 * usual dark-mode convention, because the app leans on `primaryContainer` for its app bars
 * and `primary` for its buttons. Swapping those to pale tones the way a stock dark theme
 * does would turn every header into a bright slab, which is the opposite of what a sheet
 * read at a dim table wants.
 */
val CandlelightColors: ColorScheme = darkColorScheme(
    primary = CrimsonNight,
    onPrimary = VellumBright,
    primaryContainer = CrimsonNightDeep,
    onPrimaryContainer = VellumBright,
    inversePrimary = CrimsonNightSoft,

    secondary = GoldNight,
    onSecondary = NightPage,
    secondaryContainer = NightDeep,
    onSecondaryContainer = VellumBright,

    tertiary = GoldNightDeep,
    onTertiary = NightPage,
    tertiaryContainer = NightDeep,
    onTertiaryContainer = VellumBright,

    background = NightPage,
    onBackground = VellumBright,
    surface = NightCard,
    onSurface = VellumBright,
    surfaceVariant = NightDeep,
    onSurfaceVariant = VellumSoft,
    surfaceTint = GoldNight,

    surfaceContainerLowest = NightPage,
    surfaceContainerLow = NightPage,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCard,
    surfaceContainerHighest = NightDeep,
    surfaceBright = NightDeep,
    surfaceDim = NightPage,
    inverseSurface = VellumBright,
    inverseOnSurface = NightPage,

    outline = NightShade,
    outlineVariant = NightEdge,

    error = DangerRedNight,
    onError = NightPage,
    errorContainer = NightDeep,
    onErrorContainer = DangerRedNight,

    scrim = NightPage,
)

/** The colours the page backdrop washes through, darkest last. */
val ParchmentBackdropStops: List<androidx.compose.ui.graphics.Color> = listOf(
    ParchmentPage,
    ParchmentCard.copy(alpha = 0.65f),
    ParchmentDeep.copy(alpha = 0.55f),
)

/**
 * Candlelight runs the other way to daylight — brightest where the page meets the app bar,
 * falling off toward the bottom, as though lit from above. Every stop stays darker than
 * [NightCard] so a card always reads as raised off the page rather than sinking into it.
 */
val CandlelightBackdropStops: List<androidx.compose.ui.graphics.Color> = listOf(
    NightGlow,
    NightPage,
    NightFloor,
)
