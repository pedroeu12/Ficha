package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three playtest subclasses from Villainous Options 2, walked through every level.
 *
 * Each of them leans on a system built earlier in this app's life, and the point of these
 * tests is that they lean on it rather than reimplementing it: the Path of Lament's wail is a
 * limited-use pool with its own Constitution DC, the Warrior of Venom's toxins are chosen at
 * the moment of use, and the Primordial Patron's element drives an always-prepared list that
 * has to change when the element does. A subclass that half-integrates looks fine on the
 * features page and is wrong everywhere else.
 */
class VillainousOptions2Test {

    private fun character(
        classId: String,
        subclassId: String,
        level: Int,
        selections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
        levelSelections = selections,
    )

    // ------------------------------------------------------------- They exist and are marked

    @Test
    fun `all three are present and flagged as playtest material`() {
        listOf(
            "path_of_lament" to "barbarian",
            "warrior_of_venom" to "monk",
            "primordial_patron" to "warlock",
        ).forEach { (id, classId) ->
            val subclass = SubclassData.byId(id)
                ?: throw AssertionError("$id is missing")
            assertEquals("$id belongs to the wrong class", classId, subclass.classId)
            assertTrue(
                "$id must be marked as playtest so it isn't mistaken for a published option",
                subclass.isPlaytest,
            )
            assertTrue("$id names its source", subclass.source.contains("Villainous Options 2"))
        }
    }

    @Test
    fun `each grants its features at the levels the source gives`() {
        val expected = mapOf(
            "path_of_lament" to listOf(3, 6, 6, 10, 14),
            "warrior_of_venom" to listOf(3, 3, 6, 11, 11, 17),
            "primordial_patron" to listOf(3, 3, 6, 10, 14),
        )
        expected.forEach { (id, levels) ->
            val actual = SubclassData.byId(id)!!.features.map { it.level }
            assertEquals("$id feature levels", levels, actual)
        }
    }

    @Test
    fun `every feature carries its rules text, not just a name`() {
        listOf("path_of_lament", "warrior_of_venom", "primordial_patron").forEach { id ->
            SubclassData.byId(id)!!.features.forEach { feature ->
                assertTrue(
                    "$id's ${feature.name} has no description to open",
                    feature.description.length > 80,
                )
            }
        }
    }

    // ------------------------------------------------------------- Path of Lament

    @Test
    fun `the wail is a pool that grows with Constitution and refills on a long rest`() {
        // Constitution 16 is a +3 modifier, so three wails a day.
        val barbarian = character("barbarian", "path_of_lament", 3)
        val wail = CharacterResources.definitions(barbarian)
            .find { it.id == "path_of_lament:banshees_wail" }
            ?: throw AssertionError("Banshee's Wail has no tracker")

        assertEquals(3, wail.max)
        assertTrue(
            "the Rage-for-uses trade is part of the feature and belongs on the tracker",
            wail.notes.contains("Rage", ignoreCase = true),
        )
    }

    @Test
    fun `sorrow form only appears at level 14`() {
        fun hasIt(level: Int) = CharacterResources.definitions(
            character("barbarian", "path_of_lament", level)
        ).any { it.id == "path_of_lament:sorrow_form" }

        assertFalse(hasIt(13))
        assertTrue(hasIt(14))
    }

    @Test
    fun `the wail's DC comes from Constitution, not the Barbarian's Strength`() {
        val barbarian = character("barbarian", "path_of_lament", 6)
        val dcs = CharacterDcs.all(barbarian)

        val lament = dcs.find { it.id == "subclass:path_of_lament" }
            ?: throw AssertionError("Path of Lament has no save DC of its own")
        assertEquals(Ability.CON, lament.ability)
        // 8 + Constitution modifier (+3) + Proficiency Bonus (+3) at level 6.
        assertEquals(14, lament.dc)

        // And the class's own Strength DC is still there beside it, because Banshee's Wail
        // isn't the only thing a Barbarian can force a save against.
        assertTrue(
            "the Barbarian's own DC shouldn't be replaced by the subclass's",
            dcs.any { it.ability == Ability.STR },
        )
    }

