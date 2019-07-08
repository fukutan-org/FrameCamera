package org.fukutan.libs.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.fukutan.libs.example.fragment.MenuFragment
import org.fukutan.libs.example.fragment.ThumbnailsFragment
import org.fukutan.libs.framecamera.CameraActivity

class MainActivity : AppCompatActivity(),
    MenuFragment.OnFragmentInteractionListener,
    ThumbnailsFragment.OnFragmentInteractionListener {

    private var result: CameraActivity.CaptureImageResult? = null

    override fun onFragmentInteraction() : CameraActivity.CaptureImageResult? {
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = CameraActivity.getResult(requestCode, resultCode, data)
    }
}
