package com.pedroeu.ficha.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aged-parchment palette, in a daylight and a candlelight version.
 *
 * The dark scheme is not the light one inverted, and it is deliberately not Material's greys:
 * a sheet that turned slate-grey at night would throw away the look the whole app is built
 * around. It reads instead as old leather and vellum seen by candlelight — the same warm
 * hues, with the paper pulled down to near-black and the ink lifted to a pale cream.
 */

// ------------------------------------------------------------------ Daylight

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

// ------------------------------------------------------------------ Candlelight

// Leather tones, deepest to lightest. Warm rather than neutral: every one of these carries
// more red than blue, which is what keeps the dark sheet reading as parchment at night.
val NightPage = Color(0xFF17110C)          // page background
val NightCard = Color(0xFF241C14)          // raised cards and sheets
val NightDeep = Color(0xFF302619)          // wells, chips, inset surfaces
val NightGlow = Color(0xFF1C150E)          // page where the light falls, just under a card
val NightFloor = Color(0xFF120D08)         // page at the bottom, away from the light
val NightEdge = Color(0xFF3D3123)          // borders and hairlines
val NightShade = Color(0xFF57452F)         // borders that need to read as edges

// Ink inverts: pale cream on dark leather, in the same three weights as the daylight scheme.
val VellumBright = Color(0xFFF0E4CE)       // primary text
val VellumSoft = Color(0xFFC7B593)         // secondary text
val VellumFaint = Color(0xFF9C8A6B)        // tertiary / disabled text

// Accents, lifted so they carry against a dark surface. The daylight crimson and gold are
// too deep to read as text at night, so each has a brighter counterpart here.
val CrimsonNight = Color(0xFFA83A34)       // buttons and highlights
val CrimsonNightDeep = Color(0xFF7A2622)   // app bars and the status bar
val CrimsonNightSoft = Color(0xFFC97A6E)
val GoldNight = Color(0xFFD9A94E)          // section labels, proficiency pips
val GoldNightDeep = Color(0xFFB98A34)

val SuccessGreenNight = Color(0xFF6FA872)
val DangerRedNight = Color(0xFFE06A5C)
