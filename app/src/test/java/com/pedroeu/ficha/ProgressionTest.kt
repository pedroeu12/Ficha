package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.levelup.AsiMode
import com.pedroeu.ficha.ui.levelup.LevelUpState
import com.pedroeu.ficha.ui.levelup.LevelUpStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Levelling has to work for every class at every level, not just add one to a number. */
class ProgressionTest {

    private fun character(classId: String, level: Int = 1) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 15 },
        // Every real character is trained in something: a class grants skills and so does a
        // background. Without them, Expertise — which offers the skills you are proficient
        // in — had nothing to offer, and the test was measuring a character that cannot exist.
        skillProficiencies = setOf(
            Skill.ATHLETICS.name, Skill.PERCEPTION.name,
            Skill.STEALTH.name, Skill.INSIGHT.name,
        ),
    )

    @Test
    fun `every class has a progression table and at least one subclass`() {
        ClassData.ALL.forEach { charClass ->
            val progression = ProgressionData.forClass(charClass.id)
            assertNotNull("no progression for ${charClass.id}", progression)
            assertTrue(
                "no subclasses for ${charClass.id}",
                SubclassData.forClass(charClass.id).isNotEmpty(),
            )
        }
    }

    @Test
    fun `every class picks its subclass at level 3`() {
        ProgressionData.ALL.forEach { progression ->
            assertEquals(
                "${progression.classId} should choose a subclass at 3",
                3,
                progression.subclassLevel,
            )
        }
    }

    @Test
    fun `proficiency bonus scales on the standard curve`() {
        val expected = mapOf(1 to 2, 4 to 2, 5 to 3, 8 to 3, 9 to 4, 12 to 4, 13 to 5, 17 to 6, 20 to 6)
        expected.forEach { (level, bonus) ->
            assertEquals(
                "level $level",
                bonus,
                CharacterCalculations.proficiencyBonus(character("fighter", level)),
            )
        }
    }

    @Test
    fun `full caster slots match the published table`() {
        assertEquals(mapOf(1 to 2), SpellSlotTables.slotsFor(CasterType.FULL, 1))
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 2), SpellSlotTables.slotsFor(CasterType.FULL, 5))
        val twenty = SpellSlotTables.slotsFor(CasterType.FULL, 20)
        assertEquals(4, twenty[1])
        assertEquals(2, twenty[6])
        assertEquals(1, twenty[9])
    }

    @Test
    fun `half casters cast from level 1 and then lag behind`() {
        // The 2024 Paladin and Ranger both gain Spellcasting at level 1 with two slots; the
        // older tables that started them at level 2 left a level 1 Ranger unable to cast.
        assertEquals(mapOf(1 to 2), SpellSlotTables.slotsFor(CasterType.HALF, 1))
        assertEquals(mapOf(1 to 2), SpellSlotTables.slotsFor(CasterType.HALF, 2))
        assertEquals(mapOf(1 to 3), SpellSlotTables.slotsFor(CasterType.HALF, 3))
        assertEquals(mapOf(1 to 4, 2 to 2), SpellSlotTables.slotsFor(CasterType.HALF, 5))
        assertEquals(mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 3, 5 to 2), SpellSlotTables.slotsFor(CasterType.HALF, 20))
        // A half caster still trails a full caster of the same level.
        assertTrue(
            SpellSlotTables.maxSpellLevel(CasterType.HALF, 9) <
                SpellSlotTables.maxSpellLevel(CasterType.FULL, 9)
        )
    }

    @Test
    fun `pact magic grants few slots always at the highest level`() {
        assertEquals(mapOf(1 to 1), SpellSlotTables.slotsFor(CasterType.PACT, 1))
        assertEquals(mapOf(2 to 2), SpellSlotTables.slotsFor(CasterType.PACT, 3))
        assertEquals(mapOf(5 to 3), SpellSlotTables.slotsFor(CasterType.PACT, 11))
        assertEquals(mapOf(5 to 4), SpellSlotTables.slotsFor(CasterType.PACT, 20))
    }

    @Test
    fun `fighters and rogues get their extra ability score improvements`() {
        val fighter = ProgressionData.forClass("fighter")!!
        assertTrue(fighter.grantsAsiAt(6))
        assertTrue(fighter.grantsAsiAt(14))

        val rogue = ProgressionData.forClass("rogue")!!
        assertTrue(rogue.grantsAsiAt(10))

        val wizard = ProgressionData.forClass("wizard")!!
        assertFalse(wizard.grantsAsiAt(6))
        assertFalse(wizard.grantsAsiAt(10))
    }

    @Test
    fun `level 19 grants an epic boon rather than a normal improvement`() {
        ProgressionData.ALL.forEach { progression ->
            assertTrue("${progression.classId}", progression.grantsEpicBoonAt(19))
            assertFalse("${progression.classId}", progression.grantsAsiAt(19))
        }
    }

    @Test
    fun `a level up always covers hit points and ends on the summary`() {
        ClassData.ALL.forEach { charClass ->
            (1..19).forEach { level ->
                val state = LevelUpState(character = character(charClass.id, level))
                // The class step leads whenever the character could multiclass; hit points
                // always follow, and the summary always closes.
                assertEquals(
                    "${charClass.id} L$level should open on the class or hit point step",
                    if (state.steps.first() == LevelUpStep.CLASS) {
                        LevelUpStep.CLASS
                    } else {
                        LevelUpStep.HIT_POINTS
                    },
                    state.steps.first(),
                )
                assertTrue(
                    "${charClass.id} L$level must still ask about hit points",
                    state.steps.contains(LevelUpStep.HIT_POINTS),
                )
                assertEquals(LevelUpStep.SUMMARY, state.steps.last())
                assertEquals(
                    "${charClass.id} L$level has duplicate steps",
                    state.steps.size,
                    state.steps.distinct().size,
                )
            }
        }
    }

    @Test
    fun `reaching level 3 always asks for a subclass`() {
        ClassData.ALL.forEach { charClass ->
            val state = LevelUpState(character = character(charClass.id, 2))
            assertTrue("${charClass.id} must pick a subclass at 3", state.gainsSubclass)
            assertTrue(state.steps.contains(LevelUpStep.SUBCLASS))
            assertFalse("cannot advance without choosing", state.copy(requestedStep = LevelUpStep.SUBCLASS).canAdvance)
            assertTrue(
                state.copy(requestedStep = LevelUpStep.SUBCLASS, subclassId = state.subclassOptions.first().id)
                    .canAdvance
            )
        }
    }

    @Test
    fun `an ability score improvement level demands two full points or a feat`() {
        val state = LevelUpState(character = character("wizard", 3), requestedStep = LevelUpStep.ASI)
        assertTrue(state.grantsAsi)
        assertFalse("nothing assigned yet", state.canAdvance)

        assertTrue(state.copy(asiMode = AsiMode.PLUS_TWO, asiPoints = mapOf(Ability.INT to 2)).canAdvance)
        assertFalse(state.copy(asiMode = AsiMode.PLUS_TWO, asiPoints = mapOf(Ability.INT to 1)).canAdvance)
        assertTrue(
            state.copy(
                asiMode = AsiMode.PLUS_ONE_ONE,
                asiPoints = mapOf(Ability.INT to 1, Ability.CON to 1),
            ).canAdvance
        )
        assertFalse(
            "one ability can't take both points in the split mode",
            state.copy(asiMode = AsiMode.PLUS_ONE_ONE, asiPoints = mapOf(Ability.INT to 2)).canAdvance
        )
        assertTrue(
            state.copy(asiMode = AsiMode.FEAT, featId = state.featOptions.first().id).canAdvance
        )
        assertFalse(state.copy(asiMode = AsiMode.FEAT, featId = null).canAdvance)
    }

    @Test
    fun `every choice a level up presents can actually be answered`() {
        ClassData.ALL.forEach { charClass ->
            val subclass = SubclassData.forClass(charClass.id).first()
            (1..19).forEach { level ->
                val state = LevelUpState(
                    character = character(charClass.id, level).copy(
                        subclassId = if (level >= 3) subclass.id else null
                    )
                )
                state.featureChoices.forEach { choice ->
                    assertTrue(
                        "${charClass.id} L${state.targetLevel}: '${choice.id}' has no options",
                        choice.options.isNotEmpty(),
                    )
                    assertTrue(
                        "${charClass.id} L${state.targetLevel}: '${choice.id}' asks for " +
                            "${choice.count} of ${choice.options.size}",
                        choice.count <= choice.options.size,
                    )
                }
            }
        }
    }

    @Test
    fun `casters are always offered enough cantrips to fill their new slots`() {
        listOf("bard", "cleric", "druid", "sorcerer", "warlock", "wizard").forEach { classId ->
            (1..19).forEach { level ->
                val state = LevelUpState(character = character(classId, level))
                if (state.cantripsToLearn > 0) {
                    assertTrue(
                        "$classId L${state.targetLevel}: needs ${state.cantripsToLearn} cantrips " +
                            "but only ${state.cantripOptions.size} are available",
                        state.cantripOptions.size >= state.cantripsToLearn,
                    )
                }
            }
        }
    }

    @Test
    fun `the catalog now covers every level a caster can reach`() {
        // It used to stop at 5, and a level 11 wizard fell off the end into a free-text box.
        val high = LevelUpState(character = character("wizard", 10))
        assertTrue("a wizard reaches level 6 slots here", high.maxSpellLevel > 5)
        assertTrue(
            "no caster can out-level the catalog any more",
            high.maxSpellLevel <= SpellData.MAX_CATALOGUED_LEVEL,
        )
        assertFalse("so the free-text box is not needed", high.needsManualSpellEntry)

        val low = LevelUpState(character = character("wizard", 2))
        assertFalse(low.needsManualSpellEntry)
    }

    @Test
    fun `hit points gained follow the chosen method and never drop below one`() {
        val state = LevelUpState(character = character("barbarian", 4))
        assertEquals(12, state.hitDie)
        assertEquals(7, state.averageHitPoints)
        assertEquals(7, state.hitPointsGained)

        val rolled = state.copy(
            hitPointMethod = com.pedroeu.ficha.ui.levelup.HitPointMethod.ROLL,
            rolledHitPoints = 3,
        )
        assertEquals(3, rolled.hitPointsGained)

        val manual = state.copy(
            hitPointMethod = com.pedroeu.ficha.ui.levelup.HitPointMethod.MANUAL,
            manualHitPoints = 0,
        )
        assertTrue(manual.hitPointsGained >= 1)
    }

    @Test
    fun `recorded hit point rolls feed the maximum`() {
        val con = CharacterCalculations.abilityModifiers(character("fighter"))[Ability.CON]!!
        val atOne = character("fighter", 1)
        val atThree = character("fighter", 3).copy(hitPointsPerLevel = listOf(10, 10))

        assertEquals(10 + con, CharacterCalculations.maxHitPoints(atOne))
        assertEquals(10 + con + (10 + con) + (10 + con), CharacterCalculations.maxHitPoints(atThree))
    }

    @Test
    fun `subclasses only ever reference their own class`() {
        SubclassData.ALL.forEach { subclass ->
            assertNotNull(
                "${subclass.id} belongs to unknown class ${subclass.classId}",
                ClassData.byId(subclass.classId),
            )
            assertTrue("${subclass.id} has no features", subclass.features.isNotEmpty())
        }
    }

    @Test
    fun `the playtest subclasses from the unearthed arcana document are present`() {
        val expected = listOf(
            "arcane_archer" to "fighter",
            "tattooed_warrior" to "monk",
            "conjurer" to "wizard",
            "enchanter" to "wizard",
            "necromancer" to "wizard",
            "transmuter" to "wizard",
        )
        expected.forEach { (id, classId) ->
            val subclass = SubclassData.byId(id)
            assertNotNull("missing playtest subclass $id", subclass)
            assertEquals(classId, subclass!!.classId)
            assertTrue("$id should be flagged as playtest material", subclass.isPlaytest)
        }
    }
}
