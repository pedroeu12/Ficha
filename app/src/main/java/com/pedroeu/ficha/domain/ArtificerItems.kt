package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.EquipmentData
import com.pedroeu.ficha.data.content.MagicItemData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.ReplicaData
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.data.model.MagicItem

/** A plan the Artificer has learned, and what it still needs before it can be made. */
data class KnownPlan(
    val plan: MagicItem,
    /** The pick the plan owes — which weapon, which armor, which Common item — or null. */
    val base: ReplicaData.BaseChoice?,
) {
    val needsBase: Boolean get() = base != null
}

/** An item the character has made from a plan and is carrying today. */
data class MadeItem(
    val planId: String,
    val name: String,
    /** The base item chosen for an open-ended plan, or blank when the plan named one thing. */
    val baseId: String,
    /** Where it sits in the character's inventory, so the sheet can take it back out. */
    val inventoryIndex: Int,
)

/**
 * Replicate Magic Item: which plans the Artificer knows, and which of them are actually made.
 *
 * Knowing a plan and holding the item are two different things. The class learns a handful of
 * plans over twenty levels and keeps them forever, but can only have a few of the items in
 * existence at once — two at level 2, rising to six at 18 — and chooses which ones each day.
 * The app was recording the plans and stopping there, which left the player to remember what
 * they had made and to add it to their own inventory by hand.
 *
 * Making an item writes a real inventory line, tagged with the plan that made it. That is what
 * lets everything downstream work without knowing this feature exists: a made weapon carries
 * its base weapon's id, so it appears in Attacks with the right dice and the right bonus, and
 * a made shield carries its armor id, so it counts toward Armor Class. Setting the plan aside
 * removes exactly the line it added and nothing else.
 */
object ArtificerItems {

    private const val CLASS_ID = "artificer"

    /** The key the class table asks its plan question under, at every tier. */
    private val PLAN_CHOICE_ID = ProgressionData.PLAN_CHOICE_ID

    /**
     * How the question used to be asked: one key per tier, each adding a plan.
     *
     * Characters levelled before the tiers restated the whole set still carry these, so they
     * are read when there is no answer under the single key — otherwise a level 10 Artificer
     * would open the app to find their plans gone.
     */
    private const val LEGACY_PLAN_PREFIX = "replicate_plans_"

    /**
     * The Infused Items column: how many made items can exist at once.
     *
     * Two from level 2, then one more at each of 6, 10, 14, and 18.
     */
    fun maxMadeItems(artificerLevel: Int): Int = when {
        artificerLevel >= 18 -> 6
        artificerLevel >= 14 -> 5
        artificerLevel >= 10 -> 4
        artificerLevel >= 6 -> 3
        artificerLevel >= 2 -> 2
        else -> 0
    }

    /** Levels in the Artificer specifically, so a multiclass gets the right allowance. */
    fun artificerLevel(character: PlayerCharacter): Int =
        ClassLevels.of(character).find { it.classId == CLASS_ID }?.level ?: 0

    /** True when this character has the feature at all, so the sheet can hide the section. */
    fun hasFeature(character: PlayerCharacter): Boolean = artificerLevel(character) >= 2

    /** How many items this character may have made at once. */
    fun allowance(character: PlayerCharacter): Int = maxMadeItems(artificerLevel(character))

    /**
     * How many plans the character knows, from the Plans Known column.
     *
     * Four at level 2, then one more at each of 6, 10, 14, and 18 — but the whole set is
     * chosen afresh at each of those levels rather than added to, which is what lets a plan
     * taken at level 2 ever be given up.
     */
    fun maxPlans(artificerLevel: Int): Int = when {
        artificerLevel >= 18 -> 8
        artificerLevel >= 14 -> 7
        artificerLevel >= 10 -> 6
        artificerLevel >= 6 -> 5
        artificerLevel >= 2 -> 4
        else -> 0
    }

    /**
     * Every plan the character has learned, in name order.
     *
     * The newest answer wins outright. Each tier asks the question again with the whole set
     * on the table, so merging the tiers would leave an Artificer who traded a plan away at
     * level 6 still holding it.
     */
    fun knownPlans(character: PlayerCharacter): List<KnownPlan> {
        if (!hasFeature(character)) return emptyList()

        val current = ChoiceResolver.latestSelectionFor(character, PLAN_CHOICE_ID)
        val chosen = current.ifEmpty { legacyPlans(character) }

        return chosen
            .distinct()
            .mapNotNull { planId -> MagicItemData.byId(planId) }
            // A plan learned above the character's current level — after losing a level, say —
            // is kept but not offered, the same way an over-count mastery pick is.
            .filter { plan -> (plan.artificerPlanLevel ?: 0) <= artificerLevel(character) }
            .sortedBy { it.name }
            .map { plan -> KnownPlan(plan = plan, base = ReplicaData.baseChoiceFor(plan)) }
    }

    /** Plans recorded under the old per-tier keys, unioned as they were then meant to be. */
    private fun legacyPlans(character: PlayerCharacter): List<String> {
        val fromLevels = character.levelSelections
            .filterKeys { it.substringAfter(':').startsWith(LEGACY_PLAN_PREFIX) }
            .values
            .flatten()
        val fromFlatMaps = (character.classChoiceSelections + character.originChoiceSelections)
            .filterKeys { it.startsWith(LEGACY_PLAN_PREFIX) }
            .values
            .flatten()
        return fromLevels + fromFlatMaps
    }

