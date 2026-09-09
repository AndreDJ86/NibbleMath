package com.loopworks.nibblemath.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loopworks.nibblemath.data.db.entity.PantryEntryEntity

@Dao
interface PantryEntryDao {
    @Query("SELECT * FROM pantry_entries ORDER BY sortOrder")
    fun all(): List<PantryEntryEntity>

    @Query("SELECT * FROM pantry_entries WHERE id = :id LIMIT 1")
    fun byId(id: Long): PantryEntryEntity?

    @Query("SELECT * FROM pantry_entries WHERE ingredientId = :ingredientId ORDER BY sortOrder")
    fun byIngredient(ingredientId: Long): List<PantryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PantryEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<PantryEntryEntity>)

    @Query("DELETE FROM pantry_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
