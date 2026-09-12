package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.ModifierData
import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.StatTarget
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Content that states a number, checked against content that applies one.
 *
 * Every bug in this category looked the same: a feature whose own text says "+1 bonus to Armor
 * Class" or "your Speed increases by 10 feet", printed faithfully on the sheet, and never
 * reaching the number beside it. The Defense Fighting Style went four rewrites that way — the
 * most-picked Fighting Style in the game, recorded, displayed, and worth nothing.
 *
 * So rather than fixing the ones that were found, this reads the text the app itself ships and
 * insists each such sentence is either applied by [ModifierData]/[PassiveBonusData] or listed
 * below with a reason it is not. Writing new content that states a number and forgetting to
 * declare it now fails the build, which is the only version of "automatic" that stays true.
 */
class NumericPassiveCoverageTest {

    /** One sentence in the shipped text that promises a standing change to a number. */
    private data class Claim(
        /** The rules engine's id for whatever carries it, or "option:<id>" for an option. */
        val key: String,
        val where: String,
        val target: StatTarget,
        val quote: String,
    )

    private val patterns: List<Pair<Regex, StatTarget>> = listOf(
        Regex("""your (base )?(Armor Class|AC) equals""", RegexOption.IGNORE_CASE) to
            StatTarget.ARMOR_CLASS,
        Regex("""\+\s?\d\s?(bonus )?to (your )?(Armor Class|AC)\b""", RegexOption.IGNORE_CASE) to
            StatTarget.ARMOR_CLASS,
        Regex("""bonus to (your )?Armor Class""", RegexOption.IGNORE_CASE) to
            StatTarget.ARMOR_CLASS,
        Regex("""your (walking )?Speed increases by \d+ feet""", RegexOption.IGNORE_CASE) to
            StatTarget.SPEED,
        Regex("""(bonus to Initiative|add your \w+ modifier to the roll)""", RegexOption.IGNORE_CASE) to
            StatTarget.INITIATIVE,
        Regex("""your Hit Point maximum increases by""", RegexOption.IGNORE_CASE) to
            StatTarget.MAX_HIT_POINTS,
    )

    /**
     * Sentences that state a number the sheet should not quietly apply, each with the reason.
     *
     * Every entry here is a deliberate decision, not a backlog. Two kinds qualify: a number
     * that only holds for a moment the sheet does not track (a Rage, a Bladesong, a Wild Shape
     * form), and a number that belongs to somebody or something else (an ally who drank the
     * elixir, a summoned creature, an object).
     */
    /**
     * Sentences that state a number the sheet should not quietly apply, each with its reason.
     *
     * Every entry is a decision, not a backlog. Two kinds qualify: a number that holds only
     * for a moment the sheet does not track — a Bladesong, a Wild Shape form, the turn after a
     * Dash — and a number that belongs to somebody else, like the ally who drank the elixir.
     *
     * Keyed by the stat as well as the feature, because a feature can do both. Gloom Stalker's
     * Dread Ambusher is the reason: its Initiative bonus is permanent and now applied, while
     * the leap in the sentence before it lasts one turn. Excusing the feature as a whole would
     * have buried the half that was a real bug.
     */
    private val excused: Map<Pair<String, StatTarget>, String> = mapOf(
        // -------------------------------------------------- Only while something is active
        ("subclass:moon:3:circle_forms" to StatTarget.ARMOR_CLASS) to
            "the Wild Shape form's Armor Class, not the Druid's",
        ("subclass:bladesinger:3:bladesong" to StatTarget.SPEED) to
            "only while the Bladesong is up",
        ("subclass:gloom_stalker:3:dread_ambusher" to StatTarget.SPEED) to
            "Ambusher's Leap lasts one turn; the Initiative half is applied",
        ("subclass:elements:17:elemental_epitome" to StatTarget.SPEED) to
            "only during a Step of the Wind",
        ("subclass:armorer:3:armor_model" to StatTarget.SPEED) to
            "only while wearing the Arcane Armor itself",
        ("option:infiltrator" to StatTarget.SPEED) to
            "only while wearing the Arcane Armor itself",
        ("subclass:hollow_warden:3:wrath_of_the_wild" to StatTarget.ARMOR_CLASS) to
            "only while transformed",
        ("subclass:wild_magic:18:tamed_surge" to StatTarget.ARMOR_CLASS) to
            "one rolled outcome among many, lasting a minute",
        ("feat:mythal_touched" to StatTarget.ARMOR_CLASS) to
            "one rolled outcome among many, lasting a minute",
        ("lineage:beasthide" to StatTarget.ARMOR_CLASS) to "only while shifted",
        ("lineage:swiftstride" to StatTarget.SPEED) to "only while shifted",
        ("feat:charger" to StatTarget.SPEED) to "only during a Dash",
        ("feat:defensive_duelist" to StatTarget.ARMOR_CLASS) to "a Reaction against one attack",

        // -------------------------------------------------- Somebody else's number
        ("subclass:dance:6:tandem_footwork" to StatTarget.INITIATIVE) to
            "the allies who take the Bardic Inspiration die",
        ("subclass:alchemist:3:experimental_elixir" to StatTarget.ARMOR_CLASS) to
            "whoever drinks the elixir",
    )

    // ------------------------------------------------------------------ Gathering

    private fun claimsIn(key: String, where: String, text: String): List<Claim> =
        patterns.mapNotNull { (regex, target) ->
            regex.find(text)?.let { Claim(key, where, target, quoteAround(text, it.range.first)) }
        }

    private fun quoteAround(text: String, at: Int): String =
        text.substring((at - 50).coerceAtLeast(0), (at + 70).coerceAtMost(text.length))

