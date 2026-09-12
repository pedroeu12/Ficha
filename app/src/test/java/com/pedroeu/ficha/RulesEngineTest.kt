package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGraph
import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.domain.PassiveBonuses
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.LevelScope
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.SpellGrantMode
import com.pedroeu.ficha.rules.StatTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 0: the engine reproduces what the nine tables already produce.
 *
 * The engine is built behind adapters that read the existing content, so it can be proven
 * against what is there before a line of it is rewritten. Every check below holds the engine's
 * answer against the answer the old path gives for the same character. When they agree for
 * every character shape that matters, the readers can be switched over with the existing 707
 * tests as the safety net — which is Phase 1.
 *
 * A migration that started by rewriting content would have had nothing to compare against.
 */
class RulesEngineTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        lineageId: String? = null,
        featIds: List<String> = emptyList(),
        selections: Map<String, List<String>> = emptyMap(),
        extraClasses: List<Triple<String, Int, String?>> = emptyList(),
    ): PlayerCharacter {
        val base = PlayerCharacter(
            id = "t", name = "T", speciesId = speciesId, lineageId = lineageId,
            classId = classId, subclassId = subclassId, backgroundId = "soldier",
            level = level + extraClasses.sumOf { it.second },
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            featIds = featIds,
            classChoiceSelections = selections,
        )
        if (extraClasses.isEmpty()) return base
        return base.copy(
            classLevels = listOf(
                com.pedroeu.ficha.domain.ClassLevel(classId, level, subclassId, isStarting = true)
            ) + extraClasses.map { (id, lvl, sub) ->
                com.pedroeu.ficha.domain.ClassLevel(id, lvl, sub)
            },
        )
    }

    /** The spread of characters every conformance check runs against. */
    private fun cast(): List<Pair<String, PlayerCharacter>> = listOf(
        "warlock 5 fiend" to character("warlock", 5, "fiend"),
        "warlock 12 primordial" to character(
            "warlock", 12, "primordial_patron",
            selections = mapOf(SubclassData.ELEMENT_CHOICE_ID to listOf("fire")),
        ),
        "cleric 9 life" to character("cleric", 9, "life_domain"),
        "wizard 10 abjurer" to character("wizard", 10, "abjurer"),
        "druid 7 sea" to character("druid", 7, "sea"),
        "fighter 18 psi warrior" to character("fighter", 18, "psi_warrior"),
        "monk 6" to character("monk", 6),
        "rogue 11 thief" to character("rogue", 11, "thief"),
        "artificer 10 armorer" to character("artificer", 10, "armorer"),
        "paladin 13 devotion" to character("paladin", 13, "devotion"),
        "barbarian 8 world tree" to character("barbarian", 8, "world_tree"),
        "hexblood fighter" to character("fighter", 4, speciesId = "hexblood"),
        "high elf wizard" to character(
            "wizard", 3, "evoker", speciesId = "elf", lineageId = "high_elf",
        ),
        "feats" to character("fighter", 8, featIds = listOf("tough", "magic_initiate_wizard")),
        // The shape that breaks anything re-deriving level scope by hand.
        "cleric 3 / fighter 9" to character(
            "cleric", 3, "life_domain",
            extraClasses = listOf(Triple("fighter", 9, "champion")),
        ),
        "warlock 5 / fighter 6" to character(
            "warlock", 5, "fiend",
            extraClasses = listOf(Triple("fighter", 6, "champion")),
        ),
    )

    // ================================================================ Conformance

    @Test
    fun `the engine grants the same spells the old path grants`() {
        cast().forEach { (who, pc) ->
            val fromEngine = RulesEngine.grantedSpells(pc)
                .filter { it.effect.mode != SpellGrantMode.ADDED_TO_CLASS_LIST }
                .map { it.effect.spellId }
                .toSet()
            val fromTables = CharacterSpells.granted(pc).map { it.spell.id }.toSet()
            assertEquals("$who: granted spells differ", fromTables, fromEngine intersect fromTables)
            assertTrue(
                "$who: the engine grants spells the old path does not: ${fromEngine - fromTables}",
                (fromEngine - fromTables).isEmpty(),
            )
        }
    }

    @Test
    fun `the engine finds the same limited-use pools`() {
        cast().forEach { (who, pc) ->
            val fromEngine = RulesEngine.pools(pc).map { it.effect.poolId }.toSet()
            val fromTables = CharacterResources.definitions(pc)
                .filterNot { it.isCustom }
                .map { it.id }
                .toSet()
            assertEquals("$who: pools differ", fromTables, fromEngine)
        }
    }

    @Test
    fun `the engine works out the same maximum for every pool`() {
        cast().forEach { (who, pc) ->
            CharacterResources.definitions(pc).filterNot { it.isCustom }.forEach { def ->
                assertEquals(
                    "$who: ${def.id} maximum differs",
                    def.max,
                    RulesEngine.poolMax(pc, def.id),
                )
            }
        }
    }

    @Test
    fun `the engine finds the same save DCs`() {
        cast().forEach { (who, pc) ->
            val fromEngine = RulesEngine.saveDcs(pc).map { it.effect.id }.toSet()
            val fromTables = CharacterDcs.all(pc).map { it.id }.toSet()
            assertTrue(
                "$who: the old path has DCs the engine misses: ${fromTables - fromEngine}",
                (fromTables - fromEngine).isEmpty(),
            )
        }
    }

    @Test
    fun `the engine adds the same passive bonuses`() {
        cast().forEach { (who, pc) ->
            listOf(
                StatTarget.ARMOR_CLASS to PassiveBonusData.Target.ARMOR_CLASS,
                StatTarget.MAX_HIT_POINTS to PassiveBonusData.Target.MAX_HIT_POINTS,
                StatTarget.SPEED to PassiveBonusData.Target.SPEED,
                StatTarget.INITIATIVE to PassiveBonusData.Target.INITIATIVE,
            ).forEach { (engineTarget, oldTarget) ->
                assertEquals(
                    "$who: $engineTarget differs",
                    PassiveBonuses.totalFor(pc, oldTarget),
                    RulesEngine.statBonus(pc, engineTarget),
                )
            }
        }
    }

    @Test
    fun `the engine asks every question the choice walk asks`() {
        cast().forEach { (who, pc) ->
            val fromEngine = RulesEngine.choicesOnGain(pc).map { it.effect.choice.id }.toSet()
            val fromWalk = ChoiceGraph.forCharacter(pc).map { it.id }.toSet()
            assertTrue(
                "$who: the walk asks what the engine does not: ${fromWalk - fromEngine}",
                (fromWalk - fromEngine).isEmpty(),
            )
        }
    }

    // ================================================================ The level scope

    /**
     * The distinction the app used to re-derive in five places, now one field.
     *
     * A Cleric 3 / Fighter 9 has a level 3 domain list and not a level 9 one, and a Warlock 5
     * / Fighter 6 has a level 5 Warlock's invocations. Both of those were real bugs, in
     * different files, from the same idea spelled differently.
     */
    @Test
    fun `a class feature counts its own class's level, not the character's`() {
        val multi = character(
            "cleric", 3, "life_domain",
            extraClasses = listOf(Triple("fighter", 9, "champion")),
        )
        val granted = RulesEngine.grantedSpells(multi).map { it.effect.spellId }.toSet()
        assertTrue("the level 3 domain list should be here", "bless" in granted)
        assertTrue(
            "a level 9 domain spell must not arrive on a Cleric 3",
            "greater_restoration" !in granted,
        )
    }

    @Test
    fun `a species trait counts the character's total level`() {
        val multi = character(
            "fighter", 4, speciesId = "rimekin",
            extraClasses = listOf(Triple("wizard", 1, null)),
        )
        // Character level 5, so the Rimekin's level 5 grant has arrived even though neither
        // class is level 5.
        val granted = RulesEngine.grantedSpells(multi).map { it.effect.spellId }.toSet()
        assertTrue("flame_blade" in granted)
    }

    @Test
    fun `every element names the level it counts against`() {
        cast().forEach { (who, pc) ->
            RulesEngine.elementsFor(pc).forEach { element ->
                if (element.gate.levelScope == LevelScope.OWNING_CLASS && element.gate.level > 1) {
                    assertTrue(
                        "$who: ${element.id} counts a class level but names no class",
                        element.source.owningClassId != null,
                    )
                }
            }
        }
    }

    // ================================================================ The shape itself

    @Test
    fun `no element is empty and none is duplicated`() {
        cast().forEach { (who, pc) ->
            val elements = RulesEngine.elementsFor(pc)
            assertEquals(
                "$who: two elements share an id",
                elements.map { it.id }.distinct().size,
                elements.size,
            )
            elements.forEach {
                assertTrue("$who: ${it.id} has a blank name", it.name.isNotBlank())
            }
        }
    }

    /**
     * A question raised by an answer is reachable from the engine, not only from the walk.
     *
     * Picking Agonizing Blast has to raise the cantrip question through the engine too, or
     * switching the readers over in Phase 1 would quietly lose it.
     */
    @Test
    fun `an answered option raises its own questions through the engine`() {
        val warlock = character("warlock", 5, "fiend").copy(
            levelSelections = mapOf(
                "5:${ProgressionData.INVOCATION_CHOICE_ID}" to
                    listOf("agonizing_blast", "pact_tome", "devils_sight"),
            ),
        )
        val asked = RulesEngine.choicesOnGain(warlock).map { it.effect.choice.id }.toSet()
        assertTrue("invocation:agonizing_blast:cantrip" in asked)
        assertTrue("invocation:pact_tome:cantrips" in asked)
        assertTrue("invocation:pact_tome:ritual" in asked)
    }

    @Test
    fun `the engine offers a choose-on-use decision where the old table does`() {
        val barbarian = character("barbarian", 5, "spiritual_guardian")
        val fromEngine = RulesEngine.choicesOnUse(barbarian).map { it.effect.choice.id }
        assertTrue(
            "the Spiritual Guardian's on-use choice is missing: $fromEngine",
            fromEngine.any { it.startsWith("spiritual_guardian:") },
        )
    }
}
