package com.pedroeu.ficha.data.model

/** A short spell/cantrip stub — enough to display on the sheet, not full spell mechanics. */
data class SpellStub(
    val id: String,
    val name: String,
    val level: Int,
    val school: String,
    val description: String,
)

/** A single labeled option inside a [ClassChoice.FeatureOption]. */
data class FeatureChoiceOption(
    val id: String,
    val name: String,
    val description: String,
)

/** One decision the player must make while building this class at level 1. */
sealed class ClassChoice(open val id: String, open val label: String) {
    data class SkillProficiencyChoice(
        override val id: String,
        override val label: String,
        val count: Int,
        val options: List<Skill>,
    ) : ClassChoice(id, label)

    data class FeatureOption(
        override val id: String,
        override val label: String,
        val options: List<FeatureChoiceOption>,
    ) : ClassChoice(id, label)

    data class CantripChoice(
        override val id: String,
        override val label: String,
        val count: Int,
        val options: List<SpellStub>,
    ) : ClassChoice(id, label)
}

data class CharClass(
    val id: String,
    val name: String,
    val hitDie: Int,
    val primaryAbility: List<Ability>,
    val savingThrows: List<Ability>,
    val armorProficiencies: List<String>,
    val weaponProficiencies: List<String>,
    val toolProficiencies: List<String>,
    val level1Features: List<Trait>,
    val choices: List<ClassChoice>,
    val isSpellcaster: Boolean,
    val spellcastingAbility: Ability? = null,
    val summary: String,
)
