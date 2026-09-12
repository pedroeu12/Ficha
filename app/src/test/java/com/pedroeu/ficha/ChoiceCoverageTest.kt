package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.MasteryData
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nothing in the rulebook may tell the player to pick something without the app asking.
 *
 * This is the check that should have existed six bug reports ago. Each of those was the same
 * fault — a feature worded "of your choice" that nothing asked about — and each was fixed for
 * the one feature it was reported on, because there was no list of which features worded a
 * choice and no way to notice a new one arriving unasked.
 *
 * This reads every entry in the dataset, finds the ones whose own text says the player picks
 * something the sheet has a field for, and fails unless each one either raises a real choice
 * or is named below with the reason it doesn't. Content added later is checked the moment it
 * is added: a new subclass whose feature says "one skill of your choice" fails this test until
 * someone writes the question.
 */
class ChoiceCoverageTest {

    private data class Entry(
        val kind: String,
        val id: String,
        val name: String,
        val text: String,
        val asks: Boolean,
    )

    /**
     * Wording that means a decision the *sheet* records, as opposed to one made at the table.
     *
     * The difference is the object. "Choose a creature within 30 feet" is a target, decided
     * and forgotten within a turn, and no field on a character sheet holds it. "One skill of
     * your choice" is a proficiency the sheet carries for the character's whole life, and a
     * sheet that never asked is a sheet with a blank where a trained skill should be.
     */
    private val SHEET_NOUNS = listOf(
        "skill", "tool", "tools", "language", "languages", "feat", "spell", "spells",
        "cantrip", "cantrips", "damage type", "ability score", "weapon", "maneuver",
        "invocation", "instrument", "expertise", "proficiency", "domain", "element",
    )

    /** "…of your choice", "choose a…", "chosen when you…" — with a sheet noun as the object. */
    private fun asksForSomethingRecorded(text: String): Boolean {
        val lower = text.lowercase()
        return SHEET_NOUNS.any { noun ->
            Regex("""\b$noun\b[^.]{0,40}\bof your choice\b""").containsMatchIn(lower) ||
                Regex("""\bchoose\b[^.]{0,40}\b$noun\b""").containsMatchIn(lower) ||
                Regex("""\byour choice of\b[^.]{0,25}\b$noun\b""").containsMatchIn(lower)
        }
    }

    private fun everything(): List<Entry> = buildList {
        ProgressionData.ALL.forEach { p ->
            p.features.forEach { f ->
                add(Entry("class-feature", "${p.classId}:${f.name}", f.name, f.description,
                    f.choices.isNotEmpty()))
                f.choices.filterNot { it.kind == ChoiceKind.SPELL }.forEach { c ->
                    c.options.forEach { o ->
                        add(Entry("option", "${c.id}:${o.id}", o.name, o.description,
                            o.grants.isNotEmpty()))
                    }
                }
            }
        }
        SubclassData.ALL.forEach { s ->
            s.features.forEach { f ->
                add(Entry("subclass-feature", "${s.id}:${f.name}", f.name, f.description,
                    f.choices.isNotEmpty()))
                f.choices.filterNot { it.kind == ChoiceKind.SPELL }.forEach { c ->
                    c.options.forEach { o ->
                        add(Entry("option", "${c.id}:${o.id}", o.name, o.description,
                            o.grants.isNotEmpty()))
                    }
                }
            }
        }
        SpeciesData.ALL.forEach { sp ->
            sp.traits.forEach { t ->
                add(Entry("species-trait", "${sp.id}:${t.name}", t.name, t.description,
                    t.choices.isNotEmpty() ||
                        sp.bonusSkillChoiceCount > 0 ||
                        sp.grantsOriginFeat))
            }
            sp.lineageOptions.forEach { l ->
                add(Entry("lineage", "${sp.id}:${l.id}", l.name, l.description,
                    l.choices.isNotEmpty()))
            }
        }
        BackgroundData.ALL.forEach { b ->
            add(Entry("background", b.id, b.name, b.summary, b.featChoice != null))
        }
        FeatData.ALL.forEach { f ->
            add(Entry("feat", f.id, f.name, f.description,
                FeatChoiceData.choicesFor(f.id, f.name).isNotEmpty()))
        }
        ClassData.ALL.forEach { c ->
            c.level1Features.forEach { t ->
                add(Entry("class-trait", "${c.id}:${t.name}", t.name, t.description,
                    c.choices.isNotEmpty()))
            }
        }
        MasteryData.PROPERTIES.forEach { m ->
            add(Entry("mastery", m.name, m.name, m.description, false))
        }
    }.distinctBy { it.kind + it.id }

