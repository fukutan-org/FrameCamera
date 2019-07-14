package org.fukutan.libs.framecamera.util

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.MeteringRectangle
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.Surface.*
import android.view.View
import org.fukutan.libs.framecamera.enums.CameraType
import java.io.File
import java.lang.Math.max
import java.util.*
import kotlin.collections.ArrayList
import android.util.Range
import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.FileInputStream


class CameraUtil {

    companion object {

        private const val PREFIX = "photo_"
        private const val THUMBNAIL_PREFIX = "thumbnail_"
        private const val EXTENSION = ".jpg"
        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(ROTATION_0, 90)
            ORIENTATIONS.append(ROTATION_90, 0)
            ORIENTATIONS.append(ROTATION_180, 270)
            ORIENTATIONS.append(ROTATION_270, 180)
        }

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_HEIGHT = 1080

        fun getCameraInfo(man: CameraManager, type: CameraType) : Pair<Size, String>? {

            man.cameraIdList.forEach { cameraId ->
                val characteristics = man.getCameraCharacteristics(cameraId)
                val lens = characteristics.get(CameraCharacteristics.LENS_FACING)

                val back = type == CameraType.BACK && lens == CameraCharacteristics.LENS_FACING_BACK
                val front = type == CameraType.FRONT && lens == CameraCharacteristics.LENS_FACING_FRONT
                if (back || front) {

                    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    map?.let {
                        val list = map.getOutputSizes(ImageFormat.JPEG)
                        val largest = list.maxWith(CompareSizesByArea())
                        val size = largest ?: map.getOutputSizes(SurfaceTexture::class.java).first()

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

        private fun makePhotoFilePathForCacheDirectory(context: Context) : File {

            val name = PREFIX + System.currentTimeMillis().toString()
            return File(context.cacheDir.path + File.separator + name + EXTENSION)
        }

        private fun makeThumbnailFilePathForCacheDirectory(context: Context) : File {

            val name = THUMBNAIL_PREFIX + System.currentTimeMillis().toString()
            return File(context.cacheDir.path + File.separator + name + EXTENSION)
        }

        fun getJpegOrientation(c: CameraCharacteristics, dOrientation: Int): Int {

            val deviceOrientation = dOrientation
            if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0
            val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            // Round device orientation to a multiple of 90
//            orientation = (orientation + 45) / 90 * 90
            var surfaceRotation = ORIENTATIONS.get(deviceOrientation)

            // Reverse device orientation for front-facing cameras
            val facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            if (facingFront) surfaceRotation = -surfaceRotation

            // Calculate desired JPEG orientation relative to camera orientation to make
            // the image upright relative to the device orientation

//            return (sensorOrientation + deviceOrientation + 360) % 360
            return (surfaceRotation + sensorOrientation + 270) % 360
        }

        fun getFocusAreaRectAngleForTouchMode(c: CameraCharacteristics, event: MotionEvent, touchView: View) : MeteringRectangle? {

            val sensorArraySize = c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            sensorArraySize?.also {

                val y = (event.x / touchView.width.toFloat() * sensorArraySize.height().toFloat()).toInt()
                val x = (event.y / touchView.height.toFloat() * sensorArraySize.width().toFloat()).toInt()

                val halfTouchWidth = 150 //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
                val halfTouchHeight = 150 //(int)motionEvent.getTouchMinor();

                return MeteringRectangle(
                    max(x - halfTouchWidth, 0),
                    max(y - halfTouchHeight, 0),
                    halfTouchWidth * 2,
                    halfTouchHeight * 2,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1
                )
            }
            return null
        }

        fun isMeteringAreaAFSupported(c: CameraCharacteristics): Boolean {
            val r = c.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
            if (r is Int) {
                return r >= 1
            }
            return false
        }

        fun adjustPreviewSize(activity: Activity, c: CameraCharacteristics, width: Int, height: Int, size: Size) : Size {

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            val displayRotation = activity.windowManager.defaultDisplay.rotation

            //noinspection ConstantConditions
            val orientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)
            var swappedDimensions = false
            val orientationInt = orientation.toInt()

            when (displayRotation) {
                ROTATION_0, ROTATION_180 -> if (orientationInt == 90 || orientationInt == 270) {
                    swappedDimensions = true
                }
                ROTATION_90, ROTATION_270 -> if (orientationInt == 0 || orientationInt == 180) {
                    swappedDimensions = true
                }
                else -> Log.e("CameraUtil", "Display rotation is invalid: $displayRotation")
            }

            val displaySize = Point()
            activity.windowManager.defaultDisplay.getSize(displaySize)
            var rotatedPreviewWidth = width
            var rotatedPreviewHeight = height
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y

            if (swappedDimensions) {
                rotatedPreviewWidth = height
                rotatedPreviewHeight = width
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            return chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, size
            )
        }

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as the
         * respective max size, and whose aspect ratio matches with the specified value. If such size
         * doesn't exist, choose the largest one that is at most as large as the respective max size,
         * and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended output
         * class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        private fun chooseOptimalSize(
            choices: Array<Size>, textureViewWidth: Int,
            textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()

            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough,
                    CompareSizesByArea()
                )
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough,
                    CompareSizesByArea()
                )
            } else {
                Log.e("CameraUtil", "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        fun getRange(chars: CameraCharacteristics): Range<Int>? {

            try {
                val ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                var result: Range<Int>? = null
                for (range in ranges!!) {
                    val upper = range.upper
                    // 10 - min range upper for my needs
                    if (upper >= 10) {
                        if (result == null || upper < result.upper.toInt()) {
                            result = range
                        }
                    }
                }
                if (result == null) {
                    result = ranges[0]
                }
                return result
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                return null
            }
        }

        fun createImageInCatchDir(context: Context, imageReader: ImageReader) : String {

            val img = imageReader.acquireLatestImage()
            val buffer = img.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)

            // 画像の書き込み
            val file = makePhotoFilePathForCacheDirectory(context)
            Util.writeFile(file, bytes)
            img.close()

            return file.path
        }

        fun createThumbnailInCatchDir(context: Context, path: String) : String {

            val thumbnailSIze = 150

            val fis = FileInputStream(path)
            var imageBitmap = BitmapFactory.decodeStream(fis)

            val antiAlias = true
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, thumbnailSIze, thumbnailSIze, antiAlias)

            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val bytes = baos.toByteArray()

            // 画像の書き込み
            val file = makeThumbnailFilePathForCacheDirectory(context)
            Util.writeFile(file, bytes)

            return file.path
        }
    }
}