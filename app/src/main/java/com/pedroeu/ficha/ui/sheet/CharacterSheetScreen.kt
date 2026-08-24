package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.ui.components.EditableText
import com.pedroeu.ficha.ui.layout.LocalLayoutController
import com.pedroeu.ficha.ui.tablet.TabletSheetScreen
import kotlinx.coroutines.launch

// The English names double as the keys the pager switches on, so they stay untranslated
// here and are translated where they are drawn.
private val TABS = listOf("Stats", "Skills", "Combat", "Features", "Spells", "Inventory", "Bio")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: String,
    repository: CharacterRepository,
    onBack: () -> Unit,
    onLevelUp: () -> Unit,
) {
    val viewModel: SheetViewModel = viewModel(
        factory = SheetViewModel.Factory(repository, characterId)
    )
    val character by viewModel.character.collectAsStateWithLifecycle()
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()
    val layout = LocalLayoutController.current
    var restKind by remember { mutableStateOf<RestKind?>(null) }

    // Coming back from the level-up flow, the stored character has changed underneath us.
    LaunchedEffect(Unit) { viewModel.refresh() }

    val loaded = character
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val species = SpeciesData.byId(loaded.speciesId)?.name.orEmpty()
    // A multiclass character reads "Fighter 5 / Wizard 3", which already carries the levels,
    // so the "Level N" prefix is dropped for them.
    val isMulticlassed = ClassLevels.isMulticlassed(loaded)
    val charClass = ClassLevels.label(loaded)
    // Every class picks its own subclass, so a multiclassed character has more than one.
    val subclass = ClassLevels.subclassLabel(loaded).takeIf { it.isNotBlank() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    EditableText(
                        value = loaded.name,
                        editMode = editMode,
                        onChange = { viewModel.setName(it.orEmpty()) },
                        label = tr("Character name"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = buildString {
                            if (isMulticlassed) {
                                append("$species $charClass")
                            } else {
                                append("Level ${loaded.level} $species $charClass")
                                if (subclass != null) append(" ($subclass)")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"))
                }
            },
            actions = {
                if (loaded.level < 20) {
                    IconButton(onClick = onLevelUp) {
                        Icon(Icons.Default.TrendingUp, contentDescription = tr("Level up"))
                    }
                }
                IconButton(onClick = { restKind = RestKind.SHORT }) {
                    Icon(Icons.Default.LocalCafe, contentDescription = tr("Short rest"))
                }
                IconButton(onClick = { restKind = RestKind.LONG }) {
                    Icon(Icons.Default.Bedtime, contentDescription = tr("Long rest"))
                }
                IconButton(onClick = viewModel::toggleEditMode) {
                    Icon(
                        imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (editMode) tr("Finish editing") else tr("Edit sheet"),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        AnimatedVisibility(visible = editMode) {
            EditModeBanner(
                hasOverrides = CharacterCalculations.hasManualAdjustments(loaded),
                onClearAll = viewModel::clearAllOverrides,
            )
        }

        // Given the width, the sheet is laid out like the printed one rather than paged
        // through. Which happens is the player's choice, defaulting to whatever fits.
        //
        // weight, not fillMaxSize. Inside a Column, fillMaxSize measures against the whole
        // screen rather than what the app bar left over, so the sheet was laid out an app
        // bar too tall and the last rows of it fell off the bottom. Edit Mode made it worse
        // by a banner's height, which is why Edit Mode looked like the thing that was broken.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (layout.mode.isWide(maxWidth)) {
                TabletSheetScreen(loaded, viewModel, editMode)
                return@BoxWithConstraints
            }

            PhoneSheet(
                character = loaded,
                viewModel = viewModel,
                editMode = editMode,
                pagerState = pagerState,
                onSelectTab = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            )
        }
    }

    RestSheetHost(
        restKind = restKind,
        character = loaded,
        viewModel = viewModel,
        onDismiss = { restKind = null },
    )
}

/** The narrow layout: one column at a time, chosen from a row of tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneSheet(
    character: com.pedroeu.ficha.domain.PlayerCharacter,
    viewModel: SheetViewModel,
    editMode: Boolean,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onSelectTab: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
            edgePadding = 8.dp,
        ) {
            TABS.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { onSelectTab(index) },
                    text = { Text(tr(title), style = MaterialTheme.typography.labelLarge) },
                    unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        .copy(alpha = 0.7f),
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            // The same reason as the sheet above: fillMaxSize under a tab row measures
            // against the whole column and hangs a tab row's worth off the bottom.
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (TABS[page]) {
                "Stats" -> StatsTab(character, viewModel, editMode)
                "Skills" -> SkillsTab(character, viewModel, editMode)
                "Combat" -> CombatTab(character, viewModel, editMode)
                "Features" -> FeaturesTab(character, viewModel, editMode)
                "Spells" -> SpellsTab(character, viewModel, editMode)
                "Inventory" -> InventoryTab(character, viewModel, editMode)
                "Bio" -> BioTab(character, viewModel, editMode)
            }
        }
    }
}

@Composable
private fun RestSheetHost(
    restKind: RestKind?,
    character: com.pedroeu.ficha.domain.PlayerCharacter,
    viewModel: SheetViewModel,
    onDismiss: () -> Unit,
) {
    restKind?.let { kind ->
        RestSheet(kind = kind, character = character, viewModel = viewModel, onDismiss = onDismiss)
    }
}

@Composable
private fun EditModeBanner(hasOverrides: Boolean, onClearAll: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = tr("EDIT MODE"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = tr("Tap any value to override it, or add a bonus on top of the rules."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (hasOverrides) {
                TextButton(onClick = onClearAll) { Text(tr("Reset all")) }
            }
        }
    }
}