    /**
     * An option's own text, but only where the option *is* a rule the character gains.
     *
     * An Artificer replicating a Ring of Protection and a Wizard learning Haste both store an
     * option whose description is a catalogue entry — the ring's rules, the spell's rules —
     * belonging to an item in the inventory or a spell on the list, not to the character
     * standing there. Those numbers are applied where the item or the spell is, so reading
     * them here would demand a second, wrong home for them.
     */
    private fun choiceClaims(choices: List<Choice>, where: String): List<Claim> =
        choices.flatMap { choice ->
            choice.options
                .filter { MagicItemData.byId(it.id) == null && SpellData.byId(it.id) == null }
                .flatMap { option ->
                    claimsIn("option:${option.id}", "$where / ${option.name}", option.description)
                }
        }

    private fun slug(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun allClaims(): List<Claim> = buildList {
        ProgressionData.ALL.forEach { progression ->
            progression.features.forEach { feature ->
                val key = "class:${progression.classId}:${feature.level}:${slug(feature.name)}"
                addAll(claimsIn(key, "${progression.classId} ${feature.name}", feature.description))
                addAll(choiceClaims(feature.choices, "${progression.classId} ${feature.name}"))
            }
        }
        SubclassData.ALL.forEach { subclass ->
            subclass.features.forEach { feature ->
                val key = "subclass:${subclass.id}:${feature.level}:${slug(feature.name)}"
                addAll(claimsIn(key, "${subclass.name} ${feature.name}", feature.description))
                addAll(choiceClaims(feature.choices, "${subclass.name} ${feature.name}"))
            }
        }
        SpeciesData.ALL.forEach { species ->
            species.traits.forEach { trait ->
                addAll(claimsIn(
                    "species:${species.id}:${slug(trait.name)}",
                    "${species.name} ${trait.name}",
                    trait.description,
                ))
                addAll(choiceClaims(trait.choices, "${species.name} ${trait.name}"))
            }
            species.lineageOptions.forEach { lineage ->
                addAll(claimsIn("lineage:${lineage.id}", lineage.name, lineage.description))
            }
        }
        FeatData.ALL.forEach { feat ->
            addAll(claimsIn("feat:${feat.id}", feat.name, feat.description))
        }
    }

    // ------------------------------------------------------------------ Coverage

    private fun coveredByModifier(key: String, target: StatTarget): Boolean {
        val effects = if (key.startsWith("option:")) {
            ModifierData.forOption(key.removePrefix("option:"))
        } else {
            ModifierData.forElement(key)
        }
        return effects.any { targetOf(it) == target }
    }

    private fun targetOf(effect: Effect): StatTarget? = when (effect) {
        is Effect.ModifyStat -> effect.target
        is Effect.SetStatBase -> effect.target
        else -> null
    }

    private fun coveredByPassive(key: String, target: StatTarget): Boolean {
        val legacy = when (target) {
            StatTarget.ARMOR_CLASS -> PassiveBonusData.Target.ARMOR_CLASS
            StatTarget.SPEED -> PassiveBonusData.Target.SPEED
            StatTarget.INITIATIVE -> PassiveBonusData.Target.INITIATIVE
            StatTarget.MAX_HIT_POINTS -> PassiveBonusData.Target.MAX_HIT_POINTS
            else -> return false
        }
        val parts = key.split(":")
        val bonuses = when (parts.firstOrNull()) {
            "class" -> PassiveBonusData.forClass(parts[1])
            "subclass" -> PassiveBonusData.forSubclass(parts[1])
            "species" -> PassiveBonusData.forSpecies(parts[1])
            "lineage" -> PassiveBonusData.forLineage(parts[1])
            "feat" -> PassiveBonusData.forFeat(parts[1])
            else -> emptyList()
        }
        return bonuses.any { it.target == legacy }
    }

    @Test
    fun `every number the text promises is either applied or excused`() {
        val uncovered = allClaims().distinctBy { it.key to it.target }.filterNot { claim ->
            coveredByModifier(claim.key, claim.target) ||
                coveredByPassive(claim.key, claim.target) ||
                (claim.key to claim.target) in excused
        }
        val report = uncovered.joinToString("\n") {
            "  ${it.key}  [${it.target}]  ${it.where}\n      …${it.quote.trim()}…"
        }
        assertTrue(
            "These features state a number the sheet never applies. Declare it in " +
                "ModifierData (or PassiveBonusData if it is unconditional), or add it to " +
                "`excused` with the reason it should not be applied:\n$report",
            uncovered.isEmpty(),
        )
    }

    @Test
    fun `nothing is excused that no longer says anything`() {
        // An excuse outliving its sentence is how a list like this rots into noise — and an
        // excuse that no longer matches any text is also how a reworded feature slips through.
        val claimed = allClaims().map { it.key to it.target }.toSet()
        val stale = excused.keys - claimed
        assertTrue("excused but no longer claims a number: $stale", stale.isEmpty())
    }

    @Test
    fun `the scan still sees the ones that were broken`() {
        // A scan that has stopped matching anything passes for the wrong reason. These four
        // were the real gaps it found; if the patterns drift, this says so.
        val seen = allClaims().map { it.key to it.target }.toSet()
        listOf(
            "class:ranger:6:roving" to StatTarget.SPEED,
            "subclass:gloom_stalker:3:dread_ambusher" to StatTarget.INITIATIVE,
            "subclass:noble_genies:3:genie_s_splendor" to StatTarget.ARMOR_CLASS,
            "option:defense" to StatTarget.ARMOR_CLASS,
        ).forEach { assertTrue("the scan no longer reads ${'$'}it", it in seen) }
    }
}
