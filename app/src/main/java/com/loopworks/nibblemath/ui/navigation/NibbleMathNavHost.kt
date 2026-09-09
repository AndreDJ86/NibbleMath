package com.loopworks.nibblemath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.ui.books.BooksScreen
import com.loopworks.nibblemath.ui.pantry.PantryScreen
import com.loopworks.nibblemath.ui.recipe.RecipeEditorScreen
import com.loopworks.nibblemath.ui.recipe.RecipeCookScreen

object Routes {
    const val BOOKS = "books"
    const val RECIPE = "recipe/{recipeId}"
    const val EDITOR = "editor/{recipeId}"
    const val PANTRY = "pantry"
    const val PRICE_LOOKUP = "price-lookup"
    const val OCR = "ocr"

        fun recipe(recipeId: Long) = "recipe/$recipeId"
    fun editor(recipeId: Long) = "editor/$recipeId"
}

@Composable
fun NibbleMathNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.BOOKS,
    ) {
        composable(Routes.BOOKS) {
            BooksScreen(
                container = container,
                onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
                onOpenEditor = { navController.navigate(Routes.editor(it)) },
                onOpenPantry = { navController.navigate(Routes.PANTRY) },
            )
        }
        composable(Routes.PANTRY) {
            PantryScreen(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                androidx.navigation.navArgument("recipeId") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId").orEmpty()
            RecipeEditorScreen(
                container = container,
                recipeId = recipeId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.RECIPE,
            arguments = listOf(
                androidx.navigation.navArgument("recipeId") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId").orEmpty()
            RecipeCookScreen(
                container = container,
                recipeId = backStackEntry.arguments?.getString("recipeId").orEmpty(),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(it)) },
            )
        }
    }
}
