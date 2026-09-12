package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ModifierData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.rules.Condition
import com.pedroeu.ficha.rules.ConditionEval
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.RulesEngine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Numbers that only apply in some circumstances.
 *
 * These were the last arithmetic left inside the calculations themselves: five readings of
 * Unarmored Defense, two of "while unarmoured", one Speed table and one feat, each written
 * where it happened to be needed. Nothing compared them to each other, so one of the five was
 * wrong for years — a Monk holding a Shield kept Unarmored Defense and lost the Shield.
 *
 * Now every one of them is a row in [ModifierData] hanging off the feature that grants it.
 * This pins the arithmetic that came out of that, and the last test in the file fails the
 * build if a new one gets written back into the calculation instead.
 */
class ConditionalModifierTest {

    private fun character(
        classId: String = "fighter",
        subclassId: String? = null,
        level: Int = 1,
        speciesId: String = "human",
        lineageId: String? = null,
        dex: Int = 14,
        con: Int = 10,
        wis: Int = 10,
        cha: Int = 10,
        inventory: List<InventoryItem> = emptyList(),
        featIds: List<String> = emptyList(),
        classChoices: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        lineageId = lineageId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = mapOf(
            Ability.STR.name to 10,
            Ability.DEX.name to dex,
            Ability.CON.name to con,
            Ability.WIS.name to wis,
            Ability.CHA.name to cha,
            Ability.INT.name to 10,
        ),
        inventory = inventory,
        featIds = featIds,
        classChoiceSelections = classChoices,
    )

    private fun shield() = InventoryItem(name = "Shield", armorDefId = "shield", equipped = true)
    private fun leather() =
        InventoryItem(name = "Leather Armor", armorDefId = "leather", equipped = true)
    private fun plate() = InventoryItem(name = "Plate Armor", armorDefId = "plate", equipped = true)

    // ------------------------------------------------------------------ Unarmored Defense

    @Test
    fun `a monk with a shield gets the shield, not unarmored defense`() {
        // The bug the migration found. Monk Unarmored Defense reads "while you aren't wearing
        // armor or wielding a Shield" — with a Shield it does not apply at all. The old code
        // applied it anyway and withheld the Shield's +2 instead, which is a different number
        // whenever Wisdom beats the Shield.
        val bare = character(classId = "monk", wis = 16)
        val shielded = character(classId = "monk", wis = 16, inventory = listOf(shield()))

        assertEquals("10 + Dex 2 + Wis 3", 15, CharacterCalculations.armorClass(bare))
        assertEquals(
            "no Unarmored Defense with a Shield: 10 + Dex 2 + Shield 2",
            14,
            CharacterCalculations.armorClass(shielded),
        )
    }

    @Test
    fun `a barbarian keeps unarmored defense while holding a shield`() {
        // The Barbarian's wording deliberately allows it: "You can use a Shield and still
        // gain this benefit."
        val barbarian = character(classId = "barbarian", con = 16, inventory = listOf(shield()))
        assertEquals("10 + Dex 2 + Con 3 + Shield 2", 17, CharacterCalculations.armorClass(barbarian))
    }

    @Test
    fun `worn armor wins when it is better than unarmored defense`() {
        val barbarian = character(classId = "barbarian", con = 16, inventory = listOf(plate()))
        // Plate is 18 flat against Unarmored Defense's 15, and the two never stack.
        assertEquals(18, CharacterCalculations.armorClass(barbarian))
    }

    @Test
    fun `unarmored defense stops the moment armor goes on`() {
        val con16 = character(classId = "barbarian", con = 16, inventory = listOf(leather()))
        // Leather 11 + Dex 2 = 13, not 10 + Dex + Con.
        assertEquals(13, CharacterCalculations.armorClass(con16))
    }

    @Test
    fun `a draconic sorcerer gets scales at subclass level, not before`() {
        val two = character(classId = "sorcerer", level = 2, cha = 16)
        val three = character(classId = "sorcerer", subclassId = "draconic", level = 3, cha = 16)
        assertEquals(12, CharacterCalculations.armorClass(two))
        assertEquals("10 + Dex 2 + Cha 3", 15, CharacterCalculations.armorClass(three))
    }

