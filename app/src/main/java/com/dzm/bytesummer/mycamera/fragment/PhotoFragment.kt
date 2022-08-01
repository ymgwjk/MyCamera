package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.dzm.bytesummer.mycamera.MainActivity
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.camera.PhotoCamera
import com.dzm.bytesummer.mycamera.databinding.FragmentPhotoBinding
import com.dzm.bytesummer.mycamera.utils.FileUtils.Companion.uriToPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class PhotoFragment : Fragment(), ZoomEnabledCameraFragment {

    private var _fragmentPhotoBinding: FragmentPhotoBinding? = null

    //视图绑定
    private val fragmentPhotoBinding get() = _fragmentPhotoBinding!!


    private val navController: NavController by lazy {
        Navigation.findNavController(activity!!, R.id.fragmentContainer)
    }

    private val camera = PhotoCamera()

    private val shutterAnim: Runnable by lazy {
        Runnable {
            fragmentPhotoBinding.overlay.background = Color.argb(150, 0, 0, 0).toDrawable()
            fragmentPhotoBinding.overlay.postDelayed(
                { fragmentPhotoBinding.overlay.background = null },
                MainActivity.ANIMATION_FAST_MILLIS
            )
        }
    }

    override var zoomRatio = 1f
    override lateinit var scaleDetector: ScaleGestureDetector

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
        camera.previewSurface = fragmentPhotoBinding.cameraView.surfaceProvider
        camera.startCamera(context!!, this)
        fragmentPhotoBinding.buttonReverse.setOnClickListener {
            switch()
        }

        fragmentPhotoBinding.buttonCapture.setOnClickListener {
            fragmentPhotoBinding.cameraView.post(shutterAnim)
            Timber.d("shooting")
            camera.takePhoto(
                context!!,
                activity!!.contentResolver,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val path = uriToPath(
                            context!!,
                            outputFileResults.savedUri
                        )
                        Timber.d("Photo capture succeed:$path")
                        Toast.makeText(
                            context!!,
                            "Photo capture succeed: $path",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Timber.e("failed to save")
                        Timber.e(exception)
                    }
                })
        }

        fragmentPhotoBinding.buttonGallery.setOnClickListener {
            navigateToGallery()
        }

        fragmentPhotoBinding.toVideo.setOnClickListener {
            navigateToVideo()
        }
        
        zoomRatio = 1f
        scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    zoomRatio *= detector.scaleFactor
                    zoomRatio =
                        max(zoomRatio, 1f)
                    zoomRatio =
                        min(zoomRatio, 2f)
                    val linearZoom = zoomRatio - 1
                    camera.linearZoom(linearZoom)
                    return true
                }
            })
        fragmentPhotoBinding.cameraView.setOnTouchListener { cView, motionEvent ->
            cView.performClick()
            scaleDetector.onTouchEvent(motionEvent) || cView.onTouchEvent(motionEvent)
        }

    }

    private fun switch() {
        camera.switch(context!!, this)
    }


    override fun onStop() {
        super.onStop()
        camera.closeCamera()
    }

    override fun onDestroyView() {
        _fragmentPhotoBinding = null
        super.onDestroyView()
    }

    private fun navigateToGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(
                PhotoFragmentDirections.photoToGallery()
            )
        }
    }

    private fun navigateToVideo() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(PhotoFragmentDirections.photoToVideo())
        }
    }
}