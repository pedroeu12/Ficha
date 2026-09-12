package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.Recharge
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.domain.ActionCost
import com.pedroeu.ficha.domain.CustomAttack

/** What a granted spell does to the character's list. */
enum class SpellGrantMode {
    /** "You always have X prepared" — on the sheet, prepared, costing nothing. */
    ALWAYS_PREPARED,

    /** "You learn X" — known, and prepared or not by the usual rules. */
    KNOWN,

    /**
     * "The spells on the table are added to the spell list of your spellcasting class."
     *
     * Every Dragonmark works this way and the difference is not cosmetic: treating one of
     * these as a grant hands a level 1 character a level 5 spell. The distinction existed
     * only as a comment until it cost an audit to separate.
     */
    ADDED_TO_CLASS_LIST,
}

enum class ProficiencyKind { SKILL, TOOL, LANGUAGE, ARMOR, WEAPON, SAVING_THROW }

/** The stats a rule can add to. */
enum class StatTarget {
    ARMOR_CLASS, MAX_HIT_POINTS, SPEED, INITIATIVE,
    ALL_SAVES, SAVE, SKILL_CHECK, SPELL_ATTACK, SPELL_SAVE_DC,
}

/**
 * When a modifier applies.
 *
 * Closed on purpose. A general escape hatch would let any condition be written as free text
 * and quietly computed by nobody — which is the disease, not the cure. [Descriptive] is the
 * one way out and it is deliberately visible: it applies no number and says so, so a test can
 * count how many there are and an author can see they have written prose rather than a rule.
 */
sealed interface Condition {
    data object Always : Condition
    data object Unarmored : Condition
    data object NoShield : Condition
    data object WearingArmor : Condition
    data object WhileRaging : Condition
    data object WhileWildShaped : Condition
    data object NotIncapacitated : Condition
    data object AtLowHitPoints : Condition

    /** A condition the engine does not compute. Shown to the player, never applied. */
    data class Descriptive(val text: String) : Condition
}

enum class SwapWhen { LEVEL_UP, SHORT_REST, LONG_REST }

/** How a summon's statblock is decided. */
sealed interface SummonPick {
    /** One creature, always the same. A Steel Defender, a Vestige Companion. */
    data class Fixed(val statblockId: String) : SummonPick

    /** A named set to choose from. Pact of the Chain's eight familiar forms. */
    data class FromList(val statblockIds: List<String>) : SummonPick

    /**
     * Anything matching a filter — "a Beast of Challenge Rating 1/4 or lower".
     *
     * [maxCr] is written as the rules write it ("1/4", "2") and compared numerically.
     */
    data class Filtered(
        val creatureType: String,
        val maxCr: String = "",
        val extra: String = "",
    ) : SummonPick
}

/** More or better creatures when cast with a higher-level slot. */
data class SummonScaling(val atSpellLevel: Int, val count: Formula)

/**
 * Everything a piece of content can do.
 *
 * This list is the whole contract. A rule that cannot be written as some combination of these
 * is a rule the engine does not handle, and that is meant to be obvious rather than papered
 * over with a special case somewhere.
 */
sealed interface Effect {

    // ---------------------------------------------------------------- Automatic grants

    data class GrantSpell(
        val spellId: String,
        val mode: SpellGrantMode = SpellGrantMode.ALWAYS_PREPARED,
    ) : Effect

    data class GrantProficiency(val kind: ProficiencyKind, val name: String) : Effect

    data class GrantExpertise(val skill: Skill) : Effect

    data class GrantFeat(val featId: String) : Effect

    data class GrantAttack(val attack: CustomAttack) : Effect

    data class GrantResistance(val damageType: String) : Effect

    data class GrantSense(val kind: String, val rangeFeet: Int) : Effect

    // ---------------------------------------------------------------- Calculated values

    data class ModifyStat(
        val target: StatTarget,
        val amount: Formula,
        val label: String,
        val condition: Condition = Condition.Always,
        /** For [StatTarget.SAVE] and [StatTarget.SKILL_CHECK], what it applies to. */
        val ability: Ability? = null,
        val skill: Skill? = null,
    ) : Effect

    /**
     * A thing that forces saving throws, and the ability setting its DC.
     *
     * A character carries several at once and they are not interchangeable: a Monk's Stunning
     * Strike is Wisdom while the same Monk's Magic Initiate cantrip is Intelligence.
     */
    data class ProvidesSaveDc(
        val id: String,
        val label: String,
        val ability: AbilityRef,
        val note: String = "",
    ) : Effect

    // ---------------------------------------------------------------- Decisions

    /** Decided once, when the element is gained. */
    data class AskOnGain(val choice: Choice) : Effect

    /** Decided each time the feature is used, and answered again the next time. */
    data class AskOnUse(
        val choice: Choice,
        /** The pool spent to use it, when there is one. */
        val poolId: String = "",
    ) : Effect

    /** The rules let this answer be revisited. */
    data class Swappable(val choiceId: String, val on: SwapWhen) : Effect

    // ---------------------------------------------------------------- Limited uses

    data class LimitedUses(
        val poolId: String,
        val max: Formula,
        val recharge: Recharge,
        val isPointPool: Boolean = false,
        val actionCost: ActionCost = ActionCost.OTHER,
        /** A spell this pool exists to cast for free, when that is what it is. */
        val castsSpellId: String = "",
    ) : Effect

    // ---------------------------------------------------------------- Summoning

    /**
     * Adds creatures to a summon another element already provides.
     *
     * Pact of the Chain does not summon anything of its own: it widens Find Familiar, which
     * the Warlock casts as normal, with eight special forms. Writing that as a second summon
     * would put two Find Familiars on the sheet, and writing it into Find Familiar itself
     * would offer an Imp to every Wizard who ever learned the spell.
     */
    data class ExtendsSummon(
        val summonId: String,
        val statblockIds: List<String>,
    ) : Effect

    data class Summons(
        val summonId: String,
        val label: String,
        val pick: SummonPick,
        val count: Formula = Formula.Flat(1),
        val duration: String = "",
        val concentration: Boolean = false,
        val scaling: List<SummonScaling> = emptyList(),
    ) : Effect
}

/** An effect together with the element that produced it, so the sheet can say where it came from. */
data class Applied<out T : Effect>(
    val effect: T,
    val element: RuleElement,
) {
    val sourceLabel: String get() = element.name
}
