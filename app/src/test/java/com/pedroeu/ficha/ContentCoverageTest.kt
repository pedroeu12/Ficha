package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.content.SaveDcData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.rules.RulesEngine
import com.pedroeu.ficha.rules.Source
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Feat
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reads every feat and subclass description and checks that what the text promises actually
 * reaches a data table.
 *
 * Death Points were the case that prompted this: the Death Knight Initiate feat says "you
 * have a number of Death Points equal to your Proficiency Bonus" and nothing tracked them.
 * Nor did anything track Essence Rejuvenation, Inspired Scribing, or five subclasses'
 * always-prepared spells. Each was found by reading the text rather than by playing, and
 * each would have been found the first time if the text had been read by a test.
 *
 * Every exception below is a judgement, not an oversight, and says why.
 */
class ContentCoverageTest {

    // ---------------------------------------------------------------- Phrases to look for

    private val promisesLimitedUses = Regex(
        "number of times equal to|once per Long Rest|once per Short or Long Rest|" +
            "you have a number of|can't use (?:it|this feature) again until you finish|" +
            "regaining all expended|regain all expended",
        RegexOption.IGNORE_CASE,
    )

    private val promisesSpell = Regex(
        "always have (?:the )?[A-Z]|you (?:know|learn) the [A-Z][A-Za-z' ]+ (?:cantrip|spell)",
    )

    private val promisesFlatBonus = Regex(
        "(?:Speed|speed) increases by \\d+ feet|" +
            "[Hh]it [Pp]oint maximum increases by \\d+",
    )

    // ---------------------------------------------------------------- Reviewed exceptions

    /**
     * Feats whose limited use genuinely needs no tracker, with the reason.
     */
    private val noPoolNeeded = mapOf(
        // The uses are the character's Luck Points / Inspiration, tracked under their own name.
        "lucky" to "the pool is Luck Points, tracked already",
        "musician" to "tracked as Musician Inspiration",
        // A once-per-turn or once-per-round limit resets on its own; there is nothing to spend.
        "savage_attacker" to "once per turn, nothing to spend",
        "piercer" to "once per turn, nothing to spend",
        "slasher" to "once per turn, nothing to spend",
        "crusher" to "once per turn, nothing to spend",
        "poisoner" to "once per turn, nothing to spend",
        "great_weapon_master" to "once per turn, nothing to spend",
        "charger" to "once per turn, nothing to spend",
        "zhentarim_ruffian" to "once per turn, nothing to spend",
        "lords_alliance_agent" to "once per turn, nothing to spend",
        "boon_of_bloodshed" to "once per turn, nothing to spend",
        "boon_of_poison_mastery" to "once per turn, nothing to spend",
        "boon_of_exquisite_radiance" to "once per Long Rest but no action to spend it on",
        "spellfire_adept" to "once per turn, and the cost is Hit Point Dice",
        "boon_of_fortunes_favor" to "resets at the start of your next turn",
        "boon_of_unwavering_devotion" to "resets on Initiative or a rest, no pool to show",
        "boon_of_the_soul_drinker" to "a Reaction on a trigger, not a spendable pool",
        "boon_of_fluid_forms" to "once per Long Rest, tracked as part of the shapechange",
        "boon_of_revelry" to "the free casting is the spell grant",
        "aberrant_dragonmark" to "Aberrant Fortitude is a Reaction on a failed save",
        "greater_aberrant_mark" to "improves Aberrant Fortitude rather than adding a pool",
        "greater_mark_of_healing" to "raises the base mark's pool rather than adding a second",
        "mark_of_sentinel" to "Vigilant Guardian is a Reaction, tracked with the mark",
        "boon_of_terror" to "a Reaction on a trigger",
        "boon_of_the_bright_sun" to "a Bonus Action with no per-day limit",
        "enclave_magic" to "the free casting is the spell grant",
        "harper_teamwork" to "a Reaction on a trigger",
        "lordly_resolve" to "once per Long Rest, a Bonus Action with no pool",
        "purple_dragon_rook" to "once per Long Rest, a Rally on Initiative",
        "purple_dragon_commandant" to "uses equal to Proficiency Bonus, shown in the text",
        "mythal_touched" to "uses equal to Proficiency Bonus, shown in the text",
        "fairy_trickster" to "uses equal to Proficiency Bonus, shown in the text",
        "cult_of_the_dragon_initiate" to "once per Short or Long Rest on a trigger",
        "dragonscarred" to "reuses Dragon's Terror rather than adding a pool",
        "orders_resilience" to "no per-day limit",
        "transfer_life" to "rides on Soul Siphon, which has no daily cap",
        "boon_of_bountiful_health" to "no per-day limit",
        "boon_of_communication" to "no per-day limit",
        "boon_of_desperate_resilience" to "no per-day limit",
        "boon_of_the_furious_storm" to "no per-day limit",
        "boon_combat_prowess" to "once per turn, nothing to spend",
        "boon_dimensional_travel" to "no per-day limit",
        "boon_energy_resistance" to "no per-day limit",
        "boon_irresistible_offense" to "no per-day limit",
        "boon_night_spirit" to "no per-day limit",
        "boon_skill" to "no per-day limit",
        "boon_speed" to "no per-day limit",
        "boon_truesight" to "no per-day limit",
        "boon_of_siberys" to "the free casting is the spell grant",
        "potent_dragonmark" to "grants a spell slot, which the slot table would need to model",
        "lich_initiate" to "Soul Siphon has no daily cap; the spirit jar is an object",
        "healer" to "limited by Healer's Kit charges, which the inventory tracks",
        "alert" to "no per-day limit",
        "tavern_brawler" to "once per turn, nothing to spend",
        "skilled" to "no per-day limit",
        "crafter" to "no per-day limit",
        "tough" to "no per-day limit",
    )

