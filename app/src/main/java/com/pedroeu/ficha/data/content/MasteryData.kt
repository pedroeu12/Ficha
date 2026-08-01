package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption

/**
 * The eight weapon mastery properties, and which classes can use them.
 *
 * Mastery is what a weapon *does* beyond its damage die in the 2024 rules, and it is the one
 * part of a martial character's turn the sheet had nothing to say about: every weapon already
 * carried its property name, but nobody was ever asked which weapons they had mastery with,
 * and the property meant nothing on the attack line.
 *
 * Mastery is granted per *kind* of weapon, not per weapon you carry — a Fighter with mastery
 * in Longswords uses Sap with any longsword they pick up, and none at all with a borrowed
 * greataxe.
 */
object MasteryData {

    /** One mastery property, with the rules text that says what it actually does. */
    data class Property(
        val name: String,
        val description: String,
    )

    val PROPERTIES: List<Property> = listOf(
        Property(
            "Cleave",
            "If you hit a creature with a melee attack roll using this weapon, you can make " +
                "a melee attack roll with the weapon against a second creature within 5 feet " +
                "of the first that is also within your reach. On a hit, the second creature " +
                "takes the weapon's damage, but don't add your ability modifier to that " +
                "damage unless that modifier is negative. You can make this extra attack " +
                "only once per turn.",
        ),
        Property(
            "Graze",
            "If your attack roll with this weapon misses a creature, you can deal damage to " +
                "that creature equal to the ability modifier you used to make the attack " +
                "roll. This damage is the same type dealt by the weapon, and the damage can " +
                "be increased only by increasing the ability modifier.",
        ),
        Property(
            "Nick",
            "When you make the extra attack of the Light property, you can make it as part " +
                "of the Attack action instead of as a Bonus Action. You can make this extra " +
                "attack only once per turn.",
        ),
        Property(
            "Push",
            "If you hit a creature with this weapon, you can push the creature up to 10 feet " +
                "straight away from yourself if it is Large or smaller.",
        ),
        Property(
            "Sap",
            "If you hit a creature with this weapon, that creature has Disadvantage on its " +
                "next attack roll before the start of your next turn.",
        ),
        Property(
            "Slow",
            "If you hit a creature with this weapon and deal damage to it, you can reduce " +
                "its Speed by 10 feet until the start of your next turn. If the creature is " +
                "hit more than once by weapons that have this property, the Speed reduction " +
                "doesn't exceed 10 feet.",
        ),
        Property(
            "Topple",
            "If you hit a creature with this weapon, you can force the creature to make a " +
                "Constitution saving throw (DC 8 plus the ability modifier used to make the " +
                "attack roll and your Proficiency Bonus). On a failed save, the creature has " +
                "the Prone condition.",
        ),
        Property(
            "Vex",
            "If you hit a creature with this weapon and deal damage to the creature, you " +
                "have Advantage on your next attack roll against that creature before the " +
                "end of your next turn.",
        ),
    )

    private val byName: Map<String, Property> =
        PROPERTIES.associateBy { it.name.lowercase() }

    fun byName(name: String): Property? = byName[name.lowercase()]

    fun describe(name: String): String = byName(name)?.description.orEmpty()

    /**
     * How many kinds of weapon each class has mastery with at a given level.
     *
     * These are the class tables' Weapon Mastery column. A class not listed here never gains
     * the feature, which is why a Wizard is never asked to choose.
     */
    private val COUNT_BY_CLASS: Map<String, Map<Int, Int>> = mapOf(
        "barbarian" to mapOf(1 to 2, 4 to 3, 10 to 4),
        "fighter" to mapOf(1 to 3, 4 to 4, 10 to 5, 16 to 6),
        "paladin" to mapOf(1 to 2, 9 to 3),
        "ranger" to mapOf(1 to 2, 9 to 3),
        "rogue" to mapOf(1 to 2, 9 to 3),
    )

    /** Kinds of weapon [classId] has mastery with at [level] in that class. */
    fun countFor(classId: String, level: Int): Int =
        COUNT_BY_CLASS[classId]
            ?.entries
            ?.filter { it.key <= level }
            ?.maxByOrNull { it.key }
            ?.value
            ?: 0

    /** The levels in [classId] at which the count goes up, so level up knows when to ask. */
    fun growsAt(classId: String, level: Int): Boolean =
        COUNT_BY_CLASS[classId]?.containsKey(level) == true

    fun hasWeaponMastery(classId: String): Boolean = classId in COUNT_BY_CLASS

    /** Every class id this table is keyed by, for the test that checks them against the data. */
    fun sourceIds(): Set<String> = COUNT_BY_CLASS.keys

    /**
     * The weapons that can be chosen: every weapon with a mastery property, labelled with the
     * property it grants so the choice is made on what it does rather than on the name.
     */
    fun weaponOptions(): List<ChoiceOption> = EquipmentData.WEAPONS
        .filter { it.mastery.isNotBlank() }
        .sortedBy { it.name }
        .map { weapon ->
            ChoiceOption(
                id = weapon.id,
                name = weapon.name,
                description = "${weapon.mastery}. ${describe(weapon.mastery)}",
                supporting = weapon.mastery,
            )
        }

    /**
     * The choice of which weapons to master, at the level the count reaches [count].
     *
     * Marked changeable on a rest because the rules let you swap one of your choices whenever
     * you finish a Long Rest — the sheet's rest flow already offers anything flagged this way.
     */
    fun choiceFor(classId: String, count: Int, level: Int): Choice = Choice(
        id = "class:$classId:weapon_mastery",
        label = "Weapon Mastery",
        prompt = "Choose $count kinds of weapon you're proficient with. You can use each " +
            "one's mastery property. Whenever you finish a Long Rest, you can swap one of " +
            "these choices for a different weapon.",
        count = count,
        kind = ChoiceKind.OPTION,
        options = weaponOptions(),
        source = if (level <= 1) "Level 1" else "Level $level",
        changeableOnRest = true,
    )
}
