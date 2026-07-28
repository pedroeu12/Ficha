package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.InventoryItem

enum class CatalogItemType { WEAPON, ARMOR, GEAR }

/**
 * A single browsable entry backing both the item detail view and the "add from rulebook"
 * picker, flattening weapons, armor, and gear into one searchable shape.
 */
data class CatalogItem(
    val id: String,
    val name: String,
    val type: CatalogItemType,
    val category: String,
    val costGp: Double,
    val weightLb: Double,
    val description: String,
    /** Label/value rows shown in the detail sheet, e.g. "Damage" to "1d8 Slashing". */
    val stats: List<Pair<String, String>>,
    val weaponDefId: String? = null,
    val armorDefId: String? = null,
) {
    val costLabel: String
        get() = when {
            costGp <= 0.0 -> "—"
            costGp < 0.1 -> "${(costGp * 100).toInt()} cp"
            costGp < 1.0 -> "${(costGp * 10).toInt()} sp"
            costGp == costGp.toInt().toDouble() -> "${costGp.toInt()} gp"
            else -> "$costGp gp"
        }

    val weightLabel: String
        get() = if (weightLb <= 0.0) "—" else "$weightLb lb"

    fun toInventoryItem(quantity: Int = 1): InventoryItem = InventoryItem(
        name = name,
        quantity = quantity,
        weightLb = weightLb,
        notes = description,
        weaponDefId = weaponDefId,
        armorDefId = armorDefId,
    )
}

object ItemCatalog {

    private val SIMPLE_WEAPON_IDS = setOf(
        "club", "dagger", "greatclub", "handaxe", "javelin", "light_hammer", "mace",
        "quarterstaff", "sickle", "spear", "light_crossbow", "dart", "shortbow", "sling",
    )

    val ALL: List<CatalogItem> = buildList {
        EquipmentData.WEAPONS.forEach { weapon ->
            val simple = weapon.id in SIMPLE_WEAPON_IDS
            add(
                CatalogItem(
                    id = "weapon:${weapon.id}",
                    name = weapon.name,
                    type = CatalogItemType.WEAPON,
                    category = buildString {
                        append(if (simple) "Simple" else "Martial")
                        append(if (weapon.isRanged) " Ranged" else " Melee")
                    },
                    costGp = weapon.costGp,
                    weightLb = weapon.weightLb,
                    description = weapon.description,
                    stats = buildList {
                        add("Damage" to "${weapon.damageDice} ${weapon.damageType}")
                        if (weapon.mastery.isNotBlank()) add("Mastery" to weapon.mastery)
                        if (weapon.range.isNotBlank()) add("Range" to weapon.range)
                        if (weapon.properties.isNotEmpty()) {
                            add("Properties" to weapon.properties.joinToString(", "))
                        }
                    },
                    weaponDefId = weapon.id,
                )
            )
        }

        EquipmentData.ARMOR.forEach { armor ->
            val acLabel = when {
                armor.category == ArmorCategory.SHIELD -> "+${armor.baseAc}"
                armor.maxDexBonus == null -> "${armor.baseAc} + Dex"
                armor.maxDexBonus == 0 -> "${armor.baseAc}"
                else -> "${armor.baseAc} + Dex (max ${armor.maxDexBonus})"
            }
            add(
                CatalogItem(
                    id = "armor:${armor.id}",
                    name = armor.name,
                    type = CatalogItemType.ARMOR,
                    category = when (armor.category) {
                        ArmorCategory.LIGHT -> "Light Armor"
                        ArmorCategory.MEDIUM -> "Medium Armor"
                        ArmorCategory.HEAVY -> "Heavy Armor"
                        ArmorCategory.SHIELD -> "Shield"
                    },
                    costGp = armor.costGp,
                    weightLb = armor.weightLb,
                    description = armor.description,
                    stats = buildList {
                        add("Armor Class" to acLabel)
                        if (armor.strRequirement > 0) add("Strength" to "${armor.strRequirement}")
                        if (armor.stealthDisadvantage) add("Stealth" to "Disadvantage")
                    },
                    armorDefId = armor.id,
                )
            )
        }

        EquipmentData.GEAR.forEach { gear ->
            add(
                CatalogItem(
                    id = "gear:${gear.id}",
                    name = gear.name,
                    type = CatalogItemType.GEAR,
                    category = gear.category,
                    costGp = gear.costGp,
                    weightLb = gear.weightLb,
                    description = gear.description,
                    stats = emptyList(),
                )
            )
        }
    }.sortedBy { it.name }

    val CATEGORIES: List<String> = ALL.map { it.category }.distinct().sorted()

    private val byId: Map<String, CatalogItem> = ALL.associateBy { it.id }
    private val byName: Map<String, CatalogItem> = ALL.associateBy { it.name.lowercase() }

    fun byId(id: String): CatalogItem? = byId[id]

    fun search(query: String, category: String? = null): List<CatalogItem> {
        val trimmed = query.trim().lowercase()
        return ALL.filter { item ->
            (category == null || item.category == category) &&
                (trimmed.isEmpty() ||
                    item.name.lowercase().contains(trimmed) ||
                    item.category.lowercase().contains(trimmed) ||
                    item.description.lowercase().contains(trimmed))
        }
    }

    /**
     * Finds the rulebook entry behind an inventory line. Prefers the stored def id, then falls
     * back to the name so gear added before this catalog existed still shows a description.
     * Quantity suffixes such as "Javelin (4)" are stripped before matching.
     */
    fun resolve(item: InventoryItem): CatalogItem? {
        item.weaponDefId?.let { id -> byId["weapon:$id"]?.let { return it } }
        item.armorDefId?.let { id -> byId["armor:$id"]?.let { return it } }

        val name = item.name.lowercase()
        byName[name]?.let { return it }

        val withoutQuantity = name.substringBefore(" (").trim()
        byName[withoutQuantity]?.let { return it }

        // "Arcane Focus (Crystal)" and similar should still find the base entry.
        return ALL.firstOrNull { it.name.lowercase() == withoutQuantity }
    }
}
