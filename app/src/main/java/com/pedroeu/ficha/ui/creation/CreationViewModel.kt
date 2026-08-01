package com.pedroeu.ficha.ui.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.ToolData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.AbilityScoreGeneration
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ScoreMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreationViewModel(private val repository: CharacterRepository) : ViewModel() {

    private val _state = MutableStateFlow(CreationState())
    val state: StateFlow<CreationState> = _state.asStateFlow()

    private val _savedCharacterId = MutableStateFlow<String?>(null)
    val savedCharacterId: StateFlow<String?> = _savedCharacterId.asStateFlow()

    fun selectSpecies(id: String) = _state.update { current ->
        if (current.speciesId == id) current
        else current.copy(speciesId = id, lineageId = null, speciesSkillChoices = emptySet())
            .withoutDuplicateSkills()
    }

    fun selectLineage(id: String) = _state.update { it.copy(lineageId = id) }

    fun toggleSpeciesSkill(skill: Skill) = _state.update { current ->
        val max = current.species?.bonusSkillChoiceCount ?: 0
        val selected = current.speciesSkillChoices
        val next = when {
            selected.contains(skill) -> selected - skill
            selected.size < max -> selected + skill
            // At the limit, replace the oldest pick so tapping always responds.
            else -> selected.drop(1).toSet() + skill
        }
        current.copy(speciesSkillChoices = next).withoutDuplicateSkills()
    }

    fun selectClass(id: String) = _state.update { current ->
        if (current.classId == id) current
        else current.copy(
            classId = id,
            classSkillChoices = emptySet(),
            classSelections = emptyMap(),
            expertiseChoices = emptySet(),
        )
    }

    fun toggleClassSkill(skill: Skill) = _state.update { current ->
        val choice = current.charClass?.choices
            ?.filterIsInstance<ClassChoice.SkillProficiencyChoice>()?.firstOrNull()
            ?: return@update current
        // The chip is disabled for these, but never let one through by another route.
        if (skill in current.grantedSkills) return@update current
        val selected = current.classSkillChoices
        val next = when {
            selected.contains(skill) -> selected - skill
            selected.size < choice.count -> selected + skill
            else -> selected.drop(1).toSet() + skill
        }
        // Dropping a skill must also drop any expertise riding on it.
        current.copy(
            classSkillChoices = next,
            expertiseChoices = current.expertiseChoices.filter { it in next || it in current.grantedSkills }.toSet(),
        )
    }

    fun selectFeatureOption(choiceId: String, optionId: String) = _state.update { current ->
        current.copy(classSelections = current.classSelections + (choiceId to listOf(optionId)))
    }

    fun toggleSpell(choiceId: String, spellId: String, max: Int) = _state.update { current ->
        val selected = current.classSelections[choiceId].orEmpty()
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < max -> selected + spellId
            else -> selected.drop(1) + spellId
        }
        current.copy(classSelections = current.classSelections + (choiceId to next))
    }

    fun toggleExpertise(skill: Skill) = _state.update { current ->
        val selected = current.expertiseChoices
        val next = when {
            selected.contains(skill) -> selected - skill
            selected.size < 2 -> selected + skill
            else -> selected.drop(1).toSet() + skill
        }
        current.copy(expertiseChoices = next)
    }

    fun selectBackground(id: String) = _state.update { current ->
        if (current.backgroundId == id) current
        // Only the outgoing background's own picks are dropped. A feat taken through the
        // species keeps its answers, which used to be swept away alongside.
        val oldFeat = current.background?.featId
        current.copy(
            backgroundId = id,
            backgroundBonuses = emptyMap(),
            originSelections = current.originSelections.filterKeys { key ->
                !key.startsWith("background:") &&
                    (oldFeat == null || !key.startsWith("feat:$oldFeat:"))
            },
        ).withoutDuplicateSkills()
    }

    /**
     * Toggles one option inside an origin choice. At the limit the oldest pick is dropped so
     * tapping always does something visible rather than silently failing.
     */
    fun toggleOriginChoice(choiceId: String, optionId: String, max: Int) = _state.update { current ->
        val selected = current.originSelections[choiceId].orEmpty()
        val next = when {
            selected.contains(optionId) -> selected - optionId
            selected.size < max -> selected + optionId
            max == 1 -> listOf(optionId)
            else -> selected.drop(1) + optionId
        }
        current.copy(originSelections = current.originSelections + (choiceId to next))
    }

    fun setBonusSpread(spread: BonusSpread) = _state.update { current ->
        if (current.bonusSpread == spread) current
        else current.copy(bonusSpread = spread, backgroundBonuses = emptyMap())
    }

    /**
     * Taps an ability to take the next unplaced bonus from the chosen spread; tapping an
     * ability that already holds a bonus returns that bonus to the pool.
     */
    fun toggleBackgroundBonus(ability: Ability) = _state.update { current ->
        val bonuses = current.backgroundBonuses
        if (bonuses.containsKey(ability)) {
            current.copy(backgroundBonuses = bonuses - ability)
        } else {
            val nextValue = current.unassignedBonuses().maxOrNull() ?: return@update current
            current.copy(backgroundBonuses = bonuses + (ability to nextValue))
        }
    }

    fun setScoreMethod(method: ScoreMethod) = _state.update { current ->
        current.copy(
            scoreMethod = method,
            assignedScores = Ability.ALL.associateWith { null },
            directScores = Ability.ALL.associateWith { if (method == ScoreMethod.POINT_BUY) 8 else 10 },
            rolledPool = if (method == ScoreMethod.ROLL) AbilityScoreGeneration.rollScores() else emptyList(),
        )
    }

    fun rerollScores() = _state.update { current ->
        current.copy(
            rolledPool = AbilityScoreGeneration.rollScores(),
            assignedScores = Ability.ALL.associateWith { null },
        )
    }

    fun assignScore(ability: Ability, value: Int?) = _state.update { current ->
        current.copy(assignedScores = current.assignedScores + (ability to value))
    }

    fun setDirectScore(ability: Ability, value: Int) = _state.update { current ->
        val bounded = when (current.scoreMethod) {
            ScoreMethod.POINT_BUY -> value.coerceIn(
                AbilityScoreGeneration.POINT_BUY_MIN,
                AbilityScoreGeneration.POINT_BUY_MAX,
            )
            else -> value.coerceIn(1, 30)
        }
        if (current.scoreMethod == ScoreMethod.POINT_BUY) {
            val candidate = current.directScores + (ability to bounded)
            if (AbilityScoreGeneration.pointsRemaining(candidate.values) < 0) return@update current
            current.copy(directScores = candidate)
        } else {
            current.copy(directScores = current.directScores + (ability to bounded))
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setAlignment(value: String) = _state.update { it.copy(alignment = value) }
    fun setAppearance(value: String) = _state.update { it.copy(appearance = value) }
    fun setBackstory(value: String) = _state.update { it.copy(backstory = value) }

    fun next() = _state.update { current ->
        val index = CreationStep.ORDER.indexOf(current.step)
        if (!current.canAdvance || index == CreationStep.ORDER.lastIndex) current
        else current.copy(step = CreationStep.ORDER[index + 1])
    }

    /** Returns false when already on the first step, so the caller can exit the wizard. */
    fun back(): Boolean {
        val index = CreationStep.ORDER.indexOf(_state.value.step)
        if (index == 0) return false
        _state.update { it.copy(step = CreationStep.ORDER[index - 1]) }
        return true
    }

    fun finish() {
        val current = _state.value
        if (!current.canAdvance) return
        val character = buildCharacter(current)
        viewModelScope.launch {
            repository.save(character)
            _savedCharacterId.value = character.id
        }
    }

    private fun buildCharacter(state: CreationState): PlayerCharacter {
        val charClass = state.charClass
        val background = state.background
        val className = charClass?.name.orEmpty()
        val now = System.currentTimeMillis()

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
            // The background's feat, plus any feat picked through an origin choice — the
            // Human's Versatile trait grants one the same way.
            featIds = (
                listOfNotNull(background?.featId) +
                    state.originChoices
                        .filter { it.kind == ChoiceKind.FEAT }
                        .flatMap { state.originSelections[it.id].orEmpty() }
                ).distinct(),
            knownSpells = spells,
            inventory = buildInventory(state),
            coins = Coins(
                gp = (background?.startingGold ?: 0) +
                    (EquipmentData.STARTING_KITS[state.classId]?.goldPieces ?: 0)
            ),
            alignment = state.alignment,
            appearance = state.appearance,
            backstory = state.backstory,
            createdAt = now,
            updatedAt = now,
        )

        // Start the character at full health, which depends on the assembled scores.
        return character.copy(currentHitPoints = CharacterCalculations.maxHitPoints(character))
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
        state.background?.equipment?.forEach { items += InventoryItem(name = it) }

        return items
    }

    class Factory(private val repository: CharacterRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreationViewModel(repository) as T
    }
}