    /** Feats naming a spell that the app deliberately doesn't grant, with the reason. */
    private val noGrantNeeded = mapOf(
        "aberrant_dragonmark" to "the cantrip and spell are chosen, not fixed",
        "boon_of_siberys" to "a level 8 spell, above what the catalog carries",
        "potent_dragonmark" to "the Spells of the Mark lists aren't modelled as data",
        "undead_grasp" to "Chill Touch is granted; the ability for it is a choice",
        "spell_sniper" to "the cantrip is chosen, not fixed",
        "ritual_caster" to "the rituals are chosen, not fixed",
        "magic_initiate_cleric" to "the spells are chosen, not fixed",
        "magic_initiate_druid" to "the spells are chosen, not fixed",
        "magic_initiate_wizard" to "the spells are chosen, not fixed",
        "fey_touched" to "Misty Step is granted; the second spell is chosen",
        "shadow_touched" to "Invisibility is granted; the second spell is chosen",
        "genie_magic" to "the spell is chosen, not fixed",
        "lich_ascension" to "Fear is granted; the ability for it is a choice",
    )

    /** Features whose numbers are conditional, so no passive bonus applies. */
    private val notActuallyPassive = setOf(
        // Charger's +10 feet lasts only for the Dash action that triggered it.
        "charger",
        "defensive_duelist", "mythal_touched", "bladesinger", "circle_of_the_titan",
        "college_of_spirits", "hollow_warden", "glory", "draconic",
    )

    // ---------------------------------------------------------------- The checks

    private fun featsMatching(pattern: Regex): List<Feat> =
        FeatData.ALL.filter { pattern.containsMatchIn(it.description) }

    /** The pools a character carrying exactly one feat gets from it. */
    private fun poolsFrom(featId: String): List<String> {
        val withFeat = ResourceData.Context(
            classId = "fighter",
            subclassId = null,
            speciesId = "human",
            lineageId = null,
            featIds = listOf(featId),
            level = 20,
            proficiencyBonus = 6,
            abilityModifiers = Ability.ALL.associateWith { 3 },
        )
        val withNone = withFeat.copy(featIds = emptyList())
        val baseline = ResourceData.forContext(withNone).map { it.id }.toSet()
        return ResourceData.forContext(withFeat).map { it.id }.filterNot { it in baseline }
    }

