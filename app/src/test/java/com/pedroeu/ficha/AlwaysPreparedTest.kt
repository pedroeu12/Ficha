package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The promise reaches the sheet, not just the table.
 *
 * [GrantCoverageTest] checks the data against the rules text. This checks the other end: that
 * a character built from that data actually has the spell, marked prepared, and that the
 * pickers stop offering what the character cannot not have.
 */
class AlwaysPreparedTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        selections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t", name = "T", speciesId = speciesId, classId = classId,
        subclassId = subclassId, backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        classChoiceSelections = selections,
    )

    private fun prepared(c: PlayerCharacter) =
        CharacterSpells.all(c).filter { it.prepared }.map { it.id }.toSet()

    // ------------------------------------------------------------------ Tables put right

    /**
     * Eleven subclass spell tables disagreed with the text printed above them in this repo.
     * These are one row from each, all of which the sheet was missing or getting wrong.
     */
    @Test
    fun `a subclass grants the spells its own table lists`() {
        listOf(
            Triple("druid", "sea", 3) to listOf("ray_of_frost", "thunderwave"),
            Triple("sorcerer", "aberrant", 5) to listOf("hunger_of_hadar", "sending"),
            Triple("cleric", "grave_domain", 3) to listOf("false_life", "spare_the_dying"),
            Triple("cleric", "war_domain", 3) to listOf("spiritual_weapon"),
            Triple("warlock", "archfey", 3) to listOf("sleep"),
            Triple("warlock", "celestial", 3) to listOf("light", "sacred_flame"),
            Triple("warlock", "fiend", 9) to listOf("geas", "insect_plague"),
            Triple("warlock", "undead_patron", 5) to listOf("summon_undead"),
            Triple("ranger", "fey_wanderer", 9) to listOf("summon_fey"),
            Triple("ranger", "hollow_warden", 5) to listOf("alter_self"),
            Triple("paladin", "glory", 17) to listOf("yolande_s_regal_presence"),
        ).forEach { (who, spells) ->
            val (classId, subclassId, level) = who
            val have = prepared(character(classId, level, subclassId))
            spells.forEach {
                assertTrue("$subclassId at level $level should always have $it", it in have)
            }
        }
    }

    // ------------------------------------------------------------------ Named, not tabled

    /** Features that promise one spell in a sentence rather than through a table. */
    @Test
    fun `a feature that names a spell in its text grants it too`() {
        assertTrue("guidance" in prepared(character("cleric", 3, "stars")))
        assertTrue("telekinesis" in prepared(character("fighter", 18, "psi_warrior")))
        assertTrue("hex" in prepared(character("warlock", 10, "great_old_one")))
        assertTrue("counterspell" in prepared(character("wizard", 10, "abjurer")))
        assertTrue("command" in prepared(character("bard", 6, "glamour")))
        assertTrue("summon_fiend" in prepared(character("sorcerer", 14, "demonic_sorcery")))
        assertTrue("contact_other_plane" in prepared(character("warlock", 9)))
        assertTrue("power_word_heal" in prepared(character("bard", 20)))
    }

    /** A species can promise a spell as readily as a subclass, and two of them do. */
    @Test
    fun `a species that names a spell grants it`() {
        val hexblood = prepared(character("fighter", 1, speciesId = "hexblood"))
        assertTrue("disguise_self" in hexblood && "hex" in hexblood)

        assertTrue("ray_of_frost" in prepared(character("fighter", 1, speciesId = "rimekin")))
        assertTrue("ice_knife" in prepared(character("fighter", 3, speciesId = "rimekin")))
        assertTrue("flame_blade" in prepared(character("fighter", 5, speciesId = "rimekin")))
    }

    /** A grant keyed to a level does not arrive before it. */
    @Test
    fun `a grant does not arrive before its level`() {
        assertFalse("flame_blade" in prepared(character("fighter", 4, speciesId = "rimekin")))
        assertFalse("telekinesis" in prepared(character("fighter", 17, "psi_warrior")))
    }

    // ------------------------------------------------------------------ Following an answer

    /**
     * A list that follows a choice rather than a subclass.
     *
     * The Primordial Patron's spells follow its element and the Death Domain Vestige's follow
     * the Cleric domain it borrows. Both are always-prepared, and neither can be found from
     * the subclass id alone — which is why the pickers were still offering them.
     */
    @Test
    fun `a list that follows an answer is granted and never offered`() {
        val fire = character(
            "warlock", 5, "primordial_patron",
            selections = mapOf(SubclassData.ELEMENT_CHOICE_ID to listOf("fire")),
        )
        val have = prepared(fire)
        assertTrue("burning_hands" in have && "fireball" in have)
        assertFalse("an element not chosen should grant nothing", "entangle" in have)

        val war = character(
            "warlock", 3, "vestige_patron",
            selections = mapOf("subclass:vestige_patron:domain" to listOf("War")),
        )
        val vestige = prepared(war)
        assertTrue(
            "the vestige's chosen domain grants that domain's list",
            "guiding_bolt" in vestige && "spiritual_weapon" in vestige,
        )
        assertFalse("another domain's list is not granted", "cure_wounds" in vestige)
    }

    // ------------------------------------------------------------------ Not offered twice

    /** What the rules hand over is never also a pick to spend. */
    @Test
    fun `a granted spell is never offered as a choice`() {
        val warlock = character(
            "warlock", 5, "primordial_patron",
            selections = mapOf(SubclassData.ELEMENT_CHOICE_ID to listOf("fire")),
        )
        val granted = CharacterSpells.granted(warlock).map { it.spell.id }.toSet()
        val preparable = CharacterSpells.preparable(warlock).map { it.id }.toSet()
        assertTrue(
            "these are granted and still offered: ${granted intersect preparable}",
            (granted intersect preparable).isEmpty(),
        )
    }
}
