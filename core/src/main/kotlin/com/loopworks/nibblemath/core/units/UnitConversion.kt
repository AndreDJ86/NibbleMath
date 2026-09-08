package com.loopworks.nibblemath.core.units

/** Converts an amount between units of the same [Dimension]. */
object UnitConversion {
    /** Returns [amount] of [from] expressed in [to]. Throws on dimension mismatch. */
    fun convert(amount: Double, from: Unit, to: Unit): Double {
        require(from.dimension == to.dimension) {
            "Cannot convert ${from.symbol} (${from.dimension}) to ${to.symbol} (${to.dimension})"
        }
        return amount * from.toBase / to.toBase
    }

    /** True when [from] and [to] share a dimension and are convertible. */
    fun isConvertible(from: Unit, to: Unit): Boolean = from.dimension == to.dimension
}
