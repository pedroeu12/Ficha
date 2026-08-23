package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.ResourceDef
import com.pedroeu.ficha.data.model.ResourceOption
import com.pedroeu.ficha.domain.ActionCost
import com.pedroeu.ficha.domain.CharacterResources
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.i18n.PortugueseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Splitting the limited-use trackers by what they cost on your turn.
 *
 * The list answers "what do I still have?"; the split is what makes it answer "it's my turn,
 * what can I do?". A round gives you one Action, one Bonus Action and one Reaction, so those
 * are the three buckets, and everything that costs none of them — Action Surge, a pool of
 * healing dice, a spell you cast over ten minutes — belongs in the fourth rather than being
 * guessed into one of the first three.
 */
class ActionCostTest {

    private fun pool(
        name: String = "Thing",
        description: String = "",
        notes: String = "",
        actionCost: String = "",
        spellId: String = "",
        options: List<ResourceOption> = emptyList(),
    ) = ResourceDef(
        id = "t:thing",
        name = name,
        max = 1,
        recharge = Recharge.LONG_REST,
        source = "Test",
        description = description,
        notes = notes,
        actionCost = actionCost,
        spellId = spellId,
        options = options,
    )

    // ------------------------------------------------------------------ Reading a label

    @Test
    fun `a label names its cost even with words around it`() {
        assertEquals(ActionCost.BONUS_ACTION, ActionCost.ofLabel("Bonus Action"))
        assertEquals(ActionCost.BONUS_ACTION, ActionCost.ofLabel("Bonus Action to drink"))
        assertEquals(ActionCost.REACTION, ActionCost.ofLabel("Reaction"))
        assertEquals(ActionCost.REACTION, ActionCost.ofLabel("None (Reaction)"))
        assertEquals(ActionCost.ACTION, ActionCost.ofLabel("Magic action"))
        assertEquals(ActionCost.ACTION, ActionCost.ofLabel("Action or Ritual"))
        assertEquals(ActionCost.ACTION, ActionCost.ofLabel("1 Action"))
    }

    @Test
    fun `a label that names when rather than what costs nothing of its own`() {
        assertEquals(ActionCost.OTHER, ActionCost.ofLabel("Part of the Attack action"))
        assertEquals(ActionCost.OTHER, ActionCost.ofLabel("While raging"))
        assertEquals(ActionCost.OTHER, ActionCost.ofLabel("No action"))
        assertEquals(ActionCost.OTHER, ActionCost.ofLabel("10 minutes"))
        assertEquals(ActionCost.OTHER, ActionCost.ofLabel(""))
    }

    // ------------------------------------------------------------------ Reading rules text

    @Test
    fun `the first cost the text names is the cost`() {
        // The books lead with it, so a Bonus Action that goes on to mention a Reaction is
        // still a Bonus Action.
        assertEquals(
            ActionCost.BONUS_ACTION,
            ActionCost.ofText(
                "As a Bonus Action, you imbue your aura. A creature that starts its turn " +
                    "there can take a Reaction to move away.",
            ),
        )
        assertEquals(
            ActionCost.REACTION,
            ActionCost.ofText("When a creature hits you, you can take a Reaction to halve the damage."),
        )
        assertEquals(
            ActionCost.ACTION,
            ActionCost.ofText("As a Magic action, call on your deity."),
        )
    }

    @Test
    fun `text that names no cost is not forced into one`() {
        assertEquals(
            ActionCost.OTHER,
            ActionCost.ofText("Take one additional action on your turn, once per Short or Long Rest."),
        )
        assertEquals(
            ActionCost.OTHER,
            ActionCost.ofText("Once per turn when you hit a creature, you can deal extra damage."),
        )
        assertEquals(ActionCost.OTHER, ActionCost.ofText(""))
    }

    // ------------------------------------------------------------------ A whole pool

    @Test
    fun `a pool that states its cost is read straight`() {
        assertEquals(ActionCost.ACTION, ActionCost.of(pool(actionCost = "Magic action")))
        assertEquals(
            "the stated cost wins over a paraphrase that lost it",
            ActionCost.BONUS_ACTION,
            ActionCost.of(pool(description = "Become a beacon of light.", actionCost = "Bonus Action")),
        )
    }

    @Test
    fun `a free casting costs whatever the spell costs`() {
        assertEquals(ActionCost.BONUS_ACTION, ActionCost.of(pool(spellId = "divine_smite")))
        assertEquals(ActionCost.ACTION, ActionCost.of(pool(spellId = "polymorph")))
        // Find Steed takes ten minutes, which is no action in any round.
        assertEquals(ActionCost.OTHER, ActionCost.of(pool(spellId = "find_steed")))
    }

