package org.fukutan.libs.framecamera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.view.MotionEvent
import android.view.View
import org.fukutan.libs.framecamera.util.CameraUtil
import org.fukutan.libs.framecamera.util.CaptureRequestHelper

class CameraTouchEvent(requestHelper: CaptureRequestHelper) {

    companion object {
        const val FOCUS_TAG = "FOCUS_TAG"
    }

    private var manualFocusEngaged = false
    private val autoFocusCancelRequest: CaptureRequest
    private val touchFocusBuilder: CaptureRequest.Builder
    val touchFocusRequest: CaptureRequest
    get() = touchFocusBuilder.build()

    init {
        autoFocusCancelRequest = requestHelper.getAutoFocusCancelBuilderForPreview().build()
        touchFocusBuilder = requestHelper.getRegionAutoFocusBuilderForPreview()
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
            it.stopRepeating()
            it.capture(autoFocusCancelRequest, null, null)

            //  start auto focus at touch point
            if (CameraUtil.isMeteringAreaAFSupported(c)) {
                val focusAreaTouch = CameraUtil.getFocusAreaRectAngleForTouchMode(c, motionEvent, view)
                touchFocusBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusAreaTouch))
            }
            it.capture(touchFocusBuilder.build(), null, null)
            it.setRepeatingRequest(touchFocusBuilder.build(), null, null)

            manualFocusEngaged = true
            return true
        }

        return false
    }
}