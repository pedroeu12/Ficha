package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.FeatPrerequisiteData
import com.pedroeu.ficha.data.content.MasteryData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CharacterMasteries
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.FeatPrerequisites
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.creation.CreationState
import com.pedroeu.ficha.ui.levelup.AsiMode
import com.pedroeu.ficha.ui.levelup.LevelUpState
import com.pedroeu.ficha.ui.levelup.LevelUpStep
import com.pedroeu.ficha.ui.creation.CreationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Weapon mastery, and the playtest options from three Unearthed Arcana articles.
 *
 * Mastery was the one part of a martial turn the sheet said nothing about: every weapon
 * carried its property name, but nobody was asked which weapons they had mastery with, and
 * the property meant nothing where it mattered — on the attack line.
 */
class MasteryAndPlaytestTest {

    private fun character(
        classId: String = "fighter",
        level: Int = 1,
        subclassId: String? = null,
        featIds: List<String> = emptyList(),
        masteries: List<String> = emptyList(),
        masteryLevel: Int = 1,
        weapons: List<String> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        featIds = featIds,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = if (masteries.isEmpty()) emptyMap() else {
            mapOf("$masteryLevel:class:$classId:weapon_mastery" to masteries)
        },
        inventory = weapons.map { InventoryItem(name = it, weaponDefId = it) },
    )

    // ------------------------------------------------------------- Mastery properties

    @Test
    fun `all eight mastery properties are described`() {
        assertEquals(8, MasteryData.PROPERTIES.size)
        listOf("Cleave", "Graze", "Nick", "Push", "Sap", "Slow", "Topple", "Vex")
            .forEach { name ->
                val property = MasteryData.byName(name)
                assertNotNull("$name is missing from the mastery table", property)
                assertTrue(
                    "$name needs its rules text, not just a label",
                    property!!.description.length > 60,
                )
            }
    }

    @Test
    fun `every weapon's mastery property is one the rules define`() {
        EquipmentData.WEAPONS
            .filter { it.mastery.isNotBlank() }
            .forEach { weapon ->
                assertNotNull(
                    "${weapon.name} claims the ${weapon.mastery} property, which isn't real",
                    MasteryData.byName(weapon.mastery),
                )
            }
    }

    @Test
    fun `only the martial classes have weapon mastery, at the counts the tables give`() {
        assertEquals(setOf("barbarian", "fighter", "paladin", "ranger", "rogue"), MasteryData.sourceIds())

        assertEquals(3, MasteryData.countFor("fighter", 1))
        assertEquals(4, MasteryData.countFor("fighter", 4))
        assertEquals(5, MasteryData.countFor("fighter", 10))
        assertEquals(6, MasteryData.countFor("fighter", 16))
        assertEquals(2, MasteryData.countFor("barbarian", 1))
        assertEquals(4, MasteryData.countFor("barbarian", 10))
        assertEquals(2, MasteryData.countFor("rogue", 1))
        assertEquals(3, MasteryData.countFor("rogue", 9))

        assertEquals("a Wizard never masters a weapon", 0, MasteryData.countFor("wizard", 20))
    }

    @Test
    fun `every mastery table key names a real class`() {
        val known = ClassData.ALL.map { it.id }.toSet()
        assertTrue((MasteryData.sourceIds() - known).isEmpty())
    }

    @Test
    fun `the class table asks which weapons at every level the count grows`() {
        listOf("barbarian" to listOf(1, 4, 10), "fighter" to listOf(1, 4, 10, 16))
            .forEach { (classId, levels) ->
                val progression = ProgressionData.forClass(classId)!!
                levels.forEach { level ->
                    val choice = progression.featuresAt(level)
                        .flatMap { it.choices }
                        .find { it.id == "class:$classId:weapon_mastery" }
                    assertNotNull("$classId should ask again at level $level", choice)
                    assertEquals(MasteryData.countFor(classId, level), choice!!.count)
                    assertTrue("a Long Rest lets you swap one", choice.changeableOnRest)
                }
            }
    }

    // ------------------------------------------------------------- Resolving a character's

