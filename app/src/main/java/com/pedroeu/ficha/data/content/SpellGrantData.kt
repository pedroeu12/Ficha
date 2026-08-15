package com.pedroeu.ficha.data.content

/**
 * Spells a character is simply given, with no pick to make.
 *
 * The app already asks the player to choose whenever the rules say "of your choice". This is
 * the other half: a Cleric's domain spells, a Warlock's patron spells, an Artificer knowing
 * Mending, a dragonmark's free casting. Those are always the same spells, so the sheet should
 * list them without anyone selecting anything.
 *
 * Grants are derived at read time rather than written into the character, so levelling up or
 * taking a feat adds the right spells straight away, and characters made before this existed
 * pick up what they were missing.
 */
object SpellGrantData {

    /**
     * One spell a source hands over.
     *
     * [alwaysPrepared] marks the "you always have X prepared" wording, which doesn't count
     * against the character's prepared limit.
     */
    data class Grant(
        val spellId: String,
        /** Character (or class) level at which the grant kicks in. */
        val level: Int = 1,
        val alwaysPrepared: Boolean = true,
    )

    private fun at(level: Int, vararg spellIds: String): List<Grant> =
        spellIds.map { Grant(it, level) }

    // ------------------------------------------------------------------ Classes

    private val BY_CLASS: Map<String, List<Grant>> = mapOf(
        // Tinker's Magic hands the Artificer the Mending cantrip outright.
        "artificer" to at(1, "mending"),
        // Druidic teaches Druidcraft alongside the secret language.
        "druid" to at(1, "druidcraft"),
        "warlock" to at(1, "eldritch_blast"),
        // Favored Enemy at level 1, then Paladin's Smite at 2 and Faithful Steed at 5. All
        // three are worded "you always have X prepared", so they cost nothing from the limit.
        "ranger" to at(1, "hunters_mark"),
        "paladin" to at(2, "divine_smite") + at(5, "find_steed"),
    )

    // ------------------------------------------------------------------ Subclasses

