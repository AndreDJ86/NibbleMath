package com.loopworks.nibblemath.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A specific branded product with a pack size and price (e.g. "DairyLO Whole
 * 1L"). [source] is "manual" or a store name; [fetchedAt] is epoch millis for
 * store-sourced products, null for manual.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packAmount: Double,
    val packUnit: String,
    val price: Double,
    val source: String = "manual",
    val fetchedAt: Long? = null,
)
