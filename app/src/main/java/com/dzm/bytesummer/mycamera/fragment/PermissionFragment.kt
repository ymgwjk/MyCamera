package com.dzm.bytesummer.mycamera.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PermissionFragment : Fragment() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                navigateToPhoto()
            } else {
                result.filter { !it.value }.forEach {
                    Toast.makeText(
                        context,
                        "Permission request denied: ${it.key}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                activity!!.finish()
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
        lifecycleScope.launch(Dispatchers.Main) {
            findNavController().navigate(PermissionFragmentDirections.permissionToCamera())
        }
    }

    companion object {
        private val PERMISSION_REQUIRED =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }.toTypedArray()

        private fun hasPermission(context: Context): Boolean {
            return PERMISSION_REQUIRED.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}