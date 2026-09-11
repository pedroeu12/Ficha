package com.pedroeu.ficha.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The measurements the whole app is built from.
 *
 * Before this, every screen chose its own: cards were 10.dp, 14.dp and 16.dp rounded on three
 * tabs of the same sheet, padding ran 6, 8, 10, 12, 14, 16 and 20, and a list row was a
 * different height depending on which month it was written in. None of that was a decision —
 * it was the absence of one, repeated forty times.
 *
 * The scale is a 4.dp grid. Everything here is named for what it is used for rather than how
 * big it is, so a screen asks for [Space.betweenCards] instead of picking a number, and a
 * change to the scale reaches every screen at once.
 */
object Space {
    /** Between a label and the value it labels. */
    val tight: Dp = 4.dp

    /** Inside a chip, between an icon and its text, between the rows of one block. */
    val inline: Dp = 8.dp

    /** Between the rows of a list, and between a heading and what follows it. */
    val betweenRows: Dp = 12.dp

    /** Inside a card, from its edge to its content. */
    val cardPadding: Dp = 16.dp

    /** Between cards in a column. */
    val betweenCards: Dp = 16.dp

    /** From the edge of the screen to the content. */
    val screenEdge: Dp = 16.dp

    /** Inside a bottom sheet, which sits further from the edge than a card does. */
    val sheetEdge: Dp = 20.dp

    /** Below the last thing in a sheet, clear of the gesture bar. */
    val sheetBottom: Dp = 32.dp
}

/** The corner radii. Three sizes, each with a job. */
object Corner {
    /** A progress bar's ends, where the radius is half the thickness rather than a style. */
    val bar = RoundedCornerShape(2.dp)

    /** The tablet's sheet of paper, which is barely rounded because paper barely is. */
    val page = RoundedCornerShape(4.dp)

    /** Chips, pills, small marks. */
    val small = RoundedCornerShape(8.dp)

    /** List rows and anything nested inside a card. */
    val row = RoundedCornerShape(12.dp)

    /** Cards, the outermost surface on a screen. */
    val card = RoundedCornerShape(16.dp)
}

/**
 * How long things take, and the shape of the curve.
 *
 * Three durations. Anything that must feel instant uses [quick]; anything that changes the
 * size or content of a surface uses [standard]; a whole screen or sheet arriving uses
 * [emphasis]. The easing is the same decelerating curve throughout, which is what makes
 * unrelated animations read as one interface rather than several.
 */
object Motion {
    const val QUICK_MS = 120
    const val STANDARD_MS = 220
    const val EMPHASIS_MS = 320

    /** Fast out, slow in: leaves immediately, settles gently. */
    val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Symmetric, for something that is only changing colour or alpha. */
    val gentle: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun <T> quick(): FiniteAnimationSpec<T> = tween(QUICK_MS, easing = gentle)

    fun <T> standard(): FiniteAnimationSpec<T> = tween(STANDARD_MS, easing = easing)

    fun <T> emphasis(): FiniteAnimationSpec<T> = tween(EMPHASIS_MS, easing = easing)
}

/**
 * How long a description may be before it stops belonging inline.
 *
 * Kept here rather than beside one component because it is a rule about the interface, not
 * about that component: past this length, text opens in a sheet of its own, wherever it is.
 */
const val LONG_TEXT_CHARS = 180
