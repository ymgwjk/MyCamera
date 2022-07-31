package com.dzm.bytesummer.mycamera.fragment

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dzm.bytesummer.mycamera.R
import com.dzm.bytesummer.mycamera.camera.PhotoCamera
import com.dzm.bytesummer.mycamera.camera.VideoCamera
import com.dzm.bytesummer.mycamera.databinding.FragmentGalleryBinding
import com.dzm.bytesummer.mycamera.utils.getTransformMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class GalleryFragment : Fragment() {
    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null
    private val fragmentGalleryBinding: FragmentGalleryBinding get() = _fragmentGalleryBinding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)
        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        constructImageViews(view)
    }

    private fun constructImageViews(view: View) {
        loadImages(view)
        loadVideos(view)
    }

    private fun loadImages(view: View) {
        val cursor = activity!!.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.MIME_TYPE} = 'image/jpeg'",
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )
        val files = mutableListOf<FileData>()
        view.post {
            var count = 0

            if (cursor != null && cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                do {

                    files.add(
                        FileData(
                            File(cursor.getString(pathColumn)),
                            cursor.getLong(idColumn)
                        )
                    )
                } while (cursor.moveToNext())
                files.filter { it.file.isFile }.filter { it.file.extension == "jpg" }
                    .filter { it.file.parent == "${Environment.getExternalStorageDirectory().absolutePath}${File.separator}${PhotoCamera.PICTURE_PATH}" }
                    .forEach { fileData ->
                        count++
                        val uri =
                            ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                fileData.id
                            )
                        val srcBitmap = activity!!.contentResolver.loadThumbnail(
                            uri, Size(
                                DOWN_SAMPLE_SIZE, DOWN_SAMPLE_SIZE
                            ), null
                        )
                        val matrix = getTransformMatrix(
                            ExifInterface(fileData.file).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                        )
                        val bitmap = Bitmap.createBitmap(
                            srcBitmap,
                            0,
                            0,
                            srcBitmap.width,
                            srcBitmap.height,
                            matrix,
                            true
                        )
                        val imageView = createImageContainer(bitmap, TYPE.PHOTO)
                        imageView.setOnClickListener {
                            lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().navigate(
                                    GalleryFragmentDirections.galleryToImageViewer(
                                        fileData.file.absolutePath
                                    )
                                )
                            }
                        }
                        fragmentGalleryBinding.imageContainer.addView(imageView)
                    }
            }
            Timber.d("$count images")
        }
    }

    private fun loadVideos(view: View) {
        val cursor = activity!!.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.MIME_TYPE} = 'video/mp4'",
            null,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )
        val files = mutableListOf<FileData>()
        view.post {
            var count = 0

            if (cursor != null && cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                do {
                    files.add(
                        FileData(
                            File(cursor.getString(pathColumn)),
                            cursor.getLong(idColumn)
                        )
                    )
                } while (cursor.moveToNext())
                files.filter { it.file.isFile }.filter { it.file.extension == "mp4" }
                    .filter { it.file.parent == "${Environment.getExternalStorageDirectory().absolutePath}${File.separator}${VideoCamera.VIDEO_PATH}" }
                    .forEach { fileData ->
                        count++
                        val uri =
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                fileData.id
                            )
                        val srcBitmap =
                            MediaMetadataRetriever().apply { setDataSource(fileData.file.absolutePath) }
                                .getScaledFrameAtTime(
                                    0,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                    DOWN_SAMPLE_SIZE,
                                    DOWN_SAMPLE_SIZE
                                ) ?: return@forEach
                        val matrix = getTransformMatrix(
                            ExifInterface(fileData.file).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                        )
                        val bitmap = Bitmap.createBitmap(
                            srcBitmap,
                            0,
                            0,
                            srcBitmap.width,
                            srcBitmap.height,
                            matrix,
                            true
                        )
                        val imageView = createImageContainer(bitmap, TYPE.VIDEO)
                        imageView.setOnClickListener {
                            lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().navigate(
                                    GalleryFragmentDirections.galleryToVideoViewer(
                                        uri
                                    )
                                )
                            }
                        }
                        fragmentGalleryBinding.imageContainer.addView(imageView)
                    }
            }
            Timber.d("$count videos")
        }
    }

    private fun createImageContainer(bitmap: Bitmap, type: TYPE): FrameLayout {
        val wm = requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = wm.defaultDisplay.width
        val lp = FrameLayout.LayoutParams(width / 3, width / 3)
        val imageView = createImageView(bitmap)
        return FrameLayout(context!!).apply {
            layoutParams = lp
            addView(imageView)
            if (type == TYPE.VIDEO) {
                val icoLp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.RIGHT or Gravity.BOTTOM
                    setBackgroundResource(R.color.black)
                }
                val ico = ImageView(context!!).apply {
                    setImageResource(R.drawable.ic_video_flag)
                    layoutParams = icoLp
                }
                addView(ico)
            }
        }
    }

    private fun createImageView(bitmap: Bitmap): ImageView {

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        return ImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            layoutParams = lp
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    companion object {
        private const val DOWN_SAMPLE_SIZE = 512

        data class FileData(val file: File, val id: Long)
        enum class TYPE {
            PHOTO,
            VIDEO
        }
    }
}