package com.loopworks.nibblemath.ui.pantry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loopworks.nibblemath.AppTestBase
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.ui.theme.NibbleMathTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PantryFlowTest : AppTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun showsEmptyState() {
        buildContainer()
        composeRule.setContent {
            NibbleMathTheme {
                PantryScreen(
                    container = container,
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Add"), 10_000)
        composeRule.waitUntilAtLeastOneExists(hasText("Pantry is empty"), 10_000)
        composeRule.onNodeWithText("Pantry is empty").assertIsDisplayed()
    }

    @Test
    fun addsEntryWithProduct() {
        buildContainer()
        seedCatalog("flour")

        composeRule.setContent {
            NibbleMathTheme {
                PantryScreen(
                    container = container,
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Add").onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Choose ingredient").assertIsDisplayed()
        composeRule.onNodeWithText("flour").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Choose product").assertIsDisplayed()

        composeRule.onNodeWithTag("product_dialog_scroll").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("product_pack").performTextInput("1000 g")
        composeRule.onNodeWithTag("product_price").performTextInput("2.00")
        composeRule.onNodeWithTag("product_dialog_scroll").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("product_save").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("pantry_entry"), 10_000)

        val entries = runBlocking { pantry.entries() }
        assertEquals(1, entries.size)
        assertEquals("flour", entries.first().ingredientName)
    }

    @Test
    fun usesEntryInRecipe() {
        buildContainer()
        val bookId = seedBook("Bakes")
        val flourId = seedCatalog("flour")
        val recipeId = seedRecipe(bookId, "Cake", yieldAmount = 2.0, yieldItem = "tray")
        val productId = seedProduct("Cake flour", PackSize(1000.0, Unit.G), 2.0)
        seedPantryEntry(flourId, productId)

        composeRule.setContent {
            NibbleMathTheme {
                PantryScreen(
                    container = container,
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("flour"), 10_000)
        composeRule.onNodeWithText("flour").assertIsDisplayed()
        composeRule.onNodeWithText("Use").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Use in recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Cake").performClick()
        composeRule.waitForIdle()

        val recipe = runBlocking { recipes.get(recipeId) } ?: error("recipe $recipeId not found")
        val line = recipe.ingredients.first { it.ingredientId == flourId }
        assertEquals(1.0, line.amount, 0.0)
        assertEquals(Unit.G, line.unit)
    }

    @Test
    fun deletesEntry() {
        buildContainer()
        val flourId = seedCatalog("flour")
        val productId = seedProduct("Cake flour", PackSize(1000.0, Unit.G), 2.0)
        seedPantryEntry(flourId, productId)

        composeRule.setContent {
            NibbleMathTheme {
                PantryScreen(
                    container = container,
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("flour"), 10_000)
        composeRule.onNodeWithText("flour").assertIsDisplayed()
        composeRule.onAllNodesWithText("Delete").onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete pantry entry?").assertIsDisplayed()
        composeRule.onAllNodesWithText("Delete").onLast().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntilAtLeastOneExists(hasText("Pantry is empty"), 10_000)
        composeRule.onNodeWithText("Pantry is empty").assertIsDisplayed()
        assertEquals(0, runBlocking { pantry.entries() }.size)
    }
}
