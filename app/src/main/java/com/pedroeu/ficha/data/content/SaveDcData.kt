package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability

/**
 * Which ability sets the save DC for each thing that forces a saving throw.
 *
 * A character can carry several different DCs at once and they are not interchangeable. A
 * Monk's Stunning Strike is 8 + Proficiency Bonus + Wisdom, while the same Monk's Magic
 * Initiate cantrip is 8 + Proficiency Bonus + Intelligence. Showing one number would be wrong
 * for the other, so every source is tracked separately.
 */
object SaveDcData {

    /** One thing that forces saves, and the ability that sets its DC. */
    data class DcSource(
        val id: String,
        val label: String,
        val ability: Ability,
        /** What the DC covers, shown under the number. */
        val note: String,
    )

    /**
     * Classes whose features force saves without granting spellcasting, or whose feature DC
     * uses a different ability from their spells.
     */
    private val BY_CLASS: Map<String, DcSource> = mapOf(
        "monk" to DcSource(
            "monk", "Monk", Ability.WIS,
            "Stunning Strike, Open Hand Technique, and other Monk features.",
        ),
        "barbarian" to DcSource(
            "barbarian", "Barbarian", Ability.STR,
            "Features that force a save, such as a subclass's Intimidating Presence.",
        ),
        "rogue" to DcSource(
            "rogue", "Rogue", Ability.DEX,
            "Cunning Strike options that force a saving throw.",
        ),
        "fighter" to DcSource(
            "fighter", "Fighter", Ability.STR,
            "Maneuvers and features that force a save. Battle Masters may use Dexterity instead.",
        ),
    )

    /** Subclasses whose save DC comes from an ability other than the class's usual one. */
    private val BY_SUBCLASS: Map<String, DcSource> = mapOf(
        // Subclasses whose features force a save from an ability the class doesn't cast with.
        "hell_knight" to DcSource(
            "hell_knight", "Hellfire Surge", Ability.CON,
            "Hellfire Surge, which uses 8 plus your Constitution modifier and Proficiency Bonus.",
        ),
        "hollow_warden" to DcSource(
            "hollow_warden", "Unnerving Aura", Ability.WIS,
            "Wrath of the Wild's Unnerving Aura, using your Ranger spell save DC.",
        ),
        "phantom" to DcSource(
            "phantom", "Phantom", Ability.DEX,
            "Spirit Query and Voice of Death, which use Dexterity as the spellcasting modifier.",
        ),
        "psi_warrior" to DcSource(
            "psi_warrior", "Psi Warrior", Ability.INT,
            "Psionic Power features that force a saving throw.",
        ),
        "soulknife" to DcSource(
            "soulknife", "Soulknife", Ability.DEX,
            "Psychic Blades and Psionic Power features.",
        ),
        "eldritch_knight" to DcSource(
            "eldritch_knight", "Eldritch Knight", Ability.INT,
            "Spells learned through the Eldritch Knight subclass.",
        ),
        "arcane_trickster" to DcSource(
            "arcane_trickster", "Arcane Trickster", Ability.INT,
            "Spells learned through the Arcane Trickster subclass.",
        ),
        "arcane_archer" to DcSource(
            "arcane_archer", "Arcane Archer", Ability.INT,
            "Arcane Shot options that force a saving throw.",
        ),
        "battle_master" to DcSource(
            "battle_master", "Battle Master", Ability.STR,
            "Maneuvers that force a save. You may use Dexterity in place of Strength.",
        ),
    )

