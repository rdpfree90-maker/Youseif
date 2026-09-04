package com.youseif.playerpro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val category: String = "",
    val description: String = "",
    val logo: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val headersJson: String = "",
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SourceType {
    DIRECT_VIDEO,
    HLS,
    DASH,
    WEB_PAGE,
    EMBED,
    PLAYLIST,
    UNKNOWN
}

data class PlaybackInfo(
    val resolution: String = "Unknown",
    val videoCodec: String = "Unknown",
    val audioCodec: String = "Unknown",
    val networkSpeed: String = "Unknown",
    val bufferStatus: String = "Unknown",
    val streamingType: String = "Unknown",
    val protocol: String = "Unknown",
    val currentUrl: String = "",
    val userAgent: String = "",
    val connectionStatus: String = "Unknown",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
)

sealed class PlayerError {
    data class InvalidUrl(val message: String = "Invalid URL") : PlayerError()
    data class NetworkError(val message: String = "Network Error") : PlayerError()
    data class PlaybackError(val message: String = "Playback Error") : PlayerError()
    data class UnsupportedSource(val message: String = "Unsupported Source") : PlayerError()
    data class SslError(val message: String = "SSL Error") : PlayerError()
    data class Timeout(val message: String = "Timeout") : PlayerError()
    data class HttpError(val code: Int, val message: String = "HTTP Error") : PlayerError()
    data class Generic(val message: String) : PlayerError()
}

enum class PlayerState {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    BUFFERING,
    ENDED,
    ERROR
}

data class M3uParseResult(
    val sources: List<Source>,
    val errors: List<String>,
    val totalEntries: Int,
    val validEntries: Int
)
