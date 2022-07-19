package com.dzm.bytesummer.mycamera.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dzm.bytesummer.mycamera.R


class PermissionFragment : Fragment() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                lifecycleScope.launchWhenStarted { findNavController().navigate(R.id.permission_to_camera) }
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasPermission(requireContext())) {
            lifecycleScope.launchWhenStarted { findNavController().navigate(R.id.permission_to_camera) }
        } else {
            permissionsLauncher.launch(PERMISSION_REQUIRED)
        }
    }

    companion object {
        private val PERMISSION_REQUIRED = arrayOf(Manifest.permission.CAMERA)
        private fun hasPermission(context: Context): Boolean {
            return PERMISSION_REQUIRED.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}