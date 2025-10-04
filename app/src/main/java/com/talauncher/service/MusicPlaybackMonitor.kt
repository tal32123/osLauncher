package com.talauncher.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.talauncher.data.model.MusicPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlaybackMonitor(private val context: Context) {
    private val TAG = "MusicPlaybackMonitor"

    private val _playbackState = MutableStateFlow(MusicPlaybackState.EMPTY)
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeControllers: List<MediaController> = emptyList()
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
            Log.d(TAG, "Active sessions changed: ${controllers?.size ?: 0} controllers")
            updateActiveControllers(controllers ?: emptyList())
        }
    }

    fun startMonitoring() {
        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val notificationListener = ComponentName(context, MusicNotificationListener::class.java)

            // Register listener
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, notificationListener)

            // Get initial active sessions
            val controllers = mediaSessionManager?.getActiveSessions(notificationListener) ?: emptyList()
            updateActiveControllers(controllers)

            Log.d(TAG, "Music playback monitoring started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission not granted for notification listener", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting music playback monitoring", e)
        }
    }

    fun stopMonitoring() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
            controllerCallbacks.forEach { (controller, callback) ->
                controller.unregisterCallback(callback)
            }
            controllerCallbacks.clear()
            activeControllers = emptyList()
            _playbackState.value = MusicPlaybackState.EMPTY
            Log.d(TAG, "Music playback monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music playback monitoring", e)
        }
    }

    private fun updateActiveControllers(controllers: List<MediaController>) {
        // Unregister old callbacks
        controllerCallbacks.forEach { (controller, callback) ->
            controller.unregisterCallback(callback)
        }
        controllerCallbacks.clear()

        activeControllers = controllers

        // Register new callbacks
        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    updatePlaybackState()
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    updatePlaybackState()
                }
            }
            controller.registerCallback(callback)
            controllerCallbacks[controller] = callback
        }

        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        // Find the first actively playing controller
        val playingController = activeControllers.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (playingController != null) {
            val metadata = playingController.metadata
            val playbackState = playingController.playbackState
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

            _playbackState.value = MusicPlaybackState(
                isPlaying = isPlaying,
                trackTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                packageName = playingController.packageName
            )

            Log.d(TAG, "Updated playback state: ${_playbackState.value}")
        } else {
            _playbackState.value = MusicPlaybackState.EMPTY
            Log.d(TAG, "No active playback")
        }
    }

    fun sendPlayPauseCommand() {
        val controller = getActiveController()
        controller?.transportControls?.let { controls ->
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                controls.pause()
            } else {
                controls.play()
            }
        }
    }

    fun sendPreviousCommand() {
        getActiveController()?.transportControls?.skipToPrevious()
    }

    fun sendNextCommand() {
        getActiveController()?.transportControls?.skipToNext()
    }

    private fun getActiveController(): MediaController? {
        return activeControllers.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: activeControllers.firstOrNull()
    }
}
