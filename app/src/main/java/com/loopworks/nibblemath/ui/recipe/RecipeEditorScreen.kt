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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.core.units.Unit as NibbleUnit
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.ui.common.UnitSelector
import com.loopworks.nibblemath.data.model.Ingredient
import com.loopworks.nibblemath.data.model.PantryEntry
import com.loopworks.nibblemath.data.model.Product
import com.loopworks.nibblemath.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RecipeEditorScreen(
    container: AppContainer,
    recipeId: String,
    onBack: () -> Unit,
) {
    val id = recipeId.toLongOrNull()
    var loaded by remember { mutableStateOf(false) }
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var draft by remember { mutableStateOf<RecipeDraft?>(null) }
    var pickerKey by remember { mutableStateOf<Long?>(null) }
    var saving by remember { mutableStateOf(false) }
    var nextKey by remember { mutableStateOf(-1L) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(id) {
        if (id != null) {
            val loadedRecipe = withContext(Dispatchers.IO) { container.recipeRepository.get(id) }
            recipe = loadedRecipe
            draft = loadedRecipe?.let { RecipeDraft.fromRecipe(it) }
        }
        loaded = true
    }

    fun updateDraft(transform: (RecipeDraft) -> RecipeDraft) {
        draft = draft?.let(transform)
    }

    fun addIngredient() {
        val key = nextKey
        nextKey = key - 1
        updateDraft { it.copy(ingredients = it.ingredients + IngredientDraft(key, null, "", "1", NibbleUnit.G)) }
        pickerKey = key
    }

    fun removeIngredient(key: Long) {
        updateDraft { it.copy(ingredients = it.ingredients.filter { line -> line.key != key }) }
    }

    fun updateIngredient(key: Long, transform: (IngredientDraft) -> IngredientDraft) {
        updateDraft {
            it.copy(ingredients = it.ingredients.map { line -> if (line.key == key) transform(line) else line })
        }
    }

    fun addStep() {
        updateDraft { it.copy(steps = it.steps + "") }
    }

    fun updateStep(index: Int, value: String) {
        updateDraft { it.copy(steps = it.steps.mapIndexed { i, step -> if (i == index) value else step }) }
    }

    fun removeStep(index: Int) {
        updateDraft { it.copy(steps = it.steps.filterIndexed { i, _ -> i != index }) }
    }

    fun save() {
        val base = recipe ?: return
        val current = draft ?: return
        if (!current.canSave || saving) return
        scope.launch {
            saving = true
            focusManager.clearFocus()
            withContext(Dispatchers.IO) { container.recipeRepository.update(current.toRecipe(base)) }
            saving = false
            onBack()
        }
    }

    if (!loaded) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_loading))
            }
        }
        return
    }

    val currentDraft = draft
    if (currentDraft == null) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipe_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = { save() }, enabled = currentDraft.canSave && !saving) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = currentDraft.name,
                onValueChange = { value -> updateDraft { it.copy(name = value) } },
                label = { Text(stringResource(R.string.recipe_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentDraft.yieldText,
                    onValueChange = { value -> updateDraft { it.copy(yieldText = value) } },
                    label = { Text(stringResource(R.string.recipe_yield_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = currentDraft.yieldItem,
                    onValueChange = { value -> updateDraft { it.copy(yieldItem = value) } },
                    label = { Text(stringResource(R.string.recipe_yield_item_label)) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = currentDraft.notes,
                onValueChange = { value -> updateDraft { it.copy(notes = value) } },
                label = { Text(stringResource(R.string.recipe_notes_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.recipe_ingredients_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            currentDraft.ingredients.forEach { line ->
                IngredientRow(
                    line = line,
                    onNameClick = { pickerKey = line.key },
                    onAmountChange = { value -> updateIngredient(line.key) { it.copy(amountText = value) } },
                    onUnitChange = { unit -> updateIngredient(line.key) { it.copy(unit = unit) } },
                    onRemove = { removeIngredient(line.key) },
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { addIngredient() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.recipe_add_ingredient))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.recipe_steps_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            currentDraft.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step,
                        onValueChange = { updateStep(index, it) },
                        label = { Text(stringResource(R.string.recipe_step_hint, index + 1)) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { removeStep(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { addStep() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.recipe_add_step))
            }
        }
    }

    val activePickerKey = pickerKey
    if (activePickerKey != null) {
        IngredientPickerDialog(
            container = container,
            onDismiss = { pickerKey = null },
            onSelect = { ingredientId, name, unit, product ->
                val key = pickerKey
                if (key != null) {
                    updateIngredient(key) {
                        it.copy(
                            ingredientId = ingredientId,
                            ingredientName = name,
                            unit = unit,
                            productId = product?.id,
                            productName = product?.name,
                            productPackSize = product?.packSize,
                            productPrice = product?.price,
                        )
                    }
                    pickerKey = null
                }
            },
        )
    }
}

@Composable
private fun IngredientRow(
    line: IngredientDraft,
    onNameClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (NibbleUnit) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = line.ingredientName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ingredient_name_label)) },
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNameClick),
        )
        OutlinedTextField(
            value = line.amountText,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.ingredient_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(84.dp),
        )
        UnitSelector(selected = line.unit, onUnitChange = onUnitChange)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun IngredientPickerDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSelect: (Long, String, NibbleUnit, Product?) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var catalog by remember { mutableStateOf<List<Ingredient>>(emptyList()) }
    var pantryEntries by remember { mutableStateOf<List<PantryEntry>>(emptyList()) }
    var showManual by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualUnit by remember { mutableStateOf(NibbleUnit.G) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) { container.catalogRepository.all() }
        pantryEntries = withContext(Dispatchers.IO) { container.pantryRepository.entries() }
    }

    val query = search.trim().lowercase()
    val catalogResults = remember(catalog, query) {
        if (query.isEmpty()) catalog.take(20)
        else catalog.filter { it.matches(query) }.take(20)
    }
    val pantryResults = remember(pantryEntries, query) {
        if (query.isEmpty()) pantryEntries.take(5)
        else pantryEntries.filter { it.ingredientName.lowercase().contains(query) }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ingredient_picker_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.ingredient_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (pantryResults.isNotEmpty()) {
                    Text(stringResource(R.string.ingredient_pantry_title), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    pantryResults.forEach { entry ->
                        ListItem(
                            headlineContent = { Text(entry.ingredientName) },
                            supportingContent = { Text(entry.productName) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val ingredient = withContext(Dispatchers.IO) {
                                        container.catalogRepository.byId(entry.ingredientId)
                                    }
                                    val product = withContext(Dispatchers.IO) {
                                        container.productRepository.byId(entry.productId)
                                    }
                                    onSelect(entry.ingredientId, entry.ingredientName, ingredient?.defaultUnit ?: NibbleUnit.G, product)
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (catalogResults.isNotEmpty()) {
                    Text(stringResource(R.string.ingredient_catalog_title), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    catalogResults.forEach { ingredient ->
                        ListItem(
                            headlineContent = { Text(ingredient.name) },
                            supportingContent = { Text(ingredient.defaultUnit.symbol) },
                            modifier = Modifier.clickable {
                                onSelect(ingredient.id, ingredient.name, ingredient.defaultUnit, null)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    UnitSelector(selected = manualUnit, onUnitChange = { manualUnit = it })
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = {
                                val name = manualName.trim()
                                if (name.isEmpty() || creating) return@Button
                                creating = true
                                scope.launch {
                                    val ingredientId = withContext(Dispatchers.IO) {
                                        container.catalogRepository.upsert(name, emptyList(), manualUnit)
                                    }
                                    creating = false
                                    onSelect(ingredientId, name, manualUnit, null)
                                }
                            },
                            enabled = manualName.isNotBlank() && !creating,
                        ) {
                            Text(stringResource(R.string.ingredient_manual_save))
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            showManual = false
                            manualName = ""
                        }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun Ingredient.matches(query: String): Boolean {
    val q = query.lowercase()
    return name.lowercase().contains(q) || aliases.any { it.lowercase().contains(q) }
}
