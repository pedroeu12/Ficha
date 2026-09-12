package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.Skill

/**
 * The decisions a feat leaves in the player's hands.
 *
 * A feat is rarely a flat grant. Almost every one from level 4 on raises an ability score the
 * player picks; several also name a spell, a damage type, a skill, or a tool "of your choice".
 * None of that was ever asked for, no matter which route the feat arrived by — a background,
 * a Human's Versatile trait, or an Ability Score Improvement spent on a feat.
 *
 * Keeping it in one table means the prompts are identical wherever the feat came from, and a
 * feat that grants another feat's worth of choices resolves the same way.
 */
object FeatChoiceData {

    /**
     * The abilities a feat can raise. Only a feat with more than one becomes a prompt; a feat
     * naming a single ability just raises it, which [com.pedroeu.ficha.domain.FeatBonuses]
     * handles without asking.
     */
    val ABILITY_OPTIONS: Map<String, List<Ability>> = mapOf(
        // ---------------------------------------------------------- General feats
        // These say "Increase one ability score of your choice", naming no list, so every
        // ability is on offer. They were raising nothing at all, because a feat with no
        // entry here is read as a feat that raises no score.
        "delicious_pain" to Ability.ALL,
        "love_bites" to Ability.ALL,
        "putrefy" to Ability.ALL,
        "rebuke" to Ability.ALL,
        "boon_of_blazing_dawn" to Ability.ALL,
        "boon_of_looming_shadows" to Ability.ALL,

        "actor" to listOf(Ability.CHA),
        "athlete" to listOf(Ability.STR, Ability.DEX),
        "charger" to listOf(Ability.STR, Ability.DEX),
        "chef" to listOf(Ability.CON, Ability.WIS),
        "crossbow_expert" to listOf(Ability.DEX),
        "crusher" to listOf(Ability.STR, Ability.CON),
        "defensive_duelist" to listOf(Ability.DEX),
        "dual_wielder" to listOf(Ability.STR, Ability.DEX),
        "durable" to listOf(Ability.CON),
        "elemental_adept" to Ability.ALL,
        "fey_touched" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "grappler" to listOf(Ability.STR, Ability.DEX),
        "great_weapon_master" to listOf(Ability.STR),
        "heavily_armored" to listOf(Ability.STR),
        "heavy_armor_master" to listOf(Ability.STR),
        "inspiring_leader" to listOf(Ability.WIS, Ability.CHA),
        // Both compute a number from whichever of the two they raised — Infernal
        // Bulwark's Armor Class, Infernal Dragoon's save DC — so the answer has to be
        // asked for and kept, not just applied to the score and forgotten.
        "infernal_bulwark" to listOf(Ability.CON, Ability.CHA),
        "infernal_dragoon" to listOf(Ability.CON, Ability.CHA),
        "keen_mind" to listOf(Ability.INT),
        "lightly_armored" to listOf(Ability.STR, Ability.DEX),
        "mage_slayer" to listOf(Ability.STR, Ability.DEX),
        "martial_weapon_training" to listOf(Ability.STR, Ability.DEX),
        "medium_armor_master" to listOf(Ability.STR, Ability.DEX),
        "moderately_armored" to listOf(Ability.STR, Ability.DEX),
        "mounted_combatant" to listOf(Ability.STR, Ability.DEX, Ability.WIS),
        "observant" to listOf(Ability.INT, Ability.WIS),
        "piercer" to listOf(Ability.STR, Ability.DEX),
        "poisoner" to listOf(Ability.DEX, Ability.INT),
        "polearm_master" to listOf(Ability.STR, Ability.DEX),
        "resilient" to Ability.ALL,
        "ritual_caster" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "sentinel" to listOf(Ability.STR, Ability.DEX),
        "shadow_touched" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "sharpshooter" to listOf(Ability.DEX),
        "shield_master" to listOf(Ability.STR),
        "skill_expert" to Ability.ALL,
        "skulker" to listOf(Ability.DEX),
        "slasher" to listOf(Ability.STR, Ability.DEX),
        "speedy" to listOf(Ability.DEX, Ability.CON),
        "spell_sniper" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "telekinetic" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "telepathic" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "war_caster" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "weapon_master" to listOf(Ability.STR, Ability.DEX),

        // ---------------------------------------------------------- Eberron
        "greater_aberrant_mark" to listOf(Ability.CON),
        "greater_mark_of_detection" to Ability.ALL,
        "greater_mark_of_finding" to Ability.ALL,
        "greater_mark_of_handling" to Ability.ALL,
        "greater_mark_of_healing" to Ability.ALL,
        "greater_mark_of_hospitality" to Ability.ALL,
        "greater_mark_of_making" to Ability.ALL,
        "greater_mark_of_passage" to Ability.ALL,
        "greater_mark_of_scribing" to Ability.ALL,
        "greater_mark_of_sentinel" to Ability.ALL,
        "greater_mark_of_shadow" to Ability.ALL,
        "greater_mark_of_storm" to Ability.ALL,
        "greater_mark_of_warding" to Ability.ALL,
        "potent_dragonmark" to Ability.ALL,

        // ---------------------------------------------------------- Heroes of Faerûn
        "cold_caster" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "dragonscarred" to listOf(Ability.CON, Ability.CHA),
        "enclave_magic" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "fairy_trickster" to listOf(Ability.DEX, Ability.CHA),
        "genie_magic" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "harper_teamwork" to listOf(Ability.DEX, Ability.CHA),
        "lordly_resolve" to listOf(Ability.STR, Ability.CHA),
        "mythal_touched" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "orders_resilience" to listOf(Ability.STR, Ability.WIS, Ability.CHA),
        "purple_dragon_commandant" to listOf(Ability.STR, Ability.DEX),
        "spellfire_adept" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "street_justice" to listOf(Ability.STR, Ability.DEX),
        "zhentarim_tactics" to listOf(Ability.DEX, Ability.CHA),

        // ---------------------------------------------------------- Villainous Options
        "death_knight_initiate" to listOf(Ability.STR, Ability.CHA),
        "dread_authority" to listOf(Ability.CON, Ability.CHA),
        "harbinger_of_doom" to listOf(Ability.STR, Ability.CON, Ability.CHA),
        "deathly_presence" to listOf(Ability.STR, Ability.CON, Ability.CHA),
        "unholy_steed" to listOf(Ability.STR, Ability.CON),
        "death_knight_ascension" to listOf(Ability.STR, Ability.CHA),
        "lich_initiate" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "arcane_restoration" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "transfer_life" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "undead_grasp" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "lich_ascension" to listOf(Ability.INT, Ability.WIS, Ability.CHA),

        // ---------------------------------------------------------- Epic Boons
        "boon_combat_prowess" to Ability.ALL,
        "boon_dimensional_travel" to Ability.ALL,
        "boon_energy_resistance" to Ability.ALL,
        "boon_fate" to Ability.ALL,
        "boon_fortitude" to Ability.ALL,
        "boon_irresistible_offense" to listOf(Ability.STR, Ability.DEX),
        "boon_recovery" to Ability.ALL,
        "boon_skill" to Ability.ALL,
        "boon_speed" to Ability.ALL,
        "boon_night_spirit" to Ability.ALL,
        "boon_truesight" to Ability.ALL,
        "boon_of_siberys" to Ability.ALL,
        "boon_of_bloodshed" to Ability.ALL,
        "boon_of_bountiful_health" to Ability.ALL,
        "boon_of_communication" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "boon_of_desperate_resilience" to listOf(Ability.STR, Ability.CON),
        "boon_of_exquisite_radiance" to Ability.ALL,
        "boon_of_fluid_forms" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "boon_of_fortunes_favor" to Ability.ALL,
        "boon_of_poison_mastery" to Ability.ALL,
        "boon_of_revelry" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "boon_of_terror" to listOf(Ability.CHA),
        "boon_of_the_bright_sun" to listOf(Ability.CON, Ability.WIS, Ability.CHA),
        "boon_of_the_furious_storm" to listOf(Ability.INT, Ability.WIS, Ability.CHA),
        "boon_of_the_soul_drinker" to Ability.ALL,
        "boon_of_the_bandit_king" to Ability.ALL,
        "boon_of_the_cleansed_heart" to Ability.ALL,
        "boon_of_the_hunters_eye" to Ability.ALL,
        "boon_of_unwavering_devotion" to Ability.ALL,
    )

