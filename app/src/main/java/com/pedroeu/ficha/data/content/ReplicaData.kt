package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.ArmorCategory
import com.pedroeu.ficha.data.model.ArmorDef
import com.pedroeu.ficha.data.model.ItemRarity
import com.pedroeu.ficha.data.model.MagicItem
import com.pedroeu.ficha.data.model.WeaponDef

/**
 * What a magic item plan still needs to be told before it can be made.
 *
 * Half the Artificer's plans name the item outright — a Bag of Holding is a Bag of Holding.
 * The other half name a category: "Weapon, +1" is a +1 *something*, Repeating Shot goes on a
 * weapon that fires ammunition, and "Any Common Magic Item" is exactly as open as it sounds.
 * Those can't be made until the player says which item they mean, and the list they choose
 * from has to be the one the plan actually allows — offering a longbow for a plan that says
 * "any Sword" would be worse than offering nothing.
 *
 * The category is read from the item's own type line ("Weapon (any Ammunition weapon)"),
 * which is the same string printed on the sheet, so the filter and the rules text can never
 * disagree about what qualifies.
 */
object ReplicaData {

    /** What kind of thing the player is picking, which decides how it lands in the inventory. */
    enum class BaseKind { WEAPON, ARMOR, MAGIC_ITEM }

    /** One eligible base item. */
    data class BaseOption(
        val id: String,
        val name: String,
        val supporting: String = "",
        val weightLb: Double = 0.0,
    )

    /** The pick a plan still owes, with the list it may be answered from. */
    data class BaseChoice(
        val kind: BaseKind,
        val prompt: String,
        val options: List<BaseOption>,
    )

    // The weapon families the type lines name. Matching on the word "sword" would take a
    // Longsword and leave a Rapier, which is not what the rulebook means by "any Sword".
    private val SWORDS = setOf("greatsword", "longsword", "rapier", "scimitar", "shortsword")
    private val AXES = setOf("handaxe", "battleaxe", "greataxe")
    private val BOWS = setOf("shortbow", "longbow")
    private val CROSSBOWS = setOf("hand_crossbow", "light_crossbow", "heavy_crossbow")

    /**
     * The base item [plan] still needs, or null when it names one thing and can simply be made.
     */
    fun baseChoiceFor(plan: MagicItem): BaseChoice? {
        openEndedOptions(plan)?.let { options ->
            return BaseChoice(
                kind = BaseKind.MAGIC_ITEM,
                prompt = "Choose the item to make from this plan.",
                options = options.map { magicItem ->
                    BaseOption(
                        id = magicItem.id,
                        name = magicItem.name,
                        supporting = magicItem.subtitle,
                        weightLb = magicItem.weightLb,
                    )
                },
            )
        }

        val qualifier = qualifierOf(plan.kind)
        return when {
            plan.kind.startsWith("Weapon", ignoreCase = true) -> {
                val weapons = eligibleWeapons(qualifier)
                BaseChoice(
                    kind = BaseKind.WEAPON,
                    prompt = "Choose the weapon this plan is made from.",
                    options = weapons.map { weapon ->
                        BaseOption(
                            id = weapon.id,
                            name = weapon.name,
                            supporting = "${weapon.damageDice} ${weapon.damageType}",
                            weightLb = weapon.weightLb,
                        )
                    },
                ).takeIf { weapons.isNotEmpty() }
            }

            plan.kind.startsWith("Armor", ignoreCase = true) -> {
                val armor = eligibleArmor(qualifier)
                BaseChoice(
                    kind = BaseKind.ARMOR,
                    prompt = "Choose the armor this plan is made from.",
                    options = armor.map { piece ->
                        BaseOption(
                            id = piece.id,
                            name = piece.name,
                            supporting = categoryLabel(piece.category),
                            weightLb = piece.weightLb,
                        )
                    },
                ).takeIf { armor.isNotEmpty() }
            }

            else -> null
        }
    }