    /** The items currently made, in inventory order. */
    fun madeItems(character: PlayerCharacter): List<MadeItem> =
        character.inventory.mapIndexedNotNull { index, item ->
            val planId = item.craftedFromPlanId ?: return@mapIndexedNotNull null
            MadeItem(
                planId = planId,
                name = item.name,
                baseId = item.weaponDefId ?: item.armorDefId ?: item.magicItemId.orEmpty(),
                inventoryIndex = index,
            )
        }

    /** How many more items can be made before the day's limit is reached. */
    fun remaining(character: PlayerCharacter): Int =
        (allowance(character) - madeItems(character).size).coerceAtLeast(0)

    /** True when this exact plan, made from this exact base, is already in the inventory. */
    fun isMade(character: PlayerCharacter, planId: String, baseId: String = ""): Boolean =
        madeItems(character).any { it.planId == planId && (baseId.isBlank() || it.baseId == baseId) }

    /**
     * Makes the item [planId] describes and puts it in the inventory.
     *
     * [baseId] answers the plan's own question — which weapon, which armor, which Common item
     * — and is ignored by plans that name one thing outright. Nothing happens if the day's
     * allowance is already spent or the same item is already made, so the caller can be
     * careless about double taps without ending up with two of something.
     */
    fun make(
        character: PlayerCharacter,
        planId: String,
        baseId: String = "",
    ): PlayerCharacter {
        if (remaining(character) <= 0) return character
        val plan = MagicItemData.byId(planId) ?: return character
        if (knownPlans(character).none { it.plan.id == planId }) return character

        val base = ReplicaData.baseChoiceFor(plan)
        if (base != null && base.options.none { it.id == baseId }) return character
        if (isMade(character, planId, baseId)) return character

        return character.copy(inventory = character.inventory + lineFor(plan, base, baseId))
    }

    /** Takes a made item back out of the inventory, freeing its place in the day's allowance. */
    fun unmake(
        character: PlayerCharacter,
        planId: String,
        baseId: String = "",
    ): PlayerCharacter {
        val target = madeItems(character)
            .firstOrNull { it.planId == planId && (baseId.isBlank() || it.baseId == baseId) }
            ?: return character

        return character.copy(
            inventory = character.inventory.filterIndexed { index, _ ->
                index != target.inventoryIndex
            }
        )
    }

    /** Every made item goes away at once, for the "start the day over" case. */
    fun unmakeAll(character: PlayerCharacter): PlayerCharacter =
        character.copy(inventory = character.inventory.filter { it.craftedFromPlanId == null })

    // ------------------------------------------------------------------ The inventory line

    private fun lineFor(
        plan: MagicItem,
        base: ReplicaData.BaseChoice?,
        baseId: String,
    ): InventoryItem {
        val option = base?.options?.find { it.id == baseId }

        return when (base?.kind) {
            // An open-ended plan produces the chosen item itself, not a variant of it.
            ReplicaData.BaseKind.MAGIC_ITEM -> {
                val made = MagicItemData.byId(baseId)
                InventoryItem(
                    name = made?.name ?: option?.name.orEmpty(),
                    weightLb = made?.weightLb ?: 0.0,
                    notes = made?.description.orEmpty(),
                    magicItemId = baseId,
                    craftedFromPlanId = plan.id,
                )
            }

            ReplicaData.BaseKind.WEAPON -> {
                val weapon = EquipmentData.weaponById(baseId)
                InventoryItem(
                    name = nameFor(plan, option?.name ?: baseId),
                    weightLb = weapon?.weightLb ?: 0.0,
                    notes = plan.description,
                    weaponDefId = baseId,
                    magicItemId = plan.id,
                    craftedFromPlanId = plan.id,
                )
            }

            ReplicaData.BaseKind.ARMOR -> {
                val armor = EquipmentData.armorById(baseId)
                InventoryItem(
                    name = nameFor(plan, option?.name ?: baseId),
                    weightLb = armor?.weightLb ?: 0.0,
                    notes = plan.description,
                    armorDefId = baseId,
                    magicItemId = plan.id,
                    craftedFromPlanId = plan.id,
                )
            }

            null -> InventoryItem(
                name = plan.name,
                weightLb = plan.weightLb,
                notes = plan.description,
                magicItemId = plan.id,
                craftedFromPlanId = plan.id,
            )
        }
    }

    /**
     * What the made item is called.
     *
     * A plan named for its category reads badly once the category is filled in — "Weapon, +1"
     * made from a longsword is a Longsword, +1 — so the category word is replaced. Plans with
     * a name of their own keep it and note the base in brackets: "Repeating Shot (Hand
     * Crossbow)".
     */
    private fun nameFor(plan: MagicItem, baseName: String): String {
        if (!plan.name.contains(",")) return "${plan.name} ($baseName)"

        val category = plan.name.substringBefore(",").trim()
        val isCategoryName = listOf("Weapon", "Armor", "Shield")
            .any { it.equals(category, ignoreCase = true) }

        return if (isCategoryName) {
            "$baseName, ${plan.name.substringAfter(",").trim()}"
        } else {
            "${plan.name} ($baseName)"
        }
    }
}
