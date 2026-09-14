package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterFeats
import com.pedroeu.ficha.domain.CharacterHitDice
import com.pedroeu.ficha.domain.CharacterMasteries
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.FeatEdits
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.domain.ScoreMethod
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.StatTarget
import com.pedroeu.ficha.ui.creation.BonusSpread
import com.pedroeu.ficha.ui.creation.CharacterBuilder
import com.pedroeu.ficha.ui.creation.CreationState
import com.pedroeu.ficha.ui.creation.CreationStep
import com.pedroeu.ficha.ui.levelup.AsiMode
import com.pedroeu.ficha.ui.levelup.LevelUpApplier
import com.pedroeu.ficha.ui.levelup.LevelUpState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A character's whole life, walked through the same code the screens run.
 *
 * Creation, each level up, Edit Mode, a rest and a multiclass are separate flows with
 * separate state, and the bugs that survive unit tests are the ones between them: a pick
 * made in one flow that the next reads differently, a number that moves in one place and
 * not another. Each walk below checks the sheet against itself after every step — the
 * choices it says are answered, the numbers that should have moved, the things that should
 * not have doubled.
 */
class CharacterLifecycleTest {

    // ------------------------------------------------------------------ Drivers

    /** Answers every open question in the wizard with the first eligible options. */
    private fun answered(start: CreationState): CreationState {
        var s = start
        val charClass = s.charClass!!
        // Only what the caller left open: a walk that overwrote the answers it was given
        // would be testing its own defaults rather than the character asked for.
        charClass.choices.forEach { choice ->
            when (choice) {
                is ClassChoice.SkillProficiencyChoice ->
                    if (s.classSkillChoices.size != choice.count) s = s.copy(
                        classSkillChoices = choice.options.filterNot { it in s.grantedSkills }
                            .take(choice.count).toSet(),
                    )
                is ClassChoice.FeatureOption ->
                    if (s.classSelections[choice.id].isNullOrEmpty()) s = s.copy(
                        classSelections = s.classSelections + (choice.id to listOf(choice.options.first().id)),
                    )
                is ClassChoice.CantripChoice ->
                    if (s.classSelections[choice.id]?.size != choice.count) s = s.copy(
                        classSelections = s.classSelections +
                            (choice.id to choice.options.take(choice.count).map { it.id }),
                    )
            }
        }
        if (charClass.id == "rogue") {
            s = s.copy(expertiseChoices = s.allSkillProficiencies.take(2).toSet())
        }
        // Answering a feat raises the feat's own questions, so this runs to a fixed point.
        repeat(6) {
            s.classFeatureChoices
                .filter { s.classFeatureSelections[it.id]?.size != it.count }
                .forEach { choice ->
                    s = s.copy(classFeatureSelections = s.classFeatureSelections + (choice.id to firstEligible(choice, s)))
                }
            s.originChoices
                .filter { s.originSelections[it.id]?.size != it.count }
                .forEach { choice ->
                    s = s.copy(originSelections = s.originSelections + (choice.id to firstEligible(choice, s)))
                }
        }
        return s
    }

    private fun firstEligible(choice: Choice, s: CreationState): List<String> {
        val disabled = OwnedOptions.disabledFor(choice, s.owned, classLevels = mapOf(s.classId!! to 1))
        return choice.options.filter { it.id !in disabled }.take(choice.count).map { it.id }
    }

    private fun create(state: CreationState): PlayerCharacter {
        val s = answered(state)
        CreationStep.ORDER.forEach { step ->
            assertTrue("creation cannot pass the $step step", s.copy(step = step).canAdvance)
        }
        return CharacterBuilder.build(s)
    }

    /** One level, every step answered the way a player would: first eligible option, average HP. */
    private fun levelUp(
        pc: PlayerCharacter,
        classId: String? = null,
        configure: (LevelUpState) -> LevelUpState = { it },
    ): PlayerCharacter {
        var s = LevelUpState(character = pc, levellingClassId = classId)
        if (s.gainsSubclass) s = s.copy(subclassId = s.subclassOptions.first().id)
        s = configure(s)
        s = answerFeatures(s)
        if (s.grantsAsi && s.asiMode != AsiMode.FEAT && s.asiPoints.isEmpty()) {
            s = s.copy(asiMode = AsiMode.PLUS_TWO, asiPoints = mapOf(Ability.STR to 2))
        }
        s = s.copy(
            newCantrips = s.cantripOptions.take(s.cantripsWanted).map { it.id },
            newSpells = s.spellOptions.take(s.spellsWanted).map { it.id },
        )
        s.steps.forEach { step ->
            assertTrue(
                "level ${s.targetLevel} (${s.classId}) cannot pass the $step step",
                s.copy(requestedStep = step).canAdvance,
            )
        }
        return LevelUpApplier.apply(s)
    }

