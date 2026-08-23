package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.FeatureText
import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.content.ResourceOptionData
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.ResourceDef
import com.pedroeu.ficha.data.model.ResourceOption

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
        // Each class's pools scale on the level in that class, so a Fighter 5 / Cleric 3 has
        // a level 5 Fighter's Second Wind and a level 3 Cleric's Channel Divinity. Species
        // and feat pools are gathered once, from the first class, to avoid duplicates.
        val classes = ClassLevels.of(character)
        val derived = classes.flatMapIndexed { index, entry ->
            ResourceData.forContext(
                ResourceData.Context(
                    classId = entry.classId,
                    subclassId = entry.subclassId,
                    // Only the first pass carries the species and feats.
                    speciesId = if (index == 0) character.speciesId else "",
                    lineageId = if (index == 0) character.lineageId else null,
                    featIds = if (index == 0) character.featIds else emptyList(),
                    level = entry.level,
                    proficiencyBonus = CharacterCalculations.proficiencyBonus(character),
                    abilityModifiers = CharacterCalculations.abilityModifiers(character),
                    characterLevel = character.level,
                )
            )
        }.distinctBy { it.id }

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
            .map { def ->
                def.copy(
                    options = optionsFor(character, def.id),
                    // Custom pools are the player's own words; everything else borrows the
                    // rules text from the feature that granted it.
                    description = if (def.isCustom) def.description
                    else def.description.ifBlank { FeatureText.forName(def.name) },
                )
            }
    }

    /**
     * The named abilities a pool pays for: the ones the rules grant outright, plus whatever
     * the player picked from a choice that spends this pool (Metamagic, maneuvers, Arcane
     * Shot). Both carry their own rules text so the sheet can explain each one.
     */
    fun optionsFor(character: PlayerCharacter, resourceId: String): List<ResourceOption> {
        // Every subclass the character holds, at the level they hold it in — a multiclassed
        // Cleric/Paladin has two Channel Divinity pools and each one's options come from its
        // own subclass, not from whichever subclass the plain field happens to name.
        val granted = ClassLevels.of(character)
            .flatMap { entry ->
                ResourceOptionData.forResource(
                    resourceId = resourceId,
                    subclassId = entry.subclassId,
                    level = entry.level,
                )
            }
            .ifEmpty {
                ResourceOptionData.forResource(
                    resourceId = resourceId,
                    subclassId = character.subclassId,
                    level = character.level,
                )
            }
            .distinctBy { it.id }

        val chosen = ChoiceResolver.all(character)
            .filter { it.choice.resourceId == resourceId }
            .flatMap { resolved ->
                resolved.selectedIds.mapNotNull { optionId ->
                    resolved.choice.options.find { it.id == optionId }?.let { option ->
                        ResourceOption(
                            id = "${resolved.choice.id}:${option.id}",
                            name = option.name,
                            cost = option.supporting,
                            actionType = "",
                            description = option.description,
                            unlockLevel = resolved.level,
                            isChosen = true,
                        )
                    }
                }
            }
            .distinctBy { it.name }

        return granted + chosen
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