    @Test
    fun `commune with the dead puts Speak with Dead on the sheet at level 6`() {
        assertTrue(
            CharacterSpells.granted(character("barbarian", "path_of_lament", 5))
                .none { it.spell.id == "speak_with_dead" }
        )
        val six = CharacterSpells.granted(character("barbarian", "path_of_lament", 6))
        assertTrue(
            "a Barbarian with Commune with the Dead should simply have the spell",
            six.any { it.spell.id == "speak_with_dead" },
        )
    }

    @Test
    fun `the origin table is offered as a choice rather than rolled for you`() {
        val choices = ChoiceResolver.subclassFeatureChoices(
            character("barbarian", "path_of_lament", 3)
        )
        val origin = choices.find { it.choice.id == "lament_origin" }
            ?: throw AssertionError("the d6 origin table is never presented")
        assertEquals(6, origin.choice.options.size)
    }

    // ------------------------------------------------------------- Warrior of Venom

    @Test
    fun `the toxins are asked at the moment of use, not at creation`() {
        val monk = character("monk", "warrior_of_venom", 6)

        val perUse = PerUseChoices.all(monk).map { it.choice.id }
        assertTrue(
            "Envenom Weapon's toxin is chosen when you apply it: $perUse",
            "warrior_of_venom:envenom_weapon" in perUse,
        )
        assertTrue(
            "Toxic Touch's effect is chosen when you use it: $perUse",
            "warrior_of_venom:toxic_touch" in perUse,
        )

        // And neither is ever put in front of the player as a permanent decision.
        val permanent = ChoiceResolver.all(monk).map { it.choice.id }
        assertTrue(
            "these turned up in a flow that asks once: $permanent",
            permanent.none { it.contains("envenom") || it.contains("toxic_touch") },
        )
    }

    @Test
    fun `toxic touch waits for level 6 and the breath for level 17`() {
        fun ids(level: Int) =
            PerUseChoices.all(character("monk", "warrior_of_venom", level)).map { it.choice.id }

        assertTrue("warrior_of_venom:envenom_weapon" in ids(3))
        assertFalse("Toxic Touch isn't gained until level 6", "warrior_of_venom:toxic_touch" in ids(3))
        assertTrue("warrior_of_venom:toxic_touch" in ids(6))
    }

    @Test
    fun `the toxins are spent from Focus Points, with their costs on the tracker`() {
        val monk = character("monk", "warrior_of_venom", 17)
        val focus = CharacterResources.definitions(monk).find { it.id == "monk:focus" }
            ?: throw AssertionError("a Monk has no Focus Points")

        val names = focus.options.map { it.name }
        listOf("Envenom Weapon", "Toxic Touch", "Hallucinogenic Breath").forEach {
            assertTrue("$it should be spendable from Focus Points: $names", it in names)
        }
        assertEquals(
            "the breath costs two, unlike everything else the subclass does",
            "2 Focus Points",
            focus.options.first { it.name == "Hallucinogenic Breath" }.cost,
        )
    }

    @Test
    fun `a level 11 Monk sees Toxin Refiner and Toxic Blood before the breath`() {
        val names = SubclassData.byId("warrior_of_venom")!!
            .features.filter { it.level <= 11 }.map { it.name }
        assertEquals(
            listOf("Envenom Weapon", "Potent Arsenal", "Toxic Touch", "Toxin Refiner", "Toxic Blood"),
            names,
        )
    }

    // ------------------------------------------------------------- Primordial Patron

