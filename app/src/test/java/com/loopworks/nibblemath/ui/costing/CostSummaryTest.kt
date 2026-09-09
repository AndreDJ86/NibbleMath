package com.loopworks.nibblemath.ui.costing

import com.loopworks.nibblemath.core.costing.IngredientCost
import com.loopworks.nibblemath.core.costing.RecipeCost
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CostSummaryTest {
    @Test
    fun formatsUsdPricesWithRequestedRounding() {
        assertEquals(
            "$2.50",
            CostSummary.formatPrice(2.5, "USD", 2, Locale.US),
        )
        assertEquals(
            "$3",
            CostSummary.formatPrice(2.51, "USD", 0, Locale.US),
        )
    }

    @Test
    fun fallsBackForUnknownCurrency() {
        assertEquals(
            "1 XYZ",
            CostSummary.formatPrice(1.25, "XYZ", 0, Locale.US),
        )
    }

    @Test
    fun displaysPricedRecipe() {
        val cost = RecipeCost(
            batchCost = 2.5,
            perItem = 1.25,
            yield = 2.0,
            items = listOf(
                IngredientCost(
                    ingredient = com.loopworks.nibblemath.core.costing.Ingredient(
                        name = "Flour",
                        amount = 500.0,
                        unit = Unit.G,
                        packSize = PackSize(1.0, Unit.KG),
                        packPrice = 5.0,
                    ),
                    cost = 2.5,
                    missing = false,
                    reason = null,
                ),
            ),
            missingCount = 0,
        )

        val display = CostSummary.display(cost, "USD", 2, Locale.US)

        assertTrue(display.hasIngredients)
        assertTrue(display.hasPriced)
        assertEquals("$2.50", display.batchPrice)
        assertEquals("$1.25", display.perItemPrice)
        assertEquals(0, display.missingCount)
    }

    @Test
    fun displaysUnpricedRecipe() {
        val cost = RecipeCost(
            batchCost = 0.0,
            perItem = null,
            yield = 1.0,
            items = listOf(
                IngredientCost(
                    ingredient = com.loopworks.nibblemath.core.costing.Ingredient(
                        name = "Flour",
                        amount = 500.0,
                        unit = Unit.G,
                        packSize = null,
                        packPrice = null,
                    ),
                    cost = null,
                    missing = true,
                    reason = "no pack size",
                ),
            ),
            missingCount = 1,
        )

        val display = CostSummary.display(cost, "USD", 2, Locale.US)

        assertTrue(display.hasIngredients)
        assertFalse(display.hasPriced)
        assertNull(display.batchPrice)
        assertNull(display.perItemPrice)
        assertEquals(1, display.missingCount)
    }

    @Test
    fun unitPriceLabelForMass() {
        assertEquals(
            "$0.50/100g",
            CostSummary.unitPriceLabel(2.50, PackSize(500.0, Unit.G), "USD", 2, Locale.US),
        )
    }

    @Test
    fun unitPriceLabelForVolume() {
        assertEquals(
            "$2.50/L",
            CostSummary.unitPriceLabel(2.50, PackSize(1.0, Unit.L), "USD", 2, Locale.US),
        )
    }

    @Test
    fun unitPriceLabelForCount() {
        assertEquals(
            "$0.25/each",
            CostSummary.unitPriceLabel(3.00, PackSize(12.0, Unit.EACH), "USD", 2, Locale.US),
        )
    }

    @Test
    fun unitPriceLabelIsNullWithoutPackSize() {
        assertNull(CostSummary.unitPriceLabel(1.00, null, "USD", 2, Locale.US))
    }

    @Test
    fun mapsRecipeToCost() {
        val recipe = Recipe(
            id = 1L,
            bookId = 1L,
            name = "Cake",
            yieldAmount = 2.0,
            yieldItem = "slices",
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
                    amount = 500.0,
                    unit = Unit.G,
                    sortOrder = 0,
                    productPackSize = PackSize(1.0, Unit.KG),
                    productPrice = 5.0,
                ),
            ),
        )

        val cost = CostSummary.cost(recipe)

        assertEquals(2.5, cost.batchCost)
        assertEquals(1.25, cost.perItem)
        assertEquals(0, cost.missingCount)
    }
}
