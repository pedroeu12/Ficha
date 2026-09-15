package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.model.ClassFeature
import com.pedroeu.ficha.data.model.ClassProgression
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.OptionSource
import com.pedroeu.ficha.data.model.Skill

/**
 * Level 1-20 tables for every class: features gained, ability score improvements, subclass
 * timing, cantrips known, and prepared spell counts. Spell slots come from
 * [com.pedroeu.ficha.data.model.SpellSlotTables] via the class's caster type.
 */
object ProgressionData {

    /** Ability Score Improvement levels shared by most classes. */
    private val STANDARD_ASI = setOf(4, 8, 12, 16)

    private fun feature(level: Int, name: String, description: String, vararg choices: Choice) =
        ClassFeature(level, name, description, choices.toList())

    // Full casters share this prepared-spell curve in the 2024 rules.
    private val FULL_CASTER_PREPARED = listOf(
        4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22,
    )
    private val HALF_CASTER_PREPARED = listOf(
        2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15,
    )
    private val WARLOCK_KNOWN = listOf(
        2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15,
    )

    /**
     * Arcane plans for the Artificer's Replicate Magic Item feature. The options come straight
     * from [MagicItemData], so a plan and the item a DM hands out are always the same entry.
     */
    private fun replicateChoice(count: Int, level: Int) = Choice(
        // One id across every tier, because each tier restates the whole set rather than
        // adding to it — reaching level 6 is a chance to give up a plan chosen at level 2,
        // not merely to learn a fifth. The level-up flow ticks the current plans for you.
        id = PLAN_CHOICE_ID,
        label = "Magic Item Plans",
        prompt = "Choose your $count plans. You can keep the ones you have or trade any of them now.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = MagicItemData.artificerPlans(level).map { magicItem ->
            ChoiceOption(
                id = magicItem.id,
                name = magicItem.name,
                description = magicItem.description,
                supporting = magicItem.subtitle,
            )
        },
        source = "Level $level",
    )

    /** The single key every tier of the Artificer's plan choice is stored under. */
    const val PLAN_CHOICE_ID = "replicate_plans"

    /**
     * Every Eldritch Invocation on offer, including the two that came with the Primordial
     * Patron. Both of those name a damage type, which is asked separately once the invocation
     * is held — see [OriginChoices.forClass].
     */
    /**
     * Every Eldritch Invocation, with what the rules ask of it.
     *
     * All twenty-eight from the Player's Handbook, plus the two the Primordial Patron adds.
     * Eight of them used to be here, which is not a shorter list so much as a different game:
     * the count a Warlock knows was being clamped to what was on offer, so a level 18 Warlock
     * chose ten out of ten and had no decision to make at all.
     */
    /** The five types both Primordial Patron invocations choose between. */
    private val ELEMENTAL_TYPES = listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")

    /**
     * The Warlock's own damage cantrips, for the three invocations that name one.
     *
     * Drawn from the catalogue rather than listed by hand, so a damage cantrip added to the
     * Warlock list later is offered without a second edit here.
     */
    private fun warlockDamageCantrips(): List<ChoiceOption> = SpellData.ALL
        .filter { it.level == 0 && "warlock" in it.classes && it.damage.isNotBlank() }
        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) }

    /** The cantrip an invocation singles out, asked as soon as the invocation is taken. */
    /**
     * "Choose one of *your known* Warlock cantrips that deals damage."
     *
     * The list is the Warlock's own cantrips, not the Warlock cantrip list. Offering the
     * catalogue turned "make a cantrip you already have stronger" into "learn a new cantrip",
     * which is a different feature and one these invocations do not grant. [options] stays
     * filled as a fallback for a caller with no character to read from.
     */
    private fun cantripChoice(
        invocationId: String,
        label: String,
        prompt: String,
        needsAttackRoll: Boolean = false,
    ) = Choice(
        id = "invocation:$invocationId:cantrip",
        label = label,
        prompt = prompt,
        count = 1,
        kind = ChoiceKind.SPELL,
        options = warlockDamageCantrips(),
        source = label,
        optionsFrom = OptionSource.KnownSpells(
            classId = "warlock",
            level = 0,
            needsDamage = true,
            needsAttackRoll = needsAttackRoll,
        ),
    )

    private fun elementChoice(invocationId: String, label: String, prompt: String) = Choice(
        id = "invocation:$invocationId:damage",
        label = label,
        prompt = prompt,
        count = 1,
        kind = ChoiceKind.DAMAGE_TYPE,
        options = ChoiceOptions.fromStrings(ELEMENTAL_TYPES),
        source = label,
    )

    /**
     * A spell a class feature has you name once and keep — an Arcanum, a mastered spell, a
     * signature spell. Not the same thing as preparing spells, which the spell list handles:
     * these are picked once, stay picked, and the feature is meaningless without the answer.
     */
    private fun namedSpellChoice(
        id: String,
        label: String,
        prompt: String,
        source: String,
        classId: String,
        spellLevel: Int,
        count: Int = 1,
        /** True where the rules say "in your spellbook" rather than "from the list". */
        fromWhatYouKnow: Boolean = false,
        /** True where the rules let the pick be traded at a level up or on a rest. */
        changeableOnLevelUp: Boolean = false,
        changeableOnRest: Boolean = false,
    ) = Choice(
        id = id,
        label = label,
        prompt = prompt,
        count = count,
        kind = ChoiceKind.SPELL,
        options = SpellData.forClass(classId, spellLevel)
            .map { ChoiceOption(it.id, it.name, it.description, it.subtitle, it.book) },
        source = source,
        changeableOnLevelUp = changeableOnLevelUp,
        changeableOnRest = changeableOnRest,
        optionsFrom = if (fromWhatYouKnow) {
            OptionSource.KnownSpells(classId = classId, level = spellLevel)
        } else {
            OptionSource.Declared
        },
    )

    private val INVOCATION_OPTIONS = listOf(
        ChoiceOption("agonizing_blast", "Agonizing Blast", "Choose one of your known Warlock cantrips that deals damage. You can add your Charisma modifier to that spell's damage rolls. Repeatable. You can gain this invocation more than once. Each time you do so, choose a different eligible cantrip.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock, a Warlock Cantrip That Deals Damage",
            grants = listOf(
                cantripChoice("agonizing_blast", "Agonizing Blast",
                    "Choose one of your known Warlock cantrips that deals damage. It adds " +
                        "your Charisma modifier to its damage.")
            )),
        ChoiceOption("armor_of_shadows", "Armor of Shadows", "You can cast Mage Armor on yourself without expending a spell slot."),
        ChoiceOption("ascendant_step", "Ascendant Step", "You can cast Levitate on yourself without expending a spell slot.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock"),
        ChoiceOption("devils_sight", "Devil's Sight", "You can see normally in Dim Light and Darkness-both magical and nonmagical-within 120 feet of yourself.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock"),
        ChoiceOption("devouring_blade", "Devouring Blade", "The Extra Attack of your Thirsting Blade invocation confers two extra attacks rather than one.",
            minLevel = 12,
            requiresOptions = listOf("thirsting_blade"),
            prerequisite = "Level 12+ Warlock, Thirsting Blade Invocation"),
        ChoiceOption("eldritch_mind", "Eldritch Mind", "You have Advantage on Constitution saving throws that you make to maintain Concentration."),
        ChoiceOption("eldritch_smite", "Eldritch Smite", "Once per turn when you hit a creature with your pact weapon, you can expend a Pact Magic spell slot to deal an extra 1d8 Force damage to the target, plus another 1d8 per level of the spell slot, and you can give the target the Prone condition if it is Huge or smaller.",
            minLevel = 5,
            requiresOptions = listOf("pact_blade"),
            prerequisite = "Level 5+ Warlock, Pact of the Blade Invocation"),
        ChoiceOption("eldritch_spear", "Eldritch Spear", "Choose one of your known Warlock cantrips that deals damage and has a range of 10+ feet. When you cast that spell, its range increases by a number of feet equal to 30 times your Warlock level. Repeatable. You can gain this invocation more than once. Each time you do so, choose a different eligible cantrip.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock, a Warlock Cantrip That Deals Damage",
            grants = listOf(
                cantripChoice("eldritch_spear", "Eldritch Spear",
                    "Choose one of your known Warlock cantrips that deals damage. Its range " +
                        "grows with your Warlock level.")
            )),
        ChoiceOption("fiendish_vigor", "Fiendish Vigor", "You can cast False Life on yourself without expending a spell slot. When you cast the spell with this feature, you don't roll the die for the Temporary Hit Points; you automatically get the highest number on the die.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock"),
        ChoiceOption("gaze_of_two_minds", "Gaze of Two Minds", "You can use a Bonus Action to touch a willing creature and perceive through its senses until the end of your next turn. As long as the creature is on the same plane of existence as you, you can take a Bonus Action on subsequent turns to maintain this connection, extending the duration until the end of your next turn. The connection ends if you don't maintain it in this way. While perceiving through the other creature's senses, you benefit from any special senses possessed by that creature, and you can cast spells as if you were in your space or the other creature's space if the two of you are within 60 feet of each other.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock"),
        ChoiceOption("gift_of_the_depths", "Gift of the Depths", "You can breathe underwater, and you gain a Swim Speed equal to your Speed. You can also cast Water Breathing once without expending a spell slot. You regain the ability to cast it in this way again when you finish a Long Rest.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock"),
        ChoiceOption("gift_of_the_protectors", "Gift of the Protectors", "A new page appears in your Book of Shadows when you conjure it. With your permission, a creature can take an action to write its name on that page, which can contain a number of names equal to your Charisma modifier (minimum of one name). When any creature whose name is on the page is reduced to 0 Hit Points but not killed outright, the creature magically drops to 1 Hit Point instead. Once this magic is triggered, no creature can benefit from it until you finish a Long Rest. As a Magic action, you can erase a name on the page by touching it.",
            minLevel = 9,
            requiresOptions = listOf("pact_tome"),
            prerequisite = "Level 9+ Warlock, Pact of the Tome Invocation"),
        ChoiceOption("investment_of_the_chain_master", "Investment of the Chain Master", "When you cast Find Familiar , you infuse the summoned familiar with a measure of your eldritch power, granting the creature the following benefits. Aerial or Aquatic. The familiar gains either a Fly Speed or a Swim Speed (your choice) of 40 feet. Quick Attack. As a Bonus Action, you can command the familiar to take the Attack action. Necrotic or Radiant Damage. Whenever the familiar deals Bludgeoning, Piercing, or Slashing damage, you can make it deal Necrotic or Radiant damage instead. Your Save DC. If the familiar forces a creature to make a saving throw, it uses your spell save DC. Resistance. When the familiar takes damage, you can take a Reaction to grant it Resistance against that damage.",
            minLevel = 5,
            requiresOptions = listOf("pact_chain"),
            prerequisite = "Level 5+ Warlock, Pact of the Chain Invocation"),
        ChoiceOption("lessons_of_the_first_ones", "Lessons of the First Ones", "You have received knowledge from an elder entity of the multiverse, allowing you to gain one Origin feat of your choice. Repeatable. You can gain this invocation more than once. Each time you do so, choose a different Origin feat .",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock",
            grants = listOf(
                Choice(
                    id = "invocation:lessons_of_the_first_ones:feat",
                    label = "Lessons of the First Ones",
                    prompt = "Choose the Origin feat this invocation teaches you.",
                    count = 1,
                    kind = ChoiceKind.FEAT,
                    options = FeatData.ORIGIN_FEATS
                        .map { ChoiceOption(it.id, it.name, it.description, book = it.book) },
                    source = "Lessons of the First Ones",
                )
            )),
        ChoiceOption("lifedrinker", "Lifedrinker", "Once per turn when you hit a creature with your pact weapon, you can deal an extra 1d6 Necrotic, Psychic, or Radiant damage (your choice) to the creature, and you can expend one of your Hit Point Dice to roll it and regain a number of Hit Points equal to the roll plus your Constitution modifier (minimum of 1 Hit Point).",
            minLevel = 9,
            requiresOptions = listOf("pact_blade"),
            prerequisite = "Level 9+ Warlock, Pact of the Blade Invocation"),
        ChoiceOption("mask_of_many_faces", "Mask of Many Faces", "You can cast Disguise Self without expending a spell slot.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock"),
        ChoiceOption("master_of_myriad_forms", "Master of Myriad Forms", "You can cast Alter Self without expending a spell slot.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock"),
        ChoiceOption("misty_visions", "Misty Visions", "You can cast Silent Image without expending a spell slot.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock"),
        ChoiceOption("one_with_shadows", "One with Shadows", "While you're in an area of Dim Light or Darkness, you can cast Invisibility on yourself without expending a spell slot.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock"),
        ChoiceOption("otherworldly_leap", "Otherworldly Leap", "You can cast Jump on yourself without expending a spell slot.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock"),
        ChoiceOption("pact_blade", "Pact of the Blade", "As a Bonus Action, you can conjure a pact weapon in your hand-a Simple or Martial Melee weapon of your choice with which you bond-or create a bond with a magic weapon you touch; you can't bond with a magic weapon if someone else is attuned to it or another Warlock is bonded with it. Until the bond ends, you have proficiency with the weapon, and you can use it as a Spellcasting Focus. Whenever you attack with the bonded weapon, you can use your Charisma modifier for the attack and damage rolls instead of using Strength or Dexterity; and you can cause the weapon to deal Necrotic, Psychic, or Radiant damage or its normal damage type. Your bond with the weapon ends if you use this feature's Bonus Action again, if the weapon is more than 5 feet away from you for 1 minute or more, or if you die. A conjured weapon disappears when the bond ends."),
        ChoiceOption("pact_chain", "Pact of the Chain", "You learn the Find Familiar spell and can cast it as a Magic action without expending a spell slot. When you cast the spell, you choose one of the normal forms for your familiar or one of the following special forms: Imp, Pseudodragon, Quasit, Skeleton, Slaad Tadpole, Sphinx of Wonder, Sprite, or Venomous Snake (see appendix B for the familiar's stat block). Additionally, when you take the Attack action, you can forgo one of your own attacks to allow your familiar to make one attack of its own with its Reaction."),
        ChoiceOption("pact_tome", "Pact of the Tome", "Stitching together strands of shadow, you conjure forth a book in your hand at the end of a Short or Long Rest. This Book of Shadows (you determine its appearance) contains eldritch magic that only you can access, granting you the benefits below. The book disappears if you conjure another book with this feature or if you die. Cantrips and Rituals. When the book appears, choose three cantrips, and choose two level 1 spells that have the Ritual tag. The spells can be from any class's spell list, and they must be spells you don't already have prepared. While the book is on your person, you have the chosen spells prepared, and they function as Warlock spells for you. Spellcasting Focus. You can use the book as a Spellcasting Focus.",
            grants = listOf(
                Choice(
                    id = "invocation:pact_tome:cantrips",
                    label = "Book of Shadows: Cantrips",
                    prompt = "Choose three cantrips from any class\'s spell list.",
                    count = 3,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.ALL.filter { it.level == 0 }
                        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) },
                    source = "Pact of the Tome",
                ),
                Choice(
                    id = "invocation:pact_tome:ritual",
                    label = "Book of Shadows: Rituals",
                    // "choose two level 1 spells that have the Ritual tag" — it asked for one.
                    prompt = "Choose two level 1 spells with the Ritual tag, from any class\'s list.",
                    count = 2,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.ALL.filter { it.level == 1 && it.ritual }
                        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) },
                    source = "Pact of the Tome",
                )
            )),
        ChoiceOption("repelling_blast", "Repelling Blast", "Choose one of your known Warlock cantrips that requires an attack roll. When you hit a Large or smaller creature with that cantrip, you can push the creature up to 10 feet straight away from you. Repeatable. You can gain this invocation more than once. Each time you do so, choose a different eligible cantrip.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock, a Warlock Cantrip That Deals Damage via an Attack Roll",
            grants = listOf(
                cantripChoice("repelling_blast", "Repelling Blast",
                    "Choose one of your known Warlock cantrips that requires an attack roll. " +
                        "Its hits push a creature 10 feet away.",
                    needsAttackRoll = true)
            )),
        ChoiceOption("thirsting_blade", "Thirsting Blade", "You gain the Extra Attack feature for your pact weapon only. With that feature, you can attack twice with the weapon instead of once when you take the Attack action on your turn.",
            minLevel = 5,
            requiresOptions = listOf("pact_blade"),
            prerequisite = "Level 5+ Warlock, Pact of the Blade Invocation"),
        ChoiceOption("visions_of_distant_realms", "Visions of Distant Realms", "You can cast Arcane Eye without expending a spell slot.",
            minLevel = 9,
            prerequisite = "Level 9+ Warlock"),
        ChoiceOption("whispers_of_the_grave", "Whispers of the Grave", "You can cast Speak with Dead without expending a spell slot.",
            minLevel = 7,
            prerequisite = "Level 7+ Warlock"),
        ChoiceOption("witch_sight", "Witch Sight", "You have Truesight with a range of 30 feet.",
            minLevel = 15,
            prerequisite = "Level 15+ Warlock"),
        ChoiceOption("elemental_overflow", "Elemental Overflow", "Choose a damage type: Acid, Cold, Fire, Lightning, or Thunder. Whenever you cast a spell that deals the chosen damage type, you can cause elemental energy to wreathe you until the end of your next turn. For the duration, whenever a creature within 5 feet of you hits you with a melee attack roll, that creature takes 1d4 damage of the chosen damage type. Repeatable: you can gain this invocation more than once, choosing a different damage type each time — hold one damage type per taking in the Elemental Overflow choice this raises.",
            minLevel = 5,
            prerequisite = "Level 5+ Warlock, Primordial Patron",
            grants = listOf(
                elementChoice("elemental_overflow", "Elemental Overflow",
                    "Choose the damage type your overflow wreathes you in. If you have taken " +
                        "this invocation more than once, hold one type per taking.")
            )),
        ChoiceOption("elemental_transmutation", "Elemental Transmutation", "Choose a damage type: Acid, Cold, Fire, Lightning, or Thunder. Once per turn, whenever you deal damage of any of those types, you can deal the chosen damage type instead.",
            minLevel = 2,
            prerequisite = "Level 2+ Warlock, Primordial Patron",
            grants = listOf(
                elementChoice("elemental_transmutation", "Elemental Transmutation",
                    "Choose the damage type you convert your damage into.")
            )),
    )

    /**
     * How many invocations a Warlock knows, from the 2024 Eldritch Invocations column.
     */
    private fun invocationsKnownAt(level: Int): Int = when {
        level >= 18 -> 10
        level >= 15 -> 9
        level >= 12 -> 8
        level >= 9 -> 7
        level >= 7 -> 6
        level >= 5 -> 5
        level >= 2 -> 3
        else -> 1
    }

    /**
     * The invocation question, asked again at each level the count grows.
     *
     * One id across every level, on the same footing as Weapon Mastery and the Artificer's
     * plans: each asking restates the whole set, the newest answer wins, and the level-up
     * flow arrives with the current invocations already ticked.
     */
    private fun invocationChoice(level: Int) = Choice(
        id = INVOCATION_CHOICE_ID,
        label = "Eldritch Invocations",
        prompt = "Choose your ${invocationsKnownAt(level)} Eldritch Invocation" +
            "${if (invocationsKnownAt(level) == 1) "" else "s"}. You can keep the ones you " +
            "have or trade any of them now.",
        count = invocationsKnownAt(level),
        kind = ChoiceKind.OPTION,
        prerequisiteClassId = "warlock",
        options = INVOCATION_OPTIONS,
        source = "Level $level",
        // "Whenever you gain a Warlock level, you can replace one of your invocations with
        // another one for which you qualify."
        changeableOnLevelUp = true,
    )

    /** The single key every level of the Warlock's invocation choice is stored under. */
    const val INVOCATION_CHOICE_ID = "invocations"

    private val METAMAGIC_OPTIONS = listOf(
        ChoiceOption("careful", "Careful Spell", "When you cast a spell that forces other creatures to make a saving throw, you can protect some of them. You spend 1 Sorcery Point and choose a number of those creatures up to your Charisma modifier (minimum of one). A chosen creature automatically succeeds on its saving throw against the spell, and it takes no damage if it would normally take half damage on a successful save.", "1 Sorcery Point"),
        ChoiceOption("distant", "Distant Spell", "When you cast a spell that has a range of 5 feet or greater, you can spend 1 Sorcery Point to double the spell's range. Or when you cast a spell that has a range of Touch, you can spend 1 Sorcery Point to make its range 30 feet.", "1 Sorcery Point"),
        ChoiceOption("empowered", "Empowered Spell", "When you roll damage for a spell, you can spend 1 Sorcery Point to reroll a number of the damage dice up to your Charisma modifier (minimum of one), and you must use the new rolls. You can use this Metamagic even if you have already used another one during the casting of the spell.", "1 Sorcery Point"),
        ChoiceOption("extended", "Extended Spell", "When you cast a spell that has a duration of 1 minute or longer, you can spend 1 Sorcery Point to double its duration, to a maximum duration of 24 hours. If the spell requires Concentration, you have Advantage on any saving throw you make to maintain that Concentration.", "1 Sorcery Point"),
        ChoiceOption("heightened", "Heightened Spell", "When you cast a spell that forces a creature to make a saving throw, you can spend 2 Sorcery Points to give one target of the spell Disadvantage on saves against the spell.", "2 Sorcery Points"),
        ChoiceOption("quickened", "Quickened Spell", "When you cast a spell that has a casting time of an action, you can spend 2 Sorcery Points to change the casting time to a Bonus Action for this casting. You can't modify a spell in this way if you have already cast a level 1+ spell on the current turn, nor can you cast a level 1+ spell on this turn after modifying a spell in this way.", "2 Sorcery Points"),
        ChoiceOption("seeking", "Seeking Spell", "If you make an attack roll for a spell and miss, you can spend 1 Sorcery Point to reroll the d20, and you must use the new roll. You can use this Metamagic even if you have already used another one during the casting of the spell.", "1 Sorcery Point"),
        ChoiceOption("subtle", "Subtle Spell", "When you cast a spell, you can spend 1 Sorcery Point to cast it without any Verbal, Somatic, or Material components, except Material components that are consumed by the spell or that have a cost specified in the spell.", "1 Sorcery Point"),
        ChoiceOption("transmuted", "Transmuted Spell", "When you cast a spell that deals a type of damage from the following list, you can spend 1 Sorcery Point to change that damage type to one of the other listed types: Acid, Cold, Fire, Lightning, Poison, Thunder.", "1 Sorcery Point"),
        ChoiceOption("twinned", "Twinned Spell", "When you cast a spell such as Charm Person that can be cast with a higher-level spell slot to target an additional creature, you can spend a number of Sorcery Points equal to the spell's level to increase its effective level by 1. If the spell is a cantrip, it costs 1 Sorcery Point.", "Spell level in points"),
    )

    private fun metamagicChoice(id: String, count: Int, level: Int) = Choice(
        id = id,
        label = "Metamagic",
        prompt = "Choose $count Metamagic option${if (count == 1) "" else "s"}.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = METAMAGIC_OPTIONS,
        source = "Level $level",
        resourceId = "sorcerer:sorcery_points",
        // "Whenever you gain a Sorcerer level, you can replace one of your Metamagic options
        // with one you don't know." The app had the pick and not the trade, so a Sorcerer who
        // regretted one at level 3 carried it to twenty.
        changeableOnLevelUp = true,
    )

    private val FIGHTING_STYLE_OPTIONS = listOf(
        ChoiceOption("arcane_warrior", "Arcane Warrior", "You learn two Wizard cantrips of your choice. Mage Hand and Ray of Frost are recommended. Intelligence, Wisdom, or Charisma is your spellcasting ability for these spells. Whenever you gain a level, you can replace one of these cantrips with another Wizard cantrip.",
            grants = listOf(
                Choice(
                    id = "fighting_style:arcane_warrior:cantrips",
                    label = "Arcane Warrior Cantrips",
                    prompt = "Choose 2 Wizard cantrips.",
                    count = 2,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.cantripsForClass("wizard")
                        .map { ChoiceOption(it.id, it.name, it.description, it.subtitle) },
                    source = "Arcane Warrior",
                    changeableOnLevelUp = true,
                ),
                Choice(
                    id = "fighting_style:arcane_warrior:casting_ability",
                    label = "Arcane Warrior",
                    prompt = "Choose your spellcasting ability for these cantrips.",
                    count = 1,
                    kind = ChoiceKind.ABILITY_SCORE,
                    options = ChoiceOptions.fromAbilities(
                        listOf(Ability.INT, Ability.WIS, Ability.CHA)
                    ),
                    source = "Arcane Warrior",
                ),
            )),
        ChoiceOption("archery", "Archery", "You gain a +2 bonus to attack rolls you make with Ranged weapons."),
        ChoiceOption("blind_fighting", "Blind Fighting", "You have Blindsight with a range of 10 feet. Within that range, you can see anything that isn't behind Total Cover even if you have the Blinded condition or are in Darkness. Moreover, in that range you can see a creature that has the Invisible condition."),
        ChoiceOption("defense", "Defense", "While you're wearing Light, Medium, or Heavy armor, you gain a +1 bonus to Armor Class."),
        ChoiceOption("dueling", "Dueling", "When you're wielding a Melee weapon in one hand and no other weapons, you gain a +2 bonus to damage rolls with that weapon."),
        ChoiceOption("great_weapon", "Great Weapon Fighting", "When you hit with a weapon that has the Two-Handed property, you can treat any roll of 1 or 2 on a damage die as a 3. The weapon must be a Melee weapon to gain this benefit."),
        ChoiceOption("interception", "Interception", "When a creature you can see hits a target, other than you, within 5 feet of you with an attack, you can take a Reaction to reduce the damage the target takes by 1d10 plus your Proficiency Bonus. You must be wielding a Shield or a Simple or Martial weapon to use this Reaction."),
        ChoiceOption("protection", "Protection", "When a creature you can see attacks a target other than you that is within 5 feet of you, you can take a Reaction to interpose your Shield if you're holding one. You impose Disadvantage on the triggering attack roll."),
        ChoiceOption("thrown_weapon", "Thrown Weapon Fighting", "When you hit with a Ranged attack using a weapon that has the Thrown property, you gain a +2 bonus to the damage roll."),
        ChoiceOption("two_weapon", "Two-Weapon Fighting", "When you make an extra attack as a result of the Light property, you can add your ability modifier to the damage of that extra attack."),
        // D&D Beyond Drops, July 2026
        ChoiceOption("pack_fighting", "Pack Fighting", "When you make a melee attack with a weapon or an Unarmed Strike against a creature, you gain a +1 bonus to the damage roll if at least one of your allies is within 5 feet of the creature and that ally isn't Incapacitated."),
        ChoiceOption("prone_fighting", "Prone Fighting", "While you have the Prone condition, you don't have Disadvantage due to it on attack rolls, and it doesn't grant Advantage on attack rolls made against you."),
        ChoiceOption("unarmed", "Unarmed Fighting", "Your Unarmed Strikes can deal Bludgeoning damage equal to 1d6 plus your Strength modifier on a hit. If you're not wielding any weapons or a Shield when you make the attack roll, the d6 becomes a d8. At the start of each of your turns, you can deal 1d4 Bludgeoning damage to one creature you're Grappling."),
    )

    private fun fightingStyleChoice(level: Int, id: String = "fighting_style") = Choice(
        id = id,
        label = "Fighting Style",
        prompt = "Choose a Fighting Style feat.",
        count = 1,
        kind = ChoiceKind.OPTION,
        options = FIGHTING_STYLE_OPTIONS,
        source = "Level $level",
    )

    /**
     * Which weapons the character has mastery with. The list of options and the counts live
     * in [MasteryData], so a class's table and the property text stay in one place.
     */
    private fun masteryChoice(classId: String, count: Int, level: Int) =
        MasteryData.choiceFor(classId, count, level)

    /** The feature text for a level where the mastery count goes up. */
    private fun masteryGrowth(classId: String, level: Int, count: Int) = feature(
        level,
        "Weapon Mastery",
        "The number of weapons you have mastery with increases to $count.",
        masteryChoice(classId, count, level),
    )

    /**
     * "Choose N of your *skill proficiencies*."
     *
     * Expertise doubles a proficiency you already have; it does not hand out a new skill. All
     * six of these offered every skill in the game, so a Rogue could double a bonus they did
     * not have. [options] holds the full list only as a fallback for a caller with no
     * character; the creation flow's own picker has always filtered correctly.
     */
    private fun expertiseChoice(level: Int, id: String, count: Int = 2) = Choice(
        id = id,
        label = "Expertise",
        prompt = "Choose $count of your skill proficiencies to double your proficiency " +
            "bonus with.",
        count = count,
        kind = ChoiceKind.EXPERTISE,
        options = ChoiceOptions.fromSkills(Skill.ALL),
        source = "Level $level",
        optionsFrom = OptionSource.ProficientSkills,
    )

    val ALL: List<ClassProgression> = listOf(

        // ------------------------------------------------------------------ Barbarian
        ClassProgression(
            classId = "barbarian",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Barbarian Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Rage", "You can imbue yourself with a primal power called Rage, a force that grants you extraordinary " +
                    "might and resilience. You can enter it as a Bonus Action if you aren't wearing Heavy armor. You " +
                    "can enter your Rage the number of times shown for your Barbarian level in the Rages column of " +
                    "the Barbarian Features table. You regain one expended use when you finish a Short Rest, and you " +
                    "regain all expended uses when you finish a Long Rest. While active, your Rage follows the rules " +
                    "below. Damage Resistance. You have Resistance to Bludgeoning, Piercing, and Slashing damage. " +
                    "Rage Damage. When you make an attack using Strength—with either a weapon or an Unarmed " +
                    "Strike—and deal damage to the target, you gain a bonus to the damage that increases as you gain " +
                    "levels as a Barbarian, as shown in the Rage Damage column of the Barbarian Features table. " +
                    "Strength Advantage. You have Advantage on Strength checks and Strength saving throws. No " +
                    "Concentration or Spells. You can't maintain Concentration, and you can't cast spells. Duration. " +
                    "The Rage lasts until the end of your next turn, and it ends early if you don Heavy armor or have " +
                    "the Incapacitated condition. If your Rage is still active on your next turn, you can extend the " +
                    "Rage for another round by doing one of the following: Make an attack roll against an enemy. " +
                    "Force an enemy to make a saving throw. Take a Bonus Action to extend your Rage. Each time the " +
                    "Rage is extended, it lasts until the end of your next turn. You can maintain a Rage for up to 10 " +
                    "minutes."),
                feature(1, "Unarmored Defense", "While you aren't wearing any armor, your base Armor Class equals 10 plus your Dexterity and " +
                    "Constitution modifiers. You can use a Shield and still gain this benefit."),
                feature(1, "Weapon Mastery", "Your training with weapons allows you to use the mastery properties of two kinds of weapons of " +
                    "your choice with which you have proficiency, such as Daggers and Shortbows. Whenever you finish " +
                    "a Long Rest, you can change the kinds of weapons you chose. For example, you could switch to " +
                    "using the mastery properties of Scimitars and Shortswords.",
                    masteryChoice("barbarian", 2, 1)),
                masteryGrowth("barbarian", 4, 3),
                masteryGrowth("barbarian", 10, 4),
                feature(2, "Danger Sense", "You gain an uncanny sense of when things aren't as they should be, giving you an edge when you " +
                    "dodge perils. You have Advantage on Dexterity saving throws unless you have the Incapacitated " +
                    "condition."),
                feature(2, "Reckless Attack", "You can throw aside all concern for defense to attack with increased ferocity. When you make " +
                    "your first attack roll on your turn, you can decide to attack recklessly. Doing so gives you " +
                    "Advantage on attack rolls using Strength until the start of your next turn, but attack rolls " +
                    "against you have Advantage during that time."),
                feature(3, "Primal Knowledge", "You gain proficiency in another skill of your choice from the skill list available to Barbarians " +
                    "at level 1. In addition, while your Rage is active, you can channel primal power when you " +
                    "attempt certain tasks; whenever you make an ability check using one of the following skills, you " +
                    "can make it as a Strength check even if it normally uses a different ability: Acrobatics, " +
                    "Intimidation, Perception, Stealth, or Survival. When you use this ability, your Strength " +
                    "represents primal power coursing through you, honing your agility, bearing, and senses.",
                    Choice("primal_knowledge", "Primal Knowledge", "Choose one more Barbarian skill proficiency.", 1, ChoiceKind.SKILL,
                        ChoiceOptions.fromSkills(listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL)), "Level 3")),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Fast Movement", "Your Speed increases by 10 feet while you aren't wearing Heavy armor."),
                feature(7, "Feral Instinct", "Your instincts are so honed that you have Advantage on Initiative rolls."),
                feature(7, "Instinctive Pounce", "When you enter your Rage, you can move up to half your Speed as part of that Bonus Action."),
                feature(9, "Brutal Strike", "If you use Reckless Attack, you can forgo any Advantage on one Strength-based attack roll of " +
                    "your choice on your turn. The chosen attack roll mustn't have Disadvantage. If the chosen attack " +
                    "roll hits, the target takes an extra 1d10 damage of the same type dealt by the weapon or Unarmed " +
                    "Strike, and you can cause one Brutal Strike effect of your choice. You have the following effect " +
                    "options. Forceful Blow. The target is pushed 15 feet straight away from you. You can then move " +
                    "up to half your Speed straight toward the target without provoking Opportunity Attacks. " +
                    "Hamstring Blow. The target's Speed is reduced by 15 feet until the start of your next turn. A " +
                    "target can be affected by only one Hamstring Blow at a time— the most recent one."),
                feature(11, "Relentless Rage", "Your Rage can keep you fighting despite grievous wounds. If you drop to 0 Hit Points while your " +
                    "Rage is active and don't die outright, you can make a DC 10 Constitution saving throw. If you " +
                    "succeed, your Hit Points instead change to a number equal to twice your Barbarian level. Each " +
                    "time you use this feature after the first, the DC increases by 5. When you finish a Short or " +
                    "Long Rest, the DC resets to 10."),
                feature(13, "Improved Brutal Strike", "You have honed new ways to attack furiously. The following effects are now among your Brutal " +
                    "Strike options. Staggering Blow. The target has Disadvantage on the next saving throw it makes, " +
                    "and it can't make Opportunity Attacks until the start of your next turn. Sundering Blow. Before " +
                    "the start of your next turn, the next attack roll made by another creature against the target " +
                    "gains a +5 bonus to the roll. An attack roll can gain only one Sundering Blow bonus."),
                feature(15, "Persistent Rage", "When you roll Initiative, you can regain all expended uses of Rage. After you regain uses of " +
                    "Rage in this way, you can't do so again until you finish a Long Rest. In addition, your Rage is " +
                    "so fierce that it now lasts for 10 minutes without you needing to do anything to extend it from " +
                    "round to round. Your Rage ends early if you have the Unconscious condition (not just the " +
                    "Incapacitated condition) or don Heavy armor."),
                feature(17, "Improved Brutal Strike", "The extra damage of your Brutal Strike increases to 2d10. In addition, you can use two different " +
                    "Brutal Strike effects whenever you use your Brutal Strike feature."),
                feature(18, "Indomitable Might", "If your total for a Strength check or Strength saving throw is less than your Strength score, " +
                    "you can use that score in place of the total."),
                feature(20, "Primal Champion", "Your Strength and Constitution scores increase by 4, to a maximum of 25."),
            ),
        ),

        // ------------------------------------------------------------------ Bard
        ClassProgression(
            classId = "bard",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Bard Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Bardic Inspiration", "You can supernaturally inspire others through words, music, or dance. This inspiration is " +
                    "represented by your Bardic Inspiration die, which is a d6. Using Bardic Inspiration. As a Bonus " +
                    "Action, you can inspire another creature within 60 feet of yourself who can see or hear you. " +
                    "That creature gains one of your Bardic Inspiration dice. A creature can have only one Bardic " +
                    "Inspiration die at a time. Once within the next hour when the creature fails a D20 Test, the " +
                    "creature can roll the Bardic Inspiration die and add the number rolled to the d20, potentially " +
                    "turning the failure into a success. A Bardic Inspiration die is expended when it's rolled. " +
                    "Number of Uses. You can confer a Bardic Inspiration die a number of times equal to your Charisma " +
                    "modifier (minimum of once), and you regain all expended uses when you finish a Long Rest. At " +
                    "Higher Levels. Your Bardic Inspiration die changes when you reach certain Bard levels, as shown " +
                    "in the Bardic Die column of the Bard Features table. The die becomes a d8 at level 5, a d10 at " +
                    "level 10, and a d12 at level 15."),
                feature(1, "Spellcasting", "Drawing from your innate magic, you can cast spells. See chapter 7 for the rules on " +
                    "spellcasting. The information below details how you use those rules with Sorcerer spells, which " +
                    "appear in the Sorcerer spell list later in the class's description. Cantrips. You know four " +
                    "Sorcerer cantrips of your choice. Light, Prestidigitation, Shocking Grasp, and Sorcerous Burst " +
                    "are recommended. Whenever you gain a Sorcerer level, you can replace one of your cantrips from " +
                    "this feature with another Sorcerer cantrip of your choice. When you reach Sorcerer levels 4 and " +
                    "10, you learn another Sorcerer cantrip of your choice, as shown in the Cantrips column of the " +
                    "Sorcerer Features table. Spell Slots. The Sorcerer Features table shows how many spell slots you " +
                    "have to cast your level 1+ spells. You regain all expended slots when you finish a Long Rest. " +
                    "Prepared Spells of Level 1+. You prepare the list of level 1+ spells that are available for you " +
                    "to cast with this feature. To start, choose two level 1 Sorcerer spells. Burning Hands and " +
                    "Detect Magic are recommended. The number of spells on your list increases as you gain Sorcerer " +
                    "levels, as shown in the Prepared Spells column of the Sorcerer Features table. Whenever that " +
                    "number increases, choose additional Sorcerer spells until the number of spells on your list " +
                    "matches the number in the Sorcerer Features table. The chosen spells must be of a level for " +
                    "which you have spell slots. For example, if you're a level 3 Sorcerer, your list of prepared " +
                    "spells can include six Sorcerer spells of level 1 or 2 in any combination. If another Sorcerer " +
                    "feature gives you spells that you always have prepared, those spells don't count against the " +
                    "number of spells you can prepare with this feature, but those spells otherwise count as Sorcerer " +
                    "spells for you. Changing Your Prepared Spells. Whenever you gain a Sorcerer level, you can " +
                    "replace one spell on your list with another Sorcerer spell for which you have spell slots. " +
                    "Spellcasting Ability. Charisma is your spellcasting ability for your Sorcerer spells. " +
                    "Spellcasting Focus. You can use an Arcane Focus as a Spellcasting Focus for your Sorcerer " +
                    "spells."),
                feature(2, "Expertise", "You gain Expertise (see the rules glossary) in two of your skill proficiencies of your choice. " +
                    "Performance and Persuasion are recommended if you have proficiency in them. At Bard level 9, you " +
                    "gain Expertise in two more of your skill proficiencies of your choice.",
                    expertiseChoice(2, "bard_expertise_2")),
                feature(2, "Jack of All Trades", "You can add half your Proficiency Bonus (round down) to any ability check you make that uses a " +
                    "skill proficiency you lack and that doesn't otherwise use your Proficiency Bonus. For example, " +
                    "if you make a Strength (Athletics) check and lack Athletics proficiency, you can add half your " +
                    "Proficiency Bonus to the check."),
                feature(5, "Font of Inspiration", "You now regain all your expended uses of Bardic Inspiration when you finish a Short or Long " +
                    "Rest. In addition, you can expend a spell slot (no action required) to regain one expended use " +
                    "of Bardic Inspiration."),
                feature(7, "Countercharm", "You can use musical notes or words of power to disrupt mind-influencing effects. If you or a " +
                    "creature within 30 feet of you fails a saving throw against an effect that applies the Charmed " +
                    "or Frightened condition, you can take a Reaction to cause the save to be rerolled, and the new " +
                    "roll has Advantage."),
                feature(9, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(9, "bard_expertise_9")),
                feature(10, "Magical Secrets", "You've learned secrets from various magical traditions. Whenever you reach a Bard level " +
                    "(including this level) and the Prepared Spells number in the Bard Features table increases, you " +
                    "can choose any of your new prepared spells from the Bard, Cleric, Druid, and Wizard spell lists, " +
                    "and the chosen spells count as Bard spells for you (see a class's section for its spell list). " +
                    "In addition, whenever you replace a spell prepared for this class, you can replace it with a " +
                    "spell from those lists."),
                feature(18, "Superior Inspiration", "When you roll Initiative, you regain expended uses of Bardic Inspiration until you have two."),
                feature(20, "Words of Creation", "You have mastered two of the Words of Creation: the words of life and death. You therefore " +
                    "always have the Power Word: Heal and Power Word: Kill spells prepared. When you cast either " +
                    "spell, you can target a second creature with it if that creature is within 10 feet of the first " +
                    "target."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Cleric
        ClassProgression(
            classId = "cleric",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Cleric Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Cleric spells using Wisdom, preparing them from the whole Cleric spell list."),
                feature(1, "Divine Order", "You have dedicated yourself to one of the following sacred roles of your choice. Protector. " +
                    "Trained for battle, you gain proficiency with Martial weapons and training with Heavy armor. " +
                    "Thaumaturge. You know one extra cantrip from the Cleric spell list. In addition, your mystical " +
                    "connection to the divine gives you a bonus to your Intelligence (Arcana or Religion) checks. The " +
                    "bonus equals your Wisdom modifier (minimum of +1).",
                    Choice("divine_order", "Divine Order", "Choose your Divine Order.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("protector", "Protector", "Gain training with Martial weapons and Heavy armor."),
                        ChoiceOption("thaumaturge", "Thaumaturge", "Learn an extra Cleric cantrip and add your Wisdom modifier to Arcana and Religion checks."),
                    ), "Level 1")),
                feature(2, "Channel Divinity", "You can channel divine energy directly from the Outer Planes to fuel magical effects. You start " +
                    "with two such effects: Divine Spark and Turn Undead, each of which is described below. Each time " +
                    "you use this class's Channel Divinity, choose which Channel Divinity effect from this class to " +
                    "create. You gain additional effect options at higher Cleric levels. You can use this class's " +
                    "Channel Divinity twice. You regain one of its expended uses when you finish a Short Rest, and " +
                    "you regain all expended uses when you finish a Long Rest. You gain additional uses when you " +
                    "reach certain Cleric levels, as shown in the Channel Divinity column of the Cleric Features " +
                    "table. If a Channel Divinity effect requires a saving throw, the DC equals the spell save DC " +
                    "from this class's Spellcasting feature. Divine Spark. As a Magic action, you point your Holy " +
                    "Symbol at another creature you can see within 30 feet of yourself and focus divine energy at it. " +
                    "Roll 1d8 and add your Wisdom modifier. You either restore Hit Points to the creature equal to " +
                    "that total or force the creature to make a Constitution saving throw. On a failed save, the " +
                    "creature takes Necrotic or Radiant damage (your choice) equal to that total. On a successful " +
                    "save, the creature takes half as much damage (round down). You roll an additional d8 when you " +
                    "reach Cleric levels 7 (2d8), 13 (3d8), and 18 (4d8). Turn Undead. As a Magic action, you present " +
                    "your Holy Symbol and censure Undead creatures. Each Undead of your choice within 30 feet of you " +
                    "must make a Wisdom saving throw. If the creature fails its save, it has the Frightened and " +
                    "Incapacitated conditions for 1 minute. For that duration, it tries to move as far from you as it " +
                    "can on its turns. This effect ends early on the creature if it takes any damage, if you have the " +
                    "Incapacitated condition, or if you die."),
                feature(5, "Sear Undead", "Whenever you use Turn Undead, you can roll a number of d8s equal to your Wisdom modifier " +
                    "(minimum of 1d8) and add the rolls together. Each Undead that fails its saving throw against " +
                    "that use of Turn Undead takes Radiant damage equal to the roll's total. This damage doesn't end " +
                    "the turn effect."),
                feature(7, "Blessed Strikes", "Divine power infuses you in battle. You gain one of the following options of your choice (if you " +
                    "get either option from a Cleric subclass in an older book, use only the option you choose for " +
                    "this feature). Divine Strike. Once on each of your turns when you hit a creature with an attack " +
                    "roll using a weapon, you can cause the target to take an extra 1d8 Necrotic or Radiant damage " +
                    "(your choice). Potent Spellcasting. Add your Wisdom modifier to the damage you deal with any " +
                    "Cleric cantrip.",
                    Choice("blessed_strikes", "Blessed Strikes", "Choose how your divine power sharpens your attacks.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("divine_strike", "Divine Strike", "Once per turn, a weapon hit deals an extra 1d8 Necrotic or Radiant damage."),
                        ChoiceOption("potent_spellcasting", "Potent Spellcasting", "Add your Wisdom modifier to the damage of your Cleric cantrips."),
                    ), "Level 7")),
                feature(10, "Divine Intervention", "You can call on your deity or pantheon to intervene on your behalf. As a Magic action, choose " +
                    "any Cleric spell of level 5 or lower that doesn't require a Reaction to cast. As part of the " +
                    "same action, you cast that spell without expending a spell slot or needing Material components. " +
                    "You can't use this feature again until you finish a Long Rest."),
                feature(14, "Improved Blessed Strikes", "The option you chose for Blessed Strikes grows more powerful. Divine Strike. The extra damage of " +
                    "your Divine Strike increases to 2d8. Potent Spellcasting. When you cast a Cleric cantrip and " +
                    "deal damage to a creature with it, you can give vitality to yourself or another creature within " +
                    "60 feet of yourself, granting a number of Temporary Hit Points equal to twice your Wisdom " +
                    "modifier."),
                feature(20, "Greater Divine Intervention", "You can call on even more powerful divine intervention. When you use your Divine Intervention " +
                    "feature, you can choose Wish when you select a spell. If you do so, you can't use Divine " +
                    "Intervention again until you finish 2d4 Long Rests."),
            ),
            cantripsKnown = mapOf(1 to 3, 4 to 4, 10 to 5),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Druid
        ClassProgression(
            classId = "druid",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Druid Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Druid spells using Wisdom, preparing them from the whole Druid spell list."),
                feature(1, "Druidic", "You know Druidic, the secret language of Druids. While learning this ancient tongue, you also " +
                    "unlocked the magic of communicating with animals; you always have the Speak with Animals spell " +
                    "prepared. You can use Druidic to leave hidden messages. You and others who know Druidic " +
                    "automatically spot such a message. Others spot the message's presence with a successful DC 15 " +
                    "Intelligence (Investigation) check but can't decipher it without magic."),
                feature(1, "Primal Order", "You have dedicated yourself to one of the following sacred roles of your choice. Magician. You " +
                    "know one extra cantrip from the Druid spell list. In addition, your mystical connection to " +
                    "nature gives you a bonus to your Intelligence (Arcana or Nature) checks. The bonus equals your " +
                    "Wisdom modifier (minimum bonus of +1). Warden. Trained for battle, you gain proficiency with " +
                    "Martial weapons and training with Medium armor.",
                    Choice("primal_order", "Primal Order", "Choose your Primal Order.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("magician", "Magician", "Learn an extra Druid cantrip and add your Wisdom modifier to Arcana and Nature checks."),
                        ChoiceOption("warden", "Warden", "Gain training with Martial weapons and Medium armor."),
                    ), "Level 1")),
                feature(2, "Wild Shape", "The power of nature allows you to assume the form of an animal. As a Bonus Action, you " +
                    "shape-shift into a Beast form that you have learned for this feature (see 'Known Forms' below). " +
                    "You stay in that form for a number of hours equal to half your Druid level or until you use Wild " +
                    "Shape again, have the Incapacitated condition, or die. You can also leave the form early as a " +
                    "Bonus Action. Number of Uses. You can use Wild Shape twice. You regain one expended use when you " +
                    "finish a Short Rest, and you regain all expended uses when you finish a Long Rest. You gain " +
                    "additional uses when you reach certain Druid levels, as shown in the Wild Shape column of the " +
                    "Druid Features table. Known Forms. You know four Beast forms for this feature, chosen from among " +
                    "Beast stat blocks that have a maximum Challenge Rating of 1/4 and that lack a Fly Speed (see " +
                    "appendix B for stat block options). The Rat, Riding Horse, Spider, and Wolf are recommended. " +
                    "Whenever you finish a Long Rest, you can replace one of your known forms with another eligible " +
                    "form. When you reach certain Druid levels, your number of known forms and the maximum Challenge " +
                    "Rating for those forms increases, as shown in the Beast Shapes table. In addition, starting at " +
                    "level 8, you can adopt a form that has a Fly Speed. When choosing known forms, you may look in " +
                    "the Monster Manual or elsewhere for eligible Beasts if the Dungeon Master permits you to do so. " +
                    "Druid Level | Known Forms | Max CR | Fly Speed | 2 | 4 | 1/4 | No | 4 | 6 | 1/2 | No | 8 | 8 | 1 " +
                    "| Yes | Rules While Shape-Shifted. While in a form, you retain your personality, memories, and " +
                    "ability to speak, and the following rules apply: Temporary Hit Points. When you assume a Wild " +
                    "Shape form, you gain a number of Temporary Hit Points equal to your Druid level. Game " +
                    "Statistics. Your game statistics are replaced by the Beast's stat block, but you retain your " +
                    "creature type; Hit Points; Hit Point Dice; Intelligence, Wisdom, and Charisma scores; class " +
                    "features; languages; and feats. You also retain your skill and saving throw proficiencies and " +
                    "use your Proficiency Bonus for them, in addition to gaining the proficiencies of the creature. " +
                    "If a skill or saving throw modifier in the Beast's stat block is higher than yours, use the one " +
                    "in the stat block. No Spellcasting. You can't cast spells, but shape-shifting doesn't break your " +
                    "Concentration or otherwise interfere with a spell you've already cast. Objects. Your ability to " +
                    "handle objects is determined by the form's limbs rather than your own. In addition, you choose " +
                    "whether your equipment falls in your space, merges into your new form, or is worn by it. Worn " +
                    "equipment functions as normal, but the DM decides whether it's practical for the new form to " +
                    "wear a piece of equipment based on the creature's size and shape. Your equipment doesn't change " +
                    "size or shape to match the new form, and any equipment that the new form can't wear must either " +
                    "fall to the ground or merge with the form. Equipment that merges with the form has no effect " +
                    "while you're in that form."),
                feature(2, "Wild Companion", "You can summon a nature spirit that assumes an animal form to aid you. As a Magic action, you " +
                    "can expend a spell slot or a use of Wild Shape to cast the Find Familiar spell without Material " +
                    "components. When you cast the spell in this way, the familiar is Fey and disappears when you " +
                    "finish a Long Rest."),
                feature(5, "Wild Resurgence", "Once on each of your turns, if you have no uses of Wild Shape left, you can give yourself one " +
                    "use by expending a spell slot (no action required). In addition, you can expend one use of Wild " +
                    "Shape (no action required) to give yourself a level 1 spell slot, but you can't do so again " +
                    "until you finish a Long Rest."),
                feature(7, "Elemental Fury", "The might of the elements flows through you. You gain one of the following options of your " +
                    "choice. Potent Spellcasting. Add your Wisdom modifier to the damage you deal with any Druid " +
                    "cantrip. Primal Strike. Once on each of your turns when you hit a creature with an attack roll " +
                    "using a weapon or a Beast form's attack in Wild Shape, you can cause the target to take an extra " +
                    "1d8 Cold, Fire, Lightning, or Thunder damage (choose when you hit).",
                    Choice("elemental_fury", "Elemental Fury", "Choose how your primal power expresses itself.", 1, ChoiceKind.OPTION, listOf(
                        ChoiceOption("potent_spellcasting", "Potent Spellcasting", "Add your Wisdom modifier to the damage of your Druid cantrips."),
                        ChoiceOption("primal_strike", "Primal Strike", "Once per turn, your attacks deal an extra 1d8 elemental damage."),
                    ), "Level 7")),
                feature(15, "Improved Elemental Fury", "The option you chose for Elemental Fury grows more powerful, as detailed below. Potent " +
                    "Spellcasting. When you cast a Druid cantrip with a range of 10 feet or greater, the spell's " +
                    "range increases by 300 feet. Primal Strike. The extra damage of your Primal Strike increases to " +
                    "2d8."),
                feature(18, "Beast Spells", "While using Wild Shape, you can cast spells in Beast form, except for any spell that has a " +
                    "Material component with a cost specified or that consumes its Material component."),
                feature(20, "Archdruid", "The vitality of nature constantly blooms within you, granting you the following benefits. " +
                    "Evergreen Wild Shape. Whenever you roll Initiative and have no uses of Wild Shape left, you " +
                    "regain one expended use of it. Nature Magician. You can convert uses of Wild Shape into a spell " +
                    "slot (no action required). Choose a number of your unexpended uses of Wild Shape and convert " +
                    "them into a single spell slot, with each use contributing 2 spell levels. For example, if you " +
                    "convert two uses of Wild Shape, you produce a level 4 spell slot. Once you use this benefit, you " +
                    "can't do so again until you finish a Long Rest. Longevity. The primal magic that you wield " +
                    "causes you to age more slowly. For every ten years that pass, your body ages only one year."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = FULL_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Fighter
        ClassProgression(
            classId = "fighter",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Fighter Subclass",
            asiLevels = setOf(4, 6, 8, 12, 14, 16),
            features = listOf(
                feature(1, "Fighting Style", "You gain a Fighting Style feat of your choice (see chapter 5). Defense is recommended. Whenever " +
                    "you gain a Fighter level, you can replace the feat you chose with a different Fighting Style " +
                    "feat.",
                    fightingStyleChoice(1)),
                feature(1, "Second Wind", "You have a limited well of physical and mental stamina that you can draw on. As a Bonus Action, " +
                    "you can use it to regain Hit Points equal to 1d10 plus your Fighter level. You can use this " +
                    "feature twice. You regain one expended use when you finish a Short Rest, and you regain all " +
                    "expended uses when you finish a Long Rest. When you reach certain Fighter levels, you gain more " +
                    "uses of this feature, as shown in the Second Wind column of the Fighter Features table."),
                feature(1, "Weapon Mastery", "You can use the mastery property of three kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("fighter", 3, 1)),
                masteryGrowth("fighter", 4, 4),
                masteryGrowth("fighter", 10, 5),
                masteryGrowth("fighter", 16, 6),
                feature(2, "Action Surge", "You can push yourself beyond your normal limits for a moment. On your turn, you can take one " +
                    "additional action, except the Magic action. Once you use this feature, you can't do so again " +
                    "until you finish a Short or Long Rest. Starting at level 17, you can use it twice before a rest " +
                    "but only once on a turn."),
                feature(2, "Tactical Mind", "You have a mind for tactics on and off the battlefield. When you fail an ability check, you can " +
                    "expend a use of your Second Wind to push yourself toward success. Rather than regaining Hit " +
                    "Points, you roll 1d10 and add the number rolled to the ability check, potentially turning it " +
                    "into a success. If the check still fails, this use of Second Wind isn't expended."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Tactical Shift", "When you activate Second Wind, you can move up to half your Speed without provoking Opportunity Attacks."),
                feature(9, "Indomitable", "If you fail a saving throw, you can reroll it with a bonus equal to your Fighter level. You must " +
                    "use the new roll, and you can't use this feature again until you finish a Long Rest. You can use " +
                    "this feature twice before a Long Rest starting at level 13 and three times before a Long Rest " +
                    "starting at level 17."),
                feature(9, "Tactical Master", "When you attack with a weapon whose mastery property you can use, you can replace that property " +
                    "with the Push, Sap, or Slow property for that attack."),
                feature(11, "Two Extra Attacks", "You can attack three times instead of once whenever you take the Attack action on your turn."),
                feature(13, "Indomitable (two uses)", "You can use Indomitable twice per Long Rest."),
                feature(13, "Studied Attacks", "You study your opponents and learn from each attack you make. If you make an attack roll against " +
                    "a creature and miss, you have Advantage on your next attack roll against that creature before " +
                    "the end of your next turn."),
                feature(17, "Action Surge (two uses)", "You can use Action Surge twice per rest, but only once per turn."),
                feature(17, "Indomitable (three uses)", "You can use Indomitable three times per Long Rest."),
                feature(20, "Three Extra Attacks", "You can attack four times instead of once whenever you take the Attack action on your turn."),
            ),
        ),

        // ------------------------------------------------------------------ Monk
        ClassProgression(
            classId = "monk",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Monk Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Martial Arts", "Your practice of martial arts gives you mastery of combat styles that use your Unarmed Strike " +
                    "and Monk weapons, which are the following: Simple Melee weapons Martial Melee weapons that have " +
                    "the Light property You gain the following benefits while you are unarmed or wielding only Monk " +
                    "weapons and you aren't wearing armor or wielding a Shield. Bonus Unarmed Strike. You can make an " +
                    "Unarmed Strike as a Bonus Action. Martial Arts Die. You can roll 1d6 in place of the normal " +
                    "damage of your Unarmed Strike or Monk weapons. This die changes as you gain Monk levels, as " +
                    "shown in the Martial Arts column of the Monk Features table. Dexterous Attacks. You can use your " +
                    "Dexterity modifier instead of your Strength modifier for the attack and damage rolls of your " +
                    "Unarmed Strikes and Monk weapons. In addition, when you use the Grapple or Shove option of your " +
                    "Unarmed Strike, you can use your Dexterity modifier instead of your Strength modifier to " +
                    "determine the save DC."),
                feature(1, "Unarmored Defense", "While wearing no armor or shield, your AC equals 10 + your Dexterity modifier + your Wisdom modifier."),
                feature(2, "Monk's Focus", "Your focus and martial training allow you to harness a well of extraordinary energy within " +
                    "yourself. This energy is represented by Focus Points. Your Monk level determines the number of " +
                    "points you have, as shown in the Focus Points column of the Monk Features table. You can expend " +
                    "these points to enhance or fuel certain Monk features. You start knowing three such features: " +
                    "Flurry of Blows, Patient Defense, and Step of the Wind, each of which is detailed below. When " +
                    "you expend a Focus Point, it is unavailable until you finish a Short or Long Rest, at the end of " +
                    "which you regain all your expended points. Some features that use Focus Points require your " +
                    "target to make a saving throw. The save DC equals 8 plus your Wisdom modifier and Proficiency " +
                    "Bonus. Flurry of Blows. You can expend 1 Focus Point to make two Unarmed Strikes as a Bonus " +
                    "Action. Patient Defense. You can take the Disengage action as a Bonus Action. Alternatively, you " +
                    "can expend 1 Focus Point to take both the Disengage and the Dodge actions as a Bonus Action. " +
                    "Step of the Wind. You can take the Dash action as a Bonus Action. Alternatively, you can expend " +
                    "1 Focus Point to take both the Disengage and Dash actions as a Bonus Action, and your jump " +
                    "distance is doubled for the turn."),
                feature(2, "Unarmored Movement", "Your speed increases by 10 feet while you aren't wearing armor or wielding a Shield. This bonus " +
                    "increases when you reach certain Monk levels, as shown on the Monk Features table."),
                feature(2, "Uncanny Metabolism", "When you roll Initiative, you can regain all expended Focus Points. When you do so, roll your " +
                    "Martial Arts die, and regain a number of Hit Points equal to your Monk level plus the number " +
                    "rolled. Once you use this feature, you can't use it again until you finish a Long Rest."),
                feature(3, "Deflect Attacks", "When an attack roll hits you and its damage includes Bludgeoning, Piercing, or Slashing damage, " +
                    "you can take a Reaction to reduce the attack's total damage against you. The reduction equals " +
                    "1d10 plus your Dexterity modifier and Monk level. If you reduce the damage to 0, you can expend " +
                    "1 Focus Point to redirect some of the attack's force. If you do so, choose a creature you can " +
                    "see within 5 feet of yourself if the attack was a melee attack or a creature you can see within " +
                    "60 feet of yourself that isn't behind Total Cover if the attack was a ranged attack. That " +
                    "creature must succeed on a Dexterity saving throw or take damage equal to two rolls of your " +
                    "Martial Arts die plus your Dexterity modifier. The damage is the same type dealt by the attack."),
                feature(4, "Slow Fall", "You can take a Reaction when you fall to reduce any damage you take from the fall by an amount " +
                    "equal to five times your Monk level."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Stunning Strike", "Once per turn when you hit a creature with a Monk weapon or an Unarmed Strike, you can expend 1 " +
                    "Focus Point to attempt a stunning strike. The target must make a Constitution saving throw. On a " +
                    "failed save, the target has the Stunned condition until the start of your next turn. On a " +
                    "successful save, the target's Speed is halved until the start of your next turn, and the next " +
                    "attack roll made against the target before then has Advantage."),
                feature(6, "Empowered Strikes", "Whenever you deal damage with your Unarmed Strike, it can deal your choice of Force damage or " +
                    "its normal damage type."),
                feature(7, "Evasion", "You can nimbly dodge out of the way of certain dangers. When you're subjected to an effect that " +
                    "allows you to make a Dexterity saving throw to take only half damage, you instead take no damage " +
                    "if you succeed on the saving throw and only half damage if you fail. You can't use this feature " +
                    "if you have the Incapacitated condition."),
                feature(9, "Acrobatic Movement", "While you aren't wearing armor or wielding a Shield, you gain the ability to move along vertical " +
                    "surfaces and across liquids on your turn without falling during the movement."),
                feature(10, "Heightened Focus", "Your Flurry of Blows, Patient Defense, and Step of the Wind gain the following benefits. Flurry " +
                    "of Blows. You can expend 1 Focus Point to use Flurry of Blows and make three Unarmed Strikes " +
                    "with it instead of two. Patient Defense. When you expend a Focus Point to use Patient Defense, " +
                    "you gain a number of Temporary Hit Points equal to two rolls of your Martial Arts die. Step of " +
                    "the Wind. When you expend a Focus Point to use Step of the Wind, you can choose a willing " +
                    "creature within 5 feet of yourself that is Large or smaller. You move the creature with you " +
                    "until the end of your turn. The creature's movement doesn't provoke Opportunity Attacks."),
                feature(10, "Self-Restoration", "Through sheer force of will, you can remove one of the following conditions from yourself at the " +
                    "end of each of your turns: Charmed, Frightened, or Poisoned. In addition, forgoing food and " +
                    "drink doesn't give you levels of Exhaustion."),
                feature(13, "Deflect Energy", "You can now use your Deflect Attacks feature against attacks that deal any damage type, not just " +
                    "Bludgeoning, Piercing, or Slashing."),
                feature(14, "Disciplined Survivor", "Your physical and mental discipline grant you proficiency in all saving throws. Additionally, " +
                    "whenever you make a saving throw and fail, you can expend 1 Focus Point to reroll it, and you " +
                    "must use the new roll."),
                feature(15, "Perfect Focus", "When you roll Initiative and don't use Uncanny Metabolism, you regain expended Focus Points " +
                    "until you have 4 if you have 3 or fewer."),
                feature(18, "Superior Defense", "At the start of your turn, you can expend 3 Focus Points to bolster yourself against harm for 1 " +
                    "minute or until you have the Incapacitated condition. During that time, you have Resistance to " +
                    "all damage except Force damage."),
                feature(20, "Body and Mind", "You have developed your body and mind to new heights. Your Dexterity and Wisdom scores increase " +
                    "by 4, to a maximum of 25."),
            ),
        ),

        // ------------------------------------------------------------------ Paladin
        ClassProgression(
            classId = "paladin",
            casterType = CasterType.HALF,
            subclassLevel = 3,
            subclassLabel = "Paladin Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Lay On Hands", "Your blessed touch can heal wounds. You have a pool of healing power that replenishes when you " +
                    "finish a Long Rest. With that pool, you can restore a total number of Hit Points equal to five " +
                    "times your Paladin level. As a Bonus Action, you can touch a creature (which could be yourself) " +
                    "and draw power from the pool of healing to restore a number of Hit Points to that creature, up " +
                    "to the maximum amount remaining in the pool. You can also expend 5 Hit Points from the pool of " +
                    "healing power to remove the Poisoned condition from the creature; those points don't also " +
                    "restore Hit Points to the creature."),
                feature(1, "Spellcasting", "You cast Paladin spells using Charisma, preparing them from the Paladin spell list."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("paladin", 2, 1)),
                masteryGrowth("paladin", 9, 3),
                feature(2, "Fighting Style", "You gain a Fighting Style feat of your choice (see chapter 5). Instead of choosing one of those " +
                    "feats, you can choose the option below. Druidic Warrior. You learn two Druid cantrips of your " +
                    "choice (See the Druid class's section for a list of Druid spells). Guidance and Starry Wisp are " +
                    "recommended. The chosen cantrips count as Ranger spells for you, and Wisdom is your spellcasting " +
                    "ability for them. Whenever you gain a Ranger level, you can replace one of these cantrips with " +
                    "another Druid cantrip.",
                    fightingStyleChoice(2)),
                feature(2, "Paladin's Smite", "You always have the Divine Smite spell prepared. You can cast it without expending a spell slot, " +
                    "but you must finish a Long Rest before you can cast it this way again."),
                feature(3, "Channel Divinity", "You can channel divine energy directly from the Outer Planes, using it to fuel magical effects. " +
                    "You start with one such effect: Divine Sense, which is described below. Other Paladin features " +
                    "give additional Channel Divinity effect options. Each time you use this class's Channel " +
                    "Divinity, you choose which effect from this class to create. You can use this class's Channel " +
                    "Divinity twice. You regain one of its expended uses when you finish a Short Rest, and you regain " +
                    "all expended uses when you finish a Long Rest. You gain an additional use when you reach Paladin " +
                    "level 11. If a Channel Divinity effect requires a saving throw, the DC equals the spell save DC " +
                    "from this class's Spellcasting Feature. Divine Sense. As a Bonus Action, you can open your " +
                    "awareness to detect Celestials, Fiends, and Undead. For the next 10 minutes or until you have " +
                    "the Incapacitated condition, you know the location of any creature of those types within 60 feet " +
                    "of yourself, and you know its creature type. Within the same radius, you also detect the " +
                    "presence of any place or object that has been consecrated or desecrated, as with the Hallow " +
                    "spell."),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(5, "Faithful Steed", "You can call on the aid of an otherworldly steed. You always have the Find Steed spell prepared. " +
                    "You can also cast the spell once without expending a spell slot, and you regain your ability to " +
                    "do so when you finish a Long Rest."),
                feature(6, "Aura of Protection", "You radiate a protective, unseeable aura in a 10-foot Emanation that originates from you. The " +
                    "aura is inactive while you have the Incapacitated condition. You and your allies in the aura " +
                    "gain a bonus to saving throws equal to your Charisma modifier (minimum bonus of +1). If another " +
                    "Paladin is present, a creature can benefit from only one Aura of Protection at a time; the " +
                    "creature chooses which aura while in them."),
                feature(9, "Abjure Foes", "As a Magic action, you can expend one use of this class's Channel Divinity to overwhelm foes " +
                    "with awe. As you present your Holy Symbol or weapon, you can target a number of creatures equal " +
                    "to your Charisma modifier (minimum of one creature) that you can see within 60 feet of yourself. " +
                    "Each target must succeed on a Wisdom saving throw or have the Frightened condition for 1 minute " +
                    "or until it takes any damage. While Frightened in this way, a target can only do one of the " +
                    "following on its turns: move, take an action or take a Bonus Action."),
                feature(10, "Aura of Courage", "You and your allies have Immunity to the Frightened condition while in your Aura of Protection. " +
                    "If a Frightened ally enters the aura, that condition has no effect on that ally while there."),
                feature(11, "Radiant Strikes", "Your strikes now carry supernatural power. When you hit a target with an attack roll using a " +
                    "Melee weapon or an Unarmed Strike, the target takes an extra 1d8 Radiant damage."),
                feature(14, "Restoring Touch", "When you use Lay On Hands on a creature, you can also remove one or more of the following " +
                    "conditions from the creature: Blinded, Charmed, Deafened, Frightened, Paralyzed, or Stunned. You " +
                    "must expend 5 Hit Points from the healing pool of Lay On Hands for each of these conditions you " +
                    "remove; those points don't also restore Hit Points to the creature."),
                feature(18, "Aura Expansion", "Your Aura of Protection is now a 30-foot Emanation."),
            ),
            cantripsKnown = emptyMap(),
            preparedSpells = HALF_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Ranger
        ClassProgression(
            classId = "ranger",
            casterType = CasterType.HALF,
            subclassLevel = 3,
            subclassLabel = "Ranger Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Ranger spells using Wisdom, preparing them from the Ranger spell list."),
                feature(1, "Favored Enemy", "You always have the Hunter's Mark spell prepared. You can cast it twice without expending a " +
                    "spell slot, and you regain all expended uses of this ability when you finish a Long Rest. The " +
                    "number of times you can cast the spell without a spell slot increases when you reach certain " +
                    "Ranger Levels, as shown in the Favored Enemy column of the Ranger Features table."),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("ranger", 2, 1)),
                masteryGrowth("ranger", 9, 3),
                feature(2, "Deft Explorer", "Thanks to your travels, you gain the following benefits. Expertise.: Choose one of your skill " +
                    "proficiencies with which you lack Expertise. You gain Expertise in that skill. Languages.: You " +
                    "know two languages of your choice from the language tables in chapter 2.",
                    expertiseChoice(2, "ranger_expertise_2", count = 1)),
                feature(2, "Fighting Style", "You gain a Fighting Style feat of your choice.",
                    fightingStyleChoice(2)),
                feature(5, "Extra Attack", "You can attack twice instead of once whenever you take the Attack action."),
                feature(6, "Roving", "Your speed increases by 10 feet while you aren't wearing Heavy Armor. You also have a Climb " +
                    "speed and a Swim Speed equal to your Speed."),
                feature(9, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(9, "ranger_expertise_9")),
                feature(10, "Tireless", "Primal forces now help fuel you on your journeys, granting you the following benefits. Temporary " +
                    "Hit Points. As a Magic Action, you can give yourself a number of Temporary Hit Points equal to " +
                    "1d8 plus your Wisdom modifier (minimum of 1). You can use this action a number of times equal to " +
                    "your Wisdom modifier (minimum of once), and you regain all expended uses when you finish a Long " +
                    "Rest. Decrease Exhaustion. Whenever you finish a Short Rest, your Exhaustion level, if any, " +
                    "decreases by 1."),
                feature(13, "Relentless Hunter", "Taking damage can't break your Concentration on Hunter's Mark."),
                feature(14, "Nature's Veil", "You invoke spirits of nature to magically hide yourself. As a Bonus Action you can give yourself " +
                    "the Invisible condition until the end of your next turn. You can use this feature a number of " +
                    "times equal to your Wisdom modifier (minimum of once), and you regain all expended uses when you " +
                    "finish a Long Rest."),
                feature(17, "Precise Hunter", "You have Advantage on attack rolls against the creature marked by your Hunter's Mark."),
                feature(18, "Feral Senses", "Your connection to the forces of nature grants you Blindsight with a range of 30 feet."),
                feature(20, "Foe Slayer", "The damage die of your Hunter's Mark is a d10 rather than a d6."),
            ),
            preparedSpells = HALF_CASTER_PREPARED,
        ),

        // ------------------------------------------------------------------ Rogue
        ClassProgression(
            classId = "rogue",
            casterType = CasterType.NONE,
            subclassLevel = 3,
            subclassLabel = "Rogue Subclass",
            asiLevels = setOf(4, 8, 10, 12, 16),
            features = listOf(
                feature(1, "Expertise", "You gain Expertise in two of your skill proficiencies of your choice. Sleight of Hand and " +
                    "Stealth are recommended if you have proficiency in them. At Rogue Level 6, you gain Expertise in " +
                    "two more of your skill proficiencies of your Choice.",
                    expertiseChoice(1, "rogue_expertise_1")),
                feature(1, "Sneak Attack", "You know how to strike subtly and exploit a foe's distraction. Once per turn you can deal an " +
                    "extra 1d6 damage to one creature you hit with an attack roll if you have Advantage on the roll " +
                    "and the attack uses a Finesse or a Ranged weapon. The extra damage's type is the same as the " +
                    "weapon's type. You don't need Advantage on the attack roll if at least one of your allies is " +
                    "within 5 feet of the target, the ally doesn't have the Incapacitated condition and you don't " +
                    "have Disadvantage on the attack roll. The extra damage increases as you gain Rogue levels, as " +
                    "shown in the Sneak Attack column of the Rogue Features table."),
                feature(1, "Thieves' Cant", "You picked up various languages in the communities where you plied your roguish talents. You " +
                    "know Thieves' Cant and one other language of your choice, which you choose from the language " +
                    "tables in Chapter 2.",
                    // "You know Thieves' Cant and one other language of your choice." The app
                    // granted the cant and never asked for the language — the feature's text
                    // had been a summary that did not mention it, which is why no test could.
                    Choice(
                        "rogue:cant_language", "Thieves' Cant",
                        "Choose the other language you picked up.",
                        1, ChoiceKind.LANGUAGE,
                        ChoiceOptions.fromStrings(LanguageData.ALL), "Level 1",
                    )),
                feature(1, "Weapon Mastery", "You can use the mastery property of two kinds of weapons you are proficient with. Whenever you finish a Long Rest, you can swap one of those weapons for a different one.",
                    masteryChoice("rogue", 2, 1)),
                masteryGrowth("rogue", 9, 3),
                feature(2, "Cunning Action", "Your quick thinking and agility allow you to move and act quickly. On your turn, you can take " +
                    "one of the following actions as a Bonus Action: Dash, Disengage, or Hide."),
                feature(3, "Steady Aim", "As a Bonus Action, you give yourself Advantage on your next attack roll on your current turn. " +
                    "You can use this feature only if you haven't moved during this turn, and after you use it, your " +
                    "Speed is 0 until the end of the current turn."),
                feature(5, "Cunning Strike", "You've developed cunning ways to use your Sneak Attack. When you deal Sneak Attack damage, you " +
                    "can add one of the following Cunning Strike effects. Each effect has a die cost, which is the " +
                    "number of Sneak Attack dice you must forgo to add the effect. You remove the die before rolling, " +
                    "and the effect occurs immediately after the attack's damage is dealt. For example, if you add " +
                    "the Poison effect, remove 1d6 from the Sneak Attack's damage before rolling. If a Cunning Strike " +
                    "requires a saving throw, the DC equals 8 plus your Dexterity modifier and Proficiency Bonus. " +
                    "Poison (Cost: 1d6). You add a toxin to your strike, forcing the target to make a Constitution " +
                    "saving throw. On a failed save, the target has the Poisoned condition for 1 minute. At the end " +
                    "of each of its turns, the poisoned target repeats the save, ending the effect on a success. To " +
                    "use this effect, you must have a Poisoner's Kit on your person. Trip (Cost: 1d6). If the target " +
                    "is Large or smaller, it must succeed on a Dexterity saving throw or have the Prone condition. " +
                    "Withdraw (Cost: 1d6). Immediately after the attack, you move up to half your speed without " +
                    "provoking Opportunity Attacks."),
                feature(5, "Uncanny Dodge", "When an attacker that you can see hits you with an attack roll, you can take a Reaction to halve " +
                    "the attack's damage against you (round down)."),
                feature(6, "Expertise", "Choose two more skill proficiencies to double your proficiency bonus with.",
                    expertiseChoice(6, "rogue_expertise_6")),
                feature(7, "Evasion", "On a Dexterity save for half damage, you take none on a success and half on a failure."),
                feature(7, "Reliable Talent", "Whenever you make an ability check that uses one of your skill or tool proficiencies, you can " +
                    "treat a d20 roll of 9 or lower as a 10."),
                feature(11, "Improved Cunning Strike", "You can use up to two Cunning Strike effects when you deal Sneak Attack damage, paying the die " +
                    "cost for each effect."),
                feature(14, "Devious Strikes", "You've practiced new ways to use your Sneak Attack deviously. The following effects are now " +
                    "among your Cunning Strike options. Daze (Cost: 2d6). The target must succeed on a Constitution " +
                    "saving throw, or on its next turn, it can do only one of the following: move or take an action " +
                    "or a Bonus Action. Knock Out (Cost: 6d6). The target must succeed on a Constitution saving " +
                    "throw, or it has the Unconscious condition for 1 minute or until it takes any damage. The " +
                    "Unconscious target repeats the save at the end of its turns, ending the effect on itself on a " +
                    "success. Obscure (Cost: 3d6). The target must succeed on a Dexterity saving throw, or it has the " +
                    "Blinded condition until the end of its next turn."),
                feature(15, "Slippery Mind", "Your mind is exceptionally difficult to control. You gain proficiency in Wisdom and Charisma " +
                    "saving throws."),
                feature(18, "Elusive", "You're so evasive that attackers rarely gain the upper hand against you. No attack roll can have " +
                    "advantage against you unless you have the Incapacitated condition."),
                feature(20, "Stroke of Luck", "You have a marvelous knack for succeeding when you need to. If you fail a d20 Test, you can turn " +
                    "the roll into a 20. Once you use this feature, you can't use it again until you finish a Short " +
                    "or Long Rest."),
            ),
        ),

        // ------------------------------------------------------------------ Sorcerer
        ClassProgression(
            classId = "sorcerer",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Sorcerer Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Sorcerer spells using Charisma, preparing them from the Sorcerer spell list."),
                feature(1, "Innate Sorcery", "An event in your past left an indelible mark on you, infusing you with simmering magic. As a " +
                    "Bonus Action, you can unleash that magic for 1 minute, during which you gain the following " +
                    "benefits: The spell save DC of your Sorcerer spells increases by 1. You have Advantage on the " +
                    "attack rolls of Sorcerer spells you cast. You can use this feature twice, and you regain all " +
                    "expended uses of it when you finish a Long Rest."),
                feature(2, "Font of Magic", "You can tap into the wellspring of magic within yourself. This wellspring is represented by " +
                    "Sorcery Points, which allow you to create a variety of magical effects. You have 2 Sorcery " +
                    "Points, and you gain more as you reach higher levels, as shown in the Sorcery Points column of " +
                    "the Sorcerer Features table. You can't have more Sorcery Points than the number shown in the " +
                    "table for your level. You regain all expended Sorcery Points when you finish a Long Rest. You " +
                    "can use your Sorcery Points to fuel the options below, along with other features, such as " +
                    "Metamagic, that use those points. Converting Spell Slots to Sorcery Points. You can expend a " +
                    "spell slot to gain a number of Sorcery Points equal to the slot's level (no action required). " +
                    "Creating Spell Slots. As a Bonus Action, you can transform unexpended Sorcery Points into one " +
                    "spell slot. The Creating Spell Slots table shows the cost of creating a spell slot of a given " +
                    "level, and it lists the minimum Sorcerer level you must be to create a slot. You can create a " +
                    "spell slot no higher than level 5. Any spell slot you create with this feature vanishes when you " +
                    "finish a Long Rest. Creating Spell Slots Spell Slot Level | Sorcery Point Cost | Min. Sorcerer " +
                    "Level | 1 | 2 | 2 | 2 | 3 | 3 | 3 | 5 | 5 | 4 | 6 | 7 | 5 | 7 | 9 |"),
                feature(2, "Metamagic", "Because your magic flows from within, you can alter your spells to suit your needs; you gain two " +
                    "Metamagic options of your choice from 'Metamagic Options' later in this class's description. You " +
                    "use the chosen options to temporarily modify spells you cast. To use an option, you must spend " +
                    "the number of Sorcery Points that it costs. You can use only one Metamagic option on a spell " +
                    "when you cast it unless otherwise noted in one of those options. Whenever you gain a Sorcerer " +
                    "level, you can replace one of your Metamagic options with one you don't know. You gain two more " +
                    "options at Sorcerer level 10 and two more at Sorcerer level 17.",
                    metamagicChoice("metamagic_2", 2, 2)),
                feature(5, "Sorcerous Restoration", "When you finish a Short Rest, you can regain expended Sorcery Points, but no more than a number " +
                    "equal to half your Sorcerer level (round down). Once you use this feature, you can't do so again " +
                    "until you finish a Long Rest."),
                feature(7, "Sorcery Incarnate", "If you have no uses of Innate Sorcery left, you can use it if you spend 2 Sorcery Points when " +
                    "you take the Bonus Action to activate it. In addition, while your Innate Sorcery feature is " +
                    "active, you can use up to two of your Metamagic options on each spell you cast."),
                feature(10, "Metamagic", "Choose two more Metamagic options.",
                    metamagicChoice("metamagic_10", 2, 10)),
                feature(17, "Metamagic", "Choose two more Metamagic options.",
                    metamagicChoice("metamagic_17", 2, 17)),
                feature(20, "Arcane Apotheosis", "While your Innate Sorcery feature is active, you can use one Metamagic option on each of your " +
                    "turns without spending Sorcery Points on it."),
            ),
            cantripsKnown = mapOf(1 to 4, 4 to 5, 10 to 6),
            preparedSpells = listOf(
                2, 4, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22,
            ),
        ),

        // ------------------------------------------------------------------ Warlock
        ClassProgression(
            classId = "warlock",
            casterType = CasterType.PACT,
            subclassLevel = 3,
            subclassLabel = "Warlock Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Pact Magic", "Through occult ceremony, you have formed a pact with a mysterious entity to gain magical powers. " +
                    "The entity is a voice in the shadows—its identity unclear—but its boon to you is concrete: the " +
                    "ability to cast spells. See chapter 7 for the rules on spellcasting. The information below " +
                    "details how you use those rules with Warlock spells, which appear in the Warlock spell list " +
                    "later in the class's description. Cantrips. You know two Warlock cantrips of your choice. " +
                    "Eldritch Blast and Prestidigitation are recommended. Whenever you gain a Warlock level, you can " +
                    "replace one of your cantrips from this feature with another Warlock cantrip of your choice. When " +
                    "you reach Warlock levels 4 and 10, you learn another Warlock cantrip of your choice, as shown in " +
                    "the Cantrips column of the Warlock Features table. Spell Slots. The Warlock Features table shows " +
                    "how many spell slots you have to cast your Warlock spells of levels 1–5. The table also shows " +
                    "the level of those slots, all of which are the same level. You regain all expended Pact Magic " +
                    "spell slots when you finish a Short or Long Rest. For example, when you're a level 5 Warlock, " +
                    "you have two level 3 spell slots. To cast the level 1 spell Witch Bolt, you must spend one of " +
                    "those slots, and you cast it as a level 3 spell. Prepared Spells of Level 1+. You prepare the " +
                    "list of level 1+ spells that are available for you to cast with this feature. To start, choose " +
                    "two level 1 Warlock spells. Charm Person and Hex are recommended. The number of spells on your " +
                    "list increases as you gain Warlock levels, as shown in the Prepared Spells column of the Warlock " +
                    "Features table. Whenever that number increases, choose additional Warlock spells until the " +
                    "number of spells on your list matches the number in the table. The chosen spells must be of a " +
                    "level no higher than what's shown in the table's Slot Level column for your level. When you " +
                    "reach level 6, for example, you learn a new Warlock spell, which can be of levels 1–3. If " +
                    "another Warlock feature gives you spells that you always have prepared, those spells don't count " +
                    "against the number of spells you can prepare with this feature, but those spells otherwise count " +
                    "as Warlock spells for you. Changing Your Prepared Spells. Whenever you gain a Warlock level, you " +
                    "can replace one spell on your list with another Warlock spell of an eligible level. Spellcasting " +
                    "Ability. Charisma is the spellcasting ability for your Warlock spells. Spellcasting Focus. You " +
                    "can use an Arcane Focus as a Spellcasting Focus for your Warlock spells."),
                feature(1, "Eldritch Invocations", "You have unearthed Eldritch Invocations, pieces of forbidden knowledge that imbue you with an " +
                    "abiding magical ability or other lessons. You gain one invocation of your choice, such as Pact " +
                    "of the Tome. Invocations are described in the 'Eldritch Invocation Options' section later in " +
                    "this class's description. Prerequisites. If an invocation has a prerequisite, you must meet it " +
                    "to learn that invocation. For example, if an invocation requires you to be a level 5+ Warlock, " +
                    "you can select the invocation once you reach Warlock level 5. Replacing and Gaining Invocations. " +
                    "Whenever you gain a Warlock level, you can replace one of your invocations with another one for " +
                    "which you qualify. You can't replace an invocation if it's a prerequisite for another invocation " +
                    "that you have. When you gain certain Warlock levels, you gain more invocations of your choice, " +
                    "as shown in the Invocations column of the Warlock Features table. You can't pick the same " +
                    "invocation more than once unless its description says otherwise.",
                    invocationChoice(1)),
                feature(2, "Eldritch Invocations", "You now know three Eldritch Invocations.", invocationChoice(2)),
                feature(5, "Eldritch Invocations", "You now know five Eldritch Invocations.", invocationChoice(5)),
                feature(7, "Eldritch Invocations", "You now know six Eldritch Invocations.", invocationChoice(7)),
                feature(9, "Eldritch Invocations", "You now know seven Eldritch Invocations.", invocationChoice(9)),
                feature(12, "Eldritch Invocations", "You now know eight Eldritch Invocations.", invocationChoice(12)),
                feature(15, "Eldritch Invocations", "You now know nine Eldritch Invocations.", invocationChoice(15)),
                feature(18, "Eldritch Invocations", "You now know ten Eldritch Invocations.", invocationChoice(18)),
                feature(2, "Magical Cunning", "You can perform an esoteric rite for 1 minute. At the end of it, you regain expended Pact Magic " +
                    "spell slots but no more than a number equal to half your maximum (round up). Once you use this " +
                    "feature, you can't do so again until you finish a Long Rest."),
                feature(9, "Contact Patron", "In the past, you usually contacted your patron through intermediaries. Now you can communicate " +
                    "directly; you always have the Contact Other Plane spell prepared. With this feature, you can " +
                    "cast the spell without expending a spell slot to contact your patron, and you automatically " +
                    "succeed on the spell's saving throw. Once you cast the spell with this feature, you can't do so " +
                    "in this way again until you finish a Long Rest."),
                feature(11, "Mystic Arcanum (Level 6)", "Your patron grants you a magical secret called an arcanum. Choose one level 6 Warlock spell as " +
                    "this arcanum. You can cast your arcanum spell once without expending a spell slot, and you must " +
                    "finish a Long Rest before you can cast it in this way again. As shown in the Warlock Features " +
                    "table, you gain another Warlock spell of your choice that can be cast in this way when you reach " +
                    "Warlock levels 13 (level 7 spell), 15 (level 8 spell), and 17 (level 9 spell). You regain all " +
                    "uses of your Mystic Arcanum when you finish a Long Rest. Whenever you gain a Warlock level, you " +
                    "can replace one of your arcanum spells with another Warlock spell of the same level.",
                    namedSpellChoice(
                        id = "warlock:arcanum_6",
                        label = "Mystic Arcanum (Level 6)",
                        prompt = "Choose the level 6 Warlock spell you can cast once per Long Rest.",
                        source = "Level 11",
                        classId = "warlock",
                        spellLevel = 6,
                        // "Whenever you gain a Warlock level, you can replace one of your
                        // arcanum spells with another Warlock spell of the same level."
                        changeableOnLevelUp = true,
                    )),
                feature(13, "Mystic Arcanum (Level 7)", "Choose a level 7 spell you can cast once per Long Rest without a slot.",
                    namedSpellChoice(
                        id = "warlock:arcanum_7",
                        label = "Mystic Arcanum (Level 7)",
                        prompt = "Choose the level 7 Warlock spell you can cast once per Long Rest.",
                        source = "Level 13",
                        classId = "warlock",
                        spellLevel = 7,
                        // "Whenever you gain a Warlock level, you can replace one of your
                        // arcanum spells with another Warlock spell of the same level."
                        changeableOnLevelUp = true,
                    )),
                feature(15, "Mystic Arcanum (Level 8)", "Choose a level 8 spell you can cast once per Long Rest without a slot.",
                    namedSpellChoice(
                        id = "warlock:arcanum_8",
                        label = "Mystic Arcanum (Level 8)",
                        prompt = "Choose the level 8 Warlock spell you can cast once per Long Rest.",
                        source = "Level 15",
                        classId = "warlock",
                        spellLevel = 8,
                        // "Whenever you gain a Warlock level, you can replace one of your
                        // arcanum spells with another Warlock spell of the same level."
                        changeableOnLevelUp = true,
                    )),
                feature(17, "Mystic Arcanum (Level 9)", "Choose a level 9 spell you can cast once per Long Rest without a slot.",
                    namedSpellChoice(
                        id = "warlock:arcanum_9",
                        label = "Mystic Arcanum (Level 9)",
                        prompt = "Choose the level 9 Warlock spell you can cast once per Long Rest.",
                        source = "Level 17",
                        classId = "warlock",
                        spellLevel = 9,
                        // "Whenever you gain a Warlock level, you can replace one of your
                        // arcanum spells with another Warlock spell of the same level."
                        changeableOnLevelUp = true,
                    )),
                feature(20, "Eldritch Master", "When you use your Magical Cunning feature, you regain all your expended Pact Magic spell slots."),
            ),
            cantripsKnown = mapOf(1 to 2, 4 to 3, 10 to 4),
            preparedSpells = WARLOCK_KNOWN,
        ),

        // ------------------------------------------------------------------ Wizard
        ClassProgression(
            classId = "wizard",
            casterType = CasterType.FULL,
            subclassLevel = 3,
            subclassLabel = "Wizard Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Wizard spells using Intelligence, preparing them from your spellbook."),
                feature(1, "Ritual Adept", "You can cast any spell as a Ritual if that spell has the Ritual tag and the spell is in your " +
                    "spellbook. You needn't have the spell prepared, but you must read from the book to cast a spell " +
                    "in this way."),
                feature(1, "Arcane Recovery", "You can regain some of your magical energy by studying your spellbook. When you finish a Short " +
                    "Rest, you can choose expended spell slots to recover. The spell slots can have a combined level " +
                    "equal to no more than half your Wizard level (round up), and none of the slots can be level 6 or " +
                    "higher. For example, if you're a level 4 Wizard, you can recover up to two levels' worth of " +
                    "spell slots, regaining either one level 2 spell slot or two level 1 spell slots. Once you use " +
                    "this feature, you can't do so again until you finish a Long Rest."),
                feature(2, "Scholar", "While studying magic, you also specialized in another field of study. Choose one of the " +
                    "following skills in which you have proficiency: Arcana, History, Investigation, Medicine, " +
                    "Nature, or Religion. You have Expertise in the chosen skill.",
                    Choice("scholar", "Scholar", "Choose one skill to gain Expertise in.", 1, ChoiceKind.EXPERTISE,
                        ChoiceOptions.fromSkills(listOf(Skill.ARCANA, Skill.HISTORY, Skill.INVESTIGATION, Skill.MEDICINE, Skill.NATURE, Skill.RELIGION)), "Level 2")),
                feature(5, "Memorize Spell", "Whenever you finish a Short Rest, you can study your spellbook and replace one of the level 1+ " +
                    "Wizard spells you have prepared for your Spellcasting feature with another level 1+ spell from " +
                    "the book."),
                feature(18, "Spell Mastery", "You have achieved such mastery over certain spells that you can cast them at will. Choose a " +
                    "level 1 and a level 2 spell in your spellbook that have a casting time of an action. You always " +
                    "have those spells prepared, and you can cast them at their lowest level without expending a " +
                    "spell slot. To cast either spell at a higher level, you must expend a spell slot. Whenever you " +
                    "finish a Long Rest, you can study your spellbook and replace one of those spells with an " +
                    "eligible spell of the same level from the book.",
                    namedSpellChoice(
                        id = "wizard:spell_mastery_1",
                        label = "Spell Mastery (Level 1)",
                        prompt = "Choose the level 1 Wizard spell you can cast at will.",
                        source = "Level 18",
                        classId = "wizard",
                        spellLevel = 1,
                        fromWhatYouKnow = true,
                        // "Whenever you finish a Long Rest, you can study your spellbook and
                        // replace one of those spells with an eligible spell of the same level."
                        changeableOnRest = true,
                    ),
                    namedSpellChoice(
                        id = "wizard:spell_mastery_2",
                        label = "Spell Mastery (Level 2)",
                        prompt = "Choose the level 2 Wizard spell you can cast at will.",
                        source = "Level 18",
                        classId = "wizard",
                        spellLevel = 2,
                        fromWhatYouKnow = true,
                        // "Whenever you finish a Long Rest, you can study your spellbook and
                        // replace one of those spells with an eligible spell of the same level."
                        changeableOnRest = true,
                    )),
                feature(20, "Signature Spells", "Choose two level 3 spells in your spellbook as your signature spells. You always have these " +
                    "spells prepared, and you can cast each of them once at level 3 without expending a spell slot. " +
                    "When you do so, you can't cast them in this way again until you finish a Short or Long Rest. To " +
                    "cast either spell at a higher level, you must expend a spell slot.",
                    namedSpellChoice(
                        id = "wizard:signature_spells",
                        label = "Signature Spells",
                        prompt = "Choose the two level 3 Wizard spells you always have " +
                            "prepared and can cast once each per Short Rest.",
                        source = "Level 20",
                        classId = "wizard",
                        spellLevel = 3,
                        count = 2,
                        fromWhatYouKnow = true,
                    )),
            ),
            cantripsKnown = mapOf(1 to 3, 4 to 4, 10 to 5),
            preparedSpells = FULL_CASTER_PREPARED,
        ),
        ClassProgression(
            classId = "artificer",
            casterType = CasterType.ARTIFICER,
            subclassLevel = 3,
            subclassLabel = "Artificer Subclass",
            asiLevels = STANDARD_ASI,
            features = listOf(
                feature(1, "Spellcasting", "You cast Artificer spells through Thieves' Tools, Tinker's Tools, or Artisan's Tools used as a Spellcasting Focus. Intelligence is your spellcasting ability, and you can change your prepared spells whenever you finish a Long Rest."),
                feature(1, "Tinker's Magic", "You know the Mending cantrip, and as a Magic action while holding Tinker's Tools you can conjure a mundane item within 5 feet of yourself that lasts until your next Long Rest."),
                feature(2, "Replicate Magic Item", "You learn arcane plans and can create magic items from them when you finish a Long Rest, so long as you have Tinker's Tools in hand. An item created this way vanishes 1d4 days after you die, and any Wand or Weapon you create can serve as a Spellcasting Focus.",
                    replicateChoice(4, 2)),
                feature(6, "Magic Item Tinker", "Your Replicate Magic Item feature gains two options: Charge Magic Item, which spends a level 1+ spell slot as a Bonus Action to recharge an item you made, and Drain Magic Item, which destroys one of your items to recover a spell slot once per Long Rest.",
                    replicateChoice(5, 6)),
                feature(7, "Flash of Genius", "When you or a creature you can see within 30 feet of you fails an ability check or a saving throw, you can take a Reaction to add your Intelligence modifier (minimum of +1) to the roll, potentially causing it to succeed. You can do this a number of times equal to your Intelligence modifier (minimum of once) per Long Rest."),
                feature(10, "Magic Item Adept", "You can now attune to up to four magic items at once.",
                    replicateChoice(6, 10)),
                feature(11, "Spell-Storing Item", "Whenever you finish a Long Rest, you can store a level 1, 2, or 3 Artificer spell with a casting time of an action in a weapon or Spellcasting Focus you touch. Any creature holding the object can take a Magic action to produce the spell's effect, using your spellcasting ability modifier. The spell lasts until used twice your Intelligence modifier times (minimum of twice) or until you store another."),
                feature(14, "Advanced Artifice", "Magic Item Savant lets you attune to up to five magic items at once, and Refreshed Genius returns one expended use of Flash of Genius whenever you finish a Short Rest.",
                    replicateChoice(7, 14)),
                feature(18, "Magic Item Master", "You can now attune to up to six magic items at once.",
                    replicateChoice(8, 18)),
                feature(20, "Soul of Artifice", "Cheat Death lets you disintegrate any number of Uncommon or Rare items you created when reduced to 0 Hit Points but not killed outright, setting your Hit Points to 20 times the number destroyed. Magical Guidance returns all expended uses of Flash of Genius on a Short Rest, provided you are attuned to at least one magic item."),
            ),
            cantripsKnown = mapOf(1 to 2, 10 to 3, 14 to 4),
            // The Artificer's prepared-spell curve is the same as the other half casters'.
            preparedSpells = HALF_CASTER_PREPARED,
        ),
    )

    private val byIdMap: Map<String, ClassProgression> = ALL.associateBy { it.classId }

    fun forClass(classId: String): ClassProgression? = byIdMap[classId]
}
