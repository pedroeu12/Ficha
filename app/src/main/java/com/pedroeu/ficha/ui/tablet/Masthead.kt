package com.pedroeu.ficha.ui.tablet

import com.pedroeu.ficha.ui.design.Corner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.ui.i18n.tr

/**
 * The band across the head of the sheet.
 *
 * On paper this is the first thing printed and the first thing filled in: the name in a wide
 * ruled box, the particulars beneath it, and the numbers you reach for constantly — armour
 * class, hit points, initiative — set out to the right where a hand resting on the sheet
 * doesn't cover them. It stays put while the leaves below it turn, because those numbers are
 * needed on every one of them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Masthead(handle: SheetHandle) {
    val v = LocalVellum.current
    val character = handle.character

    val species = SpeciesData.byId(character.speciesId)?.name.orEmpty()
    val background = BackgroundData.byId(character.backgroundId)?.name.orEmpty()
    // Every class picks its own subclass, so a multiclassed character has more than one.
    val subclass = ClassLevels.subclassLabel(character)
    val classes = ClassLevels.label(character)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Both halves are weighted. With only one of them weighted, the vitals could measure
        // as wide as their longest caption and leave the name nothing — which is exactly what
        // happened: a name column squeezed to a few pixels renders one letter per line.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(Corner.small)
                    .clickable(enabled = handle.editMode) {
                        handle.editText("character:name", tr("Character name"), character.name)
                    }
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                    ),
                    color = v.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (handle.editMode) 1.5.dp else 1.dp)
                        .background(if (handle.editMode) v.accent.copy(alpha = 0.7f) else v.rule)
                )
                Spacer(Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Particular(tr("Species"), species)
                    Particular(tr("Class"), classes)
                    if (subclass.isNotBlank()) Particular(tr("Subclass"), subclass)
                    if (background.isNotBlank()) Particular(tr("Origin"), background)
                    Particular("XP", "${character.experiencePoints}")
                }
            }

            // The numbers looked at every round. A FlowRow so a narrower tablet wraps them
            // onto a second line instead of crushing the name beside them.
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VitalStone(
                    handle = handle,
                    stat = OverridableStat.ARMOR_CLASS,
                    value = "${CharacterCalculations.armorClass(character)}",
                    label = tr("Armor Class"),
                )
                VitalStone(
                    handle = handle,
                    stat = OverridableStat.INITIATIVE,
                    value = CharacterCalculations.formatModifier(
                        CharacterCalculations.initiative(character)
                    ),
                    label = tr("Initiative"),
                )
                VitalStone(
                    handle = handle,
                    stat = OverridableStat.SPEED,
                    value = "${CharacterCalculations.speed(character)}",
                    label = tr("Speed"),
                )
                VitalStone(
                    handle = handle,
                    stat = OverridableStat.PROFICIENCY_BONUS,
                    value = CharacterCalculations.formatModifier(
                        CharacterCalculations.proficiencyBonus(character)
                    ),
                    label = tr("Proficiency"),
                )
                VitalStone(
                    handle = handle,
                    stat = OverridableStat.PASSIVE_PERCEPTION,
                    value = "${CharacterCalculations.passivePerception(character)}",
                    label = tr("Passive Perception"),
                )
                Stone(modifier = Modifier.width(STONE_WIDTH)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = CharacterCalculations.size(character),
                            style = NumeralMedium.copy(fontSize = 15.sp),
                            color = v.ink,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                        Caption(tr("Size"), align = TextAlign.Center)
                    }
                }
            }
        }

        InkRule(strong = true)
    }
}

/**
 * Every boxed number is the same width.
 *
 * Left to size themselves, the widest caption decides — "Passive Perception" is four times
 * the width of "Speed" — and the row grows until it has eaten the name beside it.
 */
private val STONE_WIDTH = 92.dp

/** One of the small facts printed under the name. */
@Composable
private fun Particular(label: String, value: String) {
    val v = LocalVellum.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Caption(label)
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            ),
            color = v.inkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One of the boxed numbers at the head of the sheet, writable in Edit Mode.
 *
 * Every one of these is a rules calculation that a table may want to overrule — a homebrew
 * item, a DM's ruling — which is why they are all the same shape and all open the same
 * dialog. Consistency here is what stops Edit Mode being a scavenger hunt.
 */
@Composable
private fun VitalStone(
    handle: SheetHandle,
    stat: OverridableStat,
    value: String,
    label: String,
) {
    val v = LocalVellum.current
    Stone(
        modifier = Modifier.width(STONE_WIDTH),
        // Only in Edit Mode, the same as the phone. These are rules calculations, and a stray
        // tap during play should not open a dialog offering to overrule one.
        onClick = if (handle.editMode) ({ handle.editStat(stat) }) else null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = NumeralMedium,
                color = if (handle.isAdjusted(stat)) v.accent else v.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Caption(label, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
