package com.loopworks.nibblemath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.ui.books.BooksScreen
import com.loopworks.nibblemath.ui.ocr.OcrScreen
import com.loopworks.nibblemath.ui.pantry.PantryScreen
import com.loopworks.nibblemath.ui.recipe.RecipeEditorScreen
import com.loopworks.nibblemath.ui.recipe.RecipeCookScreen

object Routes {
    const val BOOKS = "books"
    const val RECIPE = "recipe/{recipeId}"
    const val EDITOR = "editor/{recipeId}"
    const val PANTRY = "pantry"
    const val PRICE_LOOKUP = "price-lookup"
    const val OCR = "ocr/{bookId}"

        fun recipe(recipeId: Long) = "recipe/$recipeId"
    fun editor(recipeId: Long) = "editor/$recipeId"
    fun ocr(bookId: Long) = "ocr/$bookId"
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
                onOpenOcr = { bookId -> if (bookId != null) navController.navigate(Routes.ocr(bookId)) },
            )
        }
        composable(
            route = Routes.OCR,
            arguments = listOf(
                androidx.navigation.navArgument("bookId") {
                    type = androidx.navigation.NavType.LongType
                },
            ),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            OcrScreen(
                container = container,
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onSaved = { recipeId ->
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(Routes.BOOKS, false)
                        .build()
                    navController.navigate(Routes.editor(recipeId), navOptions)
                },
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
