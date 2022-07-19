package com.dzm.bytesummer.mycamera.fragment

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.dzm.bytesummer.mycamera.databinding.FragmentCameraBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CameraFragment : Fragment() {
    //日志tag
    private val TAG = CameraFragment::class.java.simpleName

    //视图绑定
    private lateinit var fragmentCameraBinding: FragmentCameraBinding


    private val cameraManager: CameraManager by lazy {
        requireContext().applicationContext.getSystemService(
            CameraManager::class.java
        )
    }

    private lateinit var cameraDevice: CameraDevice

    //前后置摄像头
    private lateinit var frontID: String
    private lateinit var backID: String
    private lateinit var frontcharacteristic: CameraCharacteristics
    private lateinit var backcharacteristic: CameraCharacteristics

    //相机线程
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        val cameraIDList = cameraManager.cameraIdList
        cameraIDList.forEach { cameraID ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraID)
            if (characteristics.isHardwareLevelSupported(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)) {
                if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontID = cameraID
                    frontcharacteristic = characteristics
                } else if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK) {
                    backID = cameraID
                    backcharacteristic = characteristics
                }
            }
        }
        return fragmentCameraBinding.root
    }

    private suspend fun openCamera(
        manager: CameraManager,
        cameraID: String,
        handler: Handler? = null
    ): CameraDevice {
        return suspendCancellableCoroutine<CameraDevice> { it ->
            manager.openCamera(cameraID, object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    //返回相机设备
                    it.resume(p0)
                }

                override fun onDisconnected(p0: CameraDevice) {
                    requireActivity().finish()
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    p0.close()
                    Log.e(TAG, "Error Occurred")
                }
            }, handler)
        }
    }

    private fun initCamera() = lifecycleScope.launch(Dispatchers.Main) {
        cameraDevice = openCamera(cameraManager, backID, cameraHandler)


    }
    companion object{
        fun CameraCharacteristics.isHardwareLevelSupported(requiredLevel: Int): Boolean {
            val levels = intArrayOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            )
            val deviceLevel = this[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            if (requiredLevel == deviceLevel) return true
            for (level in levels) {
                if (requiredLevel == level) return true
                else if (deviceLevel == level) return false
            }
            return false
        }
    }
}