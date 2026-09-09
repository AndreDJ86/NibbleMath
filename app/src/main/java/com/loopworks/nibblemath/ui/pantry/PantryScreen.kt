@file:OptIn(ExperimentalMaterial3Api::class)

package com.loopworks.nibblemath.ui.pantry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.core.units.Unit as NibbleUnit
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.Book
import com.loopworks.nibblemath.data.model.Ingredient
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.model.PantryEntry
import com.loopworks.nibblemath.data.model.RecipeSummary
import com.loopworks.nibblemath.ui.common.UnitSelector
import com.loopworks.nibblemath.ui.costing.CostSummary
import com.loopworks.nibblemath.ui.product.ProductPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantryScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var entries by remember { mutableStateOf<List<PantryEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("AUD") }
    var rounding by remember { mutableStateOf(2) }
    var showAddIngredient by remember { mutableStateOf(false) }
    var addIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var editEntry by remember { mutableStateOf<PantryEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<PantryEntry?>(null) }
    var useEntry by remember { mutableStateOf<PantryEntry?>(null) }

    suspend fun load() {
        val result = withContext(Dispatchers.IO) { container.pantryRepository.entries() }
        withContext(Dispatchers.Main) {
            entries = result
            loaded = true
        }
    }

    LaunchedEffect(Unit) {
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

    val locale = LocalLocale.current.platformLocale
    val query = search.trim().lowercase(locale)
    val visibleEntries = remember(entries, query) {
        if (query.isEmpty()) {
            entries
        } else {
            entries.filter { entry ->
                entry.ingredientName.lowercase(locale).contains(query) ||
                    entry.productName.lowercase(locale).contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pantry_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showAddIngredient = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.pantry_add),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.pantry_add))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(stringResource(R.string.pantry_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (!loaded) {
                Text(stringResource(R.string.pantry_loading))
            } else if (visibleEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.pantry_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pantry_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        PantryEntryCard(
                            entry = entry,
                            currency = currency,
                            rounding = rounding,
                            onUse = { useEntry = entry },
                            onEdit = { editEntry = entry },
                            onDelete = { deleteEntry = entry },
                        )
                    }
                }
            }
        }
    }

    if (showAddIngredient) {
        IngredientSearchDialog(
            container = container,
            onDismiss = { showAddIngredient = false },
            onSelect = { ingredient ->
                showAddIngredient = false
                addIngredient = ingredient
            },
        )
    }

    addIngredient?.let { ingredient ->
        ProductPickerDialog(
            container = container,
            title = stringResource(R.string.pantry_add_product_title),
            initialName = ingredient.name,
            currency = currency,
            rounding = rounding,
            onDismiss = { addIngredient = null },
            onSave = { product ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        container.pantryRepository.add(ingredient.id, product.id)
                    }
                    addIngredient = null
                    load()
                }
            },
        )
    }

    editEntry?.let { entry ->
        ProductPickerDialog(
            container = container,
            title = stringResource(R.string.pantry_edit_product_title),
            initialName = entry.productName,
            initialPack = entry.productPackSize,
            initialPrice = entry.productPrice,
            currency = currency,
            rounding = rounding,
            onDismiss = { editEntry = null },
            onSave = { product ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        container.pantryRepository.updateProduct(entry.id, product.id)
                    }
                    editEntry = null
                    load()
                }
            },
        )
    }

    deleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text(stringResource(R.string.pantry_delete_title)) },
            text = { Text(stringResource(R.string.pantry_delete_message, entry.ingredientName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { container.pantryRepository.remove(entry.id) }
                            deleteEntry = null
                            load()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntry = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    useEntry?.let { entry ->
        UseInRecipeDialog(
            container = container,
            entry = entry,
            onDismiss = { useEntry = null },
            onUsed = {
                scope.launch {
                    useEntry = null
                    load()
                }
            },
        )
    }
}

@Composable
private fun PantryEntryCard(
    entry: PantryEntry,
    currency: String,
    rounding: Int,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("pantry_entry")) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = entry.ingredientName,
                style = MaterialTheme.typography.titleMedium,
            )
            val productDetails = buildString {
                append(entry.productName)
                entry.productPackSize?.let {
                    append(" · ")
                    append(it.amount)
                    append(" ")
                    append(it.unit.symbol)
                }
                entry.productPrice?.let {
                    append(" · ")
                    append(CostSummary.formatPrice(it, currency, rounding, locale))
                }
            }
            Text(
                text = productDetails,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUse) {
                    Text(stringResource(R.string.pantry_use))
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.pantry_edit))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
private fun IngredientSearchDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSelect: (Ingredient) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<Ingredient>>(emptyList()) }
    var showManual by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualUnit by remember { mutableStateOf<NibbleUnit>(NibbleUnit.G) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ingredients = withContext(Dispatchers.IO) { container.catalogRepository.all() }
    }

    val locale = LocalLocale.current.platformLocale
    val query = search.trim().lowercase(locale)
    val results = remember(ingredients, query) {
        if (query.isEmpty()) {
            ingredients.take(20)
        } else {
            ingredients.filter { ingredient ->
                ingredient.name.lowercase(locale).contains(query) ||
                    ingredient.aliases.any { it.lowercase(locale).contains(query) }
            }.take(20)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pantry_add_ingredient_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.ingredient_search_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text(stringResource(R.string.pantry_no_ingredients))
                }
                results.forEach { ingredient ->
                    ListItem(
                        headlineContent = { Text(ingredient.name) },
                        supportingContent = {
                            if (ingredient.aliases.isNotEmpty()) {
                                Text(ingredient.aliases.joinToString(", "))
                            }
                        },
                        modifier = Modifier.clickable { onSelect(ingredient) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (!showManual) {
                    TextButton(onClick = { showManual = true }) {
                        Text(stringResource(R.string.ingredient_manual_add))
                    }
                } else {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text(stringResource(R.string.ingredient_name_label)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    UnitSelector(selected = manualUnit, onUnitChange = { manualUnit = it })
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (manualName.isBlank() || creating) return@Button
                            creating = true
                            scope.launch {
                                val id = withContext(Dispatchers.IO) {
                                    container.catalogRepository.upsert(manualName.trim(), emptyList(), manualUnit)
                                }
                                val created = withContext(Dispatchers.IO) {
                                    container.catalogRepository.byId(id)
                                }
                                creating = false
                                created?.let(onSelect)
                            }
                        },
                        enabled = manualName.isNotBlank() && !creating,
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun UseInRecipeDialog(
    container: AppContainer,
    entry: PantryEntry,
    onDismiss: () -> Unit,
    onUsed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var recipes by remember { mutableStateOf<List<RecipeSummary>>(emptyList()) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        books = withContext(Dispatchers.IO) { container.bookRepository.books() }
        selectedBookId = books.firstOrNull()?.id
    }
    LaunchedEffect(selectedBookId) {
        val bookId = selectedBookId ?: return@LaunchedEffect
        recipes = withContext(Dispatchers.IO) { container.bookRepository.recipes(bookId) }
    }

    fun useInRecipe(recipeId: Long) {
        if (adding) return
        adding = true
        scope.launch {
            val recipe = withContext(Dispatchers.IO) { container.recipeRepository.get(recipeId) }
            if (recipe != null) {
                val ingredient = withContext(Dispatchers.IO) {
                    container.catalogRepository.byId(entry.ingredientId)
                }
                val existingIndex = recipe.ingredients.indexOfFirst { it.ingredientId == entry.ingredientId }
                val updatedIngredients = if (existingIndex >= 0) {
                    recipe.ingredients.mapIndexed { index, line ->
                        if (index == existingIndex) {
                            line.copy(
                                amount = line.amount + 1.0,
                                productId = line.productId ?: entry.productId,
                                productName = line.productName ?: entry.productName,
                                productPackSize = line.productPackSize ?: entry.productPackSize,
                                productPrice = line.productPrice ?: entry.productPrice,
                            )
                        } else {
                            line
                        }
                    }
                } else {
                    recipe.ingredients +
                        IngredientLine(
                            id = 0,
                            recipeId = recipe.id,
                            ingredientId = entry.ingredientId,
                            ingredientName = entry.ingredientName,
                            productId = entry.productId,
                            productName = entry.productName,
                            amount = 1.0,
                            unit = ingredient?.defaultUnit ?: NibbleUnit.G,
                            sortOrder = recipe.ingredients.size,
                            productPackSize = entry.productPackSize,
                            productPrice = entry.productPrice,
                        )
                }
                withContext(Dispatchers.IO) {
                    container.recipeRepository.update(recipe.copy(ingredients = updatedIngredients))
                }
            }
            adding = false
            onUsed()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pantry_use_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (books.isEmpty()) {
                    Text(stringResource(R.string.pantry_no_books))
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                    ) {
                        books.forEach { book ->
                            FilterChip(
                                selected = book.id == selectedBookId,
                                onClick = { selectedBookId = book.id },
                                label = { Text(book.name) },
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (recipes.isEmpty()) {
                        Text(stringResource(R.string.pantry_no_recipes))
                    } else {
                        recipes.forEach { recipe ->
                            ListItem(
                                headlineContent = { Text(recipe.name) },
                                supportingContent = {
                                    Text(
                                        "${recipe.yieldAmount} ${recipe.yieldItem}".trim(),
                                    )
                                },
                                modifier = Modifier.clickable { useInRecipe(recipe.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
