package com.pedroeu.ficha.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.design.Motion
import com.pedroeu.ficha.ui.design.Space

/**
 * One row of a list, everywhere in the app.
 *
 * Every list in the sheet is the same shape underneath — something with a name, a line of
 * detail, sometimes a number on the right — and they were all drawn differently. This is the
 * one shape, and it carries the three states a player needs to read at a glance:
 *
 * - **Openable**: a chevron on the right. If there is no chevron, tapping does nothing.
 * - **Selected**: an accented border and tint, not a tick buried in the text.
 * - **Unavailable**: everything dimmed together, so it reads as one disabled thing rather
 *   than as text that happens to be grey.
 *
 * Both the background and the border animate, so selecting a row is a movement rather than a
 * repaint.
 */
@Composable
fun DetailRow(
    title: String,
    modifier: Modifier = Modifier,
    /** The one line under the name: a summary, a cost, where it came from. */
    supporting: String = "",
    /** A short label on the right, before the chevron. */
    trailing: String = "",
    selected: Boolean = false,
    enabled: Boolean = true,
    /** Non-null draws the chevron and makes the row tappable. */
    onClick: (() -> Unit)? = null,
    /** Anything that belongs before the title: a pip, a die, a level. */
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val interactive = enabled && onClick != null

    val background by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        animationSpec = Motion.quick(),
        label = "rowBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
        animationSpec = Motion.quick(),
        label = "rowBorder",
    )

    val contentAlpha = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(Corner.row)
            .background(background)
            .border(if (selected) 1.dp else 0.dp, borderColor, Corner.row)
            .then(if (interactive) Modifier.clickable { onClick!!() } else Modifier)
            .padding(horizontal = Space.betweenRows, vertical = Space.inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.betweenRows),
    ) {
        leading?.invoke(this)

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Space.tight / 2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            if (supporting.isNotBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailing.isNotBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = contentAlpha),
            )
        }

        // The chevron is the promise that there is more to read. A row without one is a row
        // that does nothing when tapped, and the player can see which is which.
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha * 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The heading of a section that folds away, with the chevron that turns as it opens.
 *
 * Every collapsible thing in the app used to draw its own: a "▾" and a "▸" swapped in a text
 * label, snapping between two glyphs with no motion at all, and written out three separate
 * times. One header, one rotation, one duration.
 */
@Composable
fun SectionDisclosure(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** A count or other short note on the right. */
    trailing: String = "",
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = Motion.standard(),
        label = "disclosureChevron",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Corner.small)
            .clickable(onClick = onToggle)
            .padding(vertical = Space.inline, horizontal = Space.tight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.tight),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        if (trailing.isNotBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The standard way a folded section's content arrives and leaves. */
@Composable
fun Disclosed(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(Motion.standard()) + fadeIn(Motion.standard()),
        exit = shrinkVertically(Motion.standard()) + fadeOut(Motion.quick()),
    ) { content() }
}
