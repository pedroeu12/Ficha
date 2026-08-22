package com.pedroeu.ficha.ui.creation

import com.pedroeu.ficha.ui.i18n.tr
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationWizardScreen(
    repository: CharacterRepository,
    onExit: () -> Unit,
    onFinished: (String) -> Unit,
) {
    val viewModel: CreationViewModel = viewModel(
        factory = CreationViewModel.Factory(repository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedId by viewModel.savedCharacterId.collectAsStateWithLifecycle()

    LaunchedEffect(savedId) {
        savedId?.let(onFinished)
    }

    BackHandler {
        if (!viewModel.back()) onExit()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = state.step.title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (!viewModel.back()) onExit() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("Back"),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                StepIndicator(current = state.step)
            }
        },
        bottomBar = {
            WizardBottomBar(
                state = state,
                onNext = {
                    if (state.step == CreationStep.DETAILS) viewModel.finish() else viewModel.next()
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = CreationStep.ORDER.indexOf(targetState) >
                    CreationStep.ORDER.indexOf(initialState)
                val offset = if (forward) 1 else -1
                (slideInHorizontally(tween(260)) { it * offset } + fadeIn(tween(260)))
                    .togetherWith(
                        slideOutHorizontally(tween(260)) { -it * offset } + fadeOut(tween(260))
                    )
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "creation-step",
        ) { step ->
            when (step) {
                CreationStep.SOURCES -> SourcesStep(state, viewModel)
                CreationStep.SPECIES -> SpeciesStep(state, viewModel)
                CreationStep.CLASS -> ClassStep(state, viewModel)
                CreationStep.CLASS_CHOICES -> ClassChoicesStep(state, viewModel)
                CreationStep.BACKGROUND -> BackgroundStep(state, viewModel)
                CreationStep.ORIGIN_CHOICES -> OriginChoicesStep(state, viewModel)
                CreationStep.ABILITIES -> AbilitiesStep(state, viewModel)
                CreationStep.DETAILS -> DetailsStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun StepIndicator(current: CreationStep) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CreationStep.ORDER.forEach { step ->
                val done = CreationStep.ORDER.indexOf(step) < CreationStep.ORDER.indexOf(current)
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

@Composable
private fun WizardBottomBar(state: CreationState, onNext: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = hintFor(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onNext,
                enabled = state.canAdvance,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (state.step == CreationStep.DETAILS) tr("Create") else tr("Next"))
            }
        }
    }
}

private fun hintFor(state: CreationState): String = when {
    state.canAdvance && state.step == CreationStep.DETAILS -> tr("Ready to create your character")
    state.canAdvance -> tr("Looks good")
    else -> when (state.step) {
        CreationStep.SOURCES -> tr("Pick at least one book to draw options from")
        CreationStep.SPECIES -> tr("Pick a species and any options it offers")
        CreationStep.CLASS -> tr("Pick a class")
        CreationStep.CLASS_CHOICES -> tr("Complete every option below")
        CreationStep.BACKGROUND -> tr("Pick an origin and assign its ability bonuses")
        CreationStep.ORIGIN_CHOICES -> tr("Resolve every grant your origin left open")
        CreationStep.ABILITIES -> tr("Assign all six ability scores")
        CreationStep.DETAILS -> tr("Give your character a name")
    }
}
