package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Color
import android.hardware.camera2.*
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.*
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.CameraHelper.Companion.getDisplaySize
import com.dzm.bytesummer.mycamera.CameraHelper.Companion.getPreviewSize
import com.dzm.bytesummer.mycamera.MainActivity
import com.dzm.bytesummer.mycamera.MyCamera
import com.dzm.bytesummer.mycamera.databinding.FragmentShootingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue

class ShootingFragment : Fragment() {


    //视图绑定
    private lateinit var fragmentShootingBinding: FragmentShootingBinding

    private lateinit var camera: MyCamera

    private val shutterAnim: Runnable by lazy {
        Runnable {
            fragmentShootingBinding.overlay.background = Color.argb(150, 0, 0, 0).toDrawable()
            fragmentShootingBinding.overlay.postDelayed(
                { fragmentShootingBinding.overlay.background = null },
                MainActivity.ANIMATION_FAST_MILLIS
            )
        }
    }

    fun performShutterAnim() {
        fragmentShootingBinding.cameraView.post(shutterAnim)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        fragmentShootingBinding = FragmentShootingBinding.inflate(inflater, container, false)
        return fragmentShootingBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentShootingBinding.buttonShutter.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.getInsets(WindowInsets.Type.systemBars()).right).toFloat()
            v.translationY = (-insets.getInsets(WindowInsets.Type.systemBars()).bottom).toFloat()
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

            }
        }
    }
}