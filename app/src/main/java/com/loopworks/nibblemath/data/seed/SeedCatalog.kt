package com.loopworks.nibblemath.data.seed

import android.content.Context
import com.loopworks.nibblemath.data.db.entity.IngredientEntity
import org.json.JSONArray
import org.json.JSONObject

object SeedCatalog {
    private const val ASSET = "seed/ingredients.json"

    fun load(context: Context): List<IngredientEntity> {
        val raw = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            IngredientEntity(
                name = item.getString("name"),
                aliases = stringList(item, "aliases"),
                defaultUnit = item.optString("unit", "g"),
                isSeed = true,
            )
        }
    }

    private fun stringList(item: JSONObject, key: String): List<String> {
        if (!item.has(key)) return emptyList()
        val array = item.getJSONArray(key)
        return List(array.length()) { array.getString(it) }
    }
}