    /** Which spell list each Magic Initiate feat draws from, with its spellcasting ability. */
    val MAGIC_INITIATE_LISTS: Map<String, Pair<String, String>> = mapOf(
        "magic_initiate_cleric" to ("cleric" to "Wisdom"),
        "magic_initiate_druid" to ("druid" to "Wisdom"),
        "magic_initiate_wizard" to ("wizard" to "Intelligence"),
    )

    private val ELEMENTAL_DAMAGE = listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")
    private val DRAGON_DAMAGE = listOf("Acid", "Cold", "Fire", "Lightning", "Poison")

    /** Every damage type an Epic Boon can grant Resistance to. */
    private val ALL_DAMAGE = listOf(
        "Acid", "Cold", "Fire", "Force", "Lightning", "Necrotic",
        "Poison", "Psychic", "Radiant", "Thunder",
    )

    private fun spellOptions(
        classId: String,
        level: Int,
        schools: Set<String> = emptySet(),
        onlyRituals: Boolean = false,
        onlyAttackRolls: Boolean = false,
    ): List<ChoiceOption> =
        SpellData.forClass(classId, level)
            .filter { schools.isEmpty() || it.school in schools }
            .filter { !onlyRituals || it.ritual }
            .filter { !onlyAttackRolls || it.needsAttackRoll }
            .map { ChoiceOption(it.id, it.name, it.description, it.subtitle) }

