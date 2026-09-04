package com.youseif.playerpro.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.youseif.playerpro.data.model.PlayerState
import com.youseif.playerpro.data.repository.SettingsRepository
import com.youseif.playerpro.viewmodel.PlayerUiState
import com.youseif.playerpro.webview.JsBridge
import com.youseif.playerpro.webview.WebPlayerChromeClient
import com.youseif.playerpro.webview.WebPlayerClient
import kotlinx.coroutines.delay

/**
 * Core WebView player surface.
 * Exposes the live WebView instance via [onWebViewReady] so external controls
 * (Play/Pause/Seek/etc.) can call evaluateJavascript reliably.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerView(
    state: PlayerUiState,
    onWebViewReady: (WebView?) -> Unit,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (String?) -> Unit,
    onProgress: (Int) -> Unit,
    onTitle: (String?) -> Unit,
    onError: (com.youseif.playerpro.data.model.PlayerError) -> Unit,
    onVideoState: (JsBridge.VideoState) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onSeek: (Double) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var brightnessOverlay by remember { mutableFloatStateOf(-1f) }
    var volumeOverlay by remember { mutableFloatStateOf(-1f) }

    // Notify parent whenever WebView instance changes
    LaunchedEffect(webView) {
        onWebViewReady(webView)
    }

    // Poll video state
    LaunchedEffect(state.playerState, state.currentUrl, webView) {
        while (
            webView != null &&
            state.currentUrl.isNotBlank() &&
            state.playerState != PlayerState.ERROR &&
            state.playerState != PlayerState.IDLE
        ) {
            webView?.evaluateJavascript(POLL_STATE_JS, null)
            delay(800)
        }
    }

    // Audio only
    LaunchedEffect(state.isAudioOnly, webView) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.hideVideoVisual(${state.isAudioOnly});}catch(e){}",
            null
        )
    }

    LaunchedEffect(state.isMuted, webView) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.setMuted(${state.isMuted});}catch(e){}",
            null
        )
    }

    LaunchedEffect(state.volume, webView) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.setVolume(${state.volume});}catch(e){}",
            null
        )
    }

    LaunchedEffect(state.playbackSpeed, webView) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.setPlaybackRate(${state.playbackSpeed});}catch(e){}",
            null
        )
    }

    // Load URL or inline HTML for direct media when it changes
    LaunchedEffect(state.currentUrl, state.loadAsHtml, state.htmlContent, webView) {
        val wv = webView ?: return@LaunchedEffect
        val target = state.currentUrl
        if (target.isBlank()) return@LaunchedEffect
        val loadKey = if (state.loadAsHtml) "html:$target" else target
        if (loadKey == loadedUrl) return@LaunchedEffect
        loadedUrl = loadKey
        if (state.loadAsHtml && state.htmlContent.isNotBlank()) {
            // Base URL = media origin so relative resolution / referrer behave better
            val base = try {
                val u = java.net.URL(target)
                "${u.protocol}://${u.host}/"
            } catch (_: Exception) {
                target
            }
            wv.loadDataWithBaseURL(base, state.htmlContent, "text/html", "UTF-8", null)
        } else {
            val headers = linkedMapOf<String, String>()
            if (state.referer.isNotBlank()) headers["Referer"] = state.referer
            state.headers.forEach { (k, v) -> if (k.isNotBlank()) headers[k] = v }
            if (headers.isNotEmpty()) {
                wv.loadUrl(target, headers)
            } else {
                wv.loadUrl(target)
            }
        }
    }

    // Gestures use physical left/right of the screen — keep LTR so Arabic does not swap sides
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.isControlsLocked) {
                if (state.isControlsLocked) return@pointerInput
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val mid = size.width / 2
                        if (offset.x < mid) onSeek(-10.0) else onSeek(10.0)
                    }
                )
            }
            .pointerInput(state.isControlsLocked) {
                if (state.isControlsLocked) return@pointerInput
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    val x = change.position.x
                    val mid = size.width / 2f
                    val delta = -dragAmount / size.height.toFloat()
                    if (x < mid) {
                        val newB = (brightnessOverlay.coerceAtLeast(0.1f) + delta).coerceIn(0.01f, 1f)
                        brightnessOverlay = newB
                        onBrightnessChange(newB)
                    } else {
                        val newV = (volumeOverlay.coerceAtLeast(0f) + delta).coerceIn(0f, 1f)
                        volumeOverlay = newV
                        onVolumeChange(newV)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val wv = createConfiguredWebView(
                    context = ctx,
                    state = state,
                    onPageStarted = onPageStarted,
                    onPageFinished = { url ->
                        onPageFinished(url)
                        // Re-inject bridge after every page finish
                        webView?.evaluateJavascript(WebPlayerClient.VIDEO_DETECT_JS, null)
                    },
                    onProgress = onProgress,
                    onTitle = onTitle,
                    onError = onError,
                    onVideoState = onVideoState,
                    onFullscreen = { view, _ ->
                        if (view != null) {
                            container.removeAllViews()
                            container.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                            onFullscreenChange(true)
                        }
                    },
                    onExitFullscreen = {
                        container.removeAllViews()
                        webView?.let { w ->
                            (w.parent as? ViewGroup)?.removeView(w)
                            container.addView(
                                w,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                        onFullscreenChange(false)
                    }
                )
                container.addView(
                    wv,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                webView = wv
                container
            },
            modifier = Modifier.fillMaxSize()
        )

        if (brightnessOverlay >= 0f) {
            GestureOverlay(
                label = "Brightness",
                value = brightnessOverlay,
                modifier = Modifier.align(Alignment.CenterStart).padding(24.dp)
            )
            LaunchedEffect(brightnessOverlay) {
                delay(900)
                brightnessOverlay = -1f
            }
        }
        if (volumeOverlay >= 0f) {
            GestureOverlay(
                label = "Volume",
                value = volumeOverlay,
                modifier = Modifier.align(Alignment.CenterEnd).padding(24.dp)
            )
            LaunchedEffect(volumeOverlay) {
                delay(900)
                volumeOverlay = -1f
            }
        }
    }
    } // end LTR for gestures

    DisposableEffect(Unit) {
        onDispose {
            onWebViewReady(null)
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeJavascriptInterface("YouseifBridge")
                removeAllViews()
                destroy()
            }
            webView = null
            loadedUrl = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    state: PlayerUiState,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (String?) -> Unit,
    onProgress: (Int) -> Unit,
    onTitle: (String?) -> Unit,
    onError: (com.youseif.playerpro.data.model.PlayerError) -> Unit,
    onVideoState: (JsBridge.VideoState) -> Unit,
    onFullscreen: (android.view.View?, android.webkit.WebChromeClient.CustomViewCallback?) -> Unit,
    onExitFullscreen: () -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = state.isJsEnabled
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = !state.autoplay
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = false
            cacheMode = if (state.isDataSaver) {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                WebSettings.LOAD_DEFAULT
            }
            userAgentString = state.userAgent.ifBlank { SettingsRepository.DEFAULT_UA }
        }
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@apply, true)
        }

        addJavascriptInterface(JsBridge(onVideoState), "YouseifBridge")

        webViewClient = WebPlayerClient(
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onError = onError,
            extraHeaders = state.headers,
            defaultReferer = state.referer
        )
        webChromeClient = WebPlayerChromeClient(
            onCustomView = onFullscreen,
            onHideCustomView = onExitFullscreen,
            onProgress = onProgress,
            onTitle = onTitle
        )
    }
}

@Composable
private fun GestureOverlay(
    label: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f), MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$label ${(value * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Commands that operate on a live WebView instance */
object WebPlayerCommands {
    fun play(webView: WebView?) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.play();}catch(e){}",
            null
        )
    }

    fun pause(webView: WebView?) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.pause();}catch(e){}",
            null
        )
    }

    fun seek(webView: WebView?, seconds: Double) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.seek($seconds);}catch(e){}",
            null
        )
    }

    fun setCurrentTime(webView: WebView?, seconds: Double) {
        webView?.evaluateJavascript(
            "try{if(window.__youseifPlayerBridge)window.__youseifPlayerBridge.setCurrentTime($seconds);}catch(e){}",
            null
        )
    }

    fun reload(webView: WebView?) {
        webView?.reload()
    }

    fun goBack(webView: WebView?): Boolean {
        return if (webView?.canGoBack() == true) {
            webView.goBack()
            true
        } else false
    }
}

private const val POLL_STATE_JS =
    "(function(){try{if(window.__youseifPlayerBridge){var s=window.__youseifPlayerBridge.getState();if(window.YouseifBridge)window.YouseifBridge.reportState(s);}}catch(e){}})();"
