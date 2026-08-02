package com.pedroeu.ficha.data.content

/**
 * What a weapon's properties actually do.
 *
 * The properties were printed on the attack line as bare words, which is fine for Light or
 * Thrown but useless for the ones firearms introduce: nobody can guess what Burst Fire costs
 * or how many shots Reload leaves. The same reasoning as weapon mastery — the rule has to be
 * readable at the moment you use it, not looked up elsewhere.
 */
object WeaponPropertyData {

    /**
     * Matched by prefix, because several properties carry a value in brackets — "Reload (15
     * shots)", "Versatile (1d10)", "Thrown (20/60)" — and the rule is the same whatever the
     * number is.
     */
    private val DESCRIPTIONS: List<Pair<String, String>> = listOf(
        "Ammunition" to "You can use a weapon that has the Ammunition property to make a " +
            "Ranged attack only if you have ammunition to fire from it. The weapon's " +
            "description names the kind it takes. Drawing the ammunition is part of the " +
            "attack, and firing it destroys it.",
        "Burst Fire" to "As an action, you can expend 10 pieces of this weapon's ammunition " +
            "to spray shots in a 10-foot Cube within the weapon's normal range. Each " +
            "creature in that area must succeed on a DC 15 Dexterity saving throw or take " +
            "damage. Roll the weapon's damage once and apply it to each creature that failed.",
        "Reload" to "You can make only the listed number of shots before you must reload " +
            "the weapon, which takes an action or a Bonus Action.",
        "Loading" to "You can fire only one piece of ammunition from this weapon when you " +
            "use an action, a Bonus Action, or a Reaction to fire it, regardless of the " +
            "number of attacks you can normally make.",
        "Finesse" to "When making an attack with this weapon, you can use your choice of " +
            "your Strength or Dexterity modifier for the attack and damage rolls. You must " +
            "use the same modifier for both.",
        "Heavy" to "You have Disadvantage on attack rolls with this weapon if it is a Melee " +
            "weapon and your Strength score is below 13, or a Ranged weapon and your " +
            "Dexterity score is below 13.",
        "Light" to "When you take the Attack action on your turn and attack with this " +
            "weapon, you can make one extra attack as a Bonus Action later on the same turn " +
            "with a different Light weapon, and you don't add your ability modifier to that " +
            "extra attack's damage unless the modifier is negative.",
        "Reach" to "This weapon adds 5 feet to your reach when you attack with it, as well " +
            "as when determining your reach for Opportunity Attacks.",
        "Thrown" to "You can throw this weapon to make a Ranged attack, using the same " +
            "ability modifier as a melee attack with it.",
        "Two-Handed" to "This weapon requires two hands when you attack with it.",
        "Versatile" to "This weapon can be used with one or two hands. The damage in " +
            "brackets applies when you use two hands and the weapon isn't a Ranged weapon.",
    )

    /** The rule for a property as written on a weapon, or blank if it needs no explaining. */
    fun describe(property: String): String =
        DESCRIPTIONS.firstOrNull { (name, _) -> property.startsWith(name, ignoreCase = true) }
            ?.second
            .orEmpty()

    /** Every property on a weapon that has a rule worth reading, paired with that rule. */
    fun explain(properties: List<String>): List<Pair<String, String>> =
        properties.mapNotNull { property ->
            describe(property).takeIf { it.isNotBlank() }?.let { property to it }
        }

    /** The property names this table covers, so a test can check none has been missed. */
    fun knownNames(): Set<String> = DESCRIPTIONS.map { it.first }.toSet()
}
