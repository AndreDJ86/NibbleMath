package com.loopworks.nibblemath.network

import com.loopworks.nibblemath.core.units.PackSize

data class PriceResult(
    val store: String,
    val productName: String,
    val price: Double,
    val packSize: PackSize?,
    val url: String? = null,
    val fetchedAt: Long,
)

enum class StoreStatus {
    UNKNOWN,
    UP,
    DEGRADED,
    DOWN,
}
