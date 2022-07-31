package com.dzm.bytesummer.mycamera

import android.hardware.camera2.CameraCharacteristics
import android.net.Uri

object Global {
    var cameraFace: Int = CameraCharacteristics.LENS_FACING_BACK
    var uriList = mutableListOf<Uri>()
}