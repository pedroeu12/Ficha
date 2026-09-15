package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.pedroeu.ficha.domain.CustomOptions
import com.pedroeu.ficha.ui.design.Corner
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Writing an option the books do not have, or writing over one they do.
 *
 * Two gestures, one form, because they are the same thing from the player's side: this option
 * should say what I say it says. Which one it is depends only on the id it was opened with —
 * a fresh one adds an entry to the list, an existing one rewrites that entry and keeps
 * everything the rules attached to it.
 */
@Composable
fun CustomOptionDialog(
    choiceId: String,
    /** The option being written over, or null to write a new one. */
    existing: CustomOption?,
    /** The book's own wording, shown as the starting point when rewriting. */
    startingName: String = "",
    startingDescription: String = "",
    onDismiss: () -> Unit,
    onSave: (CustomOption) -> Unit,
    onErase: ((CustomOption) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(existing?.name ?: startingName) }
    var description by remember {
        mutableStateOf(existing?.description ?: startingDescription)
    }

    val writingOver = existing?.isRewrite == true || (existing == null && startingName.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (writingOver) tr("Rewrite this option") else tr("Write your own")) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Name")) },
                    singleLine = true,
                    shape = Corner.row,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tr("What it does")) },
                    minLines = 4,
                    shape = Corner.row,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (writingOver) {
                        tr("The option keeps its place and its requirements; only the wording changes.")
                    } else {
                        tr("This is added to the list for this character only.")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CustomOption(
                            id = existing?.id ?: CustomOptions.newId(),
                            choiceId = choiceId,
                            name = name.trim(),
                            description = description.trim(),
                        )
                    )
                },
            ) { Text(tr("Save")) }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                if (existing != null && onErase != null) {
                    TextButton(onClick = { onErase(existing) }) {
                        Text(if (existing.isRewrite) tr("Reset") else tr("Delete"))
                    }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}
