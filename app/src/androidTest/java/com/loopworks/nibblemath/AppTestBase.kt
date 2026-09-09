package com.loopworks.nibblemath

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import com.loopworks.nibblemath.data.backup.BackupManager
import com.loopworks.nibblemath.data.db.NibbleMathDatabase
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.data.model.IngredientLine
import com.loopworks.nibblemath.data.repository.BookRepository
import com.loopworks.nibblemath.data.repository.CatalogRepository
import com.loopworks.nibblemath.data.repository.PantryRepository
import com.loopworks.nibblemath.data.repository.PriceCacheRepository
import com.loopworks.nibblemath.data.repository.ProductRepository
import com.loopworks.nibblemath.data.repository.RecipeRepository
import com.loopworks.nibblemath.data.settings.SettingsRepository
import com.loopworks.nibblemath.network.PriceFetcher
import com.loopworks.nibblemath.network.PriceLookupClient
import com.loopworks.nibblemath.network.PriceResult
import com.loopworks.nibblemath.network.PriceSource
import com.loopworks.nibblemath.network.RateLimiter
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before

abstract class AppTestBase {
    protected lateinit var context: Context
    protected lateinit var database: NibbleMathDatabase
    protected lateinit var books: BookRepository
    protected lateinit var recipes: RecipeRepository
    protected lateinit var catalog: CatalogRepository
    protected lateinit var products: ProductRepository
    protected lateinit var pantry: PantryRepository
    protected lateinit var priceCache: PriceCacheRepository
    protected lateinit var settings: SettingsRepository
    protected lateinit var container: AppContainer
    protected var now: Long = 1_700_000_000_000L

    @Before
    fun setUpApp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NibbleMathDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        books = BookRepository(database.bookDao(), database.recipeDao())
        recipes = RecipeRepository(
            database.recipeDao(),
            database.stepDao(),
            database.recipeIngredientDao(),
            database.ingredientDao(),
            database.productDao(),
        )
        catalog = CatalogRepository(database.ingredientDao())
        products = ProductRepository(database.productDao())
        pantry = PantryRepository(
            database.pantryEntryDao(),
            database.ingredientDao(),
            database.productDao(),
        )
        priceCache = PriceCacheRepository(database.priceCacheDao())
        File(context.dataStoreDirectory, "settings.preferences_pb").delete()
        settings = SettingsRepository(context)
    }

    @After
    fun tearDownApp() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    protected fun buildContainer(
        woolworths: List<PriceResult> = emptyList(),
        coles: List<PriceResult> = emptyList(),
        aldi: List<PriceResult> = emptyList(),
    ): AppContainer {
        val woolworthsSource = FakePriceSource("Woolworths", woolworths)
        val colesSource = FakePriceSource("Coles", coles)
        val aldiSource = FakePriceSource("ALDI", aldi)
        val rateLimiter = RateLimiter({ now }, 0)
        val client = PriceLookupClient(
            priceCache = priceCache,
            sources = listOf(woolworthsSource, colesSource, aldiSource),
            clock = { now },
            rateLimiter = rateLimiter,
        )
        container = AppContainer(
            AppContainer.Dependencies(
                database = database,
                bookRepository = books,
                recipeRepository = recipes,
                catalogRepository = catalog,
                productRepository = products,
                pantryRepository = pantry,
                priceCacheRepository = priceCache,
                backupManager = BackupManager(database),
                settingsRepository = settings,
                priceFetcher = NoopPriceFetcher,
                priceRateLimiter = rateLimiter,
                woolworthsSource = woolworthsSource,
                colesSource = colesSource,
                aldiSource = aldiSource,
                priceLookupClient = client,
            ),
        )
        return container
    }

    protected fun seedBook(name: String): Long = runBlocking { books.create(name) }

    protected fun seedCatalog(name: String, defaultUnit: Unit = Unit.G): Long =
        runBlocking { catalog.upsert(name, emptyList(), defaultUnit) }

    protected fun seedRecipe(
        bookId: Long,
        name: String,
        yieldAmount: Double = 2.0,
        yieldItem: String = "item",
    ): Long = runBlocking { recipes.create(bookId, name, yieldAmount, yieldItem) }

    protected fun seedProduct(
        name: String,
        pack: PackSize,
        price: Double,
        source: String = "manual",
    ): Long = runBlocking { products.upsert(name, pack, price, source, now) }

    protected fun seedPantryEntry(ingredientId: Long, productId: Long) {
        runBlocking { pantry.add(ingredientId, productId) }
    }

    protected fun addIngredientLine(
        recipeId: Long,
        ingredientId: Long,
        amount: Double,
        unit: Unit,
        productId: Long? = null,
    ) {
        runBlocking {
            val recipe = recipes.get(recipeId) ?: error("recipe $recipeId not found")
            val ingredient = catalog.byId(ingredientId) ?: error("ingredient $ingredientId not found")
            val product = productId?.let { products.byId(it) }
            val line = IngredientLine(
                id = 0,
                recipeId = recipeId,
                ingredientId = ingredientId,
                ingredientName = ingredient.name,
                productId = productId,
                productName = product?.name,
                amount = amount,
                unit = unit,
                sortOrder = recipe.ingredients.size,
                productPackSize = product?.packSize,
                productPrice = product?.price,
            )
            recipes.update(recipe.copy(ingredients = recipe.ingredients + line))
        }
    }

    protected fun priceResult(
        store: String,
        productName: String,
        price: Double,
        pack: PackSize? = null,
        fetchedAt: Long = now,
    ): PriceResult = PriceResult(store, productName, price, pack, null, fetchedAt)

    private class FakePriceSource(
        override val store: String,
        private val results: List<PriceResult>,
    ) : PriceSource {
        override suspend fun search(query: String): List<PriceResult> {
            val normalized = query.trim().lowercase()
            if (normalized.isEmpty()) return results
            return results.filter { it.productName.lowercase().contains(normalized) }
        }
    }

    private object NoopPriceFetcher : PriceFetcher {
        override suspend fun get(url: String, headers: Map<String, String>): String =
            throw IOException("network disabled in tests")

        override suspend fun post(url: String, body: String, headers: Map<String, String>): String =
            throw IOException("network disabled in tests")
    }
}
