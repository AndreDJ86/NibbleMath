package com.loopworks.nibblemath.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

private class FakePriceFetcher(
    private val body: String,
) : PriceFetcher {
    var lastUrl: String? = null
    var lastHeaders: Map<String, String> = emptyMap()
    var lastPostUrl: String? = null
    var lastPostBody: String? = null
    var lastPostHeaders: Map<String, String> = emptyMap()

    override suspend fun get(url: String, headers: Map<String, String>): String {
        lastUrl = url
        lastHeaders = headers
        return body
    }

    override suspend fun post(url: String, body: String, headers: Map<String, String>): String {
        lastPostUrl = url
        lastPostBody = body
        lastPostHeaders = headers
        return this.body
    }
}

class StoreAdapterTest {
    @Test
    fun woolworthsAdapterParsesFixtureAndPostsSearchBody() = runBlocking {
        val fetcher = FakePriceFetcher(TestFixtures.load("products.json"))
        val adapter = WoolworthsAdapter(fetcher)

        val results = adapter.search("milk")

        assertTrue(results.isNotEmpty())
        assertEquals("Woolworths", adapter.store)
        assertEquals("Woolworths", results.first().store)
        assertEquals("https://www.woolworths.com.au/", fetcher.lastUrl)
        assertEquals("https://www.woolworths.com.au/apis/ui/Search/products", fetcher.lastPostUrl)
        assertTrue(fetcher.lastPostBody!!.contains("\"SearchTerm\": \"milk\""))
        assertTrue(fetcher.lastPostHeaders.containsKey("User-Agent"))
        assertTrue(fetcher.lastPostHeaders.containsKey("Content-Type"))
    }

    @Test
    fun colesAdapterParsesFixtureAndSendsUserAgent() = runBlocking {
        val fetcher = FakePriceFetcher(TestFixtures.load("embedded.html"))
        val adapter = ColesAdapter(fetcher)

        val results = adapter.search("bread")

        assertTrue(results.isNotEmpty())
        assertEquals("Coles", adapter.store)
        assertEquals("Coles", results.first().store)
        assertNotNull(fetcher.lastUrl)
        assertTrue(fetcher.lastUrl!!.startsWith("https://www.coles.com.au/search?query="))
        assertTrue(fetcher.lastHeaders.containsKey("User-Agent"))
    }

    @Test
    fun aldiAdapterParsesFixtureAndSendsUserAgent() = runBlocking {
        val fetcher = FakePriceFetcher(TestFixtures.load("products.json"))
        val adapter = AldiAdapter(fetcher)

        val results = adapter.search("eggs")

        assertTrue(results.isNotEmpty())
        assertEquals("ALDI", adapter.store)
        assertEquals("ALDI", results.first().store)
        assertNotNull(fetcher.lastUrl)
        assertTrue(fetcher.lastUrl!!.startsWith("https://www.aldi.com.au/search?q="))
        assertTrue(fetcher.lastHeaders.containsKey("User-Agent"))
    }
}
