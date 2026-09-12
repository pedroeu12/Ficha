package com.pedroeu.ficha.rules

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatChoiceData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.PassiveBonusData
import com.pedroeu.ficha.data.content.PerUseChoiceData
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.data.content.ResourceData
import com.pedroeu.ficha.data.content.SaveDcData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.domain.ActionCost
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter

/**
 * The existing tables, read as [RuleElement]s.
 *
 * Phase 0 of the migration: the engine is built and proven against the content that is
 * already here, before a line of that content is rewritten. Every adapter in this file is
 * meant to be deleted — as each source type is migrated to declare its own effects, the
 * adapter for it goes, and the day the file is empty the migration is finished.
 *
 * Nothing here invents rules. Where a table computes a number eagerly — a resource maximum,
 * say — the adapter wraps the number it already computed in [Formula.Flat] rather than
 * guessing at the expression behind it. The expressiveness is the point of Phase 2; Phase 0
 * only has to prove the shape fits.
 */
internal object Adapters {

    fun all(character: PlayerCharacter): List<RuleElement> = buildList {
        addAll(classFeatures(character))
        addAll(subclassFeatures(character))
        addAll(speciesTraits(character))
        addAll(feats(character))
        addAll(originChoices(character))
        addAll(spellGrants(character))
        addAll(pools(character))
        addAll(passiveBonuses(character))
        addAll(saveDcs(character))
        addAll(perUseChoices(character))
    }

    // ------------------------------------------------------------------ Features

    private fun classFeatures(character: PlayerCharacter): List<RuleElement> =
        ClassLevels.of(character).flatMap { entry ->
            val progression = ProgressionData.forClass(entry.classId) ?: return@flatMap emptyList()
            progression.features.map { feature ->
                RuleElement(
                    id = "class:${entry.classId}:${feature.level}:${slug(feature.name)}",
                    name = feature.name,
                    description = feature.description,
                    source = Source.Class(entry.classId),
                    gate = Gate(level = feature.level, levelScope = LevelScope.OWNING_CLASS),
                    effects = feature.choices.map { Effect.AskOnGain(it) } +
                        feature.choices.flatMap(::swapEffects),
                )
            }
        }

    private fun subclassFeatures(character: PlayerCharacter): List<RuleElement> =
        ClassLevels.of(character).flatMap { entry ->
            val subclass = entry.subclassId?.let { SubclassData.byId(it) }
                ?: return@flatMap emptyList()
            subclass.features.map { feature ->
                RuleElement(
                    id = "subclass:${subclass.id}:${feature.level}:${slug(feature.name)}",
                    name = feature.name,
                    description = feature.description,
                    source = Source.Subclass(subclass.id, entry.classId),
                    gate = Gate(level = feature.level, levelScope = LevelScope.OWNING_CLASS),
                    effects = feature.choices.map { Effect.AskOnGain(it) } +
                        feature.choices.flatMap(::swapEffects),
                    book = subclass.book,
                )
            }
        }

    private fun speciesTraits(character: PlayerCharacter): List<RuleElement> {
        val species = SpeciesData.byId(character.speciesId) ?: return emptyList()
        val fromTraits = species.traits.map { trait ->
            RuleElement(
                id = "species:${species.id}:${slug(trait.name)}",
                name = trait.name,
                description = trait.description,
                source = Source.Species(species.id),
                gate = Gate(level = 1, levelScope = LevelScope.CHARACTER),
                effects = trait.choices.map { Effect.AskOnGain(it) },
                book = species.book,
            )
        }
        val lineage = species.lineageOptions.find { it.id == character.lineageId }
        val fromLineage = lineage?.let {
            listOf(
                RuleElement(
                    id = "lineage:${it.id}",
                    name = it.name,
                    description = it.description,
                    source = Source.Lineage(it.id, species.id),
                    gate = Gate(level = 1, levelScope = LevelScope.CHARACTER),
                    effects = it.choices.map { c -> Effect.AskOnGain(c) },
                    book = species.book,
                )
            )
        }.orEmpty()
        return fromTraits + fromLineage
    }

