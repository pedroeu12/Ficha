package com.pedroeu.ficha.ui.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SpellDef
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class LevelUpViewModel(
    private val repository: CharacterRepository,
    private val characterId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<LevelUpState?>(null)
    val state: StateFlow<LevelUpState?> = _state.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getById(characterId)?.let { character ->
                _state.value = LevelUpState(character = character)
            }
        }
    }

    private fun edit(transform: (LevelUpState) -> LevelUpState) {
        _state.update { it?.let(transform) }
    }

    // ------------------------------------------------------------------ Hit points

    fun setHitPointMethod(method: HitPointMethod) = edit { current ->
        current.copy(
            hitPointMethod = method,
            manualHitPoints = if (method == HitPointMethod.MANUAL) {
                current.manualHitPoints.coerceAtLeast(1)
            } else {
                current.manualHitPoints
            },
        )
    }

    fun rollHitPoints() = edit { current ->
        current.copy(rolledHitPoints = Random.nextInt(1, current.hitDie + 1))
    }

    fun setManualHitPoints(value: Int) = edit { current ->
        current.copy(manualHitPoints = value.coerceIn(1, current.hitDie))
    }

    // ------------------------------------------------------------------ Choices

    fun selectSubclass(id: String) = edit { it.copy(subclassId = id, selections = emptyMap()) }

    fun toggleSelection(choiceId: String, optionId: String, max: Int) = edit { current ->
        val selected = current.selections[choiceId].orEmpty()
        val next = when {
            selected.contains(optionId) -> selected - optionId
            selected.size < max -> selected + optionId
            max == 1 -> listOf(optionId)
            else -> selected.drop(1) + optionId
        }
        current.copy(selections = current.selections + (choiceId to next))
    }

    // ------------------------------------------------------------------ ASI and feats

    fun setAsiMode(mode: AsiMode) = edit {
        it.copy(asiMode = mode, asiPoints = emptyMap(), featId = null)
    }

    /**
     * Adds a point to an ability, respecting the shape of the chosen mode. Tapping an ability
     * that already holds points clears it, so a mistaken pick is easy to undo.
     */
    fun toggleAsiAbility(ability: Ability) = edit { current ->
        val points = current.asiPoints
        if (points.containsKey(ability)) return@edit current.copy(asiPoints = points - ability)

        val next = when (current.asiMode) {
            AsiMode.PLUS_TWO -> mapOf(ability to 2)
            AsiMode.PLUS_ONE_ONE ->
                if (points.size >= 2) points else points + (ability to 1)
            AsiMode.FEAT -> points
        }
        current.copy(asiPoints = next)
    }

    fun selectFeat(id: String) = edit { it.copy(featId = id) }

    // ------------------------------------------------------------------ Spells

    fun toggleCantrip(spellId: String) = edit { current ->
        val selected = current.newCantrips
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < current.cantripsToLearn -> selected + spellId
            current.cantripsToLearn == 1 -> listOf(spellId)
            else -> selected.drop(1) + spellId
        }
        current.copy(newCantrips = next)
    }

    fun toggleSpell(spellId: String) = edit { current ->
        val selected = current.newSpells
        val limit = (current.spellsToLearn - current.manualSpells.size).coerceAtLeast(0)
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < limit -> selected + spellId
            limit == 1 -> listOf(spellId)
            limit == 0 -> selected
            else -> selected.drop(1) + spellId
        }
        current.copy(newSpells = next)
    }

    fun addManualSpell(name: String) = edit { current ->
        if (name.isBlank()) current
        else current.copy(manualSpells = current.manualSpells + name.trim())
    }

    fun removeManualSpell(name: String) = edit { current ->
        current.copy(manualSpells = current.manualSpells - name)
    }

    // ------------------------------------------------------------------ Navigation

    fun next() = edit { current ->
        val steps = current.steps
        val index = steps.indexOf(current.step)
        if (!current.canAdvance || index == steps.lastIndex) current
        else current.copy(step = steps[index + 1])
    }

    /** Returns false when already on the first step, so the caller can leave the flow. */
    fun back(): Boolean {
        val current = _state.value ?: return false
        val steps = current.steps
        val index = steps.indexOf(current.step)
        if (index <= 0) return false
        _state.value = current.copy(step = steps[index - 1])
        return true
    }

    // ------------------------------------------------------------------ Commit

    fun confirm() {
        val current = _state.value ?: return
        val updated = applyLevelUp(current)
        viewModelScope.launch {
            repository.save(updated)
            _finished.value = true
        }
    }

    private fun applyLevelUp(state: LevelUpState): PlayerCharacter {
        val character = state.character
        val allChoices = state.featureChoices

        // Selections are typed by their choice kind, so each lands in the right place.
        val newSkills = mutableSetOf<String>()
        val newExpertise = mutableSetOf<String>()
        val newTools = mutableListOf<String>()
        val choiceSelections = mutableMapOf<String, List<String>>()

        allChoices.forEach { choice ->
            val picked = state.selections[choice.id].orEmpty()
            if (picked.isEmpty()) return@forEach
            choiceSelections["${state.targetLevel}:${choice.id}"] = picked

            when (choice.kind) {
                ChoiceKind.SKILL -> newSkills += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.EXPERTISE -> newExpertise += picked.filter { id ->
                    Skill.ALL.any { it.name == id }
                }
                ChoiceKind.TOOL -> newTools += picked
                else -> Unit
            }
        }

        // A subclass feature can grant Expertise in a skill the character isn't proficient
        // with yet; taking the proficiency alongside it keeps the sheet consistent.
        newSkills += newExpertise

        val learnedSpells = buildList {
            state.newCantrips.forEach { id ->
                SpellData.byId(id)?.let { add(it.toKnownSpell(state.className())) }
            }
            state.newSpells.forEach { id ->
                SpellData.byId(id)?.let { add(it.toKnownSpell(state.className())) }
            }
            state.manualSpells.forEach { name ->
                add(
                    KnownSpell(
                        id = "manual:${name.lowercase().replace(' ', '_')}",
                        name = name,
                        level = state.maxSpellLevel,
                        school = "",
                        description = "Added manually at level ${state.targetLevel}.",
                        source = state.className(),
                    )
                )
            }
        }

        val abilityImprovements = character.abilityScoreImprovements.toMutableMap()
        state.asiPoints.forEach { (ability, points) ->
            abilityImprovements[ability.name] = (abilityImprovements[ability.name] ?: 0) + points
        }

        val leveled = character.copy(
            level = state.targetLevel,
            subclassId = state.activeSubclassId,
            hitPointsPerLevel = character.hitPointsPerLevel + state.hitPointsGained,
            skillProficiencies = character.skillProficiencies + newSkills,
            skillExpertise = character.skillExpertise + newExpertise,
            toolProficiencies = (character.toolProficiencies + newTools).distinct(),
            abilityScoreImprovements = abilityImprovements,
            featIds = character.featIds + listOfNotNull(state.featId),
            knownSpells = (character.knownSpells + learnedSpells).distinctBy { it.id },
            levelSelections = character.levelSelections + choiceSelections,
        )

        // Gaining a level raises max HP; current hit points rise by the same amount so the
        // character isn't suddenly wounded by levelling up.
        val hpGain = CharacterCalculations.maxHitPoints(leveled) -
            CharacterCalculations.maxHitPoints(character)
        return leveled.copy(
            currentHitPoints = (leveled.currentHitPoints + hpGain.coerceAtLeast(0))
                .coerceAtMost(CharacterCalculations.maxHitPoints(leveled))
        )
    }

    private fun LevelUpState.className(): String =
        ClassData.byId(character.classId)?.name
            ?: character.classId.replaceFirstChar { it.uppercase() }

    private fun SpellDef.toKnownSpell(source: String) = KnownSpell(
        id = id,
        name = name,
        level = level,
        school = school,
        description = description,
        source = source,
    )

    class Factory(
        private val repository: CharacterRepository,
        private val characterId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LevelUpViewModel(repository, characterId) as T
    }
}
