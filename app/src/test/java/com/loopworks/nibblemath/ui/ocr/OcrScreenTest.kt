package com.loopworks.nibblemath.ui.ocr

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.loopworks.nibblemath.TestActivity
import com.loopworks.nibblemath.data.DataTestBase
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.settings.SettingsRepository
import com.loopworks.nibblemath.network.PriceFetcher
import com.loopworks.nibblemath.network.PriceLookupClient
import com.loopworks.nibblemath.network.PriceResult
import com.loopworks.nibblemath.network.PriceSource
import com.loopworks.nibblemath.network.RateLimiter
import java.io.IOException
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OcrScreenTest : DataTestBase() {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestActivity>(TestActivity::class.java)

    @Test
    fun showsCaptureOptions() {
        val now = 0L
        val woolworths = NoopSource("Woolworths")
        val coles = NoopSource("Coles")
        val aldi = NoopSource("ALDI")
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

        composeRule.setContent {
            OcrScreen(
                container = container,
                bookId = 1L,
                onBack = {},
                onSaved = {},
            )
        }

        composeRule.onNodeWithText("Import recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Take photo").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from gallery").assertIsDisplayed()
    }
}

private class NoopSource(override val store: String) : PriceSource {
    override suspend fun search(query: String): List<PriceResult> = emptyList()
}

private object NoopPriceFetcher : PriceFetcher {
    override suspend fun get(url: String, headers: Map<String, String>): String =
        throw IOException("no network in tests")

    override suspend fun post(url: String, body: String, headers: Map<String, String>): String =
        throw IOException("no network in tests")
}
