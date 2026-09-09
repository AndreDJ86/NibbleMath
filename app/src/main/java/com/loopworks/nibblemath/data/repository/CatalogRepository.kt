package com.loopworks.nibblemath.data.repository

import android.content.Context
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.db.dao.IngredientDao
import com.loopworks.nibblemath.data.db.entity.IngredientEntity
import com.loopworks.nibblemath.data.model.Ingredient
import com.loopworks.nibblemath.data.model.toModel
import com.loopworks.nibblemath.data.seed.SeedCatalog

class CatalogRepository(
    private val ingredientDao: IngredientDao,
) {
    fun all(): List<Ingredient> = ingredientDao.all().map { it.toModel() }

    suspend fun byId(id: Long): Ingredient? = ingredientDao.byId(id)?.toModel()

    /** Resolves an ingredient by exact name first, then case-insensitive name or alias. */
    suspend fun find(nameOrAlias: String): Ingredient? {
        val query = nameOrAlias.trim()
        if (query.isEmpty()) return null
        ingredientDao.byName(query)?.let { return it.toModel() }
        val lower = query.lowercase()
        return ingredientDao.all().firstOrNull { ingredient ->
            ingredient.name.lowercase() == lower ||
                ingredient.aliases.any { alias -> alias.lowercase() == lower }
        }?.toModel()
    }

    suspend fun upsert(name: String, aliases: List<String>, defaultUnit: Unit): Long =
        ingredientDao.upsert(
            IngredientEntity(
                name = name,
                aliases = aliases,
                defaultUnit = defaultUnit.symbol,
                isSeed = false,
            ),
        )

    suspend fun delete(id: Long) = ingredientDao.deleteById(id)

    /** Seeds the catalog from bundled assets on first launch (no-op if populated). */
    suspend fun seedIfEmpty(context: Context) {
        if (ingredientDao.count() > 0) return
        ingredientDao.insertAll(SeedCatalog.load(context))
    }
}
