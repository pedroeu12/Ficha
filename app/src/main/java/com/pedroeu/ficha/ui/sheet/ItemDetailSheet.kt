package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pedroeu.ficha.data.content.CatalogItem
import com.pedroeu.ficha.data.content.ItemCatalog
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.ui.components.Detail
import com.pedroeu.ficha.ui.components.DetailFact
import com.pedroeu.ficha.ui.components.DetailSheet
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.design.Space
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Full detail for one inventory line, in the app's one detail shape.
 *
 * Equipment is where this pattern started, and for a long time it was the only thing that had
 * it — which is exactly what made the rest of the app feel inconsistent. Now it is the same
 * sheet as everything else, with renaming and notes as actions below the text.
 */
@Composable
fun ItemDetailSheet(
    item: InventoryItem,
    onDismiss: () -> Unit,
    editMode: Boolean = false,
    onRename: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
) {
    val catalogItem: CatalogItem? = ItemCatalog.resolve(item)
    val hasOwnNotes = item.notes.isNotBlank() && item.notes != catalogItem?.description

    DetailSheet(
        detail = Detail(
            title = item.name,
            kind = catalogItem?.category ?: tr("Custom item"),
            facts = buildList {
                add(DetailFact(tr("Quantity"), "${item.quantity}"))
                catalogItem?.let {
                    add(DetailFact(tr("Cost"), it.costLabel))
                    add(DetailFact(tr("Weight"), it.weightLabel))
                }
                if (catalogItem == null && item.weightLb > 0) {
                    add(DetailFact(tr("Weight"), "${item.weightLb} lb"))
                }
                catalogItem?.stats?.forEach { (label, value) -> add(DetailFact(label, value)) }
            },
            body = catalogItem?.description?.takeIf { it.isNotBlank() }
                ?: item.notes.takeIf { it.isNotBlank() }
                ?: tr("No description recorded for this item. Add one in Edit Mode, or " +
                    "replace it with an entry from the rulebook list."),
        ),
        onDismiss = onDismiss,
    ) {
        // Renaming and note-taking are things done *to* the item, so they sit below what the
        // item is — the same place every other sheet puts its actions.
        if (editMode) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.inline)) {
                SectionHeader(tr("Item name"))
                EditableText(
                    value = item.name,
                    editMode = true,
                    onChange = { onRename(it.orEmpty()) },
                    label = tr("Item name"),
                )
            }
        }

        if (editMode || hasOwnNotes) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.inline)) {
                SectionHeader(tr("Your notes"))
                EditableText(
                    value = item.notes,
                    editMode = editMode,
                    onChange = { onNotesChange(it.orEmpty()) },
                    label = tr("Item notes"),
                    multiline = true,
                    placeholder = if (editMode) tr("Tap to add your own notes") else "",
                )
            }
        }
    }
}
