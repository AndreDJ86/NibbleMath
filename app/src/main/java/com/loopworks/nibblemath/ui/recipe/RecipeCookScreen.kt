@file:OptIn(ExperimentalMaterial3Api::class)

package com.loopworks.nibblemath.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.core.costing.RecipeCost
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.Product
import com.loopworks.nibblemath.data.model.Recipe
import com.loopworks.nibblemath.ui.costing.CostDisplay
import com.loopworks.nibblemath.ui.costing.CostSummary
import com.loopworks.nibblemath.ui.product.ProductPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RecipeCookScreen(
    container: AppContainer,
    recipeId: String,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val id = recipeId.toLongOrNull()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var loaded by remember { mutableStateOf(false) }
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var targetYield by remember { mutableStateOf(0.0) }
    var currency by remember { mutableStateOf("AUD") }
    var rounding by remember { mutableStateOf(2) }
    var pickerLine by remember { mutableStateOf<IngredientLine?>(null) }
    var savingProduct by remember { mutableStateOf(false) }

    suspend fun load() {
        val loadedRecipe = withContext(Dispatchers.IO) {
            id?.let { container.recipeRepository.get(it) }
        }
        recipe = loadedRecipe
        loadedRecipe?.let {
            if (targetYield <= 0.0) {
                targetYield = if (it.yieldAmount > 0.0) it.yieldAmount else 1.0
            }
        }
        loaded = true
    }

    LaunchedEffect(id) {
        load()
    }
    LaunchedEffect(Unit) {
        container.settingsRepository.currency.collect { currency = it }
    }
    LaunchedEffect(Unit) {
        container.settingsRepository.priceRounding.collect { rounding = it }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { load() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!loaded) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_loading))
            }
        }
        return
    }

    val currentRecipe = recipe
    if (currentRecipe == null) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.recipe_missing_title))
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                }
            }
        }
        return
    }

    val locale = LocalLocale.current.platformLocale
    val factor = CookingCalculator.factor(currentRecipe.yieldAmount, targetYield)
    val scaledRecipe = remember(currentRecipe, targetYield) {
        CookingCalculator.scaledRecipe(currentRecipe, targetYield)
    }
    val cost: RecipeCost = remember(scaledRecipe) { CostSummary.cost(scaledRecipe) }
    val display: CostDisplay = remember(cost, currency, rounding, locale) {
        CostSummary.display(cost, currency, rounding, locale)
    }
    val yieldItem = currentRecipe.yieldItem.ifBlank { stringResource(R.string.default_yield_item) }

    fun saveProduct(line: IngredientLine, product: Product) {
        val base = recipe ?: return
        if (savingProduct) return
        scope.launch {
            savingProduct = true
            val updated = base.copy(
                ingredients = base.ingredients.map { existing ->
                    if (existing.id == line.id) {
                        existing.copy(
                            productId = product.id,
                            productName = product.name,
                            productPackSize = product.packSize,
                            productPrice = product.price,
                        )
                    } else {
                        existing
                    }
                },
            )
            withContext(Dispatchers.IO) { container.recipeRepository.update(updated) }
            recipe = withContext(Dispatchers.IO) { container.recipeRepository.get(base.id) }
            savingProduct = false
            pickerLine = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentRecipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(currentRecipe.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.recipe_cook_edit),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.recipe_cook_scale_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                if (targetYield > 1.0) {
                                    targetYield = (targetYield - 1.0).coerceAtLeast(1.0)
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.recipe_cook_scale_down_symbol))
                        }
                        Text(
                            text = "${CookingCalculator.formatAmount(targetYield)} $yieldItem",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { targetYield += 1.0 }) {
                            Text(text = stringResource(R.string.recipe_cook_scale_up_symbol))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = costSummaryText(display, yieldItem),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (display.hasPriced) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            if (currentRecipe.notes.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.recipe_notes_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentRecipe.notes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.recipe_ingredients_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (currentRecipe.ingredients.isEmpty()) {
                Text(
                    text = stringResource(R.string.recipe_no_ingredients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                currentRecipe.ingredients.forEachIndexed { index, line ->
                    val item = cost.items.getOrNull(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickerLine = line }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.ingredientName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = CookingCalculator.displayAmount(
                                    CookingCalculator.scaledAmount(line, factor),
                                    line.unit,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AssistChip(
                            onClick = { pickerLine = line },
                            label = {
                                Text(line.productName ?: stringResource(R.string.recipe_cook_product_none))
                            },
                        )
                        Text(
                            text = item?.cost?.let {
                                CostSummary.formatPrice(it, currency, rounding, locale)
                            } ?: stringResource(R.string.recipe_cook_no_price),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.widthIn(min = 64.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.recipe_steps_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (currentRecipe.steps.isEmpty()) {
                Text(
                    text = stringResource(R.string.recipe_cook_no_steps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                currentRecipe.steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    val activePickerLine = pickerLine
    if (activePickerLine != null) {
        ProductPickerDialog(
            container = container,
            title = stringResource(R.string.recipe_cook_product_picker_title),
            initialName = activePickerLine.productName ?: activePickerLine.ingredientName,
            initialPack = activePickerLine.productPackSize,
            initialPrice = activePickerLine.productPrice,
            currency = currency,
            rounding = rounding,
            onDismiss = { pickerLine = null },
            onSave = { product -> saveProduct(activePickerLine, product) },
        )
    }
}

@Composable
private fun costSummaryText(display: CostDisplay, yieldItem: String): String {
    return when {
        !display.hasIngredients -> stringResource(R.string.recipe_no_ingredients)
        !display.hasPriced -> stringResource(R.string.recipe_no_priced)
        else -> {
            val parts = mutableListOf(
                stringResource(R.string.recipe_cost_batch, display.batchPrice.orEmpty()),
            )
            display.perItemPrice?.let {
                parts += stringResource(R.string.recipe_cost_per_item, it, yieldItem)
            }
            if (display.missingCount > 0) {
                parts += stringResource(R.string.recipe_cost_missing, display.missingCount)
            }
            parts.joinToString(" · ")
        }
    }
}
