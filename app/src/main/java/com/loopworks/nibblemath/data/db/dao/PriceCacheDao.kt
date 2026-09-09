package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.PriceCacheEntity

@Dao
interface PriceCacheDao {
    @Query("SELECT * FROM price_cache WHERE store = :store AND productName = :productName LIMIT 1")
    suspend fun byStoreAndProduct(store: String, productName: String): PriceCacheEntity?

    @Query("SELECT * FROM price_cache WHERE store = :store ORDER BY fetchedAt DESC")
    fun byStore(store: String): List<PriceCacheEntity>

    @Query("SELECT * FROM price_cache ORDER BY store, productName")
    fun all(): List<PriceCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PriceCacheEntity): Long

    @Insert
    suspend fun insertAll(entries: List<PriceCacheEntity>)

    @Query("DELETE FROM price_cache WHERE store = :store")
    suspend fun deleteByStore(store: String)
}
