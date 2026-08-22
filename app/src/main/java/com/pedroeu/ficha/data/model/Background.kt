package com.pedroeu.ficha.data.model

/**
 * A background that lets the player choose its origin feat rather than fixing one.
 *
 * Five backgrounds do this — "Survivor or a Dark Gift feat of your choice", "Choose one
 * Planar Pact feat" — and the app's rule is that nothing saying "of your choice" is ever
 * chosen for you. [categories] names the group the book points at; [alsoAllows] carries the
 * specific feats offered beside it.
 */
data class BackgroundFeatChoice(
    val label: String,
    val categories: Set<FeatCategory> = emptySet(),
    val alsoAllows: List<String> = emptyList(),
)

data class Background(
    val id: String,
    val name: String,
    val summary: String,
    /** Exactly 3 abilities the player distributes +2/+1/+1 across when picking this background. */
    val abilityOptions: List<Ability>,
    val skillProficiencies: List<Skill>,
    val toolProficiency: String,
    /**
     * The origin feat this grants outright, or — when [featChoice] is set — the one the book
     * recommends, used only as a fallback if the player somehow has no selection.
     */
    val featId: String,
    val equipment: List<String>,
    val startingGold: Int,
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.PHB,
    /**
     * Set when the book leaves the feat open; the creation flow then asks for it.
     *
     * Declared after the positional parameters end, because several backgrounds are still
     * written positionally and inserting a field mid-list silently reassigns their arguments.
     */
    val featChoice: BackgroundFeatChoice? = null,
) : FromSourcebook
