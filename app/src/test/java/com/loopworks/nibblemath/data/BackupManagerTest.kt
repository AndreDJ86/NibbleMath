package com.loopworks.nibblemath.data

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest : DataTestBase() {
    @Test
    fun exportAndRestoreRoundTrip() = runBlocking {
        val bookId = books.create("Book")
        val flour = catalog.upsert("Flour", listOf("AP flour"), Unit.G)
        val product = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 2.5)
        val recipeId = recipes.create(bookId, "Cake", 4.0, "slices")
        recipes.update(
            Recipe(
                id = recipeId,
                bookId = bookId,
                name = "Cake",
                yieldAmount = 4.0,
                yieldItem = "slices",
                notes = "Preheat oven",
                sortOrder = 0,
                steps = listOf("Mix", "Bake"),
                ingredients = listOf(
                    IngredientLine(0, recipeId, flour, "Flour", product, "Flour 1kg", 100.0, Unit.G, 0),
                ),
            ),
        )
        pantry.add(flour, product)
        priceCache.store("Woolworths", "flour", "Flour 1kg", PackSize(1.0, Unit.KG), 2.5, 1_000L)

        val exported = backup.export()
        assertEquals(1, JSONObject(exported).getInt("version"))

        books.delete(bookId)
        catalog.delete(flour)
        products.delete(product)
        priceCache.clear("Woolworths")
        assertTrue(books.books().isEmpty())
        assertTrue(recipes.get(recipeId) == null)
        assertTrue(catalog.all().isEmpty())
        assertTrue(products.all().isEmpty())
        assertTrue(pantry.entries().isEmpty())
        assertTrue(priceCache.byStore("Woolworths").isEmpty())

        backup.restore(exported)

        assertEquals("Book", books.books().single().name)
        assertEquals("Flour", catalog.all().single().name)
        assertEquals("Flour 1kg", products.all().single().name)

        val restored = recipes.get(recipeId)
        assertNotNull(restored)
        assertEquals("Cake", restored.name)
        assertEquals("Preheat oven", restored.notes)
        assertEquals(listOf("Mix", "Bake"), restored.steps)
        assertEquals(1, restored.ingredients.size)
        assertEquals(flour, restored.ingredients[0].ingredientId)
        assertEquals(product, restored.ingredients[0].productId)
        assertEquals(100.0, restored.ingredients[0].amount)
        assertEquals(Unit.G, restored.ingredients[0].unit)

        val pantryEntry = pantry.entries().single()
        assertEquals(flour, pantryEntry.ingredientId)
        assertEquals(product, pantryEntry.productId)

        val cached = priceCache.byStore("Woolworths").single()
        assertEquals("Flour 1kg", cached.productName)
        assertEquals(2.5, cached.price)
        assertEquals(1_000L, cached.fetchedAt)
    }

    @Test
    fun restoreRejectsUnsupportedVersion() {
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                backup.restore("""{"app":"nibblemath","version":999}""")
            }
        }
    }

    @Test
    fun restoreRejectsMalformedJson() {
        runBlocking {
            assertFailsWith<IllegalArgumentException> {
                backup.restore("{not json")
            }
        }
    }
}
