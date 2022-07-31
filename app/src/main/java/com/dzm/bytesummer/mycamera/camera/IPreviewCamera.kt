package com.dzm.bytesummer.mycamera.camera

import android.view.Surface
import androidx.camera.core.Preview
import com.dzm.bytesummer.mycamera.camera.CameraBase

interface IPreviewCamera : CameraBase {
    var previewSurface: Preview.SurfaceProvider?
}