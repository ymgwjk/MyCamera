package com.dzm.bytesummer.mycamera.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController


class PermissionFragment : Fragment() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                navigateToPhoto()
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermission(requireContext())) {
            navigateToPhoto()
        } else {
            permissionsLauncher.launch(PERMISSION_REQUIRED)
        }
    }

    private fun navigateToPhoto() {
        val cameraManager =
            requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = enumerateCameras(cameraManager)
        if (cameraIds[CameraCharacteristics.LENS_FACING_BACK] != null && cameraIds[CameraCharacteristics.LENS_FACING_FRONT] != null) {
//            MyCamera.cameraIds = cameraIds
            lifecycleScope.launchWhenStarted {
                findNavController().navigate(
                    PermissionFragmentDirections.permissionToCamera()
                )
            }
        } else
            Toast.makeText(
                context,
                "Sorry, your camera cannot meet the require.",
                Toast.LENGTH_LONG
            ).show()
    }

    companion object {
        private val PERMISSION_REQUIRED =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

        private fun hasPermission(context: Context): Boolean {
            return PERMISSION_REQUIRED.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        private fun enumerateCameras(cameraManager: CameraManager): HashMap<Int, String> {
            val availableCameras = HashMap<Int, String>()
            val cameraIds = cameraManager.cameraIdList.filter {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val capabilities =
                    characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
                    ?: false
            }
            cameraIds.forEach { cameraID ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraID)
                if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT) {
                    availableCameras[CameraCharacteristics.LENS_FACING_FRONT] = cameraID
                }
                if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK) {
                    availableCameras[CameraCharacteristics.LENS_FACING_BACK] = cameraID
                }
            }
            return availableCameras
        }
    }
}