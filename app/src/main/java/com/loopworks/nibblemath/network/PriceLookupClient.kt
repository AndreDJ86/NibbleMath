package com.loopworks.nibblemath.network

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.data.model.PriceCache
import com.loopworks.nibblemath.data.repository.PriceCacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class PriceLookupClient(
    private val priceCache: PriceCacheRepository,
    private val sources: List<PriceSource>,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val rateLimiter: RateLimiter = RateLimiter(clock),
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
) {
    private val sourceByStore: Map<String, PriceSource> = sources.associateBy { it.store }
    private val _health = MutableStateFlow<Map<String, StoreStatus>>(emptyMap())
    val health: StateFlow<Map<String, StoreStatus>> = _health.asStateFlow()
    val stores: List<String> = sources.map { it.store }.distinct()

    suspend fun searchStores(stores: List<String>?, query: String): List<PriceResult> = coroutineScope {
        val targets = stores?.mapNotNull { sourceByStore[it] }?.ifEmpty { null }
            ?: sourceByStore.values.toList()
        targets.map { source -> async { search(source.store, query) } }.awaitAll().flatten()
    }

    suspend fun search(store: String, query: String): List<PriceResult> {
        val source = requireSource(store)
        return try {
            val results = rateLimiter.withLimit { source.search(query) }
            persistResults(source.store, query, results)
            setHealth(store, StoreStatus.UP)
            results
        } catch (e: Exception) {
            val fallback = fallbackFor(store, query)
            setHealth(store, if (fallback.isEmpty()) StoreStatus.DOWN else StoreStatus.DEGRADED)
            fallback
        }
    }

    suspend fun lookup(store: String, productName: String): PriceResult {
        val source = requireSource(store)
        val cached = priceCache.lookup(store, productName)
        val now = clock()
        if (cached != null && now - cached.fetchedAt < cacheTtlMillis) {
            return cached.toResult()
        }

        try {
            val results = rateLimiter.withLimit { source.search(productName) }
            persistResults(source.store, productName, results)
            setHealth(store, StoreStatus.UP)

            val match = results.firstOrNull { it.productName.equals(productName, ignoreCase = true) }
                ?: results.firstOrNull()
            if (match != null) return match

            if (cached != null) {
                setHealth(store, StoreStatus.DEGRADED)
                return cached.toResult()
            }
            throw IOException("No price results for $productName at $store")
        } catch (e: Exception) {
            if (cached != null) {
                setHealth(store, StoreStatus.DEGRADED)
                return cached.toResult()
            }
            setHealth(store, StoreStatus.DOWN)
            throw e
        }
    }

    fun healthFor(store: String): Flow<StoreStatus> =
        health.map { it[store] ?: StoreStatus.UNKNOWN }

    private suspend fun persistResults(store: String, query: String, results: List<PriceResult>) {
        val now = clock()
        results.forEach { result ->
            result.packSize?.let { packSize: PackSize ->
                priceCache.store(
                    store = store,
                    query = query,
                    productName = result.productName,
                    packSize = packSize,
                    price = result.price,
                    fetchedAt = now,
                )
            }
        }
    }

    private suspend fun fallbackFor(store: String, query: String): List<PriceResult> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            priceCache.byStore(store)
                .filter { cache ->
                    q.isEmpty() ||
                        cache.query.lowercase().contains(q) ||
                        cache.productName.lowercase().contains(q)
                }
                .map { it.toResult() }
        }

    private fun requireSource(store: String): PriceSource =
        sourceByStore[store] ?: throw IllegalArgumentException("Unknown store: $store")

    private fun setHealth(store: String, status: StoreStatus) {
        _health.value = _health.value + (store to status)
    }

    private fun PriceCache.toResult() = PriceResult(
        store = store,
        productName = productName,
        price = price,
        packSize = packSize,
        url = null,
        fetchedAt = fetchedAt,
    )

    companion object {
        const val DEFAULT_CACHE_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
