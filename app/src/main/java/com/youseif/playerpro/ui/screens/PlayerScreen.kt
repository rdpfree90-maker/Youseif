package com.youseif.playerpro.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youseif.playerpro.R
import com.youseif.playerpro.YouseifPlayerApp
import com.youseif.playerpro.data.model.PlayerError
import com.youseif.playerpro.data.model.PlayerState
import com.youseif.playerpro.data.model.Source
import com.youseif.playerpro.ui.components.PlayerControlsOverlay
import com.youseif.playerpro.ui.components.WebPlayerCommands
import com.youseif.playerpro.ui.components.WebPlayerView
import com.youseif.playerpro.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    initialUrl: String? = null,
    initialSource: Source? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as YouseifPlayerApp
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(app.sourceRepository, app.settingsRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val activity = context as? Activity
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val pipSupported = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    LaunchedEffect(initialUrl, initialSource) {
        when {
            initialSource != null -> viewModel.loadSource(initialSource)
            !initialUrl.isNullOrBlank() -> viewModel.loadUrl(initialUrl)
        }
    }

    // After page is ready, if no <video> appears within a few seconds, show hint
    LaunchedEffect(state.playerState, state.playbackInfo.isPlaying, state.currentUrl) {
        if (state.playerState == PlayerState.READY || state.playerState == PlayerState.PAUSED) {
            kotlinx.coroutines.delay(3500)
            val stillNoVideo = !state.playbackInfo.isPlaying &&
                state.playbackInfo.durationMs <= 0L &&
                state.playbackInfo.resolution == "Unknown"
            // Re-read via a soft signal: if still not playing and duration unknown
            if (stillNoVideo && state.playerState != PlayerState.PLAYING) {
                viewModel.setNoVideoDetected(true)
            }
        }
        if (state.playerState == PlayerState.PLAYING || state.playbackInfo.durationMs > 0L) {
            viewModel.setNoVideoDetected(false)
        }
    }

    DisposableEffect(state.playerState) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val view = LocalView.current
    LaunchedEffect(state.isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (state.isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler {
        when {
            state.isFullscreen -> viewModel.setFullscreen(false)
            WebPlayerCommands.goBack(webViewRef) -> Unit
            else -> {
                viewModel.resetPlayer()
                onBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.currentUrl.isNotBlank() && state.error == null) {
            WebPlayerView(
                state = state,
                onWebViewReady = { webViewRef = it },
                onPageStarted = viewModel::onPageStarted,
                onPageFinished = viewModel::onPageFinished,
                onProgress = viewModel::onProgress,
                onTitle = viewModel::onTitle,
                onError = viewModel::onError,
                onVideoState = viewModel::onVideoState,
                onFullscreenChange = viewModel::setFullscreen,
                onSeek = { sec ->
                    WebPlayerCommands.seek(webViewRef, sec)
                    viewModel.showControlsTemporarily()
                },
                onVolumeChange = { v ->
                    viewModel.setVolume(v)
                    viewModel.setMuted(v <= 0.01f)
                },
                onBrightnessChange = { level ->
                    activity?.window?.let { w ->
                        val lp = w.attributes
                        lp.screenBrightness = level
                        w.attributes = lp
                    }
                },
                onToggleControls = viewModel::toggleControls,
                modifier = Modifier.fillMaxSize()
            )
        }


        // No video element detected (web page without playable <video>)
        if (state.noVideoDetected && state.error == null && !state.isLoading) {
            Text(
                text = stringResource(R.string.no_video_detected),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(Color.Black.copy(alpha = 0.75f), MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.isLoading && state.error == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        state.error?.let { err ->
            ErrorPanel(
                error = err,
                onRetry = {
                    viewModel.clearError()
                    if (state.currentUrl.isNotBlank()) {
                        viewModel.loadUrl(
                            state.currentUrl,
                            state.sourceName,
                            userAgent = state.userAgent,
                            referer = state.referer,
                            headers = state.headers
                        )
                    }
                },
                onReload = {
                    viewModel.clearError()
                    WebPlayerCommands.reload(webViewRef)
                },
                onBack = {
                    viewModel.resetPlayer()
                    onBack()
                },
                onOpenBrowser = { openInBrowser(context, state.currentUrl) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (state.error == null && state.currentUrl.isNotBlank()) {
            PlayerControlsOverlay(
                state = state,
                pipSupported = pipSupported,
                onBack = {
                    if (state.isFullscreen) {
                        viewModel.setFullscreen(false)
                    } else {
                        viewModel.resetPlayer()
                        onBack()
                    }
                },
                onPlayPause = {
                    if (state.playerState == PlayerState.PLAYING ||
                        state.playerState == PlayerState.BUFFERING
                    ) {
                        WebPlayerCommands.pause(webViewRef)
                    } else {
                        WebPlayerCommands.play(webViewRef)
                    }
                    viewModel.showControlsTemporarily()
                },
                onSeekBack = {
                    WebPlayerCommands.seek(webViewRef, -10.0)
                    viewModel.showControlsTemporarily()
                },
                onSeekForward = {
                    WebPlayerCommands.seek(webViewRef, 10.0)
                    viewModel.showControlsTemporarily()
                },
                onSeekTo = { fraction ->
                    val dur = state.playbackInfo.durationMs
                    if (dur > 0) {
                        val seconds = (fraction.coerceIn(0f, 1f) * dur) / 1000.0
                        WebPlayerCommands.setCurrentTime(webViewRef, seconds)
                    }
                    viewModel.showControlsTemporarily()
                },
                onReload = {
                    WebPlayerCommands.reload(webViewRef)
                    viewModel.showControlsTemporarily()
                },
                onToggleMute = {
                    viewModel.setMuted(!state.isMuted)
                    viewModel.showControlsTemporarily()
                },
                onToggleFullscreen = {
                    viewModel.setFullscreen(!state.isFullscreen)
                },
                onTogglePip = {
                    enterPip(activity)
                    viewModel.setPip(true)
                },
                onToggleLock = viewModel::toggleControlsLock,
                onToggleAudioOnly = viewModel::toggleAudioOnly,
                onToggleDataSaver = { viewModel.setDataSaver(!state.isDataSaver) },
                onSpeedChange = viewModel::setPlaybackSpeed,
                onCopyUrl = {
                    copyToClipboard(context, state.currentUrl)
                    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                },
                onShareUrl = { shareUrl(context, state.currentUrl) },
                onOpenBrowser = { openInBrowser(context, state.currentUrl) }
            )
        }

        if (state.currentUrl.isBlank() && state.error == null && !state.isLoading) {
            Text(
                text = stringResource(R.string.enter_url),
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    error: PlayerError,
    onRetry: () -> Unit,
    onReload: () -> Unit,
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        is PlayerError.InvalidUrl -> stringResource(R.string.invalid_url)
        is PlayerError.NetworkError -> stringResource(R.string.network_error)
        is PlayerError.PlaybackError -> stringResource(R.string.playback_error)
        is PlayerError.UnsupportedSource -> stringResource(R.string.unsupported_source)
        is PlayerError.SslError -> stringResource(R.string.ssl_error)
        is PlayerError.Timeout -> stringResource(R.string.timeout)
        is PlayerError.HttpError -> "HTTP ${error.code}"
        is PlayerError.Generic -> error.message
    }
    Column(
        modifier = modifier
            .padding(24.dp)
            .background(Color.Black.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.White, style = MaterialTheme.typography.titleMedium)
        val detail = when (error) {
            is PlayerError.Generic -> error.message
            is PlayerError.NetworkError -> error.message
            is PlayerError.HttpError -> error.message
            else -> ""
        }
        if (detail.isNotBlank() && detail != message) {
            Text(
                text = detail,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.retry))
        }
        TextButton(onClick = onReload) { Text(stringResource(R.string.reload)) }
        TextButton(onClick = onOpenBrowser) { Text(stringResource(R.string.open_in_browser)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

private fun enterPip(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    try {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(params)
    } catch (_: Exception) {
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("url", text))
}

private fun shareUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun openInBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}
