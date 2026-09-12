package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.StatblockData
import com.pedroeu.ficha.data.content.SummonData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.rules.ActiveSummon
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.FormulaEval
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.Statblock
import java.util.UUID

/**
 * Summoning, for whatever summons.
 *
 * One system rather than a spell's worth of special cases: what a character can summon comes
 * from the rules engine, what each creature's numbers are comes from formulas worked out
 * against the summoner, and what is currently on the table is a list the character carries.
 * Adding a summoning spell is a row in [SummonData] and a stat block in [StatblockData].
 */
object CharacterSummons {

    /** Something the character can summon right now, with its options resolved. */
    data class Summonable(
        val summons: Effect.Summons,
        /** The stat blocks this can produce — one for a fixed summon, several for a choice. */
        val options: List<Statblock>,
        /** The spell that casts it, when a spell does. */
        val spellId: String = "",
        /** The class whose level the creature's formulas count against. */
        val owningClassId: String? = null,
        /** Slot levels this can be cast at, empty for a feature that isn't a spell. */
        val castableAt: List<Int> = emptyList(),
    ) {
        val needsAChoice: Boolean get() = options.size > 1
    }

    /**
     * Everything the character could summon.
     *
     * Drawn from the spells they actually have and the features they actually reached, so a
     * Wizard who never prepared Summon Beast is not offered it, and a Battle Smith is offered
     * their Steel Defender without a spell being involved at all.
     */
    fun available(character: PlayerCharacter): List<Summonable> {
        val fromSpells = CharacterSpells.all(character).mapNotNull { known ->
            val summons = SummonData.forSpell(known.id) ?: return@mapNotNull null
            val spell = SpellData.byId(known.id)
            Summonable(
                summons = summons,
                options = StatblockData.idsMatching(
                    SummonData.statblockIdsOf(summons.pick)
                ),
                spellId = known.id,
                owningClassId = ClassLevels.of(character).firstOrNull()?.classId,
                castableAt = castableLevels(character, spell?.level ?: 1),
            )
        }

        val fromFeatures = RulesEngine.elementsFor(character).mapNotNull { element ->
            val summons = SummonData.forFeature(element.id) ?: return@mapNotNull null
            Summonable(
                summons = summons,
                options = StatblockData.idsMatching(SummonData.statblockIdsOf(summons.pick)),
                owningClassId = element.source.owningClassId,
            )
        }

        return (fromSpells + fromFeatures).distinctBy { it.summons.summonId }
    }

    /** Slot levels the character could spend on a spell of [baseLevel]. */
    private fun castableLevels(character: PlayerCharacter, baseLevel: Int): List<Int> {
        if (baseLevel == 0) return emptyList()
        val slots = CharacterCalculations.spellSlots(character)
        return (baseLevel..9).filter { level -> (slots[level] ?: 0) > 0 }
    }

    /**
     * Puts a creature on the table.
     *
     * Its hit points are worked out here and stored, rather than recomputed on every read: a
     * Bestial Spirit called up with a level 5 slot keeps its level 5 hit points even if the
     * summoner later gains a level, because the creature standing there does not change.
     */
    fun summon(
        character: PlayerCharacter,
        statblockId: String,
        sourceId: String,
        sourceLabel: String,
        spellLevel: Int = 0,
        owningClassId: String? = null,
        concentration: Boolean = false,
        howMany: Int = 1,
    ): PlayerCharacter {
        val statblock = StatblockData.byId(statblockId) ?: return character
        val maxHp = statblock.hitPointsFor(character, spellLevel, owningClassId)
        val existing = character.activeSummons.count { it.statblockId == statblockId }

        val fresh = (1..howMany.coerceAtLeast(1)).map { index ->
            ActiveSummon(
                instanceId = "summon:${UUID.randomUUID()}",
                statblockId = statblockId,
                sourceId = sourceId,
                sourceLabel = sourceLabel,
                // Numbered only once there is more than one to tell apart.
                name = if (howMany > 1 || existing > 0) {
                    "${statblock.name} ${existing + index}"
                } else {
                    statblock.name
                },
                currentHp = maxHp,
                maxHp = maxHp,
                spellLevel = spellLevel,
                concentration = concentration,
            )
        }
        return character.copy(activeSummons = character.activeSummons + fresh)
    }

