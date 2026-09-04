package com.youseif.playerpro.data.m3u

import com.youseif.playerpro.data.model.M3uParseResult
import com.youseif.playerpro.data.model.Source
import java.net.URL
import java.util.regex.Pattern

/**
 * Flexible M3U / M3U8 parser.
 * Tolerates malformed entries; skips invalid ones and reports errors.
 */
object M3uParser {

    private val attributePattern = Pattern.compile("""([\w-]+)="([^"]*)"""")

    fun parse(content: String): M3uParseResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val sources = mutableListOf<Source>()
        val errors = mutableListOf<String>()
        var total = 0
        var pendingName = "Unknown"
        var pendingLogo = ""
        var pendingCategory = ""
        var pendingUserAgent = ""
        var pendingReferer = ""
        var pendingHeaders = ""

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    // header – ignore
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    total++
                    pendingName = "Unknown"
                    pendingLogo = ""
                    pendingCategory = ""
                    pendingUserAgent = ""
                    pendingReferer = ""
                    pendingHeaders = ""

                    try {
                        val attrs = parseAttributes(line)
                        pendingLogo = attrs["tvg-logo"] ?: attrs["logo"] ?: ""
                        pendingCategory = attrs["group-title"] ?: attrs["group"] ?: ""
                        pendingName = attrs["tvg-name"]
                            ?: line.substringAfterLast(",").trim().ifBlank { "Channel $total" }
                        pendingUserAgent = attrs["user-agent"] ?: attrs["http-user-agent"] ?: ""
                        pendingReferer = attrs["referer"] ?: attrs["http-referrer"] ?: ""
                    } catch (e: Exception) {
                        errors.add("EXTINF parse error at entry $total: ${e.message}")
                    }
                }
                line.startsWith("#EXTVLCOPT", ignoreCase = true) ||
                    line.startsWith("#EXT-X-", ignoreCase = true) ||
                    line.startsWith("#", ignoreCase = true) -> {
                    // optional directives
                    if (line.contains("user-agent", ignoreCase = true)) {
                        pendingUserAgent = line.substringAfter("=").trim().removeSurrounding("\"")
                    }
                    if (line.contains("referer", ignoreCase = true) || line.contains("http-referrer", ignoreCase = true)) {
                        pendingReferer = line.substringAfter("=").trim().removeSurrounding("\"")
                    }
                }
                else -> {
                    // URL line
                    val url = line.trim()
                    if (isValidUrl(url)) {
                        sources.add(
                            Source(
                                name = pendingName.ifBlank { "Channel ${sources.size + 1}" },
                                url = url,
                                category = pendingCategory,
                                logo = pendingLogo,
                                userAgent = pendingUserAgent,
                                referer = pendingReferer,
                                headersJson = pendingHeaders
                            )
                        )
                    } else {
                        if (total > 0) {
                            errors.add("Invalid URL at entry $total: $url")
                        }
                    }
                    // reset pending
                    pendingName = "Unknown"
                    pendingLogo = ""
                    pendingCategory = ""
                    pendingUserAgent = ""
                    pendingReferer = ""
                    pendingHeaders = ""
                }
            }
            i++
        }

        return M3uParseResult(
            sources = sources,
            errors = errors,
            totalEntries = total.coerceAtLeast(sources.size),
            validEntries = sources.size
        )
    }

    fun export(sources: List<Source>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sources.forEach { s ->
            val attrs = buildString {
                if (s.logo.isNotBlank()) append(" tvg-logo=\"${escape(s.logo)}\"")
                if (s.category.isNotBlank()) append(" group-title=\"${escape(s.category)}\"")
                if (s.userAgent.isNotBlank()) append(" user-agent=\"${escape(s.userAgent)}\"")
                if (s.referer.isNotBlank()) append(" referer=\"${escape(s.referer)}\"")
            }
            sb.appendLine("#EXTINF:-1$attrs,${s.name}")
            if (s.userAgent.isNotBlank()) {
                sb.appendLine("#EXTVLCOPT:http-user-agent=${s.userAgent}")
            }
            if (s.referer.isNotBlank()) {
                sb.appendLine("#EXTVLCOPT:http-referrer=${s.referer}")
            }
            sb.appendLine(s.url)
        }
        return sb.toString()
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val matcher = attributePattern.matcher(line)
        while (matcher.find()) {
            map[matcher.group(1)!!.lowercase()] = matcher.group(2) ?: ""
        }
        return map
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val u = URL(url)
            u.protocol == "http" || u.protocol == "https" ||
                u.protocol == "rtsp" || u.protocol == "rtsps" ||
                url.startsWith("rtp://") || url.contains(".m3u8") ||
                url.contains(".mpd") || url.contains(".mp4")
        } catch (_: Exception) {
            // relative or special schemes – accept if looks like path
            url.startsWith("/") || url.contains(".")
        }
    }

    private fun escape(s: String): String = s.replace("\"", "'")
}
