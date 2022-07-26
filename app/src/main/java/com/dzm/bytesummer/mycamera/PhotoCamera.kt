package com.dzm.bytesummer.mycamera.photo

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.MyCamera
import com.dzm.bytesummer.mycamera.components.CameraView
import com.dzm.bytesummer.mycamera.utils.OrientationLiveData
import com.dzm.bytesummer.mycamera.utils.getExifOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PhotoCamera(fragment: Fragment, cameraView: CameraView) {
    private val fragment: Fragment
    private val cameraView: CameraView
    val baseCamera: MyCamera
    private lateinit var imageReader: ImageReader
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)


    lateinit var relativeOrient: OrientationLiveData
        private set

    fun initOrientation(context: Context, block: OrientationLiveData.() -> Unit) {
        relativeOrient = OrientationLiveData(context, baseCamera.characteristics).apply {
            this.block()
        }
    }

    init {
        this.fragment = fragment
        this.cameraView = cameraView
        baseCamera = MyCamera()
    }

    fun initCamera() = fragment.requireView().post {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            val size =
                baseCamera.characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                    .getOutputSizes(
                        ImageFormat.JPEG
                    ).maxByOrNull { it.width * it.height }!!
            imageReader =
                ImageReader.newInstance(
                    size.width,
                    size.height,
                    ImageFormat.JPEG,
                    MyCamera.IMAGE_BUFFER_SIZE
                )
            val targets =
                listOf(cameraView.holder.surface, imageReader.surface)
            baseCamera.open()
            baseCamera.createCaptureSession(targets)
            baseCamera.startPreview(cameraView)
        }
    }

    fun reverseFace() {
        baseCamera.close()
        baseCamera.reverseFace()
        imageReader.close()
        initCamera()
    }

    suspend fun takePhoto(shutterAnim: () -> Unit): MyCamera.Companion.CombinedCaptureResult =
        suspendCancellableCoroutine {
            while (imageReader.acquireNextImage() != null) continue

            val imageQueue = ArrayBlockingQueue<Image>(MyCamera.IMAGE_BUFFER_SIZE)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireNextImage()
                imageQueue.add(image)
            }, imageReaderHandler)

            val captureRequest =
                baseCamera.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    .apply { addTarget(imageReader.surface) }
            baseCamera.session.capture(
                captureRequest.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber)
                        shutterAnim()
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                        val exc = TimeoutException("Image dequeuing time out")
                        val timeoutRunnable = Runnable { it.resumeWithException(exc) }
                        imageReaderHandler.postDelayed(
                            timeoutRunnable,
                            MyCamera.IMAGE_CAPTURE_TIME_LIMIT
                        )

                        @Suppress("BlockingMethodInNonBlockingContext")
                        fragment.lifecycleScope.launch(it.context) {
                            while (true) {
                                val image = imageQueue.take()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && image.format != ImageFormat.DEPTH_JPEG && image.timestamp != resultTimestamp)
                                    continue
                                imageReaderHandler.removeCallbacks(timeoutRunnable)
                                imageReader.setOnImageAvailableListener(null, null)
                                while (imageQueue.size > 0) {
                                    imageQueue.take().close()
                                }
                                val rotation = relativeOrient.value ?: 0
                                val mirrored =
                                    baseCamera.characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                                val exifOrient =
                                    getExifOrientation(rotation, mirrored && MyCamera.USE_MIRROR)
                                it.resume(
                                    MyCamera.Companion.CombinedCaptureResult(
                                        image,
                                        result,
                                        exifOrient,
                                        imageReader.imageFormat
                                    )
                                )
                            }
                        }
                        Timber.d("Image Saved")
                    }
                },
                baseCamera.cameraHandler
            )
        }

    suspend fun saveResult(result: MyCamera.Companion.CombinedCaptureResult): File =
        suspendCancellableCoroutine {
            Timber.d("Saving")
            when (result.format) {
                ImageFormat.JPEG -> {
                    val buffer = result.image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                    try {
                        val output = MyCamera.createFile(fragment.context!!, "jpg")
                        FileOutputStream(output).use { it.write(bytes) }
                        it.resume(output)
                    } catch (e: IOException) {
                        Timber.e("Unable to write JPEG image to file")
                        Timber.e(e.stackTraceToString())
                        it.resumeWithException(e)
                    }
                }
                else -> {
                    val exc = RuntimeException("Unknown image format: ${result.image.format}")
                    Timber.e(exc.message, exc)
                    it.resumeWithException(exc)
                }
            }
        }

    fun destroy() {
        baseCamera.cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }

    fun close() {
        imageReader.close()
        baseCamera.close()
    }
}