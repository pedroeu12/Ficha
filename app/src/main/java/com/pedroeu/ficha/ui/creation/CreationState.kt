package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.domain.ChoiceGraph
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.ToolData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.AbilityScoreGeneration
import com.pedroeu.ficha.domain.Owned
import com.pedroeu.ficha.domain.ScoreMethod

/** The two ability spreads a 2024 background may grant across its three listed abilities. */
enum class BonusSpread(val label: String, val values: List<Int>) {
    TWO_ONE("+2 / +1", listOf(2, 1)),
    THREE_ONES("+1 / +1 / +1", listOf(1, 1, 1)),
}

/**
 * Order matters. Books come first: every later step draws its options from the books chosen
 * there, so asking anything before that would offer options the table may not use. Background
 * comes before Class so the proficiencies it grants outright are
 * already known when the class offers its skill list, letting the class step grey out what
 * the character would get anyway. Origin Options stays last of the three because some of its
 * grants depend on choices made inside the class.
 */
enum class CreationStep(private val titleKey: String, private val shortLabelKey: String) {
    SOURCES("Choose Your Books", "Books"),
    SPECIES("Choose a Species", "Species"),
    BACKGROUND("Choose an Origin", "Origin"),
    CLASS("Choose a Class", "Class"),
    CLASS_CHOICES("Class Options", "Options"),
    ORIGIN_CHOICES("Origin Options", "Grants"),
    ABILITIES("Ability Scores", "Abilities"),
    DETAILS("Name & Details", "Details");

    // Translated on read: an enum's constructor runs once, so translating there would pin
    // these to whichever language was in force the first time the class was touched.
    val title: String get() = tr(titleKey)
    val shortLabel: String get() = tr(shortLabelKey)

    companion object {
        val ORDER: List<CreationStep> = entries
    }
}

