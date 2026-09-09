package com.loopworks.nibblemath.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookRepositoryTest : DataTestBase() {
    @Test
    fun createAndListBooks() = runBlocking {
        val first = books.create("My Cookbook")
        val second = books.create("Baking")
        val all = books.books()
        assertEquals(2, all.size)
        assertEquals("My Cookbook", all.first { it.id == first }.name)
        assertEquals(0, all.first { it.id == first }.sortOrder)
        assertEquals(1, all.first { it.id == second }.sortOrder)
    }

    @Test
    fun renameBook() = runBlocking {
        val id = books.create("Old Name")
        books.rename(id, "New Name")
        assertEquals("New Name", books.books().single().name)
    }

    @Test
    fun deleteBookCascadesRecipes() = runBlocking {
        val bookId = books.create("Book")
        val recipeId = recipes.create(bookId, "Soup", 4.0, "servings")
        books.delete(bookId)
        assertEquals(0, books.books().size)
        assertNull(recipes.get(recipeId))
    }
}
