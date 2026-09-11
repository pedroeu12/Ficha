package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.domain.ChoiceGraph
import com.pedroeu.ficha.domain.Owned
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.SheetFeatures
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The standing checks, one per kind of mistake this ruleset keeps making.
 *
 * Every bug in this file's history was an instance of a category rather than a one-off: a
 * choice the rules ask for and the app never puts to the player, something granted outright
 * and offered for selection anyway, a number the text computes and nothing calculates, a list
 * restated at each level and printed once per restatement. Patching the reported instance
 * leaves the other twenty, so each category is checked here across the whole catalogue.
 *
 * A failure names the entries, so the fix is always either the data or a deliberate exception
 * written down beside the rule it breaks.
 */
class RulesIntegrityTest {

    // ================================================================ Granted, not selectable

    /**
     * A spell the rules hand over is never a spell to spend a pick on.
     *
     * Checked as the pickers actually see it: for every species and lineage that grants a
     * spell, against every class's creation list. A Tiefling was granted Thaumaturgy by their
     * lineage and offered it again in the Warlock cantrip list, where taking it bought them
     * nothing at all.
     */
    @Test
    fun `no creation picker offers a spell the character is already granted`() {
        val offenders = mutableListOf<String>()

        SpeciesData.ALL.forEach { species ->
            val lineages = species.lineageOptions.map { it.id } + listOf<String?>(null)
            lineages.forEach { lineageId ->
                ClassData.ALL.forEach { charClass ->
                    val granted = SpellGrantData.grantedSpellIds(
                        classId = charClass.id,
                        speciesId = species.id,
                        lineageId = lineageId,
                        level = 1,
                    )
                    if (granted.isEmpty()) return@forEach

                    charClass.choices
                        .filterIsInstance<ClassChoice.CantripChoice>()
                        .forEach { raw ->
                            val choice = Choice(
                                id = raw.id,
                                label = raw.label,
                                count = raw.count,
                                kind = ChoiceKind.SPELL,
                                options = raw.options.map { ChoiceOption(it.id, it.name) },
                            )
                            val selectable = raw.options.map { it.id }.toSet() -
                                OwnedOptions.disabledFor(choice, Owned(spells = granted))
                            selectable.filter { it in granted }.forEach {
                                offenders += "${species.id}/${lineageId ?: "-"}/" +
                                    "${charClass.id}: $it"
                            }
                        }
                }
            }
        }

        assertTrue("already granted and still on offer: $offenders", offenders.isEmpty())
    }

