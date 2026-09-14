package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A pick the rules let you take back is one the app offers again.
 *
 * "Whenever you finish a Long Rest, you can replace that cantrip" is a rule, and the app has
 * exactly one way to honour it: [Choice.changeableOnRest], which is what puts the question on
 * the rest sheet. A choice whose feature promises the swap and carries no flag is a decision
 * the player made once and can never unmake — the feature is quietly smaller than the book
 * says, and nothing but reading both would show it. [Choice.changeableOnLevelUp] is the same
 * shape for "whenever you gain a level".
 *
 * Checked per *feature* rather than per choice, because a feature with two grants promises
 * the swap for one of them and the text does not say which in a form a machine can read. A
 * feature that promises a swap must have somewhere to make it; which of its questions that is
 * belongs to whoever wrote the content.
 */
class SwappableCoverageTest {

    /** One feature's text, with every choice it asks. */
    private data class Feature(val name: String, val text: String, val choices: List<Choice>)

    private fun everything(): List<Feature> = buildList {
        fun feature(name: String, text: String, choices: List<Choice>) {
            if (choices.isEmpty()) return
            // An option's own grants are asked under the same feature and inherit its promise.
            val nested = choices.flatMap { c -> c.options.flatMap { it.grants } }
            add(Feature(name, text, choices + nested))
        }
        ProgressionData.ALL.forEach { progression ->
            progression.features.forEach { feature(it.name, it.description, it.choices) }
        }
        SubclassData.ALL.forEach { subclass ->
            subclass.features.forEach {
                feature("${subclass.name}: ${it.name}", it.description, it.choices)
            }
        }
        SpeciesData.ALL.forEach { species ->
            species.traits.forEach {
                feature("${species.name}: ${it.name}", it.description, it.choices)
            }
            species.lineageOptions.forEach {
                feature("${species.name}: ${it.name}", it.description, it.choices)
            }
        }
        FeatData.ALL.forEach { feat ->
            feature(feat.name, feat.description, FeatChoiceData.choicesFor(feat.id, feat.name))
        }
    }

    /**
     * A feature's text and the wording of each question it asks, one sentence at a time.
     *
     * One sentence at a time is the whole of the method. A rules paragraph says several
     * things, and reading it as one run of characters means a swap in one sentence can be
     * matched against a rest in the next — which is how "you regain the ability to cast it
     * when you finish a Long Rest" and "whenever you gain a level you can replace one of the
     * spells" read, together, as a swap on a rest. Ten features claimed one that way. A
     * promise and the moment it names are always in the same sentence.
     */
    private val Feature.sentences: List<String>
        get() = (text + " " + choices.joinToString(" ") { it.prompt })
            .split(Regex("(?<=[.;])\\s+"))

    private val verbs = Regex(
        "(?i)\\b(replace|replacing|swap|swapping|exchange|reshape|reshaping|" +
            "change|changing|choose|choosing)\\b"
    )
    private val rest = Regex("(?i)\\b(Long Rest|Short Rest)\\b")
    private val levelling = Regex("(?i)(whenever|each time|when) you gain a[^,.]{0,40}level")

    /**
     * A rest can be the moment a pick may change, or it can be how often a thing may be used,
     * and the second is far more common: "Choose a level 6 spell you can cast once per Long
     * Rest" names a pick and a rest in one sentence and promises no swap at all. Every Mystic
     * Arcanum, Signature Spells and three feats read that way. A sentence that spends or
     * recovers a use is about the use.
     */
    private val aboutAUse = Regex(
        "(?i)(once\\s+(per|each per|a|every)\\s+(Short or Long|Short|Long)\\s+Rest" +
            "|until you finish[^.]{0,30}Rest" +
            "|must finish[^.]{0,30}Rest before" +
            "|regain[^.]{0,60}(finish|Rest)" +
            "|spend a[^.]{0,20}Rest)"
    )

    private fun Feature.promises(moment: Regex): Boolean = promising(moment) != null

    /** The sentence that makes the promise, so a failure says which words it read. */
    private fun Feature.promising(moment: Regex): String? = sentences.firstOrNull {
        moment.containsMatchIn(it) && verbs.containsMatchIn(it) && !aboutAUse.containsMatchIn(it)
    }

    @Test
    fun `a feature that says you may swap on a rest has somewhere to swap`() {
        val offenders = everything()
            .filter { it.promises(rest) }
            .filterNot { feature -> feature.choices.any { it.changeableOnRest } }
            .map { "${it.name} — \"${it.promising(rest)?.take(160)}\"" }

        assertTrue(
            "${offenders.size} features promise a swap on a rest and none of their " +
                "questions is ever asked again:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    @Test
    fun `a feature that says you may swap on levelling has somewhere to swap`() {
        val offenders = everything()
            .filter { it.promises(levelling) }
            .filterNot { feature -> feature.choices.any { it.changeableOnLevelUp } }
            .map { "${it.name} — \"${it.promising(levelling)?.take(160)}\"" }

        assertTrue(
            "${offenders.size} features promise a swap on levelling and none of their " +
                "questions is ever asked again:\n" + offenders.joinToString("\n"),
            offenders.isEmpty(),
        )
    }

    /**
     * And the other way round: a question re-offered on a rest whose feature says nothing
     * about re-choosing is either a flag set by mistake or, far more often, rules text the
     * app summarised and dropped a clause from. Both are worth failing on, because the text
     * is what the player reads to know the feature exists.
     */
    @Test
    fun `nothing is re-offered without text that says it may be`() {
        // Some features are re-chosen by doing the thing again rather than by resting — a
        // familiar's imbued energy lasts "until you cast Find Familiar again" — and the rest
        // sheet is simply where the app asks. That is still the text saying it can change.
        val byDoingItAgain = Regex("(?i)until you (cast|use)[^.]{0,60}again")

        val unexplained = everything()
            .filter { feature -> feature.choices.any { it.changeableOnRest } }
            .filterNot { feature ->
                feature.promises(rest) || feature.sentences.any { byDoingItAgain.containsMatchIn(it) }
            }
            .map { it.name }

        assertTrue(
            "these are re-offered on a rest and their text never says so: " +
                unexplained.joinToString(", "),
            unexplained.isEmpty(),
        )
    }
}