    /** The same spell drawn from several class lists at once, de-duplicated by id. */
    private fun spellOptionsAcross(
        classIds: List<String>,
        level: Int,
        schools: Set<String> = emptySet(),
        onlyRituals: Boolean = false,
        onlyAttackRolls: Boolean = false,
    ): List<ChoiceOption> = classIds
        .flatMap { spellOptions(it, level, schools, onlyRituals, onlyAttackRolls) }
        .distinctBy { it.id }
        .sortedBy { it.name }

    private val ARCANE_AND_DIVINE =
        listOf("bard", "cleric", "druid", "sorcerer", "warlock", "wizard")

    /**
     * Every choice [featId] forces, ready to present. [featName] labels them so a player can
     * tell which feat is asking, which matters when several are taken at once.
     */

    /**
     * Feats whose magic says *"Intelligence, Wisdom, or Charisma is your spellcasting ability
     * for this spell (choose when you select this feat)"*.
     *
     * Twenty-three of them say it, in those words, and two were asked. The rest handed the
     * player a spell with no ability behind it, so the sheet could not work out its save DC
     * or attack bonus — the feat was on the sheet and unusable.
     *
     * They are listed rather than detected from the text: a list is what a reader can check
     * against the book, and the coverage test fails if a new feat says it and is not here.
     */
    val CASTING_ABILITY_FEATS: Map<String, String> = mapOf(
        "mark_of_detection" to "Magical Detection",
        "mark_of_finding" to "Magical Discovery",
        "mark_of_handling" to "Primal Connection",
        "mark_of_healing" to "Medical Intuition",
        "mark_of_hospitality" to "Ever Hospitable",
        "mark_of_making" to "Artisan's Intuition",
        "mark_of_passage" to "Magical Passage",
        "mark_of_scribing" to "Gifted Scribe",
        "mark_of_sentinel" to "Vigilant Guardian",
        "mark_of_shadow" to "Cunning Intuition",
        "mark_of_storm" to "Headwinds",
        "mark_of_warding" to "Wards and Seals",
        "emerald_enclave_fledgling" to "Emerald Enclave Fledgling",
        "spellfire_spark" to "Spellfire Flame",
        "child_of_the_sun" to "Child of the Sun",
        "shadowmoor_hexer" to "Shadowmoor Hexer",
        "gathered_whispers" to "Gathered Whispers",
        "living_shadow" to "Living Shadow",
        "second_skin" to "Second Skin",
        "touch_of_death" to "Death Touch",
        "fey_pact" to "Fey Pact",
        "undead_grasp" to "Paralyzing Touch",
        "lich_ascension" to "Frightening Gaze",
    )

    private fun castingAbilityChoice(featId: String, featName: String): Choice? {
        val label = CASTING_ABILITY_FEATS[featId] ?: return null
        return Choice(
            id = "feat:$featId:casting_ability",
            label = label,
            prompt = "Choose your spellcasting ability for the magic this feat grants. " +
                "The rules have you decide when you take the feat.",
            count = 1,
            kind = ChoiceKind.ABILITY_SCORE,
            options = ChoiceOptions.fromAbilities(
                listOf(Ability.INT, Ability.WIS, Ability.CHA)
            ),
            source = featName,
        )
    }

