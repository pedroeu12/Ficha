package com.pedroeu.ficha.ui.components

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.animation.AnimatedVisibility
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.design.Motion
import com.pedroeu.ficha.ui.design.Corner
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A tappable card used for picking a species, class, background, or feature option.
 *
 * Three states, each shown the same way everywhere: **selected** carries an accented border,
 * a tinted surface and a tick; **unavailable** dims as one piece and says why; anything else
 * is plainly pickable. Border and surface both animate, so choosing is a movement.
 *
 * Long rules text no longer folds open in the card. It is clamped with a "Read the rules"
 * affordance that opens the same sheet everything else in the app opens — the alternative was
 * a picker whose rows changed height under your thumb while you were reading them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    /** False greys the card out and stops it responding, for an option already owned. */
    enabled: Boolean = true,
    expandedContent: (@Composable () -> Unit)? = null,
    /**
     * Clamps a long [subtitle] to this many lines. Past the clamp the full text opens in the
     * app's one detail sheet rather than unfolding in place.
     */
    subtitleMaxLines: Int? = null,
    /** Why this option can't be taken, when it can't. */
    unavailableReason: String = tr("Already gained from another source"),
) {
    var readingRules by rememberSaveable(title) { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = Motion.quick(),
        label = "cardBorder",
    )
    val container by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = Motion.quick(),
        label = "cardContainer",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = Corner.row,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            Modifier.padding(Space.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Space.tight),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    if (!enabled) {
                        Text(
                            text = unavailableReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (trailingLabel != null) {
                        Text(
                            text = trailingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = tr("Selected"),
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = subtitleMaxLines ?: Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.length > LONG_TEXT_THRESHOLD) {
                    Text(
                        text = tr("Read the rules"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = Space.tight)
                            .clip(Corner.small)
                            .clickable { readingRules = true }
                            .padding(vertical = Space.tight / 2),
                    )
                }
            }
            AnimatedVisibility(
                visible = selected && expandedContent != null,
                enter = expandVertically(Motion.standard()) + fadeIn(Motion.standard()),
                exit = shrinkVertically(Motion.standard()) + fadeOut(Motion.quick()),
            ) {
                Column(Modifier.padding(top = Space.inline)) {
                    expandedContent?.invoke()
                }
            }
        }
    }

    if (readingRules) {
        DetailSheet(
            detail = Detail(title = title, kind = trailingLabel.orEmpty(), body = subtitle),
            onDismiss = { readingRules = false },
        )
    }
}

/** A compact toggle chip for multi-select lists such as skill proficiencies. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.secondary
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onSecondary
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .clip(Corner.row)
            .background(container)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outlineVariant,
                Corner.row,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.75f),
            )
        }
    }
}

/** A titled section header used to break long screens into labeled blocks. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
