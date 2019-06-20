package org.fukutan.libs.framecamera

import android.hardware.camera2.*
import android.view.MotionEvent
import android.view.Surface
import android.view.View

class CameraTouchEvent(cameraDevice: CameraDevice, targetSurface: Surface) {

    companion object {
        const val FOCUS_TAG = "FOCUS_TAG"
    }

    private var manualFocusEngaged = false
    private val autoFocusCancelRequest: CaptureRequest
    private val touchFocusBuilder: CaptureRequest.Builder

    init {
        val b = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        b.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
        b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        b.addTarget(targetSurface)
        autoFocusCancelRequest = b.build()

        touchFocusBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        touchFocusBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
//        touchFocusBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        touchFocusBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        touchFocusBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        touchFocusBuilder.setTag(FOCUS_TAG) //we'll capture this later for resuming the preview
        touchFocusBuilder.addTarget(targetSurface)
    }

    fun falseManualFocusEngaged() {
        manualFocusEngaged = false
    }

    fun clearAutoFocusTrigger(session: CameraCaptureSession) {

        touchFocusBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
        session.setRepeatingRequest(touchFocusBuilder.build(), null, null)
    }

    fun setFocus(session: CameraCaptureSession?, c: CameraCharacteristics, view: View, motionEvent: MotionEvent) : Boolean {

        val actionMasked = motionEvent.actionMasked
        if (actionMasked != MotionEvent.ACTION_UP) {
            return false
        }
        if (manualFocusEngaged) {
            return true
        }

        session?.also {

            //  stop auto focus preview
            it.stopRepeating()

            //  cancel auto focus
            it.capture(autoFocusCancelRequest, null, null)

            //  start auto focus at touch point
            if (CameraUtil.isMeteringAreaAFSupported(c)) {

                val focusAreaTouch = CameraUtil.getFocusAreaRectAngleForTouchMode(c, motionEvent, view)
                touchFocusBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusAreaTouch))
            }
            it.capture(touchFocusBuilder.build(), null, null)

            manualFocusEngaged = true
            return true
        }

        return false
    }
}