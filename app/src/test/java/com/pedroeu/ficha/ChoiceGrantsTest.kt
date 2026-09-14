package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.FeatEdits
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An answer's effects arrive with the answer and leave with it, by every route.
 *
 * Creation, the level-up flow and Edit Mode each used to turn an answer into a proficiency or
 * a spell with their own switch, and each switch had a hole: Edit Mode never granted
 * Expertise, no route granted a language, a spell chosen inside a class feature reached
 * nowhere, and removing a feat removed nothing it had granted.
 */
class ChoiceGrantsTest {

    private val fighter = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "fighter",
        subclassId = "champion", backgroundId = "soldier", level = 8,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        skillProficiencies = setOf(Skill.ATHLETICS.name, Skill.PERCEPTION.name),
    )

    // ------------------------------------------------------------------ Feats

    @Test
    fun `taking Skill Expert grants the proficiency and the expertise`() {
        val choices = OriginChoices.forFeat("skill_expert")
        val skill = choices.first { it.kind == ChoiceKind.SKILL }
        val expertise = choices.first { it.kind == ChoiceKind.EXPERTISE }
        val ability = choices.first { it.kind == ChoiceKind.ABILITY_SCORE }

        val taken = FeatEdits.add(
            fighter, "skill_expert",
            mapOf(
                skill.id to listOf(Skill.STEALTH.name),
                expertise.id to listOf(Skill.ATHLETICS.name),
                ability.id to listOf(Ability.DEX.name),
            ),
        )
        assertTrue("Stealth was picked and not granted", Skill.STEALTH.name in taken.skillProficiencies)
        assertTrue(
            "Expertise in Athletics was picked and not granted — the Edit Mode hole",
            Skill.ATHLETICS.name in taken.skillExpertise,
        )

        val given = FeatEdits.remove(taken, "skill_expert")
        assertFalse("Stealth stayed after the feat left", Skill.STEALTH.name in given.skillProficiencies)
        assertFalse(Skill.ATHLETICS.name in given.skillExpertise)
        assertTrue("the training Athletics had before the feat is untouched", Skill.ATHLETICS.name in given.skillProficiencies)
        assertTrue("the feat's answers were forgotten with it", given.originChoiceSelections.keys.none { it.startsWith("feat:skill_expert") })
        assertEquals(fighter.featIds, given.featIds)
    }

    @Test
    fun `taking and removing Magic Initiate adds and removes its spells`() {
        val choices = OriginChoices.forFeat("magic_initiate_wizard")
        val spellChoices = choices.filter { it.kind == ChoiceKind.SPELL }
        assertTrue(spellChoices.isNotEmpty())
        val selections = spellChoices.associate { choice ->
            choice.id to choice.options.take(choice.count).map { it.id }
        }
        val picked = selections.values.flatten().toSet()

        val taken = FeatEdits.add(fighter, "magic_initiate_wizard", selections)
        val known = CharacterSpells.all(taken).map { it.id }.toSet()
        assertTrue("the feat's spells are missing: ${picked - known}", picked.all { it in known })

        val given = FeatEdits.remove(taken, "magic_initiate_wizard")
        val left = CharacterSpells.all(given).map { it.id }.toSet()
        assertTrue("spells the feat brought stayed behind: ${picked intersect left}", picked.none { it in left })
    }

    @Test
    fun `removing a feat leaves a spell the class also knows`() {
        val choices = OriginChoices.forFeat("magic_initiate_wizard")
        val cantrips = choices.first { it.kind == ChoiceKind.SPELL && it.options.any { o -> o.id == "fire_bolt" } }
        val alsoFromClass = fighter.copy(
            knownSpells = listOf(KnownSpell("fire_bolt", "Fire Bolt", 0, "Evocation", "", source = "Wizard")),
        )
        val selections = mapOf(cantrips.id to listOf("fire_bolt", cantrips.options.first { it.id != "fire_bolt" }.id))
        val taken = FeatEdits.add(alsoFromClass, "magic_initiate_wizard", selections)
        val given = FeatEdits.remove(taken, "magic_initiate_wizard")
        assertTrue(
            "Fire Bolt came from the class list and should survive the feat leaving",
            given.knownSpells.any { it.id == "fire_bolt" && it.source == "Wizard" },
        )
    }

    @Test
    fun `a feat's language reaches the sheet`() {
        val withLanguage = FeatChoiceData.run {
            com.pedroeu.ficha.data.content.FeatData.ALL.firstOrNull { feat ->
                choicesFor(feat.id, feat.name).any { it.kind == ChoiceKind.LANGUAGE }
            }
        } ?: return
        val choice = OriginChoices.forFeat(withLanguage.id).first { it.kind == ChoiceKind.LANGUAGE }
        val others = OriginChoices.forFeat(withLanguage.id).filter { it.id != choice.id }
            .associate { it.id to it.options.take(it.count).map { o -> o.id } }
        val taken = FeatEdits.add(fighter, withLanguage.id, others + (choice.id to listOf("Elvish")))
        assertTrue("the language was picked and never written down", "Elvish" in taken.languages)
        assertFalse("Elvish" in FeatEdits.remove(taken, withLanguage.id).languages)
    }

    // ------------------------------------------------------------------ Changing an answer

    @Test
    fun `changing a skill answer in Edit Mode swaps the proficiency`() {
        val choices = OriginChoices.forFeat("skilled")
        val skill = choices.first { it.kind == ChoiceKind.SKILL }
        val taken = FeatEdits.add(
            fighter, "skilled",
            mapOf(skill.id to listOf(Skill.STEALTH.name, Skill.ARCANA.name, Skill.NATURE.name)),
        )
        val resolved = ChoiceResolver.all(taken).first { it.choice.id == skill.id }
        val changed = ChoiceGrants.answer(
            taken, resolved.choice, resolved.level,
            listOf(Skill.STEALTH.name, Skill.ARCANA.name, Skill.MEDICINE.name),
        )
        assertTrue(Skill.MEDICINE.name in changed.skillProficiencies)
        assertFalse("the pick that was given up stayed on the sheet", Skill.NATURE.name in changed.skillProficiencies)
        assertTrue(Skill.STEALTH.name in changed.skillProficiencies)
    }

    @Test
    fun `toggling reads the current answer from the character, never from a copy`() {
        val choices = OriginChoices.forFeat("skilled")
        val skill = choices.first { it.kind == ChoiceKind.SKILL }
        val taken = FeatEdits.add(fighter, "skilled", mapOf(skill.id to listOf(Skill.STEALTH.name)))
        val resolved = ChoiceResolver.all(taken).first { it.choice.id == skill.id }

        // Two taps in a row, each computed from the character as it is after the last.
        val one = ChoiceGrants.toggle(taken, resolved.choice, resolved.level, Skill.ARCANA.name)
        val two = ChoiceGrants.toggle(one, resolved.choice, resolved.level, Skill.NATURE.name)
        assertEquals(
            listOf(Skill.STEALTH.name, Skill.ARCANA.name, Skill.NATURE.name),
            ChoiceResolver.latestSelectionFor(two, skill.id),
        )
        val off = ChoiceGrants.toggle(two, resolved.choice, resolved.level, Skill.ARCANA.name)
        assertEquals(listOf(Skill.STEALTH.name, Skill.NATURE.name), ChoiceResolver.latestSelectionFor(off, skill.id))
        assertFalse(Skill.ARCANA.name in off.skillProficiencies)
    }

    // ------------------------------------------------------------------ Class features

    @Test
    fun `a spell chosen inside a class feature reaches the spell list`() {
        val warlock = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "warlock",
            subclassId = "fiend", backgroundId = "soldier", level = 2,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        val invocations = ChoiceResolver.all(warlock)
            .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        val withTome = ChoiceGrants.answer(
            warlock, invocations.choice, invocations.level, listOf("pact_tome", "devils_sight", "armor_of_shadows"),
        )
        val cantrips = ChoiceResolver.all(withTome).first { it.choice.id == "invocation:pact_tome:cantrips" }
        val picked = cantrips.choice.options.take(3).map { it.id }
        val answered = ChoiceGrants.answer(withTome, cantrips.choice, cantrips.level, picked)

        val known = CharacterSpells.all(answered).map { it.id }.toSet()
        assertTrue(
            "the Book of Shadows' cantrips were recorded and never learned: ${picked - known}",
            picked.all { it in known },
        )
    }

    @Test
    fun `a choice drawn from spells you already have learns nothing new`() {
        val warlock = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "warlock",
            subclassId = "fiend", backgroundId = "soldier", level = 5,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            knownSpells = listOf(KnownSpell("chill_touch", "Chill Touch", 0, "Necromancy", "")),
            levelSelections = mapOf(
                "5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast", "devils_sight"),
            ),
        )
        val question = ChoiceResolver.all(warlock).first { it.choice.id == "invocation:agonizing_blast:cantrip" }
        assertTrue(question.choice.optionsFrom is OptionSource.KnownSpells)
        val before = CharacterSpells.all(warlock).size
        val answered = ChoiceGrants.answer(warlock, question.choice, question.level, listOf("chill_touch"))
        assertEquals("naming a cantrip to strengthen added a spell", before, CharacterSpells.all(answered).size)
    }

    /** Every spell-kind choice a class or subclass asks names spells the catalogue has. */
    @Test
    fun `every declared spell choice in the class tables resolves`() {
        val everyChoice = buildList {
            ProgressionData.ALL.forEach { p -> p.features.forEach { addAll(it.choices) } }
            SubclassData.ALL.forEach { s -> s.features.forEach { addAll(it.choices) } }
        }.flatMap { c -> listOf(c) + c.options.flatMap { it.grants } }
            .filter { it.kind == ChoiceKind.SPELL && it.optionsFrom == OptionSource.Declared }
        assertTrue(everyChoice.isNotEmpty())
        everyChoice.forEach { choice ->
            choice.options.forEach { option ->
                assertTrue(
                    "${choice.id} offers ${option.id}, which is not a spell",
                    com.pedroeu.ficha.data.content.SpellData.byId(option.id) != null,
                )
            }
        }
    }
}
