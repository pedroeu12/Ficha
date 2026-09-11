package com.pedroeu.ficha.ui.tablet

import com.pedroeu.ficha.ui.design.Corner
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.i18n.tr

/**
 * One ability and everything that hangs off it.
 *
 * The printed sheet's best idea: the modifier is enormous, the score is a small circle beneath
 * it, and the saving throw and every skill that uses that ability are listed directly under —
 * so the number you need is always next to the thing you need it for. The phone can't do this
 * because six of these don't fit in one column; a tablet can, and it removes an entire tab.
 */
@Composable
fun AbilityBlock(
    ability: Ability,
    character: PlayerCharacter,
    viewModel: SheetHandle,
    editMode: Boolean,
) {
    val v = LocalVellum.current
    val score = CharacterCalculations.finalAbilityScores(character)[ability] ?: 10
    val modifier = CharacterCalculations.abilityModifiers(character)[ability] ?: 0
    val saveBonus = CharacterCalculations.savingThrowBonus(character, ability)
    val saveProficient = CharacterCalculations.isSavingThrowProficient(character, ability)
    val skills = Skill.ALL.filter { it.ability == ability }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The block's own head: name, modifier, score.
        Box(
            Modifier
                .fillMaxWidth()
                .clip(Corner.row)
                .background(v.well.copy(alpha = if (v.isDark) 0.85f else 0.7f))
                .border(1.dp, v.rule, Corner.row)
                .clickable(enabled = editMode) { viewModel.editAbility(ability) }
                .padding(vertical = 8.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Caption(tr(ability.fullName), color = v.accent)
                Text(
                    text = CharacterCalculations.formatModifier(modifier),
                    style = NumeralLarge,
                    color = v.ink,
                )
                // The score in its own ring, the way the sheet prints it.
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(v.page)
                        .border(1.dp, v.ruleStrong.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = v.inkSoft,
                    )
                }
            }
        }

        // The saving throw, then the skills, each on its own ruled line.
        ProficiencyLine(
            label = tr("Saving Throw"),
            bonus = saveBonus,
            state = if (saveProficient) PipState.PROFICIENT else PipState.NONE,
            emphasis = true,
            onTogglePip = if (editMode) {
                { viewModel.toggleSaveProficiency(ability) }
            } else {
                null
            },
            onOpen = if (editMode) {
                { viewModel.editSave(ability) }
            } else {
                null
            },
        )

        skills.forEach { skill ->
            val expert = skill.name in character.skillExpertise
            val proficient = skill.name in character.skillProficiencies
            ProficiencyLine(
                label = tr(skill.displayName),
                bonus = CharacterCalculations.skillBonus(character, skill),
                state = when {
                    expert -> PipState.EXPERT
                    proficient -> PipState.PROFICIENT
                    else -> PipState.NONE
                },
                adjusted = skill.name in character.skillBonuses ||
                    skill.name in character.skillOverrides,
                onTogglePip = { viewModel.cycleSkill(skill) },
                onOpen = { viewModel.editSkill(skill) },
            )
        }
    }
}

/** How trained the character is in one thing, in the three states a sheet can print. */
enum class PipState { NONE, PROFICIENT, EXPERT }

/**
 * A line of the ability block: the training mark, the name, and the number.
 *
 * Tapping the mark cycles the training, because that is the thing a player changes; tapping
 * the line opens the bonus and override controls. Two targets on one row rather than a mode
 * switch, which is how a pencil works on paper.
 */
@Composable
private fun ProficiencyLine(
    label: String,
    bonus: Int,
    state: PipState,
    emphasis: Boolean = false,
    adjusted: Boolean = false,
    onTogglePip: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    val v = LocalVellum.current

    Row(
        Modifier
            .fillMaxWidth()
            .clip(Corner.small)
            .let { if (onOpen != null) it.clickable(onClick = onOpen) else it }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrainingMark(state, onClick = onTogglePip)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (emphasis) v.ink else v.inkSoft,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = CharacterCalculations.formatModifier(bonus),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (adjusted) v.accent else v.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

/**
 * The mark beside a skill: empty, filled for proficiency, ringed for expertise.
 *
 * A doubled ring rather than a second colour, so the difference survives being read across a
 * table in bad light — and so the sheet still makes sense to someone who prints it.
 */
@Composable
private fun TrainingMark(state: PipState, onClick: (() -> Unit)?) {
    val v = LocalVellum.current
    Box(
        Modifier
            .size(16.dp)
            .clip(CircleShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        // The outer ring is always there; expertise fills a second ring inside it.
        Box(
            Modifier
                .size(if (state == PipState.EXPERT) 16.dp else 12.dp)
                .clip(CircleShape)
                .background(
                    if (state == PipState.NONE) androidx.compose.ui.graphics.Color.Transparent
                    else v.accent
                )
                .border(
                    1.5.dp,
                    if (state == PipState.NONE) v.ruleStrong.copy(alpha = 0.55f) else v.accent,
                    CircleShape,
                )
        )
        if (state == PipState.EXPERT) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(v.page)
            )
        }
    }
}

/** A thin rule with room around it, for separating stacked ability blocks. */
@Composable
fun BlockSpacer() {
    Spacer(Modifier.height(6.dp))
    InkRule()
    Spacer(Modifier.height(6.dp))
}
