package com.loopworks.nibblemath.ui.recipe

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Recipe

data class IngredientDraft(
    val key: Long,
    val ingredientId: Long?,
    val ingredientName: String,
    val amountText: String,
    val unit: Unit,
    val productId: Long? = null,
    val productName: String? = null,
    val productPackSize: PackSize? = null,
    val productPrice: Double? = null,
) {
    val amount: Double?
        get() = amountText.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

    val isValid: Boolean
        get() = ingredientId != null && ingredientName.isNotBlank() && amount != null
}

data class RecipeDraft(
    val name: String,
    val yieldText: String,
    val yieldItem: String,
    val notes: String,
    val steps: List<String>,
    val ingredients: List<IngredientDraft>,
) {
    val yieldAmount: Double?
        get() = yieldText.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }

    val canSave: Boolean
        get() = name.isNotBlank() && yieldAmount != null && ingredients.all { it.isValid }

    fun toRecipe(base: Recipe): Recipe = base.copy(
        name = name.trim(),
        yieldAmount = yieldAmount ?: base.yieldAmount,
        yieldItem = yieldItem.trim(),
        notes = notes,
        steps = steps.map { it.trim() }.filter { it.isNotBlank() },
        ingredients = ingredients.mapIndexed { index, draft ->
            IngredientLine(
                id = 0,
                recipeId = base.id,
                ingredientId = draft.ingredientId ?: 0L,
                ingredientName = draft.ingredientName.trim(),
                productId = draft.productId,
                productName = draft.productName,
                amount = draft.amount ?: 0.0,
                unit = draft.unit,
                sortOrder = index,
                productPackSize = draft.productPackSize,
                productPrice = draft.productPrice,
            )
        },
    )

    companion object {
        fun fromRecipe(recipe: Recipe?): RecipeDraft = recipe?.let {
            RecipeDraft(
                name = it.name,
                yieldText = it.yieldAmount.toString(),
                yieldItem = it.yieldItem,
                notes = it.notes,
                steps = it.steps,
                ingredients = it.ingredients.map { line ->
                    IngredientDraft(
                        key = line.id,
                        ingredientId = line.ingredientId,
                        ingredientName = line.ingredientName,
                        amountText = line.amount.toString(),
                        unit = line.unit,
                        productId = line.productId,
                        productName = line.productName,
                        productPackSize = line.productPackSize,
                        productPrice = line.productPrice,
                    )
                },
            )
        } ?: RecipeDraft(
            name = "",
            yieldText = "1",
            yieldItem = "",
            notes = "",
            steps = emptyList(),
            ingredients = emptyList(),
        )
    }
}
