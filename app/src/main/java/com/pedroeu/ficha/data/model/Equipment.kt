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
)