    @Test
    fun `dazzling footwork needs no shield, like the monk's`() {
        val dancer = character(classId = "bard", subclassId = "dance", level = 3, cha = 16)
        val withShield = character(
            classId = "bard", subclassId = "dance", level = 3, cha = 16,
            inventory = listOf(shield()),
        )
        assertEquals(15, CharacterCalculations.armorClass(dancer))
        assertEquals("10 + Dex 2 + Shield 2", 14, CharacterCalculations.armorClass(withShield))
    }

    @Test
    fun `infernal bulwark uses whichever ability the feat raised`() {
        // "…plus the modifier of the ability increased by this feat." The feat offers
        // Constitution or Charisma, so the number only exists once the player has answered.
        val raisedCon = character(
            classId = "warlock", level = 4, con = 15, cha = 10,
            featIds = listOf("infernal_bulwark"),
            classChoices = mapOf("feat:infernal_bulwark:ability" to listOf(Ability.CON.name)),
        )
        // Con 15 + the feat's own +1 = 16, a +3 modifier.
        assertEquals("10 + Dex 2 + Con 3", 15, CharacterCalculations.armorClass(raisedCon))

        val unanswered = character(
            classId = "warlock", level = 4, con = 15, cha = 10,
            featIds = listOf("infernal_bulwark"),
        )
        assertEquals(
            "nothing chosen yet, so nothing applied",
            12,
            CharacterCalculations.armorClass(unanswered),
        )
    }

    // ------------------------------------------------------------------ Speed

    @Test
    fun `fast movement stops in heavy armor only`() {
        val bare = character(classId = "barbarian", level = 5)
        val medium = character(
            classId = "barbarian", level = 5,
            inventory = listOf(InventoryItem("Hide", armorDefId = "hide", equipped = true)),
        )
        val heavy = character(classId = "barbarian", level = 5, inventory = listOf(plate()))

        assertEquals(40, CharacterCalculations.speed(bare))
        assertEquals("Hide is Medium, so Fast Movement stays", 40, CharacterCalculations.speed(medium))
        assertEquals(30, CharacterCalculations.speed(heavy))
    }

    @Test
    fun `unarmored movement needs neither armor nor shield`() {
        assertEquals(40, CharacterCalculations.speed(character(classId = "monk", level = 2)))
        assertEquals(
            30,
            CharacterCalculations.speed(
                character(classId = "monk", level = 2, inventory = listOf(shield()))
            ),
        )
        assertEquals(
            30,
            CharacterCalculations.speed(
                character(classId = "monk", level = 2, inventory = listOf(leather()))
            ),
        )
    }

    @Test
    fun `unarmored movement climbs on the monk's own table`() {
        val expected = mapOf(1 to 30, 2 to 40, 5 to 40, 6 to 45, 10 to 50, 14 to 55, 18 to 60, 20 to 60)
        expected.forEach { (level, speed) ->
            assertEquals(
                "a level $level Monk",
                speed,
                CharacterCalculations.speed(character(classId = "monk", level = level)),
            )
        }
    }

    @Test
    fun `a monk who is also a barbarian gets both, each on its own level`() {
        val both = PlayerCharacter(
            id = "t", name = "Test", speciesId = "human",
            classId = "monk", backgroundId = "soldier", level = 11,
            classLevels = listOf(
                ClassLevel("monk", 6, "open_hand", isStarting = true),
                ClassLevel("barbarian", 5, "berserker"),
            ),
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        )
        // 30 base + Unarmored Movement 15 (Monk 6) + Fast Movement 10 (Barbarian 5).
        assertEquals(55, CharacterCalculations.speed(both))
    }

    @Test
    fun `the wood elf's speed is a base, not a bonus`() {
        val woodElf = character(speciesId = "elf", lineageId = "wood_elf")
        val highElf = character(speciesId = "elf", lineageId = "high_elf")
        assertEquals(35, CharacterCalculations.speed(woodElf))
        assertEquals(30, CharacterCalculations.speed(highElf))

        // A Monk Wood Elf gets 35 + 10, not 30 + 5 + 10 by some other route.
        val monkElf = character(
            classId = "monk", level = 2, speciesId = "elf", lineageId = "wood_elf",
        )
        assertEquals(45, CharacterCalculations.speed(monkElf))
    }

