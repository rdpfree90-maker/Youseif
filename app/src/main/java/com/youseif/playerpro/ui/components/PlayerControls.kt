package com.youseif.playerpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.youseif.playerpro.R
import com.youseif.playerpro.data.model.PlayerState
import com.youseif.playerpro.viewmodel.PlayerUiState

@Composable
fun PlayerControlsOverlay(
    state: PlayerUiState,
    pipSupported: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onReload: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onTogglePip: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleAudioOnly: () -> Unit,
    onToggleDataSaver: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCopyUrl: () -> Unit,
    onShareUrl: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = state.showControls || state.isControlsLocked
    var moreExpanded by remember { mutableStateOf(false) }

    val durationMs = state.playbackInfo.durationMs
    val positionMs = state.playbackInfo.positionMs
    val hasDuration = durationMs > 0L
    var sliderDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    val displayPosition = if (sliderDragging) {
        (sliderValue * durationMs).toLong()
    } else {
        positionMs
    }

    // CRITICAL: Player controls order must NEVER flip with Arabic RTL.
    // Only text/labels follow locale; icon/button order stays LTR always.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.sourceName.ifBlank { state.pageTitle }.ifBlank { state.currentUrl },
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildList {
                        if (state.playbackInfo.resolution != "Unknown") add(state.playbackInfo.resolution)
                        if (state.playbackInfo.streamingType != "Unknown") add(state.playbackInfo.streamingType)
                    }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                IconButton(onClick = onToggleLock) {
                    Icon(
                        if (state.isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(
                            if (state.isControlsLocked) R.string.unlock_controls else R.string.lock_controls
                        ),
                        tint = Color.White
                    )
                }
                if (!state.isControlsLocked) {
                    Box {
                        IconButton(onClick = { moreExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = { moreExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.audio_only) +
                                            if (state.isAudioOnly) " ✓" else ""
                                    )
                                },
                                onClick = { moreExpanded = false; onToggleAudioOnly() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.data_saver) +
                                            if (state.isDataSaver) " ✓" else ""
                                    )
                                },
                                onClick = { moreExpanded = false; onToggleDataSaver() }
                            )
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${stringResource(R.string.playback_speed)} ${speed}x" +
                                                if (state.playbackSpeed == speed) " ✓" else ""
                                        )
                                    },
                                    onClick = { moreExpanded = false; onSpeedChange(speed) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.copy_url)) },
                                onClick = { moreExpanded = false; onCopyUrl() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_url)) },
                                onClick = { moreExpanded = false; onShareUrl() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open_in_browser)) },
                                onClick = { moreExpanded = false; onOpenBrowser() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reload)) },
                                onClick = { moreExpanded = false; onReload() }
                            )
                        }
                    }
                }
            }

            if (!state.isControlsLocked) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        icon = Icons.Default.Replay10,
                        desc = stringResource(R.string.seek_backward),
                        onClick = onSeekBack
                    )
                    Spacer(Modifier.width(24.dp))
                    ControlButton(
                        icon = if (state.playerState == PlayerState.PLAYING ||
                            state.playerState == PlayerState.BUFFERING
                        ) Icons.Default.Pause else Icons.Default.PlayArrow,
                        desc = stringResource(
                            if (state.playerState == PlayerState.PLAYING) R.string.pause else R.string.play
                        ),
                        onClick = onPlayPause,
                        large = true
                    )
                    Spacer(Modifier.width(24.dp))
                    ControlButton(
                        icon = Icons.Default.Forward10,
                        desc = stringResource(R.string.seek_forward),
                        onClick = onSeekForward
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (state.isLoading || state.progress in 1..99) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    if (hasDuration) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(displayPosition),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(48.dp)
                            )
                            Slider(
                                value = if (sliderDragging) {
                                    sliderValue
                                } else {
                                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                },
                                onValueChange = {
                                    sliderDragging = true
                                    sliderValue = it
                                },
                                onValueChangeFinished = {
                                    sliderDragging = false
                                    onSeekTo(sliderValue)
                                },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = formatTime(durationMs),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onToggleMute) {
                            Icon(
                                if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = stringResource(
                                    if (state.isMuted) R.string.unmute else R.string.mute
                                ),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onReload) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reload),
                                tint = Color.White
                            )
                        }
                        if (pipSupported) {
                            IconButton(onClick = onTogglePip) {
                                Icon(
                                    Icons.Default.PictureInPictureAlt,
                                    contentDescription = stringResource(R.string.picture_in_picture),
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                if (state.isFullscreen) Icons.Default.FullscreenExit
                                else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.fullscreen),
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.locked),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
    } // end CompositionLocalProvider — force LTR
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
    large: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(if (large) 64.dp else 48.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(if (large) 36.dp else 28.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
