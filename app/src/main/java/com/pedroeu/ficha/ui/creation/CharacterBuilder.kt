package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.ToolData
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import java.util.UUID

/**
 * The finished character a completed wizard describes.
 *
 * A pure function of the state, so the whole life of a character — made here, levelled,
 * edited, rested — can be walked in a test. What the origin answers grant goes through
 * [ChoiceGrants] at the end, which is what puts a chosen language on the sheet and the
 * cantrips a level 1 Warlock's Pact of the Tome names on the spell list; neither had a route
 * before.
 */
internal object CharacterBuilder {

    fun build(state: CreationState, now: Long = System.currentTimeMillis()): PlayerCharacter {
        val charClass = state.charClass
        val background = state.background
        val className = charClass?.name.orEmpty()

        val classSpells = charClass?.choices
            ?.filterIsInstance<ClassChoice.CantripChoice>()
            ?.flatMap { choice ->
                val picked = state.classSelections[choice.id].orEmpty()
                choice.options.filter { it.id in picked }.map { stub ->
                    KnownSpell(
                        id = stub.id,
                        name = stub.name,
                        level = stub.level,
                        school = stub.school,
                        description = stub.description,
                        source = className,
                    )
                }
            }.orEmpty()

        // Spells picked through an origin choice: Magic Initiate, a High Elf's cantrip,
        // a Thaumaturge's extra cantrip, and so on.
        val originSpells = state.originChoices
            .filter { it.kind == ChoiceKind.SPELL }
            .flatMap { choice ->
                state.originSelections[choice.id].orEmpty().mapNotNull { spellId ->
                    SpellData.byId(spellId)?.let { spell ->
                        KnownSpell(
                            id = spell.id,
                            name = spell.name,
                            level = spell.level,
                            school = spell.school,
                            description = spell.description,
                            source = choice.source,
                        )
                    }
                }
            }

        val spells = (classSpells + originSpells).distinctBy { it.id }

        val originTools = state.originChoices
            .filter { it.kind == ChoiceKind.TOOL }
            .flatMap { state.originSelections[it.id].orEmpty() }

        // A background whose tool entry named a group is replaced by the specific pick.
        val backgroundToolIsOpenEnded = background?.toolProficiency
            ?.let { ToolData.optionsForOpenEndedTool(it) != null } == true

        val toolProficiencies = buildList {
            charClass?.toolProficiencies
                ?.filterNot { it.contains("of your choice", ignoreCase = true) }
                ?.let { addAll(it) }
            if (background != null && !backgroundToolIsOpenEnded) add(background.toolProficiency)
            addAll(originTools)
        }.distinct()

        val character = PlayerCharacter(
            id = UUID.randomUUID().toString(),
            name = state.name.trim(),
            level = 1,
            // Carried onto the character so level up offers the same books creation did.
            enabledSourceIds = state.enabledSources.map { it.id }.toSet(),
            speciesId = state.speciesId.orEmpty(),
            lineageId = state.lineageId,
            classId = state.classId.orEmpty(),
            backgroundId = state.backgroundId.orEmpty(),
            baseAbilityScores = state.resolvedBaseScores().mapKeys { it.key.name },
            backgroundAbilityBonuses = state.backgroundBonuses.mapKeys { it.key.name },
            skillProficiencies = state.allSkillProficiencies.map { it.name }.toSet(),
            skillExpertise = state.expertiseChoices.map { it.name }.toSet(),
            toolProficiencies = toolProficiencies,
            armorTraining = charClass?.armorProficiencies.orEmpty(),
            weaponProficiencies = charClass?.weaponProficiencies.orEmpty(),
            classChoiceSelections = state.classSelections,
            originChoiceSelections = state.originSelections,
            // Anything the player wrote for themselves in the wizard travels with them, so a
            // homebrew invocation picked at level 1 is still in the list at level 5.
            customOptions = state.customOptions,
            // A species, class or origin the player named as their own, plus whatever they
            // wrote about it — the same two places the finished sheet keeps them.
            textOverrides = state.textOverrides,
            customFeatures = state.customFeatures,
            // Level 1 feature picks are keyed by the level that granted them, matching how
            // every later level records its own, so the sheet reads them all the same way.
            // The Rogue's Expertise is answered through its own picker, so it's recorded
            // against the feature that asked rather than left looking unchosen.
            levelSelections = state.classFeatureSelections.mapKeys { (id, _) -> "1:$id" } +
                state.level1ExpertiseChoiceIds.associate { choiceId ->
                    "1:$choiceId" to state.expertiseChoices.map { it.name }
                },
            // The background's feat, plus any feat picked through an origin choice — the
            // Human's Versatile trait grants one the same way. A background that leaves its
            // feat open contributes nothing here: its featId is only a fallback, and adding
            // it as well would hand the character two feats for one grant.
            featIds = (
                listOfNotNull(background?.takeIf { it.featChoice == null }?.featId) +
                    state.originChoices
                        .filter { it.kind == ChoiceKind.FEAT }
                        .flatMap { state.originSelections[it.id].orEmpty() }
                ).distinct(),
            knownSpells = spells,
            inventory = buildInventory(state),
            coins = Coins(
                // Option B replaces the background's whole package — its gear and its few
                // coins — with one sum. The class's own starting kit is untouched: that is a
                // separate grant, and the rules do not trade it away.
                gp = (
                    if (state.takeCoinsInstead) background?.coinsInstead ?: 0
                    else background?.startingGold ?: 0
                    ) + (EquipmentData.STARTING_KITS[state.classId]?.goldPieces ?: 0)
            ),
            alignment = state.alignment,
            appearance = state.appearance,
            backstory = state.backstory,
            createdAt = now,
            updatedAt = now,
        )

        val granted = ChoiceGrants.apply(
            character,
            state.originChoices + state.classFeatureChoices,
            state.originSelections + state.classFeatureSelections,
        )
        // A Wizard starts with six spells in the book and prepares four of them.
        val legal = CharacterSpells.withPreparedWithinLimit(granted)

        // Start the character at full health, which depends on the assembled scores.
        return legal.copy(currentHitPoints = CharacterCalculations.maxHitPoints(legal))
    }

    private fun buildInventory(state: CreationState): List<InventoryItem> {
        val kit = EquipmentData.STARTING_KITS[state.classId]
        val items = mutableListOf<InventoryItem>()

        kit?.armorIds?.groupingBy { it }?.eachCount()?.forEach { (armorId, count) ->
            val armor = EquipmentData.armorById(armorId) ?: return@forEach
            items += InventoryItem(
                name = armor.name,
                quantity = count,
                armorDefId = armor.id,
                equipped = true,
            )
        }
        kit?.weaponIds?.groupingBy { it }?.eachCount()?.forEach { (weaponId, count) ->
            val weapon = EquipmentData.weaponById(weaponId) ?: return@forEach
            items += InventoryItem(
                name = weapon.name,
                quantity = count,
                weaponDefId = weapon.id,
                equipped = true,
            )
        }
        kit?.otherGear?.forEach { items += InventoryItem(name = it) }
        if (!state.takeCoinsInstead) {
            state.background?.equipment?.forEach { items += InventoryItem(name = it) }
        }

        return items
    }
}
