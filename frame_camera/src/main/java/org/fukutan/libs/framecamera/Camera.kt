package org.fukutan.libs.framecamera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import org.fukutan.libs.framecamera.enums.CameraType
import org.fukutan.libs.framecamera.util.CameraUtil
import org.fukutan.libs.framecamera.util.Util
import org.fukutan.libs.framecamera.view.AutoFitTextureView
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import org.fukutan.libs.framecamera.util.CaptureRequestHelper
import kotlin.collections.ArrayList


class Camera(private val context: Context, private var surfaceTexture: SurfaceTexture? = null) {

    var cameraDevice: CameraDevice? = null
    var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var captureSession: CameraCaptureSession? = null
    private var usingCameraType: CameraType = CameraType.BACK
    private var repeatingRequest: CaptureRequest? = null
    private var flashSupported = false

    private lateinit var imageReader: ImageReader
    private lateinit var cameraTouchEvent: CameraTouchEvent
    private lateinit var cameraSize: Size
    private lateinit var previewSize: Size
    lateinit var cameraId: String

    private var permissionChecker: (() -> Boolean)? = null
    private var errorSender: ((message: String) -> Unit)? = null
    private var onSuccessCapture: (() -> Unit)? = null
    private var soundPlayer = SoundPlayer(context.assets.openFd("sound_shutter.wav"))
    private var rotationObserver: RotationObserver? = null

    private val cameraCharacteristics: CameraCharacteristics
        get() {
            return cameraManager.getCameraCharacteristics(cameraId)
        }

    private val _fileList = mutableListOf<String>()
    val fileList: ArrayList<String>?
        get() {
            return if (_fileList.isEmpty()) null else ArrayList(_fileList.reversed())
        }

    private val _thumbnailList = mutableListOf<String>()
    val thumbnailList: ArrayList<String>?
        get() {
            return if (_thumbnailList.isEmpty()) null else ArrayList(_thumbnailList.reversed())
        }

    fun setPermissionChecker(checker: () -> Boolean) {
        permissionChecker = checker
    }

    fun setErrorSender(sender: (message: String) -> Unit) {
        errorSender = sender
    }

    fun setCapturedCallback(callback: () -> Unit) {
        onSuccessCapture = callback
    }

    fun setRotationObserver(observer: RotationObserver) {
        rotationObserver = observer
    }

    @SuppressLint("MissingPermission")
    fun openCamera(activity: Activity, textureView: AutoFitTextureView) {

        Log.d("camera", "openCamera")

        permissionChecker?.also {
            if ( !it.invoke() ) {
                return
            }
        }

        surfaceTexture = textureView.surfaceTexture

        val info = CameraUtil.getCameraInfo(cameraManager, usingCameraType)
        if (info == null) {
            errorSender?.invoke("camera id was not found, Camera type is ${usingCameraType.name}")
            close()
            return
        }

        cameraId    = info.second
        cameraSize  = info.first

        val c = cameraCharacteristics
        previewSize = CameraUtil.adjustPreviewSize(activity, c, textureView.width, textureView.height, cameraSize)

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        val o = activity.resources.configuration.orientation
        if (o == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize.width, previewSize.height)
        } else {
            textureView.setAspectRatio(previewSize.height, previewSize.width)
        }

        // Check if the flash is supported.
        val available = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        flashSupported = available ?: false

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {

            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createPreviewSession()
            }
            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }
            override fun onError(camera: CameraDevice, error: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }

    @SuppressLint("Recycle")
    private fun createPreviewSession() {

        Log.d("Camera", "createPreviewSession")
        if (cameraDevice == null) {
            return
        }

        cameraDevice?.also { device ->

            val format = CameraUtil.checkPhotoFormat(cameraId, cameraManager, ImageFormat.JPEG) ?: return
            imageReader = ImageReader.newInstance(cameraSize.width, cameraSize.height, format, 2) ?: return
            imageReader.setOnImageAvailableListener({
                saveCaptureImage()
            }, null)

            val texture = surfaceTexture
            texture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)

            val requestHelper = CaptureRequestHelper(device, surface, cameraCharacteristics)
            cameraTouchEvent = CameraTouchEvent(requestHelper)

            val builder = requestHelper.getAutoFocusBuilderForPreview()
            val request = builder.build()
            repeatingRequest = request

            val surfaceList = listOf(surface, imageReader.surface)
            device.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {

                    if (cameraDevice == null) {
                        return
                    }

                    captureSession = session
                    captureSession?.setRepeatingRequest(repeatingRequest, null, null)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, null)
        }
    }

    fun startTouchFocus(v: View, event: MotionEvent) : Boolean {

        val characteristics = cameraCharacteristics
        val ret = cameraTouchEvent.setFocus(captureSession, characteristics, v, event)
        repeatingRequest = cameraTouchEvent.touchFocusRequest
        return ret
    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {

        //  TODO Add the flash toggle button
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    fun capture() {

        captureSession?.also { session ->

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

            imageReader.surface?.also {surface ->
                captureBuilder?.also { builder ->

                    builder.addTarget(surface)
                    builder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                    val characteristic = cameraCharacteristics
                    val dOrientation = Util.getDeviceOrientation(context)
                    val orientation = CameraUtil.getJpegOrientation(characteristic, dOrientation)
                    Log.d("Camera", "device orientation: $dOrientation, jpeg orientation: $orientation")
                    builder.set(CaptureRequest.JPEG_ORIENTATION, orientation)

                    val cameraCallback = CameraCaptureCallback(cameraTouchEvent, imageReader, context)
                    session.stopRepeating()
                    session.capture(builder.build(), cameraCallback, null)
                }
            }
        }
    }

    private fun saveCaptureImage() {

        imageReader.also {

            soundPlayer.play()

            val path    = CameraUtil.createImageInCatchDir(context, it)
            val tpath   = CameraUtil.createThumbnailInCatchDir(context, path)

            _fileList.add(path)
            _thumbnailList.add(tpath)
            onSuccessCapture?.invoke()

            repeatingRequest?.also { request ->
                captureSession?.setRepeatingRequest(request, null, null)
            }
        }
    }

    fun close() {

        captureSession?.stopRepeating()
        captureSession?.close()
        imageReader.close()
        permissionChecker = null
        errorSender = null
        onSuccessCapture = null
        cameraDevice?.close()
    }
}