package com.youseif.playerpro.webview

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * Secure, minimal bridge. Only exposes read-only state reporting.
 * All control commands go through evaluateJavascript with trusted scripts.
 */
class JsBridge(
    private val onStateUpdate: (VideoState) -> Unit
) {
    data class VideoState(
        val found: Boolean = false,
        val paused: Boolean = true,
        val ended: Boolean = false,
        val currentTime: Double = 0.0,
        val duration: Double = 0.0,
        val volume: Double = 1.0,
        val muted: Boolean = false,
        val playbackRate: Double = 1.0,
        val videoWidth: Int = 0,
        val videoHeight: Int = 0,
        val readyState: Int = 0,
        val networkState: Int = 0,
        val src: String = ""
    )

    @JavascriptInterface
    fun reportState(json: String) {
        try {
            val o = JSONObject(json)
            val state = VideoState(
                found = o.optBoolean("found", false),
                paused = o.optBoolean("paused", true),
                ended = o.optBoolean("ended", false),
                currentTime = o.optDouble("currentTime", 0.0),
                duration = o.optDouble("duration", 0.0),
                volume = o.optDouble("volume", 1.0),
                muted = o.optBoolean("muted", false),
                playbackRate = o.optDouble("playbackRate", 1.0),
                videoWidth = o.optInt("videoWidth", 0),
                videoHeight = o.optInt("videoHeight", 0),
                readyState = o.optInt("readyState", 0),
                networkState = o.optInt("networkState", 0),
                src = o.optString("src", "")
            )
            onStateUpdate(state)
        } catch (_: Exception) {
            // ignore malformed
        }
    }
}
