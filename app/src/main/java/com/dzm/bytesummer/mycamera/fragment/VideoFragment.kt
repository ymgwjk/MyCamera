package com.dzm.bytesummer.mycamera.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.databinding.FragmentVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class VideoFragment : Fragment(){

    private var _fragmentVideoBinding: FragmentVideoBinding? = null
    private val fragmentVideoBinding get() = _fragmentVideoBinding!!


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
        fragmentVideoBinding.buttonStartRec.setOnApplyWindowInsetsListener { v, insets ->
            v.translationX = (-insets.systemWindowInsetRight).toFloat()
            v.translationY = (-insets.systemWindowInsetBottom).toFloat()
            WindowInsets.CONSUMED
        }

        fragmentVideoBinding.buttonReverse2.setOnClickListener { }
        fragmentVideoBinding.buttonStartRec.setOnClickListener {
        }
        fragmentVideoBinding.buttonStopRec.setOnClickListener {
        }
    }

}