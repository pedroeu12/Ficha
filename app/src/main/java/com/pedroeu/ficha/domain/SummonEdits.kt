package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.StatblockData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.rules.ActionKind
import com.pedroeu.ficha.rules.ActiveSummon
import com.pedroeu.ficha.rules.CustomAction
import com.pedroeu.ficha.rules.CustomStatblock
import com.pedroeu.ficha.rules.Formula
import com.pedroeu.ficha.rules.Statblock
import com.pedroeu.ficha.rules.StatblockAction
import java.util.UUID

/**
 * A summoned creature as it actually stands on the table.
 *
 * Three things are folded together here, and the reason they are in one place is that every
 * screen wants the answer and none of them should have to assemble it:
 *
 *  1. the book's stat block, where the creature came from a book;
 *  2. a creature the player wrote themselves, where it did not;
 *  3. whatever has been changed about *this* one since it was called up.
 *
 * The third is the part that was missing, and it is the difference between a stat block and a
 * creature being played. A Steel Defender given a shield has a different Armor Class than the
 * book's. A wolf the DM ruled is Large is Large. One of five identical summons is the one that
 * drank the potion. None of that could be written down, so the answer was always a note on
 * paper beside the phone — which is the thing the app exists to replace.
 *
 * Everything lands on the same [Statblock] the sheet already knows how to draw, so the edit
 * path and the book path cannot drift: there is one renderer and one shape.
 */
object SummonEdits {

    /** Marks an id as a creature the player wrote rather than one the books ship. */
    const val PREFIX = "creature:"

    fun newId(): String = "$PREFIX${UUID.randomUUID()}"

    fun isCustom(statblockId: String): Boolean = statblockId.startsWith(PREFIX)

    // ================================================================ Reading

    /** The creature this summon is an instance of, before anything was changed about it. */
    fun baseOf(character: PlayerCharacter, statblockId: String): Statblock? =
        StatblockData.byId(statblockId)
            ?: character.customStatblocks.find { it.id == statblockId }?.toStatblock()

    /** The creature as it stands: the book's, or the player's, plus this one's own edits. */
    fun resolve(character: PlayerCharacter, summon: ActiveSummon): Statblock? {
        val base = baseOf(character, summon.statblockId) ?: return null
        return base.withEdits(summon)
    }

    /** What a field says now, for a screen that wants to show it and offer to change it. */
    fun field(summon: ActiveSummon, key: String, fallback: String): String =
        summon.overrides[key] ?: fallback

    fun isEdited(summon: ActiveSummon, key: String): Boolean = key in summon.overrides

    // ================================================================ Writing

    /**
     * Changes one field of one creature. A blank value puts the book's answer back, which
     * makes undoing a mistake the same gesture as making one.
     */
    fun setField(
        character: PlayerCharacter,
        instanceId: String,
        key: String,
        value: String?,
    ): PlayerCharacter = mapSummon(character, instanceId) { summon ->
        summon.copy(
            overrides = if (value.isNullOrBlank()) summon.overrides - key
            else summon.overrides + (key to value.trim()),
        )
    }

    fun addAction(
        character: PlayerCharacter,
        instanceId: String,
        action: CustomAction,
    ): PlayerCharacter = mapSummon(character, instanceId) { summon ->
        summon.copy(
            // Replacing by name rather than appending, so editing one written action twice
            // leaves one action rather than two with the same heading.
            extraActions = summon.extraActions.filterNot { it.name == action.name } + action,
            removedActions = summon.removedActions - action.name,
        )
    }

    /**
     * Takes an action off this creature.
     *
     * One the player wrote is deleted; one the book gave is hidden, because it comes back the
     * moment the stat block is read again and "remove it from my creature" has to mean
     * something either way. The same distinction the attack list already draws.
     */
    fun removeAction(
        character: PlayerCharacter,
        instanceId: String,
        name: String,
    ): PlayerCharacter = mapSummon(character, instanceId) { summon ->
        summon.copy(
            extraActions = summon.extraActions.filterNot { it.name == name },
            removedActions = summon.removedActions + name,
        )
    }

    /** Puts a creature back the way the book has it. */
    fun resetEdits(character: PlayerCharacter, instanceId: String): PlayerCharacter =
        mapSummon(character, instanceId) {
            it.copy(overrides = emptyMap(), extraActions = emptyList(), removedActions = emptySet())
        }