    private fun answerFeatures(start: LevelUpState): LevelUpState {
        var s = start
        val owned = OwnedOptions.of(s.character)
        repeat(6) {
            (s.featureChoices + s.featChoices)
                .filter { s.selections[it.id]?.size != it.count }
                .forEach { choice ->
                    val disabled = OwnedOptions.disabledFor(
                        choice, owned,
                        currentSelection = s.selections[choice.id].orEmpty().toSet(),
                        classLevels = ClassLevels.levelMap(s.leveledCharacter),
                    )
                    val pick = choice.options.filter { it.id !in disabled }.take(choice.count).map { it.id }
                    s = s.copy(selections = s.selections + (choice.id to pick))
                }
        }
        return s
    }

    /** What has to hold after every step, whatever the character is. */
    private fun assertConsistent(pc: PlayerCharacter, where: String) {
        assertEquals("$where: level and class levels disagree", pc.level, ClassLevels.totalLevel(pc))
        val spells = CharacterSpells.all(pc).map { it.id }
        assertEquals("$where: a spell is listed twice", spells.distinct().size, spells.size)
        ChoiceResolver.all(pc)
            .filter { it.choice.options.isNotEmpty() && it.choice.count > 0 }
            .forEach { resolved ->
                assertTrue(
                    "$where: ${resolved.choice.id} is unanswered on a finished sheet",
                    resolved.isAnswered,
                )
                assertTrue(
                    "$where: ${resolved.choice.id} holds an answer its options do not offer: ${resolved.selectedIds}",
                    resolved.selectedIds.all { id -> resolved.choice.options.any { it.id == id } },
                )
            }
        CharacterResources.states(pc).forEach {
            assertTrue("$where: ${it.def.id} has more spent than it has", it.spent <= it.def.max)
        }
        assertTrue("$where: hit points above maximum", pc.currentHitPoints <= CharacterCalculations.maxHitPoints(pc))
        val answers = ChoiceResolver.answers(pc)
        ChoiceResolver.all(pc).forEach { resolved ->
            assertEquals(
                "$where: ${resolved.choice.id} — the sheet and the rules disagree",
                answers[resolved.choice.id].orEmpty(), resolved.selectedIds,
            )
        }
    }

    private val standardArray = mapOf(
        Ability.STR to 15, Ability.CON to 14, Ability.INT to 13,
        Ability.DEX to 12, Ability.WIS to 10, Ability.CHA to 8,
    )

    // ------------------------------------------------------------------ A Fighter's life

