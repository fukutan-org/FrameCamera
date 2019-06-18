package org.fukutan.libs.framecamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import org.fukutan.libs.framecamera.enums.CameraType
import java.io.FileOutputStream

class Camera(private val context: Context, private var surfaceTexture: SurfaceTexture? = null) {

    private var cameraDevice: CameraDevice? = null
    private var cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var captureSession: CameraCaptureSession? = null
    private var usingCameraType: CameraType = CameraType.BACK
    private var imageReader: ImageReader? = null
    private var repeatingRequest: CaptureRequest? = null

    private lateinit var cameraSize: Size
    private lateinit var cameraId: String

    private var permissionChecker: (() -> Boolean)? = null
    private var errorSender: ((message: String) -> Unit)? = null
    private var callBackForCaptured: (() -> Unit)? = null

    fun setPermissionChecker(checker: () -> Boolean) {
        permissionChecker = checker
    }

    fun setErrorSender(sender: (message: String) -> Unit) {
        errorSender = sender
    }

    fun setCapturedCallback(callback: () -> Unit) {
        callBackForCaptured = callback
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surface: SurfaceTexture) {

        permissionChecker?.also {
            if ( !it.invoke() ) {
                return
            }
        }

        surfaceTexture = surface

        val info = CameraUtil.getCameraInfo(cameraManager, usingCameraType)
        if (info == null) {
            errorSender?.invoke("camera id was not found, Camera type is ${usingCameraType.name}")
            return
        }

        cameraId    = info.second
        cameraSize  = info.first

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

        if (cameraDevice == null) {
            return
        }

        val format = CameraUtil.checkPhotoFormat(cameraId, cameraManager, ImageFormat.JPEG) ?: return
        imageReader = ImageReader.newInstance(cameraSize.width, cameraSize.height, format, 1) ?: return
        imageReader?.setOnImageAvailableListener({

            saveCaptureImage()
        },null)

        val texture = surfaceTexture
        texture?.setDefaultBufferSize(cameraSize.width, cameraSize.height)
        val surface = Surface(texture)

        val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder.addTarget(surface)
        val request = builder.build()
        repeatingRequest = request

        val surfaceList = listOf(surface, imageReader?.surface)
        cameraDevice?.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {

            override fun onConfigureFailed(session: CameraCaptureSession) {}

            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession?.setRepeatingRequest(request, null, null)
            }
        }, null)
    }

    fun capture() {

        captureSession?.also { session ->

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

            imageReader?.surface?.also {surface ->
                captureBuilder?.also { builder ->

                    builder.addTarget(surface)
                    builder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                    val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                    val dOrientation = Util.getDeviceOrientation(context)
                    val orientation = CameraUtil.getJpegOrientation(characteristic, dOrientation)
                    builder.set(CaptureRequest.JPEG_ORIENTATION, orientation)

                    session.stopRepeating()
                    session.capture(builder.build(), CameraCaptureCallback(imageReader, context), null)
                }
            }
        }
    }

    private fun saveCaptureImage() {

        imageReader?.also {
            val img = it.acquireLatestImage()
            val buffer = img.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)

            // 画像の書き込み
            val file = CameraUtil.makePhotoFilePathInTemporaryDirectory(context)
            val output = FileOutputStream( file )
            output.write(bytes)
            output.close()

            img.close()

            callBackForCaptured?.invoke()
            repeatingRequest?.also { request ->
                captureSession?.setRepeatingRequest(request, null, null)
            }
        }
    }
}