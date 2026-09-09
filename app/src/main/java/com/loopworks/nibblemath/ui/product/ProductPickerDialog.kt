package com.loopworks.nibblemath.ui.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loopworks.nibblemath.R
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.PackSizeParser
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.Product
import com.loopworks.nibblemath.network.PriceResult
import com.loopworks.nibblemath.ui.costing.CostSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProductPickerDialog(
    container: AppContainer,
    title: String,
    initialName: String = "",
    initialPack: PackSize? = null,
    initialPrice: Double? = null,
    currency: String = "AUD",
    rounding: Int = 2,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var showManual by remember {
        mutableStateOf(initialName.isNotBlank() || initialPack != null || initialPrice != null)
    }
    var name by remember { mutableStateOf(initialName) }
    var packText by remember { mutableStateOf(initialPack?.let { "${it.amount} ${it.unit.symbol}" }.orEmpty()) }
    var priceText by remember { mutableStateOf(initialPrice?.let { it.toString() }.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var storeQuery by remember { mutableStateOf("") }
    var storeFilter by remember { mutableStateOf<String?>(null) }
    var storeResults by remember { mutableStateOf<List<PriceResult>>(emptyList()) }
    var searchingStores by remember { mutableStateOf(false) }
    var storeSearched by remember { mutableStateOf(false) }
    var storeError by remember { mutableStateOf<String?>(null) }
    val storeNames = remember { container.priceLookupClient.stores }

    LaunchedEffect(Unit) {
        products = withContext(Dispatchers.IO) { container.productRepository.all() }
    }

    val locale = LocalLocale.current.platformLocale

    fun searchStorePrices() {
        val query = storeQuery.ifBlank { search }.trim()
        if (query.isEmpty() || searchingStores) return
        searchingStores = true
        storeError = null
        scope.launch {
            try {
                storeResults = withContext(Dispatchers.IO) {
                    container.priceLookupClient.searchStores(storeFilter?.let { listOf(it) }, query)
                }.sortedWith(compareBy({ it.store }, { it.productName.lowercase(locale) }))
            } catch (e: Exception) {
                storeError = e.message
            }
            storeSearched = true
            searchingStores = false
        }
    }

    fun applyPriceResult(result: PriceResult) {
        val packSize = result.packSize
        if (packSize == null) {
            showManual = true
            name = result.productName
            packText = ""
            priceText = result.price.toString()
            return
        }
        scope.launch {
            val id = withContext(Dispatchers.IO) {
                container.productRepository.upsert(
                    name = result.productName,
                    packSize = packSize,
                    price = result.price,
                    source = result.store,
                    fetchedAt = result.fetchedAt,
                )
            }
            val saved = withContext(Dispatchers.IO) { container.productRepository.byId(id) }
            saved?.let(onSave)
        }
    }

    val query = search.trim().lowercase(locale)
    val results = remember(products, query) {
        if (query.isEmpty()) {
            products.take(20)
        } else {
            products.filter { it.name.lowercase(locale).contains(query) }.take(20)
        }
    }
    val pack = remember(packText) { PackSizeParser.parse(packText) }
    val price = remember(priceText) { priceText.trim().toDoubleOrNull() }
    val canSave = name.isNotBlank() && pack != null && price != null && price >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.product_picker_search_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                if (results.isEmpty()) {
                    Text(stringResource(R.string.product_picker_empty))
                }
                results.forEach { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = {
                            Text(
                                "${product.packSize.amount} ${product.packSize.unit.symbol} · " +
                                    CostSummary.formatPrice(product.price, currency, rounding),
                            )
                        },
                        modifier = Modifier.clickable { onSave(product) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.price_lookup_results_title), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = storeQuery,
                    onValueChange = { storeQuery = it },
                    label = { Text(stringResource(R.string.price_lookup_store_search)) },
                    singleLine = true,
                    modifier = Modifier.testTag("store_search"),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = storeFilter == null,
                        onClick = {
                            storeFilter = null
                            storeResults = emptyList()
                            storeSearched = false
                        },
                        label = { Text(stringResource(R.string.price_lookup_store_all)) },
                    )
                    storeNames.forEach { store ->
                        FilterChip(
                            selected = storeFilter == store,
                            onClick = {
                                storeFilter = store
                                storeResults = emptyList()
                                storeSearched = false
                            },
                            label = { Text(store) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { searchStorePrices() },
                    enabled = !searchingStores,
                    modifier = Modifier.testTag("store_search_button"),
                ) {
                    if (searchingStores) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(16.dp)
                                .height(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.price_lookup_search))
                }
                if (storeError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(storeError.orEmpty())
                }
                if (searchingStores) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.price_lookup_searching))
                }
                if (storeSearched && !searchingStores && storeResults.isEmpty() && storeError == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.price_lookup_empty))
                }
                storeResults.forEach { result ->
                    Spacer(Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text(result.productName) },
                        supportingContent = {
                            Column {
                                Text(result.store)
                                val unitLabel = CostSummary.unitPriceLabel(
                                    result.price,
                                    result.packSize,
                                    currency,
                                    rounding,
                                    locale,
                                )
                                Text(
                                    buildString {
                                        append(CostSummary.formatPrice(result.price, currency, rounding, locale))
                                        result.packSize?.let { append(" · ").append(it) }
                                        unitLabel?.let { append(" · ").append(it) }
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("store_result_${result.productName}")
                            .clickable { applyPriceResult(result) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (!showManual) {
                    TextButton(onClick = { showManual = true }) {
                        Text(stringResource(R.string.product_picker_manual))
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.product_picker_name_label)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = packText,
                        onValueChange = { packText = it },
                        label = { Text(stringResource(R.string.product_picker_pack_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(stringResource(R.string.product_picker_price_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val packValue = pack ?: return@Button
                            val priceValue = price ?: return@Button
                            if (saving) return@Button
                            saving = true
                            scope.launch {
                                val id = withContext(Dispatchers.IO) {
                                    container.productRepository.upsert(name, packValue, priceValue)
                                }
                                val saved = withContext(Dispatchers.IO) {
                                    container.productRepository.byId(id)
                                }
                                saving = false
                                saved?.let(onSave)
                            }
                        },
                        enabled = canSave && !saving,
                    ) {
                        Text(stringResource(R.string.product_picker_save))
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
