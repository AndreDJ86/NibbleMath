package com.loopworks.nibblemath.network

import java.io.IOException

private const val USER_AGENT = "NibbleMath/0.1 (personal recipe cost app)"
private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126 Safari/537.36"

private fun defaultHeaders(): Map<String, String> = mapOf(
    "User-Agent" to USER_AGENT,
    "Accept" to "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
    "Accept-Language" to "en-AU,en;q=0.9",
)

private fun encodeQuery(value: String): String = buildString {
    value.toByteArray().forEach { byte ->
        val b = byte.toInt() and 0xFF
        val ch = if (b < 128) b.toChar() else null
        if (ch != null && (ch.isLetterOrDigit() || ch in "-_.~")) {
            append(ch)
        } else {
            append('%')
            append(Integer.toHexString(b).uppercase().padStart(2, '0'))
        }
    }
}

class WoolworthsAdapter(
    private val fetcher: PriceFetcher,
) : PriceSource {
    override val store: String = "Woolworths"

    @Volatile
    private var cookiesPrimed = false

    override suspend fun search(query: String): List<PriceResult> {
        if (!cookiesPrimed) {
            fetcher.get("https://www.woolworths.com.au/", defaultHeaders())
            cookiesPrimed = true
        }
        val url = "https://www.woolworths.com.au/apis/ui/Search/products"
        val body = fetcher.post(url, woolworthsSearchBody(query), woolworthsHeaders())
        return PriceParser.parse(body, store)
    }

    private fun woolworthsHeaders(): Map<String, String> =
        defaultHeaders() +
            ("Accept" to "application/json, text/plain, */*") +
            ("Content-Type" to "application/json") +
            ("Origin" to "https://www.woolworths.com.au") +
            ("Referer" to "https://www.woolworths.com.au/shop/search/products")

    private fun woolworthsSearchBody(query: String): String {
        val location = jsonEscape("/shop/search/products?searchTerm=${encodeQuery(query)}")
        val searchTerm = jsonEscape(query)
        return """
            {
              "Filters": [],
              "IsSpecial": false,
              "Location": $location,
              "PageNumber": 1,
              "PageSize": 20,
              "SearchTerm": $searchTerm,
              "SortType": "BestMatch",
              "IsRegisteredRewardCardPromotion": null,
              "ExcludeSearchTypes": ["UntraceableVendors"],
              "GpBoost": 0,
              "GroupEdmVariants": true,
              "EnableAdReRanking": false
            }
        """.trimIndent()
    }
}

private fun jsonEscape(value: String): String = buildString {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u${ch.code.toString(16).padStart(4, '0')}")
            } else {
                append(ch)
            }
        }
    }
    append('"')
}

fun interface ColesApiKeyProvider {
    suspend fun provideKey(): String
}

private class DefaultColesApiKeyProvider(
    private val fetcher: PriceFetcher,
) : ColesApiKeyProvider {
    @Volatile
    private var cachedKey: String? = null

    override suspend fun provideKey(): String {
        cachedKey?.let { return it }
        val html = fetcher.get("https://www.coles.com.au/", colesHeaders())
        val key = COLES_BFF_KEY_REGEX.find(html)?.groupValues?.get(1)
            ?: throw IOException("Coles BFF subscription key not found")
        cachedKey = key
        return key
    }
}

private val COLES_BFF_KEY_REGEX =
    Regex("\"BFF_API_SUBSCRIPTION_KEY\"\\s*:\\s*\"([A-Za-z0-9]+)\"")

private const val COLES_DEFAULT_STORE_ID = "840"

private fun colesHeaders(): Map<String, String> = mapOf(
    "User-Agent" to BROWSER_USER_AGENT,
    "Accept" to "application/json, text/plain, */*",
    "Accept-Language" to "en-AU,en;q=0.9",
    "Origin" to "https://www.coles.com.au",
    "Referer" to "https://www.coles.com.au/",
    "x-api-version" to "2",
)

class ColesAdapter(
    private val fetcher: PriceFetcher,
    private val keyProvider: ColesApiKeyProvider = DefaultColesApiKeyProvider(fetcher),
) : PriceSource {
    override val store: String = "Coles"

    @Volatile
    private var apiKey: String? = null

    override suspend fun search(query: String): List<PriceResult> {
        val key = apiKey ?: keyProvider.provideKey().also { apiKey = it }
        val url = "https://www.coles.com.au/api/bff/products/search" +
            "?searchTerm=${encodeQuery(query)}" +
            "&start=0" +
            "&excludeAds=true" +
            "&pageType=search" +
            "&storeId=$COLES_DEFAULT_STORE_ID"
        val body = fetcher.get(url, colesHeaders() + ("Ocp-Apim-Subscription-Key" to key))
        return PriceParser.parse(body, store)
    }
}

private const val ALDI_API_BASE_URL = "https://asl.api.aldi.com.au/commerce"

private const val ALDI_DEFAULT_CURRENCY = "AUD"

private const val ALDI_DEFAULT_SERVICE_TYPE = "walk-in"

private const val ALDI_PAGE_SIZE = 30

private fun aldiHeaders(): Map<String, String> = mapOf(
    "User-Agent" to BROWSER_USER_AGENT,
    "Accept" to "*/*",
    "Accept-Language" to "en-AU",
    "Origin" to "https://www.aldi.com.au",
    "Referer" to "https://www.aldi.com.au/",
    "sec-ch-ua" to "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-ch-ua-platform" to "\"Windows\"",
    "Sec-Fetch-Site" to "same-site",
    "Sec-Fetch-Mode" to "cors",
    "Sec-Fetch-Dest" to "empty",
)

class AldiAdapter(
    private val fetcher: PriceFetcher,
) : PriceSource {
    override val store: String = "ALDI"

    override suspend fun search(query: String): List<PriceResult> {
        val url = "$ALDI_API_BASE_URL/v3/product-search" +
            "?q=${encodeQuery(query)}" +
            "&limit=$ALDI_PAGE_SIZE" +
            "&offset=0" +
            "&currency=$ALDI_DEFAULT_CURRENCY" +
            "&serviceType=$ALDI_DEFAULT_SERVICE_TYPE"
        val body = fetcher.get(url, aldiHeaders())
        return PriceParser.parse(body, store)
    }
}
