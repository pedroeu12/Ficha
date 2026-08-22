package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.SpellSlotTables
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.FeatBonuses
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.Multiclassing
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.levelup.LevelUpState
import com.pedroeu.ficha.ui.levelup.LevelUpStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Three bugs that shared a shape: something the rules attach to a source was only ever
 * modelled for one specific case. A feat's own choices were asked for only when a background
 * granted it; spell slots were modelled for full casters and bolted on elsewhere; and the
 * multiclass prerequisite was applied to every class rather than only to one being joined.
 */
class FeatChoicesAndHalfCasterTest {

    private fun character(
        classId: String = "fighter",
        level: Int = 1,
        speciesId: String = "human",
        featIds: List<String> = emptyList(),
        known: List<KnownSpell> = emptyList(),
        scores: Map<String, Int> = emptyMap(),
        originSelections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        classId = classId,
        backgroundId = "soldier",
        level = level,
        featIds = featIds,
        knownSpells = known,
        originChoiceSelections = originSelections,
        baseAbilityScores = Ability.ALL.associate { it.name to 10 } + scores,
    )

    // ------------------------------------------------ Multiclass prerequisites

    @Test
    fun `a class you already have never needs its own prerequisite`() {
        // Every score is 10, so nothing meets the 13 a multiclass would demand.
        ClassData.ALL.forEach { charClass ->
            val existing = character(classId = charClass.id, level = 3)
            assertTrue(
                "${charClass.id} must be able to keep levelling itself",
                Multiclassing.canTake(existing, charClass.id),
            )
        }
    }

    @Test
    fun `a bard short on charisma still levels as a bard`() {
        val bard = character(classId = "bard", level = 4, scores = mapOf("CHA" to 12))
        val option = Multiclassing.options(bard).first { it.classId == "bard" }

        assertTrue("their own class is always open", option.allowed)
        assertTrue(option.alreadyHas)
        assertTrue(
            "the level up flow must let them advance",
            LevelUpState(character = bard, levellingClassId = "bard")
                .copy(requestedStep = LevelUpStep.CLASS).canAdvance,
        )
    }

    @Test
    fun `the prerequisite still gates a class being joined`() {
        val bard = character(classId = "bard", level = 4, scores = mapOf("CHA" to 16))
        val wizard = Multiclassing.options(bard).first { it.classId == "wizard" }

        assertFalse("Intelligence 10 is not enough to become a Wizard", wizard.allowed)
        assertFalse(wizard.alreadyHas)
        assertTrue(wizard.reason.contains("Intelligence"))
    }

    @Test
    fun `the flow never opens on a step this level does not have`() {
        // All 10s means no class can be joined, so the class picker is skipped entirely.
        val fighter = character(classId = "fighter", level = 4)
        val state = LevelUpState(character = fighter)

        assertFalse("nothing to multiclass into", state.steps.contains(LevelUpStep.CLASS))
        assertEquals("so it opens on the first real step", state.steps.first(), state.step)
        assertTrue("and that step can be completed", state.canAdvance)
    }

    // ------------------------------------------------ Half casters

    @Test
    fun `every spellcasting class can cast at the level it gains spellcasting`() {
        ClassData.ALL.forEach { charClass ->
            val progression = ProgressionData.forClass(charClass.id) ?: return@forEach
            if (progression.casterType == CasterType.NONE) return@forEach

            val level1 = character(classId = charClass.id, level = 1)
            assertTrue(
                "${charClass.id} has a Spellcasting feature at level 1 and must have slots",
                CharacterCalculations.spellSlots(level1).isNotEmpty(),
            )
        }
    }

    @Test
    fun `the ranger and paladin are marked as the casters they are`() {
        listOf("ranger", "paladin").forEach { id ->
            val charClass = ClassData.byId(id)!!
            assertTrue("$id casts from level 1 in the 2024 rules", charClass.isSpellcaster)
            assertNotNull(charClass.spellcastingAbility)
        }
    }

    @Test
    fun `every prepared caster picks its level 1 spells at creation`() {
        listOf("cleric", "druid", "paladin", "ranger", "wizard", "artificer").forEach { id ->
            val choice = ClassData.byId(id)!!.choices
                .filterIsInstance<ClassChoice.CantripChoice>()
                .find { it.options.any { option -> option.level == 1 } }

            assertNotNull("$id should choose level 1 spells during creation", choice)
            assertTrue(
                "$id needs more options than picks",
                choice!!.options.size >= choice.count,
            )
        }
    }

