package com.andre.nibblemath.core.costing

import com.andre.nibblemath.core.units.PackSize
import com.andre.nibblemath.core.units.Unit
import com.andre.nibblemath.core.units.UnitConversion

/** One recipe ingredient with its amount and (optional) product pricing. */
data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: Unit,
    val packSize: PackSize?,
    val packPrice: Double?,
)

/** Result of costing a single ingredient. */
data class IngredientCost(
    val ingredient: Ingredient,
    val cost: Double?,
    val missing: Boolean,
    val reason: String?,
)

/** Result of costing a whole recipe batch. */
data class RecipeCost(
    val batchCost: Double,
    val perItem: Double?,
    val yield: Double,
    val items: List<IngredientCost>,
    val missingCount: Int,
)

/**
 * True-cost engine. Ingredient cost = (amount ÷ pack size) × pack price, with
 * amount and pack size normalised to a common base unit. Missing prices or pack
 * sizes are excluded from the total but flagged, never silently dropped.
 */
object CostingEngine {
    fun ingredientCost(ing: Ingredient): IngredientCost {
        val pack = ing.packSize
        val price = ing.packPrice
        if (pack == null) return IngredientCost(ing, null, true, "no pack size")
        if (price == null) return IngredientCost(ing, null, true, "no price")
        if (!UnitConversion.isConvertible(ing.unit, pack.unit)) {
            return IngredientCost(
                ing, null, true,
                "unit mismatch: ${ing.unit.symbol} vs ${pack.unit.symbol}",
            )
        }
        val packInBase = pack.amountInBase
        if (packInBase <= 0.0) return IngredientCost(ing, null, true, "pack size is zero")
        val amountInBase = ing.amount * ing.unit.toBase
        val cost = amountInBase / packInBase * price
        return IngredientCost(ing, cost, false, null)
    }

    /** Total batch cost = Σ ingredient costs; per-item = batch ÷ yield. */
    fun recipeCost(ingredients: List<Ingredient>, yield: Double): RecipeCost {
        val items = ingredients.map { ingredientCost(it) }
        val batchCost = items.sumOf { it.cost ?: 0.0 }
        val perItem = if (yield > 0.0) batchCost / yield else null
        val missingCount = items.count { it.missing }
        return RecipeCost(batchCost, perItem, yield, items, missingCount)
    }
}
