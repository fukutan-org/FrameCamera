package org.fukutan.libs.example

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import org.fukutan.libs.framecamera.CameraActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        CameraActivity.startCameraActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val result = CameraActivity.getResult(requestCode, resultCode, data)
        result?.also {
            Glide.with(this).load(it.filePathList?.first()).fitCenter().into(captureImage)
        }
//        finish()
    }
}
