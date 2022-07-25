package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.dzm.bytesummer.mycamera.MainActivity
import com.dzm.bytesummer.mycamera.MyCamera
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.databinding.FragmentShootingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ShootingFragment : Fragment() {

    private var _fsb: FragmentShootingBinding? = null

    //视图绑定
    private val fragmentShootingBinding get() = _fsb!!


    private lateinit var camera: MyCamera
    private val navController: NavController by lazy {
        Navigation.findNavController(activity!!, R.id.fragmentContainer)
    }

    private val shutterAnim: Runnable by lazy {
        Runnable {
            fragmentShootingBinding.overlay.background = Color.argb(150, 0, 0, 0).toDrawable()
            fragmentShootingBinding.overlay.postDelayed(
                { fragmentShootingBinding.overlay.background = null },
                MainActivity.ANIMATION_FAST_MILLIS
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _fsb = FragmentShootingBinding.inflate(inflater, container, false)
        return fragmentShootingBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentShootingBinding.buttonShutter.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            WindowInsets.CONSUMED
        }

        camera = MyCamera(this, fragmentShootingBinding.cameraView)
        assert(this.view != null)
        camera.initScreen()

        camera.initCamera()
        fragmentShootingBinding.buttonReverse.setOnClickListener {
            camera.reverseFace()
        }
        fragmentShootingBinding.buttonShutter.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                it.isEnabled = false
                camera.takePhoto { fragmentShootingBinding.cameraView.post(shutterAnim) }
                    .use { result ->
                        val output = camera.saveResult(result)

                        Timber.d("Image saved at: ${output.absolutePath}")

                        if (output.extension == "jpg") {
                            val exif = ExifInterface(output.absolutePath)
                            exif.setAttribute(
                                ExifInterface.TAG_ORIENTATION,
                                result.orientation.toString()
                            )
                            exif.saveAttributes()
                        }
                    }
                it.post {
                    Timber.d("saved")
                    it.isEnabled = true
                }
            }
        }
        fragmentShootingBinding.buttonGallery.setOnClickListener {
            navigateToGallery()
        }
        camera.initOrientation(context!!) {
            observe(viewLifecycleOwner) {
                Timber.d("Orientation changed: $it")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        camera.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.destroy()
    }

    override fun onDestroyView() {
        _fsb = null
        super.onDestroyView()
    }

    private fun navigateToGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            Timber.d(activity!!.filesDir.absolutePath)
            navController.navigate(
                ShootingFragmentDirections.actionShootingFragmentToGalleryFragment(
                    activity!!.filesDir.absolutePath
                )
            )
        }
    }
}