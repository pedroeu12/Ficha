package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.data.model.Skill

/**
 * What it takes to multiclass into a class, and the rather smaller set of proficiencies you
 * get when you do.
 *
 * Taking a class as your second is not the same as starting with it. The prerequisites keep a
 * character from dipping into a class their scores don't support — in *and* out, since you
 * must also meet your current class's requirement to leave it — and the proficiencies granted
 * are deliberately narrower than a starting character's.
 */
object MulticlassData {

    /**
     * One class's multiclass entry.
     *
     * [prerequisites] must all be met. [orPrerequisites] is for the Fighter, where either of
     * two scores will do.
     */
    data class Entry(
        val classId: String,
        val prerequisites: List<Ability> = emptyList(),
        val orPrerequisites: List<Ability> = emptyList(),
        val armorTraining: List<String> = emptyList(),
        val weaponProficiencies: List<String> = emptyList(),
        val toolProficiencies: List<String> = emptyList(),
        /** Skills you may pick one of, when the class grants one on multiclassing. */
        val skillOptions: List<Skill> = emptyList(),
        val skillCount: Int = 0,
        val note: String = "",
    ) {
        /** The score a character needs, written for the sheet. */
        val prerequisiteLabel: String
            get() = when {
                orPrerequisites.isNotEmpty() ->
                    orPrerequisites.joinToString(" or ") { "${it.fullName} 13" }

                prerequisites.isEmpty() -> "None"
                else -> prerequisites.joinToString(" and ") { "${it.fullName} 13" }
            }
    }

    /** The minimum score every multiclass prerequisite asks for. */
    const val REQUIRED_SCORE = 13

    private val ENTRIES: List<Entry> = listOf(
        Entry(
            classId = "artificer",
            prerequisites = listOf(Ability.INT),
            armorTraining = listOf("Light", "Medium", "Shields"),
            toolProficiencies = listOf("Thieves' Tools", "Tinker's Tools"),
        ),
        Entry(
            classId = "barbarian",
            prerequisites = listOf(Ability.STR),
            armorTraining = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
        ),
        Entry(
            classId = "bard",
            prerequisites = listOf(Ability.CHA),
            armorTraining = listOf("Light"),
            skillOptions = Skill.ALL,
            skillCount = 1,
            note = "You also gain proficiency with one Musical Instrument of your choice.",
        ),
        Entry(
            classId = "cleric",
            prerequisites = listOf(Ability.WIS),
            armorTraining = listOf("Light", "Medium", "Shields"),
        ),
        Entry(
            classId = "druid",
            prerequisites = listOf(Ability.WIS),
            armorTraining = listOf("Light", "Medium", "Shields"),
            note = "Druids will not wear armor or use a Shield made of metal.",
        ),
        Entry(
            classId = "fighter",
            orPrerequisites = listOf(Ability.STR, Ability.DEX),
            armorTraining = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
        ),
        Entry(
            classId = "monk",
            prerequisites = listOf(Ability.DEX, Ability.WIS),
            weaponProficiencies = listOf("Simple", "Martial weapons with the Light property"),
        ),
        Entry(
            classId = "paladin",
            prerequisites = listOf(Ability.STR, Ability.CHA),
            armorTraining = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
        ),
        Entry(
            classId = "ranger",
            prerequisites = listOf(Ability.DEX, Ability.WIS),
            armorTraining = listOf("Light", "Medium", "Shields"),
            weaponProficiencies = listOf("Simple", "Martial"),
            skillOptions = listOf(
                Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INSIGHT, Skill.INVESTIGATION,
                Skill.NATURE, Skill.PERCEPTION, Skill.STEALTH, Skill.SURVIVAL,
            ),
            skillCount = 1,
        ),
        Entry(
            classId = "rogue",
            prerequisites = listOf(Ability.DEX),
            armorTraining = listOf("Light"),
            toolProficiencies = listOf("Thieves' Tools"),
            skillOptions = listOf(
                Skill.ACROBATICS, Skill.ATHLETICS, Skill.DECEPTION, Skill.INSIGHT,
                Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.PERCEPTION, Skill.PERFORMANCE,
                Skill.PERSUASION, Skill.SLEIGHT_OF_HAND, Skill.STEALTH,
            ),
            skillCount = 1,
        ),
        Entry(
            classId = "sorcerer",
            prerequisites = listOf(Ability.CHA),
            note = "Sorcerers grant no proficiencies when taken as a multiclass.",
        ),
        Entry(
            classId = "warlock",
            prerequisites = listOf(Ability.CHA),
            armorTraining = listOf("Light"),
            weaponProficiencies = listOf("Simple"),
        ),
        Entry(
            classId = "wizard",
            prerequisites = listOf(Ability.INT),
            note = "Wizards grant no proficiencies when taken as a multiclass.",
        ),
    )

    private val byId: Map<String, Entry> = ENTRIES.associateBy { it.classId }

    fun forClass(classId: String): Entry? = byId[classId]

    fun all(): List<Entry> = ENTRIES

    /** Ids these entries are keyed by, so a test can check them against the real class list. */
    fun sourceIds(): Set<String> = byId.keys
}
