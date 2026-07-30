package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for the content added from Eberron: Forge of the Artificer, Forgotten Realms:
 * Heroes of Faerûn, and the Dungeon Master's Guide magic item list.
 */
class NewSourcebookContentTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        backgroundId: String = "soldier",
        featIds: List<String> = emptyList(),
        levelSelections: Map<String, List<String>> = emptyMap(),
        intelligence: Int = 16,
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = backgroundId,
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 } + ("INT" to intelligence),
        featIds = featIds,
        levelSelections = levelSelections,
    )

    // ------------------------------------------------------------- Artificer class

    @Test
    fun `the artificer exists as a full class with a progression table`() {
        val charClass = ClassData.ALL.find { it.id == "artificer" }
        assertNotNull("the Artificer must be selectable at creation", charClass)
        assertEquals(8, charClass!!.hitDie)
        assertEquals(Ability.INT, charClass.spellcastingAbility)

        val progression = ProgressionData.forClass("artificer")
        assertNotNull("the Artificer needs a level 1-20 table", progression)
        assertEquals(CasterType.ARTIFICER, progression!!.casterType)
        assertEquals(3, progression.subclassLevel)
    }

    @Test
    fun `the artificer gets spell slots at level 1, unlike other half casters`() {
        val artificer = SpellSlotTables.slotsFor(CasterType.ARTIFICER, 1)
        val paladin = SpellSlotTables.slotsFor(CasterType.HALF, 1)

        assertEquals("two level 1 slots straight away", mapOf(1 to 2), artificer)
        assertTrue("a Paladin has none at level 1", paladin.isEmpty())
    }

    @Test
    fun `from level 2 the artificer matches the half caster table exactly`() {
        (2..20).forEach { level ->
            assertEquals(
                "level $level should match the half caster progression",
                SpellSlotTables.slotsFor(CasterType.HALF, level),
                SpellSlotTables.slotsFor(CasterType.ARTIFICER, level),
            )
        }
    }

    @Test
    fun `artificer spell slots flow through to the sheet`() {
        val artificer = character("artificer", level = 5)
        val slots = CharacterCalculations.spellSlots(artificer)

        assertEquals("four level 1 slots at level 5", 4, slots[1])
        assertEquals("two level 2 slots at level 5", 2, slots[2])
    }

    @Test
    fun `the artificer has all five subclasses`() {
        val names = SubclassData.forClass("artificer").map { it.name }.sorted()
        assertEquals(
            listOf("Alchemist", "Armorer", "Artillerist", "Battle Smith", "Cartographer"),
            names,
        )
    }

    // ------------------------------------------------------------- Replicate Magic Item

    @Test
    fun `replicate magic item offers plans drawn from the magic item catalog`() {
        val plans = ProgressionData.forClass("artificer")!!
            .features.first { it.name == "Replicate Magic Item" }
            .choices.first()

        assertEquals(4, plans.count)
        assertTrue("the level 2 list must not be empty", plans.options.isNotEmpty())

        plans.options.forEach { option ->
            assertNotNull(
                "${option.name} must be a real catalog entry",
                MagicItemData.byId(option.id),
            )
            assertTrue(
                "${option.name} needs rules text",
                option.description.length >= 40,
            )
        }
    }

    @Test
    fun `plan lists grow as the artificer levels, and never shrink`() {
        val sizes = MagicItemData.PLAN_LEVELS.map { MagicItemData.artificerPlans(it).size }
        assertEquals(
            "each tier should add plans",
            sizes.sortedDescending().reversed(),
            sizes,
        )
        assertTrue("later tiers must actually add options", sizes.last() > sizes.first())
    }

    @Test
    fun `a level 2 artificer cannot see plans gated behind higher levels`() {
        val early = MagicItemData.artificerPlans(2).map { it.name }
        assertTrue("Bag of Holding is a starting plan", early.contains("Bag of Holding"))
        assertTrue("Flame Tongue is a level 14 plan", !early.contains("Flame Tongue"))
        assertTrue("Armor, +1 is a level 6 plan", !early.contains("Armor, +1"))
    }

    // ------------------------------------------------------------- Magic items

    @Test
    fun `magic items are addable from the same picker as everything else`() {
        val fromCatalog = ItemCatalog.search("Bag of Holding")
        assertTrue("a DM-granted item must be findable", fromCatalog.isNotEmpty())

        val entry = fromCatalog.first()
        val inventoryItem = entry.toInventoryItem()
        assertEquals("Bag of Holding", inventoryItem.name)
        assertTrue("the item carries its rules text into inventory", inventoryItem.notes.isNotBlank())
    }

    @Test
    fun `every magic item carries rarity, a type, and real rules text`() {
        MagicItemData.ALL.forEach { magicItem ->
            assertTrue("${magicItem.name} has no type", magicItem.kind.isNotBlank())
            assertTrue(
                "${magicItem.name} needs rules text",
                magicItem.description.length >= 40,
            )
            assertTrue(
                "${magicItem.name} should describe itself in its subtitle",
                magicItem.subtitle.contains(magicItem.rarity.label),
            )
        }
    }

    @Test
    fun `magic item ids are unique so the catalog can key on them`() {
        val ids = MagicItemData.ALL.map { it.id }
        assertEquals("duplicate magic item ids", ids.distinct().size, ids.size)

        val catalogIds = ItemCatalog.ALL.map { it.id }
        assertEquals("duplicate catalog ids", catalogIds.distinct().size, catalogIds.size)
    }

    // ------------------------------------------------------------- Species and origins

    @Test
    fun `the eberron species are available at creation`() {
        val ids = SpeciesData.ALL.map { it.id }
        listOf("changeling", "kalashtar", "khoravar", "shifter", "warforged").forEach { id ->
            assertTrue("$id should be selectable", ids.contains(id))
            val species = SpeciesData.byId(id)
            assertNotNull(species)
            assertTrue("$id needs traits", species!!.traits.isNotEmpty())
        }
    }

    @Test
    fun `a shifter picks how their shifting works and tracks its uses`() {
        val shifter = SpeciesData.byId("shifter")!!
        assertEquals(4, shifter.lineageOptions.size)

        val pc = character("fighter", level = 5, speciesId = "shifter")
        val shifting = CharacterResources.definitions(pc).find { it.id == "shifter:shifting" }
        assertNotNull("Shifting is a limited-use trait", shifting)
        assertEquals(
            "uses equal the proficiency bonus",
            CharacterCalculations.proficiencyBonus(pc),
            shifting!!.max,
        )
    }

    @Test
    fun `every background points at a feat that actually exists`() {
        BackgroundData.ALL.forEach { background ->
            assertNotNull(
                "${background.name} grants the unknown feat '${background.featId}'",
                FeatData.byId(background.featId),
            )
            assertEquals(
                "${background.name} must offer exactly three ability scores",
                3,
                background.abilityOptions.size,
            )
            assertEquals(
                "${background.name} must grant exactly two skills",
                2,
                background.skillProficiencies.size,
            )
        }
    }

    @Test
    fun `the new backgrounds from both books are present`() {
        val ids = BackgroundData.ALL.map { it.id }
        listOf(
            "archaeologist", "house_cannith_heir", "inquisitive",
            "harper", "zhentarim_mercenary", "spellfire_initiate",
        ).forEach { assertTrue("$it should be selectable", ids.contains(it)) }
    }

    @Test
    fun `feat ids stay unique across all three categories`() {
        val ids = FeatData.ALL.map { it.id }
        assertEquals("duplicate feat ids", ids.distinct().size, ids.size)
    }

    @Test
    fun `every feat has a real description`() {
        FeatData.ALL.forEach { feat ->
            assertTrue("${feat.name} needs a description", feat.description.length >= 40)
        }
    }

    // ------------------------------------------------------------- New subclasses

    @Test
    fun `the heroes of faerun subclasses attach to the right classes`() {
        val expected = mapOf(
            "college_of_the_moon" to "bard",
            "knowledge_domain" to "cleric",
            "banneret" to "fighter",
            "noble_genies" to "paladin",
            "winter_walker" to "ranger",
            "scion_of_the_three" to "rogue",
            "spellfire_sorcery" to "sorcerer",
            "bladesinger" to "wizard",
        )
        expected.forEach { (subclassId, classId) ->
            val subclass = SubclassData.byId(subclassId)
            assertNotNull("$subclassId should exist", subclass)
            assertEquals("$subclassId belongs to $classId", classId, subclass!!.classId)
        }
    }

    @Test
    fun `every subclass id is unique and every feature has text`() {
        val ids = SubclassData.ALL.map { it.id }
        assertEquals("duplicate subclass ids", ids.distinct().size, ids.size)

        SubclassData.ALL.forEach { subclass ->
            assertTrue("${subclass.name} has no features", subclass.features.isNotEmpty())
            subclass.features.forEach { feature ->
                assertTrue(
                    "${subclass.name} / ${feature.name} has no description",
                    feature.description.length >= 20,
                )
            }
        }
    }

    @Test
    fun `every subclass belongs to a class that exists`() {
        val classIds = ClassData.ALL.map { it.id }.toSet()
        SubclassData.ALL.forEach { subclass ->
            assertTrue(
                "${subclass.name} points at unknown class '${subclass.classId}'",
                subclass.classId in classIds,
            )
        }
    }

    // ------------------------------------------------------------- Resources

    @Test
    fun `an artificer tracks tinkers magic from level 1 and flash of genius from level 7`() {
        val early = CharacterResources.definitions(character("artificer", level = 1))
        assertNotNull(
            "Tinker's Magic is a level 1 pool",
            early.find { it.id == "artificer:tinkers_magic" },
        )
        assertNull(
            "Flash of Genius arrives at level 7",
            early.find { it.id == "artificer:flash_of_genius" },
        )

        val later = CharacterResources.definitions(character("artificer", level = 7))
        val genius = later.find { it.id == "artificer:flash_of_genius" }
        assertNotNull(genius)
        assertEquals("uses equal the Intelligence modifier", 3, genius!!.max)
    }

    @Test
    fun `an alchemist's elixirs list every effect on the table`() {
        val alchemist = character("artificer", level = 5, subclassId = "alchemist")
        val elixirs = CharacterResources.definitions(alchemist)
            .first { it.id == "alchemist:experimental_elixir" }

        assertEquals("three elixirs per Long Rest at level 5", 3, elixirs.max)
        val names = elixirs.options.map { it.name }
        listOf("Healing", "Swiftness", "Resilience", "Boldness", "Flight").forEach {
            assertTrue("the $it elixir should be listed", names.contains(it))
        }
        elixirs.options.forEach {
            assertTrue("${it.name} needs rules text", it.description.length >= 40)
        }
    }

    @Test
    fun `an artillerist's cannon lists its activation options`() {
        val artillerist = character("artificer", level = 9, subclassId = "artillerist")
        val cannon = CharacterResources.definitions(artillerist)
            .first { it.id == "artillerist:eldritch_cannon" }

        val names = cannon.options.map { it.name }
        assertTrue(names.contains("Flamethrower"))
        assertTrue(names.contains("Force Ballista"))
        assertTrue(names.contains("Protector"))
        assertTrue("Detonate unlocks at level 9", names.contains("Detonate"))
    }

    @Test
    fun `a dragonmark feat grants a tracked pool of free castings`() {
        val marked = character("rogue", level = 5, featIds = listOf("mark_of_shadow"))
        val pool = CharacterResources.definitions(marked).find { it.id == "feat:mark_of_shadow" }

        assertNotNull("the mark's free casting should be trackable", pool)
        assertEquals(1, pool!!.max)
    }

    @Test
    fun `the greater mark of healing upgrades the pool rather than adding a second`() {
        val marked = character(
            classId = "cleric",
            level = 8,
            featIds = listOf("mark_of_healing", "greater_mark_of_healing"),
        )
        val pools = CharacterResources.definitions(marked)
            .filter { it.source.contains("Mark of Healing") }

        assertEquals("only one Mark of Healing pool", 1, pools.size)
        assertEquals(
            "uses rise to the proficiency bonus",
            CharacterCalculations.proficiencyBonus(marked),
            pools.first().max,
        )
    }

    // ------------------------------------------------------------- Sub-option wiring

    @Test
    fun `armor model picks resolve and carry their rules text`() {
        val armorer = character(
            classId = "artificer",
            level = 3,
            subclassId = "armorer",
            levelSelections = mapOf("3:armor_model" to listOf("guardian")),
        )
        val resolved = ChoiceResolver.subclassFeatureChoices(armorer)
            .first { it.choice.id == "armor_model" }

        assertTrue(resolved.isAnswered)
        assertEquals(listOf("Guardian"), resolved.selectedNames)

        val picked = resolved.choice.options.first { it.id == "guardian" }
        assertTrue("the model needs its full text", picked.description.length >= 40)
    }

    @Test
    fun `a battle smith's arcane jolt picks land under its own pool`() {
        val smith = character(
            classId = "artificer",
            level = 9,
            subclassId = "battle_smith",
            levelSelections = mapOf("9:arcane_jolt" to listOf("destructive")),
        )
        val pool = CharacterResources.definitions(smith)
            .first { it.id == "battle_smith:arcane_jolt" }

        assertTrue(
            "the chosen effect should appear on the tracker",
            pool.options.any { it.name == "Destructive Energy" },
        )
    }
}
