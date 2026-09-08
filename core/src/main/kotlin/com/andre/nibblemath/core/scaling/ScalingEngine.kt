package com.andre.nibblemath.core.scaling

import com.andre.nibblemath.core.units.Unit
import kotlin.math.roundToLong

/** Scales recipe amounts and applies practical, human-friendly rounding. */
object ScalingEngine {
    /** Scale [amount] by [factor]. */
    fun scale(amount: Double, factor: Double): Double = amount * factor

    /** Factor that scales from [currentYield] servings to [targetYield] servings. */
    fun factorFor(currentYield: Double, targetYield: Double): Double {
        require(currentYield > 0.0) { "currentYield must be > 0" }
        require(targetYield > 0.0) { "targetYield must be > 0" }
        return targetYield / currentYield
    }

    /**
     * Round to a value a person can actually measure:
     * - each → whole numbers
     * - tsp / tbsp / oz → ½ increments
     * - cup → ¼ increments
     * - g → 5 g under 50 g, else 10 g
     * - kg → 50 g
     * - ml → 5 ml under 100 ml, else 10 ml
     * - L → 50 ml
     */
    fun roundPractically(amount: Double, unit: Unit): Double {
        if (amount < 0.0) return 0.0
        val step = when (unit) {
            Unit.EACH -> 1.0
            Unit.TSP, Unit.TBSP, Unit.OZ -> 0.5
            Unit.CUP -> 0.25
            Unit.G -> if (amount < 50.0) 5.0 else 10.0
            Unit.KG -> 0.05
            Unit.ML -> if (amount < 100.0) 5.0 else 10.0
            Unit.L -> 0.05
        }
        return (amount / step).roundToLong() * step
    }
}