    /**
     * The Artificer bug was an empty spell list; this walks every caster from level 1 to 20
     * taking exactly what the picker asks for, and fails if it ever asks for more than it can
     * offer. That is what would have caught the Ranger without anyone reporting it.
     */
    @Test
    fun `no level up ever asks for more spells than it can offer`() {
        ClassData.ALL.forEach { charClass ->
            val known = mutableListOf<KnownSpell>()
            charClass.choices.filterIsInstance<ClassChoice.CantripChoice>().forEach { choice ->
                choice.options.take(choice.count).forEach {
                    known += KnownSpell(
                        it.id, it.name, it.level, it.school, it.description,
                        source = charClass.name,
                    )
                }
            }

            (1..19).forEach { from ->
                val state = LevelUpState(
                    character = character(classId = charClass.id, level = from, known = known.toList()),
                    levellingClassId = charClass.id,
                )
                val cantrips = state.cantripOptions
                val spells = state.spellOptions

                assertTrue(
                    "${charClass.id} $from->${from + 1} wants ${state.cantripsToLearn} cantrips " +
                        "but offers ${cantrips.size}",
                    state.cantripsToLearn <= cantrips.size,
                )
                assertTrue(
                    "${charClass.id} $from->${from + 1} wants ${state.spellsToLearn} spells " +
                        "but offers ${spells.size} with no manual entry",
                    state.spellsToLearn <= spells.size || state.needsManualSpellEntry,
                )

                known += cantrips.take(state.cantripsToLearn).map {
                    KnownSpell(it.id, it.name, it.level, it.school, it.description, source = charClass.name)
                }
                known += spells.take(state.spellsToLearn).map {
                    KnownSpell(it.id, it.name, it.level, it.school, it.description, source = charClass.name)
                }
            }
        }
    }

    @Test
    fun `a multiclass level reads the class it goes into, not the first one`() {
        val fighterWizard = character(classId = "fighter", level = 5).copy(
            classLevels = ClassLevels.of(character(classId = "fighter", level = 5)),
        )
        val state = LevelUpState(character = fighterWizard, levellingClassId = "wizard")

        assertTrue("a first Wizard level", state.isNewClass)
        assertEquals(1, state.targetClassLevel)
        assertTrue(
            "the picker must show the Wizard list",
            state.cantripOptions.any { it.id == "fire_bolt" },
        )
        assertTrue(
            "and only what a Wizard 1 could prepare",
            state.spellOptions.all { it.level <= 1 },
        )
    }

    @Test
    fun `the ranger and paladin get the spells their features promise`() {
        val ranger = character(classId = "ranger", level = 1)
        assertTrue(
            "Favored Enemy always has Hunter's Mark prepared",
            CharacterSpells.granted(ranger).any { it.spell.id == "hunters_mark" },
        )

        val paladin = character(classId = "paladin", level = 5)
        val granted = CharacterSpells.granted(paladin).map { it.spell.id }
        assertTrue("Paladin's Smite", granted.contains("divine_smite"))
        assertTrue("Faithful Steed", granted.contains("find_steed"))

        assertTrue(
            "a level 1 Paladin has neither yet",
            CharacterSpells.granted(character(classId = "paladin", level = 1)).isEmpty(),
        )
    }

    // ------------------------------------------------ Feat sub-choices

    @Test
    fun `every feat choice key names a real feat`() {
        val known = FeatData.ALL.map { it.id }.toSet()
        val unknown = FeatChoiceData.sourceIds() - known
        assertTrue("these feat choice keys match nothing: $unknown", unknown.isEmpty())
    }

    @Test
    fun `a feat granted through a species origin feat asks its own questions`() {
        val originFeat = OriginChoices.forSpecies("human").first()

        val before = OriginChoices.all(
            speciesId = "human", lineageId = null, classId = "fighter",
            classSelections = emptyMap(), backgroundId = "soldier",
        )
        assertTrue(
            "nothing from Magic Initiate before it is chosen",
            before.none { it.id.startsWith("feat:magic_initiate_wizard") },
        )

        val after = OriginChoices.all(
            speciesId = "human", lineageId = null, classId = "fighter",
            classSelections = emptyMap(), backgroundId = "soldier",
            originSelections = mapOf(originFeat.id to listOf("magic_initiate_wizard")),
        )
        val cantrips = after.find { it.id == "feat:magic_initiate_wizard:cantrips" }
        val spell = after.find { it.id == "feat:magic_initiate_wizard:spell" }

        assertNotNull("choosing Magic Initiate must raise its cantrip prompt", cantrips)
        assertNotNull("and its level 1 spell prompt", spell)
        assertEquals(2, cantrips!!.count)
        assertEquals(ChoiceKind.SPELL, cantrips.kind)
        assertTrue(cantrips.options.isNotEmpty())
    }

    @Test
    fun `a feat taken in place of an improvement asks its questions too`() {
        val wizard = character(classId = "wizard", level = 3)
        val state = LevelUpState(character = wizard, levellingClassId = "wizard")
            .copy(featId = "skill_expert")

        assertTrue(
            "Skill Expert wants an ability, a skill, and an Expertise",
            state.steps.contains(LevelUpStep.FEAT_CHOICES),
        )
        assertEquals(3, state.featChoices.size)
        assertFalse(
            "and the level can't be finished until they're answered",
            state.copy(requestedStep = LevelUpStep.FEAT_CHOICES).canAdvance,
        )

        val answered = state.copy(
            requestedStep = LevelUpStep.FEAT_CHOICES,
            selections = state.featChoices.associate { it.id to listOf(it.options.first().id) },
        )
        assertTrue(answered.canAdvance)
    }

