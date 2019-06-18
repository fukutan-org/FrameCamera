package org.fukutan.libs.example

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.fukutan.libs.framecamera.CameraActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CameraActivity.startCameraActivity(activity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val bmp = CameraActivity.getResult(requestCode, resultCode, data)
        finish()
    }
}
