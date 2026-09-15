package com.pedroeu.ficha.ui.creation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.CustomOption
import com.pedroeu.ficha.ui.components.CustomOptionDialog
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.i18n.trf

/**
 * Making a species, a class or an origin your own, during creation.
 *
 * A homebrew class is not a name and a paragraph — it is a table of features at twenty levels,
 * a Hit Die, saving throws, and in most cases a spell list, and a sheet that let you invent one
 * from nothing would be a sheet that could not add anything up. So this does the honest thing
 * instead, and the thing a table actually does: start from whichever entry is closest, then
 * give it your own name and your own description.
 *
 * The character then reads as yours everywhere — the sheet's Bio, the page heading, the
 * summary — while the arithmetic behind it still comes from real rules. What you wrote is kept
 * as a feature, so it is on the sheet to be read at the table rather than lost in the wizard.
 */
@Composable
fun MakeItYourOwn(
    /** What is being named — "species", "class", "origin". */
    label: String,
    /** The entry the player has picked to build on, or null while nothing is picked. */
    baseName: String?,
    /** What they have already written, if anything. */
    written: String?,
    writtenDescription: String = "",
    onWrite: (name: String, description: String) -> Unit,
    onClear: () -> Unit,
) {
    var writing by remember { mutableStateOf(false) }

    Card(
        shape = Corner.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = trf("Write your own {0}", label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    written != null -> trf("This character is a {0}.", written)
                    baseName == null -> trf("Pick whichever {0} is closest first.", label)
                    else -> trf(
                        "Keep {0}'s rules and give it a name and a description of your own.",
                        baseName,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { writing = true },
                enabled = baseName != null,
            ) {
                Text(if (written != null) tr("Edit yours") else tr("Write your own"))
            }
        }
    }

    if (writing) {
        CustomOptionDialog(
            choiceId = label,
            existing = written?.let {
                CustomOption(id = label, choiceId = label, name = it, description = writtenDescription)
            },
            onDismiss = { writing = false },
            onSave = {
                onWrite(it.name, it.description)
                writing = false
            },
            onErase = {
                onClear()
                writing = false
            },
        )
    }
}