    fun choicesFor(featId: String, featName: String): List<Choice> = buildList {
        abilityChoice(featId, featName)?.let { add(it) }
        castingAbilityChoice(featId, featName)?.let { add(it) }
        addAll(specificChoices(featId, featName))
    }

    /** The "+1 to an ability score of your choice" half, when there is a real decision. */
    private fun abilityChoice(featId: String, featName: String): Choice? {
        val options = ABILITY_OPTIONS[featId] ?: return null
        if (options.size < 2) return null
        return Choice(
            id = "feat:$featId:ability",
            label = "$featName Ability Score",
            prompt = "Increase one of these by 1, to a maximum of ${abilityCap(featId)}.",
            count = 1,
            kind = ChoiceKind.ABILITY_SCORE,
            options = ChoiceOptions.fromAbilities(options),
            source = featName,
        )
    }

    /** Epic Boons raise a score past the usual ceiling. */
    private fun abilityCap(featId: String): Int = if (featId.startsWith("boon_")) 30 else 20


    /** "You know one additional language of your choice." */
    private fun languageChoice(featId: String, featName: String) = Choice(
        id = "feat:$featId:language",
        label = "$featName Language",
        prompt = "Choose 1 additional language.",
        count = 1,
        kind = ChoiceKind.LANGUAGE,
        options = ChoiceOptions.fromStrings(LanguageData.ALL),
        source = featName,
    )

    private fun specificChoices(featId: String, featName: String): List<Choice> {
        MAGIC_INITIATE_LISTS[featId]?.let { (listId, ability) ->
            val listName = listId.replaceFirstChar { it.uppercase() }
            return listOf(
                Choice(
                    id = "feat:$featId:cantrips",
                    label = "$featName Cantrips",
                    prompt = "Choose 2 cantrips from the $listName spell list. $ability is your spellcasting ability for them.",
                    count = 2,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions(listId, 0),
                    source = featName,
                ),
                Choice(
                    id = "feat:$featId:spell",
                    label = "$featName Level 1 Spell",
                    prompt = "Choose 1 level 1 spell from the $listName list. You can cast it once per Long Rest without a slot.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions(listId, 1),
                    source = featName,
                ),
            )
        }

        return when (featId) {
            "skilled" -> listOf(
                Choice(
                    id = "feat:skilled:skills",
                    label = "Skilled",
                    prompt = "Choose 3 skill proficiencies.",
                    count = 3,
                    kind = ChoiceKind.SKILL,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                )
            )

            "crafter" -> listOf(
                Choice(
                    id = "feat:crafter:tools",
                    label = "Crafter",
                    prompt = "Choose 3 kinds of Artisan's Tools to gain proficiency with.",
                    count = 3,
                    kind = ChoiceKind.TOOL,
                    options = ChoiceOptions.fromStrings(ToolData.ARTISANS_TOOLS),
                    source = featName,
                )
            )

            "musician" -> listOf(
                Choice(
                    id = "feat:musician:instruments",
                    label = "Musician",
                    prompt = "Choose 3 Musical Instruments to gain proficiency with.",
                    count = 3,
                    kind = ChoiceKind.TOOL,
                    options = ChoiceOptions.fromStrings(ToolData.MUSICAL_INSTRUMENTS),
                    source = featName,
                )
            )

            "elemental_adept" -> listOf(
                Choice(
                    id = "feat:elemental_adept:damage",
                    label = "Elemental Adept",
                    prompt = "Choose the damage type your spells ignore Resistance to.",
                    count = 1,
                    kind = ChoiceKind.DAMAGE_TYPE,
                    options = ChoiceOptions.fromStrings(ELEMENTAL_DAMAGE),
                    source = featName,
                )
            )

            "fey_touched" -> listOf(
                Choice(
                    id = "feat:fey_touched:spell",
                    label = "Fey-Touched Spell",
                    prompt = "Choose 1 level 1 Divination or Enchantment spell. You always have it and Misty Step prepared, and can cast each once per Long Rest without a slot.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptionsAcross(
                        ARCANE_AND_DIVINE, 1, schools = setOf("Divination", "Enchantment"),
                    ),
                    source = featName,
                )
            )

            "shadow_touched" -> listOf(
                Choice(
                    id = "feat:shadow_touched:spell",
                    label = "Shadow-Touched Spell",
                    prompt = "Choose 1 level 1 Illusion or Necromancy spell. You always have it and Invisibility prepared, and can cast each once per Long Rest without a slot.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptionsAcross(
                        ARCANE_AND_DIVINE, 1, schools = setOf("Illusion", "Necromancy"),
                    ),
                    source = featName,
                )
            )