data class CreationState(
    val step: CreationStep = CreationStep.SOURCES,

    /**
     * The books this character may draw on, chosen in the first step.
     *
     * Every picker in the wizard reads this, and it is written to the finished character so
     * level up keeps offering the same set.
     */
    val enabledSources: Set<Sourcebook> = Sourcebook.CORE,

    val speciesId: String? = null,
    val lineageId: String? = null,
    val speciesSkillChoices: Set<Skill> = emptySet(),

    val classId: String? = null,
    val classSkillChoices: Set<Skill> = emptySet(),
    /** ClassChoice.id -> selected option ids (feature options and spell picks). */
    val classSelections: Map<String, List<String>> = emptyMap(),
    /**
     * Choice.id -> selected option ids for level 1 features off the class table, such as a
     * martial class's Weapon Mastery. These are stored against level 1 on the character, so
     * the sheet and every later level read them the same way.
     */
    val classFeatureSelections: Map<String, List<String>> = emptyMap(),
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

    /** The species the chosen books allow, which is what the Species step lists. */
    val availableSpecies get() = SourceFiltering.available(SpeciesData.ALL, enabledSources)

    /** The classes the chosen books allow. */
    val availableClasses get() = SourceFiltering.available(ClassData.ALL, enabledSources)

    /** The backgrounds the chosen books allow. */
    val availableBackgrounds get() = SourceFiltering.available(BackgroundData.ALL, enabledSources)

    /**
     * Decisions the class table's level 1 features force, beyond the ones [charClass] already
     * models. Weapon Mastery is the case that matters: the class hands it out at level 1 and
     * nothing in the creation flow ever asked which weapons, so a Fighter reached the table
     * with three masteries they had never chosen.
     *
     * Anything the wizard already asks for another way is filtered out: by id for the older
     * [ClassChoice] shape, so a Fighter isn't asked for their Fighting Style twice, and by
     * kind for Expertise, which a Rogue picks through its own dedicated step.
     */
    val classFeatureChoices: List<Choice>
        get() {
            val classId = classId ?: return emptyList()
            val alreadyAsked = charClass?.choices.orEmpty().map { it.id }.toSet()
            val roots = ProgressionData.forClass(classId)
                ?.featuresAt(1)
                ?.flatMap { it.choices }
                ?.filterNot { it.id in alreadyAsked || it.kind == ChoiceKind.EXPERTISE }
                .orEmpty()
            // A level 1 Warlock picks one invocation here; if it is Pact of the Tome, the
            // five spells that pact wants are asked in this same step rather than left for
            // the player to discover unanswered on the finished sheet.
            return ChoiceGraph.expand(roots, classFeatureSelections, enabledSources)
        }

    /**
     * Level 1 Expertise choices off the class table, which the wizard answers through its own
     * picker. Naming them here lets the finished character record the answer against the
     * feature that asked, so the sheet doesn't show a Rogue's Expertise as still unchosen.
     */
    val level1ExpertiseChoiceIds: List<String>
        get() = classId
            ?.let { ProgressionData.forClass(it) }
            ?.featuresAt(1)
            ?.flatMap { it.choices }
            ?.filter { it.kind == ChoiceKind.EXPERTISE }
            ?.map { it.id }
            .orEmpty()

    /** Skills already granted by species or background, which class picks must not duplicate. */
    val grantedSkills: Set<Skill>
        get() = buildSet {
            species?.grantedSkills?.let { addAll(it) }
            addAll(speciesSkillChoices)
            background?.skillProficiencies?.let { addAll(it) }
        }

    /**
     * Every "of your choice" grant still outstanding, derived from the picks made so far.
     *
     * The selections go back in because some of these grants only exist once an earlier one
     * has been answered: choosing Magic Initiate as a Human's Origin feat is what brings its
     * cantrip and spell prompts into being.
     */
    val originChoices: List<Choice>
        get() = ChoiceGraph.expand(
            roots = OriginChoices.all(
                speciesId = speciesId,
                lineageId = lineageId,
                classId = classId,
                classSelections = classSelections,
                backgroundId = backgroundId,
                originSelections = originSelections,
                books = enabledSources,
            ),
            answers = originSelections,
            books = enabledSources,
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

    /** Tools granted outright by the class or the background, before any choice is made. */
    private val grantedTools: Set<String>
        get() = buildSet {
            charClass?.toolProficiencies
                ?.filterNot { it.contains("of your choice", ignoreCase = true) }
                ?.let { addAll(it) }
            background?.toolProficiency
                ?.takeIf { ToolData.optionsForOpenEndedTool(it) == null }
                ?.let { add(it) }
            originChoices
                .filter { it.kind == ChoiceKind.TOOL }
                .flatMap { originSelections[it.id].orEmpty() }
                .let { addAll(it) }
        }

    /** Spells already on the sheet, whether picked from a class list or an origin grant. */
    private val chosenSpellIds: Set<String>
        get() = buildSet {
            charClass?.choices
                ?.filterIsInstance<ClassChoice.CantripChoice>()
                ?.forEach { addAll(classSelections[it.id].orEmpty()) }
            originChoices
                .filter { it.kind == ChoiceKind.SPELL }
                .forEach { addAll(originSelections[it.id].orEmpty()) }
            // Spells nobody chose because the rules simply hand them over: a Tiefling's
            // lineage cantrip, a Drow's Dancing Lights, a feat's free casting. They were
            // missing here, so the Warlock cantrip list went on offering a Tiefling the
            // Thaumaturgy they already had.
            addAll(grantedSpellIds)
        }

    /** Spells the character is given outright by species, lineage, background, or a feat. */
    val grantedSpellIds: Set<String>
        get() = SpellGrantData.grantedSpellIds(
            classId = classId.orEmpty(),
            speciesId = speciesId.orEmpty(),
            lineageId = lineageId,
            featIds = originChoices
                .filter { it.kind == ChoiceKind.FEAT }
                .flatMap { originSelections[it.id].orEmpty() } +
                listOfNotNull(background?.takeIf { it.featChoice == null }?.featId),
            selections = classSelections + originSelections,
            level = 1,
        )

    /**
     * The names of those spells. A class's cantrip list and the spell catalog give the same
     * spell different ids, so names are what actually catch a duplicate across the two.
     */
    val ownedSpellNames: Set<String>
        get() = buildSet {
            grantedSpellIds.forEach { id -> SpellData.byId(id)?.let { add(it.name) } }
            charClass?.choices
                ?.filterIsInstance<ClassChoice.CantripChoice>()
                ?.forEach { choice ->
                    val picked = classSelections[choice.id].orEmpty()
                    choice.options.filter { it.id in picked }.forEach { add(it.name) }
                }
            originChoices
                .filter { it.kind == ChoiceKind.SPELL }
                .forEach { choice ->
                    val picked = originSelections[choice.id].orEmpty()
                    choice.options.filter { it.id in picked }.forEach { add(it.name) }
                }
        }

    /**
     * Everything the half-built character already has, so every picker in the wizard can grey
     * out a duplicate no matter which step granted it first.
     */
    val owned: Owned
        get() = Owned(
            skills = allSkillProficiencies.map { it.name }.toSet(),
            expertise = expertiseChoices.map { it.name }.toSet(),
            tools = grantedTools,
            spells = chosenSpellIds,
            spellNames = ownedSpellNames.map { it.lowercase() }.toSet(),
            languages = originChoices
                .filter { it.kind == ChoiceKind.LANGUAGE }
                .flatMap { originSelections[it.id].orEmpty() }
                .toSet(),
            // Including feats picked through a choice, so a Human offered an Origin feat
            // can't be handed the one their background already gave them.
            feats = setOfNotNull(background?.featId) + originChoices
                .filter { it.kind == ChoiceKind.FEAT }
                .flatMap { originSelections[it.id].orEmpty() },
            options = charClass?.choices
                ?.filterIsInstance<ClassChoice.FeatureOption>()
                ?.flatMap { classSelections[it.id].orEmpty() }
                .orEmpty()
                .toSet(),
        )

    /**
     * Drops class and expertise picks the character now gets for free from another source.
     *
     * The pickers grey out anything already owned, but a player can go back and change their
     * species or background after choosing class skills. Re-running this whenever those
     * change means no duplicate survives, whichever source ended up granting it first.
     */
    fun withoutDuplicateSkills(): CreationState {
        val freeSkills = grantedSkills
        val classPicks = classSkillChoices - freeSkills
        return copy(
            classSkillChoices = classPicks,
            expertiseChoices = expertiseChoices
                .filter { it in classPicks || it in freeSkills }
                .toSet(),
        )
    }

    /** True when every decision on the current step has been made. */
    val canAdvance: Boolean
        get() = when (step) {
            // At least one book, or every later step would have nothing to offer.
            CreationStep.SOURCES -> enabledSources.isNotEmpty()
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
        // Level 1 features off the class table, e.g. which weapons you have mastery with.
        if (classFeatureChoices.any { classFeatureSelections[it.id]?.size != it.count }) {
            return false
        }
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
