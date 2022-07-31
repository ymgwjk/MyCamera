package com.dzm.bytesummer.mycamera.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface CameraBase {

    var cameraExecutor: ExecutorService

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun closeCamera() {
        cameraExecutor.shutdown()
    }

    fun switch(context: Context, lifecycleOwner: LifecycleOwner)

    companion object {
        var FACE = CameraSelector.DEFAULT_BACK_CAMERA
        fun iStartCamera(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            vararg useCases: UseCase
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = FACE
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector, *useCases
                    )
                } catch (e: Exception) {
                    Timber.e(e.message, e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}