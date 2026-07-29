package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.ResourceDef

/** A resource paired with how much of it the character has spent. */
data class ResourceState(
    val def: ResourceDef,
    val spent: Int,
) {
    val remaining: Int get() = (def.max - spent).coerceIn(0, def.max)
    val isDepleted: Boolean get() = remaining == 0
}

/**
 * Gathers every limited-use pool the character has: the ones the rules derive from their
 * class, subclass, species, and feats, plus anything they added by hand. Maxima can be
 * overridden in Edit Mode, which is what makes this safe for rules the engine doesn't model.
 */
object CharacterResources {

    fun definitions(character: PlayerCharacter): List<ResourceDef> {
        val derived = ResourceData.forContext(
            ResourceData.Context(
                classId = character.classId,
                subclassId = character.subclassId,
                speciesId = character.speciesId,
                lineageId = character.lineageId,
                featIds = character.featIds,
                level = character.level,
                proficiencyBonus = CharacterCalculations.proficiencyBonus(character),
                abilityModifiers = CharacterCalculations.abilityModifiers(character),
            )
        )

        val custom = character.customResources.map { resource ->
            ResourceDef(
                id = resource.id,
                name = resource.name,
                max = resource.max,
                recharge = runCatching { Recharge.valueOf(resource.recharge) }
                    .getOrDefault(Recharge.LONG_REST),
                source = "Custom",
                notes = resource.notes,
                isCustom = true,
            )
        }

        // An override replaces the derived maximum; a max of 0 hides the row entirely.
        return (derived + custom)
            .map { def ->
                val override = character.resourceMaxOverrides[def.id]
                if (override == null) def else def.copy(max = override)
            }
            .filter { it.max > 0 }
    }

    fun states(character: PlayerCharacter): List<ResourceState> =
        definitions(character).map { def ->
            ResourceState(def, (character.resourceUses[def.id] ?: 0).coerceIn(0, def.max))
        }

    fun byRecharge(character: PlayerCharacter, recharge: Recharge): List<ResourceState> =
        states(character).filter { it.def.recharge == recharge }

    /** Spends or restores uses, clamped to the pool. */
    fun withUsesChanged(
        character: PlayerCharacter,
        resourceId: String,
        spent: Int,
    ): PlayerCharacter {
        val def = definitions(character).find { it.id == resourceId } ?: return character
        return character.copy(
            resourceUses = character.resourceUses + (resourceId to spent.coerceIn(0, def.max))
        )
    }

    /** Refills everything a rest of the given kind brings back. */
    fun withRestored(character: PlayerCharacter, restKind: Recharge): PlayerCharacter {
        val refilled = definitions(character)
            .filter { it.recharge.refilledBy(restKind) }
            .map { it.id }
            .toSet()
        return character.copy(
            resourceUses = character.resourceUses.filterKeys { it !in refilled }
        )
    }
}