    private val BY_SUBCLASS: Map<String, List<Grant>> = mapOf(
        // ---- Cleric domains
        "life_domain" to at(3, "aid", "bless", "cure_wounds", "lesser_restoration") +
            at(5, "mass_healing_word", "revivify") +
            at(7, "aura_of_life", "death_ward") +
            at(9, "greater_restoration", "mass_cure_wounds"),
        "light_domain" to at(3, "burning_hands", "faerie_fire", "scorching_ray", "see_invisibility") +
            at(5, "daylight", "fireball") +
            at(7, "arcane_eye", "wall_of_fire") +
            at(9, "flame_strike", "scrying"),
        "trickery_domain" to at(3, "charm_person", "disguise_self", "invisibility", "pass_without_trace") +
            at(5, "hypnotic_pattern", "nondetection") +
            at(7, "confusion", "dimension_door") +
            at(9, "dominate_person", "modify_memory"),
        "war_domain" to at(3, "divine_favor", "guiding_bolt", "magic_weapon", "shield_of_faith") +
            at(5, "crusaders_mantle", "spirit_guardians") +
            at(7, "fire_shield", "freedom_of_movement") +
            at(9, "hold_monster", "steel_wind_strike"),
        "knowledge_domain" to at(
            3, "command", "comprehend_languages", "detect_magic", "detect_thoughts",
            "identify", "mind_spike",
        ) + at(5, "dispel_magic", "nondetection", "tongues") +
            at(7, "arcane_eye", "banishment", "confusion") +
            at(9, "legend_lore", "scrying", "synaptic_static"),

        // ---- Paladin oaths
        "devotion" to at(3, "protection_evil_good", "shield_of_faith") +
            at(5, "aid", "zone_of_truth") +
            at(9, "beacon_of_hope", "dispel_magic") +
            at(13, "freedom_of_movement", "guardian_of_faith") +
            at(17, "commune", "flame_strike"),
        "glory" to at(3, "guiding_bolt", "heroism") +
            at(5, "enhance_ability", "magic_weapon") +
            at(9, "haste", "protection_from_energy") +
            at(13, "compulsion", "freedom_of_movement") +
            at(17, "flame_strike", "legend_lore"),
        "ancients" to at(3, "ensnaring_strike", "speak_with_animals") +
            at(5, "moonbeam", "misty_step") +
            at(9, "plant_growth", "protection_from_energy") +
            at(13, "ice_storm", "stoneskin") +
            at(17, "commune_with_nature", "tree_stride"),
        "vengeance" to at(3, "bane", "hunters_mark") +
            at(5, "hold_person", "misty_step") +
            at(9, "haste", "protection_from_energy") +
            at(13, "banishment", "dimension_door") +
            at(17, "hold_monster", "scrying"),
        "noble_genies" to at(3, "chromatic_orb", "elementalism", "thunderous_smite") +
            at(5, "mirror_image", "phantasmal_force") +
            at(9, "fly", "gaseous_form") +
            at(13, "conjure_minor_elementals", "summon_elemental") +
            at(17, "banishing_smite", "contact_other_plane"),

        // ---- Warlock patrons
        "fiend" to at(3, "burning_hands", "command", "scorching_ray", "suggestion") +
            at(5, "fireball", "stinking_cloud") +
            at(7, "fire_shield", "wall_of_fire") +
            at(9, "flame_strike", "hallow"),
        "archfey" to at(3, "calm_emotions", "faerie_fire", "misty_step", "phantasmal_force") +
            at(5, "blink", "plant_growth") +
            at(7, "dominate_beast", "greater_invisibility") +
            at(9, "dominate_person", "seeming"),
        "great_old_one" to at(3, "detect_thoughts", "dissonant_whispers", "phantasmal_force", "tashas_hideous_laughter") +
            at(5, "clairvoyance", "hunger_of_hadar") +
            at(7, "confusion", "summon_aberration") +
            at(9, "modify_memory", "telekinesis"),
        "celestial" to at(3, "aid", "cure_wounds", "guiding_bolt", "lesser_restoration") +
            at(5, "daylight", "revivify") +
            at(7, "guardian_of_faith", "wall_of_fire") +
            at(9, "greater_restoration", "summon_celestial"),

        // ---- Sorcerer origins
        "draconic" to at(3, "alter_self", "chromatic_orb", "command", "dragons_breath") +
            at(5, "fear", "fly") +
            at(7, "arcane_eye", "charm_monster") +
            at(9, "legend_lore", "summon_dragon"),
        "wild_magic" to at(3, "chaos_bolt", "confusion", "blink", "fireball") +
            at(5, "fly", "counterspell") +
            at(7, "polymorph", "greater_invisibility") +
            at(9, "bigbys_hand", "mass_cure_wounds"),
        "clockwork" to at(3, "aid", "alarm", "lesser_restoration", "protection_evil_good") +
            at(5, "dispel_magic", "protection_from_energy") +
            at(7, "freedom_of_movement", "summon_construct") +
            at(9, "greater_restoration", "wall_of_force"),
        "spellfire_sorcery" to at(3, "cure_wounds", "guiding_bolt", "lesser_restoration", "scorching_ray") +
            at(5, "aura_of_vitality", "dispel_magic") +
            at(7, "fire_shield", "wall_of_fire") +
            at(9, "greater_restoration", "flame_strike"),

        // ---- Ranger subclasses
        "gloom_stalker" to at(3, "disguise_self") +
            at(5, "rope_trick") +
            at(9, "fear") +
            at(13, "greater_invisibility") +
            at(17, "seeming"),
        "fey_wanderer" to at(3, "charm_person") +
            at(5, "misty_step") +
            at(9, "dispel_magic") +
            at(13, "dimension_door") +
            at(17, "mislead"),
        "hunter" to emptyList(),
        "beast_master" to emptyList(),
        "winter_walker" to at(3, "ice_knife") +
            at(5, "hold_person") +
            at(9, "remove_curse") +
            at(13, "ice_storm") +
            at(17, "cone_of_cold"),

        // ---- Artificer subclasses
        "alchemist" to at(3, "healing_word", "ray_of_sickness") +
            at(5, "flaming_sphere", "melfs_acid_arrow") +
            at(9, "gaseous_form", "mass_healing_word") +
            at(13, "death_ward", "vitriolic_sphere") +
            at(17, "cloudkill", "raise_dead"),
        "armorer" to at(3, "magic_missile", "thunderwave") +
            at(5, "mirror_image", "shatter") +
            at(9, "hypnotic_pattern", "lightning_bolt") +
            at(13, "fire_shield", "greater_invisibility") +
            at(17, "passwall", "wall_of_force"),
        "artillerist" to at(3, "shield", "thunderwave") +
            at(5, "scorching_ray", "shatter") +
            at(9, "fireball", "wind_wall") +
            at(13, "ice_storm", "wall_of_fire") +
            at(17, "cone_of_cold", "wall_of_force"),
        "battle_smith" to at(3, "heroism", "shield") +
            at(5, "shining_smite", "warding_bond") +
            at(9, "aura_of_vitality", "conjure_barrage") +
            at(13, "aura_of_purity", "fire_shield") +
            at(17, "banishing_smite", "mass_cure_wounds"),
        "cartographer" to at(3, "faerie_fire", "guiding_bolt", "healing_word") +
            at(5, "locate_object", "mind_spike") +
            at(9, "call_lightning", "clairvoyance") +
            at(13, "banishment", "locate_creature") +
            at(17, "scrying", "teleportation_circle"),

        // ---- Unearthed Arcana 2026: Villainous Options
        "pestilence_domain" to at(
            3, "detect_poison_disease", "protection_from_poison", "ray_of_enfeeblement",
            "ray_of_sickness",
        ) + at(5, "stinking_cloud", "vampiric_touch") +
            at(7, "blight", "giant_insect") +
            at(9, "contagion", "insect_plague"),
        "circle_of_the_titan" to at(3, "enlarge_reduce", "thaumaturgy", "thunderwave") +
            at(5, "fear") +
            at(7, "fire_shield") +
            at(9, "destructive_wave"),
        "demonic_sorcery" to at(3, "bane", "dissonant_whispers", "spike_growth", "web") +
            at(5, "bestow_curse", "dispel_magic") +
            at(7, "giant_insect", "hallucinatory_terrain") +
            at(9, "contact_other_plane", "modify_memory"),

        // ---- Unearthed Arcana 2026: Villainous Options 2
        // Commune with the Dead: a Ritual-only casting, so it belongs on the list even though
        // the Barbarian otherwise has no spells at all.
        "path_of_lament" to at(6, "speak_with_dead"),
        // The Primordial Patron's list follows its chosen element and lives in BY_CHOICE.
        // Primordial Herald is the one part that doesn't: it casts Planar Ally regardless.
        "primordial_patron" to at(14, "planar_ally"),

        // ---- Unearthed Arcana 2025: Horror Subclasses
        "reanimator" to at(3, "false_life", "spare_the_dying", "witch_bolt") +
            at(5, "blindness_deafness", "enhance_ability") +
            at(9, "animate_dead", "lightning_bolt") +
            at(13, "blight", "death_ward") +
            at(17, "antilife_shell", "raise_dead"),
        "grave_domain" to at(
            3, "bane", "chill_touch", "detect_evil_and_good", "gentle_repose",
            "ray_of_enfeeblement",
        ) + at(5, "revivify", "vampiric_touch") +
            at(7, "blight", "dispel_evil_and_good") +
            at(9, "hold_monster", "raise_dead"),
        "hollow_warden" to at(3, "wrathful_smite") +
            at(5, "spike_growth") +
            at(9, "phantom_steed") +
            at(13, "hallucinatory_terrain") +
            at(17, "awaken"),
        "shadow_sorcery" to at(3, "bane", "darkness", "inflict_wounds", "pass_without_trace") +
            at(5, "hunger_of_hadar", "summon_undead") +
            at(7, "greater_invisibility", "phantasmal_killer") +
            at(9, "contagion", "creation"),
        "hexblade_patron" to at(
            3, "arcane_vigor", "hex", "magic_weapon", "shield", "wrathful_smite",
        ) + at(5, "conjure_barrage", "dispel_magic") +
            at(7, "freedom_of_movement", "staggering_smite") +
            at(9, "animate_objects", "steel_wind_strike"),
        "undead_patron" to at(
            3, "blindness_deafness", "false_life", "phantasmal_force", "ray_of_sickness",
        ) + at(5, "speak_with_dead", "vampiric_touch") +
            at(7, "death_ward", "phantasmal_killer") +
            at(9, "antilife_shell", "cloudkill"),

        // The College of Spirits and the Phantom always have one spell rather than a table.
        "college_of_spirits" to at(6, "spirit_guardians"),
        "phantom" to at(9, "speak_with_dead") + at(9, "augury"),

        // ---- Arcane subclasses whose features name a spell they always have prepared.
        // Each of these said so in its own text and granted nothing, which is the same gap
        // that left Cleric domains empty — a promise in prose that reached no spell list.
        "glamour" to at(3, "charm_person", "mirror_image"),
        "illusionist" to at(6, "summon_beast", "summon_fey"),
        "enchanter" to at(14, "modify_memory"),
        "necromancer" to at(6, "animate_dead"),
        "transmuter" to at(3, "alter_self") + at(10, "polymorph"),
        "aberrant" to at(
            3, "arms_of_hadar", "dissonant_whispers", "calm_emotions", "detect_thoughts",
        ),
        "elements" to at(6, "elementalism"),
        "sea" to at(3, "fog_cloud", "gust_of_wind") +
            at(5, "water_breathing") +
            at(7, "control_water") +
            at(9, "conjure_elemental"),

        // ---- Other subclasses that hand over a specific spell
        "college_of_the_moon" to at(6, "moonbeam"),
        "land" to emptyList(),
        "moon" to emptyList(),
        "abjurer" to emptyList(),
        "eldritch_knight" to emptyList(),
        "arcane_trickster" to emptyList(),
    )

