package com.pedroeu.ficha.ui.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.ArtificerItems
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.CustomFeature
import com.pedroeu.ficha.domain.CustomResource
import com.pedroeu.ficha.domain.DeathSaves
import com.pedroeu.ficha.data.model.SpellDef
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.OverridableStat
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.domain.RestOutcome
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SheetViewModel(
    private val repository: CharacterRepository,
    private val characterId: String,
) : ViewModel() {

    private val _character = MutableStateFlow<PlayerCharacter?>(null)
    val character: StateFlow<PlayerCharacter?> = _character.asStateFlow()

    /** When true the sheet swaps calculated displays for editable controls. */
    private val _editMode = MutableStateFlow(false)
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    init {
        viewModelScope.launch {
            _character.value = repository.getById(characterId)
        }
    }

    /** Reloads from storage, so returning from the level-up flow shows the new numbers. */
    fun refresh() {
        viewModelScope.launch {
            _character.value = repository.getById(characterId)
        }
    }

    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }

    private fun update(transform: (PlayerCharacter) -> PlayerCharacter) {
        val current = _character.value ?: return
        val updated = transform(current)
        _character.value = updated
        viewModelScope.launch { repository.save(updated) }
    }

    // ------------------------------------------------------------------ Play tracking

    fun adjustHitPoints(delta: Int) = update { character ->
        val max = CharacterCalculations.maxHitPoints(character)
        if (delta < 0) {
            // Damage burns through temporary hit points first.
            val absorbed = minOf(character.temporaryHitPoints, -delta)
            character.copy(
                temporaryHitPoints = character.temporaryHitPoints - absorbed,
                currentHitPoints = (character.currentHitPoints + delta + absorbed).coerceIn(0, max),
            )
        } else {
            character.copy(currentHitPoints = (character.currentHitPoints + delta).coerceIn(0, max))
        }
    }

    fun setCurrentHitPoints(value: Int) = update { character ->
        character.copy(
            currentHitPoints = value.coerceIn(0, CharacterCalculations.maxHitPoints(character))
        )
    }

    fun setTemporaryHitPoints(value: Int) = update {
        it.copy(temporaryHitPoints = value.coerceAtLeast(0))
    }

    fun setHitDiceSpent(value: Int) = update { character ->
        character.copy(hitDiceSpent = value.coerceIn(0, character.level))
    }

    fun setDeathSaves(successes: Int, failures: Int) = update {
        it.copy(
            deathSaves = DeathSaves(
                successes = successes.coerceIn(0, 3),
                failures = failures.coerceIn(0, 3),
            )
        )
    }

    fun toggleHeroicInspiration() = update {
        it.copy(heroicInspiration = !it.heroicInspiration)
    }

    fun setExperiencePoints(value: Int) = update {
        it.copy(experiencePoints = value.coerceAtLeast(0))
    }

    // ------------------------------------------------------------------ Inventory

    fun toggleEquipped(index: Int) = update { character ->
        val items = character.inventory.toMutableList()
        val item = items.getOrNull(index) ?: return@update character
        items[index] = item.copy(equipped = !item.equipped)
        character.copy(inventory = items)
    }

    fun addInventoryItem(item: InventoryItem) = update { character ->
        if (item.name.isBlank()) return@update character
        character.copy(inventory = character.inventory + item)
    }

    fun addInventoryItem(name: String, quantity: Int) = update { character ->
        if (name.isBlank()) return@update character
        character.copy(
            inventory = character.inventory + InventoryItem(name = name.trim(), quantity = quantity)
        )
    }

    fun updateInventoryItem(index: Int, item: InventoryItem) = update { character ->
        val items = character.inventory.toMutableList()
        if (index !in items.indices) return@update character
        items[index] = item
        character.copy(inventory = items)
    }

    fun setInventoryQuantity(index: Int, quantity: Int) = update { character ->
        val items = character.inventory.toMutableList()
        val item = items.getOrNull(index) ?: return@update character
        if (quantity <= 0) items.removeAt(index) else items[index] = item.copy(quantity = quantity)
        character.copy(inventory = items)
    }

    fun removeInventoryItem(index: Int) = update { character ->
        val items = character.inventory.toMutableList()
        if (index !in items.indices) return@update character
        items.removeAt(index)
        character.copy(inventory = items)
    }

    fun setCoins(coins: Coins) = update { it.copy(coins = coins) }

    // ------------------------------------------------------------------ Replicate Magic Item

    /**
     * Makes one of the Artificer's known plans, which puts the item in the inventory — and
     * from there into Attacks or Armor Class, since it carries the base item's id.
     */
    fun makeArtificerItem(planId: String, baseId: String = "") = update { character ->
        ArtificerItems.make(character, planId, baseId)
    }

    /** Sets an item aside, freeing its place in the day's allowance. */
    fun unmakeArtificerItem(planId: String, baseId: String = "") = update { character ->
        ArtificerItems.unmake(character, planId, baseId)
    }

    // ------------------------------------------------------------------ Spells

    fun setSpellSlotsExpended(level: Int, expended: Int) = update { character ->
        character.copy(
            spellSlotsExpended = character.spellSlotsExpended + (level.toString() to expended.coerceAtLeast(0))
        )
    }

    fun setSpellSlotTotal(level: Int, total: Int) = update { character ->
        character.copy(
            spellSlotOverrides = character.spellSlotOverrides +
                (level.toString() to total.coerceIn(0, 20))
        )
    }

    fun clearSpellSlotOverrides() = update { it.copy(spellSlotOverrides = emptyMap()) }

    fun toggleSpellPrepared(spellId: String) = update { character ->
        character.copy(
            knownSpells = character.knownSpells.map { spell ->
                if (spell.id == spellId) spell.copy(prepared = !spell.prepared) else spell
            }
        )
    }

    /**
     * Prepare or set aside one spell, including one the character has never written down.
     *
     * A prepared caster picks from the whole class list, so most of what it can prepare is
     * not on the sheet yet — preparing is what puts it there, and setting it aside takes it
     * back off. Spells the player added by hand, and spells on a Wizard's spellbook, only
     * have the flag flipped; those are theirs whether prepared or not.
     */
    fun setSpellPrepared(spell: KnownSpell, prepared: Boolean) = update { character ->
        val onSheet = character.knownSpells.any { it.id == spell.id }
        when {
            prepared && !onSheet ->
                character.copy(knownSpells = character.knownSpells + spell.copy(prepared = true))

            !prepared && onSheet && CharacterSpells.isOnPreparedClassList(character, spell.id) ->
                character.copy(knownSpells = character.knownSpells.filterNot { it.id == spell.id })

            else -> character.copy(
                knownSpells = character.knownSpells.map {
                    if (it.id == spell.id) it.copy(prepared = prepared) else it
                }
            )
        }
    }

    /** Trade one cantrip for another, keeping the count exactly where it was. */
    fun swapCantrip(giveUpId: String, take: SpellDef, source: String) = update { character ->
        character.copy(
            knownSpells = character.knownSpells.filterNot { it.id == giveUpId } +
                KnownSpell(
                    id = take.id,
                    name = take.name,
                    level = take.level,
                    school = take.school,
                    description = take.description,
                    source = source,
                )
        )
    }

    fun addSpell(spell: KnownSpell) = update { character ->
        if (character.knownSpells.any { it.id == spell.id }) return@update character
        character.copy(knownSpells = character.knownSpells + spell)
    }

    fun removeSpell(spellId: String) = update { character ->
        character.copy(knownSpells = character.knownSpells.filterNot { it.id == spellId })
    }

    // ------------------------------------------------------------------ Edit Mode

    fun toggleSkillProficiency(skill: Skill) = update { character ->
        val proficient = character.skillProficiencies.contains(skill.name)
        character.copy(
            skillProficiencies = if (proficient) {
                character.skillProficiencies - skill.name
            } else {
                character.skillProficiencies + skill.name
            },
            // Expertise without proficiency is meaningless, so it comes off together.
            skillExpertise = if (proficient) {
                character.skillExpertise - skill.name
            } else {
                character.skillExpertise
            },
        )
    }

    fun toggleSkillExpertise(skill: Skill) = update { character ->
        val expert = character.skillExpertise.contains(skill.name)
        character.copy(
            skillExpertise = if (expert) {
                character.skillExpertise - skill.name
            } else {
                character.skillExpertise + skill.name
            },
            // Granting expertise implies proficiency.
            skillProficiencies = if (expert) {
                character.skillProficiencies
            } else {
                character.skillProficiencies + skill.name
            },
        )
    }

    fun setSkillBonus(skill: Skill, bonus: Int?) = update { character ->
        character.copy(
            skillBonuses = if (bonus == null || bonus == 0) {
                character.skillBonuses - skill.name
            } else {
                character.skillBonuses + (skill.name to bonus)
            }
        )
    }

    fun setSkillOverride(skill: Skill, total: Int?) = update { character ->
        character.copy(
            skillOverrides = if (total == null) {
                character.skillOverrides - skill.name
            } else {
                character.skillOverrides + (skill.name to total)
            }
        )
    }

    fun toggleSaveProficiency(ability: Ability) = update { character ->
        val current = CharacterCalculations.isSavingThrowProficient(character, ability)
        character.copy(
            saveProficiencyOverrides = character.saveProficiencyOverrides + (ability.name to !current)
        )
    }

    fun setSaveBonus(ability: Ability, bonus: Int?) = update { character ->
        character.copy(
            saveBonuses = if (bonus == null || bonus == 0) {
                character.saveBonuses - ability.name
            } else {
                character.saveBonuses + (ability.name to bonus)
            }
        )
    }

    fun setAbilityScore(ability: Ability, score: Int?) = update { character ->
        character.copy(
            abilityScoreOverrides = if (score == null) {
                character.abilityScoreOverrides - ability.name
            } else {
                character.abilityScoreOverrides + (ability.name to score.coerceIn(1, 30))
            }
        )
    }

    fun setAbilityBonus(ability: Ability, bonus: Int?) = update { character ->
        character.copy(
            abilityScoreBonuses = if (bonus == null || bonus == 0) {
                character.abilityScoreBonuses - ability.name
            } else {
                character.abilityScoreBonuses + (ability.name to bonus)
            }
        )
    }

    fun setStatOverride(stat: OverridableStat, value: Int?) = update { character ->
        character.copy(
            statOverrides = if (value == null) {
                character.statOverrides - stat.name
            } else {
                character.statOverrides + (stat.name to value)
            }
        )
    }

    fun setStatBonus(stat: OverridableStat, value: Int?) = update { character ->
        character.copy(
            statBonuses = if (value == null || value == 0) {
                character.statBonuses - stat.name
            } else {
                character.statBonuses + (stat.name to value)
            }
        )
    }

    fun addToolProficiency(name: String) = update { character ->
        if (name.isBlank()) return@update character
        character.copy(
            toolProficiencies = (character.toolProficiencies + name.trim()).distinct()
        )
    }

    fun removeToolProficiency(name: String) = update { character ->
        character.copy(toolProficiencies = character.toolProficiencies - name)
    }

    fun setLevel(level: Int) = update { it.copy(level = level.coerceIn(1, 20)) }

    /** Drops every manual adjustment, returning the sheet to what the rules say. */
    fun clearAllOverrides() = update { character ->
        character.copy(
            abilityScoreOverrides = emptyMap(),
            abilityScoreBonuses = emptyMap(),
            skillBonuses = emptyMap(),
            skillOverrides = emptyMap(),
            saveProficiencyOverrides = emptyMap(),
            saveBonuses = emptyMap(),
            saveOverrides = emptyMap(),
            statOverrides = emptyMap(),
            statBonuses = emptyMap(),
            spellSlotOverrides = emptyMap(),
        )
    }


    // ------------------------------------------------------------------ Resources

    fun setResourceSpent(resourceId: String, spent: Int) = update { character ->
        CharacterResources.withUsesChanged(character, resourceId, spent)
    }

    fun adjustResource(resourceId: String, delta: Int) = update { character ->
        val current = character.resourceUses[resourceId] ?: 0
        CharacterResources.withUsesChanged(character, resourceId, current + delta)
    }

    fun setResourceMax(resourceId: String, max: Int?) = update { character ->
        character.copy(
            resourceMaxOverrides = if (max == null) {
                character.resourceMaxOverrides - resourceId
            } else {
                character.resourceMaxOverrides + (resourceId to max.coerceIn(0, 999))
            }
        )
    }

    /**
     * Records what the character is doing with a feature whose option is chosen at the moment
     * of use — which cannon mode is running, which revelation is up. Not a permanent decision:
     * it can be changed as often as the rules allow, and a rest clears it.
     */
    fun setPerUseChoice(choiceId: String, optionId: String) = update { character ->
        PerUseChoices.choose(character, choiceId, optionId)
    }

    fun clearPerUseChoice(choiceId: String) = update { character ->
        PerUseChoices.clear(character, choiceId)
    }

    /** Spends a use and records what it was spent on, in one step. */
    fun spendResourceOn(resourceId: String, choiceId: String, optionId: String) =
        update { character ->
            val chosen = PerUseChoices.choose(character, choiceId, optionId)
            val spent = chosen.resourceUses[resourceId] ?: 0
            CharacterResources.withUsesChanged(chosen, resourceId, spent + 1)
        }

    fun addCustomResource(name: String, max: Int, recharge: Recharge, notes: String) =
        update { character ->
            if (name.isBlank()) return@update character
            character.copy(
                customResources = character.customResources + CustomResource(
                    id = "custom:${UUID.randomUUID()}",
                    name = name.trim(),
                    max = max.coerceAtLeast(1),
                    recharge = recharge.name,
                    notes = notes.trim(),
                )
            )
        }

    fun removeCustomResource(resourceId: String) = update { character ->
        character.copy(
            customResources = character.customResources.filterNot { it.id == resourceId },
            resourceUses = character.resourceUses - resourceId,
            resourceMaxOverrides = character.resourceMaxOverrides - resourceId,
        )
    }

    // ------------------------------------------------------------------ Attacks

    fun addCustomAttack(attack: CustomAttack) = update { character ->
        if (attack.name.isBlank()) return@update character
        character.copy(customAttacks = character.customAttacks + attack)
    }

    fun updateCustomAttack(attack: CustomAttack) = update { character ->
        character.copy(
            customAttacks = character.customAttacks.map {
                if (it.id == attack.id) attack else it
            }
        )
    }

    fun removeCustomAttack(attackId: String) = update { character ->
        character.copy(customAttacks = character.customAttacks.filterNot { it.id == attackId })
    }

    // ------------------------------------------------------------------ Feats & features

    /** Adds a feat and records any picks it forces, such as Magic Initiate's spells. */
    fun addFeat(featId: String, selections: Map<String, List<String>> = emptyMap()) =
        update { character ->
            if (character.featIds.contains(featId)) return@update character

            val feat = FeatData.byId(featId)
            val learned = feat?.let {
                OriginChoices.forFeat(it.id, it.name)
                    .filter { choice -> choice.kind == ChoiceKind.SPELL }
                    .flatMap { choice ->
                        selections[choice.id].orEmpty().mapNotNull { spellId ->
                            SpellData.byId(spellId)?.let { spell ->
                                KnownSpell(
                                    id = spell.id,
                                    name = spell.name,
                                    level = spell.level,
                                    school = spell.school,
                                    description = spell.description,
                                    source = it.name,
                                )
                            }
                        }
                    }
            }.orEmpty()

            val tools = feat?.let {
                OriginChoices.forFeat(it.id, it.name)
                    .filter { choice -> choice.kind == ChoiceKind.TOOL }
                    .flatMap { choice -> selections[choice.id].orEmpty() }
            }.orEmpty()

            val skills = feat?.let {
                OriginChoices.forFeat(it.id, it.name)
                    .filter { choice -> choice.kind == ChoiceKind.SKILL }
                    .flatMap { choice -> selections[choice.id].orEmpty() }
            }.orEmpty()

            character.copy(
                featIds = character.featIds + featId,
                originChoiceSelections = character.originChoiceSelections + selections,
                knownSpells = (character.knownSpells + learned).distinctBy { it.id },
                toolProficiencies = (character.toolProficiencies + tools).distinct(),
                skillProficiencies = character.skillProficiencies + skills,
            )
        }

    fun removeFeat(featId: String) = update { character ->
        character.copy(featIds = character.featIds - featId)
    }

    fun addCustomFeature(name: String, description: String, source: String) = update { character ->
        if (name.isBlank()) return@update character
        character.copy(
            customFeatures = character.customFeatures + CustomFeature(
                id = "feature:${UUID.randomUUID()}",
                name = name.trim(),
                description = description.trim(),
                source = source.ifBlank { "Custom" },
            )
        )
    }

    fun updateCustomFeature(feature: CustomFeature) = update { character ->
        character.copy(
            customFeatures = character.customFeatures.map {
                if (it.id == feature.id) feature else it
            }
        )
    }

    fun removeCustomFeature(featureId: String) = update { character ->
        character.copy(customFeatures = character.customFeatures.filterNot { it.id == featureId })
    }

    /** Re-records a choice, used by Edit Mode and by rests that let you swap a pick. */
    fun setChoiceSelection(choiceId: String, level: Int, optionIds: List<String>) =
        update { character ->
            if (level > 0) {
                character.copy(
                    levelSelections = character.levelSelections + ("$level:$choiceId" to optionIds)
                )
            } else {
                character.copy(
                    originChoiceSelections =
                        character.originChoiceSelections + (choiceId to optionIds)
                )
            }
        }

    // ------------------------------------------------------------------ Free text

    fun setText(key: String, value: String?) = update { character ->
        character.copy(
            textOverrides = if (value.isNullOrBlank()) {
                character.textOverrides - key
            } else {
                character.textOverrides + (key to value)
            }
        )
    }

    fun setName(value: String) = update { it.copy(name = value.ifBlank { it.name }) }
    fun setAlignment(value: String) = update { it.copy(alignment = value) }

    // ------------------------------------------------------------------ Rests

    fun shortRest(diceRolls: List<Int>): RestOutcome? {
        val current = _character.value ?: return null
        val outcome = RestEngine.shortRest(current, diceRolls)
        _character.value = outcome.character
        viewModelScope.launch { repository.save(outcome.character) }
        return outcome
    }

    fun rollHitDie(): Int {
        val current = _character.value ?: return 1
        return RestEngine.rollHitDie(CharacterCalculations.hitDie(current))
    }

    fun longRestDetailed(): RestOutcome? {
        val current = _character.value ?: return null
        val outcome = RestEngine.longRest(current)
        _character.value = outcome.character
        viewModelScope.launch { repository.save(outcome.character) }
        return outcome
    }

    // ------------------------------------------------------------------ Notes

    fun setNotes(value: String) = update { it.copy(notes = value) }
    fun setAppearance(value: String) = update { it.copy(appearance = value) }
    fun setBackstory(value: String) = update { it.copy(backstory = value) }

    class Factory(
        private val repository: CharacterRepository,
        private val characterId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SheetViewModel(repository, characterId) as T
    }
}
