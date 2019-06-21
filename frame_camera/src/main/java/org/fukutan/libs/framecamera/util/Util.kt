package org.fukutan.libs.framecamera.util

import android.content.Context
import android.view.WindowManager


class Util {

    companion object {
        fun getDeviceOrientation(context: Context) : Int {
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            return display.rotation
        }
    }
}