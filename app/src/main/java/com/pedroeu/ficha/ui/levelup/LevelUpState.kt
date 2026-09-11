package com.pedroeu.ficha.ui.levelup

import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.model.SourceFiltering
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ClassFeature
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.data.model.Subclass
import com.pedroeu.ficha.data.model.SubclassFeature
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.FeatPrerequisites
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGraph
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.PlayerCharacter

enum class LevelUpStep(private val titleKey: String, private val shortLabelKey: String) {
    /** Which class the level goes into, shown first because everything else depends on it. */
    CLASS("Choose a Class", "Class"),
    HIT_POINTS("Hit Points", "HP"),
    SUBCLASS("Choose a Subclass", "Subclass"),
    FEATURES("New Features", "Features"),
    ASI("Ability Scores or Feat", "Improve"),
    /** What the feat taken at [ASI] leaves to decide — a spell, a skill, a damage type. */
    FEAT_CHOICES("Feat Options", "Feat"),
    SPELLS("New Spells", "Spells"),
    SUMMARY("Review", "Review");

    // Translated on read rather than in the constructor, which runs only once.
    val title: String get() = tr(titleKey)
    val shortLabel: String get() = tr(shortLabelKey)
}

/** How the player wants to determine the hit points gained this level. */
enum class HitPointMethod(private val labelKey: String) {
    AVERAGE("Take the average"),
    ROLL("Roll the die"),
    MANUAL("Enter it yourself");

    val label: String get() = tr(labelKey)
}

/** What an Ability Score Improvement level is being spent on. */
enum class AsiMode(private val labelKey: String) {
    PLUS_TWO("+2 to one ability"),
    PLUS_ONE_ONE("+1 to two abilities"),
    FEAT("Take a feat instead");

    val label: String get() = tr(labelKey)
}

