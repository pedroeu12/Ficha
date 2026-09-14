package com.pedroeu.ficha

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interface rules that keep being broken, enforced against the source that breaks them.
 *
 * Every rule below is a bug that shipped, was reported, was fixed where it was reported, and
 * came back somewhere else — because the fix was an edit to one file and nothing stopped the
 * next screen from making the same mistake. A list of options that could not be scrolled to.
 * A dialog holding its own copy of an answer, so the second tap undid the first. A picker that
 * forgot to grey out what the character could not take. A dialog offering to reset a number to
 * the value the player had just pinned on it.
 *
 * None of those is findable by reading the rules content, and none of them is findable by a
 * unit test of the domain, because they are all in the wiring. They are findable by reading
 * the source for the shape of the mistake, which is what this does.
 *
 * Adding a rule here is the cheapest thing in the repository and the only thing that has ever
 * stopped one of these from coming back. When a bug in a screen is reported, write the shape
 * of it down here before fixing it.
 */
class UiInvariantTest {

    private val uiRoot: File by lazy {
        listOf(
            File("src/main/java/com/pedroeu/ficha/ui"),
            File("app/src/main/java/com/pedroeu/ficha/ui"),
        ).first { it.isDirectory }
    }

    private data class Source(val name: String, val text: String)

    private val sources: List<Source> by lazy {
        uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { Source(it.name, it.readText()) }
            .toList()
    }

    private fun fail(rule: String, offenders: List<String>) {
        assertTrue(
            "$rule\n\n" + offenders.joinToString("\n") { "  - $it" } + "\n",
            offenders.isEmpty(),
        )
    }

    /** The lines of one call, from its opening to the matching close, roughly enough to read. */
    private fun callsTo(text: String, name: String): List<String> {
        val calls = mutableListOf<String>()
        var index = text.indexOf("$name(")
        while (index >= 0) {
            var depth = 0
            var i = index + name.length
            while (i < text.length) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                i++
            }
            calls += text.substring(index, minOf(i + 1, text.length))
            index = text.indexOf("$name(", index + 1)
        }
        return calls
    }

    // ================================================================ Reachability

    /**
     * A dialog that lists things to pick has to be scrollable.
     *
     * A Material dialog bounds its body and does not scroll it, so anything taller than the
     * dialog is drawn off the bottom with no way to reach it. A Warlock's invocation list is
     * twenty-eight options long: the player saw the first few, the one they wanted was never
     * among them, and the feature read as broken.
     */
    @Test
    fun `a dialog that lists options can be scrolled to the end of them`() {
        val listsOptions = listOf("ChoiceSection(", "SelectableCard(", "ExpandableOption(")
        val scrolls = listOf("verticalScroll", "LazyColumn", "LazyRow")

        val offenders = sources.filter { source ->
            source.text.contains("AlertDialog(") &&
                listsOptions.any { source.text.contains(it) } &&
                scrolls.none { source.text.contains(it) }
        }.map { "${it.name} puts a list of options in a dialog that cannot scroll" }

        fail(
            "A dialog's body is clipped, not scrolled, so every option past the fold is " +
                "unreachable. Bound the height and add verticalScroll, or use a LazyColumn.",
            offenders,
        )
    }

    // ================================================================ Live state

    /**
     * A screen remembers *what* is open, never a copy of it.
     *
     * A dialog that holds the resolved answer it opened with computes the next tap from the
     * answer as it stood before the first, so the second tap undoes the first and the list
     * never moves. Remember the id and resolve it again from the character on each pass.
     */
    @Test
    fun `no screen remembers a copy of something the player is editing`() {
        val snapshots = listOf("ResolvedChoice", "ResourceState", "ActiveChoice", "ActiveSummon")

        val offenders = sources.flatMap { source ->
            snapshots.filter { type ->
                source.text.contains("mutableStateOf<$type?>") ||
                    source.text.contains("mutableStateOf<$type>")
            }.map { "${source.name} remembers a $it instead of its id" }
        }

        fail(
            "Remembering a resolved value freezes it: the character changes underneath and " +
                "the screen keeps showing what it opened with. Remember the id.",
            offenders,
        )
    }

    // ================================================================ Eligibility

    /**
     * Every picker applies the same eligibility.
     *
     * What the character already has, the level in the class that gates an option, and the
     * options another option requires. Three of the app's pickers applied none of it, so the
     * sheet offered a level 9 invocation to a level 2 Warlock and a feat that wanted two other
     * feats first.
     */
    @Test
    fun `every picker greys out what the character cannot take`() {
        val offenders = sources.flatMap { source ->
            callsTo(source.text, "ChoiceSection")
                .filterNot { it.contains("disabledOptionIds") }
                .map { "${source.name} renders a choice without disabledOptionIds" }
        }

        fail(
            "A picker that does not pass disabledOptionIds offers options the rules forbid " +
                "and duplicates of what the character already has.",
            offenders,
        )
    }

    // ================================================================ What a number claims

    /**
     * "The rules value" is the value without the player's pin on it.
     *
     * Four screens worked this out four ways and the tablet did not work it out at all, so
     * opening a pinned skill showed the pin labelled as the rules figure and offered to reset
     * it to itself. There is one function for it per kind of number.
     */
    @Test
    fun `a dialog never offers to reset a number to the value already pinned on it`() {
        val derived = listOf("unpinned", "poolMax", "rulesValue(")

        val offenders = sources.flatMap { source ->
            Regex("""rulesValue\s*=\s*([\s\S]{0,160}?),\n""").findAll(source.text)
                .map { it.groupValues[1] }
                .filterNot { expression -> derived.any { expression.contains(it) } }
                .map { "${source.name}: rulesValue = ${it.replace(Regex("\\s+"), " ").take(70)}" }
                .toList()
        }

        fail(
            "The rules value must be computed with the player's own bonus and override " +
                "cleared — CharacterCalculations.unpinnedStat and its neighbours do that.",
            offenders,
        )
    }

    // ================================================================ One way to do a thing

    /**
     * The phone and the tablet share the dialogs that matter.
     *
     * They each had their own copy of the choice editor, and the copies drifted: one applied
     * eligibility and the other did not, and neither could scroll. A second copy of a dialog
     * is a second place for the same bug to live.
     */
    @Test
    fun `the two layouts do not keep their own copies of the same editor`() {
        val phone = sources.filter { it.name !in setOf("TabletSheetScreen.kt") }
        val tablet = sources.filter { it.name == "TabletSheetScreen.kt" }

        val offenders = buildList {
            tablet.forEach { source ->
                if (callsTo(source.text, "ChoiceSection").isNotEmpty()) {
                    add("${source.name} builds its own choice editor rather than sharing one")
                }
            }
            // And the shared one exists, so nobody has to.
            if (phone.none { it.name == "ChoiceEditDialog.kt" }) {
                add("the shared ChoiceEditDialog is gone; the copies will come back")
            }
        }

        fail("One editor, used by both layouts.", offenders)
    }
}
