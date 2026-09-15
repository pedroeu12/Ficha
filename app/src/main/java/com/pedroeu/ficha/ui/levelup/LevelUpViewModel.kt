package com.pedroeu.ficha.ui.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.CustomOption
import com.pedroeu.ficha.domain.CustomOptions
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
                _state.value = LevelUpState(character = character).withExistingAnswers()
            }
        }
    }

    /**
     * Pre-fills any choice this level asks again under the same id.
     *
     * Weapon Mastery is asked at every level the count grows, and each asking restates the
     * whole list rather than adding one — so a Fighter reaching level 4 should see their three
     * existing weapons already ticked and add a fourth, not start from nothing. Choices that
     * genuinely accumulate (a Battle Master's maneuvers) use a different id per level and are
     * untouched by this.
     */
    private fun LevelUpState.withExistingAnswers(): LevelUpState {
        val seeded = featureChoices.mapNotNull { choice ->
            // The latest answer, not every level's — a choice that restates its whole list
            // should show what is currently held, and unioning would show what was ever held.
            ChoiceResolver.latestSelectionFor(character, choice.id)
                .takeIf { it.isNotEmpty() }
                ?.take(choice.count)
                ?.let { choice.id to it }
        }
        return if (seeded.isEmpty()) this else copy(selections = selections + seeded)
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

    /**
     * Puts this level into a different class. Everything downstream — hit die, features,
     * subclass timing — reads from it, so the picks made so far are cleared.
     */
    fun selectLevellingClass(classId: String) = edit { current ->
        if (current.classId == classId) {
            current
        } else {
            current.copy(
                levellingClassId = classId,
                subclassId = null,
                selections = emptyMap(),
                newCantrips = emptyList(),
                newSpells = emptyList(),
                manualSpells = emptyList(),
                replacedSpellId = null,
                replacedCantripId = null,
                rolledHitPoints = null,
            )
        }
    }

    fun selectSubclass(id: String) = edit { it.copy(subclassId = id, selections = emptyMap()) }

    fun toggleSelection(choiceId: String, optionId: String, max: Int) = edit { current ->
        val selected = current.selections[choiceId].orEmpty()
        val next = ChoiceGrants.nextSelection(selected, optionId, max)
        current.copy(selections = current.selections + (choiceId to next))
    }

    /**
     * Writes an option of the player's own into the character being levelled.
     *
     * Onto the character rather than beside it, because the level-up flow reads its lists
     * from the character — so the new option appears in the list underneath the moment it is
     * saved, and is still there on the finished sheet without anything having to carry it
     * across.
     */
    fun writeOwnOption(option: CustomOption) = edit { current ->
        current.copy(character = CustomOptions.write(current.character, option))
    }

    fun eraseOwnOption(option: CustomOption) = edit { current ->
        current.copy(
            character = CustomOptions.erase(current.character, option),
            // An option that no longer exists cannot stay ticked in this level's answers.
            selections = current.selections.mapValues { (_, ids) -> ids - option.id },
        )
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

    /** Changing the feat drops whatever was picked inside the old one. */
    fun selectFeat(id: String) = edit { current ->
        if (current.featId == id) return@edit current
        val staleChoiceIds = current.featChoices.map { it.id }.toSet()
        current.copy(
            featId = id,
            selections = current.selections.filterKeys { it !in staleChoiceIds },
        )
    }

    // ------------------------------------------------------------------ Spells

    fun toggleCantrip(spellId: String) = edit { current ->
        val selected = current.newCantrips
        val wanted = current.cantripsWanted
        val next = when {
            selected.contains(spellId) -> selected - spellId
            selected.size < wanted -> selected + spellId
            wanted == 1 -> listOf(spellId)
            wanted == 0 -> selected
            else -> selected.drop(1) + spellId
        }
        current.copy(newCantrips = next)
    }

    /**
     * Choose, or unchoose, the one spell traded away this level.
     *
     * Dropping the replacement also drops whatever was picked to fill its place, because the
     * pick only existed on account of the trade — leaving it behind would quietly hand the
     * character a spell it hadn't earned.
     */
    fun toggleReplacedSpell(spellId: String) = edit { current ->
        if (current.replacedSpellId == spellId) {
            current.copy(replacedSpellId = null, newSpells = current.newSpells.dropLast(1))
        } else {
            current.copy(replacedSpellId = spellId)
        }
    }

    fun toggleReplacedCantrip(spellId: String) = edit { current ->
        if (current.replacedCantripId == spellId) {
            current.copy(replacedCantripId = null, newCantrips = current.newCantrips.dropLast(1))
        } else {
            current.copy(replacedCantripId = spellId)
        }
    }

    fun toggleSpell(spellId: String) = edit { current ->
        val selected = current.newSpells
        val limit = (current.spellsWanted - current.manualSpells.size).coerceAtLeast(0)
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
        else current.copy(requestedStep = steps[index + 1])
    }

    /** Returns false when already on the first step, so the caller can leave the flow. */
    fun back(): Boolean {
        val current = _state.value ?: return false
        val steps = current.steps
        val index = steps.indexOf(current.step)
        if (index <= 0) return false
        _state.value = current.copy(requestedStep = steps[index - 1])
        return true
    }

    // ------------------------------------------------------------------ Commit

    fun confirm() {
        val current = _state.value ?: return
        val updated = LevelUpApplier.apply(current)
        viewModelScope.launch {
            repository.save(updated)
            _finished.value = true
        }
    }

    class Factory(
        private val repository: CharacterRepository,
        private val characterId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LevelUpViewModel(repository, characterId) as T
    }
}
