package com.loopworks.nibblemath.network

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.DataTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PriceLookupClientTest : DataTestBase() {
    @Test
    fun lookupReturnsFreshCacheWithoutNetwork() = runBlocking {
        val now = 1_000_000L
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.50, now)

        val source = FakeSource("Woolworths", emptyList())
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(source),
            clock = { now + 1_000L },
            rateLimiter = RateLimiter({ now + 1_000L }, 0),
        )

        val result = client.lookup("Woolworths", "Milk 1L")

        assertEquals(3.50, result.price)
        assertEquals(0, source.calls)
        assertNull(client.health.value["Woolworths"])
    }

    @Test
    fun lookupRefreshesStaleCache() = runBlocking {
        val now = PriceLookupClient.DEFAULT_CACHE_TTL_MILLIS + 1
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.00, 0L)

        val live = PriceResult(
            store = "Woolworths",
            productName = "Milk 1L",
            price = 3.75,
            packSize = PackSize(1.0, Unit.L),
            url = null,
            fetchedAt = now,
        )
        val source = FakeSource("Woolworths", listOf(live))
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(source),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val result = client.lookup("Woolworths", "Milk 1L")

        assertEquals(3.75, result.price)
        assertEquals(1, source.calls)
        assertEquals(StoreStatus.UP, client.health.value["Woolworths"])
        assertEquals(now, priceCache.lookup("Woolworths", "Milk 1L")?.fetchedAt)
    }

    @Test
    fun lookupFallsBackToStaleCacheOnFailure() = runBlocking {
        val now = PriceLookupClient.DEFAULT_CACHE_TTL_MILLIS + 1
        priceCache.store("Woolworths", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.00, 0L)

        val source = FakeSource("Woolworths", emptyList(), shouldFail = true)
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(source),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val result = client.lookup("Woolworths", "Milk 1L")

        assertEquals(3.00, result.price)
        assertEquals(1, source.calls)
        assertEquals(StoreStatus.DEGRADED, client.health.value["Woolworths"])
    }

    @Test
    fun searchStoresResultsAndMarksStoreUp() = runBlocking {
        val now = 5_000L
        val results = listOf(
            PriceResult(
                store = "Coles",
                productName = "Milk 1L",
                price = 3.20,
                packSize = PackSize(1.0, Unit.L),
                url = null,
                fetchedAt = now,
            ),
            PriceResult(
                store = "Coles",
                productName = "No Pack Milk",
                price = 2.00,
                packSize = null,
                url = null,
                fetchedAt = now,
            ),
        )
        val source = FakeSource("Coles", results)
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(source),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val found = client.search("Coles", "milk")

        assertEquals(2, found.size)
        assertEquals(1, source.calls)
        assertEquals(StoreStatus.UP, client.health.value["Coles"])
        val cached = priceCache.byStore("Coles")
        assertEquals(1, cached.size)
        assertEquals("Milk 1L", cached.single().productName)
    }

    @Test
    fun searchFallsBackToCachedStoreOnFailure() = runBlocking {
        val now = 5_000L
        priceCache.store("Coles", "milk", "Milk 1L", PackSize(1.0, Unit.L), 3.20, now - 100)

        val source = FakeSource("Coles", emptyList(), shouldFail = true)
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(source),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val found = client.search("Coles", "milk")

        assertEquals(1, found.size)
        assertEquals("Milk 1L", found.single().productName)
        assertEquals(StoreStatus.DEGRADED, client.health.value["Coles"])
    }

    @Test
    fun unknownStoreThrows() = runBlocking {
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = emptyList(),
            clock = { 0L },
            rateLimiter = RateLimiter({ 0L }, 0),
        )

        val thrown = runCatching { client.search("Nowhere", "milk") }.exceptionOrNull()
        assertTrue(thrown is IllegalArgumentException)
    }

    @Test
    fun searchStoresReturnsResultsFromAllStores() = runBlocking {
        val now = 5_000L
        val woolworths = FakeSource(
            "Woolworths",
            listOf(
                PriceResult(
                    store = "Woolworths",
                    productName = "Milk 1L",
                    price = 3.50,
                    packSize = PackSize(1.0, Unit.L),
                    url = null,
                    fetchedAt = now,
                ),
            ),
        )
        val coles = FakeSource(
            "Coles",
            listOf(
                PriceResult(
                    store = "Coles",
                    productName = "Milk 1L",
                    price = 3.20,
                    packSize = PackSize(1.0, Unit.L),
                    url = null,
                    fetchedAt = now,
                ),
            ),
        )
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(woolworths, coles),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val found = client.searchStores(null, "milk")

        assertEquals(2, found.size)
        assertEquals(1, woolworths.calls)
        assertEquals(1, coles.calls)
        assertEquals(listOf("Woolworths", "Coles"), client.stores)
    }

    @Test
    fun searchStoresFiltersByStore() = runBlocking {
        val now = 5_000L
        val woolworths = FakeSource("Woolworths", emptyList())
        val coles = FakeSource(
            "Coles",
            listOf(
                PriceResult(
                    store = "Coles",
                    productName = "Milk 1L",
                    price = 3.20,
                    packSize = PackSize(1.0, Unit.L),
                    url = null,
                    fetchedAt = now,
                ),
            ),
        )
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(woolworths, coles),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )

        val found = client.searchStores(listOf("Coles"), "milk")

        assertEquals(1, found.size)
        assertEquals(0, woolworths.calls)
        assertEquals(1, coles.calls)
    }
}

private class FakeSource(
    override val store: String,
    private val results: List<PriceResult>,
    private val shouldFail: Boolean = false,
) : PriceSource {
    var calls = 0

    override suspend fun search(query: String): List<PriceResult> {
        calls++
        if (shouldFail) throw IOException("boom")
        return results
    }
}
