package org.fukutan.libs.framecamera.util

import android.content.Context
import android.graphics.Bitmap
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream


class Util {

    companion object {

        fun getDeviceOrientation(context: Context) : Int {
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            return display.rotation
        }

        fun writeFile(file: File, bytes: ByteArray) {

            val output = FileOutputStream( file )
            output.write(bytes)
            output.close()
        }
    }
}