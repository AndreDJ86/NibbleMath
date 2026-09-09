package com.loopworks.nibblemath.data.model

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.db.entity.BookEntity
import com.loopworks.nibblemath.data.db.entity.IngredientEntity
import com.loopworks.nibblemath.data.db.entity.PriceCacheEntity
import com.loopworks.nibblemath.data.db.entity.ProductEntity
import com.loopworks.nibblemath.data.db.entity.RecipeEntity

fun BookEntity.toModel(): Book = Book(id, name, sortOrder)

fun RecipeEntity.toSummary(): RecipeSummary =
    RecipeSummary(id, bookId, name, yieldAmount, yieldItem, sortOrder)

fun RecipeEntity.toModel(steps: List<String>, ingredients: List<IngredientLine>): Recipe =
    Recipe(id, bookId, name, yieldAmount, yieldItem, notes, sortOrder, steps, ingredients)

fun IngredientEntity.toModel(): Ingredient =
    Ingredient(id, name, aliases, Unit.fromSymbol(defaultUnit) ?: Unit.G, isSeed)

fun ProductEntity.toModel(): Product =
    Product(id, name, PackSize(packAmount, Unit.fromSymbol(packUnit) ?: Unit.G), price, source, fetchedAt)

fun PriceCacheEntity.toModel(): PriceCache =
    PriceCache(id, store, query, productName, PackSize(packAmount, Unit.fromSymbol(packUnit) ?: Unit.G), price, fetchedAt)
