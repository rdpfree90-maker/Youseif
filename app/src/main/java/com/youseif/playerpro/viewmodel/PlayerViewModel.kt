package com.youseif.playerpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youseif.playerpro.data.model.PlaybackInfo
import com.youseif.playerpro.data.model.PlayerError
import com.youseif.playerpro.data.model.PlayerState
import com.youseif.playerpro.data.model.Source
import com.youseif.playerpro.data.model.SourceType
import com.youseif.playerpro.data.repository.SettingsRepository
import com.youseif.playerpro.data.repository.SourceRepository
import com.youseif.playerpro.utils.NetworkFetcher
import com.youseif.playerpro.utils.UrlAnalyzer
import com.youseif.playerpro.webview.JsBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentUrl: String = "",
    val sourceName: String = "",
    val sourceType: SourceType = SourceType.UNKNOWN,
    val playerState: PlayerState = PlayerState.IDLE,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val error: PlayerError? = null,
    val isFullscreen: Boolean = false,
    val isPip: Boolean = false,
    val isAudioOnly: Boolean = false,
    val isDataSaver: Boolean = false,
    val isControlsLocked: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val showControls: Boolean = true,
    val playbackInfo: PlaybackInfo = PlaybackInfo(),
    val userAgent: String = SettingsRepository.DEFAULT_UA,
    val referer: String = "",
    val headers: Map<String, String> = emptyMap(),
    val pageTitle: String = "",
    val isJsEnabled: Boolean = true,
    val autoplay: Boolean = false,
    val loadAsHtml: Boolean = false,
    val htmlContent: String = "",
    val noVideoDetected: Boolean = false
)