    // ================================================================ Creatures of one's own

    /** Writes a creature down, replacing any earlier version of it. */
    fun writeCreature(
        character: PlayerCharacter,
        creature: CustomStatblock,
    ): PlayerCharacter = character.copy(
        customStatblocks = character.customStatblocks.filterNot { it.id == creature.id } + creature,
    )

    /**
     * Forgets a creature, and dismisses any of it still on the table.
     *
     * Leaving one out with nothing behind it is how a sheet ends up showing a creature whose
     * rules it cannot state — the case [SheetAudit] complains about as a summon with no stat
     * block, and a real one a player would hit the moment they tidied up their list.
     */
    fun eraseCreature(character: PlayerCharacter, creatureId: String): PlayerCharacter =
        character.copy(
            customStatblocks = character.customStatblocks.filterNot { it.id == creatureId },
            activeSummons = character.activeSummons.filterNot { it.statblockId == creatureId },
        )

    // ================================================================ Plumbing

    private fun mapSummon(
        character: PlayerCharacter,
        instanceId: String,
        transform: (ActiveSummon) -> ActiveSummon,
    ): PlayerCharacter = character.copy(
        activeSummons = character.activeSummons.map {
            if (it.instanceId == instanceId) transform(it) else it
        },
    )

    private fun CustomStatblock.toStatblock() = Statblock(
        id = id,
        name = name,
        size = size,
        creatureType = creatureType,
        armorClass = Formula.Flat(armorClass),
        hitPoints = Formula.Flat(hitPoints),
        speed = speed,
        abilityScores = Ability.ALL.associateWith { (abilityScores[it.name] ?: 10) },
        actions = actions.map { it.toStatblockAction() },
        resistances = resistances.splitList(),
        immunities = immunities.splitList(),
        conditionImmunities = conditionImmunities.splitList(),
        senses = senses,
        languages = languages,
        notes = notes,
    )

    private fun CustomAction.toStatblockAction() = StatblockAction(
        name = name,
        kind = runCatching { ActionKind.valueOf(kind) }.getOrDefault(ActionKind.ACTION),
        description = description,
        // A written creature's numbers are numbers, so a bonus it names is flat rather than
        // read off the summoner — "+7" is the answer the table gave.
        toHit = toHit.trim().removePrefix("+").toIntOrNull()?.let { Formula.Flat(it) },
        damageDice = damageDice,
        damageType = damageType,
        reach = reach,
    )

    private fun String.splitList(): List<String> =
        split(",").map { it.trim() }.filter { it.isNotBlank() }

    /** The book's creature with this instance's changes written over it. */
    private fun Statblock.withEdits(summon: ActiveSummon): Statblock {
        if (summon.overrides.isEmpty() &&
            summon.extraActions.isEmpty() &&
            summon.removedActions.isEmpty()
        ) {
            return this
        }
        val o = summon.overrides
        fun text(key: String, fallback: String) = o[key]?.takeIf { it.isNotBlank() } ?: fallback
        fun list(key: String, fallback: List<String>) =
            o[key]?.let { it.splitList() } ?: fallback

        val edited = actions
            .filterNot { it.name in summon.removedActions }
            .map { action ->
                action.copy(
                    name = text("action:${action.name}:name", action.name),
                    description = text("action:${action.name}:description", action.description),
                    damageDice = text("action:${action.name}:damage", action.damageDice),
                    damageType = text("action:${action.name}:damageType", action.damageType),
                )
            }

        return copy(
            size = text("size", size),
            creatureType = text("creatureType", creatureType),
            armorClass = o["armorClass"]?.trim()?.toIntOrNull()?.let { Formula.Flat(it) }
                ?: armorClass,
            speed = text("speed", speed),
            abilityScores = abilityScores.mapValues { (ability, score) ->
                o["ability:${ability.name}"]?.trim()?.toIntOrNull() ?: score
            },
            actions = edited + summon.extraActions.map { it.toStatblockAction() },
            resistances = list("resistances", resistances),
            vulnerabilities = list("vulnerabilities", vulnerabilities),
            immunities = list("immunities", immunities),
            conditionImmunities = list("conditionImmunities", conditionImmunities),
            senses = text("senses", senses),
            languages = text("languages", languages),
            notes = text("notes", notes),
        )
    }
}
