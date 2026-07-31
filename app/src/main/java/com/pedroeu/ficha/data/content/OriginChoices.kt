package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Choice
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.ChoiceOption
import com.pedroeu.ficha.data.model.ChoiceOptions
import com.pedroeu.ficha.data.model.Skill

/**
 * Every grant outside the level-1 class table that the rulebook words as "of your choice".
 *
 * The app's rule is that nothing gets auto-picked, so each of these becomes a real prompt.
 * This is what fixes backgrounds like Sage, whose Magic Initiate feat grants two cantrips
 * and a level 1 spell that the player must choose.
 */
object OriginChoices {

    /** Which spell list each Magic Initiate origin feat draws from. */
    private val MAGIC_INITIATE_LISTS: Map<String, Pair<String, String>> = mapOf(
        "magic_initiate_cleric" to ("cleric" to "Wisdom"),
        "magic_initiate_druid" to ("druid" to "Wisdom"),
        "magic_initiate_wizard" to ("wizard" to "Intelligence"),
    )

    private fun spellOptions(classId: String, level: Int): List<ChoiceOption> =
        SpellData.forClass(classId, level).map { spell ->
            ChoiceOption(
                id = spell.id,
                name = spell.name,
                description = spell.description,
                supporting = spell.subtitle,
            )
        }

    /**
     * Choices granted by an origin feat. Magic Initiate is the important one: two cantrips
     * plus one level 1 spell, all from a specific class list.
     */
    fun forFeat(featId: String, featName: String): List<Choice> {
        MAGIC_INITIATE_LISTS[featId]?.let { (listId, ability) ->
            val listName = listId.replaceFirstChar { it.uppercase() }
            return listOf(
                Choice(
                    id = "feat:$featId:cantrips",
                    label = "$featName Cantrips",
                    prompt = "Choose 2 cantrips from the $listName spell list. $ability is your spellcasting ability for them.",
                    count = 2,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions(listId, 0),
                    source = featName,
                ),
                Choice(
                    id = "feat:$featId:spell",
                    label = "$featName Level 1 Spell",
                    prompt = "Choose 1 level 1 spell from the $listName list. You can cast it once per Long Rest without a slot.",
                    count = 1,
                    kind = ChoiceKind.SPELL,
                    options = spellOptions(listId, 1),
                    source = featName,
                ),
            )
        }

        return when (featId) {
            "skilled" -> listOf(
                Choice(
                    id = "feat:skilled:skills",
                    label = "Skilled",
                    prompt = "Choose 3 skill proficiencies.",
                    count = 3,
                    kind = ChoiceKind.SKILL,
                    options = ChoiceOptions.fromSkills(Skill.ALL),
                    source = featName,
                )
            )

            "crafter" -> listOf(
                Choice(
                    id = "feat:crafter:tools",
                    label = "Crafter",
                    prompt = "Choose 3 kinds of Artisan's Tools to gain proficiency with.",
                    count = 3,
                    kind = ChoiceKind.TOOL,
                    options = ChoiceOptions.fromStrings(ToolData.ARTISANS_TOOLS),
                    source = featName,
                )
            )

            "musician" -> listOf(
                Choice(
                    id = "feat:musician:instruments",
                    label = "Musician",
                    prompt = "Choose 3 Musical Instruments to gain proficiency with.",
                    count = 3,
                    kind = ChoiceKind.TOOL,
                    options = ChoiceOptions.fromStrings(ToolData.MUSICAL_INSTRUMENTS),
                    source = featName,
                )
            )

            else -> emptyList()
        }
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
    fun forSpecies(speciesId: String?): List<Choice> {
        val species = speciesId?.let { SpeciesData.byId(it) } ?: return emptyList()
        if (!species.grantsOriginFeat) return emptyList()
        return listOf(
            Choice(
                id = "species:$speciesId:origin_feat",
                label = "Origin Feat",
                prompt = "Choose an Origin feat. ${species.name}s gain one from Versatile.",
                count = 1,
                kind = ChoiceKind.FEAT,
                options = FeatData.ORIGIN_FEATS.map {
                    ChoiceOption(it.id, it.name, it.description)
                },
                source = species.name,
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

    /** Tool and cantrip grants attached to a class at level 1 that name a group. */
    fun forClass(classId: String, classSelections: Map<String, List<String>>): List<Choice> {
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
     * [grantedSkills] lets skill choices hide options the character already has.
     */
    fun all(
        speciesId: String?,
        lineageId: String?,
        classId: String?,
        classSelections: Map<String, List<String>>,
        backgroundId: String?,
    ): List<Choice> {
        val choices = mutableListOf<Choice>()

        choices += forSpecies(speciesId)
        if (speciesId != null) choices += forLineage(speciesId, lineageId)
        if (classId != null) choices += forClass(classId, classSelections)

        if (backgroundId != null) {
            forBackgroundTool(backgroundId)?.let { choices += it }
            val background = BackgroundData.byId(backgroundId)
            val feat = background?.featId?.let { FeatData.byId(it) }
            if (feat != null) choices += forFeat(feat.id, feat.name)
        }

        return choices
    }
}
