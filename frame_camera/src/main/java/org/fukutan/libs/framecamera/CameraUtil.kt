package org.fukutan.libs.framecamera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import org.fukutan.libs.framecamera.enums.CameraType
import java.io.File


class CameraUtil {

    companion object {

        private const val PREFIX = "photo_"
        private const val EXTENSION = ".jpg"

        fun getCameraInfo(man: CameraManager, type: CameraType) : Pair<Size, String>? {

            man.cameraIdList.forEach { cameraId ->
                val characteristics = man.getCameraCharacteristics(cameraId)
                val lens = characteristics.get(CameraCharacteristics.LENS_FACING)

                val back = type == CameraType.BACK && lens == CameraCharacteristics.LENS_FACING_BACK
                val front = type == CameraType.FRONT && lens == CameraCharacteristics.LENS_FACING_FRONT
                if (back || front) {

                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    map?.let {
                        val size = map.getOutputSizes(SurfaceTexture::class.java).first()
                        return Pair(size, cameraId)
                    }
                    return null
                }
            }
            return null
        }

        fun checkPhotoFormat(cameraId: String, manager: CameraManager, vararg formats: Int) : Int? {

            val configs = manager.getCameraCharacteristics(cameraId).
                get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            formats.forEach {format ->
                configs?.outputFormats?.forEach {
                    if (it == format) {
                        return format
                    }
                }
            }
            return null
        }

        fun makePhotoFilePathInTemporaryDirectory(context: Context) : File {

            val name = PREFIX + System.currentTimeMillis().toString()
            return File(context.cacheDir.path + File.separator + name + EXTENSION)
        }

        fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {

            var orientation = deviceOrientation
            if (orientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0
            val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            // Round device orientation to a multiple of 90
            orientation = (orientation + 45) / 90 * 90

            // Reverse device orientation for front-facing cameras
            val facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            if (facingFront) orientation = -orientation

            // Calculate desired JPEG orientation relative to camera orientation to make
            // the image upright relative to the device orientation

            return (sensorOrientation + orientation + 360) % 360
        }
    }
}