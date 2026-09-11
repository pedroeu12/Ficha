package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.domain.ChoiceGraph
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An option that is itself a question gets asked, wherever it was taken.
 *
 * This is the bug that kept coming back under a different name: a Sage's Magic Initiate
 * granting cantrips nobody picked, an Origin feat taken through Versatile asking nothing
 * further, an Eldritch Invocation added with no way to name the cantrip it modifies. Each was
 * a separate patch in a separate screen, because an option could not carry a question of its
 * own and nothing kept a list of which options implied one.
 *
 * Now the question lives on the option and one walk finds it. These tests are about the walk,
 * not about any particular invocation, which is the point.
 */
class ChoiceGraphTest {

    private fun warlock(level: Int, vararg invocations: String) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock",
        subclassId = if (level >= 3) "fiend" else null,
        backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        levelSelections = mapOf(
            "$level:${ProgressionData.INVOCATION_CHOICE_ID}" to invocations.toList(),
        ),
    )

    private fun invocationChoice(level: Int): Choice =
        ProgressionData.forClass("warlock")!!.features
            .filter { it.level <= level }
            .flatMap { it.choices }
            .last { it.id == ProgressionData.INVOCATION_CHOICE_ID }

    // ------------------------------------------------------------------ The walk itself

    @Test
    fun `an unanswered choice raises nothing`() {
        val root = Choice(
            id = "root", label = "Root",
            options = listOf(
                ChoiceOption("a", "A", grants = listOf(Choice(id = "follow", label = "Follow"))),
            ),
        )
        assertEquals(emptyList<Choice>(), ChoiceGraph.followUps(listOf(root), emptyMap()))
    }

    @Test
    fun `answering an option raises the question it carries`() {
        val root = Choice(
            id = "root", label = "Root",
            options = listOf(
                ChoiceOption("a", "A", grants = listOf(Choice(id = "follow", label = "Follow"))),
                ChoiceOption("b", "B"),
            ),
        )
        val raised = ChoiceGraph.followUps(listOf(root), mapOf("root" to listOf("a")))
        assertEquals(listOf("follow"), raised.map { it.id })
    }

    @Test
    fun `only the option actually taken raises its question`() {
        val root = Choice(
            id = "root", label = "Root",
            options = listOf(
                ChoiceOption("a", "A", grants = listOf(Choice(id = "from_a", label = "A"))),
                ChoiceOption("b", "B", grants = listOf(Choice(id = "from_b", label = "B"))),
            ),
        )
        val raised = ChoiceGraph.followUps(listOf(root), mapOf("root" to listOf("b")))
        assertEquals(listOf("from_b"), raised.map { it.id })
    }

    @Test
    fun `a question raised by an answer can raise one of its own`() {
        val deep = Choice(id = "deep", label = "Deep")
        val middle = Choice(
            id = "middle", label = "Middle",
            options = listOf(ChoiceOption("m", "M", grants = listOf(deep))),
        )
        val root = Choice(
            id = "root", label = "Root",
            options = listOf(ChoiceOption("a", "A", grants = listOf(middle))),
        )
        val raised = ChoiceGraph.followUps(
            listOf(root),
            mapOf("root" to listOf("a"), "middle" to listOf("m")),
        )
        assertEquals(listOf("middle", "deep"), raised.map { it.id })
    }

    /** A chain that loops back has to stop, not hang the screen drawing it. */
    @Test
    fun `an option that leads back to its own question terminates`() {
        lateinit var root: Choice
        val loop = Choice(
            id = "root", label = "Loop",
            options = listOf(ChoiceOption("a", "A")),
        )
        root = Choice(
            id = "root", label = "Root",
            options = listOf(ChoiceOption("a", "A", grants = listOf(loop))),
        )
        val all = ChoiceGraph.expand(listOf(root), mapOf("root" to listOf("a")))
        assertEquals(1, all.size)
    }

    // ------------------------------------------------------------------ Invocations

    @Test
    fun `an invocation that names a cantrip asks which one`() {
        listOf("agonizing_blast", "eldritch_spear", "repelling_blast").forEach { id ->
            val raised = ChoiceGraph.followUps(
                listOf(invocationChoice(5)),
                mapOf(ProgressionData.INVOCATION_CHOICE_ID to listOf(id)),
            )
            assertEquals("$id asks nothing", listOf("invocation:$id:cantrip"), raised.map { it.id })
            assertTrue(
                "$id offers no cantrip to choose",
                raised.single().options.isNotEmpty(),
            )
            assertEquals(ChoiceKind.SPELL, raised.single().kind)
        }
    }

    @Test
    fun `an invocation that names a damage type asks which one`() {
        listOf("elemental_overflow", "elemental_transmutation").forEach { id ->
            val raised = ChoiceGraph.followUps(
                listOf(invocationChoice(5)),
                mapOf(ProgressionData.INVOCATION_CHOICE_ID to listOf(id)),
            )
            assertEquals(listOf("invocation:$id:damage"), raised.map { it.id })
            assertEquals(
                listOf("Acid", "Cold", "Fire", "Lightning", "Thunder"),
                raised.single().options.map { it.name },
            )
        }
    }

    /** "choose three cantrips, and choose two level 1 spells that have the Ritual tag." */
    @Test
    fun `the Book of Shadows asks for three cantrips and two rituals`() {
        val raised = ChoiceGraph.followUps(
            listOf(invocationChoice(2)),
            mapOf(ProgressionData.INVOCATION_CHOICE_ID to listOf("pact_tome")),
        )
        assertEquals(
            listOf("invocation:pact_tome:cantrips", "invocation:pact_tome:ritual"),
            raised.map { it.id },
        )
        assertEquals(3, raised[0].count)
        assertEquals(2, raised[1].count)
        assertTrue("the rituals offered are not rituals", raised[1].options.isNotEmpty())
    }

    /**
     * Lessons of the First Ones grants an Origin feat, and that feat asks its own questions.
     *
     * Magic Initiate taken this way wants two cantrips and a level 1 spell, exactly as it
     * does when a background grants it. Nothing about the route should change the questions.
     */
    @Test
    fun `a feat granted by an invocation asks the feat's own questions`() {
        val raised = ChoiceGraph.followUps(
            listOf(invocationChoice(5)),
            mapOf(
                ProgressionData.INVOCATION_CHOICE_ID to listOf("lessons_of_the_first_ones"),
                "invocation:lessons_of_the_first_ones:feat" to listOf("magic_initiate_wizard"),
            ),
        )
        val fromFeat = FeatChoiceData.choicesFor("magic_initiate_wizard", "Magic Initiate")
        assertTrue("Magic Initiate asks nothing on its own", fromFeat.isNotEmpty())
        fromFeat.forEach { expected ->
            assertTrue(
                "the invocation's feat never asked ${expected.id}",
                raised.any { it.id == expected.id },
            )
        }
    }

    // ------------------------------------------------------------------ On the sheet

    /**
     * The reported bug, end to end: the invocation is on the sheet and so is its question.
     *
     * It used to reach the sheet through a branch in OriginChoices that knew the Warlock by
     * name. Everything here goes through the general walk instead.
     */
    @Test
    fun `a warlock's sheet shows the question the invocation raised`() {
        val character = warlock(5, "agonizing_blast", "pact_tome", "devils_sight")
        val ids = ChoiceResolver.originChoices(character).map { it.choice.id }

        assertTrue("no cantrip question for Agonizing Blast",
            "invocation:agonizing_blast:cantrip" in ids)
        assertTrue("no cantrip question for the Book of Shadows",
            "invocation:pact_tome:cantrips" in ids)
        assertTrue("no ritual question for the Book of Shadows",
            "invocation:pact_tome:ritual" in ids)
        assertTrue("Devil's Sight asks nothing and should raise nothing",
            ids.none { it.startsWith("invocation:devils_sight") })
    }

    @Test
    fun `the sheet does not show a question the character never raised`() {
        val character = warlock(5, "devils_sight", "armor_of_shadows")
        val ids = ChoiceResolver.originChoices(character).map { it.choice.id }
        assertTrue(
            "questions appeared for invocations nobody took: $ids",
            ids.none { it.startsWith("invocation:") },
        )
    }

    /**
     * A question that has not been answered reads as unanswered, which is what puts the
     * "Choose" button on the sheet rather than a blank.
     */
    @Test
    fun `an unanswered follow-up is reported as unanswered`() {
        val character = warlock(5, "agonizing_blast")
        val resolved = ChoiceResolver.originChoices(character)
            .single { it.choice.id == "invocation:agonizing_blast:cantrip" }
        assertTrue("an unanswered question claims to be answered", !resolved.isAnswered)
    }

    // ------------------------------------------------------------------ Everything else

    /**
     * Every option in the game that carries a question carries a usable one.
     *
     * A grant with no options, or one asking for more than it offers, is a question the
     * player cannot answer — and because answering is what unblocks creation and levelling,
     * it is a character that cannot be finished.
     */
    @Test
    fun `every question an option raises can actually be answered`() {
        val everyOption = buildList {
            ProgressionData.ALL.forEach { p ->
                p.features.forEach { f -> f.choices.forEach { addAll(it.options) } }
            }
            SubclassData.ALL.forEach { s ->
                s.features.forEach { f -> f.choices.forEach { addAll(it.options) } }
            }
        }
        everyOption.filter { it.grants.isNotEmpty() }.forEach { option ->
            option.grants.forEach { granted ->
                assertTrue(
                    "${option.name} raises '${granted.label}' with nothing to pick from",
                    granted.options.isNotEmpty(),
                )
                assertTrue(
                    "${option.name} raises '${granted.label}', which wants ${granted.count} " +
                        "of ${granted.options.size}",
                    granted.count <= granted.options.size,
                )
                assertTrue(
                    "${option.name} raises a question with no id",
                    granted.id.isNotBlank(),
                )
            }
        }
    }
}
