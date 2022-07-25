package com.dzm.bytesummer.mycamera.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.dzm.bytesummer.mycamera.databinding.FragmentImageViewerBinding
import com.dzm.bytesummer.mycamera.utils.getTransformMatrix
import timber.log.Timber
import java.io.File
import kotlin.math.max

class ImageViewerFragment : Fragment() {

    private var _fragmentBinding: FragmentImageViewerBinding? = null
    private val fragmentImageViewerBinding get() = _fragmentBinding!!

    private val args by navArgs<ImageViewerFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentImageViewerBinding.inflate(inflater, container, false)
        return fragmentImageViewerBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            val imageFile = File(args.fileAbsolutename)
            val srcBitmap = getBitmap(args.fileAbsolutename)
            val exifRotation = ExifInterface(imageFile).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            Timber.d("exifRotation = $exifRotation")
            val matrix = getTransformMatrix(exifRotation)
            val bitmap = Bitmap.createBitmap(
                srcBitmap,
                0,
                0,
                srcBitmap.width,
                srcBitmap.height,
                matrix,
                true
            )
            fragmentImageViewerBinding.imageView.setImageBitmap(bitmap)
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

    companion object {
        private const val DOWN_SAMPLE_SIZE = 2048
    }
}