    fun dismiss(character: PlayerCharacter, instanceId: String): PlayerCharacter =
        character.copy(
            activeSummons = character.activeSummons.filterNot { it.instanceId == instanceId }
        )

    fun dismissAll(character: PlayerCharacter): PlayerCharacter =
        character.copy(activeSummons = emptyList())

    /**
     * Ends every summon the character was concentrating on.
     *
     * Concentration is one thing at a time, so a spell that takes it dismisses whatever the
     * last one was holding up. Called when a new concentration spell is cast and when the
     * player drops concentration by hand.
     */
    fun endConcentration(character: PlayerCharacter): PlayerCharacter =
        character.copy(activeSummons = character.activeSummons.filterNot { it.concentration })

    fun setHitPoints(
        character: PlayerCharacter,
        instanceId: String,
        current: Int,
        temp: Int? = null,
    ): PlayerCharacter = character.copy(
        activeSummons = character.activeSummons.map { summon ->
            if (summon.instanceId != instanceId) summon
            else summon.copy(
                currentHp = current.coerceIn(0, summon.maxHp),
                tempHp = temp ?: summon.tempHp,
            )
        }
    )

    /** Damage lands on temporary hit points first, as it does for the character. */
    fun damage(character: PlayerCharacter, instanceId: String, amount: Int): PlayerCharacter =
        character.copy(
            activeSummons = character.activeSummons.map { summon ->
                if (summon.instanceId != instanceId) summon else {
                    val fromTemp = minOf(summon.tempHp, amount)
                    summon.copy(
                        tempHp = summon.tempHp - fromTemp,
                        currentHp = (summon.currentHp - (amount - fromTemp)).coerceAtLeast(0),
                    )
                }
            }
        )

    fun heal(character: PlayerCharacter, instanceId: String, amount: Int): PlayerCharacter =
        character.copy(
            activeSummons = character.activeSummons.map { summon ->
                if (summon.instanceId != instanceId) summon
                else summon.copy(
                    currentHp = (summon.currentHp + amount).coerceAtMost(summon.maxHp)
                )
            }
        )

    fun rename(character: PlayerCharacter, instanceId: String, name: String): PlayerCharacter =
        character.copy(
            activeSummons = character.activeSummons.map {
                if (it.instanceId == instanceId) it.copy(name = name.trim()) else it
            }
        )

    fun setNotes(character: PlayerCharacter, instanceId: String, notes: String): PlayerCharacter =
        character.copy(
            activeSummons = character.activeSummons.map {
                if (it.instanceId == instanceId) it.copy(notes = notes) else it
            }
        )

    fun spend(character: PlayerCharacter, instanceId: String, trait: String, delta: Int) =
        character.copy(
            activeSummons = character.activeSummons.map { summon ->
                if (summon.instanceId != instanceId) summon else {
                    val now = (summon.spent[trait] ?: 0) + delta
                    summon.copy(spent = summon.spent + (trait to now.coerceAtLeast(0)))
                }
            }
        )

    /** The stat block behind a creature on the table. */
    fun statblockOf(summon: ActiveSummon): Statblock? = StatblockData.byId(summon.statblockId)

    /**
     * A summoned creature's attack bonus, which is almost always the summoner's.
     *
     * "Bonus equals your spell attack modifier" is the 2024 wording for every summoned spirit,
     * so the number follows the caster and changes when they do.
     */
    fun attackBonus(character: PlayerCharacter, summon: ActiveSummon): Int? =
        CharacterCalculations.spellAttackBonus(character)

    /** A number in one of the creature's actions, worked out for this summoner and slot. */
    fun evaluate(
        character: PlayerCharacter,
        summon: ActiveSummon,
        formula: com.pedroeu.ficha.rules.Formula,
        owningClassId: String? = null,
    ): Int = FormulaEval.eval(formula, character, owningClassId, summon.spellLevel)
}
