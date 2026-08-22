package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.CharClass
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.data.model.ClassChoice
import com.pedroeu.ficha.data.model.FeatureChoiceOption
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.SpellStub
import com.pedroeu.ficha.data.model.Trait

object ClassData {

    private fun skillChoice(count: Int, options: List<Skill>) =
        ClassChoice.SkillProficiencyChoice("skills", "Skill Proficiencies", count, options)

    private fun cantrips(id: String, label: String, count: Int, options: List<SpellStub>) =
        ClassChoice.CantripChoice(id, label, count, options)

    /**
     * The class's whole list at one spell level, straight from the spell catalog.
     *
     * Every caster below chooses from this. The alternative — a curated handful written out
     * here — is what shipped first, and it was wrong twice over: it silently made a class's
     * spell list shorter than the rules say, and its private ids (`sorc_shield`) meant the
     * same spell picked at creation and granted by a feature looked like two different
     * entries. Reading the catalog fixes both, and a spell added there reaches every class
     * that lists it without a second edit here.
     */
    private fun catalogSpells(classId: String, level: Int): List<SpellStub> =
        SpellData.forClass(classId, level).map { spell ->
            SpellStub(spell.id, spell.name, spell.level, spell.school, spell.description)
        }

    val ALL: List<CharClass> = listOf(
        CharClass(
            id = "barbarian",
            name = "Barbarian",
            hitDie = 12,
            primaryAbility = listOf(Ability.STR),
            savingThrows = listOf(Ability.STR, Ability.CON),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Rage", "As a bonus action, enter a rage that grants bonus melee damage, resistance to bludgeoning, piercing, and slashing damage, and advantage on Strength checks and saves. Lasts up to 1 minute."),
                Trait("Unarmored Defense", "While not wearing armor, your AC equals 10 + your Dexterity modifier + your Constitution modifier."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL)),
            ),
            isSpellcaster = false,
            summary = "A fierce warrior who channels primal rage into devastating attacks and unmatched resilience.",
        ),
        CharClass(
            id = "bard",
            name = "Bard",
            hitDie = 8,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.DEX, Ability.CHA),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple", "Hand Crossbows", "Longswords", "Rapiers", "Shortswords"),
            toolProficiencies = listOf("Three musical instruments of your choice"),
            level1Features = listOf(
                Trait("Bardic Inspiration", "As a bonus action, give a creature within 60 feet a d6 Bardic Inspiration die to add to one ability check, attack roll, or saving throw. Uses equal to your Charisma modifier per long rest."),
                Trait("Spellcasting", "You cast Bard spells using Charisma as your spellcasting ability."),
            ),
            choices = listOf(
                skillChoice(3, Skill.ALL),
                cantrips("cantrips", "Cantrips Known", 2, catalogSpells("bard", 0)),
                cantrips("spells1", "1st-Level Spells Known", 4, catalogSpells("bard", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A charismatic performer who weaves magic through music, words, and inspiration.",
        ),
        CharClass(
            id = "cleric",
            name = "Cleric",
            hitDie = 8,
            primaryAbility = listOf(Ability.WIS),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Cleric spells using Wisdom. You prepare a number of spells from the full Cleric spell list each day equal to your Wisdom modifier + your Cleric level."),
                Trait("Channel Divinity", "You can channel divine energy to fuel magical effects, such as Turn Undead. Usable once per short or long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.HISTORY, Skill.INSIGHT, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION)),
                ClassChoice.FeatureOption("divine_order", "Divine Order", listOf(
                    FeatureChoiceOption("protector", "Protector", "You gain proficiency with martial weapons and heavy armor."),
                    FeatureChoiceOption("thaumaturge", "Thaumaturge", "You learn an extra cantrip from the Cleric list and add your Wisdom modifier to Religion checks."),
                )),
                cantrips("cantrips", "Cantrips Known", 3, catalogSpells("cleric", 0)),
                cantrips("spells1", "Prepared Spells (Level 1)", 4, catalogSpells("cleric", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.WIS,
            summary = "A conduit for divine power, blending healing magic with the favor of a deity.",
        ),
        CharClass(
            id = "druid",
            name = "Druid",
            hitDie = 8,
            primaryAbility = listOf(Ability.WIS),
            savingThrows = listOf(Ability.INT, Ability.WIS),
            armorProficiencies = listOf("Light (nonmetal)", "Medium (nonmetal)", "Shields (nonmetal)"),
            weaponProficiencies = listOf("Clubs", "Daggers", "Darts", "Javelins", "Maces", "Quarterstaffs", "Scimitars", "Sickles", "Slings", "Spears"),
            toolProficiencies = listOf("Herbalism Kit"),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Druid spells using Wisdom. You prepare a number of spells from the full Druid spell list each day equal to your Wisdom modifier + your Druid level."),
                Trait("Wild Shape", "As a bonus action, transform into a beast you've seen before. Usable twice per short or long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ANIMAL_HANDLING, Skill.INSIGHT, Skill.MEDICINE, Skill.NATURE, Skill.PERCEPTION, Skill.RELIGION, Skill.SURVIVAL)),
                ClassChoice.FeatureOption("primal_order", "Primal Order", listOf(
                    FeatureChoiceOption("magician", "Magician", "You learn an extra cantrip from the Wizard list and add your Wisdom modifier to Arcana or Nature checks."),
                    FeatureChoiceOption("warden", "Warden", "You gain proficiency with martial weapons and medium armor."),
                )),
                cantrips("cantrips", "Cantrips Known", 2, catalogSpells("druid", 0)),
                cantrips("spells1", "Prepared Spells (Level 1)", 4, catalogSpells("druid", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.WIS,
            summary = "A guardian of the natural world, wielding primal magic and the ability to take animal form.",
        ),
        CharClass(
            id = "fighter",
            name = "Fighter",
            hitDie = 10,
            primaryAbility = listOf(Ability.STR, Ability.DEX),
            savingThrows = listOf(Ability.STR, Ability.CON),
            armorProficiencies = listOf("Light", "Medium", "Heavy", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Second Wind", "As a bonus action, regain hit points equal to 1d10 + your Fighter level. Usable twice per long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ACROBATICS, Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERCEPTION, Skill.SURVIVAL)),
                ClassChoice.FeatureOption("fighting_style", "Fighting Style", listOf(
                    FeatureChoiceOption("archery", "Archery", "You gain a +2 bonus to attack rolls you make with ranged weapons."),
                    FeatureChoiceOption("defense", "Defense", "While wearing armor, you gain a +1 bonus to AC."),
                    FeatureChoiceOption("dueling", "Dueling", "While wielding a melee weapon in one hand and no other weapons, you gain +2 to damage rolls with it."),
                    FeatureChoiceOption("great_weapon", "Great Weapon Fighting", "When you roll a 1 or 2 on a damage die for a two-handed melee weapon, you can reroll it."),
                    FeatureChoiceOption("protection", "Protection", "When a creature you can see attacks a target other than you within 5 feet, you can impose disadvantage on the attack."),
                    FeatureChoiceOption("two_weapon", "Two-Weapon Fighting", "You can add your ability modifier to the damage of your off-hand attack."),
                )),
            ),
            isSpellcaster = false,
            summary = "A master of martial combat, skilled with a variety of weapons and armor.",
        ),
        CharClass(
            id = "monk",
            name = "Monk",
            hitDie = 8,
            primaryAbility = listOf(Ability.DEX, Ability.WIS),
            savingThrows = listOf(Ability.STR, Ability.DEX),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Simple", "Shortswords"),
            toolProficiencies = listOf("One artisan's tool or musical instrument of your choice"),
            level1Features = listOf(
                Trait("Martial Arts", "You can use Dexterity instead of Strength for unarmed strikes and monk weapons, and your unarmed strikes deal 1d6 damage."),
                Trait("Unarmored Defense", "While not wearing armor or a shield, your AC equals 10 + your Dexterity modifier + your Wisdom modifier."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ACROBATICS, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.RELIGION, Skill.STEALTH)),
            ),
            isSpellcaster = false,
            summary = "A disciplined martial artist who channels inner energy into supernatural feats of speed and strikes.",
        ),
        CharClass(
            id = "paladin",
            name = "Paladin",
            hitDie = 10,
            primaryAbility = listOf(Ability.STR, Ability.CHA),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light", "Medium", "Heavy", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Lay on Hands", "You have a pool of healing power equal to 5 times your Paladin level, which you can touch a creature to restore as hit points, refreshing on a long rest."),
                Trait("Spellcasting", "You cast Paladin spells using Charisma as your spellcasting ability, preparing them from the Paladin spell list and changing them whenever you finish a Long Rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ATHLETICS, Skill.INSIGHT, Skill.INTIMIDATION, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION)),
                cantrips("spells1", "Prepared Spells (Level 1)", 2, catalogSpells("paladin", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A holy warrior bound by a sacred oath, blending martial prowess with divine magic.",
        ),
        CharClass(
            id = "ranger",
            name = "Ranger",
            hitDie = 10,
            primaryAbility = listOf(Ability.DEX, Ability.WIS),
            savingThrows = listOf(Ability.STR, Ability.DEX),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Favored Enemy", "You always have Hunter's Mark prepared, and can cast it without a spell slot a number of times equal to your Proficiency Bonus per Long Rest."),
                Trait("Spellcasting", "You cast Ranger spells using Wisdom as your spellcasting ability, preparing them from the Ranger spell list and changing them whenever you finish a Long Rest."),
            ),
            choices = listOf(
                skillChoice(3, listOf(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INSIGHT, Skill.INVESTIGATION, Skill.NATURE, Skill.PERCEPTION, Skill.STEALTH, Skill.SURVIVAL)),
                cantrips("spells1", "Prepared Spells (Level 1)", 2, catalogSpells("ranger", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.WIS,
            summary = "A skilled hunter and survivalist at home in the wilderness, blending martial skill with nature magic.",
        ),
        CharClass(
            id = "rogue",
            name = "Rogue",
            hitDie = 8,
            primaryAbility = listOf(Ability.DEX),
            savingThrows = listOf(Ability.DEX, Ability.INT),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple", "Hand Crossbows", "Longswords", "Rapiers", "Shortswords"),
            toolProficiencies = listOf("Thieves' Tools"),
            level1Features = listOf(
                Trait("Expertise", "Choose two of your skill proficiencies (or one skill and Thieves' Tools). Your proficiency bonus is doubled for checks using them."),
                Trait("Sneak Attack", "Once per turn, deal an extra 1d6 damage to a creature you hit with an attack if you have advantage or an ally is within 5 feet of the target."),
                Trait("Thieves' Cant", "You know a secret mix of dialect, jargon, and code that lets you hide messages in ordinary conversation."),
            ),
            choices = listOf(
                skillChoice(4, listOf(Skill.ACROBATICS, Skill.ATHLETICS, Skill.DECEPTION, Skill.INSIGHT, Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.PERCEPTION, Skill.PERFORMANCE, Skill.PERSUASION, Skill.SLEIGHT_OF_HAND, Skill.STEALTH)),
            ),
            isSpellcaster = false,
            summary = "A cunning expert in stealth, precision strikes, and getting out of (or into) trouble.",
        ),
        CharClass(
            id = "sorcerer",
            name = "Sorcerer",
            hitDie = 6,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.CON, Ability.CHA),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Daggers", "Darts", "Slings", "Quarterstaffs", "Light Crossbows"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Sorcerer spells using Charisma as your spellcasting ability."),
                Trait("Innate Sorcery", "As a bonus action, surge with power for a minute, gaining +1 to spell save DC and advantage on spell attack rolls. Usable twice per long rest."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.DECEPTION, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERSUASION, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 4, catalogSpells("sorcerer", 0)),
                cantrips("spells1", "1st-Level Spells Known", 2, catalogSpells("sorcerer", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A wielder of magic drawn from an innate, often mysterious source deep within their blood.",
        ),
        CharClass(
            id = "warlock",
            name = "Warlock",
            hitDie = 8,
            primaryAbility = listOf(Ability.CHA),
            savingThrows = listOf(Ability.WIS, Ability.CHA),
            armorProficiencies = listOf("Light"),
            weaponProficiencies = listOf("Simple"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Pact Magic", "You cast Warlock spells using Charisma. Your spell slots recharge on a short or long rest."),
                Trait("Eldritch Invocations (from level 2)", "Starting at 2nd level, you learn magical invocations that grant passive and active powers."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.DECEPTION, Skill.HISTORY, Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.NATURE, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 2, catalogSpells("warlock", 0)),
                cantrips("spells1", "1st-Level Spells Known", 2, catalogSpells("warlock", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.CHA,
            summary = "A spellcaster who draws magic from a bargain struck with an otherworldly patron.",
        ),
        CharClass(
            id = "wizard",
            name = "Wizard",
            hitDie = 6,
            primaryAbility = listOf(Ability.INT),
            savingThrows = listOf(Ability.INT, Ability.WIS),
            armorProficiencies = emptyList(),
            weaponProficiencies = listOf("Daggers", "Darts", "Slings", "Quarterstaffs", "Light Crossbows"),
            toolProficiencies = emptyList(),
            level1Features = listOf(
                Trait("Spellcasting", "You cast Wizard spells using Intelligence. You prepare a number of spells from your spellbook each day equal to your Intelligence modifier + your Wizard level."),
                Trait("Ritual Adept", "You can cast any spell in your spellbook that has the ritual tag as a ritual, without expending a spell slot."),
            ),
            choices = listOf(
                skillChoice(2, listOf(Skill.ARCANA, Skill.HISTORY, Skill.INSIGHT, Skill.INVESTIGATION, Skill.MEDICINE, Skill.RELIGION)),
                cantrips("cantrips", "Cantrips Known", 3, catalogSpells("wizard", 0)),
                cantrips("spellbook", "Spellbook (1st-Level Spells)", 6, catalogSpells("wizard", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.INT,
            summary = "A scholarly spellcaster who masters magic through study, logic, and a well-kept spellbook.",
        ),
        CharClass(
            id = "artificer",
            name = "Artificer",
            book = Sourcebook.EBERRON,
            hitDie = 8,
            primaryAbility = listOf(Ability.INT),
            savingThrows = listOf(Ability.CON, Ability.INT),
            armorProficiencies = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple"),
            toolProficiencies = listOf(
                "Thieves' Tools",
                "Tinker's Tools",
                "One type of Artisan's Tools of your choice",
            ),
            level1Features = listOf(
                Trait(
                    "Spellcasting",
                    "You channel magic through tools. You can use Thieves' Tools, Tinker's Tools, " +
                        "or another kind of Artisan's Tools with which you have proficiency as a " +
                        "Spellcasting Focus, and you must have one of those focuses in hand when you " +
                        "cast an Artificer spell. Intelligence is your spellcasting ability. You know " +
                        "two Artificer cantrips and prepare two level 1 Artificer spells to start; " +
                        "you can change your prepared spells and swap one cantrip whenever you finish " +
                        "a Long Rest.",
                ),
                Trait(
                    "Tinker's Magic",
                    "You know the Mending cantrip. As a Magic action while holding Tinker's Tools, " +
                        "you can create one mundane item — such as a bedroll, a crowbar, a lamp, a net, " +
                        "or a torch — in an unoccupied space within 5 feet of yourself. The item lasts " +
                        "until you finish a Long Rest, at which point it vanishes. You can do this a " +
                        "number of times equal to your Intelligence modifier (minimum of once), and " +
                        "you regain all expended uses when you finish a Long Rest.",
                ),
            ),
            choices = listOf(
                skillChoice(2, listOf(
                    Skill.ARCANA, Skill.HISTORY, Skill.INVESTIGATION,
                    Skill.MEDICINE, Skill.NATURE, Skill.PERCEPTION, Skill.SLEIGHT_OF_HAND,
                )),
                cantrips("cantrips", "Cantrips Known", 2, catalogSpells("artificer", 0)),
                cantrips("spells1", "Prepared Spells (Level 1)", 2, catalogSpells("artificer", 1)),
            ),
            isSpellcaster = true,
            spellcastingAbility = Ability.INT,
            summary = "An inventor who unlocks magic in objects, replicating wondrous items and turning tools into a spellcasting focus.",
        ),
    )

    fun byId(id: String): CharClass? = ALL.find { it.id == id }
}
