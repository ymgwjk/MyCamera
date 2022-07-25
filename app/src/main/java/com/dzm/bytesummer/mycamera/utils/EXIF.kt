package com.dzm.bytesummer.mycamera.utils

import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber

fun getExifOrientation(rotation: Int, mirrored: Boolean) = when {
    rotation == 0 && !mirrored -> ExifInterface.ORIENTATION_NORMAL
    rotation == 0 && mirrored -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
    rotation == 180 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_180
    rotation == 180 && mirrored -> ExifInterface.ORIENTATION_FLIP_VERTICAL
    rotation == 90 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_90
    rotation == 90 && mirrored -> ExifInterface.ORIENTATION_TRANSPOSE
    rotation == 270 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_270
    rotation == 270 && mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
    else -> ExifInterface.ORIENTATION_UNDEFINED
}

fun getTransformMatrix(exifRotation: Int): Matrix {
    val matrix = Matrix()
    when (exifRotation) {
        ExifInterface.ORIENTATION_NORMAL -> Unit
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postScale(-1f, 1f)
            matrix.postRotate(-90F)
        }
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postScale(-1f, 1f)
            matrix.postRotate(90f)
        }
        ExifInterface.ORIENTATION_UNDEFINED -> Unit
        else -> Timber.e("Invalid orientation: ${exifRotation}")
    }
    return matrix
}