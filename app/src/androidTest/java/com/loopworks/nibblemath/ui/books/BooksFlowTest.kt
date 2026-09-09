package com.loopworks.nibblemath.ui.books

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loopworks.nibblemath.AppTestBase
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.ui.theme.NibbleMathTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class BooksFlowTest : AppTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun showsEmptyStateAndCreatesBook() {
        buildContainer()
        composeRule.setContent {
            NibbleMathTheme {
                BooksScreen(
                    container = container,
                    onOpenRecipe = {},
                    onOpenEditor = {},
                    onOpenPantry = {},
                    onOpenOcr = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No recipe books yet").assertIsDisplayed()
        composeRule.onAllNodesWithText("Add book").onFirst().performClick()
        composeRule.onAllNodes(isEditable()).onFirst().performTextInput("Bakes")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Bakes").onFirst().assertIsDisplayed()
        assertEquals(1, books.books().size)
    }

    @Test
    fun showsSeededBookAndRecipeCost() {
        buildContainer()
        val bookId = seedBook("Bakes")
        val flourId = seedCatalog("All-purpose flour")
        val recipeId = seedRecipe(bookId, "Cake", yieldAmount = 2.0, yieldItem = "tray")
        val productId = seedProduct("Cake flour", PackSize(1000.0, Unit.G), 2.0)
        addIngredientLine(recipeId, flourId, 500.0, Unit.G, productId)

        composeRule.setContent {
            NibbleMathTheme {
                BooksScreen(
                    container = container,
                    onOpenRecipe = {},
                    onOpenEditor = {},
                    onOpenPantry = {},
                    onOpenOcr = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Bakes").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Cake").assertIsDisplayed()
        composeRule.onNodeWithText("1.00/batch", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("0.50/tray", substring = true).assertIsDisplayed()
    }
}
