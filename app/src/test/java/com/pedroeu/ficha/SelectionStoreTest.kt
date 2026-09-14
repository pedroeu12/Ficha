package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevel
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One question has one current answer, whichever screen asks and whichever map it lives in.
 *
 * Answers are stored in three maps, and an answer given at several levels lives under several
 * keys. That is a storage detail, and it leaked: one reader unioned the levels, another took
 * the level a feature was granted at, and Edit Mode wrote to a third place that the first two
 * never looked at. The reported symptom was "editing invocations doesn't work"; the cause was
 * that three readers and one writer disagreed about which answer counts.
 */
class SelectionStoreTest {

    private fun warlock(
        level: Int,
        subclassId: String? = "fiend",
        levelSelections: Map<String, List<String>> = emptyMap(),
        originSelections: Map<String, List<String>> = emptyMap(),
    ) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "warlock",
        subclassId = subclassId, backgroundId = "soldier", level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        knownSpells = listOf(
            com.pedroeu.ficha.domain.KnownSpell("chill_touch", "Chill Touch", 0, "Necromancy", ""),
            com.pedroeu.ficha.domain.KnownSpell("toll_the_dead", "Toll the Dead", 0, "Necromancy", ""),
        ),
        levelSelections = levelSelections,
        originChoiceSelections = originSelections,
    )

    private fun resolved(pc: PlayerCharacter, choiceId: String) =
        ChoiceResolver.all(pc).first { it.choice.id == choiceId }

    private val cantripQuestion = "invocation:agonizing_blast:cantrip"

    // ------------------------------------------------------------------ Newest wins

    @Test
    fun `a sub-choice re-answered at a later level has one answer, the newest`() {
        val pc = warlock(
            level = 12,
            levelSelections = mapOf(
                "5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast", "devils_sight"),
                "5:$cantripQuestion" to listOf("eldritch_blast"),
                "12:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast", "devils_sight"),
                "12:$cantripQuestion" to listOf("chill_touch"),
            ),
        )
        assertEquals(
            "a one-pick question showed two picks, the union of two levels",
            listOf("chill_touch"),
            resolved(pc, cantripQuestion).selectedIds,
        )
    }

    @Test
    fun `an answer changed on the sheet is the answer the sheet shows`() {
        val before = warlock(
            level = 5,
            levelSelections = mapOf(
                "5:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast", "devils_sight"),
                "5:$cantripQuestion" to listOf("eldritch_blast"),
            ),
        )
        val question = resolved(before, cantripQuestion)
        // Edit Mode answers an origin question at level 0, which used to land in a map the
        // reader only consulted when the level map was empty — so the write was ignored.
        val after = ChoiceGrants.answer(before, question.choice, question.level, listOf("chill_touch"))

        assertEquals(listOf("chill_touch"), resolved(after, cantripQuestion).selectedIds)
        assertEquals(
            "every reader agrees, including the one grants are read from",
            listOf("chill_touch"),
            ChoiceResolver.answers(after)[cantripQuestion],
        )
    }

    @Test
    fun `a choice revisited on level up shows its newest answer on the sheet`() {
        // The level-up flow records a revisited answer at the level it was revisited, while the
        // feature that asked it lives at level 3. The Features tab read level 3 and showed
        // fire; the granted spells read the newest and followed water.
        val pc = warlock(
            level = 4, subclassId = "primordial_patron",
            levelSelections = mapOf(
                "3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("fire"),
                "4:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("water"),
            ),
        )
        val shown = ChoiceResolver.subclassFeatureChoices(pc)
            .first { it.choice.id == SubclassData.ELEMENT_CHOICE_ID }
        assertEquals(listOf("water"), shown.selectedIds)
        assertEquals(
            ChoiceResolver.answers(pc)[SubclassData.ELEMENT_CHOICE_ID],
            shown.selectedIds,
        )
    }

    @Test
    fun `changing a revisited choice from the sheet changes what the rules read`() {
        val pc = warlock(
            level = 4, subclassId = "primordial_patron",
            levelSelections = mapOf(
                "3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("fire"),
                "4:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("water"),
            ),
        )
        val element = ChoiceResolver.subclassFeatureChoices(pc)
            .first { it.choice.id == SubclassData.ELEMENT_CHOICE_ID }
        // The sheet writes at the feature's level, 3, while a newer answer sits at level 4.
        // Without clearing the other homes the level 4 answer would keep winning.
        val changed = ChoiceGrants.answer(pc, element.choice, element.level, listOf("earth"))

        assertEquals(listOf("earth"), ChoiceResolver.answers(changed)[SubclassData.ELEMENT_CHOICE_ID])
        val spells = CharacterSpells.granted(changed).map { it.spell.id }
        val fireSpells = CharacterSpells.granted(pc.copy(levelSelections = mapOf(
            "3:${SubclassData.ELEMENT_CHOICE_ID}" to listOf("fire"),
        ))).map { it.spell.id }
        assertTrue(
            "the patron's spells should follow the element the sheet now shows",
            spells != fireSpells || spells.isEmpty(),
        )
    }

    // ------------------------------------------------------------------ The invariant

    /** Every reader of an answer agrees with every other, for every question a character has. */
    @Test
    fun `every resolved choice shows the same answer the rules read`() {
        val shapes = listOf(
            warlock(
                level = 12,
                levelSelections = mapOf(
                    "2:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast"),
                    "2:$cantripQuestion" to listOf("eldritch_blast"),
                    "12:${ProgressionData.INVOCATION_CHOICE_ID}" to listOf("agonizing_blast", "pact_tome"),
                    "12:$cantripQuestion" to listOf("chill_touch"),
                ),
            ),
            PlayerCharacter(
                id = "t", name = "T", speciesId = "human", classId = "fighter",
                subclassId = "champion", backgroundId = "soldier", level = 12,
                baseAbilityScores = Ability.ALL.associate { it.name to 14 },
                levelSelections = mapOf(
                    "1:class:fighter:weapon_mastery" to listOf("longsword", "greataxe", "shortbow"),
                    "4:class:fighter:weapon_mastery" to listOf("longsword", "greataxe", "shortbow", "dagger"),
                    "10:class:fighter:weapon_mastery" to listOf("maul", "greataxe", "shortbow", "dagger", "longbow"),
                ),
            ),
            PlayerCharacter(
                id = "t", name = "T", speciesId = "human", classId = "cleric",
                subclassId = "life_domain", backgroundId = "soldier", level = 7,
                classLevels = listOf(
                    ClassLevel("cleric", 3, "life_domain", isStarting = true),
                    ClassLevel("fighter", 4, null),
                ),
                baseAbilityScores = Ability.ALL.associate { it.name to 14 },
                levelSelections = mapOf(
                    "1:class:fighter:weapon_mastery" to listOf("longsword", "greataxe", "shortbow"),
                    "4:class:fighter:weapon_mastery" to listOf("maul", "greataxe", "shortbow", "dagger"),
                ),
            ),
        )
        shapes.forEach { pc ->
            val answers = ChoiceResolver.answers(pc)
            ChoiceResolver.all(pc).forEach { resolved ->
                assertEquals(
                    "${resolved.choice.id}: the sheet and the rules disagree about the answer",
                    answers[resolved.choice.id].orEmpty(),
                    resolved.selectedIds,
                )
            }
        }
    }

    /** After a write there is exactly one home for the answer, whichever it used to have. */
    @Test
    fun `a write leaves one home for the answer`() {
        val pc = warlock(
            level = 12,
            levelSelections = mapOf(
                "5:$cantripQuestion" to listOf("eldritch_blast"),
                "12:$cantripQuestion" to listOf("chill_touch"),
            ),
            originSelections = mapOf(cantripQuestion to listOf("toll_the_dead")),
        )
        val written = ChoiceResolver.withAnswer(pc, cantripQuestion, 0, listOf("chill_touch"))
        assertTrue(written.levelSelections.keys.none { it.endsWith(":$cantripQuestion") })
        assertEquals(listOf("chill_touch"), written.originChoiceSelections[cantripQuestion])

        val atLevel = ChoiceResolver.withAnswer(pc, cantripQuestion, 12, listOf("toll_the_dead"))
        assertEquals(setOf("12:$cantripQuestion"), atLevel.levelSelections.keys)
        assertTrue(cantripQuestion !in atLevel.originChoiceSelections)
    }
}
