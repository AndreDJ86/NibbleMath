package com.loopworks.nibblemath.data

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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

/**
 * Base for data-layer tests: an in-memory Room database on the JVM via
 * Robolectric, pinned to SDK 35 (compileSdk 37 is not yet supported).
 */
@Config(sdk = [35])
abstract class DataTestBase {
    protected lateinit var context: Context
    protected lateinit var database: NibbleMathDatabase
    protected lateinit var books: BookRepository
    protected lateinit var recipes: RecipeRepository
    protected lateinit var catalog: CatalogRepository
    protected lateinit var products: ProductRepository
    protected lateinit var pantry: PantryRepository
    protected lateinit var priceCache: PriceCacheRepository
    protected lateinit var backup: BackupManager

    @BeforeTest
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
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
        backup = BackupManager(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }
}