    /**
     * Feats that grant spells, with the ability the feat sets for them. Where the rules let
     * the player pick, the most common choice is used and can be overridden in Edit Mode.
     */
    private val BY_FEAT: Map<String, DcSource> = mapOf(
        "magic_initiate_cleric" to DcSource(
            "magic_initiate_cleric", "Magic Initiate (Cleric)", Ability.WIS,
            "The spells this feat grants.",
        ),
        "magic_initiate_druid" to DcSource(
            "magic_initiate_druid", "Magic Initiate (Druid)", Ability.WIS,
            "The spells this feat grants.",
        ),
        "magic_initiate_wizard" to DcSource(
            "magic_initiate_wizard", "Magic Initiate (Wizard)", Ability.INT,
            "The spells this feat grants.",
        ),
        "fey_touched" to DcSource(
            "fey_touched", "Fey-Touched", Ability.CHA,
            "Misty Step and the level 1 spell this feat grants.",
        ),
        "shadow_touched" to DcSource(
            "shadow_touched", "Shadow-Touched", Ability.CHA,
            "Invisibility and the level 1 spell this feat grants.",
        ),
        "telekinetic" to DcSource(
            "telekinetic", "Telekinetic", Ability.INT,
            "The shove this feat grants.",
        ),
        "telepathic" to DcSource(
            "telepathic", "Telepathic", Ability.INT,
            "Detect Thoughts granted by this feat.",
        ),
        "aberrant_dragonmark" to DcSource(
            "aberrant_dragonmark", "Aberrant Dragonmark", Ability.CON,
            "The cantrip and level 1 spell this mark grants.",
        ),
        "spellfire_spark" to DcSource(
            "spellfire_spark", "Spellfire Spark", Ability.CHA,
            "Sacred Flame cast through spellfire.",
        ),
        "cold_caster" to DcSource(
            "cold_caster", "Cold Caster", Ability.INT,
            "Ray of Frost and the Frostbite effect.",
        ),
        "fairy_trickster" to DcSource(
            "fairy_trickster", "Fairy Trickster", Ability.CHA,
            "The Flustering Strike save.",
        ),
        "genie_magic" to DcSource(
            "genie_magic", "Genie Magic", Ability.CHA,
            "The Sorcerer spell this feat lets you cast.",
        ),
        "mythal_touched" to DcSource(
            "mythal_touched", "Mythal Touched", Ability.INT,
            "Effects rolled on the Mythal-Touched Magic table.",
        ),
        "cult_of_the_dragon_initiate" to DcSource(
            "cult_of_the_dragon_initiate", "Cult of the Dragon Initiate", Ability.WIS,
            "Dragon's Terror.",
        ),
        "boon_of_terror" to DcSource(
            "boon_of_terror", "Boon of Terror", Ability.CHA,
            "The Flee, Fools! Reaction.",
        ),
        "boon_of_siberys" to DcSource(
            "boon_of_siberys", "Boon of Siberys", Ability.CHA,
            "The spell this boon grants.",
        ),
    )

    // Every dragonmark lets you pick Intelligence, Wisdom, or Charisma for its spells.
    private val DRAGONMARK_FEATS = listOf(
        "mark_of_detection", "mark_of_finding", "mark_of_handling", "mark_of_healing",
        "mark_of_hospitality", "mark_of_making", "mark_of_passage", "mark_of_scribing",
        "mark_of_sentinel", "mark_of_shadow", "mark_of_storm", "mark_of_warding",
    )

    /** Species traits that grant a spell, with the ability their DC uses. */
    private val BY_SPECIES: Map<String, DcSource> = mapOf(
        "khoravar" to DcSource(
            "khoravar", "Khoravar", Ability.CHA,
            "The Fey Gift cantrip. You may use Intelligence or Wisdom instead.",
        ),
        "triton" to DcSource(
            "triton", "Control Air and Water", Ability.CHA,
            "Fog Cloud, Gust of Wind, and Wall of Water from your Triton heritage.",
        ),
    )

    private val BY_LINEAGE: Map<String, DcSource> = mapOf(
        "drow" to DcSource("drow", "Drow", Ability.CHA, "Elven Lineage spells."),
        "high_elf" to DcSource("high_elf", "High Elf", Ability.INT, "Elven Lineage cantrip."),
        "wood_elf" to DcSource("wood_elf", "Wood Elf", Ability.WIS, "Elven Lineage spells."),
        "infernal" to DcSource("infernal", "Infernal Legacy", Ability.CHA, "Fiendish Legacy spells."),
        "abyssal" to DcSource("abyssal", "Abyssal Legacy", Ability.CHA, "Fiendish Legacy spells."),
        "chthonic" to DcSource("chthonic", "Chthonic Legacy", Ability.CHA, "Fiendish Legacy spells."),
    )

    fun forClass(classId: String): DcSource? = BY_CLASS[classId]

    fun forSubclass(subclassId: String?): DcSource? = subclassId?.let { BY_SUBCLASS[it] }

    fun forSpecies(speciesId: String): DcSource? = BY_SPECIES[speciesId]

    fun forLineage(lineageId: String?): DcSource? = lineageId?.let { BY_LINEAGE[it] }

    fun forFeat(featId: String): DcSource? = BY_FEAT[featId] ?: dragonmark(featId)

    private fun dragonmark(featId: String): DcSource? =
        if (featId in DRAGONMARK_FEATS) {
            DcSource(
                id = featId,
                label = FeatData.byId(featId)?.name ?: "Dragonmark",
                ability = Ability.CHA,
                note = "The mark's spells. You may use Intelligence or Wisdom instead.",
            )
        } else {
            null
        }

    /** Every id these tables are keyed by, so a test can check them against the real data. */
    fun sourceIds(): Set<String> =
        BY_CLASS.keys + BY_SUBCLASS.keys + BY_FEAT.keys + BY_SPECIES.keys + BY_LINEAGE.keys +
            DRAGONMARK_FEATS
}
