package com.loopworks.nibblemath.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical ingredient in the catalog. [aliases] holds alternate names (matched
 * in the repository layer); [defaultUnit] is a unit symbol.
 */
@Entity(
    tableName = "ingredients",
    indices = [Index(value = ["name"], unique = true)],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aliases: List<String> = emptyList(),
    val defaultUnit: String = "g",
    val isSeed: Boolean = true,
)
