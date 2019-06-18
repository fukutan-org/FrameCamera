package org.fukutan.libs.framecamera

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log

class SoundPlayer(descriptor: AssetFileDescriptor) {

    private val player: MediaPlayer

    init {
        val mp = MediaPlayer()
        mp.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
        mp.setOnPreparedListener {
            Log.d("MusicPlayer", "OnPrepared")
        }
        mp.prepareAsync()
        mp.isLooping = false

        player = mp
    }

    fun play() {
        player.start()
    }
}