    @Test
    fun `a chosen mastery reaches the attack line with its rules text`() {
        val fighter = character(
            masteries = listOf("longsword", "greataxe", "shortbow"),
            weapons = listOf("longsword", "greatsword"),
        )

        val masteries = CharacterMasteries.all(fighter)
        assertEquals(3, masteries.size)
        assertTrue(masteries.any { it.weaponId == "longsword" })

        val longsword = CharacterAttacks.all(fighter).first { it.name == "Longsword" }
        assertEquals("Sap", longsword.masteryProperty)
        assertTrue(longsword.masteryDescription.contains("Disadvantage"))
        assertTrue("the line should name it too", longsword.notes.contains("Sap"))

        // A weapon they carry but haven't mastered names its property without granting it.
        val greatsword = CharacterAttacks.all(fighter).first { it.name == "Greatsword" }
        assertEquals("", greatsword.masteryProperty)
        assertTrue(greatsword.notes.contains("not mastered"))
    }

    @Test
    fun `a wizard's weapon names its property but never claims mastery`() {
        val wizard = character(classId = "wizard", weapons = listOf("dagger"))
        assertFalse(CharacterMasteries.hasWeaponMastery(wizard))

        val dagger = CharacterAttacks.all(wizard).first { it.name == "Dagger" }
        assertEquals("", dagger.masteryProperty)
        assertTrue(dagger.notes.contains("mastery property"))
    }

    @Test
    fun `the most recent answer wins when the count has grown`() {
        // A Fighter who reached level 4 and re-picked keeps four weapons, not seven.
        val fighter = character(level = 4).copy(
            levelSelections = mapOf(
                "1:class:fighter:weapon_mastery" to listOf("longsword", "greataxe", "shortbow"),
                "4:class:fighter:weapon_mastery" to
                    listOf("rapier", "shortsword", "handaxe", "longbow"),
            )
        )

        val masteries = CharacterMasteries.all(fighter).map { it.weaponId }
        assertEquals(4, masteries.size)
        assertTrue(masteries.containsAll(listOf("rapier", "shortsword", "handaxe", "longbow")))
        assertFalse("the level 1 answer was replaced", masteries.contains("longsword"))
    }

    @Test
    fun `the weapon master feat adds one more on top of the class's`() {
        val plain = character(masteries = listOf("longsword", "greataxe", "shortbow"))
        val withFeat = plain.copy(
            featIds = listOf("weapon_master"),
            originChoiceSelections = mapOf("feat:weapon_master:mastery" to listOf("rapier")),
        )

        assertEquals(3, CharacterMasteries.allowance(plain))
        assertEquals(4, CharacterMasteries.allowance(withFeat))
        assertTrue(CharacterMasteries.all(withFeat).any { it.weaponId == "rapier" })
    }

    @Test
    fun `a repeated rest-changeable choice is offered once, at its current size`() {
        val fighter = character(level = 4, masteries = listOf("longsword", "greataxe", "shortbow"))
        val offered = ChoiceResolver.restChangeable(fighter)
            .filter { it.choice.id == "class:fighter:weapon_mastery" }

        assertEquals("one decision, not one per level", 1, offered.size)
        assertEquals("and at the count the character has now", 4, offered.first().choice.count)
    }

    @Test
    fun `creation asks a martial class which weapons before it can move on`() {
        val fighter = CreationState(
            step = CreationStep.CLASS_CHOICES,
            classId = "fighter",
            classSkillChoices = setOf(
                com.pedroeu.ficha.data.model.Skill.ATHLETICS,
                com.pedroeu.ficha.data.model.Skill.PERCEPTION,
            ),
            classSelections = mapOf("fighting_style" to listOf("defense")),
        )

        val choice = fighter.classFeatureChoices.find { it.id == "class:fighter:weapon_mastery" }
        assertNotNull("the Fighter must be asked at creation", choice)
        assertEquals(3, choice!!.count)
        assertFalse(fighter.canAdvance)

        assertTrue(
            fighter.copy(
                classFeatureSelections = mapOf(choice.id to listOf("longsword", "greataxe", "shortbow"))
            ).canAdvance
        )
    }

    @Test
    fun `a class with no mastery is asked nothing extra at creation`() {
        val wizard = CreationState(step = CreationStep.CLASS_CHOICES, classId = "wizard")
        assertTrue(
            "no Weapon Mastery prompt for a Wizard",
            wizard.classFeatureChoices.none { it.id.endsWith("weapon_mastery") },
        )
    }

    // ------------------------------------------------------------- Playtest subclasses

    private val playtestSubclasses = listOf(
        "pestilence_domain" to "cleric",
        "circle_of_the_titan" to "druid",
        "hell_knight" to "fighter",
        "demonic_sorcery" to "sorcerer",
        "reanimator" to "artificer",
        "college_of_spirits" to "bard",
        "grave_domain" to "cleric",
        "hollow_warden" to "ranger",
        "phantom" to "rogue",
        "shadow_sorcery" to "sorcerer",
        "hexblade_patron" to "warlock",
        "undead_patron" to "warlock",
    )

