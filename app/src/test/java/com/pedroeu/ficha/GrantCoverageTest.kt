package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.content.SubclassData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A feature that says "you always have X prepared" actually puts X on the sheet.
 *
 * The other half of the choice audit. Where that one asks whether the app *prompts* for what
 * the rules leave to the player, this asks whether it *delivers* what the rules hand over
 * outright — the same fault seen from the other side, and it was just as widespread: eleven
 * subclass spell tables disagreed with the rules text printed a few lines above them in this
 * very repository, and nine more features promised a named spell that no table carried.
 *
 * The check reads each feature's own description, finds the spells it names, and holds them
 * against what [SpellGrantData] actually grants. Nothing here is typed in by hand, so a
 * subclass added later is checked against its own text the moment it arrives.
 *
 * It walks *options* as well, which it did not at first, and that omission cost a real bug: a
 * level 1 Warlock with Pact of the Chain did not have Find Familiar, and eleven other
 * invocations were handing out nothing either. An invocation is not a feature, a trait or a
 * feat — it is an option inside a choice — so a check written around those three walked past
 * every one of them. Options are where a Fighting Style, a Metamagic, a maneuver and an
 * Artificer's plan live too.
 */
class GrantCoverageTest {

    /**
     * Wording that means the spell is handed over rather than offered.
     *
     * "You can cast X without expending a spell slot" belongs here as much as "you always
     * have X prepared" does, and leaving it out is what let twelve invocations grant nothing
     * while this test reported everything covered. A spell the rules let you cast is a spell
     * that has to be on the sheet, whatever sentence hands it over.
     */
    private val PROMISE = Regex(
        "(?i)(always have|thereafter always have|have the .{0,60}? spells? prepared|" +
            "spells? prepared|you can cast|you can also cast|you learn the)"
    )

    /**
     * Catalogue names, longest first.
     *
     * Longest first and consuming each match, so "Shield of Faith" is not also read as a
     * promise of "Shield" — matching shortest-first reported thirty spells nobody was ever
     * promised and buried the real ones.
     */
    private val NAMES = SpellData.ALL
        .sortedByDescending { it.name.length }
        .map { it.name to it.id }

    private fun spellsNamedIn(text: String): Set<String> {
        var rest = text
        val found = mutableSetOf<String>()
        NAMES.forEach { (name, id) ->
            val re = Regex("""\b${Regex.escape(name)}\b""", RegexOption.IGNORE_CASE)
            if (re.containsMatchIn(rest)) {
                found += id
                rest = re.replace(rest) { m -> " ".repeat(m.value.length) }
            }
        }
        return found
    }

    private data class Gap(val source: String, val feature: String, val spellIds: List<String>)

    private fun gaps(): List<Gap> = buildList {
        fun check(sourceId: String, feature: String, text: String, covered: Set<String>) {
            if (!PROMISE.containsMatchIn(text)) return
            val missing = (spellsNamedIn(text) - covered)
                .filterNot { "$sourceId:$it" in NAMED_BUT_NOT_A_SPELL }
                .sorted()
            if (missing.isNotEmpty()) add(Gap(sourceId, feature, missing))
        }

        SubclassData.ALL.forEach { s ->
            if (s.id in GRANTED_THROUGH_A_CHOICE) return@forEach
            val covered = SpellGrantData.alwaysPreparedForSubclass(s.id, 20)
            s.features.forEach { f -> check(s.id, f.name, f.description, covered) }
        }
        ProgressionData.ALL.forEach { p ->
            val covered = SpellGrantData.alwaysPreparedForClass(p.classId, 20)
            p.features.forEach { f -> check(p.classId, f.name, f.description, covered) }
        }
        SpeciesData.ALL.forEach { sp ->
            val covered = SpellGrantData.grantedSpellIds(speciesId = sp.id, level = 20)
            sp.traits.forEach { t -> check(sp.id, t.name, t.description, covered) }
            sp.lineageOptions.forEach { l ->
                check(
                    "${sp.id}:${l.id}", l.name, l.description,
                    SpellGrantData.grantedSpellIds(
                        speciesId = sp.id, lineageId = l.id, level = 20,
                    ),
                )
            }
        }
        FeatData.ALL.forEach { f ->
            check(
                f.id, f.name, f.description,
                SpellGrantData.grantedSpellIds(featIds = listOf(f.id), level = 20),
            )
        }

        // Options: invocations, Fighting Styles, Metamagic, maneuvers, Artificer plans. Each
        // is checked against what *it alone* grants, not against everything the game grants
        // to anyone — the difference matters, because Mage Armor being granted by some other
        // source elsewhere made Armor of Shadows look covered when it granted nothing.
        // An option that *is* a spell carries the spell's own text, which describes what
        // happens when it is cast — not something the option hands over. The same is true of
        // the Artificer's plans, whose options are magic items.
        fun optionsOf(choices: List<com.pedroeu.ficha.data.model.Choice>) =
            choices
                .filterNot { it.kind == ChoiceKind.SPELL }
                .filterNot { it.id == ProgressionData.PLAN_CHOICE_ID }
                .flatMap { c -> c.options.map { c.id to it } }

        val everyOption = buildList {
            ProgressionData.ALL.forEach { p ->
                p.features.forEach { addAll(optionsOf(it.choices)) }
            }
            SubclassData.ALL.forEach { s ->
                s.features.forEach { addAll(optionsOf(it.choices)) }
            }
        }.distinctBy { "${it.first}:${it.second.id}" }

        everyOption.forEach { (choiceId, option) ->
            check(
                option.id,
                option.name,
                option.description,
                SpellGrantData.grantedSpellIds(
                    selections = mapOf(choiceId to listOf(option.id)),
                    level = 20,
                ),
            )
        }
    }