    @Test
    fun `the element is asked again at every level, not fixed at three`() {
        val warlock = character(
            "warlock", "primordial_patron", 7,
            mapOf("3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("fire")),
        )
        val revisited = ChoiceResolver.levelUpChangeable(warlock).map { it.choice.id }
        assertTrue(
            "the element must come back round on every level up: $revisited",
            SubclassData.ELEMENT_CHOICE_ID in revisited,
        )
    }

    @Test
    fun `the elemental spell list follows the chosen element`() {
        fun granted(element: String, level: Int) = CharacterSpells.granted(
            character(
                "warlock", "primordial_patron", level,
                mapOf("3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf(element)),
            )
        ).map { it.spell.id }.toSet()

        val fire = granted("fire", 5)
        assertTrue("Burning Hands is on the fire list", "burning_hands" in fire)
        assertTrue("Fireball arrives at level 5", "fireball" in fire)
        assertTrue("every element shares Chromatic Orb", "chromatic_orb" in fire)
        assertTrue("Fly belongs to air, not fire", "fly" !in fire)

        val water = granted("water", 5)
        assertTrue("ice_knife" in water)
        assertTrue("water_walk" in water)
        assertTrue(
            "changing the element must take the old list away, not add to it",
            "burning_hands" !in water,
        )
    }

    @Test
    fun `changing the element on a later level replaces the whole list`() {
        // Fire at 3, water at 4. A Warlock who switched has a water list and nothing else —
        // merging the two would hand them twice the always-prepared spells they are owed.
        val switched = character(
            "warlock", "primordial_patron", 4,
            mapOf(
                "3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("fire"),
                "4:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("water"),
            ),
        )
        val ids = CharacterSpells.granted(switched).map { it.spell.id }
        assertTrue("alter_self" in ids)
        assertTrue("the fire list should be gone", "burning_hands" !in ids)
    }

    @Test
    fun `the elemental spells arrive at the levels the table gives`() {
        fun granted(level: Int) = CharacterSpells.granted(
            character(
                "warlock", "primordial_patron", level,
                mapOf("3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("earth")),
            )
        ).map { it.spell.id }.toSet()

        assertTrue("entangle" in granted(3))
        assertTrue("plant_growth" !in granted(4))
        assertTrue("plant_growth" in granted(5))
        assertTrue("vitriolic_sphere" in granted(7))
        assertTrue("wall_of_stone" in granted(9))
    }

    @Test
    fun `every elemental grant names a spell the catalog actually has`() {
        val missing = SpellGrantData.choiceGrantSpellIds().filter { SpellData.byId(it) == null }
        assertTrue("these elemental spells resolve to nothing: $missing", missing.isEmpty())
    }

    @Test
    fun `the node, its teleports and the herald each get a tracker`() {
        fun pools(level: Int) = CharacterResources.definitions(
            character("warlock", "primordial_patron", level)
        ).associateBy { it.id }

        assertTrue("primordial_patron:elemental_node" in pools(3))
        assertFalse("Elemental Haven isn't gained until 6", "primordial_patron:elemental_teleport" in pools(3))

        val six = pools(6)
        // Charisma 16 is +3, so three teleports a day.
        assertEquals(3, six["primordial_patron:elemental_teleport"]!!.max)

        val herald = pools(14)["primordial_patron:primordial_herald"]
            ?: throw AssertionError("Primordial Herald has no tracker")
        assertTrue(
            "2d4 Long Rests isn't a rest the app can refill, so it must not claim to be one",
            herald.recharge.name == "SPECIAL",
        )
    }

    @Test
    fun `Planar Ally reaches the sheet so the herald has something to cast`() {
        val warlock = character(
            "warlock", "primordial_patron", 14,
            mapOf("3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("air")),
        )
        assertTrue(
            CharacterSpells.granted(warlock).any { it.spell.id == "planar_ally" }
        )
    }

    // ------------------------------------------------------------- The new invocations

    @Test
    fun `both new invocations are on the Warlock's list`() {
        val choices = ChoiceResolver.classFeatureChoices(character("warlock", "primordial_patron", 5))
        val invocations = choices.first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
        val ids = invocations.choice.options.map { it.id }

        assertTrue("elemental_overflow" in ids)
        assertTrue("elemental_transmutation" in ids)
    }

    @Test
    fun `an invocation that names a damage type asks which one`() {
        val warlock = character(
            "warlock", "primordial_patron", 5,
            mapOf("5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("elemental_overflow")),
        )
        val asked = ChoiceResolver.originChoices(warlock).map { it.choice.id }
        assertTrue(
            "taking Elemental Overflow has to raise its damage-type question: $asked",
            "invocation:elemental_overflow:damage" in asked,
        )

        // And a Warlock who didn't take it is never asked.
        val without = character("warlock", "primordial_patron", 5)
        assertTrue(
            ChoiceResolver.originChoices(without)
                .none { it.choice.id.startsWith("invocation:elemental") }
        )
    }

    @Test
    fun `the invocation count follows the Warlock table`() {
        fun countAt(level: Int) = ChoiceResolver
            .classFeatureChoices(character("warlock", "primordial_patron", level))
            .filter { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
            .maxOf { it.choice.count }

        assertEquals(1, countAt(1))
        assertEquals(3, countAt(2))
        assertEquals(5, countAt(5))
        assertEquals(6, countAt(7))
        assertEquals(7, countAt(9))
    }

    @Test
    fun `the invocation set is one decision, restated, not one per level`() {
        // Same shape as Weapon Mastery and the Artificer's plans: taking the level 5 asking
        // must not leave the level 2 answer lying around underneath it.
        val warlock = character(
            "warlock", "primordial_patron", 5,
            mapOf(
                "2:${ProgressionData.INVOCATION_CHOICE_ID}" to
                    listOf("agonizing_blast", "devils_sight", "pact_blade"),
                "5:${ProgressionData.INVOCATION_CHOICE_ID}" to
                    listOf("agonizing_blast", "eldritch_mind", "elemental_overflow",
                        "elemental_transmutation", "mask_of_many_faces"),
            ),
        )
        val current = ChoiceResolver.latestSelectionFor(warlock, ProgressionData.INVOCATION_CHOICE_ID)
        assertEquals(5, current.size)
        assertTrue("the invocation given up at level 5 should be gone", "devils_sight" !in current)
    }

    // ------------------------------------------------------------- Reaching the Attacks tab

    @Test
    fun `the damage-dealing features reach the attacks list with their DCs`() {
        val barbarian = character("barbarian", "path_of_lament", 9)
        val wail = CharacterAttacks.all(barbarian).find { it.name == "Banshee's Wail" }
            ?: throw AssertionError("Banshee's Wail never reaches the Attacks tab")
        // Rage Damage is +3 from Barbarian level 9, so three d12s.
        assertEquals("3d12", wail.damage)
        assertEquals("Psychic", wail.damageType)
        assertTrue("the save and its DC belong on the line", wail.notes.contains("save DC"))

        val monk = character("monk", "warrior_of_venom", 17)
        assertTrue(
            CharacterAttacks.all(monk).any { it.name == "Hallucinogenic Breath" }
        )
        assertTrue(
            "the breath isn't gained until 17",
            CharacterAttacks.all(character("monk", "warrior_of_venom", 16))
                .none { it.name == "Hallucinogenic Breath" },
        )
    }

    @Test
    fun `the node's damage and type follow the level and the element`() {
        fun node(level: Int, element: String) = CharacterAttacks.all(
            character(
                "warlock", "primordial_patron", level,
                mapOf("3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf(element)),
            )
        ).first { it.name == "Elemental Node" }

        assertEquals("1d6", node(3, "fire").damage)
        assertEquals("2d6", node(6, "fire").damage)
        assertEquals("3d6", node(14, "fire").damage)

        assertEquals("Fire", node(6, "fire").damageType)
        assertEquals("Thunder", node(6, "air").damageType)
        assertEquals("Acid", node(6, "earth").damageType)
        assertEquals("Cold", node(6, "water").damageType)
    }

    @Test
    fun `an element not yet chosen claims no damage type`() {
        // Better a dash than a plausible-looking wrong element on the Attacks tab.
        val undecided = character("warlock", "primordial_patron", 3)
        assertEquals(
            "—",
            CharacterAttacks.all(undecided).first { it.name == "Elemental Node" }.damageType,
        )
    }

    @Test
    fun `none of the three leak their features onto another class`() {
        // The narrow version of CrossClassLeakTest, aimed at this batch: a Monk must never
        // see the node, a Warlock never the wail.
        val monk = character("monk", "warrior_of_venom", 17)
        val warlock = character("warlock", "primordial_patron", 14)
        val barbarian = character("barbarian", "path_of_lament", 14)

        assertTrue(CharacterAttacks.all(monk).none { it.name == "Elemental Node" })
        assertTrue(CharacterAttacks.all(warlock).none { it.name == "Banshee's Wail" })
        assertTrue(
            CharacterResources.definitions(barbarian)
                .none { it.id.startsWith("primordial_patron") || it.id.startsWith("warrior_of_venom") }
        )
        assertTrue(
            PerUseChoices.all(barbarian).none { it.choice.id.startsWith("warrior_of_venom") }
        )
    }
}
