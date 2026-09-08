package com.loopworks.nibblemath.core.scaling

import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScalingEngineTest {
    @Test
    fun `scale by factor`() {
        assertEquals(100.0, ScalingEngine.scale(50.0, 2.0))
        assertEquals(25.0, ScalingEngine.scale(50.0, 0.5))
    }

    @Test
    fun `factor for target yield`() {
        assertEquals(0.5, ScalingEngine.factorFor(24.0, 12.0))
        assertEquals(2.0, ScalingEngine.factorFor(12.0, 24.0))
    }

    @Test
    fun `factor requires positive yields`() {
        assertFailsWith<IllegalArgumentException> { ScalingEngine.factorFor(0.0, 12.0) }
        assertFailsWith<IllegalArgumentException> { ScalingEngine.factorFor(12.0, 0.0) }
    }

    @Test
    fun `round eggs to whole`() {
        assertEquals(2.0, ScalingEngine.roundPractically(1.6, Unit.EACH))
        assertEquals(3.0, ScalingEngine.roundPractically(2.5, Unit.EACH))
    }

    @Test
    fun `round tsp to half`() {
        assertEquals(1.5, ScalingEngine.roundPractically(1.4, Unit.TSP))
        assertEquals(1.0, ScalingEngine.roundPractically(1.2, Unit.TSP))
    }

    @Test
    fun `round small grams to 5g`() {
        assertEquals(15.0, ScalingEngine.roundPractically(14.0, Unit.G))
        assertEquals(20.0, ScalingEngine.roundPractically(18.0, Unit.G))
    }

    @Test
    fun `round large grams to 10g`() {
        assertEquals(120.0, ScalingEngine.roundPractically(118.0, Unit.G))
        assertEquals(120.0, ScalingEngine.roundPractically(124.0, Unit.G))
    }

    @Test
    fun `round cup to quarter`() {
        assertEquals(1.25, ScalingEngine.roundPractically(1.2, Unit.CUP))
        assertEquals(1.5, ScalingEngine.roundPractically(1.4, Unit.CUP))
    }

    @Test
    fun `negative rounds to zero`() {
        assertEquals(0.0, ScalingEngine.roundPractically(-5.0, Unit.G))
    }
}