    @Test
    fun `a feature that promises a spell always prepared actually grants it`() {
        val found = gaps().filterNot { "${it.source}|${it.feature}" in EXPANDS_THE_SPELL_LIST }
        val report = found.joinToString("\n") { "  ${it.source} / ${it.feature}: ${it.spellIds}" }
        assertTrue(
            "${found.size} features promise a spell the sheet never hands over:\n$report",
            found.isEmpty(),
        )
    }

    /**
     * Subclasses whose always-prepared list follows an answer rather than the subclass.
     *
     * The Primordial Patron's list follows its element and the Death Domain Vestige's follows
     * the Cleric domain it borrows, so neither is reachable from the subclass id alone.
     * [SpellGrantData.alwaysPreparedForChoices] is what serves them, and
     * `PrimordialPatronTest` and `VestigeSpellsTest` are what check them.
     */
    private val GRANTED_THROUGH_A_CHOICE = setOf("primordial_patron", "vestige_patron")

    /**
     * A spell name that also occurs as an ordinary phrase in the same feature's text.
     *
     * "Light Domain Spells", "Dim Light", "the Divination school" — the words are in the
     * description but no spell is being promised. Listed one by one rather than matched
     * loosely, so a feature that really does grant Light still has to say so.
     */
    private val NAMED_BUT_NOT_A_SPELL = setOf(
        "light_domain:light",
        "college_of_the_moon:light",
        "knowledge_domain:divination",
        // "Disadvantage on saving throws made against the Scrying spell" — it works against
        // the reader, not for them.
        "watchers:scrying",
        // "Resistance to Necrotic and Poison damage", not the Resistance cantrip.
        "lich_ascension:resistance",
        // "teleport to an unoccupied space", not the Teleport spell.
        "fey_sentinel:teleport",
        // "wielding a Shield", the piece of equipment.
        "infernal_bulwark:shield",
        // "an aura of fear" and "aid you", both ordinary words here.
        "infernal_dragoon:fear",
        "infernal_dragoon:aid",
        // "you can teleport to an unoccupied space", the verb.
        "archfey:teleport",
        "cartographer:teleport",
        "cartographer:jump",
        // The feature is called Expert *Divination* and grants Darkvision to nobody.
        "diviner:divination",
        "diviner:darkvision",
        // "Resistance to Poison damage", not the cantrip.
        "alchemist:resistance",
        "touch_of_death:resistance",
        // "you can command the companion", the verb.
        "reanimator:command",
        // "the shadow can aid you", the verb.
        "living_shadow:aid",
        // The feature is called Wish Magic; it grants a level 1 spell, not Wish.
        "genie_magic:wish",
        // "while you're in an area of Dim Light or Darkness" — the condition, not the spells.
        "one_with_shadows:darkness",
        "one_with_shadows:light",
        // "If you already know that cantrip" — Light is granted, and the *alternative* is the
        // choice this feat raises.
        "illusionist:minor_illusion",
        // "Minor Telekinesis" is the name of the benefit; the spell it grants is Mage Hand.
        "telekinetic:telekinesis",
    )

    /**
     * Features whose spell table is added to the character's *class list* rather than
     * prepared.
     *
     * Every Dragonmark feat works this way: it always-prepares two or three spells, which are
     * granted, and then lists eight or nine "Spells of the Mark" that are only added to the
     * spell list of a class you already cast from. Treating those as grants would hand a
     * level 1 Dragonmarked character a level 5 spell.
     */
    private val EXPANDS_THE_SPELL_LIST: Set<String> =
        FeatData.ALL
            .filter { "Spells of the Mark" in it.description || "Siberys" in it.description }
            .map { "${it.id}|${it.name}" }
            .toSet() + setOf(
            // "You always have that spell prepared" — but *which* spell is a choice, and the
            // choice is what records it. The sentence names no spell to grant.
            "fey_touched|Fey-Touched",
            "vampire_touched|Vampire Touched",
            "boon_of_siberys|Boon of Siberys",
        )
}
