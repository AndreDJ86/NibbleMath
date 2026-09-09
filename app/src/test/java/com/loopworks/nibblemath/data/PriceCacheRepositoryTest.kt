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
class PriceCacheRepositoryTest : DataTestBase() {
    @Test
    fun storeAndLookup() = runBlocking {
        priceCache.store("Woolworths", "milk 1l", "DairyLO Whole 1L", PackSize(1.0, Unit.L), 3.50, 1000L)
        val hit = priceCache.lookup("Woolworths", "DairyLO Whole 1L")
        assertNotNull(hit)
        assertEquals(3.50, hit.price)
        assertEquals("Woolworths", hit.store)
        assertNull(priceCache.lookup("Woolworths", "Other"))
    }

    @Test
    fun storeReplacesSameStoreAndProduct() = runBlocking {
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.00, 1000L)
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.50, 2000L)
        val entries = priceCache.byStore("Woolworths")
        assertEquals(1, entries.size)
        assertEquals(3.50, entries.single().price)
        assertEquals(2000L, entries.single().fetchedAt)
    }

    @Test
    fun clearStore() = runBlocking {
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.00, 1000L)
        priceCache.clear("Woolworths")
        assertTrue(priceCache.byStore("Woolworths").isEmpty())
    }
}
