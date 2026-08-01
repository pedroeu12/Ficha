package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.MasteryData
import com.pedroeu.ficha.data.model.WeaponDef

/** A kind of weapon the character has mastery with, and what that mastery does. */
data class WeaponMastery(
    val weaponId: String,
    val weaponName: String,
    val property: String,
    val description: String,
    /** Which feature granted it — a class's Weapon Mastery, or the Weapon Master feat. */
    val source: String,
)

/**
 * Which weapons the character can use the mastery property of.
 *
 * Mastery is chosen per kind of weapon, so this resolves the player's picks into the actual
 * weapons and the property each one carries. Everything is derived at read time from the
 * stored choice, which means correcting the class tables or the property text reaches every
 * existing sheet without touching the saved character.
 */
object CharacterMasteries {

    /** Every mastery the character has, from every class they have levels in, plus feats. */
    fun all(character: PlayerCharacter): List<WeaponMastery> = buildList {
        ClassLevels.of(character).forEach { entry ->
            val count = MasteryData.countFor(entry.classId, entry.level)
            if (count == 0) return@forEach

            val className = com.pedroeu.ficha.data.content.ClassData.byId(entry.classId)?.name
                ?: entry.classId
            latestSelection(character, entry.classId, entry.level)
                // A stored pick beyond the current allowance is ignored rather than dropped:
                // a character who lost a level keeps their choices for when they get it back.
                .take(count)
                .forEach { weaponId -> addMastery(weaponId, "$className Weapon Mastery") }
        }

        // The Weapon Master feat grants one more kind of weapon on top of the class's.
        if ("weapon_master" in character.featIds) {
            ChoiceResolver
                .selectionsFor(character, "feat:weapon_master:mastery")
                .forEach { weaponId -> addMastery(weaponId, "Weapon Master") }
        }
    }.distinctBy { it.weaponId }

    /**
     * The most recent answer to a class's mastery choice.
     *
     * The choice is asked again each time the count grows, and each asking restates the whole
     * list rather than adding to it — so the answer given at the highest level reached is the
     * one that counts. Merging every level's answer instead would leave a Fighter who changed
     * their mind holding more masteries than the table allows.
     */
    private fun latestSelection(
        character: PlayerCharacter,
        classId: String,
        level: Int,
    ): List<String> {
        val choiceId = "class:$classId:weapon_mastery"
        for (candidate in level downTo 1) {
            character.levelSelections["$candidate:$choiceId"]
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        // Characters made before the choice was keyed by level fall back to the flat maps.
        return character.classChoiceSelections[choiceId]
            ?: character.originChoiceSelections[choiceId]
            ?: emptyList()
    }

    private fun MutableList<WeaponMastery>.addMastery(weaponId: String, source: String) {
        val weapon = EquipmentData.weaponById(weaponId) ?: return
        if (weapon.mastery.isBlank()) return
        add(
            WeaponMastery(
                weaponId = weapon.id,
                weaponName = weapon.name,
                property = weapon.mastery,
                description = MasteryData.describe(weapon.mastery),
                source = source,
            )
        )
    }

    /** How many kinds of weapon the character may master, across every class and the feat. */
    fun allowance(character: PlayerCharacter): Int {
        val fromClasses = ClassLevels.of(character).sumOf { entry ->
            MasteryData.countFor(entry.classId, entry.level)
        }
        val fromFeat = if ("weapon_master" in character.featIds) 1 else 0
        return fromClasses + fromFeat
    }

    /** True when this character ever chooses masteries, so the sheet can hide the section. */
    fun hasWeaponMastery(character: PlayerCharacter): Boolean = allowance(character) > 0

    /** The mastery in effect for a specific weapon, or null if the character lacks it. */
    fun forWeapon(character: PlayerCharacter, weapon: WeaponDef): WeaponMastery? =
        all(character).find { it.weaponId == weapon.id }

    /**
     * How an attack line should describe the weapon's mastery.
     *
     * A weapon's property is worth naming even when the character can't use it — knowing a
     * greataxe would Cleave is what tells a player which weapon to master next — so an
     * unavailable one is shown greyed rather than hidden.
     */
    fun noteFor(character: PlayerCharacter, weapon: WeaponDef): String = when {
        weapon.mastery.isBlank() -> ""
        forWeapon(character, weapon) != null -> "Mastery: ${weapon.mastery}"
        hasWeaponMastery(character) -> "${weapon.mastery} (not mastered)"
        else -> "${weapon.mastery} (mastery property)"
    }
}
