package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SaveDcData
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.data.model.Ability

/**
 * One save DC the character can impose, and the spell attack bonus that goes with it.
 *
 * [isPrimary] marks the starting class's spellcasting, which is what the sheet headlines.
 */
data class SaveDc(
    val id: String,
    val label: String,
    val ability: Ability,
    val dc: Int,
    val attackBonus: Int,
    val note: String,
    val isPrimary: Boolean = false,
)

/**
 * Every save DC the character has, kept separate by source.
 *
 * A Monk's Stunning Strike is 8 + Proficiency Bonus + Wisdom. If that same Monk takes Magic
 * Initiate (Wizard), the cantrip it grants uses Intelligence instead. Multiclassing adds more
 * of the same: a Cleric 3 / Wizard 2 casts from Wisdom and Intelligence at once. None of
 * these replaces another, so the sheet lists them side by side rather than picking one.
 */
object CharacterDcs {

    fun all(character: PlayerCharacter): List<SaveDc> {
        val mods = CharacterCalculations.abilityModifiers(character)
        // Proficiency Bonus is one of the two things that works off total character level.
        val pb = CharacterCalculations.proficiencyBonus(character)

        fun dcFor(
            id: String,
            label: String,
            ability: Ability,
            note: String,
            isPrimary: Boolean = false,
        ): SaveDc {
            val mod = mods[ability] ?: 0
            return SaveDc(
                id = id,
                label = label,
                ability = ability,
                dc = 8 + pb + mod,
                attackBonus = pb + mod,
                note = note,
                isPrimary = isPrimary,
            )
        }

        // The headline DC uses the best of the character's casting abilities, so the entry
        // marked primary has to be the one that produced it — not simply the starting class.
        // A Wizard 5 / Cleric 5 with the better Wisdom shows the Cleric line as primary.
        val headlineAbility = CharacterCalculations.spellcastingAbility(character)
        var primaryClaimed = false

        // Gathered by the rules engine rather than here: every source that sets a DC — a
        // caster's Spellcasting, a Monk's features, a subclass, a species, a feat — declares
        // it as one effect, so this only has to turn each into a number.
        return RulesEngine.saveDcs(character).map { applied ->
            val ability = applied.effect.ability.resolve(character) ?: Ability.CHA
            val isPrimary = !primaryClaimed &&
                ability == headlineAbility &&
                applied.effect.id.startsWith("class:")
            if (isPrimary) primaryClaimed = true
            val base = dcFor(
                id = applied.effect.id,
                label = applied.effect.label,
                ability = ability,
                note = applied.effect.note,
                isPrimary = isPrimary,
            )
            // Edit Mode can pin the headline DC and attack bonus by hand, and those overrides
            // belong to whichever line is the headline.
            if (isPrimary) {
                base.copy(
                    dc = CharacterCalculations.spellSaveDc(character) ?: base.dc,
                    attackBonus = CharacterCalculations.spellAttackBonus(character)
                        ?: base.attackBonus,
                )
            } else {
                base
            }
        }.distinctBy { it.id }
    }

    /** The DC the sheet leads with: the starting class's spellcasting, or the first source. */
    fun primary(character: PlayerCharacter): SaveDc? {
        val dcs = all(character)
        return dcs.firstOrNull { it.isPrimary } ?: dcs.firstOrNull()
    }

    /** True when the character has more than one DC in play, so the sheet must show them all. */
    fun hasMultiple(character: PlayerCharacter): Boolean = all(character).size > 1
}