    @Test
    fun `a pool with no wording of its own falls back to what it pays for`() {
        val agreeing = pool(
            options = listOf(
                ResourceOption("a", "Divine Spark", actionType = "Magic action", description = "x"),
                ResourceOption("b", "Turn Undead", actionType = "Magic action", description = "x"),
            ),
        )
        assertEquals(ActionCost.ACTION, ActionCost.of(agreeing))

        // Focus Points buy a Bonus Action, a Reaction and a rider on an attack. The pool has
        // no single cost, and pretending otherwise would be worse than saying so.
        val disagreeing = pool(
            options = listOf(
                ResourceOption("a", "Flurry of Blows", actionType = "Bonus Action", description = "x"),
                ResourceOption("b", "Deflect Attacks", actionType = "Reaction", description = "x"),
            ),
        )
        assertEquals(ActionCost.OTHER, ActionCost.of(disagreeing))
    }

    // ------------------------------------------------------------------ Against the catalogue

    private fun costOf(classId: String, subclassId: String, resourceId: String): ActionCost {
        val character = PlayerCharacter(
            id = "t", name = "T", speciesId = "human", classId = classId,
            subclassId = subclassId, backgroundId = "soldier", level = 20,
            baseAbilityScores = Ability.ALL.associate { it.name to 16 },
        )
        val def = CharacterResources.definitions(character).first { it.id == resourceId }
        return ActionCost.of(def)
    }

    @Test
    fun `the pools a player reaches for most land where they belong`() {
        assertEquals(ActionCost.BONUS_ACTION, costOf("barbarian", "berserker", "barbarian:rage"))
        assertEquals(ActionCost.BONUS_ACTION, costOf("bard", "lore", "bard:inspiration"))
        assertEquals(ActionCost.BONUS_ACTION, costOf("fighter", "champion", "fighter:second_wind"))
        assertEquals(ActionCost.BONUS_ACTION, costOf("druid", "moon", "druid:wild_shape"))
        assertEquals(ActionCost.BONUS_ACTION, costOf("paladin", "devotion", "paladin:lay_on_hands"))
        assertEquals(ActionCost.ACTION, costOf("cleric", "life_domain", "cleric:channel_divinity"))
        assertEquals(ActionCost.ACTION, costOf("cleric", "life_domain", "cleric:divine_intervention"))
        assertEquals(ActionCost.REACTION, costOf("cleric", "light_domain", "light:warding_flare"))
        assertEquals(ActionCost.REACTION, costOf("barbarian", "world_tree", "world_tree:branches"))
        // Action Surge costs nothing; a pool of Focus Points buys three different costs.
        assertEquals(ActionCost.OTHER, costOf("fighter", "champion", "fighter:action_surge"))
        assertEquals(ActionCost.OTHER, costOf("monk", "open_hand", "monk:focus"))
    }

    @Test
    fun `most of the catalogue is classified, so the split is worth showing`() {
        val seen = mutableMapOf<String, ActionCost>()
        ClassData.ALL.forEach { charClass ->
            (SubclassData.forClass(charClass.id) + listOf(null)).forEach { subclass ->
                val character = PlayerCharacter(
                    id = "t", name = "T", speciesId = "human", classId = charClass.id,
                    subclassId = subclass?.id ?: "", backgroundId = "soldier", level = 20,
                    baseAbilityScores = Ability.ALL.associate { it.name to 16 },
                )
                CharacterResources.definitions(character).forEach { def ->
                    seen.putIfAbsent(def.id, ActionCost.of(def))
                }
            }
        }

        assertTrue("nothing was gathered, so this proves nothing", seen.size > 100)
        val named = seen.values.count { it != ActionCost.OTHER }
        assertTrue(
            "only $named of ${seen.size} pools name an action; the split stops being useful",
            named >= seen.size / 2,
        )
    }

    // ------------------------------------------------------------------ The headings

    @Test
    fun `every heading is in turn order and translated`() {
        assertEquals(
            listOf(
                ActionCost.ACTION,
                ActionCost.BONUS_ACTION,
                ActionCost.REACTION,
                ActionCost.OTHER,
            ),
            ActionCost.ORDER,
        )
        ActionCost.ORDER.forEach { cost ->
            assertTrue(
                "\"${cost.label}\" would show in English on a Portuguese sheet",
                PortugueseStrings.knownKeys().contains(cost.label),
            )
        }
    }
}