    // ------------------------------------------------------------------ Initiative

    @Test
    fun `alert adds the proficiency bonus to initiative`() {
        assertEquals(2, CharacterCalculations.initiative(character()))
        assertEquals(4, CharacterCalculations.initiative(character(featIds = listOf("alert"))))
        assertEquals(
            "the Proficiency Bonus grows with the character",
            5,
            CharacterCalculations.initiative(character(level = 5, featIds = listOf("alert"))),
        )
    }

    // ------------------------------------------------------------------ What the sweep found

    @Test
    fun `the defense fighting style adds one to armor class`() {
        // The most-picked Fighting Style in the game, recorded and never applied. It is an
        // option, not a feature, which is why it sat outside everything that looked at
        // features — and why the same +1 has to reach a Champion's second style too.
        fun fighter(style: String?, armored: Boolean, choiceId: String = "fighting_style") =
            character(
                classId = "fighter", level = 7,
                inventory = if (armored) listOf(leather()) else emptyList(),
                classChoices = style?.let { mapOf(choiceId to listOf(it)) }.orEmpty(),
            )

        // Leather 11 + Dex 2 = 13.
        assertEquals(13, CharacterCalculations.armorClass(fighter(null, armored = true)))
        assertEquals(14, CharacterCalculations.armorClass(fighter("defense", armored = true)))
        assertEquals(
            "Defense needs armor to apply",
            12,
            CharacterCalculations.armorClass(fighter("defense", armored = false)),
        )
        assertEquals(
            "another style is not Defense",
            13,
            CharacterCalculations.armorClass(fighter("archery", armored = true)),
        )
        assertEquals(
            "a Champion's second style is the same option under another question",
            14,
            CharacterCalculations.armorClass(
                character(
                    classId = "fighter", subclassId = "champion", level = 7,
                    inventory = listOf(leather()),
                    classChoices = mapOf("champion_style" to listOf("defense")),
                )
            ),
        )
    }

    @Test
    fun `a gloom stalker adds wisdom to initiative`() {
        // Buried mid-paragraph in Dread Ambusher, between a leap and a damage rider.
        val stalker = character(
            classId = "ranger", subclassId = "gloom_stalker", level = 3, wis = 16,
        )
        assertEquals("Dex 2 + Wis 3", 5, CharacterCalculations.initiative(stalker))
    }

    @Test
    fun `roving raises a ranger's speed at level six`() {
        assertEquals(30, CharacterCalculations.speed(character(classId = "ranger", level = 5)))
        assertEquals(40, CharacterCalculations.speed(character(classId = "ranger", level = 6)))
    }

    @Test
    fun `genie's splendor is unarmored defense with a shield allowed`() {
        val paladin = character(
            classId = "paladin", subclassId = "noble_genies", level = 3, cha = 16,
        )
        assertEquals("10 + Dex 2 + Cha 3", 15, CharacterCalculations.armorClass(paladin))
        val shielded = character(
            classId = "paladin", subclassId = "noble_genies", level = 3, cha = 16,
            inventory = listOf(shield()),
        )
        assertEquals("the feature says a Shield is fine", 17, CharacterCalculations.armorClass(shielded))
    }

    // ------------------------------------------------------------------ The data itself

    @Test
    fun `every modifier is keyed to a feature that exists`() {
        // A key with a typo grants nothing and says nothing, which is the failure mode this
        // whole table was meant to end. Walk the characters that should have each one and
        // check the engine actually produced the element.
        val carriers = listOf(
            character(classId = "barbarian", level = 5),
            character(classId = "monk", level = 2),
            character(classId = "sorcerer", subclassId = "draconic", level = 3),
            character(classId = "bard", subclassId = "dance", level = 3),
            character(classId = "warlock", level = 4, featIds = listOf("infernal_bulwark")),
            character(featIds = listOf("alert")),
            character(speciesId = "elf", lineageId = "wood_elf"),
            character(classId = "ranger", level = 6),
            character(classId = "ranger", subclassId = "gloom_stalker", level = 3),
            character(classId = "paladin", subclassId = "noble_genies", level = 3),
        )
        val found = carriers.flatMap { RulesEngine.elementsFor(it) }.map { it.id }.toSet()
        val missing = ModifierData.elementIds() - found
        assertTrue("these keys name no feature the engine produces: $missing", missing.isEmpty())
    }