    // ------------------------------------------------------------------ Species

    private val BY_SPECIES: Map<String, List<Grant>> = mapOf(
        // The Khoravar's Fey Gift starts as Friends and can be swapped on a Long Rest.
        "khoravar" to at(1, "friends"),
        // Control Air and Water opens up as the Triton grows into their heritage.
        "triton" to at(1, "fog_cloud") + at(3, "gust_of_wind") + at(5, "wall_of_water"),
    )

    private val BY_LINEAGE: Map<String, List<Grant>> = mapOf(
        "drow" to at(1, "dancing_lights") + at(3, "faerie_fire") + at(5, "darkness"),
        "wood_elf" to at(1, "druidcraft") + at(3, "longstrider") + at(5, "pass_without_trace"),
        "infernal" to at(1, "thaumaturgy") + at(3, "hellish_rebuke") + at(5, "darkness"),
        "abyssal" to at(1, "poison_spray") + at(3, "ray_of_sickness") + at(5, "hold_person"),
        "chthonic" to at(1, "chill_touch") + at(3, "false_life") + at(5, "ray_of_enfeeblement"),
    )

    // ------------------------------------------------------------------ Feats

    private val BY_FEAT: Map<String, List<Grant>> = mapOf(
        "fey_touched" to at(1, "misty_step"),
        "shadow_touched" to at(1, "invisibility"),
        "telekinetic" to at(1, "mage_hand"),
        "telepathic" to at(1, "detect_thoughts"),

        // ---- Eberron dragonmarks
        "mark_of_detection" to at(1, "detect_magic", "detect_poison_disease") +
            at(3, "see_invisibility"),
        "mark_of_finding" to at(1, "hunters_mark") + at(3, "locate_object"),
        "mark_of_handling" to at(1, "animal_friendship", "speak_with_animals"),
        "mark_of_healing" to at(1, "cure_wounds") + at(3, "lesser_restoration"),
        "mark_of_hospitality" to at(1, "purify_food_drink", "unseen_servant") +
            at(3, "calm_emotions"),
        "mark_of_making" to at(1, "mending", "magic_weapon"),
        "mark_of_passage" to at(1, "misty_step"),
        "mark_of_scribing" to at(1, "message", "comprehend_languages") + at(3, "magic_mouth"),
        "mark_of_sentinel" to at(1, "shield"),
        "mark_of_shadow" to at(1, "minor_illusion", "invisibility"),
        "mark_of_storm" to at(1, "thunderclap") + at(3, "gust_of_wind"),
        "mark_of_warding" to at(1, "alarm", "mage_armor") + at(3, "arcane_lock"),

        // ---- Heroes of Faerûn
        "emerald_enclave_fledgling" to at(1, "speak_with_animals"),
        "enclave_magic" to at(1, "beast_sense"),
        "spellfire_spark" to at(1, "sacred_flame"),
        "cold_caster" to at(1, "ray_of_frost"),
        "boon_of_revelry" to at(1, "ottos_irresistible_dance"),

        // ---- Paths of Villainy. Each feat names one spell you always have prepared and
        // can cast by spending Death Points rather than a spell slot.
        "death_knight_initiate" to at(1, "wrathful_smite"),
        "dread_authority" to at(1, "command"),
        "harbinger_of_doom" to at(1, "bane"),
        "deathly_presence" to at(1, "fear"),
        "unholy_steed" to at(1, "find_steed"),
        "undead_grasp" to at(1, "chill_touch"),
        "lich_ascension" to at(1, "fear"),
        "boon_of_the_cleansed_heart" to at(1, "dispel_evil_and_good"),
    )

