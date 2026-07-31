package com.pedroeu.ficha.ui.levelup

import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ClassFeature
import com.pedroeu.ficha.data.model.SubclassFeature
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.PlayerCharacter

enum class LevelUpStep(val title: String, val shortLabel: String) {
    /** Which class the level goes into, shown first because everything else depends on it. */
    CLASS("Choose a Class", "Class"),
    HIT_POINTS("Hit Points", "HP"),
    SUBCLASS("Choose a Subclass", "Subclass"),
    FEATURES("New Features", "Features"),
    ASI("Ability Scores or Feat", "Improve"),
    SPELLS("New Spells", "Spells"),
    SUMMARY("Review", "Review"),
}

/** How the player wants to determine the hit points gained this level. */
enum class HitPointMethod(val label: String) {
    AVERAGE("Take the average"),
    ROLL("Roll the die"),
    MANUAL("Enter it yourself"),
}

/** What an Ability Score Improvement level is being spent on. */
enum class AsiMode(val label: String) {
    PLUS_TWO("+2 to one ability"),
    PLUS_ONE_ONE("+1 to two abilities"),
    FEAT("Take a feat instead"),
}

data class LevelUpState(
    val character: PlayerCharacter,
    val step: LevelUpStep = LevelUpStep.CLASS,

    val hitPointMethod: HitPointMethod = HitPointMethod.AVERAGE,
    val rolledHitPoints: Int? = null,
    val manualHitPoints: Int = 1,

    val subclassId: String? = null,

    /** Choice.id -> selected option ids, across class and subclass features gained now. */
    val selections: Map<String, List<String>> = emptyMap(),

    val asiMode: AsiMode = AsiMode.PLUS_TWO,
    /** Ability -> points added this level. */
    val asiPoints: Map<Ability, Int> = emptyMap(),
    val featId: String? = null,

    val newCantrips: List<String> = emptyList(),
    val newSpells: List<String> = emptyList(),
    /** Spells typed in by hand, for levels the catalog doesn't cover. */
    val manualSpells: List<String> = emptyList(),
    /**
     * The class this level goes into. Defaults to the starting class, and is set to something
     * else when the player multiclasses.
     */
    val levellingClassId: String? = null,
) {
    val currentLevel: Int get() = character.level
    val targetLevel: Int get() = character.level + 1

    /** The class gaining this level, which is what every feature lookup keys off. */
    val classId: String get() = levellingClassId ?: character.classId

    /** Levels the character will have in that class once this one is taken. */
    val targetClassLevel: Int get() = ClassLevels.levelIn(character, classId) + 1

    /** True when this level starts a class the character had no levels in. */
    val isNewClass: Boolean get() = ClassLevels.levelIn(character, classId) == 0

    val progression get() = ProgressionData.forClass(classId)

    /** Classes this character could put the level into, with the ones they can't and why. */
    val multiclassOptions get() = Multiclassing.options(character)

    /** The character with its new level applied, used to preview the resulting numbers. */
    val leveledCharacter: PlayerCharacter
        get() = Multiclassing.withLevelIn(character, classId)

    // ---------------------------------------------------------------- Hit points

    /** The hit die of the class being levelled, not of the character's first class. */
    val hitDie: Int get() = ClassData.byId(classId)?.hitDie ?: 8

    val averageHitPoints: Int get() = hitDie / 2 + 1

    val hitPointsGained: Int
        get() = when (hitPointMethod) {
            HitPointMethod.AVERAGE -> averageHitPoints
            HitPointMethod.ROLL -> rolledHitPoints ?: averageHitPoints
            HitPointMethod.MANUAL -> manualHitPoints
        }.coerceAtLeast(1)

    // ---------------------------------------------------------------- Gains this level

    val gainsSubclass: Boolean
        get() = progression?.subclassLevel == targetClassLevel &&
            ClassLevels.subclassIn(character, classId) == null

    val subclassOptions get() = SubclassData.forClass(classId)

    val activeSubclassId: String?
        get() = subclassId ?: ClassLevels.subclassIn(character, classId)

    val newClassFeatures: List<ClassFeature>
        get() = progression?.featuresAt(targetClassLevel).orEmpty()

    val newSubclassFeatures: List<SubclassFeature>
        get() = activeSubclassId?.let {
            SubclassData.byId(it)?.featuresAt(targetClassLevel)
        }.orEmpty()

    /** Every decision the new features force, from both the class and the subclass. */
    val featureChoices: List<Choice>
        get() = newClassFeatures.flatMap { it.choices } + newSubclassFeatures.flatMap { it.choices }

    val grantsAsi: Boolean get() = progression?.grantsAsiAt(targetClassLevel) == true

    val grantsEpicBoon: Boolean get() = progression?.grantsEpicBoonAt(targetLevel) == true

    // ---------------------------------------------------------------- Spellcasting

    private val currentCantripCount: Int get() = character.knownSpells.count { it.level == 0 }
    private val currentSpellCount: Int get() = character.knownSpells.count { it.level > 0 }

    val cantripsToLearn: Int
        get() {
            val progression = progression ?: return 0
            val target = progression.cantripsKnownAt(targetLevel)
            return (target - currentCantripCount).coerceAtLeast(0)
        }

    val spellsToLearn: Int
        get() {
            val progression = progression ?: return 0
            val target = progression.preparedSpellsAt(targetLevel)
            return (target - currentSpellCount).coerceAtLeast(0)
        }

    val maxSpellLevel: Int get() = CharacterCalculations.maxSpellLevel(leveledCharacter)

    private val knownSpellIds: Set<String> get() = character.knownSpells.map { it.id }.toSet()

    val cantripOptions
        get() = SpellData.cantripsForClass(character.classId).filterNot { it.id in knownSpellIds }

    val spellOptions
        get() = SpellData.forClassUpTo(character.classId, maxSpellLevel)
            .filter { it.level > 0 && it.id !in knownSpellIds }

    /**
     * True when the catalog can't satisfy this level on its own, so the player is offered a
     * free-text box. That covers spells above the catalogued range and the case where an
     * Edit Mode override has pushed the prepared count past what the class list holds.
     */
    val needsManualSpellEntry: Boolean
        get() = maxSpellLevel > SpellData.MAX_CATALOGUED_LEVEL ||
            spellOptions.size < spellsToLearn

    // ---------------------------------------------------------------- Step flow

    /** Only the steps that actually apply to this level, in order. */
    val steps: List<LevelUpStep>
        get() = buildList {
            // The class choice only appears once there's a real decision: a character who
            // can't meet any multiclass prerequisite just keeps levelling what they have.
            if (multiclassOptions.any { it.allowed && !it.alreadyHas }) {
                add(LevelUpStep.CLASS)
            }
            add(LevelUpStep.HIT_POINTS)
            if (gainsSubclass) add(LevelUpStep.SUBCLASS)
            if (newClassFeatures.isNotEmpty() || newSubclassFeatures.isNotEmpty()) {
                add(LevelUpStep.FEATURES)
            }
            if (grantsAsi || grantsEpicBoon) add(LevelUpStep.ASI)
            if (cantripsToLearn > 0 || spellsToLearn > 0) add(LevelUpStep.SPELLS)
            add(LevelUpStep.SUMMARY)
        }

    val canAdvance: Boolean
        get() = when (step) {
            LevelUpStep.CLASS -> Multiclassing.canTake(character, classId)
            LevelUpStep.HIT_POINTS -> hitPointsGained >= 1
            LevelUpStep.SUBCLASS -> activeSubclassId != null
            LevelUpStep.FEATURES -> featureChoices.all {
                selections[it.id]?.size == it.count
            }
            LevelUpStep.ASI -> asiValid()
            LevelUpStep.SPELLS -> spellsValid()
            LevelUpStep.SUMMARY -> true
        }

    private fun asiValid(): Boolean {
        if (grantsEpicBoon) return featId != null
        if (!grantsAsi) return true
        return when (asiMode) {
            AsiMode.PLUS_TWO -> asiPoints.values.sum() == 2 && asiPoints.size == 1
            AsiMode.PLUS_ONE_ONE -> asiPoints.values.sum() == 2 && asiPoints.size == 2
            AsiMode.FEAT -> featId != null
        }
    }

    private fun spellsValid(): Boolean {
        val cantripsOk = newCantrips.size == cantripsToLearn
        val totalSpells = newSpells.size + manualSpells.size
        // Manual entry covers levels the catalog lacks, so accept at least the required count.
        val spellsOk = if (needsManualSpellEntry) {
            totalSpells >= spellsToLearn || spellOptions.isEmpty()
        } else {
            totalSpells == spellsToLearn
        }
        return cantripsOk && spellsOk
    }

    val featOptions
        get() = if (grantsEpicBoon) {
            FeatData.EPIC_BOONS
        } else {
            FeatData.GENERAL_FEATS.filterNot { it.id == "ability_score_improvement" }
        }
}
