package com.pedroeu.ficha.data.model

enum class ArmorCategory { LIGHT, MEDIUM, HEAVY, SHIELD }

data class ArmorDef(
    val id: String,
    val name: String,
    val category: ArmorCategory,
    val baseAc: Int,
    /** Max Dex modifier applied on top of baseAc; null means unlimited (light armor). */
    val maxDexBonus: Int?,
    val strRequirement: Int = 0,
    val stealthDisadvantage: Boolean = false,
    val costGp: Double = 0.0,
    val weightLb: Double = 0.0,
    val description: String = "",
)

data class WeaponDef(
    val id: String,
    val name: String,
    val damageDice: String,
    val damageType: String,
    val properties: List<String>,
    val isRanged: Boolean = false,
    /** True if Dex may be used instead of Str for attack/damage (finesse or ranged). */
    val usesDexOption: Boolean = false,
    /** The weapon's mastery property in the 2024 rules, e.g. "Vex" or "Topple". */
    val mastery: String = "",
    val costGp: Double = 0.0,
    val weightLb: Double = 0.0,
    val range: String = "",
    val description: String = "",
)

/** Gear that is neither armor nor a weapon: packs, tools, consumables, and sundries. */
data class GearDef(
    val id: String,
    val name: String,
    val category: String,
    val costGp: Double = 0.0,
    val weightLb: Double = 0.0,
    val description: String = "",
)

/** A starting kit of gear granted by a class, resolved at character creation. */
data class StartingKit(
    val armorIds: List<String> = emptyList(),
    val weaponIds: List<String> = emptyList(),
    val otherGear: List<String> = emptyList(),
    val goldPieces: Int = 0,
)

/** A free-form inventory line the player owns; not every item maps to an ArmorDef/WeaponDef. */
@kotlinx.serialization.Serializable
data class InventoryItem(
    val name: String,
    val quantity: Int = 1,
    val weightLb: Double = 0.0,
    val notes: String = "",
    val equipped: Boolean = false,
    /** If this line represents a known weapon/armor, its def id, else null for generic gear. */
    val weaponDefId: String? = null,
    val armorDefId: String? = null,
    /**
     * The magic item this line is, if any. A magic weapon is both: [weaponDefId] says what it
     * is made of and this says what is magical about it, which is how a +1 Longsword can be a
     * longsword on the attack line and still carry its bonus.
     */
    val magicItemId: String? = null,
    /**
     * The Artificer plan that made this item, for lines the character created rather than
     * found. Set means the line is owned by Replicate Magic Item: it appeared when the plan
     * was chosen for the day and it goes away when the plan is set aside, so nothing has to
     * be tidied up by hand.
     */
    val craftedFromPlanId: String? = null,
)
