package com.dzm.bytesummer.mycamera.fragment

import android.view.ScaleGestureDetector

interface ZoomEnabledCameraFragment {
    var scaleDetector: ScaleGestureDetector
    var zoomRatio: Float
}