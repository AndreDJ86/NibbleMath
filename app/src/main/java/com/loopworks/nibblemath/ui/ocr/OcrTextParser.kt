package com.loopworks.nibblemath.ui.ocr

import com.loopworks.nibblemath.core.units.Unit

data class ParsedIngredient(
    val name: String,
    val amountText: String,
    val unit: Unit,
)

data class ParsedRecipe(
    val name: String,
    val yieldText: String,
    val yieldItem: String,
    val ingredients: List<ParsedIngredient>,
)

object OcrTextParser {
    private val ingredientHeading = Regex("(?i)^ingredients?\\s*:?$")
    private val sectionHeading = Regex("(?i)^(ingredients?|method|instructions|directions|steps|cooking|notes?)\\s*:?$")
    private val methodHeading = Regex("(?i)^(method|instructions|directions|steps|cooking|notes?)\\s*:?$")
    private val yieldKeyword = Regex("(?i)^(serves|serving|makes|yield)\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(.*)$")
    private val yieldCount = Regex("(?i)^(\\d+(?:\\.\\d+)?)\\s+(servings?|portions?)$")
    private val ingredient = Regex(
        """
        ^\s*
        (?<amount>\d+\s+\d+/\d+|\d+/\d+|\d+(?:\.\d+)?)
        \s*(?:x\s*)?
        (?:\s*(?<unit>millilitres?|milliliters?|kilograms?|litres?|liters?|tablespoons?|teaspoons?|grams?|ounces?|cups?|each|kg|ml|tbsp|tsp|cup|oz|g|l)(?=\s|$|x))?
        \s*
        (?<rest>.+)$
        """.trimIndent().replace("\n", ""),
        RegexOption.IGNORE_CASE,
    )
    private val mixedNumber = Regex("^(\\d+)\\s+(\\d+)/(\\d+)$")
    private val fraction = Regex("^(\\d+)/(\\d+)$")

    fun parse(text: String): ParsedRecipe {
        val lines = text.lines()
            .map { it.trim().removeSuffix(",").trim() }
            .filter { it.isNotEmpty() }

        val ingredientIndex = lines.indexOfFirst { ingredientHeading.matches(it) }
        val methodIndex = lines.indexOfFirst { methodHeading.matches(it) }
        val sectionStart = if (ingredientIndex >= 0) ingredientIndex + 1 else 0
        val sectionEnd = if (ingredientIndex >= 0 && methodIndex > sectionStart) methodIndex else lines.size
        val sectionLines = lines.subList(sectionStart, sectionEnd)

        var yieldText = ""
        var yieldItem = ""
        var yieldLineIndex = -1
        lines.forEachIndexed { index, line ->
            if (yieldLineIndex == -1) {
                parseYield(line)?.let { (amount, item) ->
                    yieldText = amount
                    yieldItem = item
                    yieldLineIndex = index
                }
            }
        }

        val nameCandidates = if (ingredientIndex >= 0) lines.subList(0, ingredientIndex) else lines.take(3)
        val name = nameCandidates
            .firstOrNull { line ->
                line != lines.getOrNull(yieldLineIndex) &&
                    !sectionHeading.matches(line) &&
                    !isYield(line) &&
                    !hasQuantity(line)
            }
            ?.trim()
            .orEmpty()

        val ingredients = mutableListOf<ParsedIngredient>()
        sectionLines.forEach { line ->
            if (sectionHeading.matches(line) || isYield(line)) {
                return@forEach
            }
            val segments = line.split(',')
            val parsedSegments = segments.mapNotNull { parseSegment(it) }
            if (parsedSegments.isNotEmpty()) {
                ingredients.addAll(parsedSegments)
            } else if (ingredientIndex >= 0 && !methodHeading.matches(line)) {
                val name = line.trim()
                if (name.isNotEmpty()) {
                    ingredients.add(ParsedIngredient(name, "", Unit.EACH))
                }
            }
        }

        return ParsedRecipe(
            name = name,
            yieldText = yieldText.ifBlank { "1" },
            yieldItem = yieldItem,
            ingredients = ingredients,
        )
    }

    private fun parseYield(line: String): Pair<String, String>? {
        yieldKeyword.matchEntire(line)?.let { match ->
            val amount = match.groupValues[2]
            val item = match.groupValues[3].trim().removeSuffix(",").trim()
            return amount to item.ifEmpty { "servings" }
        }
        yieldCount.matchEntire(line)?.let { match ->
            return match.groupValues[1] to match.groupValues[2].lowercase()
        }
        return null
    }

    private fun isYield(line: String): Boolean = parseYield(line) != null

    private fun hasQuantity(line: String): Boolean = line.split(',').any { parseSegment(it) != null }

    private fun parseSegment(segment: String): ParsedIngredient? {
        val match = ingredient.matchEntire(segment.trim()) ?: return null
        val amountRaw = match.groups["amount"]?.value.orEmpty().trim()
        val unitRaw = match.groups["unit"]?.value?.trim()?.lowercase().orEmpty()
        var rest = match.groups["rest"]?.value.orEmpty().trim().removeSuffix(",").trim()
        if (rest.lowercase().startsWith("of ")) {
            rest = rest.substring(3).trim()
        }
        if (rest.isEmpty()) {
            return null
        }
        val amount = parseAmount(amountRaw) ?: return null
        val unit = unitRaw.toUnit() ?: Unit.EACH
        return ParsedIngredient(
            name = rest,
            amountText = formatAmount(amount),
            unit = unit,
        )
    }

    private fun parseAmount(raw: String): Double? {
        mixedNumber.matchEntire(raw)?.let { match ->
            val whole = match.groupValues[1].toDouble()
            val numerator = match.groupValues[2].toDouble()
            val denominator = match.groupValues[3].toDouble()
            if (denominator != 0.0) {
                return whole + numerator / denominator
            }
            return null
        }
        fraction.matchEntire(raw)?.let { match ->
            val numerator = match.groupValues[1].toDouble()
            val denominator = match.groupValues[2].toDouble()
            if (denominator != 0.0) {
                return numerator / denominator
            }
            return null
        }
        return raw.toDoubleOrNull()
    }

    private fun formatAmount(value: Double): String {
        if (!value.isFinite() || value < 0.0) {
            return ""
        }
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun String.toUnit(): Unit? = when {
        this.isEmpty() -> null
        this in listOf("g", "gram", "grams") -> Unit.G
        this in listOf("kg", "kilogram", "kilograms") -> Unit.KG
        this in listOf("ml", "millilitre", "millilitres", "milliliter", "milliliters") -> Unit.ML
        this in listOf("l", "litre", "litres", "liter", "liters") -> Unit.L
        this in listOf("tsp", "teaspoon", "teaspoons") -> Unit.TSP
        this in listOf("tbsp", "tablespoon", "tablespoons") -> Unit.TBSP
        this in listOf("cup", "cups") -> Unit.CUP
        this in listOf("oz", "ounce", "ounces") -> Unit.OZ
        this in listOf("each", "ea", "eaches") -> Unit.EACH
        else -> null
    }
}
