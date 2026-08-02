package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.i18n.tr
import com.pedroeu.ficha.ui.layout.distributeInto

/**
 * One block of the sheet, so the tablet layout can place it in a column rather than a tab.
 *
 * The content is exactly what the phone shows on that tab — the whole point of this layout is
 * that nothing is added, removed, or simplified, only put somewhere else.
 */
private data class SheetSection(
    val title: String,
    val content: @Composable () -> Unit,
)

/**
 * The character sheet laid out like the printed one.
 *
 * The official sheet is two sides: the front carries who you are, what you roll, and what you
 * can do; the back carries magic, gear, and the person behind the numbers. On a phone those
 * become seven tabs because there is only ever room for one column. Given a tablet's width,
 * the same sections can sit side by side the way they do on paper, and the sheet stops being
 * something you page through and becomes something you read.
 *
 * How many columns fit is decided from the actual width rather than assumed. A section that
 * doesn't get a column of its own shares one, with a small switch at the top — which is still
 * fewer taps than the tab row it replaces, and never squeezes a column below a phone's width.
 */
@Composable
fun TabletSheet(
    character: PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
) {
    // Kept across rotation, so turning a tablet doesn't send you back to the front page.
    var page by rememberSaveable { mutableStateOf(0) }

    val front = listOf(
        SheetSection("Stats") { StatsTab(character, viewModel, editMode) },
        SheetSection("Skills") { SkillsTab(character, viewModel, editMode) },
        SheetSection("Combat") { CombatTab(character, viewModel, editMode) },
        SheetSection("Features") { FeaturesTab(character, viewModel, editMode) },
    )
    val back = listOf(
        SheetSection("Spells") { SpellsTab(character, viewModel, editMode) },
        SheetSection("Inventory") { InventoryTab(character, viewModel, editMode) },
        SheetSection("Bio") { BioTab(character, viewModel, editMode) },
    )

    Column(Modifier.fillMaxSize()) {
        IdentityBanner(character)

        TabRow(
            selectedTabIndex = page,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
        ) {
            listOf("Character", "Magic & Gear").forEachIndexed { index, title ->
                Tab(
                    selected = page == index,
                    onClick = { page = index },
                    text = { Text(tr(title), style = MaterialTheme.typography.labelLarge) },
                    unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        .copy(alpha = 0.7f),
                )
            }
        }

        SectionColumns(sections = if (page == 0) front else back)
    }
}

/**
 * The sections spread across as many columns as the window can hold without cramping.
 *
 * Each column scrolls on its own, which is what makes this worth doing: reading your spell
 * list doesn't move your hit points off the screen.
 */
@Composable
private fun SectionColumns(sections: List<SheetSection>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // A column narrower than a phone would be a step backwards, so this is the floor
        // rather than a target: four columns need a very wide screen to appear at all.
        val columnCount = (maxWidth / MIN_COLUMN_WIDTH)
            .toInt()
            .coerceIn(1, sections.size)

        val columns = sections.distributeInto(columnCount)

        Row(Modifier.fillMaxSize()) {
            columns.forEachIndexed { index, group ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SectionColumn(group)
                }
                // The hairline the printed sheet rules between its columns.
                if (index < columns.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
    }
}

/** One column: a single section, or a few with a switch at the top. */
@Composable
private fun SectionColumn(sections: List<SheetSection>) {
    if (sections.isEmpty()) return
    if (sections.size == 1) {
        sections.first().content()
        return
    }

    var selected by remember(sections.map { it.title }) { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selected,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.secondary,
        ) {
            sections.forEachIndexed { index, section ->
                Tab(
                    selected = selected == index,
                    onClick = { selected = index },
                    text = { Text(tr(section.title), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        sections[selected].content()
    }
}

/**
 * The band across the top of the printed sheet: who this character is, in one line.
 *
 * On a phone this lives in the app bar, where there is only room for two lines of it. Given
 * the width, the rest of what the paper sheet prints up there — origin, subclass, experience —
 * fits beside it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityBanner(character: PlayerCharacter) {
    val species = SpeciesData.byId(character.speciesId)?.name.orEmpty()
    val background = BackgroundData.byId(character.backgroundId)?.name.orEmpty()
    val subclass = character.subclassId?.let { SubclassData.byId(it)?.name }.orEmpty()
    val classes = ClassLevels.label(character)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BannerField(tr("Species"), species)
            BannerField(tr("Class"), classes)
            if (subclass.isNotBlank()) BannerField(tr("Subclass"), subclass)
            if (background.isNotBlank()) BannerField(tr("Origin"), background)
            BannerField(tr("Level"), "${character.level}")
            BannerField("XP", "${character.experiencePoints}")
        }
    }
}

@Composable
private fun BannerField(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The narrowest a column may be before the layout drops to fewer of them. */
private val MIN_COLUMN_WIDTH = 360.dp
