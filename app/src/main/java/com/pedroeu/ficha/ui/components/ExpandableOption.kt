package com.pedroeu.ficha.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * One named thing with rules behind it: a maneuver, an invocation, a mastery property, a
 * feature's chosen option.
 *
 * It used to fold open in place, which meant a list of nineteen maneuvers became nineteen
 * pages the moment you were curious about one, and meant that reading a feature worked
 * differently from reading a spell or an item three tabs away. It is a row now, and the row
 * opens the same sheet everything else opens.
 */
@Composable
fun ExpandableOption(
    name: String,
    description: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    trailingLabel: String = "",
) {
    var open by rememberSaveable(name) { mutableStateOf(false) }
    val hasBody = description.isNotBlank()

    DetailRow(
        title = name,
        modifier = modifier,
        // The first sentence stands in for the rest, so the list says something useful
        // without the player having to open every row to find out which one they want.
        supporting = if (hasBody) firstSentenceOf(description) else subtitle,
        trailing = trailingLabel,
        onClick = if (hasBody) ({ open = true }) else null,
    )

    if (open) {
        DetailSheet(
            detail = Detail(title = name, kind = subtitle, body = description),
            onDismiss = { open = false },
        )
    }
}
