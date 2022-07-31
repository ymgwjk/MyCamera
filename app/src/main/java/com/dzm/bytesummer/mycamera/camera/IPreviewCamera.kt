package com.dzm.bytesummer.mycamera.camera

import androidx.camera.core.Preview

interface IPreviewCamera : CameraBase {
    var previewSurface: Preview.SurfaceProvider?
}