package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.LineageOption
import com.pedroeu.ficha.data.model.Skill
import com.pedroeu.ficha.data.model.Species
import com.pedroeu.ficha.data.model.Trait

object SpeciesData {

    val ALL: List<Species> = listOf(
        Species(
            id = "human",
            name = "Human",
            size = "Medium",
            speed = 30,
            darkvisionRange = 0,
            summary = "Ambitious and adaptable, humans are found in every corner of the world, thriving through sheer versatility.",
            traits = listOf(
                Trait("Resourceful", "You gain proficiency in one skill of your choice."),
                Trait("Determined", "Once per long rest, when you fail an ability check, you can reroll it."),
            ),
            bonusSkillChoiceCount = 1,
        ),
        Species(
            id = "elf",
            name = "Elf",
            size = "Medium",
            speed = 30,
            darkvisionRange = 60,
            summary = "Graceful and long-lived, elves feel a deep connection to magic and the natural world.",
            traits = listOf(
                Trait("Fey Ancestry", "You have advantage on saving throws against being charmed, and magic can't put you to sleep."),
                Trait("Keen Senses", "You gain proficiency in the Perception skill."),
                Trait("Trance", "You don't need to sleep. Instead you meditate for 4 hours a day to gain the benefit of a long rest."),
            ),
            lineageChoiceLabel = "Elven Lineage",
            lineageOptions = listOf(
                LineageOption("drow", "Drow", "Your darkvision extends to 120 feet and you know the Dancing Lights cantrip (Charisma)."),
                LineageOption("high_elf", "High Elf", "You know one cantrip of your choice from the Wizard spell list (Intelligence)."),
                LineageOption("wood_elf", "Wood Elf", "Your speed increases to 35 feet and you know the Druidcraft cantrip (Wisdom)."),
            ),
            grantedSkills = listOf(Skill.PERCEPTION),
        ),
        Species(
            id = "dwarf",
            name = "Dwarf",
            size = "Medium",
            speed = 30,
            darkvisionRange = 120,
            summary = "Stout and sturdy, dwarves are famed for their resilience, craftsmanship, and unshakable resolve.",
            traits = listOf(
                Trait("Dwarven Resilience", "You have resistance to poison damage and advantage on saving throws against being poisoned."),
                Trait("Dwarven Toughness", "Your hit point maximum increases by 1, and increases by 1 again whenever you gain a level."),
                Trait("Stonecunning", "You have expertise on History checks related to the origin of stonework."),
            ),
        ),
        Species(
            id = "halfling",
            name = "Halfling",
            size = "Small",
            speed = 30,
            darkvisionRange = 0,
            summary = "Small, cheerful, and endlessly lucky, halflings find comfort and courage in community.",
            traits = listOf(
                Trait("Lucky", "When you roll a 1 on the d20 for an attack roll, ability check, or saving throw, you can reroll the die."),
                Trait("Brave", "You have advantage on saving throws against being frightened."),
                Trait("Naturally Stealthy", "You can attempt to hide even when obscured only by a creature at least one size larger than you."),
            ),
        ),
        Species(
            id = "gnome",
            name = "Gnome",
            size = "Small",
            speed = 30,
            darkvisionRange = 60,
            summary = "Curious and inventive, gnomes pursue knowledge, magic, and mischief with equal enthusiasm.",
            traits = listOf(
                Trait("Gnomish Cunning", "You have advantage on Intelligence, Wisdom, and Charisma saving throws against magic."),
            ),
            lineageChoiceLabel = "Gnomish Lineage",
            lineageOptions = listOf(
                LineageOption("forest_gnome", "Forest Gnome", "You know the Minor Illusion cantrip (Intelligence) and can communicate simple ideas with Small or smaller beasts."),
                LineageOption("rock_gnome", "Rock Gnome", "You gain proficiency with Tinker's Tools and can spend 1 hour crafting a tiny clockwork device with a minor effect."),
            ),
        ),
        Species(
            id = "goliath",
            name = "Goliath",
            size = "Medium",
            speed = 35,
            darkvisionRange = 0,
            summary = "Towering nomads shaped by harsh peaks, goliaths carry a spark of giant heritage in their blood.",
            traits = listOf(
                Trait("Powerful Build", "You count as one size larger when determining your carrying capacity and the weight you can push, drag, or lift."),
            ),
            lineageChoiceLabel = "Giant Ancestry",
            lineageOptions = listOf(
                LineageOption("cloud", "Cloud's Jaunt", "As a bonus action, you can teleport up to 30 feet to an unoccupied space you can see. Usable a number of times equal to your proficiency bonus per long rest."),
                LineageOption("fire", "Fire's Burn", "When you hit with an attack, you can deal an extra 1d10 fire damage. Usable once per long rest."),
                LineageOption("frost", "Frost's Chill", "When you hit with an attack, you can deal an extra 1d6 cold damage and reduce the target's speed by 10 feet until the end of your next turn."),
                LineageOption("hill", "Hill's Tumble", "When you hit a Large or smaller creature with an attack, you can knock it prone. Usable once per long rest."),
                LineageOption("stone", "Stone's Endurance", "When you take damage, you can reduce it by 1d12 + your Constitution modifier. Usable once per long rest."),
                LineageOption("storm", "Storm's Thunder Clap", "As an action, you can create a burst of thunderous force in a 5-foot radius, dealing 1d8 thunder damage. Usable once per long rest."),
            ),
        ),
        Species(
            id = "orc",
            name = "Orc",
            size = "Medium",
            speed = 30,
            darkvisionRange = 120,
            summary = "Fierce and driven, orcs channel relentless energy and endurance into everything they do.",
            traits = listOf(
                Trait("Adrenaline Rush", "As a bonus action you can Dash and gain temporary hit points equal to your proficiency bonus. Usable a number of times equal to your proficiency bonus per long rest."),
                Trait("Relentless Endurance", "When you're reduced to 0 hit points but not killed outright, you can drop to 1 hit point instead. Usable once per long rest."),
            ),
        ),
        Species(
            id = "tiefling",
            name = "Tiefling",
            size = "Medium",
            speed = 30,
            darkvisionRange = 60,
            summary = "Marked by a fiendish bloodline, tieflings carry an infernal legacy that shapes their innate magic.",
            traits = listOf(
                Trait("Otherworldly Presence", "You know the Thaumaturgy cantrip (Charisma)."),
            ),
            lineageChoiceLabel = "Fiendish Legacy",
            lineageOptions = listOf(
                LineageOption("abyssal", "Abyssal Legacy", "You have resistance to poison damage and know the Poison Spray cantrip."),
                LineageOption("chthonic", "Chthonic Legacy", "You have resistance to necrotic damage and know the Chill Touch cantrip."),
                LineageOption("infernal", "Infernal Legacy", "You have resistance to fire damage and know the Fire Bolt cantrip."),
            ),
        ),
        Species(
            id = "aasimar",
            name = "Aasimar",
            size = "Medium",
            speed = 30,
            darkvisionRange = 60,
            summary = "Touched by the Upper Planes, aasimar carry a spark of celestial radiance within their mortal form.",
            traits = listOf(
                Trait("Celestial Resistance", "You have resistance to necrotic damage and radiant damage."),
                Trait("Healing Hands", "As an action, you touch a creature and it regains hit points equal to your proficiency bonus times a d4. Usable once per long rest."),
                Trait("Light Bearer", "You know the Light cantrip (Charisma)."),
            ),
            lineageChoiceLabel = "Celestial Revelation (unlocks at level 3)",
            lineageOptions = listOf(
                LineageOption("necrotic_shroud", "Necrotic Shroud", "Your eyes turn into pools of darkness and spectral wings sprout from your back, frightening nearby foes."),
                LineageOption("radiant_consumption", "Radiant Consumption", "You glow with an inner light that damages creatures near you and sheds light."),
                LineageOption("astral_form", "Astral Form", "You sprout spectral wings and gain a flying speed for the duration."),
            ),
        ),
        Species(
            id = "dragonborn",
            name = "Dragonborn",
            size = "Medium",
            speed = 30,
            darkvisionRange = 0,
            summary = "Proud descendants of dragons, dragonborn carry a breath weapon and resistance tied to their ancestry.",
            traits = listOf(
                Trait("Breath Weapon", "As an action, you exhale destructive energy in a 15-foot cone (or 30-foot line, your choice). Each creature in the area makes a Dexterity or Constitution saving throw (DC 8 + proficiency bonus + Constitution modifier) taking 1d10 damage of your ancestry's type on a fail, half as much on a save. Usable a number of times equal to your proficiency bonus per long rest."),
                Trait("Draconic Resistance", "You have resistance to the damage type associated with your Draconic Ancestry."),
            ),
            lineageChoiceLabel = "Draconic Ancestry",
            lineageOptions = listOf(
                LineageOption("black", "Black Dragon", "Acid damage."),
                LineageOption("blue", "Blue Dragon", "Lightning damage."),
                LineageOption("brass", "Brass Dragon", "Fire damage."),
                LineageOption("bronze", "Bronze Dragon", "Lightning damage."),
                LineageOption("copper", "Copper Dragon", "Acid damage."),
                LineageOption("gold", "Gold Dragon", "Fire damage."),
                LineageOption("green", "Green Dragon", "Poison damage."),
                LineageOption("red", "Red Dragon", "Fire damage."),
                LineageOption("silver", "Silver Dragon", "Cold damage."),
                LineageOption("white", "White Dragon", "Cold damage."),
            ),
        ),
    )

    fun byId(id: String): Species? = ALL.find { it.id == id }
}
