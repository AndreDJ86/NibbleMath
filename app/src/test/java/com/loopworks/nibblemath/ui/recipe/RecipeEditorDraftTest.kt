package com.loopworks.nibblemath.ui.recipe

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeEditorDraftTest {
    @Test
    fun fromRecipeMapsFields() {
        val recipe = Recipe(
            id = 1,
            bookId = 10,
            name = "Cake",
            yieldAmount = 8.0,
            yieldItem = "slices",
            notes = "notes",
            sortOrder = 0,
            steps = listOf(" Mix ", "Bake", ""),
            ingredients = listOf(
                IngredientLine(
                    id = 5,
                    recipeId = 1,
                    ingredientId = 2,
                    ingredientName = "Flour",
                    productId = 7,
                    productName = "Dairy",
                    amount = 200.0,
                    unit = Unit.G,
                    sortOrder = 0,
                    productPackSize = PackSize(1.0, Unit.KG),
                    productPrice = 1.5,
                ),
            ),
        )

        val draft = RecipeDraft.fromRecipe(recipe)

        assertEquals("Cake", draft.name)
        assertEquals("8.0", draft.yieldText)
        assertEquals("slices", draft.yieldItem)
        assertEquals("notes", draft.notes)
        assertEquals(listOf(" Mix ", "Bake", ""), draft.steps)
        assertEquals(1, draft.ingredients.size)
        assertEquals(5L, draft.ingredients[0].key)
        assertEquals(2L, draft.ingredients[0].ingredientId)
        assertEquals("Flour", draft.ingredients[0].ingredientName)
        assertEquals("200.0", draft.ingredients[0].amountText)
        assertEquals(Unit.G, draft.ingredients[0].unit)
        assertEquals(7L, draft.ingredients[0].productId)
        assertEquals("Dairy", draft.ingredients[0].productName)
        assertEquals(PackSize(1.0, Unit.KG), draft.ingredients[0].productPackSize)
        assertEquals(1.5, draft.ingredients[0].productPrice)
        assertTrue(draft.canSave)
    }

    @Test
    fun fromNullRecipeUsesDefaults() {
        val draft = RecipeDraft.fromRecipe(null)
        assertEquals("", draft.name)
        assertEquals("1", draft.yieldText)
        assertEquals("", draft.yieldItem)
        assertTrue(draft.steps.isEmpty())
        assertTrue(draft.ingredients.isEmpty())
        assertFalse(draft.canSave)
    }

    @Test
    fun canSaveRequiresValidNameYieldAndIngredients() {
        val base = RecipeDraft("Cake", "8", "slices", "", emptyList(), emptyList())
        assertTrue(base.canSave)
        assertFalse(base.copy(name = " ").canSave)
        assertFalse(base.copy(yieldText = "0").canSave)
        assertFalse(base.copy(yieldText = "abc").canSave)

        val invalidLine = IngredientDraft(-1, null, "", "1", Unit.G)
        assertFalse(base.copy(ingredients = listOf(invalidLine)).canSave)

        val validLine = IngredientDraft(-1, 2L, "Flour", "1.5", Unit.G)
        assertTrue(base.copy(ingredients = listOf(validLine)).canSave)
    }

    @Test
    fun toRecipeTrimsStepsAndPreservesProductDetails() {
        val base = Recipe(
            id = 1,
            bookId = 10,
            name = "Old",
            yieldAmount = 2.0,
            yieldItem = "servings",
            notes = "",
            sortOrder = 0,
            steps = emptyList(),
            ingredients = emptyList(),
        )
        val draft = RecipeDraft(
            name = " Cake ",
            yieldText = " 8 ",
            yieldItem = "slices",
            notes = "notes",
            steps = listOf(" Mix ", "", "Bake"),
            ingredients = listOf(
                IngredientDraft(
                    key = -1,
                    ingredientId = 2L,
                    ingredientName = " Flour ",
                    amountText = " 200 ",
                    unit = Unit.G,
                    productId = 7L,
                    productName = "Dairy",
                    productPackSize = PackSize(1.0, Unit.KG),
                    productPrice = 1.5,
                ),
            ),
        )

        val saved = draft.toRecipe(base)

        assertEquals("Cake", saved.name)
        assertEquals(8.0, saved.yieldAmount)
        assertEquals("slices", saved.yieldItem)
        assertEquals("notes", saved.notes)
        assertEquals(listOf("Mix", "Bake"), saved.steps)
        assertEquals(1, saved.ingredients.size)
        val line = saved.ingredients[0]
        assertEquals(0L, line.id)
        assertEquals(1L, line.recipeId)
        assertEquals(2L, line.ingredientId)
        assertEquals("Flour", line.ingredientName)
        assertEquals(200.0, line.amount)
        assertEquals(Unit.G, line.unit)
        assertEquals(0, line.sortOrder)
        assertEquals(7L, line.productId)
        assertEquals("Dairy", line.productName)
        assertEquals(PackSize(1.0, Unit.KG), line.productPackSize)
        assertEquals(1.5, line.productPrice)
    }
}
