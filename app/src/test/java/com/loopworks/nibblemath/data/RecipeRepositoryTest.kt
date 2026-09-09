package com.loopworks.nibblemath.data

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecipeRepositoryTest : DataTestBase() {
    @Test
    fun createRecipeWithStepsAndLines() = runBlocking {
        val bookId = books.create("Book")
        val flour = catalog.upsert("Flour", listOf("AP flour"), Unit.G)
        val milk = catalog.upsert("Milk", emptyList(), Unit.ML)
        val recipeId = recipes.create(bookId, "Cake", 8.0, "slices")

        recipes.update(
            Recipe(
                id = recipeId,
                bookId = bookId,
                name = "Cake",
                yieldAmount = 8.0,
                yieldItem = "slices",
                notes = "Preheat oven",
                sortOrder = 0,
                steps = listOf("Mix dry", "Bake 30 min"),
                ingredients = listOf(
                    IngredientLine(0, recipeId, flour, "Flour", null, null, 200.0, Unit.G, 0),
                    IngredientLine(0, recipeId, milk, "Milk", null, null, 250.0, Unit.ML, 1),
                ),
            ),
        )

        val loaded = recipes.get(recipeId)
        assertNotNull(loaded)
        assertEquals("Preheat oven", loaded.notes)
        assertEquals(listOf("Mix dry", "Bake 30 min"), loaded.steps)
        assertEquals(2, loaded.ingredients.size)
        assertEquals("Flour", loaded.ingredients[0].ingredientName)
        assertEquals(200.0, loaded.ingredients[0].amount)
        assertEquals(Unit.G, loaded.ingredients[0].unit)
        assertEquals("Milk", loaded.ingredients[1].ingredientName)
    }

    @Test
    fun updateReplacesStepsAndLines() = runBlocking {
        val bookId = books.create("Book")
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val recipeId = recipes.create(bookId, "Cake", 4.0, "slices")

        recipes.update(
            Recipe(
                id = recipeId, bookId = bookId, name = "Cake", yieldAmount = 4.0,
                yieldItem = "slices", notes = "", sortOrder = 0,
                steps = listOf("Step 1", "Step 2", "Step 3"),
                ingredients = listOf(
                    IngredientLine(0, recipeId, flour, "Flour", null, null, 100.0, Unit.G, 0),
                ),
            ),
        )
        var loaded = recipes.get(recipeId)!!
        assertEquals(3, loaded.steps.size)
        assertEquals(1, loaded.ingredients.size)

        recipes.update(loaded.copy(steps = listOf("Only step"), ingredients = emptyList()))
        loaded = recipes.get(recipeId)!!
        assertEquals(listOf("Only step"), loaded.steps)
        assertTrue(loaded.ingredients.isEmpty())
    }

    @Test
    fun updatePersistsProductChoiceOnLine() = runBlocking {
        val bookId = books.create("Book")
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val recipeId = recipes.create(bookId, "Cookies", 24.0, "cookies")
        val productId = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 5.0)

        recipes.update(
            Recipe(
                id = recipeId,
                bookId = bookId,
                name = "Cookies",
                yieldAmount = 24.0,
                yieldItem = "cookies",
                notes = "",
                sortOrder = 0,
                steps = emptyList(),
                ingredients = listOf(
                    IngredientLine(0, recipeId, flour, "Flour", null, null, 240.0, Unit.G, 0),
                ),
            ),
        )

        val loaded = recipes.get(recipeId)!!
        val updatedLine = loaded.ingredients[0].copy(
            productId = productId,
            productName = "Flour 1kg",
            productPackSize = PackSize(1.0, Unit.KG),
            productPrice = 5.0,
        )
        recipes.update(loaded.copy(ingredients = listOf(updatedLine)))

        val reloaded = recipes.get(recipeId)!!
        assertEquals(productId, reloaded.ingredients[0].productId)
        assertEquals("Flour 1kg", reloaded.ingredients[0].productName)
        assertEquals(PackSize(1.0, Unit.KG), reloaded.ingredients[0].productPackSize)
        assertEquals(5.0, reloaded.ingredients[0].productPrice)
    }

    @Test
    fun summariesListedByBook() = runBlocking {
        val bookId = books.create("Book")
        recipes.create(bookId, "A", 2.0, "servings")
        recipes.create(bookId, "B", 4.0, "servings")
        val otherBook = books.create("Other")
        recipes.create(otherBook, "C", 1.0, "serving")
        assertEquals(2, recipes.summaries(bookId).size)
        assertEquals(1, recipes.summaries(otherBook).size)
    }

    @Test
    fun deleteRecipeRemovesIt() = runBlocking {
        val bookId = books.create("Book")
        val recipeId = recipes.create(bookId, "Cake", 4.0, "slices")
        recipes.delete(recipeId)
        assertNull(recipes.get(recipeId))
    }
}
