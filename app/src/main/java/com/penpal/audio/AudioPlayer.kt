package com.penpal.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var currentFilePath: String? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    _isPlaying.value = false
                    exoPlayer?.seekTo(0)
                }
                Player.STATE_READY -> {
                    _duration.value = exoPlayer?.duration ?: 0L
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }

    suspend fun play(filePath: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (currentFilePath != filePath) {
                stop()
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val file = File(filePath)
                    if (!file.exists()) {
                        return@withContext Result.failure(Exception("File not found: $filePath"))
                    }
                    setMediaItem(MediaItem.fromUri(file.toURI().toString()))
                    prepare()
                    addListener(playerListener)
                }
                currentFilePath = filePath
            }

            exoPlayer?.play()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        currentFilePath = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun updatePosition() {
        _currentPosition.value = exoPlayer?.currentPosition ?: 0L
    }

    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        currentFilePath = null
    }
}