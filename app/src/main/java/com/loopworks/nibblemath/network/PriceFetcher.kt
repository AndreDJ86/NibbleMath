package com.loopworks.nibblemath.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface PriceFetcher {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String
    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String
}

class OkHttpPriceFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(InMemoryCookieJar())
        .build(),
) : PriceFetcher {
    override suspend fun get(url: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(url)
                .get()
            headers.forEach { (name, value) -> builder.header(name, value) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} for $url")
                }
                response.body?.string().orEmpty()
            }
        }

    override suspend fun post(url: String, body: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
            headers.forEach { (name, value) -> builder.header(name, value) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} for $url")
                }
                response.body?.string().orEmpty()
            }
        }
}

private class InMemoryCookieJar : CookieJar {
    private val cookiesByHost = mutableMapOf<String, List<Cookie>>()

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookiesByHost[url.host].orEmpty().filter { it.expiresAt > System.currentTimeMillis() }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookiesByHost[url.host] = cookies
    }
}
