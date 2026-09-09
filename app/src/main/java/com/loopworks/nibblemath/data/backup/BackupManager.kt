package com.loopworks.nibblemath.data.backup

import androidx.room.withTransaction
import com.loopworks.nibblemath.data.db.NibbleMathDatabase
import com.loopworks.nibblemath.data.db.entity.BookEntity
import com.loopworks.nibblemath.data.db.entity.IngredientEntity
import com.loopworks.nibblemath.data.db.entity.PantryEntryEntity
import com.loopworks.nibblemath.data.db.entity.PriceCacheEntity
import com.loopworks.nibblemath.data.db.entity.ProductEntity
import com.loopworks.nibblemath.data.db.entity.RecipeEntity
import com.loopworks.nibblemath.data.db.entity.RecipeIngredientEntity
import com.loopworks.nibblemath.data.db.entity.StepEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Versioned JSON backup/restore for the whole local database.
 *
 * The backup preserves primary keys so foreign-key relationships survive a
 * round trip. Restore is a full replacement: existing rows are cleared before
 * the imported rows are inserted.
 */
class BackupManager(private val database: NibbleMathDatabase) {
    suspend fun export(): String {
        val books = database.bookDao().all()
        val recipes = database.recipeDao().all()
        val steps = database.stepDao().all()
        val ingredients = database.ingredientDao().all()
        val products = database.productDao().all()
        val lines = database.recipeIngredientDao().all()
        val pantry = database.pantryEntryDao().all()
        val priceCache = database.priceCacheDao().all()

        return JSONObject()
            .put("app", APP_NAME)
            .put("version", SUPPORTED_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("books", JSONArray().apply { books.forEach { put(it.toJson()) } })
            .put("recipes", JSONArray().apply { recipes.forEach { put(it.toJson()) } })
            .put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })
            .put("ingredients", JSONArray().apply { ingredients.forEach { put(it.toJson()) } })
            .put("products", JSONArray().apply { products.forEach { put(it.toJson()) } })
            .put("recipeIngredients", JSONArray().apply { lines.forEach { put(it.toJson()) } })
            .put("pantryEntries", JSONArray().apply { pantry.forEach { put(it.toJson()) } })
            .put("priceCache", JSONArray().apply { priceCache.forEach { put(it.toJson()) } })
            .toString()
    }

    suspend fun restore(json: String) {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Invalid backup JSON", e)
        }

        if (root.has("app") && root.optString("app") != APP_NAME) {
            throw IllegalArgumentException("Not a NibbleMath backup")
        }
        val version = root.optInt("version", -1)
        if (version != SUPPORTED_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val books = parseBooks(root)
        val recipes = parseRecipes(root)
        val steps = parseSteps(root)
        val ingredients = parseIngredients(root)
        val products = parseProducts(root)
        val lines = parseRecipeIngredients(root)
        val pantry = parsePantryEntries(root)
        val priceCache = parsePriceCache(root)

        database.withTransaction {
            database.clearAllTables()
            database.ingredientDao().insertAll(ingredients)
            database.productDao().insertAll(products)
            database.bookDao().insertAll(books)
            database.recipeDao().insertAll(recipes)
            database.stepDao().insertAll(steps)
            database.recipeIngredientDao().insertAll(lines)
            database.pantryEntryDao().insertAll(pantry)
            database.priceCacheDao().insertAll(priceCache)
        }
    }

    private fun parseBooks(root: JSONObject): List<BookEntity> =
        root.getJSONArray("books").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                BookEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    sortOrder = obj.optInt("sortOrder", 0),
                )
            }
        }

    private fun parseRecipes(root: JSONObject): List<RecipeEntity> =
        root.getJSONArray("recipes").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RecipeEntity(
                    id = obj.getLong("id"),
                    bookId = obj.getLong("bookId"),
                    name = obj.getString("name"),
                    yieldAmount = obj.optDouble("yieldAmount", 0.0),
                    yieldItem = obj.optString("yieldItem", ""),
                    notes = obj.optString("notes", ""),
                    sortOrder = obj.optInt("sortOrder", 0),
                )
            }
        }

    private fun parseSteps(root: JSONObject): List<StepEntity> =
        root.getJSONArray("steps").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                StepEntity(
                    id = obj.getLong("id"),
                    recipeId = obj.getLong("recipeId"),
                    text = obj.getString("text"),
                    sortOrder = obj.optInt("sortOrder", 0),
                )
            }
        }

    private fun parseIngredients(root: JSONObject): List<IngredientEntity> =
        root.getJSONArray("ingredients").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val aliases = mutableListOf<String>()
                val aliasArray = obj.optJSONArray("aliases") ?: JSONArray()
                for (j in 0 until aliasArray.length()) aliases += aliasArray.getString(j)
                IngredientEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    aliases = aliases,
                    defaultUnit = obj.optString("defaultUnit", "g"),
                    isSeed = obj.optBoolean("isSeed", true),
                )
            }
        }

    private fun parseProducts(root: JSONObject): List<ProductEntity> =
        root.getJSONArray("products").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ProductEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    packAmount = obj.optDouble("packAmount", 0.0),
                    packUnit = obj.optString("packUnit", "g"),
                    price = obj.optDouble("price", 0.0),
                    source = obj.optString("source", "manual"),
                    fetchedAt = if (obj.isNull("fetchedAt")) null else obj.getLong("fetchedAt"),
                )
            }
        }

    private fun parseRecipeIngredients(root: JSONObject): List<RecipeIngredientEntity> =
        root.getJSONArray("recipeIngredients").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                RecipeIngredientEntity(
                    id = obj.getLong("id"),
                    recipeId = obj.getLong("recipeId"),
                    ingredientId = obj.getLong("ingredientId"),
                    productId = if (obj.isNull("productId")) null else obj.getLong("productId"),
                    amount = obj.optDouble("amount", 0.0),
                    unit = obj.optString("unit", "g"),
                    sortOrder = obj.optInt("sortOrder", 0),
                )
            }
        }

    private fun parsePantryEntries(root: JSONObject): List<PantryEntryEntity> =
        root.getJSONArray("pantryEntries").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PantryEntryEntity(
                    id = obj.getLong("id"),
                    ingredientId = obj.getLong("ingredientId"),
                    productId = obj.getLong("productId"),
                    sortOrder = obj.optInt("sortOrder", 0),
                )
            }
        }

    private fun parsePriceCache(root: JSONObject): List<PriceCacheEntity> =
        root.getJSONArray("priceCache").let { array ->
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PriceCacheEntity(
                    id = obj.getLong("id"),
                    store = obj.getString("store"),
                    query = obj.getString("query"),
                    productName = obj.getString("productName"),
                    packAmount = obj.optDouble("packAmount", 0.0),
                    packUnit = obj.optString("packUnit", "g"),
                    price = obj.optDouble("price", 0.0),
                    fetchedAt = obj.optLong("fetchedAt", 0L),
                )
            }
        }

    private fun BookEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("sortOrder", sortOrder)

    private fun RecipeEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("bookId", bookId)
            .put("name", name)
            .put("yieldAmount", yieldAmount)
            .put("yieldItem", yieldItem)
            .put("notes", notes)
            .put("sortOrder", sortOrder)

    private fun StepEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("recipeId", recipeId)
            .put("text", text)
            .put("sortOrder", sortOrder)

    private fun IngredientEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("aliases", JSONArray().apply { aliases.forEach { put(it) } })
            .put("defaultUnit", defaultUnit)
            .put("isSeed", isSeed)

    private fun ProductEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("packAmount", packAmount)
            .put("packUnit", packUnit)
            .put("price", price)
            .put("source", source)
            .put("fetchedAt", fetchedAt ?: JSONObject.NULL)

    private fun RecipeIngredientEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("recipeId", recipeId)
            .put("ingredientId", ingredientId)
            .put("productId", productId ?: JSONObject.NULL)
            .put("amount", amount)
            .put("unit", unit)
            .put("sortOrder", sortOrder)

    private fun PantryEntryEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("ingredientId", ingredientId)
            .put("productId", productId)
            .put("sortOrder", sortOrder)

    private fun PriceCacheEntity.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("store", store)
            .put("query", query)
            .put("productName", productName)
            .put("packAmount", packAmount)
            .put("packUnit", packUnit)
            .put("price", price)
            .put("fetchedAt", fetchedAt)

    private companion object {
        const val APP_NAME = "nibblemath"
        const val SUPPORTED_VERSION = 1
    }
}
