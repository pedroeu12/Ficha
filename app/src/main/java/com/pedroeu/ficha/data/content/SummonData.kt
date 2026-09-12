package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.rules.Effect
import com.pedroeu.ficha.rules.Formula
import com.pedroeu.ficha.rules.SummonPick

/**
 * What each summoning spell or feature actually puts on the table.
 *
 * Declared once, as data, for the same reason everything else is: the picker, the tab, the
 * hit-point tracking and the dismissal are general, so supporting a new summoning spell is
 * writing a row here and a stat block in [StatblockData] — not new code.
 *
 * The two shapes the request named are both here. A spell that names one creature is
 * [SummonPick.Fixed]; one that offers a set is [SummonPick.FromList]; one that says "a Beast
 * of Challenge Rating 1/4 or lower" is [SummonPick.Filtered], which the picker reads as a
 * filter over whatever stat blocks match rather than as a hand-written list.
 */
object SummonData {

    /** Summons hung off a spell, keyed by spell id. */
    val BY_SPELL: Map<String, Effect.Summons> = mapOf(
        "summon_beast" to Effect.Summons(
            summonId = "summon_beast",
            label = "Summon Beast",
            pick = SummonPick.FromList(
                listOf("bestial_spirit_air", "bestial_spirit_land", "bestial_spirit_water")
            ),
            duration = "1 hour",
            concentration = true,
        ),
        "summon_undead" to Effect.Summons(
            summonId = "summon_undead",
            label = "Summon Undead",
            pick = SummonPick.FromList(
                listOf("undead_spirit_ghostly", "undead_spirit_putrid", "undead_spirit_skeletal")
            ),
            duration = "1 hour",
            concentration = true,
        ),
        "summon_fey" to Effect.Summons(
            summonId = "summon_fey",
            label = "Summon Fey",
            pick = SummonPick.FromList(
                listOf("fey_spirit_fuming", "fey_spirit_mirthful", "fey_spirit_tricksy")
            ),
            duration = "1 hour",
            concentration = true,
        ),
        "summon_elemental" to Effect.Summons(
            summonId = "summon_elemental",
            label = "Summon Elemental",
            pick = SummonPick.FromList(
                listOf(
                    "elemental_spirit_air", "elemental_spirit_earth",
                    "elemental_spirit_fire", "elemental_spirit_water",
                )
            ),
            duration = "1 hour",
            concentration = true,
        ),
        "find_familiar" to Effect.Summons(
            summonId = "find_familiar",
            label = "Find Familiar",
            pick = SummonPick.FromList(
                listOf(
                    "familiar_bat", "familiar_cat", "familiar_frog", "familiar_hawk",
                    "familiar_lizard", "familiar_octopus", "familiar_owl", "familiar_rat",
                    "familiar_raven", "familiar_spider", "familiar_weasel",
                )
            ),
            duration = "Until dismissed",
        ),
        "find_steed" to Effect.Summons(
            summonId = "find_steed",
            label = "Find Steed",
            pick = SummonPick.Fixed("otherworldly_steed"),
            duration = "Until dismissed",
        ),
        // "The target becomes an Undead creature: a Skeleton if you chose bones or a Zombie
        // if you chose a corpse." One per casting, and a caster keeps several at once, which
        // is the case that made multiple simultaneous summons a requirement rather than a
        // nicety.
        "animate_dead" to Effect.Summons(
            summonId = "animate_dead",
            label = "Animate Dead",
            pick = SummonPick.FromList(listOf("skeleton", "zombie")),
            duration = "Until destroyed",
        ),
    )

    /**
     * Options that widen a summon someone else provides, keyed by the option's id.
     *
     * Pact of the Chain summons nothing of its own — the Warlock still casts Find Familiar —
     * but it adds eight forms to it. Kept apart from [BY_SPELL] so an Imp is offered to the
     * Warlock who took the pact and to nobody else.
     */
    val EXTENSIONS_BY_OPTION: Map<String, Effect.ExtendsSummon> = mapOf(
        "pact_chain" to Effect.ExtendsSummon(
            summonId = "find_familiar",
            statblockIds = StatblockData.PACT_OF_THE_CHAIN_FORMS,
        ),
    )

    /** Summons hung off a class or subclass feature, keyed by the feature's element id. */
    val BY_FEATURE: Map<String, Effect.Summons> = mapOf(
        "subclass:battle_smith:3:steel_defender" to Effect.Summons(
            summonId = "steel_defender",
            label = "Steel Defender",
            pick = SummonPick.Fixed("steel_defender"),
            duration = "Until it dies or you make a new one",
        ),
        "subclass:vestige_patron:3:vestige_companion" to Effect.Summons(
            summonId = "vestige_companion",
            label = "Vestige Companion",
            pick = SummonPick.FromList(
                listOf("vestige_celestial", "vestige_fiend", "vestige_undead")
            ),
            duration = "Until it drops to 0 Hit Points or you dismiss it",
        ),
    )

    fun forSpell(spellId: String): Effect.Summons? = BY_SPELL[spellId]

    fun forFeature(elementId: String): Effect.Summons? = BY_FEATURE[elementId]

    /** Every summon the app knows about, for a test to hold against the stat block catalogue. */
    fun all(): List<Effect.Summons> = BY_SPELL.values.toList() + BY_FEATURE.values

    /** Stat block ids a pick can produce, whatever shape the pick is. */
    fun statblockIdsOf(pick: SummonPick): List<String> = when (pick) {
        is SummonPick.Fixed -> listOf(pick.statblockId)
        is SummonPick.FromList -> pick.statblockIds
        is SummonPick.Filtered -> StatblockData.ALL
            .filter { it.creatureType.contains(pick.creatureType, ignoreCase = true) }
            .map { it.id }
    }

    /** How many creatures a casting produces at [spellLevel]. */
    fun countAt(summons: Effect.Summons, spellLevel: Int): Formula =
        summons.scaling
            .filter { it.atSpellLevel <= spellLevel }
            .maxByOrNull { it.atSpellLevel }
            ?.count
            ?: summons.count
}