    @Test
    fun `a human fighter from creation to a wizard level`() {
        val made = create(
            CreationState(
                enabledSources = Sourcebook.CORE,
                speciesId = "human",
                speciesSkillChoices = setOf(Skill.ARCANA),
                backgroundId = "soldier",
                bonusSpread = BonusSpread.TWO_ONE,
                backgroundBonuses = mapOf(Ability.STR to 2, Ability.CON to 1),
                classId = "fighter",
                classSelections = mapOf("fighting_style" to listOf("defense")),
                scoreMethod = ScoreMethod.STANDARD_ARRAY,
                assignedScores = standardArray,
                name = "Varek",
            )
        )
        assertConsistent(made, "creation")
        assertEquals(1, made.level)
        assertEquals(17, CharacterCalculations.finalAbilityScores(made)[Ability.STR])
        assertEquals(3, CharacterMasteries.all(made).size)
        assertEquals("Defense is applied, not only recorded", 1, RulesEngine.statBonus(made, StatTarget.ARMOR_CLASS))
        assertEquals(made.currentHitPoints, CharacterCalculations.maxHitPoints(made))
        assertTrue(Skill.ARCANA.name in made.skillProficiencies)

        val two = levelUp(made)
        assertConsistent(two, "level 2")
        assertTrue(CharacterResources.definitions(two).any { it.name.contains("Action Surge") })
        assertEquals(
            CharacterCalculations.maxHitPoints(made) + 6 + 2,
            CharacterCalculations.maxHitPoints(two),
        )

        val three = levelUp(two)
        assertConsistent(three, "level 3")
        assertTrue("level 3 is where a Fighter picks one", ClassLevels.subclassIn(three, "fighter") != null)

        val four = levelUp(three) { it.copy(asiMode = AsiMode.FEAT, featId = "skill_expert") }
        assertConsistent(four, "level 4")
        assertTrue("skill_expert" in CharacterFeats.heldBy(four))
        assertTrue("the feat's expertise reached the sheet", four.skillExpertise.isNotEmpty())
        assertEquals(
            "the feat raised the score it was asked to raise",
            CharacterCalculations.finalAbilityScores(three).values.sum() + 1,
            CharacterCalculations.finalAbilityScores(four).values.sum(),
        )
        assertEquals(4, CharacterMasteries.all(four).size)

        // Edit Mode: change the style, pin a score, take the feat away.
        val style = ChoiceResolver.all(four).first { it.choice.id == "fighting_style" }
        val dueling = ChoiceGrants.answer(four, style.choice, style.level, listOf("dueling"))
        assertConsistent(dueling, "after changing the style")
        assertEquals(0, RulesEngine.statBonus(dueling, StatTarget.ARMOR_CLASS))
        assertEquals(listOf("dueling"), ChoiceResolver.latestSelectionFor(dueling, "fighting_style"))

        val strong = dueling.copy(abilityScoreOverrides = mapOf(Ability.STR.name to 20))
        val before = CharacterCalculations.attacks(dueling).first { it.name.contains("sword", true) || true }
        val after = CharacterCalculations.attacks(strong).first { it.id == before.id }
        assertEquals(
            "a pinned score reaches the attack line",
            before.attackBonus + (5 - CharacterCalculations.abilityModifiers(dueling)[Ability.STR]!!),
            after.attackBonus,
        )
        assertEquals(5 + CharacterCalculations.proficiencyBonus(strong), CharacterCalculations.savingThrowBonus(strong, Ability.STR))

        val noFeat = FeatEdits.remove(strong, "skill_expert")
        assertConsistent(noFeat, "after removing the feat")
        assertFalse("skill_expert" in CharacterFeats.heldBy(noFeat))
        assertTrue(noFeat.skillExpertise.isEmpty())

        // A day of adventure, then a Long Rest.
        val surge = CharacterResources.definitions(noFeat).first { it.name.contains("Action Surge") }
        val spent = CharacterResources.withUsesChanged(noFeat, surge.id, 1).copy(
            currentHitPoints = 5, temporaryHitPoints = 3,
        )
        val rested = RestEngine.longRest(spent).character
        assertConsistent(rested, "after a long rest")
        assertEquals(0, rested.resourceUses[surge.id] ?: 0)
        assertEquals(0, rested.temporaryHitPoints)
        assertEquals(CharacterCalculations.maxHitPoints(rested), rested.currentHitPoints)

        // Into a second class. Intelligence 13 opens the Wizard; Strength 17 lets a Fighter leave.
        assertTrue(Multiclassing.canTake(rested, "wizard"))
        val wizard = levelUp(rested, classId = "wizard")
        assertConsistent(wizard, "fighter 4 / wizard 1")
        assertEquals(listOf("fighter" to 4, "wizard" to 1), ClassLevels.of(wizard).map { it.classId to it.level })
        assertEquals(5, wizard.level)
        assertEquals(3, CharacterCalculations.proficiencyBonus(wizard))
        assertEquals(2, CharacterCalculations.spellSlots(wizard)[1])
        assertEquals(
            "a new class's cantrips and spellbook are learned",
            3, CharacterSpells.all(wizard).count { it.level == 0 },
        )
        assertEquals(6, CharacterSpells.all(wizard).count { it.level > 0 })
        assertFalse("saves come only from the first class", CharacterCalculations.isSavingThrowProficient(wizard, Ability.INT))
        assertTrue(CharacterCalculations.isSavingThrowProficient(wizard, Ability.STR))
        assertEquals(setOf(10, 6), CharacterHitDice.pools(wizard).map { it.die }.toSet())
        assertEquals(Ability.INT, CharacterCalculations.spellcastingAbility(wizard))
    }

    // ------------------------------------------------------------------ A Warlock's cantrip

