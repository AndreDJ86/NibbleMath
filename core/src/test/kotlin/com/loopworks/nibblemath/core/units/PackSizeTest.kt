package com.loopworks.nibblemath.core.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackSizeTest {
    @Test
    fun `simple volume`() {
        assertEquals(PackSize(1.0, Unit.L), PackSizeParser.parse("1 L"))
    }

    @Test
    fun `simple mass`() {
        assertEquals(PackSize(500.0, Unit.G), PackSizeParser.parse("500 g"))
    }

    @Test
    fun `mass with descriptor`() {
        assertEquals(PackSize(1.0, Unit.KG), PackSizeParser.parse("1 kg bag"))
    }

    @Test
    fun `dozen`() {
        assertEquals(PackSize(144.0, Unit.EACH), PackSizeParser.parse("12 dozen"))
    }

    @Test
    fun `dozen with descriptor`() {
        assertEquals(PackSize(24.0, Unit.EACH), PackSizeParser.parse("2 dozen eggs"))
    }

    @Test
    fun `pack count`() {
        assertEquals(PackSize(6.0, Unit.EACH), PackSizeParser.parse("6 pack"))
    }

    @Test
    fun `eggs count`() {
        assertEquals(PackSize(12.0, Unit.EACH), PackSizeParser.parse("12 eggs"))
    }

    @Test
    fun `bare number is each`() {
        assertEquals(PackSize(500.0, Unit.EACH), PackSizeParser.parse("500"))
    }

    @Test
    fun `decimal amount`() {
        assertEquals(PackSize(1.5, Unit.L), PackSizeParser.parse("1.5 L"))
    }

    @Test
    fun `unknown unit is null`() {
        assertNull(PackSizeParser.parse("1 xyz"))
    }

    @Test
    fun `no number is null`() {
        assertNull(PackSizeParser.parse("abc"))
    }

    @Test
    fun `empty is null`() {
        assertNull(PackSizeParser.parse(""))
    }

    @Test
    fun `concatenated volume`() {
        assertEquals(PackSize(3.0, Unit.L), PackSizeParser.parse("3L"))
    }

    @Test
    fun `toStringUsesUnitSymbol`() {
        assertEquals("3.0 L", PackSize(3.0, Unit.L).toString())
    }
}
