package com.loopworks.nibblemath.data.repository

import com.loopworks.nibblemath.data.db.dao.BookDao
import com.loopworks.nibblemath.data.db.dao.RecipeDao
import com.loopworks.nibblemath.data.db.entity.BookEntity
import com.loopworks.nibblemath.data.model.Book
import com.loopworks.nibblemath.data.model.RecipeSummary
import com.loopworks.nibblemath.data.model.toModel
import com.loopworks.nibblemath.data.model.toSummary

class BookRepository(
    private val bookDao: BookDao,
    private val recipeDao: RecipeDao,
) {
    fun books(): List<Book> = bookDao.all().map { it.toModel() }

    fun recipes(bookId: Long): List<RecipeSummary> = recipeDao.byBook(bookId).map { it.toSummary() }

    suspend fun create(name: String): Long =
        bookDao.upsert(BookEntity(name = name, sortOrder = bookDao.all().size))

    suspend fun rename(id: Long, name: String) {
        val existing = bookDao.byId(id) ?: return
        bookDao.upsert(existing.copy(name = name))
    }

    suspend fun delete(id: Long) = bookDao.deleteById(id)
}
