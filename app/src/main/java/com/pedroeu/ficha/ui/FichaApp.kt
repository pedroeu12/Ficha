package com.pedroeu.ficha.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.ui.creation.CreationWizardScreen
import com.pedroeu.ficha.ui.home.HomeScreen
import com.pedroeu.ficha.ui.i18n.AppLanguage
import com.pedroeu.ficha.ui.i18n.Language
import com.pedroeu.ficha.ui.i18n.LanguageController
import com.pedroeu.ficha.ui.i18n.LocalLanguageController
import com.pedroeu.ficha.ui.layout.LayoutController
import com.pedroeu.ficha.ui.layout.LayoutMode
import com.pedroeu.ficha.ui.layout.LocalLayoutController
import com.pedroeu.ficha.ui.levelup.LevelUpScreen
import com.pedroeu.ficha.ui.sheet.CharacterSheetScreen

object Routes {
    const val HOME = "home"
    const val CREATE = "create"
    const val SHEET = "sheet"
    const val LEVEL_UP = "levelup"
    fun sheet(id: String) = "$SHEET/$id"
    fun levelUp(id: String) = "$LEVEL_UP/$id"
}

@Composable
fun FichaApp(
    repository: CharacterRepository,
    language: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {},
    layout: LayoutMode = LayoutMode.AUTOMATIC,
    onLayoutChange: (LayoutMode) -> Unit = {},
) {
    val navController = rememberNavController()

    // Read once here so the whole tree recomposes when it changes. tr() itself reads a plain
    // value rather than a CompositionLocal — it has to be callable outside a composable — so
    // without this the text would only change on the next navigation.
    val languageController = remember(language, onLanguageChange) {
        Language.current = language
        LanguageController(language = language, setLanguage = onLanguageChange)
    }

    val layoutController = remember(layout, onLayoutChange) {
        LayoutController(mode = layout, setMode = onLayoutChange)
    }

    CompositionLocalProvider(
        LocalLanguageController provides languageController,
        LocalLayoutController provides layoutController,
    ) {
        key(language) {
            AppNavHost(navController, repository)
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    repository: CharacterRepository,
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onCreateCharacter = { navController.navigate(Routes.CREATE) },
                onOpenCharacter = { id -> navController.navigate(Routes.sheet(id)) },
            )
        }
        composable(Routes.CREATE) {
            CreationWizardScreen(
                repository = repository,
                onExit = { navController.popBackStack() },
                onFinished = { id ->
                    navController.navigate(Routes.sheet(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(
            route = "${Routes.SHEET}/{characterId}",
            arguments = listOf(navArgument("characterId") { type = NavType.StringType }),
        ) { entry ->
            val characterId = entry.arguments?.getString("characterId").orEmpty()
            CharacterSheetScreen(
                characterId = characterId,
                repository = repository,
                onBack = { navController.popBackStack() },
                onLevelUp = { navController.navigate(Routes.levelUp(characterId)) },
            )
        }
        composable(
            route = "${Routes.LEVEL_UP}/{characterId}",
            arguments = listOf(navArgument("characterId") { type = NavType.StringType }),
        ) { entry ->
            val characterId = entry.arguments?.getString("characterId").orEmpty()
            LevelUpScreen(
                characterId = characterId,
                repository = repository,
                onExit = { navController.popBackStack() },
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
