package com.loopworks.nibblemath.data.repository

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.db.dao.IngredientDao
import com.loopworks.nibblemath.data.db.dao.PantryEntryDao
import com.loopworks.nibblemath.data.db.dao.ProductDao
import com.loopworks.nibblemath.data.db.entity.PantryEntryEntity
import com.loopworks.nibblemath.data.model.PantryEntry

class PantryRepository(
    private val pantryDao: PantryEntryDao,
    private val ingredientDao: IngredientDao,
    private val productDao: ProductDao,
) {
    suspend fun entries(): List<PantryEntry> {
        val result = mutableListOf<PantryEntry>()
        for (entry in pantryDao.all()) result += entry.toModel()
        return result
    }

    suspend fun forIngredient(ingredientId: Long): List<PantryEntry> {
        val result = mutableListOf<PantryEntry>()
        for (entry in pantryDao.byIngredient(ingredientId)) result += entry.toModel()
        return result
    }

    suspend fun add(ingredientId: Long, productId: Long) {
        pantryDao.upsert(PantryEntryEntity(ingredientId = ingredientId, productId = productId))
    }

    suspend fun updateProduct(id: Long, productId: Long) {
        val existing = pantryDao.byId(id) ?: return
        pantryDao.upsert(existing.copy(productId = productId))
    }

    suspend fun remove(id: Long) = pantryDao.deleteById(id)

    private suspend fun PantryEntryEntity.toModel(): PantryEntry {
        val ingredient = ingredientDao.byId(ingredientId)
        val product = productDao.byId(productId)
        return PantryEntry(
            id = id,
            ingredientId = ingredientId,
            ingredientName = ingredient?.name ?: "Unknown",
            productId = productId,
            productName = product?.name ?: "Unknown",
            sortOrder = sortOrder,
            productPackSize = product?.let { PackSize(it.packAmount, Unit.fromSymbol(it.packUnit) ?: Unit.G) },
            productPrice = product?.price,
        )
    }
}
