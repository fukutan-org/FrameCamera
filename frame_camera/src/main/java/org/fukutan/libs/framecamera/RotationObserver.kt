package org.fukutan.libs.framecamera

import android.util.Log
import androidx.lifecycle.Observer

class RotationObserver : Observer<Orientation> {

    private var orientationDegree: Int = 0
    val orientation: Int
        get() = orientationDegree

    override fun onChanged(orientation: Orientation?) {

        if (orientation == null) return
        Log.d("rotation observer", "azimuth:${orientation.azimuth}, pitch: ${orientation.pitch}, roll: ${orientation.roll}")

        when (orientation.roll) {

        }
        orientationDegree = orientation.pitch.toInt()
    }
}