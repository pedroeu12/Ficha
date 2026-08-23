package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A pool's tracker has to list what the character can actually spend it on.
 *
 * Fifteen Paladin oath and Cleric domain features said "Spend Channel Divinity" and none of
 * them reached the Channel Divinity tracker: the table of options only knew about five Monk
 * subclasses. The options are derived from the subclasses themselves now, so a subclass that
 * grants a way to spend a pool cannot be added without its option appearing.
 */
class ResourceOptionsTest {

    private fun character(
        classId: String,
        subclassId: String?,
        level: Int = 20,
        classLevels: List<ClassLevel> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        classLevels = classLevels,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun optionNames(classId: String, subclassId: String?, resource: String, level: Int = 20) =
        CharacterResources.optionsFor(character(classId, subclassId, level), resource).map { it.name }

    // ------------------------------------------------------------- The reported bug

    @Test
    fun `a paladin oath puts its channel divinity option on the tracker`() {
        listOf(
            "devotion" to "Sacred Weapon",
            "vengeance" to "Vow of Enmity",
            "glory" to "Peerless Athlete",
            "ancients" to "Nature's Wrath",
            "oathbreaker" to "Conjure Undead",
        ).forEach { (oath, option) ->
            assertTrue(
                "$oath should offer $option to spend Channel Divinity on",
                optionNames("paladin", oath, "paladin:channel_divinity").contains(option),
            )
        }
    }

    @Test
    fun `a cleric domain does too`() {
        listOf(
            "life_domain" to "Preserve Life",
            "light_domain" to "Radiance of the Dawn",
            "trickery_domain" to "Invoke Duplicity",
            "war_domain" to "Guided Strike",
            "grave_domain" to "Path to the Grave",
        ).forEach { (domain, option) ->
            assertTrue(
                "$domain should offer $option",
                optionNames("cleric", domain, "cleric:channel_divinity").contains(option),
            )
        }
    }

    @Test
    fun `the class's own options are still there alongside them`() {
        val devotion = optionNames("paladin", "devotion", "paladin:channel_divinity")
        assertTrue("Divine Sense comes from the class", devotion.contains("Divine Sense"))

        val life = optionNames("cleric", "life_domain", "cleric:channel_divinity")
        assertTrue(life.contains("Divine Spark"))
        assertTrue(life.contains("Turn Undead"))
    }

    @Test
    fun `an option only appears once the level that grants it is reached`() {
        assertTrue(
            "Path to the Grave arrives at Cleric 3",
            !optionNames("cleric", "grave_domain", "cleric:channel_divinity", level = 2)
                .contains("Path to the Grave"),
        )
        assertTrue(
            optionNames("cleric", "grave_domain", "cleric:channel_divinity", level = 3)
                .contains("Path to the Grave"),
        )
    }

    // ------------------------------------------------------------- The general rule

    @Test
    fun `every subclass feature that spends a pool is listed against that pool`() {
        val spends = Regex(
            "(?:[Ss]pend|[Ee]xpend(?:ing|s)?)[^.]{0,60}?" +
                "(Channel Divinity|Bardic Inspiration|Focus Point|Sorcery Point|Wild Shape|" +
                "[Ss]uperiority [Dd]i|Rage)",
        )
        val missing = mutableListOf<String>()
        SubclassData.ALL.forEach { subclass ->
            val spenders = subclass.features.filter { spends.containsMatchIn(it.description) }
            if (spenders.isEmpty()) return@forEach
            val character = character(subclass.classId, subclass.id)
            val offered = CharacterResources.definitions(character)
                .flatMap { CharacterResources.optionsFor(character, it.id) }
                .map { it.name }
                .toSet()
            spenders.filterNot { it.name in offered }
                .forEach { missing += "${subclass.id}: ${it.name}" }
        }
        assertTrue("these can be spent but are on no tracker: $missing", missing.isEmpty())
    }

    @Test
    fun `a pool does not pick up options from another class that names it the same`() {
        // Both Cleric and Paladin call it Channel Divinity; a domain must not reach an oath.
        val paladin = optionNames("paladin", "devotion", "paladin:channel_divinity")
        assertTrue("no Cleric options on a Paladin", !paladin.contains("Preserve Life"))
        assertTrue("and none the other way", !optionNames("cleric", "life_domain", "cleric:channel_divinity")
            .contains("Sacred Weapon"))
    }

    @Test
    fun `a multiclass character sees the options of every subclass it holds`() {
        val clericPaladin = character(
            "cleric", "life_domain", level = 10,
            classLevels = listOf(
                ClassLevel("cleric", 5, subclassId = "life_domain", isStarting = true),
                ClassLevel("paladin", 5, subclassId = "devotion"),
            ),
        )
        val paladinOptions =
            CharacterResources.optionsFor(clericPaladin, "paladin:channel_divinity").map { it.name }
        assertTrue(
            "the Paladin half's oath option was invisible when only the primary subclass counted",
            paladinOptions.contains("Sacred Weapon"),
        )
    }

    @Test
    fun `a curated option is not duplicated by a derived one`() {
        val focus = optionNames("monk", "mercy", "monk:focus")
        assertEquals(
            "Hand of Harm should appear once",
            1,
            focus.count { it == "Hand of Harm" },
        )
    }
}
