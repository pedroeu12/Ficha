package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A choice that says "one you already have" offers what you have, not the catalogue.
 *
 * Agonizing Blast reads "Choose one of your known Warlock cantrips that deals damage". Written
 * as the whole Warlock cantrip list it stopped being that question and became "learn a new
 * cantrip" — a different feature, and one the invocation does not grant. The player was asked
 * to learn Eldritch Blast in order to make Eldritch Blast stronger.
 *
 * The same wording is on Eldritch Spear, Repelling Blast, the Wizard's Spell Mastery and
 * Signature Spells ("in your spellbook"), and on every Expertise choice in the game — six of
 * them, each offering all eighteen skills where the rules offer the ones you are trained in.
 */
class OwnedOptionsTest {

    private fun warlock(vararg cantrips: String) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock", subclassId = "fiend",
        backgroundId = "soldier", level = 5,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        knownSpells = cantrips.map {
            KnownSpell(id = it, name = it, level = 0, school = "Evocation", description = "")
        },
        levelSelections = mapOf(
            "5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf(
                "agonizing_blast", "repelling_blast", "devils_sight",
                "armor_of_shadows", "eldritch_mind",
            ),
        ),
    )

    private fun ask(character: PlayerCharacter, choiceId: String): Choice =
        ChoiceResolver.all(character).first { it.choice.id == choiceId }.choice

    // ------------------------------------------------------------------ The reported case

    @Test
    fun `Agonizing Blast offers the cantrips you know, not the ones you could learn`() {
        val pc = warlock("eldritch_blast", "chill_touch", "prestidigitation")
        val offered = ask(pc, "invocation:agonizing_blast:cantrip").options.map { it.id }

        assertTrue("Eldritch Blast is known and deals damage", "eldritch_blast" in offered)
        assertTrue("Chill Touch is known and deals damage", "chill_touch" in offered)
        assertTrue(
            "Prestidigitation deals no damage, so it is not eligible",
            "prestidigitation" !in offered,
        )
        assertTrue(
            "a cantrip the character never learned is being offered: $offered",
            offered.none { it == "toll_the_dead" || it == "poison_spray" },
        )
    }

    @Test
    fun `Repelling Blast wants a cantrip that makes an attack roll`() {
        // Chill Touch is a melee spell attack; Sacred Flame forces a save.
        val pc = warlock("eldritch_blast", "chill_touch", "sacred_flame")
        val offered = ask(pc, "invocation:repelling_blast:cantrip").options.map { it.id }

        assertTrue("eldritch_blast" in offered)
        assertTrue(
            "Sacred Flame forces a saving throw, so it cannot be pushed with",
            "sacred_flame" !in offered,
        )
    }

    /**
     * A Warlock is granted Eldritch Blast, so the list is never empty — but it is never more
     * than what they have either. Picking up no other damage cantrip leaves exactly one.
     */
    @Test
    fun `a warlock who learned no other damage cantrip is offered only the one they have`() {
        val pc = warlock("prestidigitation", "minor_illusion")
        assertEquals(
            listOf("eldritch_blast"),
            ask(pc, "invocation:agonizing_blast:cantrip").options.map { it.id },
        )
    }

    // ------------------------------------------------------------------ Expertise

    @Test
    fun `Expertise offers the skills you are trained in`() {
        val rogue = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "rogue", subclassId = "thief",
            backgroundId = "soldier", level = 6,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            skillProficiencies = setOf(
                Skill.STEALTH.name, Skill.ACROBATICS.name, Skill.PERCEPTION.name,
            ),
        )
        val offered = ask(rogue, "rogue_expertise_6").options.map { it.id }.toSet()
        assertEquals(
            setOf(Skill.STEALTH.name, Skill.ACROBATICS.name, Skill.PERCEPTION.name),
            offered,
        )
    }

    // ------------------------------------------------------------------ The spellbook

    @Test
    fun `Spell Mastery picks from the spellbook, not the class list`() {
        val wizard = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "wizard", subclassId = "evoker",
            backgroundId = "soldier", level = 18,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            knownSpells = listOf(
                KnownSpell("magic_missile", "Magic Missile", 1, "Evocation", ""),
                KnownSpell("shield", "Shield", 1, "Abjuration", ""),
                KnownSpell("misty_step", "Misty Step", 2, "Conjuration", ""),
            ),
        )
        val level1 = ask(wizard, "wizard:spell_mastery_1").options.map { it.id }.toSet()
        assertEquals(setOf("magic_missile", "shield"), level1)

        val level2 = ask(wizard, "wizard:spell_mastery_2").options.map { it.id }.toSet()
        assertEquals(setOf("misty_step"), level2)
    }

    // ================================================================ The whole category

    /**
     * Nothing else words a choice this way and offers the catalogue.
     *
     * The wording is what gives it away: "your known", "in your spellbook", "one of your",
     * "skill proficiencies". A choice whose own prompt says the list is the character's, and
     * which does not read it from the character, is the bug this test exists for — and it was
     * present nine times.
     */
    @Test
    fun `every choice that says the list is yours reads it from the character`() {
        val ownWording = Regex(
            "(?i)(your known|in your spellbook|from your spellbook|one of your|" +
                "your skill proficiencies|you are proficient|you have proficiency in)"
        )

        val everyChoice = buildList {
            fun walk(choices: List<Choice>) {
                choices.forEach { c -> add(c); c.options.forEach { walk(it.grants) } }
            }
            ProgressionData.ALL.forEach { p -> p.features.forEach { walk(it.choices) } }
            SubclassData.ALL.forEach { s -> s.features.forEach { walk(it.choices) } }
            SpeciesData.ALL.forEach { sp ->
                sp.traits.forEach { walk(it.choices) }
                sp.lineageOptions.forEach { walk(it.choices) }
            }
            FeatData.ALL.forEach { walk(FeatChoiceData.choicesFor(it.id, it.name)) }
        }.distinctBy { it.id }

        val offenders = everyChoice
            .filter { ownWording.containsMatchIn("${it.label} ${it.prompt}") }
            .filter { it.optionsFrom == OptionSource.Declared }
            .map { "${it.id}: \"${it.prompt.take(70)}\"" }

        assertTrue(
            "${offenders.size} choices say the list is the character's and offer the " +
                "catalogue instead:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /** And a resolved list never offers something the character does not have. */
    @Test
    fun `a resolved list never contains something the character lacks`() {
        val pc = warlock("eldritch_blast", "chill_touch")
        val known = pc.knownSpells.map { it.id }.toSet()

        ChoiceResolver.all(pc)
            .map { it.choice }
            .filter { it.optionsFrom is OptionSource.KnownSpells }
            .forEach { choice ->
                choice.options.forEach { option ->
                    assertTrue(
                        "${choice.id} offers ${option.id}, which the character does not know",
                        option.id in known,
                    )
                }
            }
    }
}
