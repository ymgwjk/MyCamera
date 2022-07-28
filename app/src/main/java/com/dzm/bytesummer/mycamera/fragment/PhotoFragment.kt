package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.dzm.bytesummer.mycamera.MainActivity
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.databinding.FragmentPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoFragment : Fragment(){

    private var _fragmentPhotoBinding: FragmentPhotoBinding? = null

    //视图绑定
    private val fragmentPhotoBinding get() = _fragmentPhotoBinding!!


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

        fragmentPhotoBinding.buttonReverse.setOnClickListener {
        }

        fragmentPhotoBinding.buttonCapture.setOnClickListener {
        }

        fragmentPhotoBinding.buttonGallery.setOnClickListener {
            navigateToGallery()
        }

        fragmentPhotoBinding.toVideo.setOnClickListener {
            navigateToVideo()
        }
    }



    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onDestroyView() {
        _fragmentPhotoBinding = null
        super.onDestroyView()
    }

    private fun navigateToGallery() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(
                PhotoFragmentDirections.photoToGallery(
                    activity!!.filesDir.absolutePath
                )
            )
        }
    }

    private fun navigateToVideo() {
        lifecycleScope.launch(Dispatchers.Main) {
            navController.navigate(PhotoFragmentDirections.photoToVideo())
        }
    }
}