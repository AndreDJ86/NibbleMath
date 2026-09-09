package com.loopworks.nibblemath.network

import com.loopworks.nibblemath.core.units.PackSizeParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object PriceParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val ldJson = Regex(
        "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val nextData = Regex(
        "<script[^>]+id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(body: String, store: String, now: Long = System.currentTimeMillis()): List<PriceResult> {
        val candidates = mutableListOf(body)
        ldJson.findAll(body).forEach { candidates += it.groupValues[1].trim() }
        nextData.findAll(body).forEach { candidates += it.groupValues[1].trim() }

        val results = LinkedHashMap<String, PriceResult>()
        candidates.forEach { candidate ->
            val root = parse(candidate) ?: return@forEach
            val arrays = mutableListOf<JsonArray>()
            collectProductArrays(root, arrays)
            arrays.forEach { array ->
                array.forEach { item ->
                    if (item is JsonObject) {
                        parseProduct(item, store, now)?.let { result ->
                            val key = listOf(
                                result.productName.lowercase(),
                                result.price,
                                result.packSize?.toString(),
                                result.url,
                            ).joinToString("|")
                            results[key] = result
                        }
                    }
                }
            }
        }
        return results.values.toList()
    }

    private fun parse(text: String): JsonElement? = try {
        json.parseToJsonElement(text.trim())
    } catch (e: Exception) {
        null
    }

    private fun collectProductArrays(element: JsonElement, out: MutableList<JsonArray>) {
        when (element) {
            is JsonObject -> element.values.forEach { collectProductArrays(it, out) }
            is JsonArray -> {
                if (element.isNotEmpty() &&
                    element.all { it is JsonObject } &&
                    element.any { looksLikeProduct((it as JsonObject)) }
                ) {
                    out.add(element)
                } else {
                    element.forEach { collectProductArrays(it, out) }
                }
            }
            is JsonPrimitive -> Unit
        }
    }

    private fun looksLikeProduct(obj: JsonObject): Boolean {
        val hasName = obj.containsKey("name") ||
            obj.containsKey("title") ||
            obj.containsKey("productName") ||
            obj.containsKey("product_name") ||
            obj.containsKey("Name") ||
            obj.containsKey("DisplayName")
        val hasPrice = obj.containsKey("price") ||
            obj.containsKey("Price") ||
            obj.containsKey("currentPrice") ||
            obj.containsKey("offerPrice") ||
            obj.containsKey("sellingPrice") ||
            obj.containsKey("priceValue")
        return hasName && (hasPrice || hasColesPricing(obj))
    }

    private fun parseProduct(obj: JsonObject, store: String, now: Long): PriceResult? {
        val name = stringOf(
            obj["DisplayName"] ?:
                obj["name"] ?:
                obj["title"] ?:
                obj["productName"] ?:
                obj["product_name"] ?:
                obj["Name"],
        )?.trim().orEmpty()
        if (name.isEmpty()) return null
        if (booleanOf(obj["discontinued"])) return null

        val price = parsePrice(
            aldiPrice(obj) ?:
                obj["Price"] ?:
                obj["price"] ?:
                obj["currentPrice"] ?:
                obj["offerPrice"] ?:
                obj["sellingPrice"] ?:
                obj["priceValue"] ?:
                colesPrice(obj),
        ) ?: return null

        val packText = stringOf(
            obj["PackageSize"] ?:
                obj["packSize"] ?:
                obj["pack"] ?:
                obj["size"] ?:
                obj["sellingSize"] ?:
                obj["packSizeText"] ?:
                obj["unitSize"] ?:
                obj["CupMeasure"] ?:
                obj["pack_size"],
        )?.trim()
        val packSize = packText?.let { PackSizeParser.parse(it) }

        val url = aldiUrl(obj)
            ?: stringOf(obj["url"] ?: obj["link"] ?: obj["href"])
                ?.trim()
                ?.ifEmpty { null }

        return PriceResult(
            store = store,
            productName = name,
            price = price,
            packSize = packSize,
            url = url,
            fetchedAt = now,
        )
    }

    private fun hasColesPricing(obj: JsonObject): Boolean {
        val pricing = obj["pricing"] as? JsonObject ?: return false
        return pricing.containsKey("now") || pricing.containsKey("rawPriceNow")
    }

    private fun colesPrice(obj: JsonObject): JsonElement? =
        (obj["pricing"] as? JsonObject)
            ?.get("now")
            ?: (obj["pricing"] as? JsonObject)
                ?.get("rawPriceNow")

    private fun aldiPrice(obj: JsonObject): JsonElement? {
        val price = obj["price"] as? JsonObject ?: return null
        stringOf(price["amountRelevantDisplay"] ?: price["amountDisplay"])?.let { display ->
            return JsonPrimitive(display)
        }
        val cents = (price["amountRelevant"] ?: price["amount"])
            ?.let { (it as? JsonPrimitive)?.content?.toDoubleOrNull() }
            ?: return null
        return JsonPrimitive(cents / 100.0)
    }

    private fun aldiUrl(obj: JsonObject): String? {
        val slug = stringOf(obj["urlSlugText"])?.trim()?.ifEmpty { null } ?: return null
        val sku = stringOf(obj["sku"])?.trim()?.ifEmpty { null } ?: return null
        return "https://www.aldi.com.au/product/$slug-$sku"
    }

    private fun booleanOf(element: JsonElement?): Boolean =
        element is JsonPrimitive && element.content.equals("true", ignoreCase = true)

    private fun stringOf(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> if (element.isString) element.content else null
        is JsonObject -> stringOf(element["value"] ?: element["text"] ?: element["name"])
        else -> null
    }

    private fun parsePrice(element: JsonElement?): Double? = when (element) {
        is JsonPrimitive -> element.content.let(::cleanPrice)
        is JsonObject -> parsePrice(element["amount"] ?: element["value"] ?: element["price"])
        else -> null
    }

    private fun cleanPrice(raw: String): Double? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (Regex("\\b(for|per)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
        val cleaned = text
            .replace("$", "")
            .replace("USD", "")
            .replace("AUD", "")
            .replace(",", "")
        val match = Regex("\\d+(?:\\.\\d+)?").find(cleaned) ?: return null
        return match.value.toDoubleOrNull()
    }
}
