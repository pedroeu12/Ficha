package com.pedroeu.ficha.data.model

/**
 * The group a feat belongs to, as the books print them.
 *
 * This is what lets a background say "a Dark Gift feat of your choice" and have the app
 * actually offer that set, rather than picking one and calling it fixed. [ORIGIN] and
 * [GENERAL] also decide when a feat may be taken at all.
 */
enum class FeatCategory(val displayName: String) {
    ORIGIN("Origin Feat"),
    GENERAL("General Feat"),
    EPIC_BOON("Epic Boon"),
    DRAGONMARK("Dragonmark Feat"),
    PLANAR_PACT("Planar Pact Feat"),
    DARK_GIFT("Dark Gift"),
}

data class Feat(
    val id: String,
    val name: String,
    val description: String,
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.PHB,
    /**
     * Which group the book prints it under.
     *
     * Defaults to [FeatCategory.ORIGIN] only because the origin list is where the first of
     * these were declared; every entry sets it explicitly through the lists in FeatData.
     */
    val category: FeatCategory = FeatCategory.ORIGIN,
) : FromSourcebook
