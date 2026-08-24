package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CustomResource
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Anything with a limited number of uses needs a tracker, and rests have to refill it. */
class ResourceAndRestTest {

    private fun character(
        classId: String,
        level: Int = 1,
        subclassId: String? = null,
        speciesId: String = "human",
        feats: List<String> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        featIds = feats,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    private fun resourceIds(character: PlayerCharacter) =
        CharacterResources.definitions(character).map { it.id }

    // ------------------------------------------------------------------ Derivation

    @Test
    fun `monk focus points scale with level and return on a short rest`() {
        val monk = character("monk", level = 5)
        val focus = CharacterResources.definitions(monk).find { it.id == "monk:focus" }
        assertNotNull("a Monk must have a Focus Point tracker", focus)
        assertEquals(5, focus!!.max)
        assertEquals(Recharge.SHORT_REST, focus.recharge)

        // A level 1 Monk hasn't unlocked them yet.
        assertFalse("monk:focus" in resourceIds(character("monk", level = 1)))
    }

    @Test
    fun `sorcery points scale with level and return on a long rest`() {
        val sorcerer = character("sorcerer", level = 7)
        val points = CharacterResources.definitions(sorcerer).find { it.id == "sorcerer:sorcery_points" }
        assertNotNull(points)
        assertEquals(7, points!!.max)
        assertEquals(Recharge.LONG_REST, points.recharge)
    }

    @Test
    fun `a once per long rest feat gets its own tracker`() {
        val sage = character("fighter", feats = listOf("magic_initiate_wizard"))
        val tracker = CharacterResources.definitions(sage)
            .find { it.id == "feat:magic_initiate_wizard" }
        assertNotNull("Magic Initiate's free casting must be trackable", tracker)
        assertEquals(1, tracker!!.max)
        assertEquals(Recharge.LONG_REST, tracker.recharge)
    }

    @Test
    fun `lucky scales with the proficiency bonus`() {
        assertEquals(
            2,
            CharacterResources.definitions(character("fighter", 1, feats = listOf("lucky")))
                .first { it.id == "feat:lucky" }.max,
        )
        assertEquals(
            4,
            CharacterResources.definitions(character("fighter", 12, feats = listOf("lucky")))
                .first { it.id == "feat:lucky" }.max,
        )
    }

    @Test
    fun `species abilities with limited uses are tracked`() {
        assertTrue("orc:relentless_endurance" in resourceIds(character("fighter", speciesId = "orc")))
        assertTrue("dragonborn:breath_weapon" in resourceIds(character("fighter", speciesId = "dragonborn")))
        assertTrue("aasimar:healing_hands" in resourceIds(character("fighter", speciesId = "aasimar")))
    }

    @Test
    fun `subclass resources appear only once the subclass is chosen`() {
        val plain = character("fighter", level = 3)
        assertFalse("battle_master:superiority" in resourceIds(plain))

        val master = character("fighter", level = 3, subclassId = "battle_master")
        val dice = CharacterResources.definitions(master)
            .first { it.id == "battle_master:superiority" }
        assertEquals(4, dice.max)
        assertEquals(6, CharacterResources.definitions(
            character("fighter", level = 15, subclassId = "battle_master")
        ).first { it.id == "battle_master:superiority" }.max)
    }

    @Test
    fun `every class has at least one tracker by level twenty`() {
        ClassData.ALL.forEach { charClass ->
            val subclass = SubclassData.forClass(charClass.id).first().id
            val maxed = character(charClass.id, level = 20, subclassId = subclass)
            assertTrue(
                "${charClass.id} has nothing to track at level 20",
                CharacterResources.definitions(maxed).isNotEmpty(),
            )
        }
    }

    @Test
    fun `no derived resource is ever created empty or nameless`() {
        ClassData.ALL.forEach { charClass ->
            SubclassData.forClass(charClass.id).forEach { subclass ->
                (1..20).forEach { level ->
                    CharacterResources.definitions(
                        character(charClass.id, level, subclass.id, speciesId = "orc")
                    ).forEach { def ->
                        assertTrue("${def.id} has max ${def.max}", def.max > 0)
                        assertTrue("a resource has no name", def.name.isNotBlank())
                        assertTrue("${def.id} has no source", def.source.isNotBlank())
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Spending

    @Test
    fun `spending is clamped to the pool`() {
        val monk = character("monk", level = 4)
        val overspent = CharacterResources.withUsesChanged(monk, "monk:focus", 99)
        val state = CharacterResources.states(overspent).first { it.def.id == "monk:focus" }
        assertEquals(4, state.spent)
        assertEquals(0, state.remaining)
        assertTrue(state.isDepleted)

        val negative = CharacterResources.withUsesChanged(monk, "monk:focus", -5)
        assertEquals(0, negative.resourceUses["monk:focus"])
    }

    @Test
    fun `a custom tracker can be added and behaves like any other`() {
        val base = character("fighter").copy(
            customResources = listOf(
                CustomResource("custom:x", "Potion of Speed", 3, Recharge.SPECIAL.name)
            )
        )
        val custom = CharacterResources.definitions(base).first { it.id == "custom:x" }
        assertEquals(3, custom.max)
        assertEquals(Recharge.SPECIAL, custom.recharge)
        assertTrue(custom.isCustom)
    }

    @Test
    fun `an overridden maximum wins and zero hides the tracker`() {
        val monk = character("monk", level = 4)
        val raised = monk.copy(resourceMaxOverrides = mapOf("monk:focus" to 10))
        assertEquals(10, CharacterResources.definitions(raised).first { it.id == "monk:focus" }.max)

        val hidden = monk.copy(resourceMaxOverrides = mapOf("monk:focus" to 0))
        assertFalse("monk:focus" in resourceIds(hidden))
    }

    // ------------------------------------------------------------------ Rests

    /** Hit Dice spent from one class's pool, which is what a Short Rest now takes. */
    private fun dice(classId: String, vararg rolls: Int) =
        rolls.map { RestEngine.DieSpent(classId, it) }

    @Test
    fun `a short rest heals per die plus constitution and spends the dice`() {
        val con = 2 // Constitution 14
        val fighter = character("fighter", level = 5).copy(currentHitPoints = 10)
        val outcome = RestEngine.shortRest(fighter, dice("fighter", 6, 4))

        assertEquals(2, outcome.hitDiceSpent)
        assertEquals((6 + con) + (4 + con), outcome.hitPointsRegained)
        assertEquals(2, outcome.character.hitDiceSpent)
        assertEquals(10 + outcome.hitPointsRegained, outcome.character.currentHitPoints)
    }

    @Test
    fun `a short rest cannot spend more hit dice than remain`() {
        val fighter = character("fighter", level = 2).copy(
            currentHitPoints = 1,
            hitDiceSpent = 1,
        )
        val outcome = RestEngine.shortRest(fighter, dice("fighter", 5, 5, 5))
        assertEquals("only one die was left", 1, outcome.hitDiceSpent)
        assertEquals(2, outcome.character.hitDiceSpent)
    }

    @Test
    fun `a short rest never heals past the maximum`() {
        val fighter = character("fighter", level = 5)
        val max = CharacterCalculations.maxHitPoints(fighter)
        val full = fighter.copy(currentHitPoints = max)
        val outcome = RestEngine.shortRest(full, dice("fighter", 10))
        assertEquals(0, outcome.hitPointsRegained)
        assertEquals(max, outcome.character.currentHitPoints)
    }

    @Test
    fun `a short rest restores short rest resources but not long rest ones`() {
        val monk = character("monk", level = 5).copy(
            resourceUses = mapOf(
                "monk:focus" to 5,
                "monk:uncanny_metabolism" to 1,
            )
        )
        val outcome = RestEngine.shortRest(monk, emptyList())
        val after = outcome.character

        assertNull("Focus Points come back on a Short Rest", after.resourceUses["monk:focus"])
        assertEquals(
            "Uncanny Metabolism needs a Long Rest",
            1,
            after.resourceUses["monk:uncanny_metabolism"],
        )
    }

    @Test
    fun `a warlock gets pact slots back on a short rest but a wizard does not`() {
        val warlock = character("warlock", level = 5)
            .copy(spellSlotsExpended = mapOf("3" to 2))
        assertTrue(RestEngine.shortRest(warlock, emptyList()).character.spellSlotsExpended.isEmpty())

        val wizard = character("wizard", level = 5).copy(spellSlotsExpended = mapOf("1" to 4))
        assertEquals(
            mapOf("1" to 4),
            RestEngine.shortRest(wizard, emptyList()).character.spellSlotsExpended,
        )
    }

    @Test
    fun `a long rest restores everything and returns half the hit dice`() {
        val monk = character("monk", level = 10).copy(
            currentHitPoints = 1,
            temporaryHitPoints = 4,
            hitDiceSpent = 10,
            spellSlotsExpended = mapOf("1" to 2),
            resourceUses = mapOf("monk:focus" to 10, "monk:uncanny_metabolism" to 1),
        )
        val outcome = RestEngine.longRest(monk)
        val after = outcome.character

        assertEquals(CharacterCalculations.maxHitPoints(monk), after.currentHitPoints)
        assertEquals(0, after.temporaryHitPoints)
        assertEquals("half of ten hit dice come back", 5, after.hitDiceSpent)
        assertTrue(after.spellSlotsExpended.isEmpty())
        assertTrue("every tracker resets", after.resourceUses.isEmpty())
        assertEquals(0, after.deathSaves.successes)
    }

    @Test
    fun `a long rest leaves special recharge resources alone`() {
        val healer = character("fighter", feats = listOf("healer"))
            .copy(resourceUses = mapOf("feat:healer" to 4))
        val after = RestEngine.longRest(healer).character
        assertEquals(
            "a Healer's Kit is replaced, not rested",
            4,
            after.resourceUses["feat:healer"],
        )
    }

    @Test
    fun `long rest hit dice recovery is at least one`() {
        assertEquals(1, RestEngine.hitDiceRecoveredOnLongRest(character("fighter", 1)))
        assertEquals(1, RestEngine.hitDiceRecoveredOnLongRest(character("fighter", 3)))
        assertEquals(10, RestEngine.hitDiceRecoveredOnLongRest(character("fighter", 20)))
    }

    @Test
    fun `rolling a hit die stays within the die`() {
        val barbarian = character("barbarian", level = 3)
        repeat(200) {
            val roll = RestEngine.rollHitDie(CharacterCalculations.hitDie(barbarian))
            assertTrue("rolled $roll on a d12", roll in 1..12)
        }
    }
}
