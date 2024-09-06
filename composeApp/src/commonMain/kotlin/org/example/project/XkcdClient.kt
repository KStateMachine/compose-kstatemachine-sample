package org.example.project

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.Serializable
import io.ktor.serialization.kotlinx.json.*

class XkcdClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    @Serializable
    internal data class XkcdResponse(
        val month: String,
        val num: Int,
        val link: String,
        val year: String,
        val news: String,
        val safe_title: String,
        val transcript: String,
        val alt: String,
        val img: String,
        val title: String,
        val day: String
    )

    suspend fun getCurrentXkcdImageUrl(): String? {
        return try {
            // The URL of the current XKCD comic
            val response: XkcdResponse = client.get("https://xkcd.com/info.0.json").body()
            response.img
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        client.close()
    }
}
