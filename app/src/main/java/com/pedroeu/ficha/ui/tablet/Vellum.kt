package com.pedroeu.ficha.ui.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedroeu.ficha.ui.theme.CrimsonNight
import com.pedroeu.ficha.ui.theme.CrimsonPrimary
import com.pedroeu.ficha.ui.theme.DangerRed
import com.pedroeu.ficha.ui.theme.DangerRedNight
import com.pedroeu.ficha.ui.theme.GoldAccentDeep
import com.pedroeu.ficha.ui.theme.GoldNight
import com.pedroeu.ficha.ui.theme.InkBrown
import com.pedroeu.ficha.ui.theme.InkBrownFaint
import com.pedroeu.ficha.ui.theme.InkBrownSoft
import com.pedroeu.ficha.ui.theme.NightCard
import com.pedroeu.ficha.ui.theme.NightDeep
import com.pedroeu.ficha.ui.theme.NightEdge
import com.pedroeu.ficha.ui.theme.NightFloor
import com.pedroeu.ficha.ui.theme.NightPage
import com.pedroeu.ficha.ui.theme.NightShade
import com.pedroeu.ficha.ui.theme.ParchmentCard
import com.pedroeu.ficha.ui.theme.ParchmentDeep
import com.pedroeu.ficha.ui.theme.ParchmentEdge
import com.pedroeu.ficha.ui.theme.ParchmentPage
import com.pedroeu.ficha.ui.theme.ParchmentShade
import com.pedroeu.ficha.ui.theme.VellumBright
import com.pedroeu.ficha.ui.theme.VellumFaint
import com.pedroeu.ficha.ui.theme.VellumSoft

/**
 * The look of the tablet sheet: printed paper rather than an interface.
 *
 * The phone is a stack of Material cards, which is right for a phone — a card is a good way
 * to say "this is one thing" when only one thing fits on screen at a time. A tablet showing
 * the whole sheet at once doesn't need that: forty cards floating at forty elevations is
 * busier than the paper it replaces, and the shadows are the busiest part of it.
 *
 * So this set has no cards and no elevation. Everything is drawn *on* one sheet: regions are
 * separated by hairline rules, headings are engraved into the page with a rule running out to
 * either side, values sit above ruled lines the way a printed form arranges them, and counters
 * are circles that fill with ink. The only thing that ever floats is a dialog, because a
 * dialog genuinely is a thing laid on top of the sheet.
 */
data class VellumTokens(
    /** The page itself, and the tone at its edges where the light falls away. */
    val page: Color,
    val pageEdge: Color,
    /** The table the sheet is lying on, visible as a margin around it. */
    val table: Color,
    /** An inset region — a ledger's banded row, a well, a stone tile. */
    val well: Color,
    val wellDeep: Color,
    /** Ink, in three weights. */
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    /** Ruled lines: the hairline a form is printed with, and the heavier one under a heading. */
    val rule: Color,
    val ruleStrong: Color,
    /** The gold a printed sheet uses for its section labels and filled pips. */
    val accent: Color,
    /** The crimson of the masthead. */
    val crimson: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LocalVellum = staticCompositionLocalOf { DaylightVellum }

/** Ink on aged paper, read by daylight. */
val DaylightVellum = VellumTokens(
    page = ParchmentPage,
    pageEdge = ParchmentShade,
    table = ParchmentShade,
    well = ParchmentCard,
    wellDeep = ParchmentDeep,
    ink = InkBrown,
    inkSoft = InkBrownSoft,
    inkFaint = InkBrownFaint,
    rule = ParchmentEdge,
    ruleStrong = InkBrownFaint,
    accent = GoldAccentDeep,
    crimson = CrimsonPrimary,
    danger = DangerRed,
    isDark = false,
)

/** The same sheet on the same table, by candlelight. */
val CandlelightVellum = VellumTokens(
    page = NightPage,
    pageEdge = NightFloor,
    table = NightFloor,
    well = NightCard,
    wellDeep = NightDeep,
    ink = VellumBright,
    inkSoft = VellumSoft,
    inkFaint = VellumFaint,
    rule = NightEdge,
    ruleStrong = NightShade,
    accent = GoldNight,
    crimson = CrimsonNight,
    danger = DangerRedNight,
    isDark = true,
)

// ---------------------------------------------------------------------- Type

/**
 * The lettering a printed sheet uses for its box labels: small, spaced out, and set in
 * capitals so it reads as a caption and never competes with the number above it.
 */
val EngravedLabel = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 1.4.sp,
)

/** The serif a sheet prints its numbers in — the thing your eye goes to first. */
val NumeralLarge = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
)

val NumeralMedium = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
)

// ---------------------------------------------------------------------- The page

/**
 * The sheet of paper, with the table showing at its edges.
 *
 * The wash is three overlapping gradients rather than one: paper doesn't age evenly, and a
 * single linear fade reads as a gradient, which is exactly the flat digital look this is
 * trying to get away from. The corners darken, the middle stays bright, and a faint warm
 * bloom sits off-centre so the two halves of the page aren't mirror images.
 */
