package com.loopworks.nibblemath.ui.ocr

import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrTextParserTest {
    @Test
    fun parsesHeadedRecipe() {
        val parsed = OcrTextParser.parse(
            """
            Pancakes
            Serves 4
            Ingredients
            200g flour
            300 ml milk
            2 eggs
            1 tbsp oil
            Method
            Mix everything.
            """.trimIndent(),
        )

        assertEquals("Pancakes", parsed.name)
        assertEquals("4", parsed.yieldText)
        assertEquals("servings", parsed.yieldItem)
        assertEquals(4, parsed.ingredients.size)
        assertEquals("flour", parsed.ingredients[0].name)
        assertEquals("200", parsed.ingredients[0].amountText)
        assertEquals(Unit.G, parsed.ingredients[0].unit)
        assertEquals("milk", parsed.ingredients[1].name)
        assertEquals("300", parsed.ingredients[1].amountText)
        assertEquals(Unit.ML, parsed.ingredients[1].unit)
        assertEquals("eggs", parsed.ingredients[2].name)
        assertEquals("2", parsed.ingredients[2].amountText)
        assertEquals(Unit.EACH, parsed.ingredients[2].unit)
        assertEquals("oil", parsed.ingredients[3].name)
        assertEquals("1", parsed.ingredients[3].amountText)
        assertEquals(Unit.TBSP, parsed.ingredients[3].unit)
    }

    @Test
    fun parsesQuantityOnlyLines() {
        val parsed = OcrTextParser.parse(
            """
            200g whole milk
            120g butter
            2 eggs
            """.trimIndent(),
        )

        assertEquals("", parsed.name)
        assertEquals("1", parsed.yieldText)
        assertEquals(3, parsed.ingredients.size)
        assertEquals("whole milk", parsed.ingredients[0].name)
        assertEquals(Unit.G, parsed.ingredients[0].unit)
        assertEquals("butter", parsed.ingredients[1].name)
        assertEquals(Unit.G, parsed.ingredients[1].unit)
        assertEquals("eggs", parsed.ingredients[2].name)
        assertEquals(Unit.EACH, parsed.ingredients[2].unit)
    }

    @Test
    fun parsesFractionsAndMixedNumbers() {
        val parsed = OcrTextParser.parse(
            """
            1/2 cup cocoa
            1 1/2 tsp vanilla
            0.5 l water
            """.trimIndent(),
        )

        assertEquals("cocoa", parsed.ingredients[0].name)
        assertEquals("0.5", parsed.ingredients[0].amountText)
        assertEquals(Unit.CUP, parsed.ingredients[0].unit)
        assertEquals("vanilla", parsed.ingredients[1].name)
        assertEquals("1.5", parsed.ingredients[1].amountText)
        assertEquals(Unit.TSP, parsed.ingredients[1].unit)
        assertEquals("water", parsed.ingredients[2].name)
        assertEquals("0.5", parsed.ingredients[2].amountText)
        assertEquals(Unit.L, parsed.ingredients[2].unit)
    }

    @Test
    fun parsesCommaSeparatedIngredients() {
        val parsed = OcrTextParser.parse("200g flour, 300ml milk, 2 eggs")

        assertEquals(3, parsed.ingredients.size)
        assertEquals("flour", parsed.ingredients[0].name)
        assertEquals("milk", parsed.ingredients[1].name)
        assertEquals("eggs", parsed.ingredients[2].name)
    }

    @Test
    fun keepsUnquantifiedLinesInsideIngredientsSection() {
        val parsed = OcrTextParser.parse(
            """
            Ingredients
            200g flour
            salt to taste
            Method
            Mix.
            """.trimIndent(),
        )

        assertEquals(2, parsed.ingredients.size)
        assertEquals("flour", parsed.ingredients[0].name)
        assertEquals("salt to taste", parsed.ingredients[1].name)
        assertEquals("", parsed.ingredients[1].amountText)
        assertEquals(Unit.EACH, parsed.ingredients[1].unit)
    }

    @Test
    fun parsesYieldVariants() {
        val makes = OcrTextParser.parse(
            """
            Macarons
            Makes 12 macarons
            Ingredients
            100g almond flour
            """.trimIndent(),
        )
        val servings = OcrTextParser.parse(
            """
            Soup
            4 servings
            Ingredients
            1 l stock
            """.trimIndent(),
        )

        assertEquals("12", makes.yieldText)
        assertEquals("macarons", makes.yieldItem)
        assertEquals("4", servings.yieldText)
        assertEquals("servings", servings.yieldItem)
    }

    @Test
    fun ignoresMethodLinesAsIngredients() {
        val parsed = OcrTextParser.parse(
            """
            Bread
            Ingredients
            500g bread flour
            Method
            1 tbsp yeast
            Mix and prove.
            """.trimIndent(),
        )

        assertEquals(1, parsed.ingredients.size)
        assertEquals("bread flour", parsed.ingredients[0].name)
        assertTrue(parsed.ingredients.none { it.name == "yeast" })
    }
}