data class LevelUpState(
    val character: PlayerCharacter,
    /**
     * The step the flow has navigated to. Read [step] instead: a character with nothing to
     * multiclass into never sees the class picker, and landing on a step that isn't part of
     * this level's flow left the player looking at a screen whose Next button could never
     * light up.
     */
    val requestedStep: LevelUpStep = LevelUpStep.CLASS,

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
     * The one spell and the one cantrip being traded away this level, if any.
     *
     * Both are optional — the rules grant the swap, they don't require it — so neither ever
     * blocks the step from advancing.
     */
    val replacedSpellId: String? = null,
    val replacedCantripId: String? = null,
    /**
     * The class this level goes into. Defaults to the starting class, and is set to something
     * else when the player multiclasses.
     */
    val levellingClassId: String? = null,
) {
    /** The step actually being shown, clamped to the ones this level calls for. */
    val step: LevelUpStep get() = if (requestedStep in steps) requestedStep else steps.first()

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

    /** Subclasses for this class that the character's books allow. */
    val subclassOptions
        get() = SourceFiltering.available(SubclassData.forClass(classId), character.enabledSources)

    val activeSubclassId: String?
        get() = subclassId ?: ClassLevels.subclassIn(character, classId)

    val newClassFeatures: List<ClassFeature>
        get() = progression?.featuresAt(targetClassLevel).orEmpty()

    val newSubclassFeatures: List<SubclassFeature>
        get() = activeSubclassId?.let {
            SubclassData.byId(it)?.featuresAt(targetClassLevel)
        }.orEmpty()

    /**
     * Every decision this level forces, from the class, the subclass, and anything the rules
     * let you rethink on levelling.
     *
     * That last part is why this isn't simply the new features' choices. A Primordial Patron
     * chooses its element again at every level, not only at the one that granted it, and a
     * choice offered nowhere is a choice the player silently loses.
     */
    val featureChoices: List<Choice>
        get() {
            val fromNewFeatures =
                newClassFeatures.flatMap { it.choices } + newSubclassFeatures.flatMap { it.choices }
            val revisited = ChoiceResolver.levelUpChangeable(character)
                .map { it.choice }
                .filterNot { revisit -> fromNewFeatures.any { it.id == revisit.id } }
            // What has just been ticked, over what the character already held. An invocation
            // picked a moment ago names a cantrip; that question has to appear here, next to
            // the invocation, not on a card the player finds days later. Reading the live
            // selections is what makes it appear as the option is ticked.
            val answered = ChoiceResolver.answers(character) + selections
            return ChoiceGraph.expand(
                roots = fromNewFeatures + revisited,
                answers = answered,
                books = character.enabledSources,
            )
        }

    val grantsAsi: Boolean get() = progression?.grantsAsiAt(targetClassLevel) == true

    val grantsEpicBoon: Boolean get() = progression?.grantsEpicBoonAt(targetLevel) == true

    /**
     * What the feat taken this level still asks for. Skill Expert wants a skill and an
     * Expertise, Fey-Touched a spell, Elemental Adept a damage type, and most feats from
     * level 4 on want to know which ability score goes up. None of it used to be asked.
     */
    val featChoices: List<Choice>
        get() = featId?.let {
            // Expanded, not just the feat's own list: a feat that grants a feat — and a feat
            // whose answer opens another question — asks all of it before the flow moves on.
            ChoiceGraph.expand(
                roots = OriginChoices.forFeat(it),
                answers = ChoiceResolver.answers(character) + selections,
                books = character.enabledSources,
            )
        }.orEmpty()

    // ---------------------------------------------------------------- Spellcasting

    /**
     * Spells already on the sheet that count against *this* class's allowance.
     *
     * Each class tracks its own list, so a Wizard 5 who takes a level of Cleric starts the
     * Cleric list from nothing rather than being told they already know eight Cleric spells.
     * A single-class character counts everything, since their spells may predate the source
     * label and there is only one list to belong to anyway.
     */
    private fun knownFromThisClass(predicate: (KnownSpell) -> Boolean): Int {
        val spells = character.knownSpells.filter(predicate)
        if (!ClassLevels.isMulticlassed(character) && !isNewClass) return spells.size
        val className = ClassData.byId(classId)?.name ?: classId
        return spells.count { it.source.equals(className, ignoreCase = true) }
    }

    private val currentCantripCount: Int get() = knownFromThisClass { it.level == 0 }
    private val currentSpellCount: Int get() = knownFromThisClass { it.level > 0 }

    /**
     * The subclass this level's spellcasting comes from, when the class has none of its own.
     *
     * Only the Eldritch Knight and the Arcane Trickster. Everything below has to consult it,
     * because a Fighter's class table says nothing about cantrips or prepared spells and so
     * offered its Eldritch Knight neither.
     */
    private val castingSubclass: Subclass?
        get() = activeSubclassId
            ?.let { SubclassData.byId(it) }
            ?.takeIf { it.isSpellcaster && it.classId == classId }

    val cantripsToLearn: Int
        get() {
            val target = (progression?.cantripsKnownAt(targetClassLevel) ?: 0) +
                (castingSubclass?.cantripsKnownAt(targetClassLevel) ?: 0)
            return (target - currentCantripCount).coerceAtLeast(0)
        }

    /**
     * How many level 1+ spells this level asks the player to choose.
     *
     * For most casters this is the gap between the prepared count and what is already on the
     * sheet. The Wizard is not most casters: its prepared count is drawn *from its spellbook*,
     * and the spellbook grows by two spells at every level regardless. Reading the prepared
     * column for it meant a level 2 Wizard was told they needed five spells, already had six,
     * and was asked for nothing — which is exactly what a Wizard levelling up reported.
     */
    val spellsToLearn: Int
        get() {
            spellbookSpellsThisLevel?.let { return it }
            val target = (progression?.preparedSpellsAt(targetClassLevel) ?: 0) +
                (castingSubclass?.preparedSpellsAt(targetClassLevel) ?: 0)
            return (target - currentSpellCount).coerceAtLeast(0)
        }

    /**
     * The Wizard's Spellbook feature: *"whenever you gain a Wizard level after 1, add two
     * Wizard spells of your choice to your spellbook"*. Null for everyone else.
     */
    private val spellbookSpellsThisLevel: Int?
        get() = if (classId == "wizard") {
            // Taking the first level of Wizard by multiclassing copies the starting six.
            if (isNewClass) 6 else 2
        } else {
            null
        }

    /** True when the spells chosen this level go into a spellbook rather than being prepared. */
    val learnsIntoSpellbook: Boolean get() = spellbookSpellsThisLevel != null

    /** The highest level of slot the character will have, across every class. */
    val maxSpellLevel: Int get() = CharacterCalculations.maxSpellLevel(leveledCharacter)

    /**
     * The highest spell level this class's own list offers. Slots come from the combined
     * table, but what you may prepare is still capped by your level in the class that grants
     * it — a Cleric 1 / Wizard 8 prepares level 1 Cleric spells, not level 4 ones.
     */
    private val maxSpellLevelForThisClass: Int
        get() = SpellSlotTables
            .maxSpellLevel(
                CharacterCalculations.casterTypeFor(classId, activeSubclassId),
                targetClassLevel,
            )
            .coerceAtMost(maxSpellLevel)

    /**
     * The list the spells are drawn from, which needn't be the class's own — both third
     * casters learn Wizard spells.
     */
    private val spellListClassId: String
        get() = castingSubclass?.spellListClassId?.takeIf { it.isNotBlank() } ?: classId

    /**
     * Spells a picker must not offer: the ones already on the sheet, plus the ones the class
     * and subclass hand out whatever the player does.
     *
     * A Ranger always has Hunter's Mark prepared and a Gloom Stalker always has Fear; picking
     * either buys nothing and puts a duplicate row on the Spells tab.
     */
    private val knownSpellIds: Set<String>
        get() = character.knownSpells.map { it.id }.toSet() +
            // Everything already granted: a lineage cantrip, a feat's free casting, a
            // subclass's always-prepared list. None of it is a pick to spend.
            CharacterSpells.granted(character).map { it.spell.id } +
            SpellGrantData.alwaysPreparedForClass(classId, targetClassLevel) +
            SpellGrantData.alwaysPreparedForSubclass(activeSubclassId, targetClassLevel)

    val cantripOptions
        get() = SpellData.cantripsForClass(spellListClassId).filterNot { it.id in knownSpellIds }

    /**
     * The level 1+ spells on offer this level.
     *
     * A third caster is normally held to two schools, and the picker has to say so — but the
     * levels the rules exempt (8, 14, and 20) open it back up to the whole list, which is the
     * kind of clause that goes missing without anyone noticing.
     */
    val spellOptions
        get() = SpellData.forClassUpTo(spellListClassId, maxSpellLevelForThisClass)
            .filter { it.level > 0 && it.id !in knownSpellIds }
            .filter { spell -> spell.school in allowedSchools || allowedSchools.isEmpty() }

    /** The schools this level's pick may come from; empty means no restriction. */
    val allowedSchools: Set<String>
        get() {
            val subclass = castingSubclass ?: return emptySet()
            if (targetClassLevel in subclass.freeSchoolLevels) return emptySet()
            return subclass.spellSchools
        }

    // ------------------------------------------------- Replacing what you already know

    /**
     * Whether this level offers to trade one spell, or one cantrip, for another.
     *
     * The spell trade belongs to the classes with a fixed list — a Bard, Sorcerer, or
     * Warlock has no other way to shed a spell that stopped earning its place. The cantrip
     * trade belongs to everyone who has cantrips at all, except the Artificer, which does
     * it on a Long Rest instead.
     */
    val canReplaceSpell: Boolean
        get() = CharacterSpells.swapsSpellOnLevelUp(classId) && replaceableSpells.isNotEmpty()

    val canReplaceCantrip: Boolean
        get() = CharacterSpells.swapsCantripOnLevelUp(classId) && replaceableCantrips.isNotEmpty()

    /** Spells the character actually chose for this class, which are the ones it may trade. */
    val replaceableSpells: List<KnownSpell>
        get() = ownSpells { it.level > 0 }

    val replaceableCantrips: List<KnownSpell>
        get() = ownSpells { it.level == 0 }

    /**
     * Spells belonging to the class being levelled and picked by the player.
     *
     * A spell the rules hand out — a subclass's always-prepared list, a feat's — isn't the
     * character's to trade away, and it would come straight back on the next redraw anyway.
     */
    private fun ownSpells(predicate: (KnownSpell) -> Boolean): List<KnownSpell> {
        val granted = CharacterSpells.granted(character).map { it.spell.id }.toSet()
        val mine = character.knownSpells.filter(predicate).filterNot { it.id in granted }
        // On a single-class sheet everything belongs to that class, including spells saved
        // before the source label existed — the same rule [knownFromThisClass] counts by.
        if (!ClassLevels.isMulticlassed(character) && !isNewClass) return mine
        val className = ClassData.byId(classId)?.name ?: classId
        return mine.filter { it.source.equals(className, ignoreCase = true) }
    }

    /**
     * True when the catalog can't satisfy this level on its own, so the player is offered a
     * free-text box. That covers spells above the catalogued range and the case where an
     * Edit Mode override has pushed the prepared count past what the class list holds.
     */
    val needsManualSpellEntry: Boolean
        get() = maxSpellLevelForThisClass > SpellData.MAX_CATALOGUED_LEVEL ||
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
            // Not only when the level grants something new: a choice the rules let you
            // revisit needs the step to exist on a level that grants nothing at all.
            if (newClassFeatures.isNotEmpty() || newSubclassFeatures.isNotEmpty() ||
                featureChoices.isNotEmpty()
            ) {
                add(LevelUpStep.FEATURES)
            }
            if (grantsAsi || grantsEpicBoon) add(LevelUpStep.ASI)
            if (featChoices.isNotEmpty()) add(LevelUpStep.FEAT_CHOICES)
            // The swap is offered at every level of a fixed-list caster, not only the levels
            // that hand out something new, so the step has to appear for it alone.
            if (cantripsToLearn > 0 || spellsToLearn > 0 || canReplaceSpell || canReplaceCantrip) {
                add(LevelUpStep.SPELLS)
            }
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
            LevelUpStep.FEAT_CHOICES -> featChoices.all { selections[it.id]?.size == it.count }
            LevelUpStep.SPELLS -> spellsValid()
            LevelUpStep.SUMMARY -> true
        }

    private fun asiValid(): Boolean {
        // A feat has to be one the character qualifies for, not merely one they tapped.
        if (grantsEpicBoon) return featChoiceValid()
        if (!grantsAsi) return true
        return when (asiMode) {
            AsiMode.PLUS_TWO -> asiPoints.values.sum() == 2 && asiPoints.size == 1
            AsiMode.PLUS_ONE_ONE -> asiPoints.values.sum() == 2 && asiPoints.size == 2
            AsiMode.FEAT -> featChoiceValid()
        }
    }

    private fun spellsValid(): Boolean {
        // A traded-away spell leaves a place to fill, so the counts this step must satisfy
        // grow by one for each replacement the player has chosen to make.
        val cantripsOk = newCantrips.size == cantripsToLearn + replacedCount(replacedCantripId)
        val totalSpells = newSpells.size + manualSpells.size
        val spellsWanted = spellsToLearn + replacedCount(replacedSpellId)
        // Manual entry covers levels the catalog lacks, so accept at least the required count.
        val spellsOk = if (needsManualSpellEntry) {
            totalSpells >= spellsWanted || spellOptions.isEmpty()
        } else {
            totalSpells == spellsWanted
        }
        return cantripsOk && spellsOk
    }

    private fun replacedCount(id: String?) = if (id == null) 0 else 1

    /** How many cantrips this step is waiting for, counting one traded away. */
    val cantripsWanted: Int get() = cantripsToLearn + replacedCount(replacedCantripId)

    /** How many level 1+ spells this step is waiting for, counting one traded away. */
    val spellsWanted: Int get() = spellsToLearn + replacedCount(replacedSpellId)

    /**
     * The feats on offer. A feat already taken is left out entirely rather than shown and
     * greyed, since taking the same one twice grants nothing.
     */
    val featOptions
        get() = if (grantsEpicBoon) {
            FeatData.EPIC_BOONS
        } else {
            FeatData.GENERAL_FEATS.filterNot { it.id == "ability_score_improvement" }
        }.filterNot { it.id in character.featIds }
            .let { SourceFiltering.available(it, character.enabledSources) }

    /**
     * Why a feat can't be taken yet, keyed by feat id and empty for the ones that can.
     *
     * Unavailable feats stay on the list rather than vanishing: a player weighing up the
     * Path of the Lich needs to see that Lich Ascension is the end of it and what it costs
     * to get there.
     */
    val featBlockers: Map<String, String>
        get() = featOptions.associate { feat ->
            feat.id to FeatPrerequisites.check(leveledCharacter, feat.id).missing
        }

    /** True when the feat currently selected is one the character actually qualifies for. */
    private fun featChoiceValid(): Boolean =
        featId?.let { FeatPrerequisites.isAllowed(leveledCharacter, it) } ?: false
}
