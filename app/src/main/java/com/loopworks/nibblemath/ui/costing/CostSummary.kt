package com.loopworks.nibblemath.ui.costing

import com.loopworks.nibblemath.core.costing.CostingEngine
import com.loopworks.nibblemath.core.costing.Ingredient
import com.loopworks.nibblemath.core.costing.RecipeCost
import com.loopworks.nibblemath.core.units.Dimension
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.data.model.Recipe
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CostDisplay(
    val batchPrice: String?,
    val perItemPrice: String?,
    val missingCount: Int,
    val hasIngredients: Boolean,
    val hasPriced: Boolean,
)

object CostSummary {
    fun cost(recipe: Recipe?): RecipeCost =
        CostingEngine.recipeCost(
            ingredients = recipe?.ingredients?.map {
                Ingredient(
                    name = it.ingredientName,
                    amount = it.amount,
                    unit = it.unit,
                    packSize = it.productPackSize,
                    packPrice = it.productPrice,
                )
            } ?: emptyList(),
            yield = recipe?.yieldAmount ?: 0.0,
        )

    fun display(
        cost: RecipeCost,
        currency: String,
        rounding: Int,
        locale: Locale = Locale.getDefault(),
    ): CostDisplay {
        val hasPriced = cost.items.any { !it.missing }
        return CostDisplay(
            batchPrice = if (cost.items.isNotEmpty() && hasPriced) {
                formatPrice(cost.batchCost, currency, rounding, locale)
            } else {
                null
            },
            perItemPrice = if (hasPriced) {
                cost.perItem?.let { formatPrice(it, currency, rounding, locale) }
            } else {
                null
            },
            missingCount = cost.missingCount,
            hasIngredients = cost.items.isNotEmpty(),
            hasPriced = hasPriced,
        )
    }

    fun formatPrice(
        amount: Double,
        currency: String,
        rounding: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        val places = rounding.coerceAtLeast(0)
        return try {
            val currencyInstance = Currency.getInstance(currency.uppercase(Locale.ROOT))
            NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currencyInstance
                maximumFractionDigits = places
                minimumFractionDigits = places
            }.format(amount)
        } catch (e: IllegalArgumentException) {
            String.format(locale, "%,.${places}f %s", amount, currency)
        }
    }

    fun unitPriceLabel(
        price: Double,
        packSize: PackSize?,
        currency: String,
        rounding: Int,
        locale: Locale = Locale.getDefault(),
    ): String? {
        val base = packSize?.amountInBase ?: return null
        if (base <= 0.0 || !price.isFinite()) return null
        val (unitPrice, suffix) = when (packSize.unit.dimension) {
            Dimension.MASS -> price / (base / 100.0) to "/100g"
            Dimension.VOLUME -> price / (base / 1000.0) to "/L"
            Dimension.COUNT -> price / base to "/each"
        }
        if (!unitPrice.isFinite()) return null
        return formatPrice(unitPrice, currency, rounding, locale) + suffix
    }
}