fun Modifier.paperPage(tokens: VellumTokens): Modifier = this
    .background(tokens.page)
    .drawBehind {
        // Light falling from above and to the left, as it would from a window or a candle
        // set beside the sheet.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, tokens.pageEdge.copy(alpha = 0.55f)),
                center = Offset(size.width * 0.38f, size.height * 0.30f),
                radius = size.maxDimension * 0.92f,
            )
        )
        // A second, tighter bloom keeps the fade from looking mechanical.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tokens.well.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * 0.62f, size.height * 0.66f),
                radius = size.minDimension * 0.55f,
            )
        )
        // The very edges, where a sheet on a table always sits in its own shadow.
        drawRect(
            brush = Brush.verticalGradient(
                0f to tokens.pageEdge.copy(alpha = 0.30f),
                0.06f to Color.Transparent,
                0.94f to Color.Transparent,
                1f to tokens.pageEdge.copy(alpha = 0.38f),
            )
        )
    }

// ---------------------------------------------------------------------- Marks on the page

/** The hairline a form is ruled with. */
@Composable
fun InkRule(modifier: Modifier = Modifier, strong: Boolean = false) {
    val v = LocalVellum.current
    Box(
        modifier
            .fillMaxWidth()
            .height(if (strong) 1.5.dp else 1.dp)
            .background(if (strong) v.ruleStrong.copy(alpha = 0.55f) else v.rule)
    )
}

/**
 * A section title cut into the page, with a rule running out to either side.
 *
 * This is what replaces a card header. A card says "here is a container"; a rule through a
 * heading says "the page continues, and this part of it is about attacks" — which is what a
 * printed sheet says, and what a player reading one expects.
 */
@Composable
fun EngravedHeading(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val v = LocalVellum.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(v.rule)
        )
        Text(
            text = text.uppercase(),
            style = EngravedLabel.copy(fontSize = 11.sp, letterSpacing = 2.sp),
            color = v.accent,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(v.rule)
        )
        if (trailing != null) trailing()
    }
}

/** A caption in the sheet's small capitals. */
@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 2,
) {
    val v = LocalVellum.current
    Text(
        text = text.uppercase(),
        style = EngravedLabel,
        color = color ?: v.inkFaint,
        textAlign = align,
        // Capped, so a caption in a container that measured narrower than expected clips
        // rather than turning into a column of single letters.
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * A value written on a ruled line, with its label beneath — the shape every box on a printed
 * character sheet takes.
 *
 * [onClick] makes it writable. In Edit Mode the rule thickens so it is obvious at a glance
 * which parts of the page can be written on, without putting a pencil icon on forty fields.
 */
@Composable
fun RuledField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = NumeralMedium,
    editable: Boolean = false,
    adjusted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val v = LocalVellum.current
    Column(
        modifier
            .let { if (onClick != null) it.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else it }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = valueStyle,
            color = if (adjusted) v.accent else v.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (editable) 1.5.dp else 1.dp)
                .background(if (editable) v.accent.copy(alpha = 0.7f) else v.rule)
        )
        Spacer(Modifier.height(3.dp))
        Caption(label, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * A tile with a value in it — the rounded boxes a printed sheet uses for Armor Class,
 * Initiative, and the ability scores.
 *
 * Inset rather than raised: the fill is slightly deeper than the page and the border is a
 * hairline, so it reads as something printed into the paper rather than a card lying on it.
 */
@Composable
fun Stone(
    modifier: Modifier = Modifier,
    corner: Dp = 10.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val v = LocalVellum.current
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(v.well.copy(alpha = if (v.isDark) 0.9f else 0.75f))
            .border(1.dp, v.rule, RoundedCornerShape(corner))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * One of the little circles a sheet is counted on: a use spent, a death save marked, a spell
 * slot burned. Filled with ink when used, an empty ring when not.
 */
@Composable
fun Pip(
    filled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val v = LocalVellum.current
    val colour = tint ?: v.accent
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) colour else Color.Transparent)
            .border(1.5.dp, if (filled) colour else v.ruleStrong.copy(alpha = 0.6f), CircleShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    )
}

/**
 * A region of the page: a heading, a rule, and whatever belongs under it.
 *
 * No container, no elevation, no corner radius — the section is part of the sheet, and what
 * separates it from the next one is the space around it and the line above it.
 */
@Composable
fun Leaf(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EngravedHeading(title, trailing = trailing)
        content()
    }
}

/**
 * A ruled table, the way a sheet prints its attacks and its spells.
 *
 * Alternating rows carry a barely-there wash, which is what makes a long list readable across
 * a wide column without drawing a box around every line.
 */
@Composable
fun LedgerRow(
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val v = LocalVellum.current
    val banded = index % 2 == 1
    Box(
        modifier
            .fillMaxWidth()
            .background(if (banded) v.wellDeep.copy(alpha = if (v.isDark) 0.5f else 0.45f) else Color.Transparent)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) { content() }
}

/** The column headings above a ledger. */
@Composable
fun LedgerHeader(columns: List<Pair<String, Float>>) {
    val v = LocalVellum.current
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            columns.forEach { (title, weight) ->
                Caption(title, modifier = Modifier.weight(weight))
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(v.ruleStrong.copy(alpha = 0.45f))
        )
    }
}

/** Body text on the sheet, at the weight the page reads best. */
@Composable
fun SheetText(
    text: String,
    modifier: Modifier = Modifier,
    soft: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    val v = LocalVellum.current
    Text(
        text = text,
        style = style,
        color = if (soft) v.inkSoft else v.ink,
        modifier = modifier,
    )
}

/**
 * A word set in the sheet's own hand — used for the small "written in" values that aren't
 * numbers, like a species name or a damage type.
 */
@Composable
fun InkedValue(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    maxLines: Int = 2,
) {
    val v = LocalVellum.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = v.ink,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A gap the width of a printed sheet's gutter. */
@Composable
fun Gutter(width: Dp = 20.dp) = Spacer(Modifier.width(width))
