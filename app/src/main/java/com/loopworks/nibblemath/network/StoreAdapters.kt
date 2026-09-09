package com.loopworks.nibblemath.network

private const val USER_AGENT = "NibbleMath/0.1 (personal recipe cost app)"

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

class ColesAdapter(
    private val fetcher: PriceFetcher,
) : PriceSource {
    override val store: String = "Coles"

    override suspend fun search(query: String): List<PriceResult> {
        val encoded = encodeQuery(query)
        val url = "https://www.coles.com.au/search?query=$encoded"
        val body = fetcher.get(url, defaultHeaders())
        return PriceParser.parse(body, store)
    }
}

class AldiAdapter(
    private val fetcher: PriceFetcher,
) : PriceSource {
    override val store: String = "ALDI"

    override suspend fun search(query: String): List<PriceResult> {
        val encoded = encodeQuery(query)
        val url = "https://www.aldi.com.au/search?q=$encoded"
        val body = fetcher.get(url, defaultHeaders())
        return PriceParser.parse(body, store)
    }
}
