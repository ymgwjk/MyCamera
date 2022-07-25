package com.dzm.bytesummer.mycamera.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.dzm.bytesummer.mycamera.databinding.FragmentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import kotlin.math.max


class GalleryFragment : Fragment() {
    private var _fragmentGallaryBinding: FragmentGalleryBinding? = null
    private val fragmentGallaryBinding: FragmentGalleryBinding get() = _fragmentGallaryBinding!!

//    val bitmapOptions = BitmapFactory.Options().apply {
//        inJustDecodeBounds = false
//        if (max(outHeight, outWidth) > DOWN_SAMPLE_SIZE) {
//            val scaleX = outWidth / DOWN_SAMPLE_SIZE + 1
//            val scaleY = outHeight / DOWN_SAMPLE_SIZE + 1
//            inSampleSize = max(scaleX, scaleY)
//        }
//        Timber.d("inSampleSize = $inSampleSize")
//    }

    private val args by navArgs<GalleryFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGallaryBinding = FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGallaryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        constructImageViews(view)
    }

    private fun constructImageViews(view: View) {
        val directoryPath = args.directoryPath
        Timber.d(directoryPath)
        val fileWalkTree = File(directoryPath).walk()
        view.post {
            var count = 0
            fileWalkTree.filter { it.isFile }
                .filter { it.extension == "jpg" || it.extension == "mp4" }
                .forEach { file ->
                    count++
                    val srcBitmap = getBitmap(file.absolutePath)
//                    val exifInfo = ExifInterface(file)
//                    val rotation = exifInfo.getAttributeInt(
//                        ExifInterface.TAG_ORIENTATION,
//                        ExifInterface.ORIENTATION_NORMAL
//                    )
                    val imageView = createImageView(srcBitmap)
                    imageView.setOnClickListener {
                        lifecycleScope.launch(Dispatchers.Main) {
                            findNavController().navigate(
                                GalleryFragmentDirections.actionGalleryFragmentToImageViewerFragment(
                                    file.absolutePath
                                )
                            )
                        }
                    }
                    fragmentGallaryBinding.imageContainer.addView(imageView)
                }
            Timber.d("$count files")
        }
    }

    private fun getBitmap(filePath: String): Bitmap = BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, this)
        inSampleSize = if (max(outHeight, outWidth) > DOWN_SAMPLE_SIZE) {
            val scaleX = outWidth / DOWN_SAMPLE_SIZE + 1
            val scaleY = outHeight / DOWN_SAMPLE_SIZE + 1
            max(scaleX, scaleY)
        } else 1
        Timber.d("$inSampleSize")
        inJustDecodeBounds = false
        BitmapFactory.decodeFile(filePath, this)
    }

    private fun createImageView(bitmap: Bitmap): ImageView {
        val imageView = ImageView(requireContext())

        val wm = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = wm.defaultDisplay.width

        imageView.setImageBitmap(bitmap)
        val lp = FrameLayout.LayoutParams(
            width / 3,
            width / 3
        )
        imageView.layoutParams = lp
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return imageView
    }

    companion object {
        private const val DOWN_SAMPLE_SIZE = 512
    }
}