package com.pedroeu.ficha.ui.sheet

import androidx.compose.runtime.Composable
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.ui.components.Detail
import com.pedroeu.ficha.ui.components.DetailFact
import com.pedroeu.ficha.ui.components.DetailSheet
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Everything about one spell, in the app's one detail shape.
 *
 * The stored [KnownSpell] carries only a name, level, school and description, so the numbers
 * come from the catalogue. A spell the player wrote by hand won't be in there, and the sheet
 * shows what it has rather than pretending to know the rest.
 */
@Composable
fun SpellDetailSheet(spell: KnownSpell, onDismiss: () -> Unit) {
    val entry = SpellData.byId(spell.id)

    DetailSheet(
        detail = Detail(
            title = spell.name,
            kind = listOf(
                if (spell.level == 0) tr("Cantrip") else "${tr("Level")} ${spell.level}",
                spell.school,
            ).filter { it.isNotBlank() }.joinToString(" • "),
            // What a caster needs in the half-second before casting: what it does to whom.
            summary = entry?.let { catalogue ->
                listOfNotNull(
                    catalogue.damage.takeIf { it.isNotBlank() }
                        ?.let { "$it ${catalogue.damageType}".trim() },
                    when {
                        catalogue.needsAttackRoll -> tr("Spell attack roll")
                        catalogue.saveAbility != null ->
                            "${catalogue.saveAbility!!.fullName} ${tr("saving throw")}"
                        else -> null
                    },
                    tr("Concentration").takeIf { catalogue.concentration },
                    tr("Ritual").takeIf { catalogue.ritual },
                ).joinToString(" • ")
            }.orEmpty(),
            facts = listOfNotNull(
                entry?.let { DetailFact(tr("Casting Time"), it.castingTime) },
                entry?.let { DetailFact(tr("Range"), it.range) },
                entry?.let { DetailFact(tr("Components"), it.components) },
                entry?.let { DetailFact(tr("Duration"), it.duration) },
                spell.source.takeIf { it.isNotBlank() }?.let { DetailFact(tr("From"), it) },
            ),
            body = entry?.description?.takeIf { it.isNotBlank() }
                ?: spell.description.ifBlank { tr("No description recorded for this spell.") },
            footnote = if (entry == null) {
                tr("This spell isn't in the rulebook data, so only what you entered is " +
                    "shown. You can edit its text in Edit Mode.")
            } else {
                ""
            },
        ),
        onDismiss = onDismiss,
    )
}