            "spell_sniper" -> listOf(
                Choice(
                    id = "feat:spell_sniper:cantrip",
                    label = "Spell Sniper Cantrip",
                    prompt = "Choose 1 cantrip that requires an attack roll.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptionsAcross(ARCANE_AND_DIVINE, 0, onlyAttackRolls = true),
                    source = featName,
                )
            )

            "ritual_caster" -> listOf(
                Choice(
                    id = "feat:ritual_caster:rituals",
                    label = "Ritual Caster",
                    prompt = "Choose 2 level 1 spells with the Ritual tag for your ritual book.",
                    count = 2,
                    kind = ChoiceKind.SPELL,
                    options = spellOptionsAcross(ARCANE_AND_DIVINE, 1, onlyRituals = true),
                    source = featName,
                )
            )

            "skill_expert" -> listOf(
                Choice(
                    id = "feat:skill_expert:skill",
                    label = "Skill Expert Proficiency",
                    prompt = "Choose 1 skill to gain proficiency in.",
                    count = 1,
                    kind = ChoiceKind.SKILL,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                ),
                Choice(
                    id = "feat:skill_expert:expertise",
                    label = "Skill Expert Expertise",
                    prompt = "Choose 1 skill you're proficient in to gain Expertise with.",
                    count = 1,
                    kind = ChoiceKind.EXPERTISE,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                ),
            )

            "weapon_master" -> listOf(
                Choice(
                    id = "feat:weapon_master:mastery",
                    label = "Weapon Mastery",
                    prompt = "Choose 1 more kind of weapon whose mastery property you can use.",
                    count = 1,
                    kind = ChoiceKind.OPTION,
                    // The same list a class's Weapon Mastery draws from, so the property and
                    // its rules text read identically wherever the choice is made.
                    options = MasteryData.weaponOptions(),
                    source = featName,
                )
            )

            "harper_agent" -> listOf(
                Choice(
                    id = "feat:harper_agent:instrument",
                    label = "Instrument Training",
                    prompt = "Choose 1 Musical Instrument to gain proficiency with.",
                    count = 1,
                    kind = ChoiceKind.TOOL,
                    options = ChoiceOptions.fromStrings(ToolData.MUSICAL_INSTRUMENTS),
                    source = featName,
                )
            )

            "purple_dragon_rook" -> listOf(
                Choice(
                    id = "feat:purple_dragon_rook:skill",
                    label = "Entreat",
                    prompt = "Choose 1 skill to gain proficiency in.",
                    count = 1,
                    kind = ChoiceKind.SKILL,
                    options = ChoiceOptions.fromSkills(
                        listOf(Skill.INSIGHT, Skill.PERFORMANCE, Skill.PERSUASION)
                    ),
                    source = featName,
                )
            )

            "cult_of_the_dragon_initiate" -> listOf(
                Choice(
                    id = "feat:cult_of_the_dragon_initiate:language",
                    label = "Dragon's Tongue",
                    prompt = "You learn Draconic, or another language if you already know it.",
                    count = 1,
                    kind = ChoiceKind.LANGUAGE,
                    options = ChoiceOptions.fromStrings(LanguageData.ALL),
                    source = featName,
                )
            )

            "dragonscarred" -> listOf(
                Choice(
                    id = "feat:dragonscarred:resistance",
                    label = "Damage Resistance",
                    prompt = "Choose the damage type you gain Resistance to.",
                    count = 1,
                    kind = ChoiceKind.DAMAGE_TYPE,
                    options = ChoiceOptions.fromStrings(DRAGON_DAMAGE),
                    source = featName,
                )
            )

            "boon_energy_resistance" -> listOf(
                Choice(
                    id = "feat:boon_energy_resistance:damage",
                    label = "Energy Resistance",
                    prompt = "Choose 2 damage types you gain Resistance to.",
                    count = 2,
                    kind = ChoiceKind.DAMAGE_TYPE,
                    options = ChoiceOptions.fromStrings(ALL_DAMAGE),
                    source = featName,
                )
            )

