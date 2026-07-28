package com.pedroeu.ficha.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aged-parchment palette. The app deliberately ships a single warm scheme rather than a
 * light/dark pair, so a phone in dark mode never falls back to grey or white surfaces.
 */

// Paper tones, lightest to deepest.
val ParchmentPage = Color(0xFFF4E8D0)      // page background
val ParchmentCard = Color(0xFFEDE0C8)      // raised cards and sheets
val ParchmentDeep = Color(0xFFE8D9B5)      // wells, chips, inset surfaces
val ParchmentEdge = Color(0xFFD9C6A0)      // borders and hairlines
val ParchmentShade = Color(0xFFCBB489)     // vignette edge

// Ink tones for text on parchment.
val InkBrown = Color(0xFF2E2013)           // primary text
val InkBrownSoft = Color(0xFF5B4632)       // secondary text
val InkBrownFaint = Color(0xFF8A7355)      // tertiary / disabled text

// Accents.
val CrimsonPrimary = Color(0xFF8C2F2F)     // headers, app bars
val CrimsonDeep = Color(0xFF6B2020)        // status bar, pressed states
val CrimsonSoft = Color(0xFFB05A4A)
val GoldAccent = Color(0xFF9A6B1F)         // section labels, proficiency pips
val GoldAccentDeep = Color(0xFF7A5416)

val SuccessGreen = Color(0xFF3F6B41)
val DangerRed = Color(0xFF9B2C22)