    @Test
    fun `a warlock names the cantrip Agonizing Blast strengthens and can rename it later`() {
        val made = create(
            CreationState(
                enabledSources = Sourcebook.CORE,
                speciesId = "human",
                speciesSkillChoices = setOf(Skill.ARCANA),
                backgroundId = "sage",
                backgroundBonuses = mapOf(Ability.CHA to 2, Ability.CON to 1),
                classId = "warlock",
                scoreMethod = ScoreMethod.STANDARD_ARRAY,
                assignedScores = mapOf(
                    Ability.CHA to 15, Ability.CON to 14, Ability.DEX to 13,
                    Ability.WIS to 12, Ability.INT to 10, Ability.STR to 8,
                ),
                name = "Ilse",
            )
        )
        assertConsistent(made, "creation")
        assertTrue("eldritch_blast" in CharacterSpells.all(made).map { it.id })

        val two = levelUp(made) { s ->
            s.copy(
                selections = s.selections + (ProgressionData.INVOCATION_CHOICE_ID to
                    listOf("agonizing_blast", "devils_sight", "armor_of_shadows")),
            )
        }
        assertConsistent(two, "level 2")
        val question = ChoiceResolver.all(two).first { it.choice.id == "invocation:agonizing_blast:cantrip" }
        assertTrue("the cantrip question was answered during the level", question.isAnswered)
        assertTrue(
            "the invocation's own spell arrived",
            "mage_armor" in CharacterSpells.all(two).map { it.id },
        )
        val spellCount = CharacterSpells.all(two).size

        // Named again, on a different cantrip than the one it currently names.
        val other = question.choice.options.first { it.id !in question.selectedIds }
        val renamed = ChoiceGrants.toggle(two, question.choice, question.level, other.id)
        assertConsistent(renamed, "after renaming the cantrip")
        assertEquals(listOf(other.id), ChoiceResolver.latestSelectionFor(renamed, question.choice.id))
        assertEquals("naming a cantrip to strengthen never learns one", spellCount, CharacterSpells.all(renamed).size)

        val three = levelUp(renamed)
        assertConsistent(three, "level 3")
        assertTrue(ClassLevels.subclassIn(three, "warlock") != null)
    }

    // ------------------------------------------------------------------ A Wizard's spellbook

    @Test
    fun `a high elf wizard learns into a spellbook and can rest`() {
        val made = create(
            CreationState(
                enabledSources = Sourcebook.CORE,
                speciesId = "elf",
                lineageId = "high_elf",
                speciesSkillChoices = setOf(Skill.PERCEPTION),
                backgroundId = "sage",
                backgroundBonuses = mapOf(Ability.INT to 2, Ability.CON to 1),
                classId = "wizard",
                scoreMethod = ScoreMethod.STANDARD_ARRAY,
                assignedScores = mapOf(
                    Ability.INT to 15, Ability.CON to 14, Ability.DEX to 13,
                    Ability.WIS to 12, Ability.CHA to 10, Ability.STR to 8,
                ),
                name = "Aelar",
            )
        )
        assertConsistent(made, "creation")
        val lineageCantrip = ChoiceResolver.restChangeable(made)
        assertTrue("the High Elf's cantrip may be changed on a rest", lineageCantrip.isNotEmpty())
        assertTrue(
            "the lineage cantrip is on the spell list",
            lineageCantrip.first().selectedIds.all { id -> CharacterSpells.all(made).any { it.id == id } },
        )

        val two = levelUp(made)
        assertConsistent(two, "level 2")
        assertEquals(
            "two spells joined the spellbook",
            CharacterSpells.all(made).count { it.level > 0 } + 2,
            CharacterSpells.all(two).count { it.level > 0 },
        )
        val three = levelUp(two)
        assertConsistent(three, "level 3")
        assertEquals(2, CharacterCalculations.spellSlots(three)[2])
        assertTrue("a spellbook to prepare from", CharacterSpells.preparable(three).isNotEmpty())
        // The book holds ten by now and the table allows four.
        assertTrue(
            "a Wizard's spellbook was counted as its prepared list: " +
                "${CharacterSpells.preparedCount(three)} of ${CharacterCalculations.maxPreparedSpells(three)}",
            CharacterSpells.preparedCount(three) <= CharacterCalculations.maxPreparedSpells(three),
        )
        assertTrue(
            "the spells past the limit stay in the book",
            CharacterSpells.all(three).count { it.level > 0 } >
                CharacterCalculations.maxPreparedSpells(three),
        )

        val rested = RestEngine.longRest(three.copy(spellSlotsExpended = mapOf("1" to 2))).character
        assertTrue("slots come back", rested.spellSlotsExpended.isEmpty())
        assertConsistent(rested, "after a rest")
    }
}
