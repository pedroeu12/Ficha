package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.CustomOption
import com.pedroeu.ficha.domain.CustomOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Answering the sheet's questions with something the books do not contain.
 *
 * The two things a table actually asks for, and they are the same record: an option that is
 * not in any book, and an option that is but does something else here.
 */
class CustomOptionTest {

    private val warlock = SheetAudit.character("warlock", 5, "fiend")
    private val invocations = ProgressionData.INVOCATION_CHOICE_ID

    private fun invocationRow(character: PlayerCharacter): ResolvedChoice =
        ChoiceResolver.all(character).first { it.choice.id == invocations }

    private fun written(name: String, description: String = "") = CustomOption(
        id = CustomOptions.newId(),
        choiceId = invocations,
        name = name,
        description = description,
    )

    // ================================================================ Writing something new

    @Test
    fun `an option the player writes is offered by the question it was written for`() {
        val mine = written("Whispering Dark", "You always know which way is down.")
        val character = CustomOptions.write(warlock, mine)

        val options = invocationRow(character).choice.options
        assertTrue(
            "the written invocation is not in the list",
            options.any { it.id == mine.id && it.name == "Whispering Dark" },
        )
    }

    /**
     * And nowhere else. An invocation written for a Warlock has no business turning up in the
     * Fighter's weapon masteries, and a list that collects everything anyone ever wrote stops
     * being a list of invocations.
     */
    @Test
    fun `a written option belongs to its own question`() {
        val character = CustomOptions.write(warlock, written("Whispering Dark"))

        val elsewhere = ChoiceResolver.all(character)
            .filter { it.choice.id != invocations }
            .filter { row -> row.choice.options.any { it.name == "Whispering Dark" } }

        assertTrue(
            "it leaked into ${elsewhere.map { it.choice.id }}",
            elsewhere.isEmpty(),
        )
    }

    @Test
    fun `a written option can be picked and stays picked`() {
        val mine = written("Whispering Dark")
        var character = CustomOptions.write(warlock, mine)
        val row = invocationRow(character)

        character = ChoiceGrants.toggle(character, row.choice, row.level, mine.id, unbounded = true)

        assertTrue(mine.id in invocationRow(character).selectedIds)
        assertTrue(
            "the sheet cannot name what was picked",
            "Whispering Dark" in invocationRow(character).selectedNames,
        )
    }

    /**
     * Deleting one the character had taken unpicks it too.
     *
     * Otherwise the answer points at an option that no longer exists: the sheet prints a blank
     * line where an invocation should be, and [SheetAudit] rightly calls it a stored answer
     * that is not among the options.
     */
    @Test
    fun `deleting a written option takes it off the sheet as well`() {
        val mine = written("Whispering Dark")
        var character = CustomOptions.write(warlock, mine)
        val row = invocationRow(character)
        character = ChoiceGrants.toggle(character, row.choice, row.level, mine.id, unbounded = true)

        character = CustomOptions.erase(character, mine)

        assertTrue(mine.id !in invocationRow(character).selectedIds)
        assertEquals(emptyList<String>(), SheetAudit.complaints("after a deletion", character))
    }

    // ================================================================ Writing over a printed one

    @Test
    fun `a rewrite changes what an option says and nothing else`() {
        val book = invocationRow(warlock).choice.options.first { it.id == "lifedrinker" }
        val rewrite = CustomOption(
            id = "lifedrinker",
            choiceId = invocations,
            name = "Soulbrand",
            description = "Your pact weapon burns.",
        )

        val after = invocationRow(CustomOptions.write(warlock, rewrite)).choice.options
            .first { it.id == "lifedrinker" }

        assertEquals("Soulbrand", after.name)
        assertEquals("Your pact weapon burns.", after.description)
        // What the rules attached to it survives, so it is still gated as the books gate it.
        assertEquals(book.minLevel, after.minLevel)
        assertEquals(book.requiresOptions, after.requiresOptions)
        assertEquals(book.prerequisite, after.prerequisite)
    }

    @Test
    fun `a rewrite keeps the option's place in the list`() {
        val before = invocationRow(warlock).choice.options.map { it.id }
        val rewrite = CustomOption("lifedrinker", invocations, "Soulbrand")

        val after = invocationRow(CustomOptions.write(warlock, rewrite)).choice.options.map { it.id }

        assertEquals(before, after)
    }

    @Test
    fun `resetting a rewrite puts the book's wording back and keeps the option taken`() {
        val rewrite = CustomOption("lifedrinker", invocations, "Soulbrand")
        var character = CustomOptions.write(warlock, rewrite)
        val row = invocationRow(character)
        character = ChoiceGrants.toggle(character, row.choice, row.level, "lifedrinker", unbounded = true)

        character = CustomOptions.erase(character, rewrite)

        val option = invocationRow(character).choice.options.first { it.id == "lifedrinker" }
        assertEquals("Lifedrinker", option.name)
        assertTrue(
            "resetting the wording took the invocation away",
            "lifedrinker" in invocationRow(character).selectedIds,
        )
    }

    // ================================================================ Housekeeping

    @Test
    fun `writing the same option again replaces it rather than adding a second`() {
        val mine = written("Whispering Dark")
        val character = CustomOptions.write(
            CustomOptions.write(warlock, mine),
            mine.copy(name = "Whispering Deep"),
        )

        val matching = invocationRow(character).choice.options.filter { it.id == mine.id }
        assertEquals(1, matching.size)
        assertEquals("Whispering Deep", matching.single().name)
    }

    @Test
    fun `an option written with no name is not an option`() {
        val mine = written("Whispering Dark")
        val character = CustomOptions.write(
            CustomOptions.write(warlock, mine),
            mine.copy(name = ""),
        )

        assertNull(character.customOptions.find { it.id == mine.id })
    }

    /** A written id is its own; a rewrite is the book's. The sheet tells them apart by that. */
    @Test
    fun `a written option and a rewrite are told apart by their id`() {
        assertTrue(written("Whispering Dark").isRewrite.not())
        assertTrue(CustomOption("lifedrinker", invocations, "Soulbrand").isRewrite)
    }

    /** Nothing written should ever leave the character in a state the audit objects to. */
    @Test
    fun `a character carrying written options still passes the audit`() {
        var character = CustomOptions.write(warlock, written("Whispering Dark", "It whispers."))
        character = CustomOptions.write(
            character,
            CustomOption("lifedrinker", invocations, "Soulbrand", "It burns."),
        )
        val row = invocationRow(character)
        val mine = row.choice.options.first { CustomOptions.isCustom(it.id) }
        character = ChoiceGrants.toggle(character, row.choice, row.level, mine.id, unbounded = true)

        assertEquals(emptyList<String>(), SheetAudit.complaints("homebrew Warlock", character))
    }

    /** A feat of the player's own is an id the books do not know, and the sheet copes. */
    @Test
    fun `a feat written by the player does not upset anything that reads feats`() {
        val id = CustomOptions.newId()
        val character = warlock.copy(
            featIds = warlock.featIds + id,
            textOverrides = warlock.textOverrides +
                ("feat:$id:name" to "Blood of the Pact") +
                ("feat:$id:description" to "You are owed a favour."),
        )

        assertEquals(emptyList<String>(), SheetAudit.complaints("a written feat", character))
        assertNotNull(character.textOverrides["feat:$id:name"])
    }
}