    @Test
    fun `every playtest subclass is present, on the right class, and marked as playtest`() {
        playtestSubclasses.forEach { (id, classId) ->
            val subclass = SubclassData.byId(id)
            assertNotNull("$id is missing", subclass)
            assertEquals("$id belongs to the $classId", classId, subclass!!.classId)
            assertTrue(
                "$id must be labelled so it isn't mistaken for a published option",
                subclass.isPlaytest,
            )
            assertTrue("$id needs its features", subclass.features.isNotEmpty())
            assertTrue(
                "$id should gain something at level 3",
                subclass.featuresAt(3).isNotEmpty(),
            )
        }
    }

    @Test
    fun `the revised villainous subclasses carry the later text`() {
        listOf("circle_of_the_titan", "hell_knight", "demonic_sorcery").forEach { id ->
            assertEquals(
                "$id was revisited, so it should carry the update's wording",
                "Unearthed Arcana 2026: Villainous Options Update",
                SubclassData.byId(id)!!.attribution,
            )
        }
        // The Pestilence Domain was not revised, so it keeps its original attribution.
        assertEquals(
            "Unearthed Arcana 2026: Villainous Options",
            SubclassData.byId("pestilence_domain")!!.attribution,
        )
    }

    @Test
    fun `each playtest subclass grants the spells its table promises`() {
        fun granted(classId: String, subclassId: String, level: Int) =
            CharacterSpells
                .granted(character(classId = classId, level = level, subclassId = subclassId))
                .map { it.spell.id }

        assertTrue(granted("cleric", "pestilence_domain", 3).contains("ray_of_sickness"))
        assertTrue(granted("cleric", "grave_domain", 5).contains("revivify"))
        assertTrue(granted("druid", "circle_of_the_titan", 9).contains("destructive_wave"))
        assertTrue(granted("sorcerer", "demonic_sorcery", 3).contains("dissonant_whispers"))
        // Both of these read the table wrong until the grants were rebuilt from the
        // subclass's own printed text: Shadow Sorcery's level 5 row is Hunger of Hadar and
        // Nondetection, and the Undead patron's level 7 row is Greater Invisibility and
        // Phantasmal Killer.
        assertTrue(granted("sorcerer", "shadow_sorcery", 5).contains("nondetection"))
        assertTrue(granted("artificer", "reanimator", 3).contains("witch_bolt"))
        assertTrue(granted("ranger", "hollow_warden", 3).contains("wrathful_smite"))
        assertTrue(granted("warlock", "hexblade_patron", 3).contains("hex"))
        assertTrue(granted("warlock", "undead_patron", 7).contains("greater_invisibility"))

        // A table entry above the character's level hasn't arrived yet.
        assertFalse(granted("cleric", "pestilence_domain", 3).contains("contagion"))
    }

    @Test
    fun `the playtest subclasses track what the rules limit`() {
        fun pools(classId: String, subclassId: String, level: Int) =
            CharacterResources
                .definitions(character(classId = classId, level = level, subclassId = subclassId))
                .map { it.id }

        assertTrue(pools("fighter", "hell_knight", 3).contains("hell_knight:infernal_wound"))
        assertTrue(pools("rogue", "phantom", 9).contains("phantom:soul_trinkets"))
        assertTrue(pools("warlock", "undead_patron", 3).contains("undead_patron:form_of_dread"))
        assertTrue(pools("warlock", "hexblade_patron", 3).contains("hexblade_patron:curse"))
        assertTrue(pools("cleric", "grave_domain", 6).contains("grave_domain:sentinel"))
        assertTrue(pools("artificer", "reanimator", 3).contains("reanimator:companion"))
    }

    // ------------------------------------------------------------- Playtest feats

    @Test
    fun `the villainous origin feats and epic boons are available`() {
        listOf("atoners_grace", "raised_by_cultists", "trapper", "underhanded").forEach { id ->
            assertTrue(
                "$id should be offered as an Origin feat",
                FeatData.ORIGIN_FEATS.any { it.id == id },
            )
        }
        listOf(
            "boon_of_the_bandit_king", "boon_of_the_cleansed_heart",
            "boon_of_the_hunters_eye", "boon_of_unwavering_devotion",
        ).forEach { id ->
            assertTrue("$id should be an Epic Boon", FeatData.EPIC_BOONS.any { it.id == id })
        }
    }

