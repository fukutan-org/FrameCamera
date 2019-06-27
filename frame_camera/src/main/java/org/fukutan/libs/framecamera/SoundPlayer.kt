package org.fukutan.libs.framecamera

import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.renderscript.RenderScript
import android.util.Log

class SoundPlayer(descriptor: AssetFileDescriptor) {

    companion object {
        private const val TAG = "SoundPlayer"
    }
    private val player: SoundPool
    private val soundId: Int
    private var loaded = false

    init {

        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        player = SoundPool.Builder()
            .setAudioAttributes(attr)
            .setMaxStreams(1)
            .build()
        soundId = player.load(descriptor, 1)

        player.setOnLoadCompleteListener { soundPool, sampleId, status ->

            if (status == 0) {
                loaded = true
            }
        }
    }

    fun play() {
        if (loaded) {
            player.play(soundId, 1.0f, 1.0f, 0, 0, 1f)
        } else {
            Log.d(TAG, "sound file was not loaded yet")
        }
    }
}