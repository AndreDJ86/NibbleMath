package com.loopworks.nibblemath.ui.ocr

import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.repository.CatalogRepository
import com.loopworks.nibblemath.ui.recipe.IngredientDraft
import com.loopworks.nibblemath.ui.recipe.RecipeDraft

suspend fun ParsedRecipe.toDraft(catalog: CatalogRepository, defaultName: String): RecipeDraft {
    val ingredients = ingredients.mapIndexed { index, parsed ->
        val known = catalog.find(parsed.name)
        IngredientDraft(
            key = -1L - index,
            ingredientId = known?.id,
            ingredientName = known?.name ?: parsed.name,
            amountText = parsed.amountText,
            unit = if (parsed.amountText.isBlank()) {
                known?.defaultUnit ?: Unit.EACH
            } else {
                parsed.unit
            },
        )
    }
    return RecipeDraft(
        name = name.ifBlank { defaultName },
        yieldText = yieldText.ifBlank { "1" },
        yieldItem = yieldItem,
        notes = "",
        steps = emptyList(),
        ingredients = ingredients,
    )
}