    @Test
    fun `both paths of villainy are complete, from initiate to ascension`() {
        val deathKnight = listOf(
            "death_knight_initiate", "dread_authority", "harbinger_of_doom",
            "deathly_presence", "unholy_steed", "death_knight_ascension",
        )
        val lich = listOf(
            "lich_initiate", "arcane_restoration", "transfer_life",
            "undead_grasp", "lich_ascension",
        )

        (deathKnight + lich).forEach { id ->
            val feat = FeatData.byId(id)
            assertNotNull("$id is missing", feat)
            assertTrue(
                "$id raises an ability score, so it must ask which",
                FeatChoiceData.ABILITY_OPTIONS.containsKey(id),
            )
        }

        // Every feat on both paths, the Initiates included, has a level 4+ prerequisite and
        // is taken in place of an Ability Score Improvement. None of them is an Origin feat,
        // so a level 1 character can't start down either path at creation.
        (deathKnight + lich).forEach { id ->
            assertTrue(
                "$id needs level 4, so it can't sit with the Origin feats",
                FeatData.GENERAL_FEATS.any { it.id == id },
            )
            assertFalse(
                "$id must not be offered at character creation",
                FeatData.ORIGIN_FEATS.any { it.id == id },
            )
        }
    }

    /**
     * The Origin feat list is what a background grants and what a Human's Versatile trait
     * draws from, both at level 1. Anything in it whose own text demands a level is in the
     * wrong list — which is exactly how the two Initiate feats slipped through.
     */
    @Test
    fun `no origin feat asks for a level the character cannot have yet`() {
        val levelled = FeatData.ORIGIN_FEATS.filter { feat ->
            Regex("""level \d+""", RegexOption.IGNORE_CASE).containsMatchIn(feat.description) &&
                !feat.description.contains("character level", ignoreCase = true) &&
                !feat.description.contains("level 1 spell", ignoreCase = true)
        }
        assertTrue(
            "these are offered at creation but require a level: ${levelled.map { it.id }}",
            levelled.isEmpty(),
        )
    }

    // ------------------------------------------------------------- Feat prerequisites

    @Test
    fun `every prerequisite key and target names a real feat`() {
        val known = FeatData.ALL.map { it.id }.toSet()
        val unknown = FeatPrerequisiteData.sourceIds() - known
        assertTrue("these prerequisite ids match no feat: $unknown", unknown.isEmpty())
    }

    @Test
    fun `a path opens at level 4 and only for a class that qualifies`() {
        // A Fighter has Weapon Mastery, so the Death Knight path is open from level 4.
        val fighter = character(classId = "fighter", level = 4)
        assertTrue(FeatPrerequisites.isAllowed(fighter, "death_knight_initiate"))

        // A Wizard has no Weapon Mastery, so it never is.
        val wizard = character(classId = "wizard", level = 12)
        val blocked = FeatPrerequisites.check(wizard, "death_knight_initiate")
        assertFalse(blocked.allowed)
        assertTrue(blocked.missing.contains("Weapon Mastery"))

        // And the Lich path wants spellcasting, which the Fighter lacks and the Wizard has.
        assertTrue(FeatPrerequisites.isAllowed(wizard, "lich_initiate"))
        assertFalse(FeatPrerequisites.isAllowed(fighter, "lich_initiate"))
    }

    @Test
    fun `a path feat needs its initiate first`() {
        val fighter = character(classId = "fighter", level = 8)
        val without = FeatPrerequisites.check(fighter, "deathly_presence")
        assertFalse(without.allowed)
        assertTrue(without.missing.contains("Death Knight Initiate"))

        val initiated = fighter.copy(featIds = listOf("death_knight_initiate"))
        assertTrue(FeatPrerequisites.isAllowed(initiated, "deathly_presence"))

        // Level 8 is its own bar on top of that.
        val tooEarly = character(classId = "fighter", level = 4)
            .copy(featIds = listOf("death_knight_initiate"))
        assertFalse(FeatPrerequisites.isAllowed(tooEarly, "deathly_presence"))
    }

