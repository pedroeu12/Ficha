package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.OwnedOptions
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.domain.ResolvedChoice
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Changing one answer, from the sheet.
 *
 * One dialog for both layouts, because the phone and the tablet had their own copies of it and
 * the copies had drifted: neither could scroll, and only one of them checked what the character
 * was allowed to take.
 *
 * The scrolling is the part that matters. A Material dialog's body is bounded but not
 * scrollable — content taller than the dialog is simply clipped — and a Warlock's invocation
 * list is twenty-eight options long. Everything past the first handful was drawn off the bottom
 * of the dialog with no way to reach it, which is most of what "editing invocations does not
 * work" looked like: the list you could see never contained the thing you wanted, and the
 * options you could see were the same few every time.
 */
@Composable
fun ChoiceEditDialog(
    character: PlayerCharacter,
    resolved: ResolvedChoice,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    // The same eligibility every other picker applies: what is already held, the level in the
    // class that gates it, and an option another option requires.
    val disabled = OwnedOptions.disabledFor(
        choice = resolved.choice,
        owned = OwnedOptions.of(character),
        currentSelection = resolved.selectedIds.toSet(),
        classLevels = ClassLevels.levelMap(character),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resolved.choice.label) },
        text = {
            Box(
                Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ChoiceSection(
                    choice = resolved.choice,
                    selected = resolved.selectedIds,
                    onToggle = onToggle,
                    disabledOptionIds = disabled,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Done")) } },
    )
}
