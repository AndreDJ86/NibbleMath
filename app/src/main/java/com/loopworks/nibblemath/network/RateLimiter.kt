package com.loopworks.nibblemath.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val minIntervalMillis: Long = 1_000L,
) {
    private val mutex = Mutex()
    private var nextAllowedAt = 0L

    suspend fun <T> withLimit(block: suspend () -> T): T = mutex.withLock {
        val now = clock()
        if (now < nextAllowedAt) {
            delay(nextAllowedAt - now)
        }
        nextAllowedAt = clock() + minIntervalMillis
        block()
    }
}
