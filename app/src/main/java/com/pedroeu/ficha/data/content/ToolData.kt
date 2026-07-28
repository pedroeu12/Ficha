package com.pedroeu.ficha.data.content

/** Tool groups the rules refer to as "of your choice", so the app can offer the real list. */
object ToolData {

    val ARTISANS_TOOLS: List<String> = listOf(
        "Alchemist's Supplies",
        "Brewer's Supplies",
        "Calligrapher's Supplies",
        "Carpenter's Tools",
        "Cartographer's Tools",
        "Cobbler's Tools",
        "Cook's Utensils",
        "Glassblower's Tools",
        "Jeweler's Tools",
        "Leatherworker's Tools",
        "Mason's Tools",
        "Painter's Supplies",
        "Potter's Tools",
        "Smith's Tools",
        "Tinker's Tools",
        "Weaver's Tools",
        "Woodcarver's Tools",
    )

    val MUSICAL_INSTRUMENTS: List<String> = listOf(
        "Bagpipes",
        "Drum",
        "Dulcimer",
        "Flute",
        "Horn",
        "Lute",
        "Lyre",
        "Pan Flute",
        "Shawm",
        "Viol",
    )

    val GAMING_SETS: List<String> = listOf(
        "Dice Set",
        "Dragonchess Set",
        "Playing Card Set",
        "Three-Dragon Ante Set",
    )

    val OTHER_TOOLS: List<String> = listOf(
        "Cartographer's Tools",
        "Disguise Kit",
        "Forgery Kit",
        "Herbalism Kit",
        "Navigator's Tools",
        "Poisoner's Kit",
        "Thieves' Tools",
    )

    val ALL_TOOLS: List<String> =
        (ARTISANS_TOOLS + MUSICAL_INSTRUMENTS + GAMING_SETS + OTHER_TOOLS).distinct().sorted()

    /**
     * True when a background's tool entry names a group rather than a specific tool,
     * meaning the player still has a decision to make.
     */
    fun optionsForOpenEndedTool(toolProficiency: String): List<String>? = when {
        toolProficiency.contains("Artisan's Tools", ignoreCase = true) -> ARTISANS_TOOLS
        toolProficiency.contains("Musical Instrument", ignoreCase = true) -> MUSICAL_INSTRUMENTS
        toolProficiency.contains("Gaming Set", ignoreCase = true) -> GAMING_SETS
        else -> null
    }
}

/** Languages a character can learn when a feature grants one "of your choice". */
object LanguageData {
    val STANDARD: List<String> = listOf(
        "Common", "Common Sign Language", "Draconic", "Dwarvish", "Elvish",
        "Giant", "Gnomish", "Goblin", "Halfling", "Orc",
    )

    val RARE: List<String> = listOf(
        "Abyssal", "Celestial", "Deep Speech", "Druidic", "Infernal",
        "Primordial", "Sylvan", "Thieves' Cant", "Undercommon",
    )

    val ALL: List<String> = (STANDARD + RARE).distinct()
}
