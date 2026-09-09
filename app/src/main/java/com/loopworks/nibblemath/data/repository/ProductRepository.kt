package com.loopworks.nibblemath.data.repository

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.data.db.dao.ProductDao
import com.loopworks.nibblemath.data.db.entity.ProductEntity
import com.loopworks.nibblemath.data.model.Product
import com.loopworks.nibblemath.data.model.toModel

class ProductRepository(
    private val productDao: ProductDao,
) {
    fun all(): List<Product> = productDao.all().map { it.toModel() }

    suspend fun byId(id: Long): Product? = productDao.byId(id)?.toModel()

    fun search(query: String): List<Product> =
        productDao.search("%${query.trim()}%").map { it.toModel() }

    suspend fun upsert(
        name: String,
        packSize: PackSize,
        price: Double,
        source: String = "manual",
        fetchedAt: Long? = null,
    ): Long {
        val trimmedName = name.trim()
        val existing = productDao.byName(trimmedName)
        return productDao.upsert(
            ProductEntity(
                id = existing?.id ?: 0,
                name = trimmedName,
                packAmount = packSize.amount,
                packUnit = packSize.unit.symbol,
                price = price,
                source = source,
                fetchedAt = fetchedAt,
            ),
        )
    }

    suspend fun delete(id: Long) = productDao.deleteById(id)
}