    /**
     * Every id these tables are keyed by. A key that matches no real class, subclass, species,
     * lineage, or feat silently grants nothing, so a test checks this against the rulebook data.
     */
    fun sourceIds(): Set<String> =
        BY_CLASS.keys + BY_SUBCLASS.keys + BY_SPECIES.keys + BY_LINEAGE.keys + BY_FEAT.keys

    // ------------------------------------------------------------------ Answers

    /**
     * Grants that follow an answer the player gave rather than a fixed source.
     *
     * The Primordial Patron is the first of these: its always-prepared list depends on the
     * element it chose, and that element changes every time the Warlock gains a level. Since
     * grants are derived at read time rather than written into the character, changing the
     * element on Tuesday swaps the whole list on Tuesday — no migration, nothing left behind.
     *
     * [owner] is the class or subclass whose *own* level the grant's level counts against, so
     * a Warlock 5 / Fighter 3 gets their level 5 elemental spells, not their level 8 ones.
     */
    private data class ChoiceGrants(val owner: String, val grants: List<Grant>)

    /** The four elemental lists, keyed "<choiceId>:<optionId>". */
    private val BY_CHOICE: Map<String, ChoiceGrants> = run {
        // Every element shares these; only the element-specific rows differ.
        val shared = at(3, "chromatic_orb", "darkvision") +
            at(5, "elemental_weapon") +
            at(7, "summon_elemental") +
            at(9, "commune_with_nature")

        fun element(vararg rows: List<Grant>) =
            ChoiceGrants("primordial_patron", shared + rows.toList().flatten())

        val choice = SubclassData.ELEMENT_CHOICE_ID
        mapOf(
            "$choice:air" to element(
                at(3, "feather_fall", "shatter"), at(5, "fly"),
                at(7, "freedom_of_movement"), at(9, "steel_wind_strike"),
            ),
            "$choice:earth" to element(
                at(3, "entangle", "knock"), at(5, "plant_growth"),
                at(7, "vitriolic_sphere"), at(9, "wall_of_stone"),
            ),
            "$choice:fire" to element(
                at(3, "burning_hands", "heat_metal"), at(5, "fireball"),
                at(7, "wall_of_fire"), at(9, "flame_strike"),
            ),
            "$choice:water" to element(
                at(3, "alter_self", "ice_knife"), at(5, "water_walk"),
                at(7, "control_water"), at(9, "cone_of_cold"),
            ),
        )
    }

