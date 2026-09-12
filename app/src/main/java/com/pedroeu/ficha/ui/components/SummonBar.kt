package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.rules.ActiveSummon
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Who the sheet is showing: the character, or one of the creatures they have out.
 *
 * It sits above the point where the phone and the tablet part, so both get the same row from
 * one implementation and neither can gain a creature the other cannot reach. It shows itself
 * only when there is something to switch to or something to summon, so a character who never
 * summons anything never sees it.
 */
@Composable
fun SummonBar(
    summons: List<ActiveSummon>,
    /** Null when the character's own sheet is showing. */
    selectedInstanceId: String?,
    onSelectCharacter: () -> Unit,
    onSelectSummon: (String) -> Unit,
    onSummonSomething: (() -> Unit)?,
    characterName: String,
    modifier: Modifier = Modifier,
) {
    if (summons.isEmpty() && onSummonSomething == null) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Space.screenEdge, vertical = Space.inline),
        horizontalArrangement = Arrangement.spacedBy(Space.inline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Persona(
                label = characterName.ifBlank { tr("Character") },
                detail = tr("Your sheet"),
                selected = selectedInstanceId == null,
                onClick = onSelectCharacter,
            )
        }
        items(summons, key = { it.instanceId }) { summon ->
            Persona(
                label = summon.name,
                detail = "${summon.currentHp}/${summon.maxHp}",
                selected = summon.instanceId == selectedInstanceId,
                down = summon.isDown,
                onClick = { onSelectSummon(summon.instanceId) },
            )
        }
        if (onSummonSomething != null) {
            item {
                Row(
                    Modifier
                        .clip(Corner.row)
                        .clickable(onClick = onSummonSomething)
                        .padding(horizontal = Space.betweenRows, vertical = Space.inline),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.tight),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = tr("Summon"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun Persona(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    down: Boolean = false,
) {
    val border = when {
        selected -> MaterialTheme.colorScheme.primary
        down -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        Modifier
            .clip(Corner.row)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, border, Corner.row)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.betweenRows, vertical = Space.inline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.inline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = if (down) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
