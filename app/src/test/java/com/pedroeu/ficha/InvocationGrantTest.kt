package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.CharacterSummons
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An Eldritch Invocation that hands over a spell hands it over.
 *
 * Reported straight off a level 1 Warlock: Pact of the Chain says "You learn the Find Familiar
 * spell" and the sheet had no Find Familiar on it. Eleven other invocations were doing the
 * same — Armor of Shadows granting no Mage Armor, Otherworldly Leap no Jump.
 *
 * They were missed because the grant audit walked features, traits and feats, and an
 * invocation is none of those: it is an *option inside a choice*, and nothing was checking
 * options. Fighting Styles, Metamagic, maneuvers and the Artificer's plans live there too, so
 * the hole was bigger than the Warlock.
 */
class InvocationGrantTest {

    private fun warlock(level: Int, vararg invocations: String) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock",
        subclassId = if (level >= 3) "fiend" else null,
        backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = mapOf(
            "$level:${ProgressionData.INVOCATION_CHOICE_ID}" to invocations.toList(),
        ),
    )

    private fun spells(pc: PlayerCharacter) =
        CharacterSpells.all(pc).map { it.id }.toSet()

    // ------------------------------------------------------------------ The reported case

    @Test
    fun `a level 1 warlock with Pact of the Chain has Find Familiar`() {
        val pc = warlock(1, "pact_chain")
        assertTrue(
            "Pact of the Chain says 'You learn the Find Familiar spell'",
            "find_familiar" in spells(pc),
        )
        assertTrue(
            "and it is always available, not something to prepare",
            CharacterSpells.all(pc).single { it.id == "find_familiar" }.prepared,
        )
    }

    /** "…or one of the following special forms: Imp, Pseudodragon, Quasit, Skeleton…" */
    @Test
    fun `Pact of the Chain adds its eight special familiar forms`() {
        val pc = warlock(1, "pact_chain")
        val familiar = CharacterSummons.available(pc)
            .single { it.summons.summonId == "find_familiar" }
        val names = familiar.options.map { it.name }

        listOf(
            "Imp", "Pseudodragon", "Quasit", "Skeleton",
            "Slaad Tadpole", "Sphinx of Wonder", "Sprite", "Venomous Snake",
        ).forEach {
            assertTrue("Pact of the Chain should offer a $it: $names", it in names)
        }
        assertTrue("the ordinary forms are still there", "Owl" in names)
    }

    /** And only to the Warlock who took the pact. */
    @Test
    fun `a familiar without the pact offers only the ordinary forms`() {
        val wizard = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "wizard",
            backgroundId = "soldier", level = 5,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            knownSpells = listOf(
                com.pedroeu.ficha.domain.KnownSpell(
                    id = "find_familiar", name = "Find Familiar", level = 1,
                    school = "Conjuration", description = "",
                )
            ),
        )
        val names = CharacterSummons.available(wizard)
            .single { it.summons.summonId == "find_familiar" }
            .options.map { it.name }
        assertFalse("a Wizard should not be offered an Imp: $names", "Imp" in names)
        assertTrue("Owl" in names)
    }

    // ------------------------------------------------------------------ The other eleven

    /**
     * Every invocation whose text hands over a spell, held against the spell it names.
     *
     * Written out rather than derived, so this is a list a reader can check against the book.
     */
    @Test
    fun `every invocation that grants a spell grants it`() {
        listOf(
            "armor_of_shadows" to "mage_armor",
            "ascendant_step" to "levitate",
            "fiendish_vigor" to "false_life",
            "gift_of_the_depths" to "water_breathing",
            "mask_of_many_faces" to "disguise_self",
            "master_of_myriad_forms" to "alter_self",
            "misty_visions" to "silent_image",
            "one_with_shadows" to "invisibility",
            "otherworldly_leap" to "jump",
            "pact_chain" to "find_familiar",
            "visions_of_distant_realms" to "arcane_eye",
            "whispers_of_the_grave" to "speak_with_dead",
        ).forEach { (invocation, spellId) ->
            val pc = warlock(12, invocation)
            assertTrue(
                "$invocation grants $spellId and the sheet does not have it",
                spellId in spells(pc),
            )
        }
    }

    /** An invocation nobody took grants nothing. */
    @Test
    fun `an invocation not taken grants nothing`() {
        val pc = warlock(12, "devils_sight", "eldritch_mind")
        val have = spells(pc)
        listOf("mage_armor", "levitate", "find_familiar", "jump", "arcane_eye").forEach {
            assertFalse("$it arrived without the invocation that grants it", it in have)
        }
    }

    /** The spell is granted, so it is never also offered as a pick to spend. */
    @Test
    fun `a spell an invocation grants is not offered as a choice`() {
        val pc = warlock(5, "armor_of_shadows", "misty_visions", "devils_sight")
        val granted = CharacterSpells.granted(pc).map { it.spell.id }.toSet()
        val preparable = CharacterSpells.preparable(pc).map { it.id }.toSet()
        assertEquals(
            "these are granted and still offered: ${granted intersect preparable}",
            emptySet<String>(),
            granted intersect preparable,
        )
    }
}
