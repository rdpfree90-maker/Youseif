package com.youseif.playerpro.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object NetworkFetcher {

    data class FetchResult(
        val success: Boolean,
        val body: String = "",
        val error: String? = null,
        val httpCode: Int = 0
    )

    data class HeadResult(
        val success: Boolean,
        val contentType: String? = null,
        val contentDisposition: String? = null,
        val finalUrl: String? = null,
        val contentLength: Long = -1L,
        val httpCode: Int = 0,
        val error: String? = null
    )

    suspend fun fetchText(
        urlString: String,
        timeoutMs: Int = 20000,
        userAgent: String = "YouseifPlayerPro/1.0"
    ): FetchResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val normalized = if (!urlString.contains("://")) "https://$urlString" else urlString
            val url = URL(normalized)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "*/*")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
            } ?: ""
            if (code in 200..299) {
                FetchResult(success = true, body = body, httpCode = code)
            } else {
                FetchResult(success = false, body = body, error = "HTTP $code", httpCode = code)
            }
        } catch (e: Exception) {
            FetchResult(success = false, error = e.message ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Fast HEAD (falls back to GET range) to learn Content-Type without downloading the file.
     * Timeout kept low so playback path stays snappy.
     */
    suspend fun probeHead(
        urlString: String,
        timeoutMs: Int = 6000,
        userAgent: String = SettingsUa
    ): HeadResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val normalized = if (!urlString.contains("://")) "https://$urlString" else urlString
            conn = open(normalized, "HEAD", timeoutMs, userAgent)
            var code = conn.responseCode
            // Some CDNs reject HEAD → tiny ranged GET
            if (code == HttpURLConnection.HTTP_BAD_METHOD || code == 403 || code == 405) {
                conn.disconnect()
                conn = open(normalized, "GET", timeoutMs, userAgent).apply {
                    setRequestProperty("Range", "bytes=0-0")
                }
                code = conn.responseCode
            }
            val type = conn.contentType
            val disposition = conn.getHeaderField("Content-Disposition")
            val length = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            val finalUrl = conn.url?.toString()
            // Drain tiny body if any so connection can close cleanly
            try {
                (if (code in 200..299 || code == 206) conn.inputStream else conn.errorStream)
                    ?.close()
            } catch (_: Exception) {
            }
            HeadResult(
                success = code in 200..299 || code == 206,
                contentType = type,
                contentDisposition = disposition,
                finalUrl = finalUrl,
                contentLength = length,
                httpCode = code
            )
        } catch (e: Exception) {
            HeadResult(success = false, error = e.message ?: "Probe failed")
        } finally {
            conn?.disconnect()
        }
    }

    private fun open(
        urlString: String,
        method: String,
        timeoutMs: Int,
        userAgent: String
    ): HttpURLConnection {
        return (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            requestMethod = method
            setRequestProperty(
                "User-Agent",
                userAgent
            )
            setRequestProperty("Accept", "*/*")
        }
    }

    private const val SettingsUa =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
