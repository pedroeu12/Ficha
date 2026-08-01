package com.pedroeu.ficha

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.pedroeu.ficha.ui.theme.CandlelightColors
import com.pedroeu.ficha.ui.theme.ParchmentColors
import com.pedroeu.ficha.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The two colour schemes, and how the app decides between them.
 *
 * Contrast is checked rather than eyeballed because a palette is exactly the kind of thing
 * that gets nudged later for looks: a scheme that drifts into unreadable text fails here
 * instead of on someone's phone at a dark table.
 */
class ThemeTest {

    // ------------------------------------------------------------------ Choosing a scheme

    @Test
    fun `follow system defers to the phone, and the other two do not`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))

        assertTrue("dark means dark whatever the phone says", ThemeMode.DARK.isDark(false))
        assertFalse("and light means light", ThemeMode.LIGHT.isDark(true))
    }

    @Test
    fun `the modes cycle back around`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }

    @Test
    fun `an unrecognised stored value falls back to following the phone`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("SEPIA"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
    }

    @Test
    fun `every mode is labelled for the menu`() {
        ThemeMode.entries.forEach { mode ->
            assertTrue("${mode.name} needs a label", mode.label.isNotBlank())
        }
    }

    // ------------------------------------------------------------------ Readability

    /** Relative luminance, per WCAG 2.1. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** Every pair where the scheme puts text on a surface. */
    private fun textPairs(scheme: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("onPrimary on primary", scheme.onPrimary, scheme.primary),
        Triple("onPrimaryContainer on primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer),
        Triple("onSecondary on secondary", scheme.onSecondary, scheme.secondary),
        Triple("onSecondaryContainer on secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer),
        Triple("onTertiary on tertiary", scheme.onTertiary, scheme.tertiary),
        Triple("onTertiaryContainer on tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer),
        Triple("onBackground on background", scheme.onBackground, scheme.background),
        Triple("onSurface on surface", scheme.onSurface, scheme.surface),
        Triple("onSurfaceVariant on surfaceVariant", scheme.onSurfaceVariant, scheme.surfaceVariant),
        // The sheet prints secondary text straight onto the card, not only onto its own
        // container, so that combination has to hold up too.
        Triple("onSurfaceVariant on surface", scheme.onSurfaceVariant, scheme.surface),
        Triple("secondary on surface", scheme.secondary, scheme.surface),
        Triple("error on surface", scheme.error, scheme.surface),
        Triple("onError on error", scheme.onError, scheme.error),
    )

    @Test
    fun `every text pair in both schemes is readable`() {
        listOf("daylight" to ParchmentColors, "candlelight" to CandlelightColors)
            .forEach { (name, scheme) ->
                textPairs(scheme).forEach { (label, foreground, background) ->
                    val ratio = contrast(foreground, background)
                    assertTrue(
                        "$name: $label is %.2f:1, below the 4.5:1 needed to read".format(ratio),
                        ratio >= 4.5,
                    )
                }
            }
    }

    @Test
    fun `the dark scheme is actually dark and the light one light`() {
        assertTrue(
            "candlelight's page should be near black",
            luminance(CandlelightColors.background) < 0.05,
        )
        assertTrue(
            "daylight's page should be near white",
            luminance(ParchmentColors.background) > 0.6,
        )
        // Cards must sit apart from the page, or every panel edge disappears.
        assertTrue(
            "candlelight cards must be distinguishable from the page",
            contrast(CandlelightColors.surface, CandlelightColors.background) > 1.05,
        )
    }

    @Test
    fun `neither scheme falls back to grey`() {
        // The whole point of a second scheme rather than Material's default is that it stays
        // warm. A neutral surface would mean the parchment look was quietly lost.
        listOf(
            "candlelight background" to CandlelightColors.background,
            "candlelight surface" to CandlelightColors.surface,
            "candlelight surfaceVariant" to CandlelightColors.surfaceVariant,
            "daylight background" to ParchmentColors.background,
            "daylight surface" to ParchmentColors.surface,
        ).forEach { (name, color) ->
            assertTrue(
                "$name is neutral grey, not parchment",
                color.red > color.blue,
            )
        }
    }

    @Test
    fun `the dark scheme fills every role the light one does`() {
        // A role left unset shows through as a stray Material default — usually a bright
        // grey panel over the page, which is exactly what the schemes exist to prevent.
        val roles: List<Pair<String, (ColorScheme) -> Color>> = listOf(
            "surfaceContainerLowest" to { it.surfaceContainerLowest },
            "surfaceContainerLow" to { it.surfaceContainerLow },
            "surfaceContainer" to { it.surfaceContainer },
            "surfaceContainerHigh" to { it.surfaceContainerHigh },
            "surfaceContainerHighest" to { it.surfaceContainerHighest },
            "surfaceBright" to { it.surfaceBright },
            "surfaceDim" to { it.surfaceDim },
            "inverseSurface" to { it.inverseSurface },
            "inverseOnSurface" to { it.inverseOnSurface },
            "outline" to { it.outline },
            "outlineVariant" to { it.outlineVariant },
            "scrim" to { it.scrim },
        )

        roles.forEach { (name, role) ->
            val light = role(ParchmentColors)
            val dark = role(CandlelightColors)
            assertTrue(
                "$name is the same in both schemes, so one of them wasn't set",
                light != dark,
            )
        }
    }

    @Test
    fun `menus and dialogs stay on the page's own surfaces`() {
        // Material pulls these for popups. If any of them is lighter than the page in the
        // dark scheme, a menu opens as a bright slab over a dark sheet.
        listOf(
            "surfaceContainerLowest" to CandlelightColors.surfaceContainerLowest,
            "surfaceContainer" to CandlelightColors.surfaceContainer,
            "surfaceContainerHighest" to CandlelightColors.surfaceContainerHighest,
            "surfaceBright" to CandlelightColors.surfaceBright,
        ).forEach { (name, color) ->
            assertTrue(
                "$name is too bright for a dark sheet",
                luminance(color) < 0.1,
            )
        }
    }
}
