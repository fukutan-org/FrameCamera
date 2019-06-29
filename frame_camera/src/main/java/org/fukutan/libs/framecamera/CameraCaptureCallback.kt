package org.fukutan.libs.framecamera

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader


class CameraCaptureCallback(
    private val cameraTouchEvent: CameraTouchEvent,
    private val jpegImageReader: ImageReader?, context: Context) : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {

        if (jpegImageReader == null) {
            return
        }

        cameraTouchEvent.falseManualFocusEngaged()

        if (request.tag == CameraTouchEvent.FOCUS_TAG) {
            //the focus trigger is complete -
            //resume repeating (preview surface will get frames), clear AF trigger
            cameraTouchEvent.clearAutoFocusTrigger(session)
        }
    }

    override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
        super.onCaptureFailed(session, request, failure)
        cameraTouchEvent.falseManualFocusEngaged()
    }
}