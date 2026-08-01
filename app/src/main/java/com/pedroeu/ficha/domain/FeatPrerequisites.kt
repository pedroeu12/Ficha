package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.FeatPrerequisiteData
import com.pedroeu.ficha.data.content.FeatPrerequisiteData.RequiredFeature
import com.pedroeu.ficha.data.content.MasteryData
import com.pedroeu.ficha.data.model.CasterType
import com.pedroeu.ficha.data.content.ProgressionData

/** Whether a feat can be taken, and what's missing when it can't. */
data class FeatAvailability(
    val featId: String,
    val allowed: Boolean,
    /** Empty when allowed; otherwise what the character still needs, ready to show. */
    val missing: String,
)

/**
 * Checks a feat's prerequisites against the character.
 *
 * An unavailable feat is reported rather than hidden, the same way the multiclass picker
 * explains a class you can't join. A player choosing between paths needs to see that Death
 * Knight Ascension exists and wants two other feats first — hiding it just makes the path
 * look shorter than it is.
 */
object FeatPrerequisites {

    fun check(character: PlayerCharacter, featId: String): FeatAvailability {
        val requirement = FeatPrerequisiteData.forFeat(featId)
            ?: return FeatAvailability(featId, allowed = true, missing = "")

        val unmet = buildList {
            if (character.level < requirement.minLevel) {
                add("level ${requirement.minLevel}")
            }

            val hasNamedFeat = requirement.anyOf.isEmpty() ||
                requirement.anyOf.any { it in character.featIds }
            val hasFeature = requirement.feature?.let { hasFeature(character, it) } ?: true

            // A few feats accept the named feat *or* the feature; the rest want both.
            if (featId in FeatPrerequisiteData.FEATURE_OR_FEAT) {
                if (!hasNamedFeat && !hasFeature) {
                    add(nameOf(requirement.anyOf.first()) + " or " + requirement.feature!!.label)
                }
            } else {
                if (!hasNamedFeat) add(eitherOf(requirement.anyOf))
                if (!hasFeature) add(requirement.feature!!.label)
            }

            requirement.countFrom?.let { (needed, pool) ->
                val held = pool.count { it in character.featIds }
                if (held < needed) {
                    add("$needed feats from this path (you have $held)")
                }
            }
        }

        return FeatAvailability(
            featId = featId,
            allowed = unmet.isEmpty(),
            missing = if (unmet.isEmpty()) "" else "Needs " + unmet.joinToString(", and "),
        )
    }

    fun isAllowed(character: PlayerCharacter, featId: String): Boolean =
        check(character, featId).allowed

    private fun hasFeature(character: PlayerCharacter, feature: RequiredFeature): Boolean {
        val classes = ClassLevels.of(character)
        return when (feature) {
            RequiredFeature.WEAPON_MASTERY ->
                classes.any { MasteryData.hasWeaponMastery(it.classId) }

            RequiredFeature.SPELLCASTING -> classes.any { entry ->
                val caster = ProgressionData.forClass(entry.classId)?.casterType
                caster != null && caster != CasterType.NONE
            }

            RequiredFeature.MARTIAL_WEAPONS -> {
                val profs = classes
                    .mapNotNull { ClassData.byId(it.classId) }
                    .flatMap { it.weaponProficiencies } + character.weaponProficiencies
                profs.any { it.equals("Martial", ignoreCase = true) }
            }
        }
    }

    private fun nameOf(featId: String): String =
        FeatData.byId(featId)?.name ?: featId

    private fun eitherOf(featIds: List<String>): String = when {
        featIds.size == 1 -> "the ${nameOf(featIds.first())} feat"
        else -> "any of these feats: " + featIds.joinToString(", ") { nameOf(it) }
    }
}