    private fun feats(character: PlayerCharacter): List<RuleElement> =
        character.featIds.mapNotNull { featId ->
            val feat = FeatData.byId(featId) ?: return@mapNotNull null
            RuleElement(
                id = "feat:${feat.id}",
                name = feat.name,
                description = feat.description,
                source = Source.Feat(feat.id),
                gate = Gate(level = 1, levelScope = LevelScope.CHARACTER),
                effects = FeatChoiceData.choicesFor(feat.id, feat.name)
                    .map { Effect.AskOnGain(it) },
                book = feat.book,
            )
        }

    /**
     * Questions raised by a species, a background or a class outside any one feature.
     *
     * These have no feature to hang off — a Bard's three instruments, a background's open
     * tool — so they get an element of their own rather than being lost.
     */
    private fun originChoices(character: PlayerCharacter): List<RuleElement> =
        OriginChoices.all(
            speciesId = character.speciesId,
            lineageId = character.lineageId,
            classId = character.classId,
            classSelections = ChoiceResolver.answers(character),
            backgroundId = character.backgroundId,
            originSelections = character.originChoiceSelections,
            extraFeatIds = character.featIds,
            books = character.enabledSources,
        ).map { choice ->
            RuleElement(
                id = "origin:${choice.id}",
                name = choice.label,
                description = choice.prompt,
                source = Source.Background(choice.source),
                gate = Gate.ALWAYS,
                effects = listOf(Effect.AskOnGain(choice)) + swapEffects(choice),
            )
        }

    private fun swapEffects(choice: Choice): List<Effect> = buildList {
        if (choice.changeableOnLevelUp) {
            add(Effect.Swappable(choice.id, SwapWhen.LEVEL_UP))
        }
        if (choice.changeableOnRest) {
            add(Effect.Swappable(choice.id, SwapWhen.LONG_REST))
        }
    }

    // ------------------------------------------------------------------ Granted spells

    private fun spellGrants(character: PlayerCharacter): List<RuleElement> {
        val classes = ClassLevels.of(character)
        return classes.flatMapIndexed { index, entry ->
            SpellGrantData.forSources(
                classId = entry.classId,
                subclassId = entry.subclassId,
                speciesId = if (index == 0) character.speciesId else "",
                lineageId = if (index == 0) character.lineageId else null,
                featIds = if (index == 0) character.featIds else emptyList(),
                selections = ChoiceResolver.answers(character),
            ).map { (sourceId, grant) ->
                val isClassGrant = sourceId == entry.classId || sourceId == entry.subclassId
                RuleElement(
                    id = "grant:$sourceId:${grant.spellId}",
                    name = grant.spellId,
                    source = sourceFor(sourceId, entry.classId),
                    gate = Gate(
                        level = grant.level,
                        levelScope =
                            if (isClassGrant) LevelScope.OWNING_CLASS else LevelScope.CHARACTER,
                    ),
                    effects = listOf(
                        Effect.GrantSpell(
                            grant.spellId,
                            if (grant.alwaysPrepared) SpellGrantMode.ALWAYS_PREPARED
                            else SpellGrantMode.KNOWN,
                        )
                    ),
                )
            }
        }.distinctBy { it.id }
    }

    // ------------------------------------------------------------------ Limited uses

