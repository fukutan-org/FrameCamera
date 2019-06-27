package org.fukutan.libs.framecamera.util

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest.*
import android.view.Surface
import org.fukutan.libs.framecamera.CameraTouchEvent

class CaptureRequestHelper (
    private val device: CameraDevice,
    private  val surface: Surface,
    private  val characteristics: CameraCharacteristics) {

    companion object {
        private const val JPEG_QUALITY_100: Byte = 100
    }

    fun getAutoFocusCancelBuilderForPreview() : Builder {

        val b = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        b.apply {
            set(CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            set(CONTROL_AF_MODE, CONTROL_AF_MODE_OFF)
            addTarget(surface)
        }

        return b
    }

    fun getRegionAutoFocusBuilderForPreview() : Builder {

        val b = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        b.apply {
            addTarget(surface)
            setTag(CameraTouchEvent.FOCUS_TAG)      //we'll capture this later for resuming the preview
            set(CONTROL_MODE,                       CONTROL_MODE_AUTO)
            set(CONTROL_AF_MODE,                    CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CONTROL_AF_TRIGGER,                 CONTROL_AF_TRIGGER_START)
            setAutoExposure(b)
            setHighQuality(b)
        }

        return b
    }

    fun getAutoFocusBuilderForPreview() : Builder {

        val b = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        b.apply {
            set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            setAutoExposure(b)
            setHighQuality(b)
            addTarget(surface)
        }

        return b
    }

    private fun setHighQuality(b: Builder) {

        b.apply {
            set(JPEG_QUALITY,   JPEG_QUALITY_100)
            set(EDGE_MODE,      EDGE_MODE_HIGH_QUALITY)
            set(SHADING_MODE,   SHADING_MODE_HIGH_QUALITY)
            set(TONEMAP_MODE,   TONEMAP_MODE_HIGH_QUALITY)
            set(HOT_PIXEL_MODE, HOT_PIXEL_MODE_HIGH_QUALITY)

            set(COLOR_CORRECTION_MODE,  COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
            set(NOISE_REDUCTION_MODE,   NOISE_REDUCTION_MODE_HIGH_QUALITY)

            set(COLOR_CORRECTION_ABERRATION_MODE,   COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY)
            set(LENS_OPTICAL_STABILIZATION_MODE,    LENS_OPTICAL_STABILIZATION_MODE_ON)
        }
    }

    private fun setAutoExposure(b: Builder) {
        b.apply {
            set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
            set(CONTROL_AE_TARGET_FPS_RANGE, CameraUtil.getRange(characteristics))
        }
    }
}