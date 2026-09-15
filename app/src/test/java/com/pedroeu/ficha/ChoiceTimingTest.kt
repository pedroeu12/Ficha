package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * *When* the app asks, held against when the rules say to choose.
 *
 * A choice has a moment attached to it, and the app has three: answered once when the feature
 * is gained, revisited on a rest, or made afresh every time the feature is used. Putting a
 * choice in the wrong one is not a cosmetic error — it silently deletes most of a feature.
 *
 * The Path of the Spiritual Guardian is the case that prompted this. Its level 3 effect reads
 * "while your Rage is active, when you hit a creature, it suffers one of the following effects
 * of your choice", so the question belongs to each hit and costs nothing. It was hung off the
 * Rage tracker, which asks when a use is spent — so the player was asked once, as they raged,
 * and got one effect for the whole Rage instead of a fresh choice on every hit.
 *
 * The same sweep found eleven more: Rage of the Wilds and Elemental Strikes frozen at
 * creation, the Aura of Elemental Shielding pinned to a rest when it changes every turn, and
 * nine features whose choice was never asked at all.
 */
class ChoiceTimingTest {

    /** Wording that means the choice belongs to a moment of use. */
    private val atUse = Regex(
        "(?i)(when you (hit|use|cast|activate|create|assume|expend|enter|roll|deal)" +
            "|whenever you (create|use|cast|hit|activate|expend)|each time you" +
            "|at the start of (your|each of your) turn|as a (Magic|Bonus) [Aa]ction" +
            "|immediately after you|you can expend|whenever you activate)"
    )

    /** Wording that means it is settled when the feature is gained. */
    private val atGain = Regex(
        "(?i)(when you (select|gain|choose) (this|the)|chosen when you select" +
            "|whenever you finish a (Short or )?Long Rest, you c)"
    )

    private val picks = Regex(
        "(?i)(one of the following|choose one of the following|the following options of your choice)"
    )

    /**
     * Features that word a pick at a moment of use and still ask nothing, with the reason.
     *
     * Three kinds qualify: the pick belongs to somebody else (an ally's Reaction, a condition
     * ended on the target), it is a list of what an action can do rather than a mode to
     * record, or it is a preference with no mechanical state behind it.
     */
    private val noQuestionNeeded = mapOf(
        "Physician's Touch" to "the condition ended belongs to the creature you healed",
        "Fast Hands" to "a list of what the Bonus Action can do, not a mode to keep",
        "Rallying Surge" to "each ally chooses their own Reaction, not the Fighter",
        "Spirits from Beyond" to "a rolled result; the condition ended is the target's",
        "Necromancy Spellbook" to
            "the familiar's form is chosen when Find Familiar is cast, by the summon picker",
        "Alter Memories" to
            "the creature is one the spell is already targeting, chosen as it is cast",
    )

    private data class Feature(val owner: String, val name: String, val text: String,
                               val choices: List<Choice>)

    private fun everyFeature(): List<Feature> = buildList {
        SubclassData.ALL.forEach { sub ->
            sub.features.forEach { add(Feature(sub.id, it.name, it.description, it.choices)) }
        }
        ProgressionData.ALL.forEach { prog ->
            prog.features.forEach { add(Feature(prog.classId, it.name, it.description, it.choices)) }
        }
        SpeciesData.ALL.forEach { sp ->
            sp.traits.forEach { add(Feature(sp.id, it.name, it.description, it.choices)) }
        }
        FeatData.ALL.forEach { feat ->
            add(Feature(feat.id, feat.name, feat.description,
                FeatChoiceData.choicesFor(feat.id, feat.name)))
        }
    }

    @Test
    fun `a feature that words a pick asks it somewhere`() {
        val perUseSources = PerUseChoiceData.ALL.map { it.source }.toSet()
        val missing = everyFeature().filter { feature ->
            picks.containsMatchIn(feature.text) &&
                atUse.containsMatchIn(feature.text) &&
                feature.name !in perUseSources &&
                feature.choices.isEmpty() &&
                feature.name !in noQuestionNeeded
        }
        val report = missing.joinToString("\n") { "  ${it.owner} / ${it.name}" }
        assertTrue(
            "these features word a pick and nothing asks it — add a row to PerUseChoiceData " +
                "or a Choice to the feature, or list it in `noQuestionNeeded` with the " +
                "reason:\n$report",
            missing.isEmpty(),
        )
    }