    @Test
    fun `a feat with nothing to decide adds no step`() {
        val wizard = character(classId = "wizard", level = 3)
        val state = LevelUpState(character = wizard, levellingClassId = "wizard")
            .copy(featId = "savage_attacker")

        assertTrue(state.featChoices.isEmpty())
        assertFalse(state.steps.contains(LevelUpStep.FEAT_CHOICES))
    }

    @Test
    fun `feats that name several abilities ask which one goes up`() {
        FeatChoiceData.ABILITY_OPTIONS
            .filterValues { it.size > 1 }
            .forEach { (featId, options) ->
                val choice = OriginChoices.forFeat(featId)
                    .find { it.kind == ChoiceKind.ABILITY_SCORE }
                assertNotNull("$featId offers a choice of ability but never asks", choice)
                assertEquals(options.size, choice!!.options.size)
            }
    }

    @Test
    fun `a feat's ability increase reaches the score`() {
        // Durable names Constitution outright, so it applies with nothing to ask.
        val plain = character()
        val durable = character(featIds = listOf("durable"))
        assertEquals(
            CharacterCalculations.finalAbilityScores(plain)[Ability.CON]!! + 1,
            CharacterCalculations.finalAbilityScores(durable)[Ability.CON],
        )

        // Athlete offers Strength or Dexterity, so nothing applies until the player answers.
        val undecided = character(featIds = listOf("athlete"))
        assertEquals(
            CharacterCalculations.finalAbilityScores(plain),
            CharacterCalculations.finalAbilityScores(undecided),
        )

        val decided = character(
            featIds = listOf("athlete"),
            originSelections = mapOf("feat:athlete:ability" to listOf(Ability.DEX.name)),
        )
        assertEquals(
            CharacterCalculations.finalAbilityScores(plain)[Ability.DEX]!! + 1,
            CharacterCalculations.finalAbilityScores(decided)[Ability.DEX],
        )
        assertEquals(1, FeatBonuses.all(decided).size)
    }

    @Test
    fun `a feat cannot push a score past twenty, but an epic boon can pass thirty's door`() {
        val maxed = character(scores = mapOf("CON" to 20), featIds = listOf("durable"))
        assertEquals(20, CharacterCalculations.finalAbilityScores(maxed)[Ability.CON])

        val boon = character(
            scores = mapOf("CHA" to 20),
            featIds = listOf("boon_of_terror"),
        )
        assertEquals(21, CharacterCalculations.finalAbilityScores(boon)[Ability.CHA])
    }

    @Test
    fun `every spell a feat choice offers exists in the catalog`() {
        FeatData.ALL.forEach { feat ->
            OriginChoices.forFeat(feat.id)
                .filter { it.kind == ChoiceKind.SPELL }
                .forEach { choice ->
                    assertTrue(
                        "${feat.id} offers ${choice.id} with no spells",
                        choice.options.isNotEmpty(),
                    )
                    choice.options.forEach { option ->
                        assertNotNull(
                            "${choice.id} lists ${option.id}, which the catalog lacks",
                            SpellData.byId(option.id),
                        )
                    }
                }
        }
    }

    // ------------------------------------------------ Content that is not 2024

    @Test
    fun `species with no 2024 printing are gone, and stay gone`() {
        // Grung and Triton are 2014 species. Nothing on dnd2024.wikidot.com carries them, and
        // a character built on one would level up against rules the app cannot show.
        listOf("grung", "triton").forEach { id ->
            assertNull("$id has no 2024 printing", SpeciesData.byId(id))
        }
    }

    @Test
    fun `nothing still points at the species or spells that were removed`() {
        // A dangling grant is invisible until someone rolls the character that triggers it.
        val sources = SpellGrantData.sourceIds()
        listOf("grung", "triton").forEach { id ->
            assertFalse("$id still grants spells", sources.contains(id))
        }
        val catalogue = SpellData.ALL.map { it.id }.toSet()
        SpellGrantData.allGrantedSpellIds().forEach { spellId ->
            assertTrue("granted spell $spellId is not in the catalogue", spellId in catalogue)
        }
    }

    @Test
    fun `spells with no 2024 printing are gone from the catalogue`() {
        listOf("chaos_bolt", "wall_of_water", "raulothims_psychic_lance", "far_step")
            .forEach { id ->
                assertNull("$id is a 2014 spell", SpellData.ALL.firstOrNull { it.id == id })
            }
        // The id stays, because Draconic Sorcery grants it — but it is the 2024 spell now.
        assertEquals("Summon Dragon", SpellData.ALL.first { it.id == "summon_dragon" }.name)
    }
}