class PlayerViewModel(
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var hideControlsJob: Job? = null
    private var currentSourceId: Long? = null

    init {
        viewModelScope.launch {
            val dataSaver = settingsRepository.dataSaver.first()
            val ua = settingsRepository.customUserAgent.first()
            val referer = settingsRepository.defaultReferer.first()
            val js = settingsRepository.enableJavascript.first()
            val autoplay = settingsRepository.autoplay.first()
            _uiState.value = _uiState.value.copy(
                isDataSaver = dataSaver,
                userAgent = ua,
                referer = referer,
                isJsEnabled = js,
                autoplay = autoplay
            )
        }
    }

    fun loadUrl(
        url: String,
        name: String = "",
        sourceId: Long? = null,
        userAgent: String? = null,
        referer: String? = null,
        headers: Map<String, String> = emptyMap()
    ) {
        val normalized = UrlAnalyzer.resolveCanonicalMediaUrl(UrlAnalyzer.normalizeUrl(url))
        if (!UrlAnalyzer.isValidUrl(normalized)) {
            _uiState.value = _uiState.value.copy(
                error = PlayerError.InvalidUrl(),
                playerState = PlayerState.ERROR,
                isLoading = false
            )
            return
        }
        currentSourceId = sourceId

        var type = UrlAnalyzer.detectType(normalized)
        val ua = userAgent?.takeIf { it.isNotBlank() } ?: _uiState.value.userAgent
        val ref = referer?.takeIf { it.isNotBlank() } ?: _uiState.value.referer

        // Clear file extension (.mp4 etc.) → open instantly, no probe
        val clearExtension = UrlAnalyzer.extractExtension(normalized).let { ext ->
            ext in setOf(
                "mp4", "mkv", "webm", "avi", "mov", "flv", "m4v", "ts",
                "3gp", "ogv", "mp3", "m4a", "aac"
            )
        }
        val clearManifest = type == SourceType.HLS || type == SourceType.DASH

        val knownDirect = (type == SourceType.DIRECT_VIDEO && clearExtension) || clearManifest
        val immediateHtml = if (knownDirect) {
            UrlAnalyzer.buildDirectVideoHtml(normalized, autoplay = _uiState.value.autoplay)
        } else ""

        _uiState.value = _uiState.value.copy(
            currentUrl = normalized,
            sourceName = name.ifBlank { normalized },
            sourceType = type,
            playerState = PlayerState.LOADING,
            isLoading = true,
            error = null,
            progress = 0,
            userAgent = ua,
            referer = ref,
            headers = headers,
            playbackInfo = PlaybackInfo(currentUrl = normalized, streamingType = type.name),
            loadAsHtml = knownDirect,
            htmlContent = immediateHtml,
            noVideoDetected = false
        )

        sourceId?.let { id ->
            viewModelScope.launch { sourceRepository.markPlayed(id) }
        }

        // Universal path: for unlimited hosts without clear extension, probe Content-Type (fast HEAD)
        if (knownDirect) return

        viewModelScope.launch {
            var playUrl = normalized
            var resolvedType = type

            if (UrlAnalyzer.shouldProbe(type) || !clearExtension) {
                val head = NetworkFetcher.probeHead(normalized, timeoutMs = 4500, userAgent = ua)
                if (head.success) {
                    val refined = UrlAnalyzer.refineTypeFromHeaders(
                        contentType = head.contentType,
                        contentDisposition = head.contentDisposition,
                        finalUrl = head.finalUrl
                    )
                    if (refined != SourceType.UNKNOWN) {
                        resolvedType = refined
                    }
                    if (!head.finalUrl.isNullOrBlank()) {
                        playUrl = head.finalUrl
                    }
                }
                // If probe failed: still open as WebView page — works for any site player
            }

            val direct = resolvedType == SourceType.DIRECT_VIDEO ||
                resolvedType == SourceType.HLS ||
                resolvedType == SourceType.DASH

            val html = if (direct) {
                UrlAnalyzer.buildDirectVideoHtml(playUrl, autoplay = _uiState.value.autoplay)
            } else ""

            if (_uiState.value.currentUrl == normalized || _uiState.value.currentUrl == playUrl) {
                _uiState.value = _uiState.value.copy(
                    currentUrl = playUrl,
                    sourceType = resolvedType,
                    loadAsHtml = direct,
                    htmlContent = html,
                    playbackInfo = _uiState.value.playbackInfo.copy(
                        currentUrl = playUrl,
                        streamingType = resolvedType.name,
                        protocol = playUrl.substringBefore("://", "http")
                    )
                )
            }
        }
    }

    fun loadSource(source: Source) {
        val headers = parseHeaders(source.headersJson)
        loadUrl(
            url = source.url,
            name = source.name,
            sourceId = source.id,
            userAgent = source.userAgent,
            referer = source.referer,
            headers = headers
        )
    }

    fun onPageStarted(url: String?) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            playerState = PlayerState.LOADING,
            error = null
        )
    }

    fun onPageFinished(url: String?) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            playerState = PlayerState.READY,
            currentUrl = url ?: _uiState.value.currentUrl
        )
        scheduleHideControls()
    }

    fun onProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(progress = progress)
    }

    fun onTitle(title: String?) {
        if (!title.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(pageTitle = title)
        }
    }

    fun onError(error: PlayerError) {
        _uiState.value = _uiState.value.copy(
            error = error,
            playerState = PlayerState.ERROR,
            isLoading = false
        )
    }

    fun setNoVideoDetected(value: Boolean) {
        _uiState.value = _uiState.value.copy(noVideoDetected = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onVideoState(state: JsBridge.VideoState) {
        if (state.found) {
            _uiState.value = _uiState.value.copy(noVideoDetected = false)
        }
        val info = _uiState.value.playbackInfo.copy(
            resolution = if (state.videoWidth > 0 && state.videoHeight > 0)
                "${state.videoWidth}x${state.videoHeight}" else "Unknown",
            durationMs = (state.duration * 1000).toLong().coerceAtLeast(0),
            positionMs = (state.currentTime * 1000).toLong().coerceAtLeast(0),
            isPlaying = state.found && !state.paused && !state.ended,
            isBuffering = state.readyState < 3 && !state.paused,
            playbackSpeed = state.playbackRate.toFloat(),
            volume = state.volume.toFloat(),
            isMuted = state.muted,
            currentUrl = state.src.ifBlank { _uiState.value.currentUrl }
        )
        val pState = when {
            !state.found -> _uiState.value.playerState
            state.ended -> PlayerState.ENDED
            state.paused -> PlayerState.PAUSED
            info.isBuffering -> PlayerState.BUFFERING
            else -> PlayerState.PLAYING
        }
        _uiState.value = _uiState.value.copy(
            playbackInfo = info,
            playerState = pState,
            isMuted = state.muted,
            volume = state.volume.toFloat(),
            playbackSpeed = state.playbackRate.toFloat()
        )
    }

    fun setFullscreen(value: Boolean) {
        _uiState.value = _uiState.value.copy(isFullscreen = value)
    }

    fun setPip(value: Boolean) {
        _uiState.value = _uiState.value.copy(isPip = value)
    }

    fun toggleAudioOnly() {
        val newVal = !_uiState.value.isAudioOnly
        _uiState.value = _uiState.value.copy(isAudioOnly = newVal)
    }

    fun setDataSaver(value: Boolean) {
        _uiState.value = _uiState.value.copy(isDataSaver = value)
        viewModelScope.launch { settingsRepository.setDataSaver(value) }
    }

    fun toggleControlsLock() {
        _uiState.value = _uiState.value.copy(isControlsLocked = !_uiState.value.isControlsLocked)
    }

    fun setMuted(muted: Boolean) {
        _uiState.value = _uiState.value.copy(isMuted = muted)
    }

    fun setVolume(volume: Float) {
        _uiState.value = _uiState.value.copy(volume = volume.coerceIn(0f, 1f))
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun showControlsTemporarily() {
        if (_uiState.value.isControlsLocked) return
        _uiState.value = _uiState.value.copy(showControls = true)
        scheduleHideControls()
    }

    fun toggleControls() {
        if (_uiState.value.isControlsLocked) return
        val show = !_uiState.value.showControls
        _uiState.value = _uiState.value.copy(showControls = show)
        if (show) scheduleHideControls() else hideControlsJob?.cancel()
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(4000)
            if (!_uiState.value.isControlsLocked) {
                _uiState.value = _uiState.value.copy(showControls = false)
            }
        }
    }

    fun resetPlayer() {
        _uiState.value = PlayerUiState(
            isDataSaver = _uiState.value.isDataSaver,
            userAgent = _uiState.value.userAgent,
            referer = _uiState.value.referer,
            isJsEnabled = _uiState.value.isJsEnabled,
            autoplay = _uiState.value.autoplay
        )
        currentSourceId = null
    }

    private fun parseHeaders(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return try {
            // Simple line-based: Key: Value
            json.lines()
                .mapNotNull { line ->
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                    } else null
                }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    class Factory(
        private val sourceRepository: SourceRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(sourceRepository, settingsRepository) as T
        }
    }
}