    /** The same rule from the sheet's side: nothing granted is ever missing from "known". */
    @Test
    fun `a granted spell counts as known for every character`() {
        val tiefling = PlayerCharacter(
            id = "t", name = "T", speciesId = "tiefling", lineageId = "infernal",
            classId = "warlock", backgroundId = "soldier", level = 1,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        val known = CharacterSpells.all(tiefling).map { it.id }
        assertTrue("the lineage cantrip must be on the sheet", "thaumaturgy" in known)
    }

    // ================================================================ Sub-choices get asked

    /**
     * An option whose text tells the player to choose something must raise that question.
     *
     * The invocations are the case that keeps recurring: Agonizing Blast names a cantrip,
     * Elemental Overflow names a damage type, Pact of the Tome names three cantrips and a
     * ritual. Taking one and being asked nothing leaves a feature on the sheet with a blank
     * where its subject should be.
     */
    @Test
    fun `an option that tells you to choose something raises a choice for it`() {
        // Wording that means "there is a second decision here", as opposed to the option
        // itself being the decision.
        val asksAgain = Regex(
            "(?i)\\b(choose|choosing) (one|two|three|a|an|the) " +
                "(?!of the following effects)(?!when you gain)",
        )

        val invocations = ProgressionData.forClass("warlock")!!.features
            .flatMap { it.choices }
            .first { it.id == ProgressionData.INVOCATION_CHOICE_ID }
            .options

        // Every question the app raises off the back of an invocation. It asks for all of
        // them at once — an option raises nothing until it is taken — which is the whole of
        // the general rule, applied here to the one list this test is about.
        val raised = ChoiceGraph.followUps(
            roots = listOf(
                ProgressionData.forClass("warlock")!!.features
                    .flatMap { it.choices }
                    .first { it.id == ProgressionData.INVOCATION_CHOICE_ID }
            ),
            answers = mapOf(
                ProgressionData.INVOCATION_CHOICE_ID to invocations.map { it.id },
            ),
        ).map { it.id }.toSet() + PerUseChoiceData.ALL.map { it.id }.toSet()

        val missing = invocations
            .filter { asksAgain.containsMatchIn(it.description) }
            .filterNot { option ->
                raised.any { it.contains(option.id) } || option.id in KNOWN_UNPROMPTED
            }
            .map { it.name }

        assertTrue(
            "these invocations tell the player to choose something and nothing asks: $missing",
            missing.isEmpty(),
        )
    }

    /**
     * Options whose sub-choice is deliberately not modelled, with the reason.
     *
     * Each of these picks something the sheet has no field for, so a prompt would collect an
     * answer nothing could use. They stay on this list rather than quietly passing the check.
     */
    private val KNOWN_UNPROMPTED = setOf(
        // Names a familiar's form, which the sheet records as a written note rather than a
        // modelled creature.
        "investment_of_the_chain_master",
        "pact_chain",
    )

    // ================================================================ Formulas get computed

    /**
     * A feat whose text computes from "the ability increased by this feat" must name which
     * abilities it could be, or the number can never be worked out.
     */
    @Test
    fun `a feat computing from its own ability increase offers that choice`() {
        val computesFromIncrease = Regex("(?i)modifier of the ability (increased|you increased)")
        val missing = FeatData.ALL
            .filter { computesFromIncrease.containsMatchIn(it.description) }
            .filterNot { FeatChoiceData.ABILITY_OPTIONS.containsKey(it.id) }
            .map { it.id }

        assertTrue(
            "these compute from an ability the player picks and never ask which: $missing",
            missing.isEmpty(),
        )
    }

    // ================================================================ Restatements listed once

    /**
     * A feature the class table restates at higher levels appears on the sheet once.
     *
     * A Warlock's table says "Eldritch Invocations" at eight levels and a martial class's says
     * "Weapon Mastery" at three or four. Each is the same choice with a bigger number, and
     * printing one row per restatement gave a level 5 Warlock three headings and nine copies
     * of the same list.
     */
    @Test
    fun `no class lists the same choice twice on the sheet`() {
        val offenders = mutableListOf<String>()

        ClassData.ALL.forEach { charClass ->
            val character = PlayerCharacter(
                id = "t", name = "T", speciesId = "human", classId = charClass.id,
                backgroundId = "soldier", level = 20,
                classLevels = listOf(ClassLevel(charClass.id, 20, null, isStarting = true)),
                baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            )
            SheetFeatures.classFeatures(character, charClass.id)
                .filter { it.choiceIds.isNotEmpty() }
                .groupBy { it.name to it.choiceIds }
                .filterValues { it.size > 1 }
                .forEach { (key, rows) ->
                    offenders += "${charClass.id}: ${key.first} × ${rows.size}"
                }
        }

        assertTrue("a feature is listed once per restatement: $offenders", offenders.isEmpty())
    }

    /** And the same for every subclass. */
    @Test
    fun `no subclass lists the same choice twice on the sheet`() {
        val offenders = mutableListOf<String>()

        SubclassData.ALL.forEach { subclass ->
            val character = PlayerCharacter(
                id = "t", name = "T", speciesId = "human", classId = subclass.classId,
                subclassId = subclass.id, backgroundId = "soldier", level = 20,
                classLevels = listOf(
                    ClassLevel(subclass.classId, 20, subclass.id, isStarting = true),
                ),
                baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            )
            SheetFeatures.subclassFeatures(character, subclass.classId)
                .filter { it.choiceIds.isNotEmpty() }
                .groupBy { it.name to it.choiceIds }
                .filterValues { it.size > 1 }
                .forEach { (key, rows) -> offenders += "${subclass.id}: ${key.first} × ${rows.size}" }
        }

        assertTrue("a feature is listed once per restatement: $offenders", offenders.isEmpty())
    }

    /** A level-1 feature is not printed twice because two tables both describe it. */
    @Test
    fun `no class describes a level 1 feature in two places`() {
        val offenders = mutableListOf<String>()

        ClassData.ALL.forEach { charClass ->
            val character = PlayerCharacter(
                id = "t", name = "T", speciesId = "human", classId = charClass.id,
                backgroundId = "soldier", level = 3,
                baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            )
            SheetFeatures.classFeatures(character, charClass.id)
                .groupBy { it.name }
                .filterValues { it.size > 1 }
                .forEach { (name, rows) -> offenders += "${charClass.id}: $name × ${rows.size}" }
        }

        assertTrue("the same feature, listed twice: $offenders", offenders.isEmpty())
    }

    // ================================================================ Per-use, not per-life

    /**
     * Every per-use choice reaches the player somewhere.
     *
     * One that names a pool appears on that pool's tracker and is asked when a use is spent;
     * one that names none appears on its own card. A choice belonging to a subclass nobody
     * can take, or naming a pool that doesn't exist, reaches neither.
     */
    @Test
    fun `every per-use choice belongs to something real`() {
        val subclassIds = SubclassData.ALL.map { it.id }.toSet()
        val speciesIds = SpeciesData.ALL.map { it.id }.toSet()
        val broken = PerUseChoiceData.ALL.filter { choice ->
            val owner = when {
                choice.subclassId.isNotBlank() -> choice.subclassId in subclassIds
                choice.speciesId.isNotBlank() -> choice.speciesId in speciesIds
                else -> false
            }
            !owner || choice.options.isEmpty()
        }.map { it.id }

        assertTrue("these can never be shown: $broken", broken.isEmpty())
    }
}
