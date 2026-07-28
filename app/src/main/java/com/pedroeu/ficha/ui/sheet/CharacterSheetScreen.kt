package com.pedroeu.ficha.ui.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpeciesData
import kotlinx.coroutines.launch

private val TABS = listOf("Stats", "Skills", "Combat", "Features", "Spells", "Inventory", "Bio")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    characterId: String,
    repository: CharacterRepository,
    onBack: () -> Unit,
) {
    val viewModel: SheetViewModel = viewModel(
        factory = SheetViewModel.Factory(repository, characterId)
    )
    val character by viewModel.character.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    val loaded = character
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val species = SpeciesData.byId(loaded.speciesId)?.name.orEmpty()
    val charClass = ClassData.byId(loaded.classId)?.name.orEmpty()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(loaded.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Level ${loaded.level} $species $charClass",
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
                IconButton(onClick = viewModel::longRest) {
                    Icon(Icons.Default.Bedtime, contentDescription = "Long rest")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

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
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp),
        ) { page ->
            when (TABS[page]) {
                "Stats" -> StatsTab(loaded, viewModel)
                "Skills" -> SkillsTab(loaded)
                "Combat" -> CombatTab(loaded)
                "Features" -> FeaturesTab(loaded)
                "Spells" -> SpellsTab(loaded, viewModel)
                "Inventory" -> InventoryTab(loaded, viewModel)
                "Bio" -> BioTab(loaded, viewModel)
            }
        }
    }
}
