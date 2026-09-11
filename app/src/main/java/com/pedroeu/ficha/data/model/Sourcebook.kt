package com.pedroeu.ficha.data.model

/**
 * A book an option came from.
 *
 * Every species, background, class, subclass, feat, spell and magic item names one. A
 * character carries the set it was built with, and creation and level up only ever offer
 * options whose book is in that set — so a table running the core books alone never sees a
 * Lorwyn species in a picker, and a table running everything sees all of it.
 *
 * The id is what gets written to a saved character, so it must stay stable; the display name
 * can be reworded freely. Ordering is the order the selector shows them: the two core books,
 * then published settings, then playtest material last.
 */
enum class Sourcebook(
    val id: String,
    val displayName: String,
    /** Core books, on by default when a character is created. */
    val isCore: Boolean = false,
    /** True for playtest rules that may change or be withdrawn. */
    val isPlaytest: Boolean = false,
) {
    PHB("phb", "Player's Handbook", isCore = true),
    DMG("dmg", "Dungeon Master's Guide", isCore = true),

    EBERRON("eberron", "Eberron: Forge of the Artificer"),
    HEROES_OF_FAERUN("heroes_faerun", "Forgotten Realms: Heroes of Faerûn"),
    ADVENTURES_IN_FAERUN("adventures_faerun", "Forgotten Realms: Adventures in Faerûn"),
    NETHERILS_FALL("netherils_fall", "Netheril's Fall"),
    RAVENLOFT("ravenloft", "Ravenloft: The Horrors Within"),
    ASTARIONS_BOOK("astarions_book", "Astarion's Book of Hungers"),
    LORWYN("lorwyn", "Lorwyn: First Light"),
    ARCANA_UNLEASHED("arcana_unleashed", "Arcana Unleashed"),
    HELLFIRE_CLUB("hellfire_club", "Welcome to the Hellfire Club"),
    UNI_LOST_HORN("uni_lost_horn", "Uni and the Hunt for the Lost Horn"),

    /**
     * The monthly D&D Beyond drops, collapsed into one entry.
     *
     * They are a rolling series rather than a book, and a selector with a row per month would
     * be a wall of near-identical toggles for a handful of options each.
     */
    DDB_DROPS("ddb_drops", "D&D Beyond Drops"),

    UA_ARCANE("ua_arcane", "Unearthed Arcana 2025: Arcane Updates", isPlaytest = true),
    UA_HORROR("ua_horror", "Unearthed Arcana 2025: Horror Subclasses", isPlaytest = true),
    UA_VILLAINOUS("ua_villainous", "Unearthed Arcana 2026: Villainous Options", isPlaytest = true),
    UA_SUBCLASSES("ua_subclasses", "Unearthed Arcana 2025: Updated Subclasses", isPlaytest = true),
    ;

    companion object {
        val ALL: List<Sourcebook> = entries

        val CORE: Set<Sourcebook> = entries.filter { it.isCore }.toSet()

        /** Every book, which is what a character saved before books existed is treated as having. */
        val EVERYTHING: Set<Sourcebook> = entries.toSet()

        fun byId(id: String): Sourcebook? = entries.firstOrNull { it.id == id }

        /**
         * Reads a stored set of ids back.
         *
         * An empty set means the character predates this field, and gets everything rather
         * than nothing — the alternative would empty every picker on an existing sheet.
         * Unknown ids are dropped, so removing a book from this enum can't stop a character
         * from loading.
         */
        fun fromIds(ids: Set<String>): Set<Sourcebook> =
            if (ids.isEmpty()) EVERYTHING else ids.mapNotNull(::byId).toSet()
    }
}

/**
 * Anything that comes out of a book and can therefore be filtered out of a picker.
 *
 * Implemented by the content models rather than checked by name, so a new kind of content
 * cannot quietly skip the filter: it won't compile against [SourceFiltering.available] until
 * it says where it came from.
 */
interface FromSourcebook {
    val book: Sourcebook
}

object SourceFiltering {

    /** The entries of [items] that [enabled] allows. */
    fun <T : FromSourcebook> available(items: List<T>, enabled: Set<Sourcebook>): List<T> =
        items.filter { it.book in enabled }
}
