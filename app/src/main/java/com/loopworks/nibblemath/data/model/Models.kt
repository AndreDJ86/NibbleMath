package com.loopworks.nibblemath.data.model

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit

data class Book(val id: Long, val name: String, val sortOrder: Int)

data class RecipeSummary(
    val id: Long,
    val bookId: Long,
    val name: String,
    val yieldAmount: Double,
    val yieldItem: String,
    val sortOrder: Int,
)

data class Recipe(
    val id: Long,
    val bookId: Long,
    val name: String,
    val yieldAmount: Double,
    val yieldItem: String,
    val notes: String,
    val sortOrder: Int,
    val steps: List<String>,
    val ingredients: List<IngredientLine>,
)

data class IngredientLine(
    val id: Long,
    val recipeId: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val productId: Long?,
    val productName: String?,
    val amount: Double,
    val unit: Unit,
    val sortOrder: Int,
    val productPackSize: PackSize? = null,
    val productPrice: Double? = null,
)

data class Ingredient(
    val id: Long,
    val name: String,
    val aliases: List<String>,
    val defaultUnit: Unit,
    val isSeed: Boolean,
)

data class Product(
    val id: Long,
    val name: String,
    val packSize: PackSize,
    val price: Double,
    val source: String,
    val fetchedAt: Long?,
)

data class PantryEntry(
    val id: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val productId: Long,
    val productName: String,
    val sortOrder: Int,
    val productPackSize: PackSize? = null,
    val productPrice: Double? = null,
)

data class PriceCache(
    val id: Long,
    val store: String,
    val query: String,
    val productName: String,
    val packSize: PackSize,
    val price: Double,
    val fetchedAt: Long,
)