    /** The weapons a type line's qualifier allows. */
    fun eligibleWeapons(qualifier: String): List<WeaponDef> {
        val text = qualifier.lowercase()
        val all = EquipmentData.WEAPONS
        return when {
            text.isBlank() || text == "any" -> all
            !text.startsWith("any") -> all.filter { it.name.equals(qualifier, ignoreCase = true) }
            text.contains("thrown") -> all.filter { weapon ->
                weapon.properties.any { it.startsWith("Thrown", ignoreCase = true) }
            }
            text.contains("ammunition") -> all.filter { weapon ->
                weapon.properties.any { it.startsWith("Ammunition", ignoreCase = true) }
            }
            else -> {
                val families = buildSet {
                    if (text.contains("sword")) addAll(SWORDS)
                    if (text.contains("axe")) addAll(AXES)
                    if (text.contains("bow") && !text.contains("crossbow")) addAll(BOWS)
                    if (text.contains("crossbow")) addAll(CROSSBOWS)
                }
                val byFamily = if (families.isEmpty()) all else all.filter { it.id in families }
                // "any Slashing Sword" narrows the family further by damage type.
                val damageType = listOf("Slashing", "Piercing", "Bludgeoning")
                    .firstOrNull { text.contains(it.lowercase()) }
                if (damageType == null) byFamily
                else byFamily.filter { it.damageType.equals(damageType, ignoreCase = true) }
            }
        }
    }

    /** The armor a type line's qualifier allows. */
    fun eligibleArmor(qualifier: String): List<ArmorDef> {
        val text = qualifier.lowercase()
        val all = EquipmentData.ARMOR
        val shields = all.filter { it.category == ArmorCategory.SHIELD }
        val bodyArmor = all.filter { it.category != ArmorCategory.SHIELD }

        if (text == "shield") return shields
        if (text.isBlank() || text == "any") return bodyArmor

        val categories = buildSet {
            if (text.contains("light")) add(ArmorCategory.LIGHT)
            if (text.contains("medium")) add(ArmorCategory.MEDIUM)
            if (text.contains("heavy")) add(ArmorCategory.HEAVY)
        }
        if (categories.isEmpty()) {
            // A named piece, e.g. "Armor (Chain Shirt)" or "Armor (Plate)". The catalog spells
            // some of them out in full — "Plate Armor" for "Plate" — so a prefix match is what
            // reliably finds them.
            return bodyArmor.filter { piece ->
                piece.name.equals(qualifier, ignoreCase = true) ||
                    piece.name.startsWith(qualifier, ignoreCase = true)
            }
        }

        // "Medium or Heavy, except Hide" — the exclusion is named after the comma.
        val excluded = text.substringAfter("except", "").trim()
        return bodyArmor.filter { piece ->
            piece.category in categories &&
                (excluded.isBlank() || !piece.name.startsWith(excluded, ignoreCase = true))
        }
    }

    /**
     * The items an open-ended plan — "Any Common Magic Item" and its siblings — can produce.
     * Returns null for anything else, which is how the caller tells the two kinds apart.
     */
    fun openEndedOptions(plan: MagicItem): List<MagicItem>? = when (plan.id) {
        "plan_any_common" -> ALL_BROWSABLE.filter {
            it.rarity == ItemRarity.COMMON && !it.kind.startsWith("Potion") &&
                !it.kind.startsWith("Scroll")
        }
        "plan_any_uncommon_wondrous" -> ALL_BROWSABLE.filter {
            it.rarity == ItemRarity.UNCOMMON && it.kind == "Wondrous Item"
        }
        "plan_any_rare_wondrous" -> ALL_BROWSABLE.filter {
            it.rarity == ItemRarity.RARE && it.kind == "Wondrous Item"
        }
        else -> null
    }

    private val ALL_BROWSABLE: List<MagicItem> get() = MagicItemData.ALL

    /** The text inside the first pair of brackets on a type line, e.g. "any Sword". */
    private fun qualifierOf(kind: String): String =
        if (kind.contains('(')) kind.substringAfter('(').substringBefore(')').trim() else ""

    private fun categoryLabel(category: ArmorCategory): String = when (category) {
        ArmorCategory.LIGHT -> "Light Armor"
        ArmorCategory.MEDIUM -> "Medium Armor"
        ArmorCategory.HEAVY -> "Heavy Armor"
        ArmorCategory.SHIELD -> "Shield"
    }
}
