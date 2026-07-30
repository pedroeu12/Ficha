package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData

/**
 * The character's full spell list: the ones they chose, plus the ones the rules simply hand
 * them.
 *
 * Granted spells are derived here rather than written into the character when it's created.
 * That way a Cleric who reaches level 5, or anyone who picks up a dragonmark feat, sees the
 * new spells immediately — and characters made before this existed stop being short of the
 * spells they should always have had.
 */
object CharacterSpells {

    /** A spell the rules give outright, resolved against the spell catalog. */
    data class GrantedSpell(
        val spell: com.pedroeu.ficha.data.model.SpellDef,
        val source: String,
        val alwaysPrepared: Boolean,
    )

    /**
     * Spells granted with no choice involved, filtered to the character's level and resolved
     * to real catalog entries. A grant naming a spell the catalog doesn't have is skipped
     * rather than shown as a blank row; [unresolvedGrants] exists so tests can catch that.
     */
    fun granted(character: PlayerCharacter): List<GrantedSpell> =
        grantsInEffect(character).mapNotNull { (sourceId, grant) ->
            SpellData.byId(grant.spellId)?.let { spell ->
                GrantedSpell(
                    spell = spell,
                    source = sourceLabel(sourceId, character),
                    alwaysPrepared = grant.alwaysPrepared,
                )
            }
        }.distinctBy { it.spell.id }

    /** Grant ids that don't match anything in the spell catalog. Should always be empty. */
    fun unresolvedGrants(character: PlayerCharacter): List<String> =
        grantsInEffect(character)
            .map { it.second.spellId }
            .filter { SpellData.byId(it) == null }
            .distinct()

    /**
     * Everything on the spell list, chosen and granted alike, with granted spells first so a
     * player can see at a glance what they didn't have to pick.
     */
    fun all(character: PlayerCharacter): List<KnownSpell> {
        val grantedSpells = granted(character).map { it.toKnownSpell() }
        val grantedIds = grantedSpells.map { it.id }.toSet()

        // A spell the player also picked or added by hand doesn't get listed twice; the
        // granted entry wins because it carries the source that explains where it came from.
        return grantedSpells + character.knownSpells.filterNot { it.id in grantedIds }
    }

    /** True when the sheet should refuse to delete a spell, because the rules keep granting it. */
    fun isGranted(character: PlayerCharacter, spellId: String): Boolean =
        granted(character).any { it.spell.id == spellId }

    private fun grantsInEffect(
        character: PlayerCharacter,
    ): List<Pair<String, SpellGrantData.Grant>> = SpellGrantData.forSources(
        classId = character.classId,
        subclassId = character.subclassId,
        speciesId = character.speciesId,
        lineageId = character.lineageId,
        featIds = character.featIds,
    ).filter { (_, grant) -> grant.level <= character.level }

    /** Turns a source id into something worth printing under the spell's name. */
    private fun sourceLabel(sourceId: String, character: PlayerCharacter): String = when (sourceId) {
        character.classId -> ClassData.byId(sourceId)?.name ?: sourceId
        character.subclassId -> SubclassData.byId(sourceId)?.name ?: sourceId
        character.speciesId -> SpeciesData.byId(sourceId)?.name ?: sourceId
        character.lineageId -> SpeciesData.byId(character.speciesId)
            ?.lineageOptions?.find { it.id == sourceId }?.name ?: sourceId

        else -> FeatData.byId(sourceId)?.name ?: sourceId
    }

    private fun GrantedSpell.toKnownSpell() = KnownSpell(
        id = spell.id,
        name = spell.name,
        level = spell.level,
        school = spell.school,
        description = spell.description,
        // "You always have it prepared" means it never has to be prepared by hand.
        prepared = alwaysPrepared,
        source = source,
    )
}
