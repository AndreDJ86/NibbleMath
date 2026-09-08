package com.loopworks.nibblemath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loopworks.nibblemath.ui.books.BooksScreen

object Routes {
    const val BOOKS = "books"
    const val RECIPE = "recipe/{recipeId}"
    const val EDITOR = "editor"
    const val PANTRY = "pantry"
    const val PRICE_LOOKUP = "price-lookup"
    const val OCR = "ocr"

    fun recipe(recipeId: String) = "recipe/$recipeId"
}

@Composable
fun NibbleMathNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.BOOKS,
    ) {
        composable(Routes.BOOKS) {
            BooksScreen(
                onOpenRecipe = { navController.navigate(Routes.recipe(it)) },
            )
        }
    }
}