    /** Anything a per-use table already covers is answered where the rules say to answer it. */
    private fun coveredByPerUse(entry: Entry): Boolean {
        val perUseSources = PerUseChoiceData.ALL.map { it.source.lowercase() }.toSet()
        val perUseIds = PerUseChoiceData.ALL.map { it.id.substringAfter(':') }.toSet()
        return entry.name.lowercase() in perUseSources ||
            entry.id.substringAfterLast(':') in perUseIds
    }

    @Test
    fun `everything that tells the player to pick something asks for it`() {
        val everyChoiceId: Set<String> = buildSet {
            fun walk(choices: List<Choice>) {
                choices.forEach { c ->
                    add(c.id)
                    c.options.forEach { walk(it.grants) }
                }
            }
            ProgressionData.ALL.forEach { p -> p.features.forEach { walk(it.choices) } }
            SubclassData.ALL.forEach { s -> s.features.forEach { walk(it.choices) } }
            SpeciesData.ALL.forEach { sp -> sp.traits.forEach { walk(it.choices) } }
            FeatData.ALL.forEach { f -> walk(FeatChoiceData.choicesFor(f.id, f.name)) }
            ClassData.ALL.forEach { c -> c.choices.forEach { add(it.id) } }
        }

        val unasked = everything()
            .filter { asksForSomethingRecorded(it.text) }
            .filterNot { entry ->
                entry.asks ||
                    coveredByPerUse(entry) ||
                    entry.id.substringAfterLast(':') in DECIDED_AT_THE_TABLE ||
                    entry.name in DECIDED_AT_THE_TABLE ||
                    entry.id in ASKED_ELSEWHERE ||
                    entry.name in ASKED_ELSEWHERE ||
                    everyChoiceId.any { it.endsWith(":${entry.id.substringAfterLast(':')}") }
            }

        val report = unasked.joinToString("\n") { "  ${it.kind} ${it.id}" }
        assertTrue(
            "${unasked.size} entries tell the player to pick something and nothing asks:\n$report",
            unasked.isEmpty(),
        )
    }

    /**
     * Decisions made in the moment, which no field on a sheet holds.
     *
     * A Battle Master choosing which ally to Rally, a Warlock naming the weapon their pact
     * conjures this minute, a Sorcerer picking whom Careful Spell spares — these are answered
     * at the table and answered differently next round. Writing them down once would be
     * wrong, not merely unnecessary.
     */
    private val DECIDED_AT_THE_TABLE = setOf(
        // "As a Bonus Action, you can conjure a pact weapon" — a different weapon every time
        // the Warlock conjures one, which is not something a sheet holds.
        "pact_blade",
        // "As a Magic action, you can touch the item and transform it into a type of
        // Artisan's Tools of your choice" — the point of the item is that it changes.
        "manifold_tool",
        // The damage type is chosen as the burst is spent, and again on the next one.
        "Elemental Burst",
        // The effect is chosen at the moment of casting, from a table rolled on otherwise.
        "Tamed Surge",
        // Which creatures the spell spares, decided as it is cast.
        "Sculpt Spells",
        // "Choose one spell ... that you have prepared" and cast it now.
        "Mind Magic",
    )

    /** Asked somewhere the entry's own text can't show, with the reason. */
    private val ASKED_ELSEWHERE = setOf(
        // Not a pick at all: it widens the list a Bard prepares from, and the spell step is
        // what offers that list.
        "bard:Magical Secrets",
        // The level-up flow's own Ability Score Improvement step, which offers +2 to one
        // score or +1 to two and enforces the cap. A Choice would ask it a second time.
        "ability_score_improvement",
        // A subclass that casts declares its caster type, spell list and cantrips known, and
        // the spell step asks for every one of them. Writing a Choice as well would ask for
        // the same cantrips twice and record them in two places.
        "mystic_arts:Spellcasting",
        "eldritch_knight:Spellcasting",
        "arcane_trickster:Spellcasting",
        // Which slots come back is answered on the rest sheet, where the recovery happens.
        "land:Natural Recovery",
    )
}
