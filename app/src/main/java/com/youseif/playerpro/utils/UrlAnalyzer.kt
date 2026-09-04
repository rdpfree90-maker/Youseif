package com.youseif.playerpro.utils

import com.youseif.playerpro.data.model.SourceType
import java.net.URI
import java.net.URL
import java.util.Locale

/**
 * Universal URL classifier — not limited to a fixed list of websites.
 *
 * Strategy:
 *  1) Instant local signals (extension, generic path shapes)
 *  2) Fast HEAD probe for Content-Type on ambiguous / download-like links
 *  3) Any HTML page opens in WebView (works for unlimited sites with their own players)
 */
object UrlAnalyzer {

    private val videoExtensions = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "flv", "m4v", "ts", "m3u8", "mpd", "m3u",
        "3gp", "ogv", "mts", "m2ts", "f4v", "vob", "wmv", "asf", "mpg", "mpeg",
        "mp3", "m4a", "aac", "ogg", "opus", "flac", "wav"
    )

    private val directPathHints = listOf(
        "/api/file", "/api/download", "/api/stream", "/api/media",
        "/file/", "/files/", "/download/", "/downloads/", "/dl/", "/d/",
        "/raw/", "/get/", "/stream/", "/media/", "/video/", "/videos/",
        "/content/", "/attachment/", "/attachments/", "/storage/",
        "/upload/", "/uploads/", "/bucket/", "/object/", "/objects/",
        "/direct/", "/redir/download"
    )

    private val manifestHints = listOf(
        ".m3u8", "playlist.m3u8", "index.m3u8", "master.m3u8",
        ".mpd", "manifest.mpd", "stream.mpd"
    )

    private val embedPathHints = listOf(
        "/embed", "/embed/", "/e/", "/player/", "/iframe"
    )

    fun isValidUrl(raw: String): Boolean {
        val url = raw.trim()
        if (url.isBlank()) return false
        return try {
            val u = URL(if (!url.contains("://")) "https://$url" else url)
            u.protocol in listOf("http", "https", "rtsp", "rtsps", "file")
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeUrl(raw: String): String {
        val url = raw.trim()
        if (url.isBlank()) return url
        return if (!url.contains("://")) "https://$url" else url
    }

    fun resolveCanonicalMediaUrl(raw: String): String {
        val normalized = normalizeUrl(raw)
        return try {
            val uri = URI(normalized)
            val host = uri.host?.lowercase(Locale.US) ?: return normalized
            val path = uri.path ?: ""
            if (host.contains("pixeldrain")) {
                val id = Regex("""/(?:u|file)/([A-Za-z0-9_-]+)""").find(path)?.groupValues?.getOrNull(1)
                if (id != null && !path.contains("/api/file")) {
                    return "https://$host/api/file/$id"
                }
            }
            normalized
        } catch (_: Exception) {
            normalized
        }
    }

    fun detectType(raw: String): SourceType {
        val normalized = normalizeUrl(raw)
        val url = normalized.lowercase(Locale.US)
        val path = try {
            URI(normalized).path?.lowercase(Locale.US) ?: ""
        } catch (_: Exception) {
            ""
        }

        if (manifestHints.any { url.contains(it) }) {
            return if (url.contains("mpd")) SourceType.DASH else SourceType.HLS
        }

        val ext = extractExtension(normalized)
        if (ext in videoExtensions) {
            return when (ext) {
                "m3u8", "m3u" -> SourceType.HLS
                "mpd" -> SourceType.DASH
                else -> SourceType.DIRECT_VIDEO
            }
        }
        if (videoExtensions.any {
                url.contains(".$it") || url.contains("%2e$it")
            }
        ) {
            if (url.contains(".m3u8") || url.contains(".m3u")) return SourceType.HLS
            if (url.contains(".mpd")) return SourceType.DASH
            return SourceType.DIRECT_VIDEO
        }

        if (directPathHints.any { path.contains(it) || url.contains(it) }) {
            return SourceType.DIRECT_VIDEO
        }

        if (embedPathHints.any { path.contains(it) || url.contains(it) }) {
            return SourceType.EMBED
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return SourceType.WEB_PAGE
        }
        return SourceType.UNKNOWN
    }

    fun extractExtension(raw: String): String {
        return try {
            val path = URI(normalizeUrl(raw)).path ?: return ""
            val name = path.substringAfterLast('/').substringBefore('?').substringBefore('#')
            val ext = name.substringAfterLast('.', missingDelimiterValue = "")
            if (ext.isNotBlank() && ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) {
                ext.lowercase(Locale.US)
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isDirectMedia(raw: String): Boolean {
        val type = detectType(raw)
        return type == SourceType.DIRECT_VIDEO || type == SourceType.HLS || type == SourceType.DASH
    }

    fun isDirectPlayableMedia(raw: String): Boolean = isDirectMedia(raw)

    fun refineTypeFromHeaders(
        contentType: String?,
        contentDisposition: String?,
        finalUrl: String?
    ): SourceType {
        val ct = contentType?.lowercase(Locale.US)?.substringBefore(';')?.trim().orEmpty()
        val cd = contentDisposition?.lowercase(Locale.US).orEmpty()
        val url = finalUrl?.lowercase(Locale.US).orEmpty()

        when {
            ct.contains("mpegurl") || ct.contains("x-mpegurl") ||
                ct == "application/vnd.apple.mpegurl" -> return SourceType.HLS
            ct.contains("dash+xml") || ct.contains("application/mpd") -> return SourceType.DASH
            ct.startsWith("video/") || ct.startsWith("audio/") -> return SourceType.DIRECT_VIDEO
            ct == "application/octet-stream" || ct == "binary/octet-stream" ||
                ct == "application/force-download" || ct == "application/x-download" ||
                ct == "application/download" -> {
                if (looksLikeMediaFilename(cd) || looksLikeMediaFilename(url) || cd.contains("filename")) {
                    return SourceType.DIRECT_VIDEO
                }
                // Many CDNs send octet-stream for real videos
                return SourceType.DIRECT_VIDEO
            }
            ct.startsWith("text/html") || ct.startsWith("application/xhtml") ->
                return SourceType.WEB_PAGE
            ct.contains("json") -> return SourceType.WEB_PAGE
        }

        if (!finalUrl.isNullOrBlank()) {
            val local = detectType(finalUrl)
            if (local != SourceType.WEB_PAGE && local != SourceType.UNKNOWN) return local
        }
        return SourceType.UNKNOWN
    }

    private fun looksLikeMediaFilename(value: String): Boolean {
        val v = value.lowercase(Locale.US)
        return videoExtensions.any { v.contains(".$it") }
    }

    /** Probe almost everything unclear; also verify download-like paths. */
    fun shouldProbe(type: SourceType): Boolean {
        return type == SourceType.WEB_PAGE ||
            type == SourceType.UNKNOWN ||
            type == SourceType.DIRECT_VIDEO ||
            type == SourceType.EMBED
    }

    fun buildDirectVideoHtml(mediaUrl: String, autoplay: Boolean = false): String {
        val safe = mediaUrl
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
        val ap = if (autoplay) "autoplay" else ""
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
              <style>
                html,body{margin:0;padding:0;background:#000;height:100%;width:100%;overflow:hidden}
                video{width:100%;height:100%;object-fit:contain;background:#000}
              </style>
            </head>
            <body>
              <video id="v" controls playsinline webkit-playsinline preload="metadata" $ap src="$safe"></video>
              <script>
                (function(){
                  var v=document.getElementById('v');
                  v.addEventListener('error', function(){
                    if(!document.getElementById('err')){
                      var d=document.createElement('div');
                      d.id='err';
                      d.style.cssText='color:#fff;text-align:center;padding:24px;font-family:sans-serif;position:fixed;inset:0;display:flex;align-items:center;justify-content:center;background:#000';
                      d.textContent='Playback error — try Open in Browser';
                      document.body.appendChild(d);
                    }
                  });
                })();
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
