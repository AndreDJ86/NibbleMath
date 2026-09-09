package com.loopworks.nibblemath.network

import com.loopworks.nibblemath.core.units.PackSize
import com.loopworks.nibblemath.core.units.Unit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PriceParserTest {
    @Test
    fun parsesJsonProductArray() {
        val body = TestFixtures.load("products.json")
        val results = PriceParser.parse(body, "TestStore", now = 123L)

        assertEquals(3, results.size)
        assertEquals("TestStore", results.first().store)
        assertEquals(123L, results.first().fetchedAt)

        val milk = results.first { it.productName == "Test Whole Milk 1L" }
        assertEquals(3.50, milk.price)
        assertEquals(PackSize(1.0, Unit.L), milk.packSize)
        assertEquals("https://example.com/milk", milk.url)

        val bread = results.first { it.productName == "Test Bread 800g" }
        assertEquals(2.80, bread.price)
        assertEquals(PackSize(800.0, Unit.G), bread.packSize)

        val eggs = results.first { it.productName == "Test Eggs 12" }
        assertEquals(6.00, eggs.price)
        assertEquals(PackSize(144.0, Unit.EACH), eggs.packSize)
    }

    @Test
    fun parsesWoolworthsJson() {
        val body = TestFixtures.load("woolworths.json")
        val results = PriceParser.parse(body, "Woolworths", now = 123L)

        assertEquals(2, results.size)
        val milk = results.first { it.productName == "Woolworths Full Cream Milk 3L" }
        assertEquals("Woolworths", milk.store)
        assertEquals(4.95, milk.price)
        assertEquals(PackSize(3.0, Unit.L), milk.packSize)
        assertEquals(123L, milk.fetchedAt)

        val lactoseFree = results.first { it.productName == "Woolworths Lactose Free Milk 2L" }
        assertEquals(2.20, lactoseFree.price)
        assertEquals(PackSize(2.0, Unit.L), lactoseFree.packSize)
    }

    @Test
    fun parsesColesJson() {
        val body = TestFixtures.load("coles.json")
        val results = PriceParser.parse(body, "Coles", now = 123L)

        assertEquals(2, results.size)
        val milk = results.first { it.productName == "Full Cream Milk" }
        assertEquals("Coles", milk.store)
        assertEquals(4.95, milk.price)
        assertEquals(PackSize(3.0, Unit.L), milk.packSize)
        assertEquals(123L, milk.fetchedAt)

        val lactoseFree = results.first { it.productName == "Lactose Free Milk" }
        assertEquals(3.10, lactoseFree.price)
        assertEquals(PackSize(2.0, Unit.L), lactoseFree.packSize)
    }

    @Test
    fun parsesAldiJson() {
        val body = TestFixtures.load("aldi.json")
        val results = PriceParser.parse(body, "ALDI", now = 123L)

        assertEquals(3, results.size)
        val milk = results.first { it.productName == "Full Cream Milk 3L" }
        assertEquals("ALDI", milk.store)
        assertEquals(4.95, milk.price)
        assertEquals(PackSize(3.0, Unit.L), milk.packSize)
        assertEquals(
            "https://www.aldi.com.au/product/full-cream-milk-3l-000000000111111001",
            milk.url,
        )
        assertEquals(123L, milk.fetchedAt)

        val lactoseFree = results.first { it.productName == "Lactose Free Milk 2L" }
        assertEquals(3.10, lactoseFree.price)
        assertEquals(PackSize(2.0, Unit.L), lactoseFree.packSize)

        val wholeMilk = results.first { it.productName == "Whole Milk 3L" }
        assertEquals(4.95, wholeMilk.price)
        assertEquals(PackSize(3.0, Unit.L), wholeMilk.packSize)
    }

    @Test
    fun parsesEmbeddedHtmlJson() {
        val body = TestFixtures.load("embedded.html")
        val results = PriceParser.parse(body, "TestStore", now = 1L)

        val embedded = results.first { it.productName == "Embedded Milk 1L" }
        assertEquals(4.10, embedded.price)
        assertEquals(PackSize(1.0, Unit.L), embedded.packSize)

        val next = results.first { it.productName == "Next Bread 700g" }
        assertEquals(3.20, next.price)
        assertEquals(PackSize(700.0, Unit.G), next.packSize)
    }

    @Test
    fun malformedBodyReturnsEmpty() {
        assertTrue(PriceParser.parse("<html>not json</html>", "TestStore").isEmpty())
        assertTrue(PriceParser.parse("{invalid", "TestStore").isEmpty())
    }

    @Test
    fun ambiguousPackBecomesNull() {
        val body = """{"products":[{"name":"Mystery","price":"$1.00","packSize":"family size"}]}"""
        val result = PriceParser.parse(body, "TestStore").single()
        assertNull(result.packSize)
    }

    @Test
    fun promotionalPriceTextIsRejected() {
        val body = """{"products":[{"name":"Mystery","price":"3 for $5.00","packSize":"1 L"}]}"""
        assertTrue(PriceParser.parse(body, "TestStore").isEmpty())
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val body = """{"products":[{"name":"Known","price":1.25,"packSize":"1 L","analytics":{"id":"x"}}]}"""
        val result = PriceParser.parse(body, "TestStore").single()
        assertNotNull(result.packSize)
        assertEquals(1.25, result.price)
    }
}
