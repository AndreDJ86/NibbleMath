package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.RecipeEntity

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY sortOrder, name")
    fun all(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE bookId = :bookId ORDER BY sortOrder, name")
    fun byBook(bookId: Long): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun byId(id: Long): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: RecipeEntity): Long

    @Insert
    suspend fun insertAll(recipes: List<RecipeEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
