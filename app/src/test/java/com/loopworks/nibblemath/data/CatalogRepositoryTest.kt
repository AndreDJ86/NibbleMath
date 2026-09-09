package com.loopworks.nibblemath.data

import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogRepositoryTest : DataTestBase() {
    @Test
    fun seedIfEmptyLoadsBundledCatalog() = runBlocking {
        catalog.seedIfEmpty(context)
        val all = catalog.all()
        assertTrue(all.size >= 20, "expected at least 20 seeded ingredients, got ${all.size}")
        assertTrue(all.all { it.isSeed })
    }

    @Test
    fun seedIfEmptyIsIdempotent() = runBlocking {
        catalog.seedIfEmpty(context)
        val firstCount = catalog.all().size
        catalog.seedIfEmpty(context)
        assertEquals(firstCount, catalog.all().size)
    }

    @Test
    fun findByAlias() = runBlocking {
        catalog.upsert("Whole milk", listOf("milk", "full cream milk"), Unit.ML)
        assertEquals("Whole milk", catalog.find("Whole milk")!!.name)
        assertEquals("Whole milk", catalog.find("milk")!!.name)
        assertEquals("Whole milk", catalog.find("MILK")!!.name)
        assertNull(catalog.find("nonexistent"))
    }

    @Test
    fun upsertReplacesByName() = runBlocking {
        catalog.upsert("Butter", emptyList(), Unit.G)
        catalog.upsert("Butter", listOf("unsalted"), Unit.G)
        assertEquals(1, catalog.all().size)
        assertEquals(listOf("unsalted"), catalog.find("Butter")!!.aliases)
    }

    @Test
    fun byIdReturnsIngredient() = runBlocking {
        val id = catalog.upsert("Flour", emptyList(), Unit.G)
        val loaded = catalog.byId(id)
        assertNotNull(loaded)
        assertEquals("Flour", loaded.name)
        assertNull(catalog.byId(999))
    }
}
