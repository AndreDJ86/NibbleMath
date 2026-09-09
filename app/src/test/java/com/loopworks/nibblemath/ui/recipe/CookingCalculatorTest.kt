package com.loopworks.nibblemath.ui.recipe

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import com.loopworks.nibblemath.ui.costing.CostSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class CookingCalculatorTest {
    @Test
    fun factorHalvesWhenTargetIsHalfBase() {
        assertEquals(0.5, CookingCalculator.factor(24.0, 12.0))
    }

    @Test
    fun scaledRecipeHalvesAmountsAndYield() {
        val recipe = recipe()

        val scaled = CookingCalculator.scaledRecipe(recipe, 12.0)

        assertEquals(12.0, scaled.yieldAmount)
        assertEquals(120.0, scaled.ingredients[0].amount)
    }

    @Test
    fun scaledRecipeHalvesCost() {
        val recipe = recipe()

        val full = CostSummary.cost(recipe)
        val half = CostSummary.cost(CookingCalculator.scaledRecipe(recipe, 12.0))

        assertEquals(full.batchCost / 2.0, half.batchCost)
        assertEquals(full.perItem, half.perItem)
    }

    @Test
    fun displayAmountRoundsPractically() {
        assertEquals("110 g", CookingCalculator.displayAmount(112.5, Unit.G))
        assertEquals("0.5 cup", CookingCalculator.displayAmount(0.5, Unit.CUP))
        assertEquals("1 each", CookingCalculator.displayAmount(1.2, Unit.EACH))
    }

    @Test
    fun formatAmountOmitsTrailingZero() {
        assertEquals("25", CookingCalculator.formatAmount(25.0))
        assertEquals("0.5", CookingCalculator.formatAmount(0.5))
    }

    private fun recipe(): Recipe = Recipe(
        id = 1L,
        bookId = 1L,
        name = "Cookies",
        yieldAmount = 24.0,
        yieldItem = "cookies",
        notes = "",
        sortOrder = 0,
        steps = emptyList(),
        ingredients = listOf(
            IngredientLine(
                id = 1L,
                recipeId = 1L,
                ingredientId = 1L,
                ingredientName = "Flour",
                productId = 1L,
                productName = "Flour 1kg",
                amount = 240.0,
                unit = Unit.G,
                sortOrder = 0,
                productPackSize = PackSize(1.0, Unit.KG),
                productPrice = 5.0,
            ),
        ),
    )
}
