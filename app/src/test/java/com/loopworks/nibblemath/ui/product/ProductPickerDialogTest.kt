package com.loopworks.nibblemath.ui.product

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.DataTestBase
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.Product
import com.loopworks.nibblemath.data.settings.SettingsRepository
import com.loopworks.nibblemath.network.PriceFetcher
import com.loopworks.nibblemath.network.PriceLookupClient
import com.loopworks.nibblemath.network.PriceResult
import com.loopworks.nibblemath.network.PriceSource
import com.loopworks.nibblemath.network.RateLimiter
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class ProductPickerDialogTest : DataTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun storeSearchShowsResultAndSavesProduct() {
        val now = 5_000L
        val result = PriceResult(
            store = "Woolworths",
            productName = "Milk 1L",
            price = 3.50,
            packSize = PackSize(1.0, Unit.L),
            url = null,
            fetchedAt = now,
        )
        val woolworths = FakeStoreSource("Woolworths", listOf(result))
        val coles = FakeStoreSource("Coles", emptyList())
        val aldi = FakeStoreSource("ALDI", emptyList())
        val priceLookupClient = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(woolworths, coles, aldi),
            clock = { now },
            rateLimiter = RateLimiter({ now }, 0),
        )
        val container = AppContainer(
            AppContainer.Dependencies(
                database = database,
                bookRepository = books,
                recipeRepository = recipes,
                catalogRepository = catalog,
                productRepository = products,
                pantryRepository = pantry,
                priceCacheRepository = priceCache,
                backupManager = backup,
                settingsRepository = SettingsRepository(context),
                priceFetcher = NoopPriceFetcher,
                priceRateLimiter = RateLimiter({ now }, 0),
                woolworthsSource = woolworths,
                colesSource = coles,
                aldiSource = aldi,
                priceLookupClient = priceLookupClient,
            ),
        )

        val saved = AtomicReference<Product>()
        composeRule.setContent {
            ProductPickerDialog(
                container = container,
                title = "Choose product",
                currency = "AUD",
                rounding = 2,
                onDismiss = {},
                onSave = { saved.set(it) },
            )
        }

        composeRule.onNodeWithTag("store_search").performScrollTo()
        composeRule.onNodeWithTag("store_search").performTextInput("milk")
        composeRule.onNodeWithTag("store_search_button").performScrollTo()
        composeRule.onNodeWithTag("store_search_button").performClick()

        var attempts = 0
        while (
            composeRule.onAllNodesWithText("Milk 1L", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty() &&
            attempts < 500
        ) {
            Thread.sleep(10)
            composeRule.waitForIdle()
            attempts++
        }
        composeRule.onNodeWithText("Milk 1L", useUnmergedTree = true).performScrollTo()
        composeRule.onNodeWithText("Milk 1L", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Milk 1L", useUnmergedTree = true).performClick()

        attempts = 0
        while (saved.get() == null && attempts < 500) {
            Thread.sleep(10)
            composeRule.waitForIdle()
            attempts++
        }
        val product = assertNotNull(saved.get())
        assertEquals("Milk 1L", product.name)
        assertEquals(3.50, product.price)
        assertEquals(PackSize(1.0, Unit.L), product.packSize)
        assertEquals("Woolworths", product.source)
    }
}

private class FakeStoreSource(
    override val store: String,
    private val results: List<PriceResult>,
) : PriceSource {
    override suspend fun search(query: String): List<PriceResult> = results
}

private object NoopPriceFetcher : PriceFetcher {
    override suspend fun get(url: String, headers: Map<String, String>): String =
        throw IOException("no network in tests")

    override suspend fun post(url: String, body: String, headers: Map<String, String>): String =
        throw IOException("no network in tests")
}
