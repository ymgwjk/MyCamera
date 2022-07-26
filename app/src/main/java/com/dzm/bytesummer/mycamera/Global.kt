package com.dzm.bytesummer.mycamera

import android.hardware.camera2.CameraCharacteristics

object Global {
    var activity: MainActivity? = null
    var cameraFace: Int = CameraCharacteristics.LENS_FACING_BACK
}