    /**
     * The resource tables, which compute their maxima eagerly.
     *
     * Wrapped as [Formula.Flat] of the number the table already worked out. Phase 2 is where
     * "uses equal to your Proficiency Bonus" becomes a formula rather than an integer.
     */
    private fun pools(character: PlayerCharacter): List<RuleElement> {
        val classes = ClassLevels.of(character)
        return classes.flatMapIndexed { index, entry ->
            ResourceData.forContext(
                ResourceData.Context(
                    classId = entry.classId,
                    subclassId = entry.subclassId,
                    speciesId = if (index == 0) character.speciesId else "",
                    lineageId = if (index == 0) character.lineageId else null,
                    featIds = if (index == 0) character.featIds else emptyList(),
                    level = entry.level,
                    proficiencyBonus = CharacterCalculations.proficiencyBonus(character),
                    abilityModifiers = CharacterCalculations.abilityModifiers(character),
                    characterLevel = character.level,
                )
            ).map { def ->
                RuleElement(
                    id = "pool:${def.id}",
                    name = def.name,
                    description = def.description,
                    source = sourceFor(def.id.substringBefore(':'), entry.classId),
                    gate = Gate.ALWAYS,
                    effects = listOf(
                        Effect.LimitedUses(
                            poolId = def.id,
                            max = Formula.Flat(def.max),
                            recharge = def.recharge,
                            isPointPool = def.isPointPool,
                            actionCost = ActionCost.of(def),
                            castsSpellId = def.spellId,
                        )
                    ) + def.options.map { option ->
                        Effect.AskOnUse(
                            choice = Choice(
                                id = "${def.id}:${option.id}",
                                label = option.name,
                                prompt = option.description,
                            ),
                            poolId = def.id,
                        )
                    },
                )
            }
        }.distinctBy { it.id }
    }

    // ------------------------------------------------------------------ Numbers

    private fun passiveBonuses(character: PlayerCharacter): List<RuleElement> {
        fun bonuses(sourceId: String, source: Source, list: List<PassiveBonusData.Bonus>) =
            list.map { bonus ->
                RuleElement(
                    id = "bonus:$sourceId:${slug(bonus.label)}:${bonus.target}",
                    name = bonus.label,
                    source = source,
                    gate = Gate.ALWAYS,
                    effects = listOf(
                        Effect.ModifyStat(
                            target = targetOf(bonus.target),
                            amount = if (bonus.perLevel) {
                                Formula.PerLevel(bonus.amount, LevelScope.CHARACTER)
                            } else {
                                Formula.Flat(bonus.amount)
                            },
                            label = bonus.label,
                            skill = bonus.skill,
                        )
                    ),
                )
            }

        val classes = ClassLevels.of(character)
        return buildList {
            addAll(bonuses(character.speciesId, Source.Species(character.speciesId),
                PassiveBonusData.forSpecies(character.speciesId)))
            character.lineageId?.let {
                addAll(bonuses(it, Source.Lineage(it, character.speciesId),
                    PassiveBonusData.forLineage(it)))
            }
            character.featIds.forEach {
                addAll(bonuses(it, Source.Feat(it), PassiveBonusData.forFeat(it)))
            }
            classes.forEach { entry ->
                addAll(bonuses(entry.classId, Source.Class(entry.classId),
                    PassiveBonusData.forClass(entry.classId)))
                entry.subclassId?.let { sub ->
                    addAll(bonuses(sub, Source.Subclass(sub, entry.classId),
                        PassiveBonusData.forSubclass(sub)))
                }
            }
        }.distinctBy { it.id }
    }

    private fun targetOf(target: PassiveBonusData.Target): StatTarget = when (target) {
        PassiveBonusData.Target.ARMOR_CLASS -> StatTarget.ARMOR_CLASS
        PassiveBonusData.Target.MAX_HIT_POINTS -> StatTarget.MAX_HIT_POINTS
        PassiveBonusData.Target.SPEED -> StatTarget.SPEED
        PassiveBonusData.Target.INITIATIVE -> StatTarget.INITIATIVE
        PassiveBonusData.Target.ALL_SAVES -> StatTarget.ALL_SAVES
    }

