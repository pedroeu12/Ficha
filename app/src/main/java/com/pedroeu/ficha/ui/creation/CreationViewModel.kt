package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.CustomFeature
import com.pedroeu.ficha.domain.CustomOption
import com.pedroeu.ficha.domain.CustomOptions
import com.pedroeu.ficha.domain.AbilityScoreGeneration
import com.pedroeu.ficha.domain.ScoreMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreationViewModel(private val repository: CharacterRepository) : ViewModel() {

    private val _state = MutableStateFlow(CreationState())
    val state: StateFlow<CreationState> = _state.asStateFlow()

    private val _savedCharacterId = MutableStateFlow<String?>(null)
    val savedCharacterId: StateFlow<String?> = _savedCharacterId.asStateFlow()

    fun setSources(books: Set<Sourcebook>) = _state.update { it.withSources(books) }

    fun toggleSource(book: Sourcebook) = _state.update { current ->
        val next = if (book in current.enabledSources) {
            current.enabledSources - book
        } else {
            current.enabledSources + book
        }
        current.withSources(next)
    }

    /**
     * Applies a new book set, dropping any choice the new set no longer allows.
     *
     * Turning a book back off after picking from it has to clear that pick, or the character
     * would be finished carrying a species the table does not use — and the step that chose
     * it is already behind the player, so nothing else would ever ask again.
     */
    private fun CreationState.withSources(books: Set<Sourcebook>): CreationState {
        var next = copy(enabledSources = books)
        if (next.species?.book !in books) {
            next = next.copy(speciesId = null, lineageId = null, speciesSkillChoices = emptySet())
        }
        if (next.charClass?.book !in books) {
            next = next.copy(
                classId = null,
                classSkillChoices = emptySet(),
                classSelections = emptyMap(),
                classFeatureSelections = emptyMap(),
                expertiseChoices = emptySet(),
            )
        }
        if (next.background?.book !in books) {
            next = next.copy(
                backgroundId = null,
                backgroundBonuses = emptyMap(),
                originSelections = emptyMap(),
            )
        }
        return next
    }

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
            classFeatureSelections = emptyMap(),
            expertiseChoices = emptySet(),
        )
    }

    /** Toggles an option inside a level 1 class feature choice, e.g. Weapon Mastery. */
    fun toggleClassFeatureChoice(choiceId: String, optionId: String, max: Int) =
        _state.update { current ->
            val selected = current.classFeatureSelections[choiceId].orEmpty()
            val next = when {
                selected.contains(optionId) -> selected - optionId
                selected.size < max -> selected + optionId
                max == 1 -> listOf(optionId)
                else -> selected.drop(1) + optionId
            }
            current.copy(
                classFeatureSelections = current.classFeatureSelections + (choiceId to next)
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

    /**
     * Records an option the player wrote during creation, or rewrote.
     *
     * Every list the wizard shows is rebuilt from this state, so the new option is in the
     * picker underneath as soon as it is saved, and [CharacterBuilder] carries the lot onto
     * the finished character — where the sheet and every later level up read the same list.
     */
    fun writeOwnOption(option: CustomOption) = _state.update { current ->
        current.copy(customOptions = CustomOptions.write(current.customOptions, option))
    }

    /** Takes one back, and unpicks it wherever the wizard had it ticked. */
    fun eraseOwnOption(option: CustomOption) = _state.update { current ->
        current.copy(
            customOptions = current.customOptions.filterNot {
                it.id == option.id && it.choiceId == option.choiceId
            },
            originSelections = current.originSelections.mapValues { (_, ids) -> ids - option.id },
            classFeatureSelections = current.classFeatureSelections
                .mapValues { (_, ids) -> ids - option.id },
            classSelections = current.classSelections.mapValues { (_, ids) -> ids - option.id },
        )
    }

    /**
     * Names one of the three things a character is, and says what it means.
     *
     * The rules still come from the entry underneath — this is the name on the page and the
     * paragraph that explains it, which is the part of a homebrew species a sheet can honestly
     * hold. Written as a feature so it is readable at the table rather than only in the wizard.
     */
    fun writeOwnIdentity(
        key: String,
        featureSource: String,
        name: String,
        description: String,
    ) = _state.update { current ->
        val featureId = "identity:$key"
        current.copy(
            textOverrides = if (name.isBlank()) current.textOverrides - key
            else current.textOverrides + (key to name.trim()),
            customFeatures = current.customFeatures.filterNot { it.id == featureId } +
                listOfNotNull(
                    description.takeIf { it.isNotBlank() }?.let {
                        CustomFeature(
                            id = featureId,
                            name = name.trim().ifBlank { featureSource },
                            description = it.trim(),
                            source = featureSource,
                        )
                    }
                ),
        )
    }

    /** Puts the book's own name back. */
    fun clearOwnIdentity(key: String) = _state.update { current ->
        current.copy(
            textOverrides = current.textOverrides - key,
            customFeatures = current.customFeatures.filterNot { it.id == "identity:$key" },
        )
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
        val character = CharacterBuilder.build(current)
        viewModelScope.launch {
            repository.save(character)
            _savedCharacterId.value = character.id
        }
    }

    class Factory(private val repository: CharacterRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreationViewModel(repository) as T
    }
}
