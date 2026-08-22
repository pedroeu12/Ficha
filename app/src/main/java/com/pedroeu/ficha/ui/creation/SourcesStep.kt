package com.pedroeu.ficha.ui.creation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.i18n.tr

/**
 * The first step: which books this character may draw on.
 *
 * It comes before species because every later picker is filtered by what is chosen here. The
 * two core books start on; everything else is opt-in, so the default flow is unchanged for
 * anyone who does not care about the extra material.
 */
@Composable
fun SourcesStep(state: CreationState, viewModel: CreationViewModel) {
    val published = Sourcebook.ALL.filter { !it.isCore && !it.isPlaytest }
    val playtest = Sourcebook.ALL.filter { it.isPlaytest }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = tr(
                    "Only options from the books you tick here are offered, both now and every " +
                        "time this character levels up. You can change this later in Edit Mode.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.setSources(Sourcebook.EVERYTHING) }) {
                    Text(tr("Select all"))
                }
                TextButton(onClick = { viewModel.setSources(Sourcebook.CORE) }) {
                    Text(tr("Core only"))
                }
            }
        }

        item { SectionHeader(tr("Core")) }
        items(Sourcebook.ALL.filter { it.isCore }, key = { it.id }) { book ->
            SourceRow(book, book in state.enabledSources) { viewModel.toggleSource(book) }
        }

        item { SectionHeader(tr("Settings and Supplements")) }
        items(published, key = { it.id }) { book ->
            SourceRow(book, book in state.enabledSources) { viewModel.toggleSource(book) }
        }

        item { SectionHeader(tr("Playtest")) }
        item {
            Text(
                text = tr("Unearthed Arcana rules are unfinished and may change or be withdrawn."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(playtest, key = { it.id }) { book ->
            SourceRow(book, book in state.enabledSources) { viewModel.toggleSource(book) }
        }

        if (state.enabledSources.isEmpty()) {
            item {
                Text(
                    text = tr("Pick at least one book to continue."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SourceRow(book: Sourcebook, checked: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = book.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
