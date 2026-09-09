package com.loopworks.nibblemath.data

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PantryRepositoryTest : DataTestBase() {
    @Test
    fun addAndListEntries() = runBlocking {
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val milk = catalog.upsert("Milk", emptyList(), Unit.ML)
        val flourProduct = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 2.50)
        val milkProduct = products.upsert("Milk 1L", PackSize(1.0, Unit.L), 3.00)

        pantry.add(flour, flourProduct)
        pantry.add(milk, milkProduct)

        val entries = pantry.entries()
        assertEquals(2, entries.size)
        val flourEntry = entries.first { it.ingredientId == flour }
        assertEquals("Flour", flourEntry.ingredientName)
        assertEquals("Flour 1kg", flourEntry.productName)
    }

    @Test
    fun forIngredientFilters() = runBlocking {
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val milk = catalog.upsert("Milk", emptyList(), Unit.ML)
        val flourProduct = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 2.50)
        val milkProduct = products.upsert("Milk 1L", PackSize(1.0, Unit.L), 3.00)
        pantry.add(flour, flourProduct)
        pantry.add(milk, milkProduct)

        val flourEntries = pantry.forIngredient(flour)
        assertEquals(1, flourEntries.size)
        assertEquals(flour, flourEntries.single().ingredientId)
    }

    @Test
    fun removeEntry() = runBlocking {
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val flourProduct = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 2.50)
        pantry.add(flour, flourProduct)
        val entryId = pantry.entries().single().id
        pantry.remove(entryId)
        assertTrue(pantry.entries().isEmpty())
    }

    @Test
    fun updateProductChangesProductId() = runBlocking {
        val flour = catalog.upsert("Flour", emptyList(), Unit.G)
        val oldProduct = products.upsert("Flour 1kg", PackSize(1.0, Unit.KG), 2.50)
        val newProduct = products.upsert("Flour 2kg", PackSize(2.0, Unit.KG), 4.00)
        pantry.add(flour, oldProduct)

        val entryId = pantry.entries().single().id
        pantry.updateProduct(entryId, newProduct)

        val updated = pantry.entries().single()
        assertEquals(newProduct, updated.productId)
        assertEquals("Flour 2kg", updated.productName)
        assertEquals(PackSize(2.0, Unit.KG), updated.productPackSize)
        assertEquals(4.00, updated.productPrice)
    }
}
