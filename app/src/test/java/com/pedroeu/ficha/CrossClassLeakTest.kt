package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterDcs
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PerUseChoices
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One class's features must never appear on another class's sheet.
 *
 * A Monk was shown the Artificer's Replicate Magic Item, because a screen rendered that
 * section without first asking whether the character had it. The specific fix is one line;
 * this is the general one. Everything the sheet derives is keyed by the source that grants
 * it — `artillerist:eldritch_cannon`, `druid:wild_shape` — so a Monk carrying anything keyed
 * to a class or subclass that isn't theirs is a leak, whatever caused it.
 */
class CrossClassLeakTest {

    private fun character(classId: String, subclassId: String? = null) = PlayerCharacter(
        id = "t",
        name = "Test",
        // A species and background with nothing of their own, so anything that turns up is
        // unambiguously coming from the class.
        speciesId = "human",
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = 20,
        baseAbilityScores = Ability.ALL.associate { it.name to 16 },
    )

    /** The prefixes that would mark something as belonging to a different class. */
    private fun foreignPrefixes(classId: String): List<String> {
        val otherClasses = ClassData.ALL.map { it.id }.filterNot { it == classId }
        val otherSubclasses = SubclassData.ALL
            .filterNot { it.classId == classId }
            .map { it.id }
        return (otherClasses + otherSubclasses).map { "$it:" }
    }

    @Test
    fun `no class carries another class's limited-use pools`() {
        ClassData.ALL.forEach { charClass ->
            val ids = CharacterResources.definitions(character(charClass.id)).map { it.id }
            val foreign = foreignPrefixes(charClass.id)

            val leaked = ids.filter { id -> foreign.any { id.startsWith(it) } }
            assertTrue(
                "a ${charClass.name} is carrying pools that belong to another class: $leaked",
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `no class carries another class's per-use choices`() {
        ClassData.ALL.forEach { charClass ->
            val ids = PerUseChoices.all(character(charClass.id)).map { it.choice.id }
            val foreign = foreignPrefixes(charClass.id)

            val leaked = ids.filter { id -> foreign.any { id.startsWith(it) } }
            assertTrue(
                "a ${charClass.name} is being asked another class's questions: $leaked",
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `no class carries another class's save DCs`() {
        ClassData.ALL.forEach { charClass ->
            val ids = CharacterDcs.all(character(charClass.id)).map { it.id }
            val foreign = foreignPrefixes(charClass.id)

            val leaked = ids.filter { id -> foreign.any { id.startsWith(it) } }
            assertTrue(
                "a ${charClass.name} has save DCs from another class: $leaked",
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `a subclass brings its own and no sibling's`() {
        // The narrower version of the same rule: picking Artillerist must not bring Battle
        // Smith's Arcane Jolt along with it.
        SubclassData.ALL.forEach { subclass ->
            val character = character(subclass.classId, subclass.id)
            val siblings = SubclassData.forClass(subclass.classId)
                .filterNot { it.id == subclass.id }
                .map { "${it.id}:" }
            if (siblings.isEmpty()) return@forEach

            val ids = CharacterResources.definitions(character).map { it.id } +
                PerUseChoices.all(character).map { it.choice.id } +
                CharacterDcs.all(character).map { it.id }

            val leaked = ids.filter { id -> siblings.any { id.startsWith(it) } }
            assertTrue(
                "${subclass.name} is carrying a sibling subclass's features: $leaked",
                leaked.isEmpty(),
            )
        }
    }

    @Test
    fun `no class is asked another class's questions during creation or level up`() {
        // Choices are the other route a feature reaches the sheet, and the resolver walks the
        // class tables — so a mis-filed choice would surface here rather than in a pool.
        ClassData.ALL.forEach { charClass ->
            val choices = ChoiceResolver.classFeatureChoices(character(charClass.id))
            val foreign = foreignPrefixes(charClass.id)

            val leaked = choices
                .map { it.choice.id }
                .filter { id -> foreign.any { id.startsWith(it) } }

            assertTrue(
                "a ${charClass.name} is asked another class's choices: $leaked",
                leaked.isEmpty(),
            )
        }
    }
}
