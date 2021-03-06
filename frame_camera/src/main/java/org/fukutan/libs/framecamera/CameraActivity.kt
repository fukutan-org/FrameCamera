package org.fukutan.libs.framecamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.TextureView
import android.view.Window
import android.view.animation.AnimationUtils
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.camera_preview.*
import org.fukutan.libs.framecamera.util.Util

class CameraActivity : AppCompatActivity() {

    private var shutterClick: (() -> Unit)? = null
    private lateinit var camera: Camera
    private val rotationObserver = RotationObserver()

    class CaptureImageResult(fileList: ArrayList<String>?, thumbnailList: ArrayList<String>?) {

        var filePathList = fileList?.toList() ?: listOf()
        var thumbnailList = thumbnailList?.toList() ?: listOf()
        private val iterator = filePathList.iterator()

        fun nextImage() : Bitmap? {
            return BitmapFactory.decodeFile(iterator.next())
        }
    }

    companion object {
        private const val REQUEST_CODE = 1
        private const val TAG = "CameraActivity"
        private const val CAPTURE_LIST = "CaptureFilePathList"
        private const val THUMBNAIL_LIST = "ThumbnailFilePathList"

        const val REQUEST_CODE_CAMERA = 1000
        const val RESULT_FAILED = -1000
        const val RESULT_CANCELED = Activity.RESULT_CANCELED

        fun startCameraActivity(activity: Activity?) {

            val intent = Intent(activity, CameraActivity::class.java)
            activity?.startActivityForResult(intent, REQUEST_CODE_CAMERA)
        }

        fun getResult(requestCode: Int, resultCode: Int, data: Intent?) : CaptureImageResult? {

            if (requestCode == REQUEST_CODE_CAMERA) {

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val fileList = data?.getStringArrayListExtra(CAPTURE_LIST)
                        val thumbnailList = data?.getStringArrayListExtra(THUMBNAIL_LIST)
                        return fileList?.let { CaptureImageResult(fileList, thumbnailList) }
                    }
                    RESULT_FAILED -> {
                        return null
                    }
                    RESULT_CANCELED -> {
                        return null
                    }
                }
            }
            return null
        }

        private fun sendError(error: String) {
            Log.e(TAG, error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.camera_preview)

        setupUI()
        textureView.scaleX = 1.00001f
        camera = Camera(this, textureView.surfaceTexture)
        camera.setErrorSender(::failedOpenCamera)
        camera.setPermissionChecker(::requestCameraPermission)
        camera.setCapturedCallback(::onSuccessCapture)
        camera.setRotationObserver(rotationObserver)

        actionBar?.hide()
    }

    private fun setupUI() {

        val small = AnimationUtils.loadAnimation(this, R.anim.down_scale)
        val normal = AnimationUtils.loadAnimation(this, R.anim.normal_scale)

        shutterButton.setOnTouchListener { v, event ->
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    shutterClick?.also { v?.startAnimation(small) }
                }
                MotionEvent.ACTION_UP -> {
                    v?.startAnimation(normal)
                    shutterClick?.invoke()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v?.startAnimation(normal)
                }
            }
            true
        }
        setShutterEvent()

        textureView.setOnTouchListener { v, event ->

            when (event!!.action) {
                MotionEvent.ACTION_UP -> {
                    camera.startTouchFocus(v, event)
                }
            }
            true
        }

        OrientationLiveData(this).observe(this, rotationObserver)
    }

    private fun setShutterEvent() {

        if (shutterClick == null) {
            shutterClick = {
                shutterClick = null
                camera.capture()
            }
        }
    }

    private fun onSuccessCapture() {

        setShutterEvent()
        val data = Intent()
        data.putExtra(CAPTURE_LIST, camera.fileList)
        data.putExtra(THUMBNAIL_LIST, camera.thumbnailList)
        setResult(Activity.RESULT_OK, data)
    }

    override fun onResume() {
        super.onResume()

        if (!textureView.isAvailable && textureView.surfaceTextureListener == null) {

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    return true
                }

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    camera.openCamera(this@CameraActivity, textureView)
                }
            }
        }
    }

    private fun requestCameraPermission() : Boolean {

        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (permission == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE)
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//
//                //
//            }
        }

        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    camera.openCamera(this@CameraActivity, textureView)
                } else {
                    failedOpenCamera("Camera Permission is not granted")
                }
            }
        }
    }

    private fun failedOpenCamera(error: String) {
        sendError(error)
        setResult(RESULT_FAILED)
        finish()
    }

    override fun onBackPressed() {
        camera.close()
        super.onBackPressed()
    }
}