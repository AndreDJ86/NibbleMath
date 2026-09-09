package com.loopworks.nibblemath.core.units

/** A purchasable pack: how much product one pack contains (e.g. 1 L, 500 g, 12 dozen). */
data class PackSize(val amount: Double, val unit: Unit) {
    /** Amount in the dimension's base unit (g / ml / count). */
    val amountInBase: Double get() = amount * unit.toBase

    override fun toString(): String = "$amount ${unit.symbol}"
}

/**
 * Parses a human pack-size string into a [PackSize].
 *
 * Handles: "1 L", "500 g", "1 kg bag", "12 dozen", "6 pack", "12 eggs".
 * Descriptor words (bag, box, tub, carton, pack, eggs, ...) are ignored.
 * "dozen" multiplies the count by 12. A bare number means "each".
 *
 * Returns null when the string is ambiguous or unparseable — the caller then
 * forces manual entry rather than guessing.
 */
object PackSizeParser {
    private val DESCRIPTORS = setOf(
        "bag", "bags", "box", "boxes", "tub", "tubs", "carton", "cartons",
        "pack", "packs", "pack of", "eggs", "each", "eaches", "bottle", "bottles",
        "jar", "jars", "can", "cans", "container", "containers", "pouch", "pouches",
        "tray", "trays", "roll", "rolls", "block", "blocks",
    )

    fun parse(input: String): PackSize? {
        val tokens = input.trim().lowercase()
            .replace(Regex("(?<=[0-9])(?=[a-z])"), " ")
            .replace(",", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null

        val numIndex = tokens.indexOfFirst { it.toDoubleOrNull() != null }
        if (numIndex < 0) return null
        val amount = tokens[numIndex].toDoubleOrNull() ?: return null

        val rest = tokens.filterIndexed { i, _ -> i != numIndex }

        if ("dozen" in rest) {
            return PackSize(amount * 12.0, Unit.EACH)
        }

        val unitToken = rest.firstOrNull { Unit.fromSymbol(it) != null }
        if (unitToken != null) {
            return PackSize(amount, Unit.fromSymbol(unitToken)!!)
        }

        val nonDescriptors = rest.filter { it !in DESCRIPTORS }
        if (nonDescriptors.isEmpty()) {
            return PackSize(amount, Unit.EACH)
        }

        return null
    }
}
