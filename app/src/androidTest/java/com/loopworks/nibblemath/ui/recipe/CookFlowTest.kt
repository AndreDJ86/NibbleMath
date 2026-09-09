package com.loopworks.nibblemath.ui.recipe

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loopworks.nibblemath.AppTestBase
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.ui.theme.NibbleMathTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CookFlowTest : AppTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun showsCostAndScalesRecipe() {
        buildContainer()
        val bookId = seedBook("Bakes")
        val flourId = seedCatalog("flour")
        val recipeId = seedRecipe(bookId, "Cake", yieldAmount = 2.0, yieldItem = "tray")
        val productId = seedProduct("Cake flour", PackSize(1000.0, Unit.G), 2.0)
        addIngredientLine(recipeId, flourId, 500.0, Unit.G, productId)

        composeRule.setContent {
            NibbleMathTheme {
                RecipeCookScreen(
                    container = container,
                    recipeId = recipeId.toString(),
                    onBack = {},
                    onEdit = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 tray").assertIsDisplayed()
        composeRule.onNodeWithText("1.00/batch", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("0.50/tray", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("+").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3 tray").assertIsDisplayed()
        composeRule.onNodeWithText("1.50/batch", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("0.50/tray", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("750 g").assertIsDisplayed()

        composeRule.onNodeWithText("−").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 tray").assertIsDisplayed()
    }

    @Test
    fun setsProductFromStoreSearch() {
        val result = priceResult("Woolworths", "Cake flour", 2.5, PackSize(1000.0, Unit.G))
        buildContainer(woolworths = listOf(result))
        val bookId = seedBook("Bakes")
        val flourId = seedCatalog("flour")
        val recipeId = seedRecipe(bookId, "Cake", yieldAmount = 2.0, yieldItem = "tray")
        addIngredientLine(recipeId, flourId, 500.0, Unit.G, null)

        composeRule.setContent {
            NibbleMathTheme {
                RecipeCookScreen(
                    container = container,
                    recipeId = recipeId.toString(),
                    onBack = {},
                    onEdit = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("No product").assertIsDisplayed()
        composeRule.onNodeWithText("flour").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Set product").assertIsDisplayed()

        composeRule.onNodeWithTag("store_search").performTextInput("flour")
        composeRule.onNodeWithTag("store_search_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cake flour").assertIsDisplayed()
        composeRule.onNodeWithTag("store_result_Cake flour").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Cake flour").assertIsDisplayed()
        composeRule.onNodeWithText("1.25/batch", substring = true).assertIsDisplayed()
    }
}
