package com.pedroeu.ficha.ui.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.domain.CharacterCalculations
import kotlinx.coroutines.launch

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
    val charClass = ClassData.byId(loaded.classId)?.name.orEmpty()
    val subclass = loaded.subclassId?.let { SubclassData.byId(it)?.name }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(loaded.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = buildString {
                            append("Level ${loaded.level} $species $charClass")
                            if (subclass != null) append(" ($subclass)")
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (loaded.level < 20) {
                    IconButton(onClick = onLevelUp) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Level up")
                    }
                }
                IconButton(onClick = viewModel::longRest) {
                    Icon(Icons.Default.Bedtime, contentDescription = "Long rest")
                }
                IconButton(onClick = viewModel::toggleEditMode) {
                    Icon(
                        imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (editMode) "Finish editing" else "Edit sheet",
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

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
            edgePadding = 8.dp,
        ) {
            TABS.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        .copy(alpha = 0.7f),
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (TABS[page]) {
                "Stats" -> StatsTab(loaded, viewModel, editMode)
                "Skills" -> SkillsTab(loaded, viewModel, editMode)
                "Combat" -> CombatTab(loaded)
                "Features" -> FeaturesTab(loaded)
                "Spells" -> SpellsTab(loaded, viewModel, editMode)
                "Inventory" -> InventoryTab(loaded, viewModel, editMode)
                "Bio" -> BioTab(loaded, viewModel)
            }
        }
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
                    text = "EDIT MODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Tap any value to override it, or add a bonus on top of the rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (hasOverrides) {
                TextButton(onClick = onClearAll) { Text("Reset all") }
            }
        }
    }
}
