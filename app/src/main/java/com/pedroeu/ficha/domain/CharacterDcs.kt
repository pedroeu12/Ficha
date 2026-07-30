package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SaveDcData
import com.pedroeu.ficha.data.model.Ability

/**
 * One save DC the character can impose, and the spell attack bonus that goes with it.
 *
 * [isPrimary] marks the class's own spellcasting, which is what the Spells tab headlines.
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
 * Initiate (Wizard), the cantrip it grants uses Intelligence instead. Both are live at once
 * and neither is "the" DC, so the sheet lists them side by side rather than picking one.
 */
object CharacterDcs {

    fun all(character: PlayerCharacter): List<SaveDc> {
        val mods = CharacterCalculations.abilityModifiers(character)
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

        return buildList {
            // The class's own spellcasting, when it has any.
            val charClass = ClassData.byId(character.classId)
            charClass?.spellcastingAbility?.let { ability ->
                add(
                    dcFor(
                        id = "class:${character.classId}",
                        label = charClass.name,
                        ability = ability,
                        note = "${charClass.name} spells.",
                        isPrimary = true,
                    ).let { primary ->
                        // Edit Mode can pin the headline DC and attack bonus by hand.
                        primary.copy(
                            dc = CharacterCalculations.spellSaveDc(character) ?: primary.dc,
                            attackBonus = CharacterCalculations.spellAttackBonus(character)
                                ?: primary.attackBonus,
                        )
                    }
                )
            }

            // Features that force saves without granting spellcasting, e.g. a Monk's.
            SaveDcData.forClass(character.classId)
                ?.takeIf { charClass?.spellcastingAbility != it.ability }
                ?.let { add(dcFor("feature:${it.id}", it.label, it.ability, it.note)) }

            SaveDcData.forSubclass(character.subclassId)
                ?.let { add(dcFor("subclass:${it.id}", it.label, it.ability, it.note)) }

            SaveDcData.forSpecies(character.speciesId)
                ?.let { add(dcFor("species:${it.id}", it.label, it.ability, it.note)) }

            SaveDcData.forLineage(character.lineageId)
                ?.let { add(dcFor("lineage:${it.id}", it.label, it.ability, it.note)) }

            character.featIds.forEach { featId ->
                SaveDcData.forFeat(featId)
                    ?.let { add(dcFor("feat:${it.id}", it.label, it.ability, it.note)) }
            }
        }.distinctBy { it.id }
    }

    /** The DC the sheet leads with: the class's spellcasting, or the first source it has. */
    fun primary(character: PlayerCharacter): SaveDc? =
        all(character).firstOrNull { it.isPrimary } ?: all(character).firstOrNull()

    /** True when the character has more than one DC in play, so the sheet must show them all. */
    fun hasMultiple(character: PlayerCharacter): Boolean = all(character).size > 1
}
