package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A feature that promises a number of uses has something counting them.
 *
 * [ContentCoverageTest] asks this of feats; this asks it of subclasses and species, where it
 * had never been asked. Twelve features said "once you use this, you can't do so again until
 * you finish a Long Rest" and the sheet gave the player nowhere to record that they had —
 * among them the Battle Master's Know Your Enemy, both of the Clockwork Sorcerer's capstones,
 * the College of Glamour's Beguiling Magic, two of the Vestige Patron's, and the Dragonborn's
 * Draconic Flight.
 */
class UseTrackerCoverageTest {

    private val promisesUses = Regex(
        "number of times equal to|once per Long Rest|once per Short or Long Rest|" +
            "you have a number of|can't use (?:it|this feature|this trait) again until you finish|" +
            "can't do so again until you finish|regaining all expended|regain all expended|" +
            "Once you use this (?:feature|trait|benefit)",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Features that read as limited and need no tracker of their own, with the reason.
     *
     * Two shapes: a limit that resets every turn rather than on a rest, and a feature that
     * raises a pool another feature already owns.
     */
    private val noTrackerNeeded = mapOf(
        "Spellcasting" to "its text is about recovering spell slots, which the slots track",
        "Improved Warding Flare" to "it raises the Warding Flare pool rather than adding one",
        "Vengeful Spirits" to "the limit is once per turn, not per rest",
    )

    /**
     * A feature whose tracker is named for one benefit inside it, or for the older name.
     *
     * The sheet labels a tracker with what the player spends, which is not always the feature's
     * own title: a Reanimator spends Jolt to Life, not Reanimator's Skill Set.
     */
    private val trackedUnder = mapOf(
        "Psionic Power" to "psionic_energy",
        "The Third Eye" to "third_eye",
        "Sentinel at Death's Door" to "sentinel",
        "Wails from the Grave" to "wails",
        "Dread Ambusher" to "dreadful_strike",
        "Dark One's Own Luck" to "dark_ones_own_luck",
        "Shape-Shifter" to "shapechanger",
        "Banshee's Wail" to "banshees_wail",
        "Reanimator's Skill Set" to "jolt_to_life",
        "Refined Reanimation" to "facilitated_revival",
        "Reanimated Companion" to "companion",
        "Hexblade Manifest" to "curse",
        // Death's Master grants two things; only Bolster Undead is rationed, so that is what
        // the tracker is named after — the counter says what it counts, not what granted it.
        "Death's Master" to "bolster_undead",
        "Combat Superiority" to "superiority",
        "Hand of Ultimate Mercy" to "ultimate_mercy",
        "Celestial Revelation" to "celestial_revelation",
        "Strengthen" to "vampiric_bite",
        "Drain" to "vampiric_bite",
        "Remote Viewing" to "eerie_token",
        "Distant Message" to "eerie_token",
    )

    private fun context(classId: String, subclassId: String?, speciesId: String) =
        ResourceData.Context(
            classId = classId,
            subclassId = subclassId,
            speciesId = speciesId,
            lineageId = null,
            featIds = emptyList(),
            level = 20,
            proficiencyBonus = 6,
            abilityModifiers = Ability.ALL.associateWith { 3 },
            characterLevel = 20,
        )

    private fun slug(name: String) =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    @Test
    fun `every subclass feature that promises uses has a tracker`() {
        val missing = mutableListOf<String>()
        SubclassData.ALL.forEach { subclass ->
            val withIt = ResourceData.forContext(context(subclass.classId, subclass.id, "human"))
                .map { it.id }
            val baseline = ResourceData.forContext(context(subclass.classId, null, "human"))
                .map { it.id }.toSet()
            val own = withIt.filterNot { it in baseline }.map { it.substringAfter(':') }.toSet()

            subclass.features
                .filter { promisesUses.containsMatchIn(it.description) }
                .filterNot { it.name in noTrackerNeeded }
                .filterNot { slug(it.name) in own }
                .filterNot { trackedUnder[it.name] in own }
                .forEach { missing += "${subclass.id} / ${it.name} (level ${it.level})" }
        }
        assertTrue(
            "these promise uses and nothing counts them:\n" +
                missing.joinToString("\n") { "  $it" },
            missing.isEmpty(),
        )
    }

    @Test
    fun `every species trait that promises uses has a tracker`() {
        val missing = mutableListOf<String>()
        val baseline = ResourceData.forContext(context("fighter", null, "human"))
            .map { it.id }.toSet()
        SpeciesData.ALL.forEach { species ->
            val own = ResourceData.forContext(context("fighter", null, species.id))
                .map { it.id }.filterNot { it in baseline }
                .map { it.substringAfter(':') }.toSet()

            species.traits
                .filter { promisesUses.containsMatchIn(it.description) }
                .filterNot { it.name in noTrackerNeeded }
                .filterNot { slug(it.name) in own }
                .filterNot { trackedUnder[it.name] in own }
                .forEach { missing += "${species.id} / ${it.name}" }
        }
        assertTrue(
            "these promise uses and nothing counts them:\n" +
                missing.joinToString("\n") { "  $it" },
            missing.isEmpty(),
        )
    }

    @Test
    fun `nothing is excused that no longer promises uses`() {
        val named = (SubclassData.ALL.flatMap { it.features }.map { it.name } +
            SpeciesData.ALL.flatMap { it.traits }.map { it.name }).toSet()
        val stale = noTrackerNeeded.keys - named
        assertTrue("excused but no such feature: $stale", stale.isEmpty())
    }
}
