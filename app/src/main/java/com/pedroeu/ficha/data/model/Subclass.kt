package com.pedroeu.ficha.data.model

data class SubclassFeature(
    val level: Int,
    val name: String,
    val description: String,
    val choices: List<Choice> = emptyList(),
)

data class Subclass(
    val id: String,
    val classId: String,
    val name: String,
    val summary: String,
    val features: List<SubclassFeature>,
    /**
     * The book this comes from. Shown next to the name so playtest options are never mistaken
     * for published ones, and checked against the character's chosen books before the
     * subclass is offered at all.
     */
    override val book: Sourcebook = Sourcebook.PHB,
    /**
     * The exact printing, when [book] is too coarse to name it.
     *
     * The Villainous Options material was published three times and the wording differs
     * between them, so a subclass has to be able to say which one it carries even though all
     * three are one toggle in the book selector. Empty means [book]'s own name says it.
     */
    val printing: String = "",
    /**
     * Spellcasting the subclass grants where the class has none.
     *
     * The Eldritch Knight and the Arcane Trickster are the only two, and leaving this off the
     * model is precisely how they came to have no spell slots, no prepared count, no save DC
     * and no level-up prompts: every one of those reads the *class* table, a Fighter's says
     * NONE, and nothing thought to ask the subclass.
     */
    val casterType: CasterType = CasterType.NONE,
    /** The ability that casts them, when [casterType] is not NONE. */
    val spellcastingAbility: Ability? = null,
    /**
     * The class list the subclass draws on, which needn't be its own class's — both third
     * casters learn Wizard spells.
     */
    val spellListClassId: String = "",
    /** Class level -> cantrips known. Sparse; the highest entry at or below applies. */
    val cantripsKnown: Map<Int, Int> = emptyMap(),
    /** Class level -> prepared spell count, indexed 1..20. */
    val preparedSpells: List<Int> = emptyList(),
    /**
     * Schools the subclass is limited to, when it is limited at all.
     *
     * An Eldritch Knight learns Abjuration and Evocation; an Arcane Trickster Illusion and
     * Enchantment. Empty means the whole list, which is every other caster.
     */
    val spellSchools: Set<String> = emptySet(),
    /**
     * Class levels at which the school restriction is lifted for that level's pick.
     *
     * *"except that the spells you gain at levels 8, 14, and 20 can be from any school"* —
     * a clause easy to leave out and impossible to notice missing, since it only ever shows
     * up as a picker that is quietly too short on three levels out of twenty.
     */
    val freeSchoolLevels: Set<Int> = emptySet(),
) : FromSourcebook {
    val isPlaytest: Boolean get() = book.isPlaytest

    /** What to credit on screen: the exact printing when there is one, else the book. */
    val attribution: String get() = printing.ifEmpty { book.displayName }

    val isSpellcaster: Boolean get() = casterType != CasterType.NONE

    fun featuresAt(level: Int): List<SubclassFeature> = features.filter { it.level == level }

    fun cantripsKnownAt(level: Int): Int =
        cantripsKnown.entries.filter { it.key <= level }.maxByOrNull { it.key }?.value ?: 0

    fun preparedSpellsAt(level: Int): Int = preparedSpells.getOrNull(level - 1) ?: 0
}
