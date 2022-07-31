package com.dzm.bytesummer.mycamera.fragment

import android.media.Image
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.VideoView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.databinding.FragmentVideoViewerBinding
import com.dzm.bytesummer.mycamera.utils.FileUtils.Companion.uriToPath
import kotlinx.coroutines.launch

class VideoViewerFragment : Fragment() {
    private var _viewBinding: FragmentVideoViewerBinding? = null
    private val viewBinding: FragmentVideoViewerBinding get() = _viewBinding!!

    private val args by navArgs<VideoViewerFragmentArgs>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = FragmentVideoViewerBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            showVideo(args.videoUri)
        } else {
            val path = uriToPath(context!!, args.videoUri) ?: return
            MediaScannerConnection.scanFile(
                context!!, arrayOf(path), null
            ) { _, uri ->
                if (uri != null) {
                    lifecycleScope.launch {
                        showVideo(uri)
                    }
                }
            }
        }
        viewBinding.videoController.setImageResource(R.drawable.ic_pause)
        viewBinding.videoController.setOnClickListener {
            if (viewBinding.videoView.isPlaying()) {
                it.isEnabled = false
                (it as ImageButton).setImageResource(R.drawable.ic_play)
                viewBinding.videoView.pause()
                it.isEnabled = true
            } else {
                it.isEnabled = false
                (it as ImageButton).setImageResource(R.drawable.ic_pause)
                viewBinding.videoView.start()
                it.isEnabled = true
            }
        }
        viewBinding.videoView.setOnCompletionListener {
            viewBinding.videoController.setImageResource(R.drawable.ic_play)
        }
    }

    private fun showVideo(uri: Uri) {
        val fileSize = getFileSizeFromUri(uri)
        if (fileSize == null || fileSize <= 0) {
            Log.e("VideoViewerFragment", "Failed to get recorded file size, could not be played!")
            return
        }

        val filePath = uriToPath(context!!, uri) ?: return
        val fileInfo = "FileSize: $fileSize\n $filePath"

        val mc = MediaController(requireContext())
        viewBinding.videoView.apply {
            setVideoURI(uri)
            setMediaController(mc)
            requestFocus()
        }.start()
        mc.show(0)
    }

    private fun getFileSizeFromUri(contentUri: Uri): Long? {
        val cursor = requireContext()
            .contentResolver
            .query(contentUri, arrayOf(MediaStore.Video.Media.SIZE), null, null, null)
            ?: return null

        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        cursor.moveToFirst()

        cursor.use {
            return it.getLong(sizeIndex)
        }
    }
}