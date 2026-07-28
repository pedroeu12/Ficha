package com.pedroeu.ficha.ui.levelup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.ClassData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpScreen(
    characterId: String,
    repository: CharacterRepository,
    onExit: () -> Unit,
    onFinished: () -> Unit,
) {
    val viewModel: LevelUpViewModel = viewModel(
        factory = LevelUpViewModel.Factory(repository, characterId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()

    LaunchedEffect(finished) {
        if (finished) onFinished()
    }

    BackHandler {
        if (!viewModel.back()) onExit()
    }

    val loaded = state
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val className = ClassData.byId(loaded.character.classId)?.name.orEmpty()
    val steps = loaded.steps
    val isLastStep = loaded.step == LevelUpStep.SUMMARY

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(loaded.step.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "$className ${loaded.currentLevel} → ${loaded.targetLevel}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!viewModel.back()) onExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                LevelUpStepIndicator(steps = steps, current = loaded.step)
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = hintFor(loaded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { if (isLastStep) viewModel.confirm() else viewModel.next() },
                        enabled = loaded.canAdvance,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(if (isLastStep) "Level Up" else "Next")
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (loaded.step) {
                LevelUpStep.HIT_POINTS -> HitPointsStep(loaded, viewModel)
                LevelUpStep.SUBCLASS -> SubclassStep(loaded, viewModel)
                LevelUpStep.FEATURES -> FeaturesStep(loaded, viewModel)
                LevelUpStep.ASI -> AbilityImprovementStep(loaded, viewModel)
                LevelUpStep.SPELLS -> NewSpellsStep(loaded, viewModel)
                LevelUpStep.SUMMARY -> SummaryStep(loaded)
            }
        }
    }
}

@Composable
private fun LevelUpStepIndicator(steps: List<LevelUpStep>, current: LevelUpStep) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEach { step ->
                val done = steps.indexOf(step) < steps.indexOf(current)
                val active = step == current
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            active -> MaterialTheme.colorScheme.secondary
                            done -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        },
                    ) {}
                    Text(
                        text = step.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun hintFor(state: LevelUpState): String = when {
    state.step == LevelUpStep.SUMMARY -> "Confirm to save your new level"
    state.canAdvance -> "Looks good"
    else -> when (state.step) {
        LevelUpStep.HIT_POINTS -> "Choose how to gain hit points"
        LevelUpStep.SUBCLASS -> "Pick your subclass"
        LevelUpStep.FEATURES -> "Complete every choice below"
        LevelUpStep.ASI -> "Assign both points, or choose a feat"
        LevelUpStep.SPELLS -> "Choose your new spells"
        LevelUpStep.SUMMARY -> ""
    }
}
