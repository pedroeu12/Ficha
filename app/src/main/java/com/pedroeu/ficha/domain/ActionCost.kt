package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.FeatureText
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.ResourceDef
import com.pedroeu.ficha.data.model.ResourceOption

/**
 * What a thing costs on your turn: an Action, a Bonus Action, a Reaction, or none of those.
 *
 * The sheet lists everything with limited uses in one column, which answers "what do I still
 * have?" but not the question actually asked in play — "it's my turn, what can I do?". You
 * get one Action, one Bonus Action and one Reaction per round, so those three are the buckets
 * a player sorts by at the table, and the list is far more useful split the same way.
 *
 * [OTHER] is not a failure to classify so much as its own real answer: Action Surge costs
 * nothing, Focus Points buy three different things at three different costs, and a pool of
 * healing dice is spent whenever the feature that spends it says so.
 */
enum class ActionCost(val label: String) {
    ACTION("Action"),
    BONUS_ACTION("Bonus Action"),
    REACTION("Reaction"),
    OTHER("No action or varies");

    companion object {

        /** In turn order, which is also the order the sections are shown in. */
        val ORDER: List<ActionCost> = listOf(ACTION, BONUS_ACTION, REACTION, OTHER)

        // The books lead with the cost — "As a Bonus Action, you…" — so whichever of these
        // appears first in the text is the cost, and a Bonus Action that later mentions a
        // Reaction is still a Bonus Action.
        private val PATTERNS: List<Pair<ActionCost, Regex>> = listOf(
            REACTION to Regex("""(?i)\b(?:take|takes|taking|as)\s+a\s+Reaction\b"""),
            REACTION to Regex("""(?i)\buse\s+your\s+Reaction\b"""),
            BONUS_ACTION to Regex("""(?i)\bas\s+a\s+(?:single\s+)?Bonus\s+Action\b"""),
            BONUS_ACTION to Regex("""(?i)\bBonus\s+Action\s*(?:,|:|\bto\b)"""),
            ACTION to Regex("""(?i)\bas\s+a\s+Magic\s+action\b"""),
            ACTION to Regex("""(?i)\bMagic\s+action\s*(?:,|:)"""),
        )

        /**
         * The cost named by a free-text label such as an option's `actionType`.
         *
         * Substring matching on purpose: the catalogues carry things like "Bonus Action to
         * drink" and "None (Reaction)", which name a cost perfectly clearly.
         */
        fun ofLabel(label: String): ActionCost = when {
            label.isBlank() -> OTHER
            label.contains("bonus action", ignoreCase = true) -> BONUS_ACTION
            label.contains("reaction", ignoreCase = true) -> REACTION
            label.contains("magic action", ignoreCase = true) -> ACTION
            // "Action", "1 Action", "Action or Ritual" — but not "Part of the Attack action",
            // which describes when the ability happens rather than what it costs.
            ACTION_LABEL.containsMatchIn(label) -> ACTION
            else -> OTHER
        }

        private val ACTION_LABEL = Regex("""(?i)^\s*(?:1\s+)?Action\b""")

        /**
         * The cost the rules text names, or [OTHER] when it names none.
         *
         * Reads the *first* cost mentioned rather than the strongest, because that is where
         * the books put it. "Part of the Attack action" and "once per turn" name no cost of
         * their own and correctly come back as [OTHER].
         */
        fun ofText(text: String): ActionCost {
            if (text.isBlank()) return OTHER
            return PATTERNS
                .mapNotNull { (cost, pattern) -> pattern.find(text)?.let { cost to it.range.first } }
                .minByOrNull { it.second }
                ?.first
                ?: OTHER
        }

        /** What one named ability a pool pays for costs. */
        fun of(option: ResourceOption): ActionCost {
            val declared = ofLabel(option.actionType)
            return if (declared != OTHER) declared else ofText(option.description)
        }

        /**
         * What a limited-use pool costs to use.
         *
         * The pool's own wording wins where it has any — Wild Shape says "as a Bonus Action"
         * and that settles it. Where it doesn't, the abilities it pays for do, but only when
         * they agree: Channel Divinity buys two things and both are a Magic action, so the
         * pool is an Action; Focus Points buy a Bonus Action, a Reaction and a rider on an
         * attack, so the pool is [OTHER] and the costs belong on each option instead.
         */
        fun of(def: ResourceDef): ActionCost {
            ofLabel(def.actionCost).let { if (it != OTHER) return it }

            // A free casting costs what the spell costs, whatever the pool's blurb says.
            def.spellId.takeIf { it.isNotBlank() }
                ?.let { SpellData.byId(it) }
                ?.let { ofLabel(it.castingTime) }
                ?.let { if (it != OTHER) return it }

            ofText(def.name + ". " + def.description + " " + def.notes)
                .let { if (it != OTHER) return it }

            // A tracker's blurb is often a one-line paraphrase; the feature it came from
            // carries the book's own wording, and that is where the cost is written.
            ofText(FeatureText.forName(def.name)).let { if (it != OTHER) return it }

            val fromOptions = def.options.map { of(it) }.filter { it != OTHER }.distinct()
            return fromOptions.singleOrNull() ?: OTHER
        }
    }
}
