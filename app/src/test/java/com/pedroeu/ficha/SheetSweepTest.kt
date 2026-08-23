package com.pedroeu.ficha

import com.pedroeu.ficha.data.CharacterBackup
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.RestEngine
import com.pedroeu.ficha.ui.levelup.LevelUpState
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every sheet the app can produce, opened and exercised.
 *
 * The individual tests elsewhere each check one rule on one character. This walks the whole
 * space instead — every class, every subclass, every species and lineage, a spread of
 * multiclass pairs, and every level-up from 1 to 20 — and asks only that nothing throws and
 * no pool comes out nonsensical. It is the check that catches a combination nobody thought
 * to write a test for.
 */
class SheetSweepTest {

    private val problems = mutableListOf<String>()

    private fun character(
        classId: String,
        subclassId: String?,
        level: Int,
        speciesId: String = "human",
        lineageId: String? = null,
        classLevels: List<ClassLevel> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        lineageId = lineageId,
        classId = classId,
        subclassId = subclassId,
        classLevels = classLevels,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    /** Opens every derived view of a character and records anything that blows up. */
    private fun exercise(label: String, character: PlayerCharacter) {
        fun guard(what: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Throwable) {
                problems += "$label $what: $e"
            }
        }
        guard("resources") { CharacterResources.states(character) }
        guard("spells") { CharacterSpells.all(character) }
        guard("choices") { ChoiceResolver.all(character) }
        guard("attacks") { CharacterAttacks.all(character) }
        guard("save DCs") { CharacterDcs.all(character) }
        guard("armour class") { CharacterCalculations.armorClass(character) }
        guard("hit points") { CharacterCalculations.maxHitPoints(character) }
        guard("spell slots") { CharacterCalculations.spellSlots(character) }
        guard("short rest") { RestEngine.shortRest(character, List(20) { 5 }) }
        guard("long rest") { RestEngine.longRest(character) }
        guard("backup round trip") {
            val restored = CharacterBackup
                .decode(CharacterBackup.encode(listOf(character), "test", 0L))
                .getOrNull()
            if (restored?.singleOrNull()?.id != character.id) {
                problems += "$label backup round trip lost the character"
            }
        }
    }

    @Test
    fun `every class and subclass opens at the levels where things change`() {
        ClassData.ALL.forEach { charClass ->
            listOf(1, 2).forEach { level ->
                exercise("${charClass.id} L$level", character(charClass.id, null, level))
            }
            SubclassData.forClass(charClass.id).forEach { subclass ->
                listOf(3, 5, 11, 20).forEach { level ->
                    exercise(
                        "${charClass.id}/${subclass.id} L$level",
                        character(charClass.id, subclass.id, level),
                    )
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `every species and lineage opens`() {
        SpeciesData.ALL.forEach { species ->
            exercise("species ${species.id}", character("fighter", "champion", 8, species.id))
            species.lineageOptions.forEach { lineage ->
                exercise(
                    "lineage ${species.id}/${lineage.id}",
                    character("fighter", "champion", 8, species.id, lineage.id),
                )
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `multiclass pairs open`() {
        listOf(
            "wizard" to "cleric", "paladin" to "warlock", "fighter" to "rogue",
            "bard" to "druid", "artificer" to "sorcerer", "monk" to "barbarian",
        ).forEach { (first, second) ->
            exercise(
                "$first/$second",
                character(
                    first, null, 10,
                    classLevels = listOf(
                        ClassLevel(first, 5, isStarting = true),
                        ClassLevel(second, 5),
                    ),
                ),
            )
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `every level up from 1 to 20 builds its steps`() {
        ClassData.ALL.forEach { charClass ->
            (1..19).forEach { level ->
                val subclass = SubclassData.forClass(charClass.id).firstOrNull()?.id
                    .takeIf { level >= 3 }
                try {
                    LevelUpState(character = character(charClass.id, subclass, level)).steps
                } catch (e: Throwable) {
                    problems += "level up ${charClass.id} $level to ${level + 1}: $e"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `no pool is empty or recharges on nothing without saying so`() {
        val odd = mutableListOf<String>()
        ClassData.ALL.forEach { charClass ->
            SubclassData.forClass(charClass.id).forEach { subclass ->
                CharacterResources.definitions(character(charClass.id, subclass.id, 20))
                    .forEach { pool ->
                        if (pool.max <= 0) {
                            odd += "${charClass.id}/${subclass.id} ${pool.id} has ${pool.max} uses"
                        }
                        if (pool.recharge == Recharge.SPECIAL && pool.notes.isBlank()) {
                            odd += "${charClass.id}/${subclass.id} ${pool.id} never says how it comes back"
                        }
                    }
            }
        }
        assertTrue(odd.distinct().joinToString("\n"), odd.isEmpty())
    }
}
