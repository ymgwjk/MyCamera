package com.dzm.bytesummer.mycamera.camera

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner
import com.dzm.bytesummer.mycamera.utils.FileUtils.Companion.FILENAME_FORMAT
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.math.max
import kotlin.math.min

class PhotoCamera : IPreviewCamera, ZoomEnabledCamera {
    override lateinit var cameraExecutor: ExecutorService
    override lateinit var camera: Camera
    override var previewSurface: Preview.SurfaceProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    override fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        super.startCamera(context, lifecycleOwner)
        preview = Preview.Builder().build()
            .apply { setSurfaceProvider(previewSurface) }
        imageCapture = ImageCapture.Builder().build()
        iStartCamera(context, lifecycleOwner, preview!!, imageCapture!!)
    }

    override fun switch(context: Context, lifecycleOwner: LifecycleOwner) {
        if (CameraBase.FACE == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraBase.FACE = CameraSelector.DEFAULT_FRONT_CAMERA
        else if (CameraBase.FACE == CameraSelector.DEFAULT_FRONT_CAMERA)
            CameraBase.FACE = CameraSelector.DEFAULT_BACK_CAMERA
        startCamera(context, lifecycleOwner)
    }

    override fun linearZoom(linear: Float) {
        var clampedLinear = max(linear, 0f)
        clampedLinear = min(clampedLinear, 1f)
        camera.cameraControl.setLinearZoom(clampedLinear)
    }

    fun takePhoto(
        context: Context,
        contentResolver: ContentResolver,
        onImageSavedCallback: ImageCapture.OnImageSavedCallback
    ) {
        if (imageCapture == null) {
            Toast.makeText(
                context,
                "camera is not prepared",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }
        val imageCapture = this.imageCapture!!
        val name =
            "IMG_${
                SimpleDateFormat(
                    FILENAME_FORMAT,
                    Locale.CHINA
                ).format(System.currentTimeMillis())
            }"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //自定义外部存储空间
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCamera")
            }
        }
        val metadata = ImageCapture.Metadata()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).setMetadata(metadata).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            onImageSavedCallback
        )
    }

    companion object {
        const val PICTURE_PATH = "Pictures/MyCamera"
    }
}