    private fun saveDcs(character: PlayerCharacter): List<RuleElement> {
        /**
         * The prefixes match what [com.pedroeu.ficha.domain.CharacterDcs] already uses.
         *
         * A character carries several DCs at once and they are not interchangeable — a Monk's
         * Stunning Strike is Wisdom while the same Monk's Magic Initiate cantrip is
         * Intelligence — so each is filed under where it came from.
         */
        fun element(
            prefix: String,
            dc: SaveDcData.DcSource?,
            source: Source,
        ): RuleElement? = dc?.let {
            val id = "$prefix:${it.id}"
            RuleElement(
                id = "dc:$id",
                name = it.label,
                description = it.note,
                source = source,
                gate = Gate.ALWAYS,
                effects = listOf(
                    Effect.ProvidesSaveDc(id, it.label, AbilityRef.Fixed(it.ability), it.note)
                ),
            )
        }

        return buildList {
            ClassLevels.of(character).forEach { entry ->
                val charClass = ClassData.byId(entry.classId)

                // A caster's spell save DC, which SaveDcData does not list: that table holds
                // the classes whose *features* force saves, and a Warlock's DC comes from its
                // Spellcasting rather than from a feature.
                charClass?.spellcastingAbility?.let { ability ->
                    add(
                        RuleElement(
                            id = "dc:class:${entry.classId}",
                            name = charClass.name,
                            description = "${charClass.name} spells.",
                            source = Source.Class(entry.classId),
                            gate = Gate.ALWAYS,
                            effects = listOf(
                                Effect.ProvidesSaveDc(
                                    "class:${entry.classId}",
                                    charClass.name,
                                    AbilityRef.Fixed(ability),
                                    "${charClass.name} spells.",
                                )
                            ),
                        )
                    )
                }

                SaveDcData.forClass(entry.classId)
                    ?.takeIf { charClass?.spellcastingAbility != it.ability }
                    ?.let { element("feature", it, Source.Class(entry.classId)) }
                    ?.let(::add)

                entry.subclassId?.let { sub ->
                    element("subclass", SaveDcData.forSubclass(sub),
                        Source.Subclass(sub, entry.classId))?.let(::add)
                }
            }
            element("species", SaveDcData.forSpecies(character.speciesId),
                Source.Species(character.speciesId))?.let(::add)
            element("lineage", SaveDcData.forLineage(character.lineageId),
                Source.Lineage(character.lineageId.orEmpty(), character.speciesId))?.let(::add)
            character.featIds.forEach { featId ->
                element("feat", SaveDcData.forFeat(featId), Source.Feat(featId))?.let(::add)
            }
        }.distinctBy { it.id }
    }

    // ------------------------------------------------------------------ Choose on use

    private fun perUseChoices(character: PlayerCharacter): List<RuleElement> =
        PerUseChoiceData.ALL.map { perUse ->
            RuleElement(
                id = "peruse:${perUse.id}",
                name = perUse.label,
                description = perUse.prompt,
                source = when {
                    perUse.subclassId.isNotBlank() ->
                        Source.Subclass(perUse.subclassId, classOf(perUse.subclassId))
                    perUse.speciesId.isNotBlank() -> Source.Species(perUse.speciesId)
                    else -> Source.Custom(perUse.id)
                },
                gate = Gate(
                    level = perUse.minLevel,
                    levelScope = if (perUse.speciesId.isNotBlank()) LevelScope.CHARACTER
                    else LevelScope.OWNING_CLASS,
                ),
                effects = listOf(
                    Effect.AskOnUse(
                        choice = Choice(
                            id = perUse.id,
                            label = perUse.label,
                            prompt = perUse.prompt,
                            options = perUse.options,
                            source = perUse.source,
                        ),
                        poolId = perUse.resourceId,
                    )
                ),
            )
        }

    // ------------------------------------------------------------------ Helpers

    private fun classOf(subclassId: String): String =
        SubclassData.byId(subclassId)?.classId.orEmpty()

    private fun sourceFor(sourceId: String, classId: String): Source = when {
        ClassData.byId(sourceId) != null -> Source.Class(sourceId)
        SubclassData.byId(sourceId) != null -> Source.Subclass(sourceId, classOf(sourceId))
        SpeciesData.byId(sourceId) != null -> Source.Species(sourceId)
        FeatData.byId(sourceId) != null -> Source.Feat(sourceId)
        else -> Source.Class(classId)
    }

    private fun slug(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
