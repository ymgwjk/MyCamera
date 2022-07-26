package com.dzm.bytesummer.mycamera

import android.content.Context
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.dzm.bytesummer.mycamera.utils.OrientationLiveData
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MyCamera : Closeable {

    private val cameraManager: CameraManager

    var cameraFace: Int = 0
        set(face) {
            if (field != face) {
                characteristics = cameraManager.getCameraCharacteristics(getCameraId())
                Global.cameraFace = face
            }
            field = face
        }

    var characteristics: CameraCharacteristics


    lateinit var cameraDevice: CameraDevice
        private set

    lateinit var session: CameraCaptureSession
        private set

    //相机线程
    val cameraThread = HandlerThread("CameraThread").apply { start() }
    val cameraHandler = Handler(cameraThread.looper)


    init {
        cameraManager =
            Global.activity!!.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        characteristics = cameraManager.getCameraCharacteristics(getCameraId())
        cameraFace = Global.cameraFace
    }


    fun getPreviewSize(context: Context, targetClass: Class<*>) =
        CameraHelper.getPreviewSize(context, characteristics, targetClass)


    fun getCameraId(): String {
        return cameraIds!![cameraFace]!!
    }

    fun reverseFace() {
        if (cameraFace == CameraCharacteristics.LENS_FACING_BACK) {
            cameraFace = CameraCharacteristics.LENS_FACING_FRONT
        } else if (cameraFace == CameraCharacteristics.LENS_FACING_FRONT)
            cameraFace = CameraCharacteristics.LENS_FACING_BACK
    }


    suspend fun open() {
        cameraDevice = suspendCancellableCoroutine {
            cameraManager.openCamera(getCameraId(), object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    //返回相机设备
                    it.resume(p0)
                }

                override fun onDisconnected(p0: CameraDevice) {
                    Global.activity!!.finish()
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
        targets: List<Surface>,
    ) {
        session = suspendCancellableCoroutine {
            cameraDevice.createCaptureSession(
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

        const val USE_MIRROR = false
    }

    override fun close() {
        cameraDevice.close()
        session.close()
    }

}
