package org.fukutan.libs.framecamera

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader


class CameraCaptureCallback(
    private val jpegImageReader: ImageReader?,
    context: Context) : CameraCaptureSession.CaptureCallback() {

    private var soundPlayer = SoundPlayer(context.assets.openFd("sound_shutter.wav"))

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {

        if (jpegImageReader == null) { return }
        soundPlayer.play()
    }
}