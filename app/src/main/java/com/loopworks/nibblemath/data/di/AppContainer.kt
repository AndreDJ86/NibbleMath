package com.loopworks.nibblemath.data.di

import android.content.Context
import androidx.room.Room
import com.loopworks.nibblemath.data.backup.BackupManager
import com.loopworks.nibblemath.data.db.NibbleMathDatabase
import com.loopworks.nibblemath.data.repository.BookRepository
import com.loopworks.nibblemath.data.repository.CatalogRepository
import com.loopworks.nibblemath.data.repository.PantryRepository
import com.loopworks.nibblemath.data.repository.PriceCacheRepository
import com.loopworks.nibblemath.data.repository.ProductRepository
import com.loopworks.nibblemath.data.repository.RecipeRepository
import com.loopworks.nibblemath.data.settings.SettingsRepository
import com.loopworks.nibblemath.network.AldiAdapter
import com.loopworks.nibblemath.network.ColesAdapter
import com.loopworks.nibblemath.network.OkHttpPriceFetcher
import com.loopworks.nibblemath.network.PriceFetcher
import com.loopworks.nibblemath.network.PriceLookupClient
import com.loopworks.nibblemath.network.PriceSource
import com.loopworks.nibblemath.network.RateLimiter
import com.loopworks.nibblemath.network.WoolworthsAdapter

/**
 * Manual dependency graph for the data layer. Constructed once (e.g. in the
 * Application class) and passed down; no DI framework.
 */
class AppContainer internal constructor(private val deps: Dependencies) {
    val database: NibbleMathDatabase get() = deps.database
    val bookRepository: BookRepository get() = deps.bookRepository
    val recipeRepository: RecipeRepository get() = deps.recipeRepository
    val catalogRepository: CatalogRepository get() = deps.catalogRepository
    val productRepository: ProductRepository get() = deps.productRepository
    val pantryRepository: PantryRepository get() = deps.pantryRepository
    val priceCacheRepository: PriceCacheRepository get() = deps.priceCacheRepository
    val backupManager: BackupManager get() = deps.backupManager
    val settingsRepository: SettingsRepository get() = deps.settingsRepository
    val priceFetcher: PriceFetcher get() = deps.priceFetcher
    val priceRateLimiter: RateLimiter get() = deps.priceRateLimiter
    val woolworthsSource: PriceSource get() = deps.woolworthsSource
    val colesSource: PriceSource get() = deps.colesSource
    val aldiSource: PriceSource get() = deps.aldiSource
    val priceLookupClient: PriceLookupClient get() = deps.priceLookupClient

    constructor(context: Context) : this(createDependencies(context))

    internal data class Dependencies(
        val database: NibbleMathDatabase,
        val bookRepository: BookRepository,
        val recipeRepository: RecipeRepository,
        val catalogRepository: CatalogRepository,
        val productRepository: ProductRepository,
        val pantryRepository: PantryRepository,
        val priceCacheRepository: PriceCacheRepository,
        val backupManager: BackupManager,
        val settingsRepository: SettingsRepository,
        val priceFetcher: PriceFetcher,
        val priceRateLimiter: RateLimiter,
        val woolworthsSource: PriceSource,
        val colesSource: PriceSource,
        val aldiSource: PriceSource,
        val priceLookupClient: PriceLookupClient,
    )

    private companion object {
        fun createDependencies(context: Context): Dependencies {
            val appContext = context.applicationContext
            val database = Room.databaseBuilder(
                appContext,
                NibbleMathDatabase::class.java,
                NibbleMathDatabase.NAME,
            ).build()
            val priceFetcher = OkHttpPriceFetcher()
            val woolworthsSource = WoolworthsAdapter(priceFetcher)
            val colesSource = ColesAdapter(priceFetcher)
            val aldiSource = AldiAdapter(priceFetcher)
            val priceRateLimiter = RateLimiter()
            return Dependencies(
                database = database,
                bookRepository = BookRepository(database.bookDao(), database.recipeDao()),
                recipeRepository = RecipeRepository(
                    database.recipeDao(),
                    database.stepDao(),
                    database.recipeIngredientDao(),
                    database.ingredientDao(),
                    database.productDao(),
                ),
                catalogRepository = CatalogRepository(database.ingredientDao()),
                productRepository = ProductRepository(database.productDao()),
                pantryRepository = PantryRepository(
                    database.pantryEntryDao(),
                    database.ingredientDao(),
                    database.productDao(),
                ),
                priceCacheRepository = PriceCacheRepository(database.priceCacheDao()),
                backupManager = BackupManager(database),
                settingsRepository = SettingsRepository(appContext),
                priceFetcher = priceFetcher,
                priceRateLimiter = priceRateLimiter,
                woolworthsSource = woolworthsSource,
                colesSource = colesSource,
                aldiSource = aldiSource,
                priceLookupClient = PriceLookupClient(
                    priceCache = PriceCacheRepository(database.priceCacheDao()),
                    sources = listOf(woolworthsSource, colesSource, aldiSource),
                    rateLimiter = priceRateLimiter,
                ),
            )
        }
    }
}
