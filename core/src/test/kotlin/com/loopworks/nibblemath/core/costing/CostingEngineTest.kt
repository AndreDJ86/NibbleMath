package com.loopworks.nibblemath.core.costing

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CostingEngineTest {
    @Test
    fun `ingredient cost proration`() {
        // 200 g of flour, pack 1 kg ($3.00) -> 200/1000 * 3.00 = 0.60
        val ing = Ingredient("flour", 200.0, Unit.G, PackSize(1.0, Unit.KG), 3.00)
        val cost = CostingEngine.ingredientCost(ing)
        assertEquals(0.60, cost.cost!!, 1e-9)
        assertTrue(!cost.missing)
    }

    @Test
    fun `ingredient cost across units`() {
        // 500 ml milk, pack 1 L ($2.00) -> 500/1000 * 2.00 = 1.00
        val ing = Ingredient("milk", 500.0, Unit.ML, PackSize(1.0, Unit.L), 2.00)
        val cost = CostingEngine.ingredientCost(ing)
        assertEquals(1.00, cost.cost!!, 1e-9)
    }

    @Test
    fun `missing price flagged`() {
        val ing = Ingredient("sugar", 100.0, Unit.G, PackSize(1.0, Unit.KG), null)
        val cost = CostingEngine.ingredientCost(ing)
        assertNull(cost.cost)
        assertTrue(cost.missing)
    }

    @Test
    fun `missing pack flagged`() {
        val ing = Ingredient("sugar", 100.0, Unit.G, null, 1.00)
        val cost = CostingEngine.ingredientCost(ing)
        assertNull(cost.cost)
        assertTrue(cost.missing)
    }

    @Test
    fun `unit mismatch flagged`() {
        val ing = Ingredient("x", 100.0, Unit.G, PackSize(1.0, Unit.L), 1.00)
        val cost = CostingEngine.ingredientCost(ing)
        assertNull(cost.cost)
        assertTrue(cost.missing)
    }

    @Test
    fun `recipe batch and per item`() {
        val ings = listOf(
            Ingredient("flour", 200.0, Unit.G, PackSize(1.0, Unit.KG), 3.00),   // 0.60
            Ingredient("milk", 500.0, Unit.ML, PackSize(1.0, Unit.L), 2.00),     // 1.00
            Ingredient("egg", 2.0, Unit.EACH, PackSize(12.0, Unit.EACH), 3.60),  // 0.60
        )
        val recipe = CostingEngine.recipeCost(ings, yield = 24.0)
        assertEquals(2.20, recipe.batchCost, 1e-9)
        assertEquals(2.20 / 24.0, recipe.perItem!!, 1e-9)
        assertEquals(0, recipe.missingCount)
    }

    @Test
    fun `zero yield gives null per item`() {
        val ings = listOf(Ingredient("flour", 200.0, Unit.G, PackSize(1.0, Unit.KG), 3.00))
        val recipe = CostingEngine.recipeCost(ings, yield = 0.0)
        assertNull(recipe.perItem)
        assertEquals(0.60, recipe.batchCost, 1e-9)
    }

    @Test
    fun `missing excluded from total but counted`() {
        val ings = listOf(
            Ingredient("flour", 200.0, Unit.G, PackSize(1.0, Unit.KG), 3.00),   // 0.60
            Ingredient("vanilla", 5.0, Unit.ML, PackSize(10.0, Unit.ML), null), // missing
        )
        val recipe = CostingEngine.recipeCost(ings, yield = 12.0)
        assertEquals(0.60, recipe.batchCost, 1e-9)
        assertEquals(1, recipe.missingCount)
    }
}
