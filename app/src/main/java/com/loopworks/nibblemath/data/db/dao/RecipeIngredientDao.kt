package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun byRecipe(recipeId: Long): List<RecipeIngredientEntity>

    @Query("SELECT * FROM recipe_ingredients ORDER BY recipeId, sortOrder")
    fun all(): List<RecipeIngredientEntity>

    @Insert
    suspend fun insertAll(items: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteByRecipe(recipeId: Long)
}
