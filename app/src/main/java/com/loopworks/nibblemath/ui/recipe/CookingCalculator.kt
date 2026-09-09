package com.loopworks.nibblemath.ui.recipe

import com.loopworks.nibblemath.core.scaling.ScalingEngine
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe

object CookingCalculator {
    fun factor(baseYield: Double, targetYield: Double): Double =
        if (baseYield > 0.0 && targetYield > 0.0) targetYield / baseYield else 1.0

    fun scaledRecipe(recipe: Recipe, targetYield: Double): Recipe {
        val factor = factor(recipe.yieldAmount, targetYield)
        return recipe.copy(
            yieldAmount = if (targetYield > 0.0) targetYield else recipe.yieldAmount,
            ingredients = recipe.ingredients.map { it.copy(amount = ScalingEngine.scale(it.amount, factor)) },
        )
    }

    fun scaledAmount(line: IngredientLine, factor: Double): Double =
        ScalingEngine.scale(line.amount, factor)

    fun displayAmount(amount: Double, unit: Unit): String =
        "${formatAmount(ScalingEngine.roundPractically(amount, unit))} ${unit.symbol}"

    fun formatAmount(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
