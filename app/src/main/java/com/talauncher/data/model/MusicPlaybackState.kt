package com.talauncher.data.model

data class MusicPlaybackState(
    val isPlaying: Boolean = false,
    val trackTitle: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val packageName: String? = null
) {
    val hasActivePlayback: Boolean
        get() = trackTitle != null || artist != null

    companion object {
        val EMPTY = MusicPlaybackState()
    }
}
