package com.loopworks.nibblemath.ui.ocr

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loopworks.nibblemath.AppTestBase
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.ui.recipe.IngredientDraft
import com.loopworks.nibblemath.ui.recipe.RecipeDraft
import com.loopworks.nibblemath.ui.theme.NibbleMathTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class OcrDraftFlowTest : AppTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun savesDraftRecipe() {
        buildContainer()
        val bookId = seedBook("Bakes")
        val flourId = seedCatalog("flour")
        val draft = RecipeDraft(
            name = "Pancakes",
            yieldText = "4",
            yieldItem = "serve",
            notes = "",
            steps = emptyList(),
            ingredients = listOf(
                IngredientDraft(
                    key = 1L,
                    ingredientId = flourId,
                    ingredientName = "flour",
                    amountText = "200",
                    unit = Unit.G,
                ),
            ),
        )
        var savedId: Long? = null

        composeRule.setContent {
            NibbleMathTheme {
                OcrScreen(
                    container = container,
                    bookId = bookId,
                    onBack = {},
                    onSaved = { savedId = it },
                    initialDraft = draft,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Review recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Pancakes").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertNotNull(savedId)
        val saved = runBlocking { recipes.get(savedId!!) }
        assertNotNull(saved)
        assertEquals("Pancakes", saved?.name)
        assertEquals(4.0, saved?.yieldAmount ?: 0.0, 0.0)
        assertEquals(1, saved?.ingredients?.size ?: 0)
        assertEquals("flour", saved?.ingredients?.firstOrNull()?.ingredientName)
    }
}
