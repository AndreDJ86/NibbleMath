package com.loopworks.nibblemath.data.repository

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.db.dao.IngredientDao
import com.loopworks.nibblemath.data.db.dao.ProductDao
import com.loopworks.nibblemath.data.db.dao.RecipeDao
import com.loopworks.nibblemath.data.db.dao.RecipeIngredientDao
import com.loopworks.nibblemath.data.db.dao.StepDao
import com.loopworks.nibblemath.data.db.entity.RecipeEntity
import com.loopworks.nibblemath.data.db.entity.RecipeIngredientEntity
import com.loopworks.nibblemath.data.db.entity.StepEntity
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import com.loopworks.nibblemath.data.model.RecipeSummary
import com.loopworks.nibblemath.data.model.toModel
import com.loopworks.nibblemath.data.model.toSummary

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val stepDao: StepDao,
    private val lineDao: RecipeIngredientDao,
    private val ingredientDao: IngredientDao,
    private val productDao: ProductDao,
) {
    fun summaries(bookId: Long): List<RecipeSummary> = recipeDao.byBook(bookId).map { it.toSummary() }

    suspend fun get(id: Long): Recipe? {
        val recipe = recipeDao.byId(id) ?: return null
        val steps = stepDao.byRecipe(id).map { it.text }
        val lines = mutableListOf<IngredientLine>()
        for (line in lineDao.byRecipe(id)) {
            val ingredient = ingredientDao.byId(line.ingredientId)
            val product = line.productId?.let { productDao.byId(it) }
            lines += IngredientLine(
                id = line.id,
                recipeId = line.recipeId,
                ingredientId = line.ingredientId,
                ingredientName = ingredient?.name ?: "Unknown",
                productId = line.productId,
                productName = product?.name,
                amount = line.amount,
                unit = Unit.fromSymbol(line.unit) ?: Unit.G,
                sortOrder = line.sortOrder,
                productPackSize = product?.let { PackSize(it.packAmount, Unit.fromSymbol(it.packUnit) ?: Unit.G) },
                productPrice = product?.price,
            )
        }
        return recipe.toModel(steps, lines)
    }

    suspend fun create(bookId: Long, name: String, yieldAmount: Double, yieldItem: String): Long =
        recipeDao.upsert(
            RecipeEntity(
                bookId = bookId,
                name = name,
                yieldAmount = yieldAmount,
                yieldItem = yieldItem,
                sortOrder = recipeDao.byBook(bookId).size,
            ),
        )

    /** Persists the recipe header, its steps, and its ingredient lines together. */
    suspend fun update(recipe: Recipe) {
        recipeDao.upsert(
            RecipeEntity(
                id = recipe.id,
                bookId = recipe.bookId,
                name = recipe.name,
                yieldAmount = recipe.yieldAmount,
                yieldItem = recipe.yieldItem,
                notes = recipe.notes,
                sortOrder = recipe.sortOrder,
            ),
        )
        stepDao.deleteByRecipe(recipe.id)
        stepDao.insertAll(
            recipe.steps.mapIndexed { index, text ->
                StepEntity(recipeId = recipe.id, text = text, sortOrder = index)
            },
        )
        lineDao.deleteByRecipe(recipe.id)
        lineDao.insertAll(
            recipe.ingredients.mapIndexed { index, line ->
                RecipeIngredientEntity(
                    recipeId = recipe.id,
                    ingredientId = line.ingredientId,
                    productId = line.productId,
                    amount = line.amount,
                    unit = line.unit.symbol,
                    sortOrder = index,
                )
            },
        )
    }

    suspend fun delete(id: Long) = recipeDao.deleteById(id)
}
