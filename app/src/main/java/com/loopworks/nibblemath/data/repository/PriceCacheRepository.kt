package com.loopworks.nibblemath.data.repository

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.data.db.dao.PriceCacheDao
import com.loopworks.nibblemath.data.db.entity.PriceCacheEntity
import com.loopworks.nibblemath.data.model.PriceCache
import com.loopworks.nibblemath.data.model.toModel

class PriceCacheRepository(
    private val priceCacheDao: PriceCacheDao,
) {
    suspend fun lookup(store: String, productName: String): PriceCache? =
        priceCacheDao.byStoreAndProduct(store, productName)?.toModel()

    fun byStore(store: String): List<PriceCache> =
        priceCacheDao.byStore(store).map { it.toModel() }

    suspend fun store(
        store: String,
        query: String,
        productName: String,
        packSize: PackSize,
        price: Double,
        fetchedAt: Long,
    ): Long =
        priceCacheDao.upsert(
            PriceCacheEntity(
                store = store,
                query = query,
                productName = productName,
                packAmount = packSize.amount,
                packUnit = packSize.unit.symbol,
                price = price,
                fetchedAt = fetchedAt,
            ),
        )

    suspend fun clear(store: String) = priceCacheDao.deleteByStore(store)
}
