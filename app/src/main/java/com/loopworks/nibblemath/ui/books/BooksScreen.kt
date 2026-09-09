package com.loopworks.nibblemath.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.Book
import com.loopworks.nibblemath.data.model.RecipeSummary
import com.loopworks.nibblemath.ui.costing.CostDisplay
import com.loopworks.nibblemath.ui.costing.CostSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    container: AppContainer,
    onOpenRecipe: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenPantry: () -> Unit,
    onOpenOcr: (Long?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var recipes by remember { mutableStateOf<List<RecipeSummary>>(emptyList()) }
    var costs by remember { mutableStateOf<Map<Long, CostDisplay>>(emptyMap()) }
    var currency by remember { mutableStateOf("AUD") }
    var rounding by remember { mutableStateOf(2) }
    var showAddBook by remember { mutableStateOf(false) }
    var showRenameBook by remember { mutableStateOf(false) }
    var showDeleteBook by remember { mutableStateOf(false) }
    var newBookName by remember { mutableStateOf("") }
    var renameBookName by remember { mutableStateOf("") }

    val selectedBook = books.firstOrNull { it.id == selectedBookId }
    val newRecipeName = stringResource(R.string.new_recipe)
    val defaultYieldItem = stringResource(R.string.default_yield_item)

    suspend fun refreshBooks(selectId: Long? = null) {
        val latest = withContext(Dispatchers.IO) { container.bookRepository.books() }
        books = latest
        selectedBookId = selectId
            ?: selectedBookId?.takeIf { id -> latest.any { it.id == id } }
            ?: latest.firstOrNull()?.id
    }

    suspend fun loadRecipes(bookId: Long?) {
        val list = withContext(Dispatchers.IO) {
            bookId?.let { container.bookRepository.recipes(it) } ?: emptyList()
        }
        recipes = list
        costs = withContext(Dispatchers.IO) {
            list.associate { summary ->
                val recipe = container.recipeRepository.get(summary.id)
                summary.id to CostSummary.display(CostSummary.cost(recipe), currency, rounding)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBooks()
    }
    LaunchedEffect(selectedBookId, currency, rounding) {
        loadRecipes(selectedBookId)
    }
    LaunchedEffect(Unit) {
        container.settingsRepository.currency.collect { currency = it }
    }
    LaunchedEffect(Unit) {
        container.settingsRepository.priceRounding.collect { rounding = it }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadRecipes(selectedBookId) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = { onOpenOcr(selectedBookId) },
                        enabled = selectedBookId != null,
                    ) {
                        Text(stringResource(R.string.ocr_import))
                    }
                    TextButton(onClick = onOpenPantry) {
                        Text(stringResource(R.string.pantry_title))
                    }
                    IconButton(onClick = {
                        newBookName = ""
                        showAddBook = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.books_add_book))
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (books.isEmpty()) {
                EmptyBooks(
                    onCreateBook = {
                        newBookName = ""
                        showAddBook = true
                    },
                )
            } else if (maxWidth >= 840.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BookListPane(
                        books = books,
                        selectedBookId = selectedBookId,
                        onSelect = { selectedBookId = it },
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight(),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    RecipePane(
                        book = selectedBook,
                        recipes = recipes,
                        costs = costs,
                        onAddRecipe = {
                            scope.launch {
                                val bookId = selectedBookId ?: return@launch
                                val recipeId = withContext(Dispatchers.IO) {
                                    container.recipeRepository.create(bookId, newRecipeName, 1.0, defaultYieldItem)
                                }
                                loadRecipes(bookId)
                                onOpenRecipe(recipeId)
                            }
                        },
                        onOpenRecipe = onOpenRecipe,
                        onRenameBook = {
                            renameBookName = selectedBook?.name.orEmpty()
                            showRenameBook = true
                        },
                        onDeleteBook = { showDeleteBook = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        items(books) { book ->
                            FilterChip(
                                selected = book.id == selectedBookId,
                                onClick = { selectedBookId = book.id },
                                label = { Text(book.name) },
                            )
                        }
                    }
                    RecipePane(
                        book = selectedBook,
                        recipes = recipes,
                        costs = costs,
                        onAddRecipe = {
                            scope.launch {
                                val bookId = selectedBookId ?: return@launch
                                val recipeId = withContext(Dispatchers.IO) {
                                    container.recipeRepository.create(bookId, newRecipeName, 1.0, defaultYieldItem)
                                }
                                loadRecipes(bookId)
                                onOpenRecipe(recipeId)
                            }
                        },
                        onOpenRecipe = onOpenRecipe,
                        onRenameBook = {
                            renameBookName = selectedBook?.name.orEmpty()
                            showRenameBook = true
                        },
                        onDeleteBook = { showDeleteBook = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showAddBook) {
        AlertDialog(
            onDismissRequest = { showAddBook = false },
            title = { Text(stringResource(R.string.books_add_book)) },
            text = {
                OutlinedTextField(
                    value = newBookName,
                    onValueChange = { newBookName = it },
                    label = { Text(stringResource(R.string.book_name_label)) },
                    placeholder = { Text(stringResource(R.string.book_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newBookName.isNotBlank(),
                    onClick = {
                        val name = newBookName.trim()
                        showAddBook = false
                        scope.launch {
                            val id = withContext(Dispatchers.IO) { container.bookRepository.create(name) }
                            refreshBooks(id)
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBook = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showRenameBook && selectedBook != null) {
        AlertDialog(
            onDismissRequest = { showRenameBook = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = renameBookName,
                    onValueChange = { renameBookName = it },
                    label = { Text(stringResource(R.string.book_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameBookName.isNotBlank(),
                    onClick = {
                        val name = renameBookName.trim()
                        val id = selectedBook.id
                        showRenameBook = false
                        scope.launch {
                            withContext(Dispatchers.IO) { container.bookRepository.rename(id, name) }
                            refreshBooks(id)
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameBook = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteBook && selectedBook != null) {
        AlertDialog(
            onDismissRequest = { showDeleteBook = false },
            title = { Text(stringResource(R.string.delete_book_title)) },
            text = { Text(stringResource(R.string.delete_book_message, selectedBook.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = selectedBook.id
                        showDeleteBook = false
                        scope.launch {
                            withContext(Dispatchers.IO) { container.bookRepository.delete(id) }
                            refreshBooks()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBook = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyBooks(onCreateBook: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.books_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.books_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateBook) {
            Text(stringResource(R.string.books_add_book))
        }
    }
}

@Composable
private fun BookListPane(
    books: List<Book>,
    selectedBookId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(books) { book ->
            val isSelected = book.id == selectedBookId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onSelect(book.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecipePane(
    book: Book?,
    recipes: List<RecipeSummary>,
    costs: Map<Long, CostDisplay>,
    onAddRecipe: () -> Unit,
    onOpenRecipe: (Long) -> Unit,
    onRenameBook: () -> Unit,
    onDeleteBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (book != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.recipe_count, recipes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRenameBook) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
                }
                IconButton(onClick = onDeleteBook) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (recipes.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_recipes_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_recipes_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAddRecipe) {
                    Text(stringResource(R.string.books_add_recipe))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recipes) { summary ->
                    RecipeCard(
                        summary = summary,
                        display = costs[summary.id],
                        onClick = { onOpenRecipe(summary.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    summary: RecipeSummary,
    display: CostDisplay?,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = costText(display, summary.yieldItem),
                style = MaterialTheme.typography.bodyMedium,
                color = if (display?.hasPriced == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun costText(display: CostDisplay?, yieldItem: String): String {
    return when {
        display == null || !display.hasIngredients -> stringResource(R.string.recipe_no_ingredients)
        !display.hasPriced -> stringResource(R.string.recipe_no_priced)
        else -> {
            val parts = mutableListOf(
                stringResource(R.string.recipe_cost_batch, display.batchPrice.orEmpty()),
            )
            display.perItemPrice?.let {
                parts += stringResource(
                    R.string.recipe_cost_per_item,
                    it,
                    yieldItem.ifBlank { stringResource(R.string.default_yield_item) },
                )
            }
            if (display.missingCount > 0) {
                parts += stringResource(R.string.recipe_cost_missing, display.missingCount)
            }
            parts.joinToString(" · ")
        }
    }
}
