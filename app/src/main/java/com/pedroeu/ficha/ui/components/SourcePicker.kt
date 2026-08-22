package com.pedroeu.ficha.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.ui.i18n.tr

/**
 * Searching and source-grouping for the lists the player picks from.
 *
 * These lists went from a dozen entries to several hundred when the other books came in, and
 * scrolling is not a way to find Fireball among 419 spells. Everything here is deliberately
 * plain data plus small composables, so the same behaviour can sit inside a LazyColumn (the
 * creation steps), a Column (a choice card), or a bottom sheet without three implementations.
 */
object SourceGrouping {

    /** How many entries a list needs before searching it is worth the row it costs. */
    const val SEARCH_THRESHOLD = 8

    /** How many entries before the list is worth splitting by book. */
    const val GROUP_THRESHOLD = 12

    /**
     * The entries matching [query], by a loose substring match over whatever [text] returns.
     *
     * Blank query means everything, so a picker can pass its field straight through.
     */
    fun <T> matching(items: List<T>, query: String, text: (T) -> String): List<T> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items.filter { text(it).lowercase().contains(q) }
    }

    /**
     * [items] split by book, in the enum's own order so the core books come first.
     *
     * Entries whose book is null are gathered under a null key at the end; that is where a
     * damage type or a skill goes, neither of which comes out of a book.
     */
    fun <T> byBook(items: List<T>, book: (T) -> Sourcebook?): List<Pair<Sourcebook?, List<T>>> {
        val grouped = items.groupBy(book)
        return buildList {
            Sourcebook.ALL.forEach { entry ->
                grouped[entry]?.let { add(entry to it) }
            }
            grouped[null]?.let { add(null to it) }
        }
    }

    /**
     * True when splitting [items] by book would actually help.
     *
     * One book is no grouping at all, and a short list is easier read straight.
     */
    fun <T> worthGrouping(items: List<T>, book: (T) -> Sourcebook?): Boolean =
        items.size >= GROUP_THRESHOLD && items.mapNotNull(book).distinct().size > 1
}

/** The search field that sits above a long list. */
@Composable
fun PickerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = tr("Search by name"),
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        label = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A tappable header for one book's group, showing how many entries it holds.
 *
 * Collapsing is the point: a table running ten books wants the two they actually use open and
 * the rest folded away, not a wall of headers.
 */
@Composable
fun SourceSectionHeader(
    book: Sourcebook?,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = (if (expanded) "▾  " else "▸  ") + (book?.displayName ?: tr("Other")),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Emits [items] into a LazyColumn, split into collapsible sections by book.
 *
 * Falls back to a flat list when grouping would not help — one book, a short list, or an
 * active search, which is already a filter and would only bury its own matches under headers.
 */
fun <T : Any> LazyListScope.sourceGroupedItems(
    items: List<T>,
    query: String,
    collapsed: Set<String>,
    onToggleBook: (String) -> Unit,
    id: (T) -> String,
    book: (T) -> Sourcebook?,
    itemContent: @Composable (T) -> Unit,
) {
    val sections =
        if (query.isBlank() && SourceGrouping.worthGrouping(items, book)) {
            SourceGrouping.byBook(items, book)
        } else {
            listOf(null to items)
        }

    sections.forEach { (sectionBook, entries) ->
        val key = sectionBook?.id ?: "all"
        val open = key !in collapsed
        if (sectionBook != null) {
            item(key = "book:$key") {
                SourceSectionHeader(
                    book = sectionBook,
                    count = entries.size,
                    expanded = open,
                    onToggle = { onToggleBook(key) },
                )
            }
        }
        if (open) {
            items(entries.size, key = { id(entries[it]) }) { index -> itemContent(entries[index]) }
        }
    }
}

/** Shown in place of the list when a search matches nothing. */
@Composable
fun NoSearchResults(query: String, modifier: Modifier = Modifier) {
    Text(
        text = tr("Nothing matches") + " \"" + query.trim() + "\"",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}
