package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.FeatCategory
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.data.model.Skill

/**
 * Every grant outside the level-1 class table that the rulebook words as "of your choice".
 *
 * The app's rule is that nothing gets auto-picked, so each of these becomes a real prompt.
 * This is what fixes backgrounds like Sage, whose Magic Initiate feat grants two cantrips
 * and a level 1 spell that the player must choose.
 */
object OriginChoices {

    private fun spellOptions(classId: String, level: Int): List<ChoiceOption> =
        SpellData.forClass(classId, level).map { spell ->
            ChoiceOption(
                id = spell.id,
                name = spell.name,
                description = spell.description,
                supporting = spell.subtitle,
                book = spell.book,
            )
        }

    /**
     * Every choice a feat forces, whatever route the feat arrived by. The table lives in
     * [FeatChoiceData] so a feat asks the same questions whether it came from a background,
     * a species trait, or an Ability Score Improvement.
     */
    fun forFeat(featId: String, featName: String): List<Choice> =
        FeatChoiceData.choicesFor(featId, featName)

    /** The same, resolved from the feat id alone. */
    fun forFeat(featId: String): List<Choice> =
        forFeat(featId, FeatData.byId(featId)?.name ?: featId)

    /**
     * Choices for a set of feats, following each one into the feats *it* grants.
     *
     * A Human's Versatile trait offers an Origin feat; picking Magic Initiate there has to
     * raise Magic Initiate's own cantrip and spell prompts. Nothing in the rules nests more
     * than a level or two deep, but resolving to a fixed point costs nothing and means a
     * future feat-granting feat needs no special handling. [selections] is what has been
     * picked so far, which is how a feat chosen inside a choice becomes visible here.
     */
    fun forFeats(
        featIds: Collection<String>,
        selections: Map<String, List<String>>,
    ): List<Choice> {
        val choices = mutableListOf<Choice>()
        val seen = mutableSetOf<String>()
        var frontier = featIds.toList()

        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            frontier.forEach { featId ->
                if (!seen.add(featId)) return@forEach
                forFeat(featId).forEach { choice ->
                    choices += choice
                    // A FEAT-kind choice hands us more feats to follow, once answered.
                    if (choice.kind == ChoiceKind.FEAT) {
                        next += selections[choice.id].orEmpty()
                    }
                }
            }
            frontier = next
        }

