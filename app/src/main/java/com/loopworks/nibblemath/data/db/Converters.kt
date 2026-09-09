package com.loopworks.nibblemath.data.db

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromAliases(aliases: List<String>): String =
        JSONArray(aliases.toTypedArray()).toString()

    @TypeConverter
    fun toAliases(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { array.getString(it) }
    }
}
