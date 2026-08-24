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

        val starting = ClassData.byId(classId)?.level1Features.orEmpty()
            .map { Entry(0, it.name, it.description) }
        val later = ProgressionData.forClass(classId)?.features.orEmpty()
            .filter { it.level in 2..level }
            .sortedBy { it.level }
            .map { Entry(it.level, it.name, it.description) }
        return starting + later
    }

    /** The subclass features earned in one class, empty when it has no subclass yet. */
    fun subclassFeatures(character: PlayerCharacter, classId: String): List<Entry> {
        val level = ClassLevels.levelIn(character, classId)
        val subclass = ClassLevels.subclassIn(character, classId)
            ?.let { SubclassData.byId(it) }
            ?: return emptyList()
        return subclass.features
            .filter { it.level <= level }
            .sortedBy { it.level }
            .map { Entry(it.level, it.name, it.description) }
    }
}
