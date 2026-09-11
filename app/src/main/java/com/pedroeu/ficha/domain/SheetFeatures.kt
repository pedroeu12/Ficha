package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.SubclassData

/**
 * What the Features page lists, per class.
 *
 * The phone and the tablet draw this differently but decide it identically, and both used to
 * work it out from the plain `classId` and `subclassId` fields — which is how a multiclassed
 * character came to see the class they started as and nothing else. Deciding it once, here,
 * is what keeps the two screens from disagreeing about it again.
 */
object SheetFeatures {

    /**
     * One feature as the page lists it.
     *
     * [level] is 0 for the ones every member of the class has from the start, which are shown
     * without a "Level N —" prefix because there is no level at which they arrived.
     */
    data class Entry(
        val level: Int,
        val name: String,
        val description: String,
        /** The choices this feature asks, by id, so a restatement of one can be spotted. */
        val choiceIds: List<String> = emptyList(),
    )

    /**
     * The class-table features earned in one class, starting features first.
     *
     * Filtered by the level in *that* class rather than the character's total: a Fighter 5 /
     * Wizard 3 has a level 5 Fighter's features and a level 3 Wizard's, not level 8 of either.
     */
    fun classFeatures(character: PlayerCharacter, classId: String): List<Entry> {
        val level = ClassLevels.levelIn(character, classId)
        if (level <= 0) return emptyList()

        val progression = ProgressionData.forClass(classId)?.features.orEmpty()
        // The progression table alone. It carries level 1 as well, and the class's own
        // level1Features list is an older, shorter copy of the same features — so reading
        // both printed Rage, Spellcasting, Sneak Attack and the rest twice on every sheet,
        // once in the book's words and once in a summary written before the audit.
        if (progression.isNotEmpty()) {
            return collapseRestatements(
                progression
                    .filter { it.level <= level }
                    .sortedBy { it.level }
                    .map { Entry(it.level, it.name, it.description, it.choices.map { c -> c.id }) }
            )
        }

        // A class with no progression table still has to show something.
        return ClassData.byId(classId)?.level1Features.orEmpty()
            .map { Entry(1, it.name, it.description) }
    }

    /**
     * Folds away a feature that is the same feature again, said louder.
     *
     * A Warlock's table lists "Eldritch Invocations" at levels 1, 2, 5, 7, 9, 12, 15 and 18,
     * and a martial class lists "Weapon Mastery" three or four times. Each of those is the
     * same choice restated with a bigger number — they share a choice id — so the page shows
     * the first and lets the choice itself say how many you now know. Features that merely
     * share a name keep their own rows: a Rogue's Expertise at 1 and at 6 are different
     * picks, and they have different choice ids to prove it.
     */
    private fun collapseRestatements(entries: List<Entry>): List<Entry> {
        val seen = mutableSetOf<Pair<String, List<String>>>()
        return entries.filter { entry ->
            if (entry.choiceIds.isEmpty()) return@filter true
            seen.add(entry.name to entry.choiceIds)
        }
    }

    /** The subclass features earned in one class, empty when it has no subclass yet. */
    fun subclassFeatures(character: PlayerCharacter, classId: String): List<Entry> {
        val level = ClassLevels.levelIn(character, classId)
        val subclass = ClassLevels.subclassIn(character, classId)
            ?.let { SubclassData.byId(it) }
            ?: return emptyList()
        return collapseRestatements(
            subclass.features
                .filter { it.level <= level }
                .sortedBy { it.level }
                .map { Entry(it.level, it.name, it.description, it.choices.map { c -> c.id }) }
        )
    }
}
