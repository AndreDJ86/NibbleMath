package com.loopworks.nibblemath.network

internal object TestFixtures {
    fun load(name: String): String =
        javaClass.getResource("/price-fixtures/$name")?.readText()
            ?: error("Missing test fixture: $name")
}
