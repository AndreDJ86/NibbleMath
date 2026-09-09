package com.loopworks.nibblemath.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached online price lookup (store, product, price, fetchedAt). */
@Entity(
    tableName = "price_cache",
    indices = [Index(value = ["store", "productName"], unique = true)],
)
data class PriceCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val store: String,
    val query: String,
    val productName: String,
    val packAmount: Double,
    val packUnit: String,
    val price: Double,
    val fetchedAt: Long,
)
