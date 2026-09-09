package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun all(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE :pattern ORDER BY name")
    fun search(pattern: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long

    @Insert
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