    @Test
    fun `every modifier reaches the character it belongs to`() {
        // Keyed correctly is not the same as applied: an element can exist and its effect
        // still be dropped. Each of these should be visible through the engine's own view.
        fun labels(pc: PlayerCharacter): Set<String> =
            (RulesEngine.view<Effect.ModifyStat>(pc).map { it.effect.label } +
                RulesEngine.view<Effect.SetStatBase>(pc).map { it.effect.label }).toSet()

        assertTrue(labels(character(classId = "barbarian", level = 5)).containsAll(
            setOf("Unarmored Defense", "Fast Movement")))
        assertTrue(labels(character(classId = "monk", level = 2)).containsAll(
            setOf("Unarmored Defense", "Unarmored Movement")))
        assertTrue("Draconic Resilience" in
            labels(character(classId = "sorcerer", subclassId = "draconic", level = 3)))
        assertTrue("Dazzling Footwork" in
            labels(character(classId = "bard", subclassId = "dance", level = 3)))
        assertTrue("Devil's Flesh" in
            labels(character(classId = "warlock", level = 4, featIds = listOf("infernal_bulwark"))))
        assertTrue("Initiative Proficiency" in labels(character(featIds = listOf("alert"))))
        assertTrue("Fleet of Foot" in labels(character(speciesId = "elf", lineageId = "wood_elf")))
        assertTrue("Roving" in labels(character(classId = "ranger", level = 6)))
        assertTrue("Dread Ambusher" in
            labels(character(classId = "ranger", subclassId = "gloom_stalker", level = 3)))
        assertTrue("Genie's Splendor" in
            labels(character(classId = "paladin", subclassId = "noble_genies", level = 3)))
        assertTrue("Defense" in labels(character(
            classId = "fighter", level = 1,
            classChoices = mapOf("fighting_style" to listOf("defense")),
        )))
    }

    @Test
    fun `no rule has been written as prose instead of a condition`() {
        // Condition.Descriptive applies no number by design. It exists so that writing one is
        // visible rather than silent; the count climbing is the signal that the closed list of
        // conditions has become too narrow to say what content needs to say.
        val prose = ModifierData.all().mapNotNull {
            when (it) {
                is Effect.ModifyStat -> it.condition as? Condition.Descriptive
                is Effect.SetStatBase -> it.condition as? Condition.Descriptive
                else -> null
            }
        }
        assertTrue("written as prose, so applied to nothing: $prose", prose.isEmpty())
    }

    @Test
    fun `an uncomputable condition never contributes a number`() {
        val raging = Condition.WhileRaging
        assertFalse(ConditionEval.isComputable(raging))
        assertFalse(ConditionEval.holds(raging, character()))
        // And it is not computable in combination either, so a half-known pair stays out.
        assertFalse(ConditionEval.isComputable(Condition.all(Condition.Unarmored, raging)))
    }

    // ------------------------------------------------------------------ The invariant

    @Test
    fun `the calculations name no feature of their own`() {
        // The point of the migration. Armor Class, Speed and Initiative should compute from
        // what the engine gives them and know nothing about which class or feat gave it — so
        // that adding the next Unarmored Defense is a row in ModifierData and nothing else.
        val source = File("src/main/java/com/pedroeu/ficha/domain/CharacterCalculations.kt")
            .readText()
        val body = source.substring(
            source.indexOf("// ------------------------------------------------------------------ Movement & defense"),
            source.indexOf("// ------------------------------------------------------------------ Spellcasting"),
        )
        val named = listOf(
            "\"barbarian\"", "\"monk\"", "\"draconic\"", "\"dance\"",
            "\"wood_elf\"", "\"alert\"", "\"infernal_bulwark\"",
        ).filter { it in body }
        assertTrue(
            "these are named inside the calculation rather than declared by the feature: " +
                "$named — put the rule in ModifierData instead",
            named.isEmpty(),
        )
    }
}