            "boon_skill" -> listOf(
                Choice(
                    id = "feat:boon_skill:expertise",
                    label = "Boon of Skill",
                    prompt = "You gain proficiency in every skill. Choose 1 to gain Expertise with.",
                    count = 1,
                    kind = ChoiceKind.EXPERTISE,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                )
            )

            // "You have proficiency in two skills of your choice. In addition, choose one
            // skill you have proficiency in [for Expertise]. You know one additional
            // language of your choice." Three questions; none was asked.
            "echoing_soul" -> listOf(
                Choice(
                    id = "feat:echoing_soul:skills",
                    label = "Echoing Soul Skills",
                    prompt = "Choose 2 skills you are proficient in.",
                    count = 2,
                    kind = ChoiceKind.SKILL,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                ),
                Choice(
                    id = "feat:echoing_soul:expertise",
                    label = "Echoing Soul Expertise",
                    prompt = "Choose 1 skill you are proficient in to gain Expertise in.",
                    count = 1,
                    kind = ChoiceKind.EXPERTISE,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                ),
                languageChoice("echoing_soul", featName),
            )

            "symbiotic_being" -> listOf(languageChoice("symbiotic_being", featName))

            // "You know the Light cantrip. If you already know that cantrip, you learn a
            // different Cleric cantrip of your choice."
            "light_bringer" -> listOf(
                Choice(
                    id = "feat:light_bringer:spare_cantrip",
                    label = "Light Bringer",
                    prompt = "You learn Light. If you already knew it, choose the Cleric " +
                        "cantrip you learn instead.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions("cleric", 0).filterNot { it.id == "light" },
                    source = featName,
                )
            )

            "vampire_touched" -> listOf(
                Choice(
                    id = "feat:vampire_touched:spell",
                    label = "Vampire Touched",
                    prompt = "Choose 1 level 1 spell from the Enchantment or Illusion school.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.ALL
                        .filter {
                            it.level == 1 &&
                                (it.school.equals("Enchantment", ignoreCase = true) ||
                                    it.school.equals("Illusion", ignoreCase = true))
                        }
                        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) },
                    source = featName,
                )
            )

            "genie_magic" -> listOf(
                Choice(
                    id = "feat:genie_magic:spell",
                    label = "Wish Magic",
                    prompt = "Choose 1 level 1 Sorcerer spell you can cast once per Long Rest.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions("sorcerer", 1),
                    source = featName,
                )
            )

            "aberrant_dragonmark" -> listOf(
                Choice(
                    id = "feat:aberrant_dragonmark:cantrip",
                    label = "Aberrant Magic Cantrip",
                    prompt = "Choose 1 cantrip from the Sorcerer spell list. Constitution is your spellcasting ability for it.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions("sorcerer", 0),
                    source = featName,
                ),
                Choice(
                    id = "feat:aberrant_dragonmark:spell",
                    label = "Aberrant Magic Spell",
                    prompt = "Choose 1 level 1 Sorcerer spell you always have prepared and can cast free once per Short or Long Rest.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions("sorcerer", 1),
                    source = featName,
                ),
            )

            else -> emptyList()
        }
    }

    /**
     * Every feat id these tables are keyed by. A key matching no real feat would ask for a
     * choice nobody can act on, or silently skip one that should be asked, so a test checks
     * this against [FeatData].
     */
    fun sourceIds(): Set<String> =
        ABILITY_OPTIONS.keys + MAGIC_INITIATE_LISTS.keys +
            CASTING_ABILITY_FEATS.keys + FEATS_WITH_SPECIFIC_CHOICES

    private val FEATS_WITH_SPECIFIC_CHOICES = setOf(
        "skilled", "crafter", "musician", "elemental_adept", "fey_touched", "shadow_touched",
        "spell_sniper", "ritual_caster", "skill_expert", "weapon_master", "harper_agent",
        "purple_dragon_rook", "cult_of_the_dragon_initiate", "dragonscarred",
        "boon_energy_resistance", "boon_skill", "genie_magic", "aberrant_dragonmark",
        "echoing_soul", "symbiotic_being", "light_bringer", "vampire_touched",
    )
}
