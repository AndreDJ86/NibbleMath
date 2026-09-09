package com.loopworks.nibblemath.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loopworks.nibblemath.data.db.dao.BookDao
import com.loopworks.nibblemath.data.db.dao.IngredientDao
import com.loopworks.nibblemath.data.db.dao.PantryEntryDao
import com.loopworks.nibblemath.data.db.dao.PriceCacheDao
import com.loopworks.nibblemath.data.db.dao.ProductDao
import com.loopworks.nibblemath.data.db.dao.RecipeDao
import com.loopworks.nibblemath.data.db.dao.RecipeIngredientDao
import com.loopworks.nibblemath.data.db.dao.StepDao
import com.loopworks.nibblemath.data.db.entity.BookEntity
import com.loopworks.nibblemath.data.db.entity.IngredientEntity
import com.loopworks.nibblemath.data.db.entity.PantryEntryEntity
import com.loopworks.nibblemath.data.db.entity.PriceCacheEntity
import com.loopworks.nibblemath.data.db.entity.ProductEntity
import com.loopworks.nibblemath.data.db.entity.RecipeEntity
import com.loopworks.nibblemath.data.db.entity.RecipeIngredientEntity
import com.loopworks.nibblemath.data.db.entity.StepEntity

@Database(
    entities = [
        BookEntity::class,
        RecipeEntity::class,
        StepEntity::class,
        IngredientEntity::class,
        ProductEntity::class,
        RecipeIngredientEntity::class,
        PantryEntryEntity::class,
        PriceCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NibbleMathDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun recipeDao(): RecipeDao
    abstract fun stepDao(): StepDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun productDao(): ProductDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun pantryEntryDao(): PantryEntryDao
    abstract fun priceCacheDao(): PriceCacheDao

    companion object {
        const val NAME = "nibblemath.db"
    }
}