    /** Every choice-keyed grant, so a test can hold each one against a real spell. */
    fun choiceGrantKeys(): Set<String> = BY_CHOICE.keys

    fun choiceGrantSpellIds(): Set<String> =
        BY_CHOICE.values.flatMap { it.grants }.map { it.spellId }.toSet()

    /** Everything the character is handed outright, before any level filtering. */
    fun forSources(
        classId: String,
        subclassId: String?,
        speciesId: String,
        lineageId: String?,
        featIds: List<String>,
        selections: Map<String, List<String>> = emptyMap(),
    ): List<Pair<String, Grant>> = buildList {
        BY_CLASS[classId]?.forEach { add(classId to it) }
        subclassId?.let { id -> BY_SUBCLASS[id]?.forEach { add(id to it) } }
        BY_SPECIES[speciesId]?.forEach { add(speciesId to it) }
        lineageId?.let { id -> BY_LINEAGE[id]?.forEach { add(id to it) } }
        featIds.forEach { featId -> BY_FEAT[featId]?.forEach { add(featId to it) } }

        selections.forEach { (choiceId, optionIds) ->
            optionIds.forEach { optionId ->
                BY_CHOICE["$choiceId:$optionId"]?.let { entry ->
                    // Filed under its owner so the level filter and the source caption both
                    // treat it as what it is: a feature of that subclass.
                    entry.grants.forEach { add(entry.owner to it) }
                }
            }
        }
    }
}
