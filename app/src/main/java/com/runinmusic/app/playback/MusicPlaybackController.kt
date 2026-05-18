package com.runinmusic.app.playback

import android.media.AudioAttributes
import android.media.MediaPlayer

class MusicPlaybackController {
    private var player: MediaPlayer? = null

    fun play(url: String, onPrepared: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stop()
        val nextPlayer = MediaPlayer()
        player = nextPlayer
        runCatching {
            nextPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            nextPlayer.setDataSource(url)
            nextPlayer.setOnPreparedListener {
                it.start()
                onPrepared()
            }
            nextPlayer.setOnErrorListener { _, what, extra ->
                onError("播放器错误 what=$what extra=$extra")
                stop()
                true
            }
            nextPlayer.prepareAsync()
        }.onFailure { error ->
            onError(error.message ?: "无法播放服务器音乐")
            stop()
        }
    }

    fun stop() {
        player?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        player = null
    }
}
