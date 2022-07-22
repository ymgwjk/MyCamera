package com.dzm.bytesummer.mycamera

import android.content.Context
import android.graphics.ImageFormat
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.components.CameraView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MyCamera(fragment: Fragment, view: CameraView) {
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
        val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(
                ImageFormat.JPEG
            ).maxByOrNull { it.width * it.height }!!
        imageReader =
            ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, IMAGE_BUFFER_SIZE)
        createCaptureSession(cameraDevice, targets)
    }

    fun initCamera() = fragment.requireView().post {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            val targets = listOf(cameraView.holder.surface)
            prepare(targets)
            startPreview(cameraView)
        }
    }


    suspend fun openCamera() {
        cameraDevice = suspendCancellableCoroutine<CameraDevice> { it ->
            cameraManager.openCamera(getCameraId(), object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    //返回相机设备
                    it.resume(p0)
                }

                override fun onDisconnected(p0: CameraDevice) {
                    fragment.activity!!.finish()
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    p0.close()
                    Timber.e("Error Occurred")
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

                            }
                        }
                    }
                },
                cameraHandler
            )
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
    }
}
