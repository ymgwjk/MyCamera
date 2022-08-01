package com.dzm.bytesummer.mycamera.fragment

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.camera.VideoCamera
import com.dzm.bytesummer.mycamera.camera.VideoStateCallbacks
import com.dzm.bytesummer.mycamera.databinding.FragmentVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class VideoFragment : Fragment(), ZoomEnabledCameraFragment {

    private var _fragmentVideoBinding: FragmentVideoBinding? = null
    private val fragmentVideoBinding get() = _fragmentVideoBinding!!
    private val camera = VideoCamera()
    private val navController: NavController by lazy {
        Navigation.findNavController(activity!!, R.id.fragmentContainer)
    }

    override var zoomRatio = 1f
    override lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _fragmentVideoBinding =
            FragmentVideoBinding.inflate(inflater, container, false)
        return fragmentVideoBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentVideoBinding.buttonRec.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            WindowInsets.CONSUMED
        }
        camera.previewSurface = fragmentVideoBinding.cameraView2.surfaceProvider
        camera.startCamera(context!!, activity!!)

        fragmentVideoBinding.buttonReverse2.setOnClickListener { switch() }
        fragmentVideoBinding.buttonRec.setOnClickListener {
            camera.captureVideo(
                context!!,
                activity!!.contentResolver,
                object : VideoStateCallbacks {
                    override fun onPrepare() {
                        view.post { it.isEnabled = false }
                    }

                    override fun onStart() {
                        view.post {
                            fragmentVideoBinding.buttonReverse2.visibility = View.INVISIBLE
                            fragmentVideoBinding.buttonGallery2.visibility = View.INVISIBLE
                            (it as ImageButton).setImageResource(R.drawable.ic_record_stop)
                            it.isEnabled = true
                        }
                    }

                    override fun onFinished() {
                        view.post {
                            (it as ImageButton).setImageResource(R.drawable.ic_record)
                            it.isEnabled = true
                            fragmentVideoBinding.buttonReverse2.visibility = View.VISIBLE
                            fragmentVideoBinding.buttonGallery2.visibility = View.VISIBLE
                        }
                    }
                })
        }
        fragmentVideoBinding.toCapture.setOnClickListener {
            navigateToPhoto()
        }
        fragmentVideoBinding.buttonGallery2.setOnClickListener {
            navigateToGallery()
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
        fragmentVideoBinding.cameraView2.setOnTouchListener { cView, motionEvent ->
            cView.performClick()
            scaleDetector.onTouchEvent(motionEvent) || cView.onTouchEvent(motionEvent)
        }
    }

    private fun switch() {
        camera.switch(context!!, activity!!)
    }

    override fun onStop() {
        super.onStop()
        camera.closeCamera()
    }

    override fun onDestroyView() {
        _fragmentVideoBinding = null
        super.onDestroyView()
    }

    private fun navigateToGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(VideoFragmentDirections.videoToGallery())
        }
    }


    private fun navigateToPhoto() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(VideoFragmentDirections.videoToPhoto())
        }
    }
}