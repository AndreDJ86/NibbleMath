package com.andre.nibblemath.core.units

/** Physical dimension a unit measures. Units only convert within a dimension. */
enum class Dimension { MASS, VOLUME, COUNT }

/**
 * Measurement units supported by NibbleMath. Metric is the default; imperial
 * units are provided for conversion. [toBase] is the factor to the dimension's
 * base unit (g for mass, ml for volume, 1 for count).
 *
 * Volume conventions: Australian cup = 250 ml, metric tsp = 5 ml,
 * metric tbsp = 15 ml, oz = US fluid ounce (29.57 ml).
 */
enum class Unit(val symbol: String, val dimension: Dimension, val toBase: Double) {
    G("g", Dimension.MASS, 1.0),
    KG("kg", Dimension.MASS, 1000.0),
    ML("ml", Dimension.VOLUME, 1.0),
    L("L", Dimension.VOLUME, 1000.0),
    TSP("tsp", Dimension.VOLUME, 5.0),
    TBSP("tbsp", Dimension.VOLUME, 15.0),
    CUP("cup", Dimension.VOLUME, 250.0),
    OZ("oz", Dimension.VOLUME, 29.57),
    EACH("each", Dimension.COUNT, 1.0),
    ;

    companion object {
        fun fromSymbol(symbol: String): Unit? =
            entries.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
    }
}