    @Test
    fun `ascension needs level 12 and two other feats from the same path`() {
        val oneFeat = character(classId = "fighter", level = 12)
            .copy(featIds = listOf("death_knight_initiate"))
        val short = FeatPrerequisites.check(oneFeat, "death_knight_ascension")
        assertFalse("one path feat isn't enough", short.allowed)
        assertTrue(short.missing.contains("2 feats from this path"))

        val twoFeats = oneFeat.copy(
            featIds = listOf("death_knight_initiate", "harbinger_of_doom")
        )
        assertTrue(FeatPrerequisites.isAllowed(twoFeats, "death_knight_ascension"))

        val tooEarly = character(classId = "fighter", level = 8)
            .copy(featIds = listOf("death_knight_initiate", "harbinger_of_doom"))
        assertFalse(FeatPrerequisites.isAllowed(tooEarly, "death_knight_ascension"))
    }

    @Test
    fun `a greater dragonmark needs the mark it improves`() {
        val plain = character(level = 4)
        assertFalse(FeatPrerequisites.isAllowed(plain, "greater_mark_of_storm"))

        val marked = plain.copy(featIds = listOf("mark_of_storm"))
        assertTrue(FeatPrerequisites.isAllowed(marked, "greater_mark_of_storm"))
        // Potent Dragonmark takes any mark, not one specific one.
        assertTrue(FeatPrerequisites.isAllowed(marked, "potent_dragonmark"))
        assertFalse(FeatPrerequisites.isAllowed(plain, "potent_dragonmark"))
    }

    @Test
    fun `a feat that accepts either the feat or the feature takes whichever you have`() {
        val sparked = character(classId = "fighter", level = 4)
            .copy(featIds = listOf("spellfire_spark"))
        assertTrue("the feat alone qualifies", FeatPrerequisites.isAllowed(sparked, "spellfire_adept"))

        val caster = character(classId = "wizard", level = 4)
        assertTrue("so does being a caster", FeatPrerequisites.isAllowed(caster, "spellfire_adept"))

        val neither = character(classId = "fighter", level = 4)
        assertFalse(FeatPrerequisites.isAllowed(neither, "spellfire_adept"))
    }

    @Test
    fun `an ordinary feat has nothing to meet`() {
        val anyone = character(level = 4)
        listOf("alert", "tough", "skill_expert", "war_caster").forEach { id ->
            assertTrue("$id should need no prerequisite", FeatPrerequisites.isAllowed(anyone, id))
        }
    }

    @Test
    fun `the level up picker blocks a feat you don't qualify for`() {
        val wizard = character(classId = "wizard", level = 3)
        val state = LevelUpState(character = wizard, levellingClassId = "wizard")

        assertTrue("the ASI level offers feats", state.grantsAsi)
        assertTrue(
            "an unavailable feat stays on the list with a reason",
            state.featBlockers["death_knight_initiate"]?.contains("Weapon Mastery") == true,
        )
        assertEquals(
            "an available one carries no reason",
            "",
            state.featBlockers["war_caster"],
        )

        // Picking a blocked feat must not let the level be finished.
        val blocked = state.copy(
            requestedStep = LevelUpStep.ASI,
            asiMode = AsiMode.FEAT,
            featId = "death_knight_initiate",
        )
        assertFalse(blocked.canAdvance)

        assertTrue(
            blocked.copy(featId = "war_caster").canAdvance
        )
    }

    @Test
    fun `a path feat hands over the spell it names`() {
        val deathKnight = character(featIds = listOf("deathly_presence"))
        assertTrue(
            CharacterSpells.granted(deathKnight).any { it.spell.id == "fear" },
        )

        val lich = character(featIds = listOf("undead_grasp"))
        assertTrue(
            CharacterSpells.granted(lich).any { it.spell.id == "chill_touch" },
        )
    }

    @Test
    fun `every spell the new content names exists in the catalog`() {
        // The subclass tables and path feats reference spells by id; one that isn't in the
        // catalog would grant nothing at all, and say nothing about it.
        playtestSubclasses.forEach { (subclassId, classId) ->
            val leveled = character(classId = classId, level = 20, subclassId = subclassId)
            assertTrue(
                "$subclassId names spells the catalog lacks: " +
                    CharacterSpells.unresolvedGrants(leveled),
                CharacterSpells.unresolvedGrants(leveled).isEmpty(),
            )
        }

        listOf(
            "wrathful_smite", "staggering_smite", "arcane_vigor", "spike_growth",
            "blindness_deafness", "speak_with_dead", "summon_undead", "phantasmal_killer",
            "antilife_shell", "contagion", "destructive_wave", "awaken", "augury",
        ).forEach { id ->
            assertNotNull("$id should be in the catalog", SpellData.byId(id))
        }
    }
}
