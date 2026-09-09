package com.loopworks.nibblemath.data

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductRepositoryTest : DataTestBase() {
    @Test
    fun upsertAndSearch() = runBlocking {
        products.upsert("DairyLO Whole 1L", PackSize(1.0, Unit.L), 3.50)
        products.upsert("DairyLO Semi 1L", PackSize(1.0, Unit.L), 3.20)
        products.upsert("Organic Milk 2L", PackSize(2.0, Unit.L), 6.00)

        assertEquals(3, products.all().size)
        val dairylo = products.search("DairyLO")
        assertEquals(2, dairylo.size)
        assertEquals(3.50, dairylo.first { it.name == "DairyLO Whole 1L" }.price)
        assertEquals(Unit.L, dairylo.first { it.name == "DairyLO Whole 1L" }.packSize.unit)
    }

    @Test
    fun deleteProduct() = runBlocking {
        val id = products.upsert("Milk", PackSize(1.0, Unit.L), 3.00)
        products.delete(id)
        assertTrue(products.all().isEmpty())
    }

    @Test
    fun byIdReturnsProduct() = runBlocking {
        val id = products.upsert("Milk", PackSize(1.0, Unit.L), 3.00)
        val loaded = products.byId(id)
        assertNotNull(loaded)
        assertEquals("Milk", loaded.name)
        assertNull(products.byId(999))
    }

    @Test
    fun upsertReusesExactName() = runBlocking {
        val first = products.upsert("Milk", PackSize(1.0, Unit.L), 3.00)
        val second = products.upsert("Milk", PackSize(2.0, Unit.L), 5.00)

        assertEquals(first, second)
        val all = products.all()
        assertEquals(1, all.size)
        assertEquals(5.00, all.single().price)
        assertEquals(PackSize(2.0, Unit.L), all.single().packSize)
    }
}
