package com.pedroeu.ficha.ui.home

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pedroeu.ficha.BuildConfig
import com.pedroeu.ficha.data.BackupFiles
import com.pedroeu.ficha.data.CharacterBackup
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.domain.ClassLevels
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.i18n.AppLanguage
import com.pedroeu.ficha.ui.i18n.LocalLanguageController
import com.pedroeu.ficha.ui.theme.LocalThemeController
import com.pedroeu.ficha.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: CharacterRepository,
    onCreateCharacter: () -> Unit,
    onOpenCharacter: (String) -> Unit,
) {
    val characters by remember { repository.observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<PlayerCharacter?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("Ficha"), style = MaterialTheme.typography.headlineMedium)
                        // A visible build stamp: if this doesn't match what you just installed,
                        // the device is still running an older APK.
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    LanguageButton()
                    AppearanceButton()
                    BackupMenu(repository)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateCharacter,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(tr("New Character")) },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        if (characters.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(characters, key = { it.id }) { character ->
                    CharacterRow(
                        character = character,
                        onClick = { onOpenCharacter(character.id) },
                        onDelete = { pendingDelete = character },
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(trf("Delete {0}?", target.name)) },
            text = { Text(tr("This character will be permanently removed from this device.")) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.delete(target.id) }
                    pendingDelete = null
                }) { Text(tr("Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(tr("Cancel")) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterRow(
    character: PlayerCharacter,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val species = SpeciesData.byId(character.speciesId)?.name ?: tr("Unknown")
    // "Fighter 5 / Wizard 3" once multiclassed, just the class name otherwise.
    val charClass = ClassLevels.label(character).ifBlank { tr("Unknown") }
    val background = BackgroundData.byId(character.backgroundId)?.name ?: ""

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = trf("Level {0} {1} {2}", character.level, species, charClass),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (background.isNotEmpty()) {
                    Text(
                        text = background,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${CharacterCalculations.armorClass(character)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = tr("AC"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${character.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = tr("No characters yet"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = tr("Tap New Character to roll up your first adventurer: pick a species, a class, an origin, and your ability scores."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Switches between the daylight and candlelight schemes, or hands the decision back to the
 * phone. Tapping cycles the three; holding isn't discoverable enough for something a player
 * changes when the light in the room changes, so a menu names each one.
 */
@Composable
private fun AppearanceButton() {
    val theme = LocalThemeController.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = if (theme.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                // The label says what tapping leads to, which is what a screen reader needs.
                contentDescription = if (theme.isDark) tr("Appearance: candlelight") else tr("Appearance: daylight"),
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        theme.setMode(mode)
                        showMenu = false
                    },
                    trailingIcon = {
                        if (mode == theme.mode) {
                            Icon(Icons.Default.Check, contentDescription = tr("Selected"))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Switches the interface between English and Brazilian Portuguese.
 *
 * A menu rather than a toggle, so the language a player wants is named in that language and
 * they can find it without reading the one they don't speak. The choice is saved, so it holds
 * across launches — the rules text stays in English either way, since translating a rule
 * loosely is worse than leaving it in the words the book uses.
 */
@Composable
private fun LanguageButton() {
    val languages = LocalLanguageController.current
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = tr("Language") + ": " + languages.language.label,
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    // Each language names itself, so the one you want is legible whichever
                    // is currently in force.
                    text = { Text(language.label) },
                    onClick = {
                        languages.setLanguage(language)
                        showMenu = false
                    },
                    trailingIcon = {
                        if (language == languages.language) {
                            Icon(Icons.Default.Check, contentDescription = tr("Selected"))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Backing characters up and restoring them.
 *
 * Updating the app means installing over the old build, and when that doesn't take, the
 * advice is to uninstall first — which deletes the database along with it. A character that
 * took an evening to build shouldn't be a casualty of a version bump, so this writes them to
 * a file the uninstall can't reach.
 */
@Composable
private fun BackupMenu(repository: CharacterRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember(context) { BackupFiles(context) }

    var showMenu by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CharacterBackup.MIME_TYPE)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val characters = repository.getAll()
            val text = CharacterBackup.encode(
                characters = characters,
                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                now = System.currentTimeMillis(),
            )
            result = files.write(uri, text).fold(
                onSuccess = {
                    val count = characters.size
                    "Backed up $count character${if (count == 1) "" else "s"}."
                },
                onFailure = { it.message ?: tr("Couldn't write that file.") },
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            result = files.read(uri)
                .mapCatching { text -> CharacterBackup.decode(text).getOrThrow() }
                .fold(
                    onSuccess = { incoming ->
                        val plan = CharacterBackup.plan(repository.getAll(), incoming)
                        repository.restore(plan.toWrite)
                        plan.summary()
                    },
                    onFailure = { it.message ?: tr("Couldn't read that file.") },
                )
        }
    }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = tr("Backup and restore"))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(tr("Back up characters")) },
                onClick = {
                    showMenu = false
                    exportLauncher.launch(
                        CharacterBackup.fileName(System.currentTimeMillis())
                    )
                },
                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(tr("Restore from backup")) },
                onClick = {
                    showMenu = false
                    // Some file providers hand back a generic type for a .json file, so
                    // anything is accepted and the contents decide whether it's a backup.
                    importLauncher.launch(arrayOf("*/*"))
                },
                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
            )
        }
    }

    result?.let { message ->
        AlertDialog(
            onDismissRequest = { result = null },
            title = { Text(tr("Backup")) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { result = null }) { Text(tr("OK")) }
            },
        )
    }
}
