package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.AbilityScoreGeneration
import com.pedroeu.ficha.domain.ScoreMethod

/** The two ability spreads a 2024 background may grant across its three listed abilities. */
enum class BonusSpread(val label: String, val values: List<Int>) {
    TWO_ONE("+2 / +1", listOf(2, 1)),
    THREE_ONES("+1 / +1 / +1", listOf(1, 1, 1)),
}

enum class CreationStep(val title: String, val shortLabel: String) {
    SPECIES("Choose a Species", "Species"),
    CLASS("Choose a Class", "Class"),
    CLASS_CHOICES("Class Options", "Options"),
    BACKGROUND("Choose an Origin", "Origin"),
    ORIGIN_CHOICES("Origin Options", "Grants"),
    ABILITIES("Ability Scores", "Abilities"),
    DETAILS("Name & Details", "Details");

    companion object {
        val ORDER: List<CreationStep> = entries
    }
}

data class CreationState(
    val step: CreationStep = CreationStep.SPECIES,

    val speciesId: String? = null,
    val lineageId: String? = null,
    val speciesSkillChoices: Set<Skill> = emptySet(),

    val classId: String? = null,
    val classSkillChoices: Set<Skill> = emptySet(),
    /** ClassChoice.id -> selected option ids (feature options and spell picks). */
    val classSelections: Map<String, List<String>> = emptyMap(),
    val expertiseChoices: Set<Skill> = emptySet(),

    val backgroundId: String? = null,
    val bonusSpread: BonusSpread = BonusSpread.TWO_ONE,
    /** Ability -> +2 or +1 granted by the background. */
    val backgroundBonuses: Map<Ability, Int> = emptyMap(),

    /** Choice.id -> selected option ids for every "of your choice" grant. */
    val originSelections: Map<String, List<String>> = emptyMap(),

    val scoreMethod: ScoreMethod = ScoreMethod.STANDARD_ARRAY,
    /** Ability -> assigned score for array/roll methods; null means unassigned. */
    val assignedScores: Map<Ability, Int?> = Ability.ALL.associateWith { null },
    /** Ability -> score for point buy and manual entry. */
    val directScores: Map<Ability, Int> = Ability.ALL.associateWith { 8 },
    val rolledPool: List<Int> = emptyList(),

    val name: String = "",
    val alignment: String = "",
    val appearance: String = "",
    val backstory: String = "",
) {
    val species get() = speciesId?.let { SpeciesData.byId(it) }
    val charClass get() = classId?.let { ClassData.byId(it) }
    val background get() = backgroundId?.let { BackgroundData.byId(it) }

    /** Skills already granted by species or background, which class picks must not duplicate. */
    val grantedSkills: Set<Skill>
        get() = buildSet {
            species?.grantedSkills?.let { addAll(it) }
            addAll(speciesSkillChoices)
            background?.skillProficiencies?.let { addAll(it) }
        }

    /** Every "of your choice" grant still outstanding, derived from the picks made so far. */
    val originChoices: List<Choice>
        get() = OriginChoices.all(
            speciesId = speciesId,
            lineageId = lineageId,
            classId = classId,
            classSelections = classSelections,
            backgroundId = backgroundId,
        )

    /** Skills picked through an origin choice, such as the Skilled feat. */
    private val originSkillChoices: Set<Skill>
        get() = originChoices
            .filter { it.kind == ChoiceKind.SKILL }
            .flatMap { originSelections[it.id].orEmpty() }
            .mapNotNull { id -> Skill.ALL.find { it.name == id } }
            .toSet()

    val allSkillProficiencies: Set<Skill>
        get() = grantedSkills + classSkillChoices + originSkillChoices

    /** True when every decision on the current step has been made. */
    val canAdvance: Boolean
        get() = when (step) {
            CreationStep.SPECIES -> speciesValid()
            CreationStep.CLASS -> classId != null
            CreationStep.CLASS_CHOICES -> classChoicesValid()
            CreationStep.BACKGROUND -> backgroundValid()
            CreationStep.ORIGIN_CHOICES -> originChoicesValid()
            CreationStep.ABILITIES -> abilitiesValid()
            CreationStep.DETAILS -> name.isNotBlank()
        }

    private fun originChoicesValid(): Boolean =
        originChoices.all { choice -> originSelections[choice.id]?.size == choice.count }

    private fun speciesValid(): Boolean {
        val s = species ?: return false
        if (s.lineageOptions.isNotEmpty() && lineageId == null) return false
        return speciesSkillChoices.size == s.bonusSkillChoiceCount
    }

    private fun classChoicesValid(): Boolean {
        val c = charClass ?: return false
        for (choice in c.choices) {
            when (choice) {
                is ClassChoice.SkillProficiencyChoice ->
                    if (classSkillChoices.size != choice.count) return false
                is ClassChoice.FeatureOption ->
                    if (classSelections[choice.id]?.size != 1) return false
                is ClassChoice.CantripChoice ->
                    if (classSelections[choice.id]?.size != choice.count) return false
            }
        }
        if (c.id == "rogue" && expertiseChoices.size != 2) return false
        return true
    }

    private fun backgroundValid(): Boolean {
        background ?: return false
        return backgroundBonuses.values.sorted() == bonusSpread.values.sorted()
    }

    /** Bonus values from the chosen spread that have not yet been placed on an ability. */
    fun unassignedBonuses(): List<Int> {
        val used = backgroundBonuses.values.toMutableList()
        return bonusSpread.values.filter { value -> !used.remove(value) }
    }

    private fun abilitiesValid(): Boolean = when (scoreMethod) {
        ScoreMethod.STANDARD_ARRAY, ScoreMethod.ROLL ->
            assignedScores.values.none { it == null }
        ScoreMethod.POINT_BUY ->
            AbilityScoreGeneration.pointsRemaining(directScores.values) >= 0
        ScoreMethod.MANUAL ->
            directScores.values.all { it in 1..30 }
    }

    /** The base scores to store, resolved from whichever generation method is active. */
    fun resolvedBaseScores(): Map<Ability, Int> = when (scoreMethod) {
        ScoreMethod.STANDARD_ARRAY, ScoreMethod.ROLL ->
            Ability.ALL.associateWith { assignedScores[it] ?: 10 }
        ScoreMethod.POINT_BUY, ScoreMethod.MANUAL ->
            Ability.ALL.associateWith { directScores[it] ?: 10 }
    }

    /** Final scores including background bonuses, for live preview during creation. */
    fun previewFinalScores(): Map<Ability, Int> {
        val base = resolvedBaseScores()
        return Ability.ALL.associateWith { (base[it] ?: 10) + (backgroundBonuses[it] ?: 0) }
    }

    /** The pool of scores still available to assign for array/roll methods. */
    fun availablePool(): List<Int> {
        val pool = when (scoreMethod) {
            ScoreMethod.STANDARD_ARRAY -> AbilityScoreGeneration.STANDARD_ARRAY
            ScoreMethod.ROLL -> rolledPool
            else -> return emptyList()
        }
        val used = assignedScores.values.filterNotNull().toMutableList()
        return pool.filter { value -> !used.remove(value) }
    }
}
