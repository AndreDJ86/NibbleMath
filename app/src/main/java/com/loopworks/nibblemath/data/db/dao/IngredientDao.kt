package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.IngredientEntity

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY name")
    fun all(): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun byId(id: Long): IngredientEntity?

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): IngredientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(ingredient: IngredientEntity): Long

    @Insert
    suspend fun insertAll(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun count(): Int
}
