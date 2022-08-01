package com.dzm.bytesummer.mycamera.camera

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.video.*
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import com.dzm.bytesummer.mycamera.utils.FileUtils.Companion.FILENAME_FORMAT
import com.dzm.bytesummer.mycamera.utils.FileUtils.Companion.uriToPath
import timber.log.Timber
import java.util.*
import java.util.concurrent.ExecutorService
import kotlin.math.max
import kotlin.math.min

class VideoCamera : IPreviewCamera, ZoomEnabledCamera {
    override lateinit var cameraExecutor: ExecutorService
    override lateinit var camera: Camera
    override var previewSurface: Preview.SurfaceProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private lateinit var recorder: Recorder
    private var recording: Recording? = null
    private var preview: Preview? = null

    override fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        super.startCamera(context, lifecycleOwner)
        preview = Preview.Builder().build()
            .also { it.setSurfaceProvider(previewSurface) }
        recorder = Recorder.Builder().setQualitySelector(
            QualitySelector.from(
                Quality.HIGHEST,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
            )
        ).build()
        videoCapture = VideoCapture.withOutput(recorder)
        iStartCamera(context, lifecycleOwner, preview!!, videoCapture!!)
    }

    override fun switch(context: Context, lifecycleOwner: LifecycleOwner) {
        if (CameraBase.FACE == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraBase.FACE = CameraSelector.DEFAULT_FRONT_CAMERA
        else if (CameraBase.FACE == CameraSelector.DEFAULT_FRONT_CAMERA)
            CameraBase.FACE = CameraSelector.DEFAULT_BACK_CAMERA
        startCamera(context, lifecycleOwner)
    }

    fun captureVideo(
        context: Context,
        contentResolver: ContentResolver,
        videoStateCallbacks: VideoStateCallbacks
    ) {
        if (this.videoCapture == null) {
            Toast.makeText(context, "camera is not prepared", Toast.LENGTH_SHORT).show()
            return
        }
        val videoCapture = this.videoCapture!!

        videoStateCallbacks.onPrepare()

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }
        val name =
            SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, VIDEO_PATH)
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        recording = videoCapture.output.prepareRecording(context, mediaStoreOutputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            )
                withAudioEnabled()
        }.start(cameraExecutor) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> videoStateCallbacks.onStart()
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg =
                            "Video capture succeed: ${
                                uriToPath(
                                    context,
                                    recordEvent.outputResults.outputUri
                                )
                            }"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        Timber.d(msg)
                    } else {
                        recording?.close()
                        recording = null
                        Timber.e("Video capture error: ${recordEvent.error}")
                    }
                    videoStateCallbacks.onFinished()
                }
            }
        }
    }

    companion object {
        const val VIDEO_PATH = "Movies/MyCamera"
    }

    override fun linearZoom(linear: Float) {
        var clampedLinear = max(linear, 0f)
        clampedLinear = min(clampedLinear, 1f)
        camera.cameraControl.setLinearZoom(clampedLinear)
    }
}