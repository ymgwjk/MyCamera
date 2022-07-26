package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.dzm.bytesummer.mycamera.MainActivity
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.databinding.FragmentPhotoBinding
import com.dzm.bytesummer.mycamera.photo.PhotoCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PhotoFragment : Fragment() {

    private var _fragmentPhotoBinding: FragmentPhotoBinding? = null

    //视图绑定
    private val fragmentPhotoBinding get() = _fragmentPhotoBinding!!


    private lateinit var camera: PhotoCamera
    private val navController: NavController by lazy {
        Navigation.findNavController(activity!!, R.id.fragmentContainer)
    }

    private val shutterAnim: Runnable by lazy {
        Runnable {
            fragmentPhotoBinding.overlay.background = Color.argb(150, 0, 0, 0).toDrawable()
            fragmentPhotoBinding.overlay.postDelayed(
                { fragmentPhotoBinding.overlay.background = null },
                MainActivity.ANIMATION_FAST_MILLIS
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _fragmentPhotoBinding = FragmentPhotoBinding.inflate(inflater, container, false)
        return fragmentPhotoBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentPhotoBinding.buttonCapture.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            WindowInsets.CONSUMED
        }

        camera = PhotoCamera(this, fragmentPhotoBinding.cameraView)
        assert(this.view != null)

        initSurfaceView()

        camera.initCamera()
        fragmentPhotoBinding.buttonReverse.setOnClickListener {
            camera.reverseFace()
        }

        fragmentPhotoBinding.buttonCapture.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                it.isEnabled = false
                camera.takePhoto { fragmentPhotoBinding.cameraView.post(shutterAnim) }
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

        fragmentPhotoBinding.buttonGallery.setOnClickListener {
            navigateToGallery()
        }

        camera.initOrientation(context!!) {
            observe(viewLifecycleOwner) {
                Timber.d("Orientation changed: $it")
            }
        }
    }

    private fun initSurfaceView() {
        Log.d("ShootingFragment", "initScreen")
        fragmentPhotoBinding.cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                setSurfaceSize()
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
            }
        })
    }

    private fun setSurfaceSize() {
        val previewSize = camera.baseCamera.getPreviewSize(context!!, SurfaceHolder::class.java)
        fragmentPhotoBinding.cameraView.setAspectRatio(
            previewSize.width,
            previewSize.height
        )
        Log.d(
            "ShootingFragment",
            "cameraSize: ${fragmentPhotoBinding.cameraView.width}x${fragmentPhotoBinding.cameraView.height}"
        )
        Log.d("ShootingFragment", "previewSize: ${previewSize}")
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
        _fragmentPhotoBinding = null
        super.onDestroyView()
    }

    private fun navigateToGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            Timber.d(activity!!.filesDir.absolutePath)
            navController.navigate(
                PhotoFragmentDirections.photoToGallery(
                    activity!!.filesDir.absolutePath
                )
            )
        }
    }
}