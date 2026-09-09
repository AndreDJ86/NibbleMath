package com.loopworks.nibblemath.network

interface PriceSource {
    val store: String

    suspend fun search(query: String): List<PriceResult>
}
