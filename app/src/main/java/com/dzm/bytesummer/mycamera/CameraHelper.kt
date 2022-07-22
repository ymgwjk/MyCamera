package com.dzm.bytesummer.mycamera

import android.content.Context
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraHelper {

    companion object {
        class SmartSize(width: Int, height: Int) {
            var size = Size(width, height)
            var long = max(width, height)
            var short = min(width, height)
            override fun toString(): String {
                return "(${long}, ${short})"
            }
        }

        private val SIZE_1080P = SmartSize(1080, 1440)

        @Suppress("DEPRECATION")
        fun getDisplaySize(context: Context): SmartSize {
            val point = Point()
            context.display!!.getSize(point)
            return SmartSize(point.x, point.y)
        }

        fun getPreviewSize(
            context: Context,
            characteristics: CameraCharacteristics,
            targetClass: Class<*>
        ): Size {
            val displaySize = getDisplaySize(context)
            val maxSize =
                if (displaySize.long >= SIZE_1080P.long || displaySize.short >= SIZE_1080P.short) SIZE_1080P
                else displaySize

            val configurationMap =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
            val sizes = configurationMap.getOutputSizes(targetClass)
            val validSizes = sizes.sortedWith(compareBy { it.width * it.height })
                .map { SmartSize(it.width, it.height) }.reversed()
            var resultSize: Size
            try {
                resultSize =
                    validSizes.first { abs(it.long.toFloat() / it.short.toFloat() - maxSize.long.toFloat() / maxSize.short.toFloat()) <= 1e-6 }.size
            } catch (e: NoSuchElementException) {
                resultSize =
                    validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
            }
            return resultSize
        }
    }
}