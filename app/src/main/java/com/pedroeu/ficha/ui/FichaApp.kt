package com.pedroeu.ficha.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.ui.creation.CreationWizardScreen
import com.pedroeu.ficha.ui.home.HomeScreen
import com.pedroeu.ficha.ui.sheet.CharacterSheetScreen

object Routes {
    const val HOME = "home"
    const val CREATE = "create"
    const val SHEET = "sheet"
    fun sheet(id: String) = "$SHEET/$id"
}

@Composable
fun FichaApp(repository: CharacterRepository) {
    val navController = rememberNavController()

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
            )
        }
    }
}
