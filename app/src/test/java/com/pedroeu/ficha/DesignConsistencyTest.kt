package com.pedroeu.ficha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The interface is allowed to be opinionated; it is not allowed to have several opinions.
 *
 * Every screen in this app was written in a different week, and it showed: four different
 * sheets for "here is what this thing does", two inline expanders that did the same job
 * differently, card corners at 10, 12, 14 and 16, and padding picked afresh each time. None
 * of that was a decision — it was the absence of one, repeated. These checks hold the line by
 * reading the sources: a new screen that invents its own spacing or its own way of showing a
 * description fails here rather than at the table.
 */
class DesignConsistencyTest {

    private val ui = File("src/main/java/com/pedroeu/ficha/ui")

    private fun sources(vararg under: String): List<File> =
        under.flatMap { dir ->
            File(ui, dir).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }

    /** Components are where the shared vocabulary lives, so they are held to it loosely. */
    private fun screens(): List<File> =
        sources("sheet", "creation", "levelup", "tablet", "home")

    // ================================================================ One detail pattern

    /**
     * Surfaces that are a place to *do* something rather than a place to read something.
     *
     * A picker, the rest flow, the item builder: these are bottom sheets too, and they are
     * allowed to be, because they are a task with its own controls rather than a description.
     * The rule this test enforces is about descriptions — one anatomy, one gesture — and
     * anything added to this list has to be a task surface, not a shortcut around that.
     */
    private val taskSurfaces = setOf(
        "RestSheet.kt", "PickerSheets.kt", "ArtificerItemsCard.kt", "AddItemSheet.kt",
    )

    @Test
    fun `only the shared sheet presents a description`() {
        // A ModalBottomSheet anywhere but the component library and the task surfaces means a
        // screen has built its own way of showing detail, which is how this started.
        val offenders = screens()
            .filter { it.readText().contains("ModalBottomSheet(") }
            .map { it.name }
            .filterNot { it in taskSurfaces }

        assertTrue(
            "these build their own detail sheet instead of using DetailSheet: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `the detail sheet is the only thing that lays out a description`() {
        val detailSheet = File(ui, "components/DetailSheet.kt")
        assertTrue("the shared sheet is missing", detailSheet.isFile)

        val text = detailSheet.readText()
        // The anatomy every description follows, in order.
        listOf("val title", "val kind", "val summary", "val facts", "val body", "val footnote")
            .forEach {
                assertTrue("the shared detail shape lost its $it", text.contains(it))
            }
    }

    @Test
    fun `nothing unfolds a description in place any more`() {
        // The old pattern: a body of rules text revealed inline, so the list you were reading
        // changed height under your thumb and the gesture meant something different from the
        // same gesture one row down.
        val offenders = screens()
            .filter { file ->
                val text = file.readText()
                text.contains("readInASheet") || text.contains("subtitleExpanded")
            }
            .map { it.name }

        assertTrue("these still expand rules text inline: $offenders", offenders.isEmpty())
    }

    // ================================================================ One set of measurements

    @Test
    fun `screens do not invent their own corner radii`() {
        // Rounding is a design decision made once, in Corner. A screen naming its own is a
        // screen that will drift from the others.
        val radius = Regex("""RoundedCornerShape\((\d+)\.dp\)""")
        val offenders = screens().flatMap { file ->
            radius.findAll(file.readText()).map { "${file.name}: ${it.value}" }
        }.filterNot {
            // Vellum is the tablet's paper vocabulary and defines its own surfaces; it is
            // the one file allowed to name a radius, because it is where they are named.
            it.startsWith("Vellum.kt")
        }

        assertTrue(
            "use Corner.small, Corner.row or Corner.card instead: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `the spacing scale is a 4dp grid`() {
        val tokens = File(ui, "design/Tokens.kt").readText()
        val values = Regex("""val \w+: Dp = (\d+)\.dp""")
            .findAll(tokens)
            .map { it.groupValues[1].toInt() }
            .toList()

        assertTrue("no spacing tokens were found", values.isNotEmpty())
        val offGrid = values.filter { it % 4 != 0 }
        assertTrue("these are off the 4dp grid: $offGrid", offGrid.isEmpty())
    }

    // ================================================================ One set of durations

    @Test
    fun `animations come from the shared durations`() {
        // A duration written into a screen is a duration that will disagree with the one next
        // to it. Three are defined; everything uses one of them.
        val inlineTween = Regex("""tween\(\s*\d+""")
        val offenders = screens()
            .filter { inlineTween.containsMatchIn(it.readText()) }
            .map { it.name }

        assertTrue("these time their own animations: $offenders", offenders.isEmpty())
    }

    @Test
    fun `there are exactly three durations, fast to slow`() {
        val tokens = File(ui, "design/Tokens.kt").readText()
        val durations = Regex("""const val \w+_MS = (\d+)""")
            .findAll(tokens)
            .map { it.groupValues[1].toInt() }
            .toList()

        assertEquals("three durations, no more", 3, durations.size)
        assertEquals("and they are ordered", durations.sorted(), durations)
    }

    // ================================================================ One disclosure

    @Test
    fun `no screen draws its own disclosure triangle`() {
        // Two glyphs swapped in a text label, snapping with no motion, written out three
        // times. There is one header with one rotation now.
        val offenders = screens()
            .filter { it.readText().contains("▸") || it.readText().contains("▾") }
            .map { it.name }

        assertTrue("use SectionDisclosure instead: $offenders", offenders.isEmpty())
    }
}
