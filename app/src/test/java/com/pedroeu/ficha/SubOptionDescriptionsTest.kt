package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A resource tracker that only says "3 Focus Points" is useless at the table — you also need
 * to know that Flurry of Blows exists and what it does. These tests hold every named thing a
 * pool can be spent on to a real paragraph of rules text, whether the rules granted it or the
 * player chose it.
 */
class SubOptionDescriptionsTest {

    private companion object {
        /** Short enough to allow a terse rule, long enough to reject a stub or a bare name. */
        const val MIN_RULES_TEXT = 40
    }

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        levelSelections: Map<String, List<String>> = emptyMap(),
        classChoiceSelections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = levelSelections,
        classChoiceSelections = classChoiceSelections,
    )

    // ------------------------------------------------------------- granted options

    @Test
    fun `a monk's focus points list the abilities they pay for, each with its rules text`() {
        val monk = character("monk", level = 5)
        val focus = CharacterResources.definitions(monk).first { it.id == "monk:focus" }

        val names = focus.options.map { it.name }
        assertTrue("Flurry of Blows must be listed", names.contains("Flurry of Blows"))
        assertTrue("Patient Defense must be listed", names.contains("Patient Defense"))
        assertTrue("Step of the Wind must be listed", names.contains("Step of the Wind"))
        assertTrue("Stunning Strike unlocks at 5", names.contains("Stunning Strike"))

        focus.options.forEach { option ->
            assertTrue(
                "${option.name} needs real rules text, not a name",
                option.description.length >= MIN_RULES_TEXT,
            )
        }
    }

    @Test
    fun `options gated behind a level do not appear before it`() {
        // Focus Points arrive at level 2, which is the earliest the pool exists at all.
        val fresh = character("monk", level = 2)
        val names = CharacterResources.definitions(fresh)
            .first { it.id == "monk:focus" }
            .options.map { it.name }

        assertTrue("Flurry of Blows is available at 1", names.contains("Flurry of Blows"))
        assertFalse("Stunning Strike is a level 5 feature", names.contains("Stunning Strike"))
    }

    @Test
    fun `a subclass adds its own ways to spend the shared pool`() {
        val mercy = character("monk", level = 3, subclassId = "mercy")
        val plain = character("monk", level = 3)

        val mercyNames = CharacterResources.definitions(mercy)
            .first { it.id == "monk:focus" }.options.map { it.name }
        val plainNames = CharacterResources.definitions(plain)
            .first { it.id == "monk:focus" }.options.map { it.name }

        assertTrue(
            "Hand of Harm and Hand of Healing come from the subclass",
            mercyNames.size > plainNames.size,
        )
        assertTrue(mercyNames.containsAll(plainNames))
    }

    // ------------------------------------------------------------- chosen options

    @Test
    fun `metamagic the player picked shows up under sorcery points with its rules text`() {
        val sorcerer = character(
            classId = "sorcerer",
            level = 3,
            subclassId = "draconic",
            levelSelections = mapOf("2:metamagic_2" to listOf("twinned", "quickened")),
        )

        val points = CharacterResources.definitions(sorcerer)
            .first { it.id == "sorcerer:sorcery_points" }
        val chosen = points.options.filter { it.isChosen }

        assertEquals(
            listOf("Twinned Spell", "Quickened Spell"),
            chosen.map { it.name },
        )
        chosen.forEach { option ->
            assertTrue(
                "${option.name} needs its full rules text",
                option.description.length >= MIN_RULES_TEXT,
            )
            assertTrue(
                "${option.name} should say what it costs",
                option.cost.isNotBlank(),
            )
        }
    }

    @Test
    fun `maneuvers the player picked show up under superiority dice`() {
        val fighter = character(
            classId = "fighter",
            level = 3,
            subclassId = "battle_master",
            levelSelections = mapOf("3:bm_maneuvers_3" to listOf("riposte", "trip_attack", "parry")),
        )

        val dice = CharacterResources.definitions(fighter)
            .first { it.id == "battle_master:superiority" }
        val chosen = dice.options.filter { it.isChosen }.map { it.name }

        assertEquals(listOf("Riposte", "Trip Attack", "Parry"), chosen)
        dice.options.forEach { option ->
            assertTrue(
                "${option.name} needs its full rules text",
                option.description.length >= MIN_RULES_TEXT,
            )
        }
    }

    @Test
    fun `arcane shot options the player picked show up under the arcane shot pool`() {
        val archer = character(
            classId = "fighter",
            level = 3,
            subclassId = "arcane_archer",
            levelSelections = mapOf("3:arcane_shot_3" to listOf("banishing", "seeking")),
        )

        val pool = CharacterResources.definitions(archer)
            .first { it.id == "arcane_archer:arcane_shot" }

        assertEquals(
            listOf("Banishing Shot", "Seeking Shot"),
            pool.options.filter { it.isChosen }.map { it.name },
        )
    }

    @Test
    fun `an unanswered choice contributes nothing to the tracker`() {
        val sorcerer = character("sorcerer", level = 3, subclassId = "draconic")
        val points = CharacterResources.definitions(sorcerer)
            .first { it.id == "sorcerer:sorcery_points" }

        assertTrue(
            "nothing is chosen yet, so nothing is listed as chosen",
            points.options.none { it.isChosen },
        )
    }

    // ------------------------------------------------------------- general sweep

    @Test
    fun `every option of every resource-backed choice carries rules text`() {
        val subclassChoices = SubclassData.ALL.flatMap { subclass ->
            subclass.features.flatMap { feature ->
                feature.choices.map { subclass.name to it }
            }
        }
        val progressionChoices = ClassData.ALL.flatMap { charClass ->
            ProgressionData.forClass(charClass.id)
                ?.features.orEmpty()
                .flatMap { feature -> feature.choices.map { charClass.name to it } }
        }

        val backed = (subclassChoices + progressionChoices)
            .filter { (_, choice) -> choice.resourceId != null }

        assertTrue("the sweep must actually find some", backed.isNotEmpty())

        backed.forEach { (owner, choice) ->
            choice.options.forEach { option ->
                assertTrue(
                    "$owner / ${choice.label} / ${option.name} has no rules text",
                    option.description.length >= MIN_RULES_TEXT,
                )
            }
        }
    }

    @Test
    fun `no granted option anywhere is left without rules text`() {
        val everySubclass: List<String?> = listOf(null) + SubclassData.ALL.map { it.id }

        ClassData.ALL.forEach { charClass ->
            everySubclass.forEach { subclassId ->
                val pc = character(charClass.id, level = 20, subclassId = subclassId)
                CharacterResources.definitions(pc).forEach { def ->
                    def.options.forEach { option ->
                        assertTrue(
                            "${charClass.name}/${subclassId ?: "none"} — " +
                                "${def.name} / ${option.name} has no rules text",
                            option.description.length >= MIN_RULES_TEXT,
                        )
                        assertTrue(
                            "${def.name} / ${option.name} has a blank name",
                            option.name.isNotBlank(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `every tracker explains itself, either with options or its own rules text`() {
        val bare = sortedSetOf<String>()
        val everySubclass: List<String?> = listOf(null) + SubclassData.ALL.map { it.id }

        ClassData.ALL.forEach { charClass ->
            everySubclass.forEach { subclassId ->
                val pc = character(charClass.id, level = 20, subclassId = subclassId)
                CharacterResources.definitions(pc)
                    .filter { it.options.isEmpty() && it.description.isBlank() }
                    .forEach { bare.add("${it.id} (${it.name})") }
            }
        }

        assertTrue(
            "these pools show a name and a number and nothing else: $bare",
            bare.isEmpty(),
        )
    }

    @Test
    fun `a pool named after its feature borrows that feature's rules text`() {
        val fighter = character("fighter", level = 2)
        val surge = CharacterResources.definitions(fighter)
            .first { it.id == "fighter:action_surge" }

        assertTrue(
            "Action Surge should carry the class feature's text",
            surge.description.length >= MIN_RULES_TEXT,
        )
    }

    @Test
    fun `a pool named after a spell borrows the spell's rules text`() {
        val ranger = character("ranger", level = 1)
        val mark = CharacterResources.definitions(ranger)
            .first { it.id == "ranger:favored_enemy" }

        assertTrue(
            "Free Hunter's Mark should carry the spell's text",
            mark.description.length >= MIN_RULES_TEXT,
        )
    }

    @Test
    fun `option ids stay unique within a pool so the list can be keyed`() {
        val monk = character("monk", level = 20, subclassId = "open_hand")
        CharacterResources.definitions(monk).forEach { def ->
            val ids = def.options.map { it.id }
            assertEquals("${def.name} has duplicate option ids", ids.distinct().size, ids.size)
        }
    }
}
