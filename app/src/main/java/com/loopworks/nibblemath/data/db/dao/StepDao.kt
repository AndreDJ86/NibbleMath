package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.StepEntity

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun byRecipe(recipeId: Long): List<StepEntity>

    @Query("SELECT * FROM steps ORDER BY recipeId, sortOrder")
    fun all(): List<StepEntity>

    @Insert
    suspend fun insertAll(steps: List<StepEntity>)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteByRecipe(recipeId: Long)
}
