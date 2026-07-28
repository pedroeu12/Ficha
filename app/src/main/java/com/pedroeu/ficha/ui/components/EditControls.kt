package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Editor for a single calculated value. A *bonus* is added to whatever the rules produce,
 * which suits narrative awards from a DM; an *override* pins the value outright, for cases
 * the app's rules engine doesn't model. Leaving a field blank removes that adjustment.
 */
@Composable
fun StatEditDialog(
    title: String,
    rulesValue: Int,
    currentBonus: Int?,
    currentOverride: Int?,
    onDismiss: () -> Unit,
    onConfirm: (bonus: Int?, override: Int?) -> Unit,
    allowNegative: Boolean = true,
    supportingText: String? = null,
) {
    var bonusText by remember { mutableStateOf(currentBonus?.toString().orEmpty()) }
    var overrideText by remember { mutableStateOf(currentOverride?.toString().orEmpty()) }

    fun parse(text: String): Int? {
        val cleaned = if (allowNegative) {
            text.filterIndexed { index, c -> c.isDigit() || (index == 0 && c == '-') }
        } else {
            text.filter { it.isDigit() }
        }
        return cleaned.toIntOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "The rules give $rulesValue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = bonusText,
                    onValueChange = { bonusText = it },
                    label = { Text("Bonus (added to the rules)") },
                    placeholder = { Text("e.g. 2") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowNegative) KeyboardType.Text else KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = overrideText,
                    onValueChange = { overrideText = it },
                    label = { Text("Override (replaces everything)") },
                    placeholder = { Text("Leave blank to use the rules") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowNegative) KeyboardType.Text else KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parse(bonusText), parse(overrideText)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null, null) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** A compact "-  value  +" control for values edited in place. */
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 99,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
        ) { Text("−") }
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        TextButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
        ) { Text("+") }
    }
}
