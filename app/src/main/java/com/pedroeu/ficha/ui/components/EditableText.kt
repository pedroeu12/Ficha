package com.pedroeu.ficha.ui.components

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Text that becomes editable in Edit Mode. Rules text is never mutated in place: the player's
 * version is stored as an override, so [onChange] receiving null restores the original.
 */
@Composable
fun EditableText(
    value: String,
    editMode: Boolean,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = tr("Edit"),
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight? = null,
    multiline: Boolean = false,
    /** True when this text has already been rewritten, so it can be marked and reset. */
    isOverridden: Boolean = false,
    placeholder: String = "",
) {
    var editing by remember { mutableStateOf(false) }

    val shown = value.ifBlank { placeholder }

    Row(
        modifier = modifier.then(
            if (editMode) Modifier.clickable { editing = true } else Modifier
        ),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        Text(
            text = shown,
            style = style,
            fontWeight = fontWeight,
            color = if (isOverridden) MaterialTheme.colorScheme.primary else color,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (editMode) {
            Icon(
                Icons.Default.Edit,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp, top = 2.dp)
                    .size(13.dp),
            )
        }
    }

    if (editing) {
        TextEditDialog(
            title = label,
            initial = value,
            multiline = multiline,
            canReset = isOverridden,
            onDismiss = { editing = false },
            onConfirm = {
                onChange(it)
                editing = false
            },
        )
    }
}

@Composable
fun TextEditDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    multiline: Boolean = false,
    canReset: Boolean = false,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = !multiline,
                    minLines = if (multiline) 4 else 1,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (canReset) {
                    Text(
                        text = tr("Reset puts the original rulebook text back."),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(tr("Save")) }
        },
        dismissButton = {
            Row {
                if (canReset) {
                    TextButton(onClick = { onConfirm(null) }) { Text(tr("Reset")) }
                }
                TextButton(onClick = onDismiss) { Text(tr("Cancel")) }
            }
        },
    )
}