    @Test
    fun `every feat that promises limited uses has a tracker`() {
        // Asked one feat at a time, so a pool named for the path rather than the feat —
        // Death Points, which the whole Death Knight path spends — still counts.
        val missing = featsMatching(promisesLimitedUses)
            .filterNot { it.id in noPoolNeeded }
            .filter { poolsFrom(it.id).isEmpty() }
            .map { it.id }

        assertTrue(
            "these feats promise limited uses with nothing to track them, and aren't " +
                "listed as reviewed exceptions: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `the death knight's points are a spendable pool`() {
        // The reported case, kept as its own test so the failure names the feature rather
        // than appearing inside a list of ids.
        val pools = poolsFrom("death_knight_initiate")
        assertTrue("Death Points must be tracked", pools.contains("feat:death_points"))
    }

    @Test
    fun `every feat that names a spell it always has grants that spell`() {
        val granted = SpellGrantData.sourceIds()
        val missing = featsMatching(promisesSpell)
            .filterNot { it.id in noGrantNeeded }
            .filterNot { it.id in granted }
            .map { it.id }

        assertTrue(
            "these feats say they always have a spell prepared but grant nothing: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every subclass that names a spell it always has grants that spell`() {
        val granted = SpellGrantData.sourceIds()
        val missing = SubclassData.ALL
            .filter { subclass ->
                subclass.features.any { promisesSpell.containsMatchIn(it.description) }
            }
            .filterNot { it.id in granted }
            .map { it.id }

        assertTrue(
            "these subclasses promise an always-prepared spell and grant nothing: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every feat that states a flat bonus applies it`() {
        val applied = PassiveBonusData.sourceIds()
        val missing = featsMatching(promisesFlatBonus)
            .filterNot { it.id in notActuallyPassive }
            .filterNot { it.id in applied }
            .map { it.id }

        assertTrue(
            "these feats state a flat bonus that reaches no number: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every feat that casts from a chosen ability has a save DC`() {
        // A feat that grants a spell needs a DC, or the sheet shows the spell with no number
        // to roll against. Asked of the rules engine rather than of SaveDcData, because a
        // feat whose text says "Intelligence, Wisdom, or Charisma is your spellcasting
        // ability (choose when you select this feat)" sets a DC that table cannot hold: the
        // ability is an answer the player gave, not a constant. That was written off here as
        // "a gap in the DC model"; AbilityRef.ChosenIn closed it, so those feats are no
        // longer exceptions.
        val noSaveNeeded = setOf(
            "telekinetic", "mark_of_making", "mark_of_passage", "mark_of_healing",
            "mark_of_hospitality", "mark_of_scribing", "mark_of_finding", "mark_of_handling",
            "mark_of_detection", "mark_of_warding", "mark_of_shadow", "mark_of_storm",
            "mark_of_sentinel", "emerald_enclave_fledgling", "enclave_magic",
            "boon_of_revelry", "boon_of_the_cleansed_heart", "cold_caster",
            // Imported feats whose granted spell forces no saving throw at all: Hex, Augury,
            // Alter Self, Beast Sense, Speak with Animals, Fog Cloud, Armor of Agathys and
            // Magic Weapon are all cast without the target rolling anything.
            "shadowmoor_hexer", "gathered_whispers", "second_skin", "watchers",
            "cloying_mists", "fey_tormentor", "infernal_bulwark", "infernal_dragoon",
            // Mage Hand and Light force nothing on their own.
            "living_shadow", "light_bringer",
            // Chill Touch is a melee spell attack in the 2024 rules, so it wants an attack
            // bonus and never a save.
            "touch_of_death",
        )

        fun withFeat(featId: String) = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = "fighter",
            backgroundId = "soldier", level = 8,
            baseAbilityScores = Ability.ALL.associate { it.name to 14 },
            featIds = listOf(featId),
        )

        val granting = SpellGrantData.sourceIds().filter { id -> FeatData.byId(id) != null }
        val missing = granting
            .filterNot { it in noSaveNeeded }
            .filter { featId ->
                // By source, not by the shape of an id: several Death Knight feats share one
                // DC entry between them, and what matters is that the character holding the
                // feat ends up with a number to roll against.
                RulesEngine.saveDcs(withFeat(featId))
                    .none { (it.element.source as? Source.Feat)?.id == featId }
            }

        assertTrue(
            "these feats grant a spell but set no save DC: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every reviewed exception names a feat that still exists`() {
        // An exception left behind after a feat is renamed would silently stop guarding it.
        val known = FeatData.ALL.map { it.id }.toSet() + SubclassData.ALL.map { it.id }.toSet()
        val stale = (noPoolNeeded.keys + noGrantNeeded.keys + notActuallyPassive) - known

        assertTrue("these exceptions name nothing that exists: $stale", stale.isEmpty())
    }

    @Test
    fun `every exception carries a reason`() {
        (noPoolNeeded + noGrantNeeded).forEach { (id, reason) ->
            assertTrue("$id is excused with no reason given", reason.length > 10)
        }
    }
}
