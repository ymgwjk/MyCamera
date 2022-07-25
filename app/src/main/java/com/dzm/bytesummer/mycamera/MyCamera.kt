package com.dzm.bytesummer.mycamera

import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.components.CameraView
import com.dzm.bytesummer.mycamera.utils.OrientationLiveData
import com.dzm.bytesummer.mycamera.utils.getExifOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MyCamera(fragment: Fragment, view: CameraView) : Closeable {
    private val cameraView: CameraView
    private val fragment: Fragment
    private val cameraManager: CameraManager

    var cameraFace = CameraCharacteristics.LENS_FACING_BACK

    var characteristics: CameraCharacteristics


    lateinit var cameraDevice: CameraDevice
        private set

    lateinit var session: CameraCaptureSession
        private set

    lateinit var imageReader: ImageReader
        private set

    private lateinit var relativeOrient: OrientationLiveData

    //image reader 线程
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    //相机线程
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)


    init {
        this.fragment = fragment
        cameraManager =
            fragment.context!!.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        characteristics = cameraManager.getCameraCharacteristics(getCameraId())
        cameraView = view
    }

    fun initOrientation(context: Context, block: OrientationLiveData.() -> Unit) {
        relativeOrient = OrientationLiveData(context, characteristics).apply {
            this.block()
        }
    }

    fun initScreen() {
        Log.d("ShootingFragment", "initScreen")
        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                val previewSize = CameraHelper.getPreviewSize(
                    cameraView.context,
                    characteristics,
                    SurfaceHolder::class.java
                )
                cameraView.setAspectRatio(
                    previewSize.width,
                    previewSize.height
                )
                Log.d("ShootingFragment", "cameraSize: ${cameraView.width}x${cameraView.height}")
                Log.d("ShootingFragment", "previewSize: ${previewSize}")
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
            }
        })
    }

    private fun resetSurfaceSize() {
        val previewSize = CameraHelper.getPreviewSize(
            cameraView.context,
            characteristics,
            SurfaceHolder::class.java
        )
        cameraView.setAspectRatio(
            previewSize.width,
            previewSize.height
        )
    }


    fun getCameraId(): String {
        return cameraIds!![cameraFace]!!
    }

    fun reverseFace() {
        if (cameraFace == CameraCharacteristics.LENS_FACING_BACK) {
            cameraFace = CameraCharacteristics.LENS_FACING_FRONT
        } else if (cameraFace == CameraCharacteristics.LENS_FACING_FRONT)
            cameraFace = CameraCharacteristics.LENS_FACING_BACK
        else return
        cameraDevice.close()
        session.close()
        imageReader.close()
        characteristics = cameraManager.getCameraCharacteristics(getCameraId())
        resetSurfaceSize()
        initCamera()
    }

    suspend fun prepare(targets: List<Surface>) {
        openCamera()
        createCaptureSession(cameraDevice, targets)
    }

    fun initCamera() = fragment.requireView().post {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                .getOutputSizes(
                    ImageFormat.JPEG
                ).maxByOrNull { it.width * it.height }!!
            imageReader =
                ImageReader.newInstance(
                    size.width,
                    size.height,
                    ImageFormat.JPEG,
                    IMAGE_BUFFER_SIZE
                )
            val targets = listOf(cameraView.holder.surface, imageReader.surface)
            prepare(targets)
            startPreview(cameraView)
        }
    }


    suspend fun openCamera() {
        cameraDevice = suspendCancellableCoroutine {
            cameraManager.openCamera(getCameraId(), object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    //返回相机设备
                    it.resume(p0)
                }

                override fun onDisconnected(p0: CameraDevice) {
                    fragment.activity!!.finish()
                }

                override fun onError(p0: CameraDevice, error: Int) {
                    p0.close()
                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera ${getCameraId()} error: ($error) $msg")
                    Timber.d(exc.message, exc)
                    if (it.isActive) it.resumeWithException(exc)
                }
            }, cameraHandler)
        }
    }


    suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
    ) {
        session = suspendCancellableCoroutine {
            device.createCaptureSession(
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    targets.map { OutputConfiguration(it) },
                    { runnable -> runnable.run() },
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(p0: CameraCaptureSession) {
                            it.resume(p0)
                        }

                        override fun onConfigureFailed(p0: CameraCaptureSession) {
                            val exc =
                                RuntimeException("Camera ${getCameraId()} session configuration failed")
                            Timber.e(exc, exc.message)
                            it.resumeWithException(exc)
                        }

                    })
            )
        }
    }

    fun startPreview(surfaceView: SurfaceView) {
        val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            .apply { addTarget(surfaceView.holder.surface) }

        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
    }

    suspend fun takePhoto(shutterAnim: () -> Unit): CombinedCaptureResult =
        suspendCancellableCoroutine {
            while (imageReader.acquireNextImage() != null) continue

            val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireNextImage()
                imageQueue.add(image)
            }, imageReaderHandler)

            val captureRequest =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    .apply { addTarget(imageReader.surface) }
            session.capture(
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
                        imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIME_LIMIT)

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
                                    characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                                val exifOrient = getExifOrientation(rotation, mirrored)
                                it.resume(
                                    CombinedCaptureResult(
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
                cameraHandler
            )
        }

    suspend fun saveResult(result: CombinedCaptureResult): File = suspendCancellableCoroutine {
        Timber.d("Saving")
        when (result.format) {
            ImageFormat.JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    val output = createFile(fragment.context!!, "jpg")
//                    val exif = ExifInterface(output)
//                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, result.orientation.toString())
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

    companion object {
        var cameraIds: Map<Int, String>? = null

        const val IMAGE_BUFFER_SIZE = 3
        const val IMAGE_CAPTURE_TIME_LIMIT = 5000L

        data class CombinedCaptureResult(
            val image: Image,
            val metadata: CaptureResult,
            val orientation: Int,
            val format: Int
        ) : Closeable {
            override fun close() {
                image.close()
            }
        }

        fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_SSS", Locale.CHINA)
            return File(context.filesDir, "IMG_${sdf.format(Date())}.$extension")
        }
    }

    override fun close() {
        cameraDevice.close()
    }

    fun destroy() {
        cameraThread.quitSafely()
        imageReaderThread.quitSafely()
    }

}