        return choices
    }

    /** A background's tool grant, when it names a group instead of a specific tool. */
    fun forBackgroundTool(backgroundId: String): Choice? {
        val background = BackgroundData.byId(backgroundId) ?: return null
        val options = ToolData.optionsForOpenEndedTool(background.toolProficiency) ?: return null
        return Choice(
            id = "background:$backgroundId:tool",
            label = background.toolProficiency,
            prompt = "Choose which one you trained with.",
            count = 1,
            kind = ChoiceKind.TOOL,
            options = ChoiceOptions.fromStrings(options),
            source = background.name,
        )
    }

    /** Cantrips a species lineage lets the player pick rather than granting outright. */
    /**
     * An Origin feat granted by the species itself, as the Human's Versatile trait does. It
     * draws from the same list a background's feat comes from.
     */
    fun forSpecies(speciesId: String?, books: Set<Sourcebook> = Sourcebook.EVERYTHING): List<Choice> {
        val species = speciesId?.let { SpeciesData.byId(it) } ?: return emptyList()
        if (!species.grantsOriginFeat) return emptyList()
        val options = FeatData.inCategories(setOf(FeatCategory.ORIGIN), books)
        if (options.isEmpty()) return emptyList()
        return listOf(
            Choice(
                id = "species:$speciesId:origin_feat",
                label = "Origin Feat",
                prompt = "Choose an Origin feat. ${species.name}s gain one from Versatile.",
                count = 1,
                kind = ChoiceKind.FEAT,
                options = options.map { ChoiceOption(it.id, it.name, it.description, book = it.book) },
                source = species.name,
            )
        )
    }

    /**
     * The origin feat a background leaves open, as five of them do.
     *
     * A background with a fixed feat produces nothing here — that feat is simply granted.
     */
    fun forBackgroundFeat(
        backgroundId: String?,
        books: Set<Sourcebook> = Sourcebook.EVERYTHING,
    ): List<Choice> {
        val background = backgroundId?.let { BackgroundData.byId(it) } ?: return emptyList()
        val choice = background.featChoice ?: return emptyList()
        val fromCategories = FeatData.inCategories(choice.categories, books)
        val named = choice.alsoAllows.mapNotNull { FeatData.byId(it) }.filter { it.book in books }
        val options = (named + fromCategories).distinctBy { it.id }
        if (options.isEmpty()) return emptyList()
        return listOf(
            Choice(
                id = "background:$backgroundId:origin_feat",
                label = "Origin Feat",
                prompt = "Choose your Origin feat: ${choice.label}.",
                count = 1,
                kind = ChoiceKind.FEAT,
                options = options.map { ChoiceOption(it.id, it.name, it.description, book = it.book) },
                source = background.name,
            )
        )
    }

    fun forLineage(speciesId: String, lineageId: String?): List<Choice> {
        if (lineageId == null) return emptyList()
        return when {
            speciesId == "elf" && lineageId == "high_elf" -> listOf(
                Choice(
                    id = "lineage:high_elf:cantrip",
                    label = "High Elf Cantrip",
                    prompt = "Choose a cantrip from the Wizard spell list. Intelligence is your spellcasting ability for it.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions("wizard", 0),
                    source = "High Elf",
                )
            )

            else -> emptyList()
        }
    }

    /** The five types both Primordial Patron invocations choose between. */
    private val ELEMENTAL_OVERFLOW_TYPES = listOf("Acid", "Cold", "Fire", "Lightning", "Thunder")

    /** Tool and cantrip grants attached to a class at level 1 that name a group. */
    fun forClass(
        classId: String,
        classSelections: Map<String, List<String>>,
        /** The character's books, for a choice whose options are themselves book content. */
        books: Set<Sourcebook> = Sourcebook.EVERYTHING,
    ): List<Choice> {
        val choices = mutableListOf<Choice>()

        when (classId) {
            "bard" -> choices += Choice(
                id = "class:bard:instruments",
                label = "Musical Instruments",
                prompt = "Choose 3 Musical Instruments to gain proficiency with.",
                count = 3,
                kind = ChoiceKind.TOOL,
                options = ChoiceOptions.fromStrings(ToolData.MUSICAL_INSTRUMENTS),
                source = "Bard",
            )

            "monk" -> choices += Choice(
                id = "class:monk:tool",
                label = "Artisan's Tool or Instrument",
                prompt = "Choose one kind of Artisan's Tools or one Musical Instrument.",
                count = 1,
                kind = ChoiceKind.TOOL,
                options = ChoiceOptions.fromStrings(
                    ToolData.ARTISANS_TOOLS + ToolData.MUSICAL_INSTRUMENTS
                ),
                source = "Monk",
            )
        }

        // A Cleric Thaumaturge and a Druid Magician each learn one more cantrip.
        val divineOrder = classSelections["divine_order"]?.firstOrNull()
        if (classId == "cleric" && divineOrder == "thaumaturge") {
            choices += Choice(
                id = "class:cleric:thaumaturge_cantrip",
                label = "Thaumaturge Cantrip",
                prompt = "Choose one extra cantrip from the Cleric spell list.",
                count = 1,
                kind = ChoiceKind.SPELL,
                options = spellOptions("cleric", 0),
                source = "Divine Order: Thaumaturge",
            )
        }

        // Two of the Primordial Patron's invocations name a damage type. Asked here rather
        // than inside the invocation list, because an option can't carry a question of its
        // own — the same route a Cleric's Thaumaturge cantrip takes.
        if (classId == "warlock") {
            val invocations = classSelections[ProgressionData.INVOCATION_CHOICE_ID].orEmpty()
            if ("elemental_overflow" in invocations) {
                choices += Choice(
                    id = "invocation:elemental_overflow:damage",
                    label = "Elemental Overflow",
                    // Repeatable in the rules — "each time you do so, choose a different
                    // damage type" — so each type held here is one taking of the invocation.
                    // Edit Mode's pool editor is where a second taking gets added.
                    prompt = "Choose the damage type your overflow wreathes you in. If you " +
                        "have taken this invocation more than once, hold one type per taking.",
                    count = 1,
                    kind = ChoiceKind.DAMAGE_TYPE,
                    options = ChoiceOptions.fromStrings(ELEMENTAL_OVERFLOW_TYPES),
                    source = "Elemental Overflow",
                )
            }
            // The three that name one of your own damage cantrips. Each is Repeatable, so a
            // second taking picks a second cantrip; the pool editor is where that is added.
            listOf(
                Triple("agonizing_blast", "Agonizing Blast",
                    "Choose the damage cantrip that adds your Charisma modifier to its damage."),
                Triple("eldritch_spear", "Eldritch Spear",
                    "Choose the damage cantrip whose range grows with your Warlock level."),
                Triple("repelling_blast", "Repelling Blast",
                    "Choose the damage cantrip whose hits push a creature 10 feet away."),
            ).forEach { (id, label, prompt) ->
                if (id in invocations) {
                    choices += Choice(
                        id = "invocation:$id:cantrip",
                        label = label,
                        prompt = prompt,
                        count = 1,
                        kind = ChoiceKind.SPELL,
                        options = warlockDamageCantrips(),
                        source = label,
                    )
                }
            }

            // "You gain one Origin feat of your choice."
            if ("lessons_of_the_first_ones" in invocations) {
                choices += Choice(
                    id = "invocation:lessons_of_the_first_ones:feat",
                    label = "Lessons of the First Ones",
                    prompt = "Choose the Origin feat this invocation teaches you.",
                    count = 1,
                    kind = ChoiceKind.FEAT,
                    options = FeatData.ORIGIN_FEATS
                        .filter { it.book in books }
                        .map { ChoiceOption(it.id, it.name, it.description, book = it.book) },
                    source = "Lessons of the First Ones",
                )
            }

            // The Book of Shadows: three cantrips from any list, and a level 1 ritual.
            if ("pact_tome" in invocations) {
                choices += Choice(
                    id = "invocation:pact_tome:cantrips",
                    label = "Book of Shadows: Cantrips",
                    prompt = "Choose three cantrips from any class's spell list.",
                    count = 3,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.ALL
                        .filter { it.level == 0 }
                        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) },
                    source = "Pact of the Tome",
                )
                choices += Choice(
                    id = "invocation:pact_tome:ritual",
                    label = "Book of Shadows: Ritual",
                    prompt = "Choose a level 1 spell with the Ritual tag, from any class's list.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = SpellData.ALL
                        .filter { it.level == 1 && it.ritual }
                        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) },
                    source = "Pact of the Tome",
                )
            }

            if ("elemental_transmutation" in invocations) {
                choices += Choice(
                    id = "invocation:elemental_transmutation:damage",
                    label = "Elemental Transmutation",
                    prompt = "Choose the damage type you convert your damage into.",
                    count = 1,
                    kind = ChoiceKind.DAMAGE_TYPE,
                    options = ChoiceOptions.fromStrings(ELEMENTAL_OVERFLOW_TYPES),
                    source = "Elemental Transmutation",
                )
            }
        }

        val primalOrder = classSelections["primal_order"]?.firstOrNull()
        if (classId == "druid" && primalOrder == "magician") {
            choices += Choice(
                id = "class:druid:magician_cantrip",
                label = "Magician Cantrip",
                prompt = "Choose one extra cantrip from the Druid spell list.",
                count = 1,
                kind = ChoiceKind.SPELL,
                options = spellOptions("druid", 0),
                source = "Primal Order: Magician",
            )
        }

        return choices
    }

    /**
     * Every outstanding choice for a character being built, in the order they should be shown.
     *
     * [originSelections] matters as much as the ids: a feat is only known once it has been
     * chosen, so the choices that feat brings with it can't appear until the player has picked
     * it. Passing the selections back in is what lets a Human who takes Magic Initiate through
     * Versatile then be asked which cantrips they learn.
     *
     * [extraFeatIds] covers feats the character already holds by another route — one taken in
     * place of an Ability Score Improvement, for instance — so their prompts show up too.
     */
    fun all(
        speciesId: String?,
        lineageId: String?,
        classId: String?,
        classSelections: Map<String, List<String>>,
        backgroundId: String?,
        originSelections: Map<String, List<String>> = emptyMap(),
        extraFeatIds: Collection<String> = emptyList(),
        books: Set<Sourcebook> = Sourcebook.EVERYTHING,
    ): List<Choice> {
        val choices = mutableListOf<Choice>()

        choices += forSpecies(speciesId, books)
        choices += forBackgroundFeat(backgroundId, books)
        if (speciesId != null) choices += forLineage(speciesId, lineageId)
        if (classId != null) choices += forClass(classId, classSelections, books)
        if (backgroundId != null) forBackgroundTool(backgroundId)?.let { choices += it }

        // Every feat the character has, from wherever, followed into whatever it grants.
        val rootFeats = buildList {
            val background = backgroundId?.let { BackgroundData.byId(it) }
            // A background that leaves its feat open contributes what was chosen, not its
            // fallback: granting both would hand out a feat the player never picked.
            if (background?.featChoice == null) {
                background?.featId?.let(::add)
            } else {
                forBackgroundFeat(backgroundId, books)
                    .forEach { addAll(originSelections[it.id].orEmpty()) }
            }
            // A species Origin feat is itself a choice, so it starts from what was selected.
            forSpecies(speciesId, books).forEach { addAll(originSelections[it.id].orEmpty()) }
            addAll(extraFeatIds)
        }.distinct()

        choices += forFeats(rootFeats, originSelections)

        return choices.distinctBy { it.id }
    }

    /**
     * The Warlock's own damage cantrips, for the three invocations that name one.
     *
     * Drawn from the catalogue rather than listed by hand, so a damage cantrip added to the
     * Warlock list later is offered without a second edit here.
     */
    private fun warlockDamageCantrips(): List<ChoiceOption> = SpellData.ALL
        .filter { it.level == 0 && "warlock" in it.classes && it.damage.isNotBlank() }
        .map { ChoiceOption(it.id, it.name, it.description, it.school, it.book) }

}
