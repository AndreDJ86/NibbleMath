package com.loopworks.nibblemath.core.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UnitConversionTest {
    @Test
    fun `kg to g`() {
        assertEquals(1000.0, UnitConversion.convert(1.0, Unit.KG, Unit.G))
    }

    @Test
    fun `L to ml`() {
        assertEquals(1000.0, UnitConversion.convert(1.0, Unit.L, Unit.ML))
    }

    @Test
    fun `cup to ml australian`() {
        assertEquals(250.0, UnitConversion.convert(1.0, Unit.CUP, Unit.ML))
    }

    @Test
    fun `tbsp to ml`() {
        assertEquals(15.0, UnitConversion.convert(1.0, Unit.TBSP, Unit.ML))
    }

    @Test
    fun `tsp to ml`() {
        assertEquals(5.0, UnitConversion.convert(1.0, Unit.TSP, Unit.ML))
    }

    @Test
    fun `g to kg`() {
        assertEquals(0.5, UnitConversion.convert(500.0, Unit.G, Unit.KG))
    }

    @Test
    fun `dimension mismatch throws`() {
        assertFailsWith<IllegalArgumentException> {
            UnitConversion.convert(1.0, Unit.G, Unit.ML)
        }
    }

    @Test
    fun `isConvertible`() {
        assertTrue(UnitConversion.isConvertible(Unit.G, Unit.KG))
        assertTrue(!UnitConversion.isConvertible(Unit.G, Unit.ML))
    }

    @Test
    fun `fromSymbol case insensitive`() {
        assertEquals(Unit.L, Unit.fromSymbol("l"))
        assertEquals(Unit.G, Unit.fromSymbol("G"))
        assertEquals(null, Unit.fromSymbol("xyz"))
    }
}