    @Test
    fun `a choice made on every hit does not read off a pool`() {
        // The reported bug, stated as a rule. A pool's tracker asks when a use is spent, and a
        // hit never spends one — you hit many times per Rage, per Flurry, per attunement. So a
        // feature that words its pick "when you hit" and reads off a tracker is asked once, at
        // the wrong moment, and the player gets one answer where the rules give them many.
        // Read the sentence the pick is actually in, not the whole feature: a long feature
        // mentions hits all over its options, and only the sentence offering the choice says
        // when the choice is made.
        val onAHit = Regex("(?i)when(ever)? you hit")
        fun pickSentence(text: String): String? {
            val at = picks.find(text)?.range?.first ?: return null
            val start = text.lastIndexOf('.', at).let { if (it < 0) 0 else it + 1 }
            val end = text.indexOf('.', at).let { if (it < 0) text.length else it }
            return text.substring(start, end)
        }
        val wrong = PerUseChoiceData.ALL
            .filter { it.resourceId.isNotBlank() }
            .filter { choice ->
                val text = everyFeature().firstOrNull { it.name == choice.source }?.text
                val sentence = text?.let(::pickSentence)
                sentence != null && onAHit.containsMatchIn(sentence)
            }
            .map { "${it.id} -> ${it.resourceId}" }
        assertTrue(
            "these are chosen on a hit, which costs nothing, yet they hang off a tracker that " +
                "only asks when a use is spent: $wrong",
            wrong.isEmpty(),
        )
    }

    @Test
    fun `a per-use choice is not also asked once at creation`() {
        // Asking both ways is how the Spiritual Guardian's bug would come back sideways: the
        // creation answer is the one the sheet shows, and the per-use one goes unnoticed.
        val perUseSources = PerUseChoiceData.ALL.map { it.source }.toSet()
        val doubled = everyFeature()
            .filter { it.name in perUseSources && it.choices.isNotEmpty() }
            // The Abyssal Rupture's creation question is what the rupture *looks* like, which
            // the feature itself says has no effect on your game statistics.
            .filterNot { it.choices.all { c -> "no effect on your game statistics" in c.prompt } }
            .map { "${it.owner} / ${it.name}" }
        assertTrue("asked twice, in two different moments: $doubled", doubled.isEmpty())
    }

    /**
     * Text that ends without a full stop but is not cut off: a spell table, a d6 table, a list
     * of tool-and-item pairs. Each ends on its last row rather than on a sentence.
     */
    private val endsOnATable = setOf(
        "Knowledge Domain Spells", "Crafter", "Second Skin", "Boon of Siberys",
        "Mark of Detection", "Mark of Finding", "Mark of Handling", "Mark of Healing",
        "Mark of Hospitality", "Mark of Making", "Mark of Passage", "Mark of Scribing",
        "Mark of Sentinel", "Mark of Shadow", "Mark of Storm", "Mark of Warding",
    )

    @Test
    fun `no feature's rules text stops in the middle of itself`() {
        // Three of these shipped. The Rimekin's magic ended on "choose the abilit", the
        // Flamekin's on "choose when you select ", and the Dhampir's Vampiric Bite stopped at
        // the colon that was about to list what the bite actually does. Each read as finished
        // on the sheet and simply left the rule out.
        val broken = everyFeature().filter { feature ->
            val t = feature.text.trim()
            when {
                t.isEmpty() -> true
                feature.name in endsOnATable -> false
                t.endsWith(".") || t.endsWith("!") || t.endsWith("?") -> false
                t.endsWith(")") || t.endsWith("\"") -> false
                // A table ends on a proper noun; a cut sentence ends on an ordinary word.
                else -> t.split(" ").last().firstOrNull()?.isLowerCase() ?: true
            }
        }.map { "${it.owner} / ${it.name}: …${it.text.trim().takeLast(60)}" }
        assertTrue(
            "rules text that stops mid-sentence:\n${broken.joinToString("\n")}",
            broken.isEmpty(),
        )
    }

    @Test
    fun `nothing is excused as a table that no longer is one`() {
        val named = everyFeature().map { it.name }.toSet()
        assertTrue("no such feature: ${endsOnATable - named}", (endsOnATable - named).isEmpty())
    }

    @Test
    fun `nothing is excused that no longer words a pick`() {
        val named = everyFeature().map { it.name }.toSet()
        val stale = noQuestionNeeded.keys - named
        assertTrue("excused but no such feature: $stale", stale.isEmpty())
    }

    @Test
    fun `a choice the rules let you revisit on a rest says so`() {
        // The other half of the same mistake. "Whenever you finish a Long Rest, you can change
        // your choice" is printed on the feature, and a Choice that does not carry the flag
        // makes the answer permanent — Aspect of the Wilds, Hunter's Prey and Defensive
        // Tactics were all frozen this way.
        val changeable = Regex(
            "(?i)(whenever you finish a (Short or Long|Long|Short) Rest,? you can " +
                "(change|replace|swap)|you can change .{0,40}whenever you finish a)"
        )
        val frozen = everyFeature().filter { feature ->
            changeable.containsMatchIn(feature.text) &&
                feature.choices.isNotEmpty() &&
                feature.choices.none { it.changeableOnRest }
        }.map { "${it.owner} / ${it.name}" }
        assertTrue("the text says it can be swapped on a rest and the choice cannot: $frozen",
            frozen.isEmpty())
    }
}
