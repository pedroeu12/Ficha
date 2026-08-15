package com.pedroeu.ficha.domain

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.ProgressionData
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

    /**
     * Classes that rebuild their prepared list whenever they finish a Long Rest, rather than
     * knowing a fixed set of spells. Bards, Sorcerers, and Warlocks are the exceptions — they
     * swap a spell when they gain a level, not each day.
     */
    private val PREPARES_DAILY = setOf(
        "artificer", "cleric", "druid", "paladin", "ranger", "wizard",
    )

    /**
     * Of those, the ones whose list to prepare from is the whole class list.
     *
     * A Cleric does not "know" a personal handful of Cleric spells the way a Sorcerer knows
     * its own — every spell on the list is theirs, and the class table only limits how many
     * they carry at a time. The same is true of the Artificer: *"You prepare the list of
     * level 1+ spells that are available for you to cast… choose additional Artificer
     * spells"*, drawn from the Artificer spell list and nothing narrower.
     *
     * The Wizard is deliberately absent. It prepares daily too, but from its spellbook
     * rather than the class list, and the spellbook is what [PlayerCharacter.knownSpells]
     * holds for a Wizard — so the plain chosen-spells path is already right for it.
     */
    private val PREPARES_FROM_CLASS_LIST = setOf(
        "artificer", "cleric", "druid", "paladin", "ranger",
    )

    /**
     * Classes that may replace one spell they know each time they gain a level.
     *
     * These are the three that never rebuild their list: *"Whenever you gain a Bard level,
     * you can replace one spell on your list with another Bard spell for which you have
     * spell slots."* A prepared caster has no need of the rule, because the whole list is
     * already theirs to rearrange each morning.
     */
    private val SWAPS_SPELL_ON_LEVEL_UP = setOf("bard", "sorcerer", "warlock")

    /**
     * Classes that swap a cantrip on a Long Rest rather than on gaining a level.
     *
     * The Artificer alone: *"Whenever you finish a Long Rest, you can replace one of your
     * cantrips from this feature with another Artificer cantrip of your choice."* Every
     * other caster does it on levelling.
     */
    private val SWAPS_CANTRIP_ON_LONG_REST = setOf("artificer")

    /** True when a Long Rest should offer to rebuild this character's prepared list. */
    fun preparesDaily(character: PlayerCharacter): Boolean =
        ClassLevels.of(character).any { it.classId in PREPARES_DAILY }

    /** True when gaining a level in this class lets the player trade one known spell for another. */
    fun swapsSpellOnLevelUp(classId: String): Boolean = classId in SWAPS_SPELL_ON_LEVEL_UP

    /** True when gaining a level in this class lets the player trade one cantrip for another. */
    fun swapsCantripOnLevelUp(classId: String): Boolean =
        classId !in SWAPS_CANTRIP_ON_LONG_REST && hasCantrips(classId)

    /** The classes whose cantrips a Long Rest may reshuffle, for the rest sheet to offer. */
    fun classesSwappingCantripsOnLongRest(character: PlayerCharacter): List<String> =
        ClassLevels.of(character)
            .map { it.classId }
            .filter { it in SWAPS_CANTRIP_ON_LONG_REST && hasCantrips(it) }
            .distinct()

    private fun hasCantrips(classId: String): Boolean =
        (ProgressionData.forClass(classId)?.cantripsKnownAt(20) ?: 0) > 0

    /**
     * The cantrips a rest may trade away: the ones the player chose from this class's list.
     *
     * A cantrip the rules grant outright — Tinker's Magic hands the Artificer Mending — is
     * not one of them. Trading it would only mean losing it until the next redraw put it
     * back, and the feature that grants it never offered a choice in the first place.
     */
    fun swappableCantrips(character: PlayerCharacter, classId: String): List<KnownSpell> {
        val granted = granted(character).map { it.spell.id }.toSet()
        val className = ClassData.byId(classId)?.name ?: classId
        val single = !ClassLevels.isMulticlassed(character)
        return character.knownSpells
            .filter { it.level == 0 && it.id !in granted }
            .filter { single || it.source.equals(className, ignoreCase = true) }
            .filter { classId in (SpellData.byId(it.id)?.classes ?: emptySet()) }
    }

    /** Everything on a class's cantrip list, for the other half of a trade. */
    fun cantripChoices(classId: String) = SpellData.cantripsForClass(classId)

    /**
     * The spells a Long Rest can prepare or set aside.
     *
     * For a Cleric, Druid, Paladin, Ranger, or Artificer this is the class's whole list up to
     * the highest level they have slots for — that is what "prepared caster" means, and
     * offering only the spells already written on the sheet turned every one of them into a
     * known-spells caster by accident. For everyone else it is what they chose.
     *
     * Cantrips are always available and granted spells are always prepared, so neither is
     * part of the daily decision.
     */
    fun preparable(character: PlayerCharacter): List<KnownSpell> {
        val granted = granted(character).map { it.spell.id }.toSet()
        val chosen = all(character).filter { it.level > 0 && it.id !in granted }
        val chosenIds = chosen.map { it.id }.toSet()

        val maxLevel = CharacterCalculations.maxSpellLevel(character)
        val fromClassLists = ClassLevels.of(character)
            .map { it.classId }
            .filter { it in PREPARES_FROM_CLASS_LIST }
            .flatMap { classId ->
                SpellData.forClassUpTo(classId, maxLevel).map { classId to it }
            }
            .filter { (_, spell) ->
                spell.level in 1..maxLevel && spell.id !in granted && spell.id !in chosenIds
            }
            .distinctBy { (_, spell) -> spell.id }
            .map { (classId, spell) ->
                KnownSpell(
                    id = spell.id,
                    name = spell.name,
                    level = spell.level,
                    school = spell.school,
                    description = spell.description,
                    // Not on the sheet yet, so not prepared. Preparing it is what puts it there.
                    prepared = false,
                    source = ClassData.byId(classId)?.name.orEmpty(),
                )
            }

        return (chosen + fromClassLists).sortedWith(compareBy({ it.level }, { it.name }))
    }

    /**
     * True when the spell comes from a list the character prepares from wholesale.
     *
     * Setting one of these aside can take it off the sheet entirely: the list it came from
     * is still there tomorrow, so nothing is lost, and the alternative is a Spells tab that
     * grows by one row every time the player changes their mind about a spell.
     */
    fun isOnPreparedClassList(character: PlayerCharacter, spellId: String): Boolean {
        val spell = SpellData.byId(spellId) ?: return false
        return ClassLevels.of(character)
            .map { it.classId }
            .any { it in PREPARES_FROM_CLASS_LIST && it in spell.classes }
    }

    /** How many of those are currently prepared, for checking against the class limit. */
    fun preparedCount(character: PlayerCharacter): Int =
        preparable(character).count { it.prepared }

    /** True when the sheet should refuse to delete a spell, because the rules keep granting it. */
    fun isGranted(character: PlayerCharacter, spellId: String): Boolean =
        granted(character).any { it.spell.id == spellId }

    private fun grantsInEffect(
        character: PlayerCharacter,
    ): List<Pair<String, SpellGrantData.Grant>> {
        // A subclass's spell list advances on the level in that class, so a Cleric 3 who
        // multiclasses into Fighter still has only their level 3 domain spells.
        val classes = ClassLevels.of(character)
        return classes.flatMapIndexed { index, entry ->
            SpellGrantData.forSources(
                classId = entry.classId,
                subclassId = entry.subclassId,
                speciesId = if (index == 0) character.speciesId else "",
                lineageId = if (index == 0) character.lineageId else null,
                featIds = if (index == 0) character.featIds else emptyList(),
                // A grant can hang off an answer rather than a source: the Primordial
                // Patron's list follows the element it chose, which changes on any level up.
                selections = ChoiceResolver.answers(character),
            ).filter { (sourceId, grant) ->
                // Species and feat grants key off total character level; class and subclass
                // grants key off the level in that class.
                val isClassGrant = sourceId == entry.classId || sourceId == entry.subclassId
                grant.level <= if (isClassGrant) entry.level else character.level
            }
        }.distinctBy { it.second.spellId }
    }

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
