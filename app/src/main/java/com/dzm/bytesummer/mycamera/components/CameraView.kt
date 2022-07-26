package com.dzm.bytesummer.mycamera.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import kotlin.math.roundToInt

class CameraView : SurfaceView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private var aspectRatio = 0f

    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Size cannot be negative" }
        aspectRatio = width.toFloat() / height.toFloat()
        Log.d("ShootingFragment", "${aspectRatio}")
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (aspectRatio == 0f)
            setMeasuredDimension(width, height)
        else {
            val newHeight: Int
            val newWidth: Int
            val ratio = if (width > height) aspectRatio else 1f / aspectRatio
            if (width < height * ratio) {
                newWidth = width
                newHeight = (width / ratio).roundToInt()
            } else {
                newHeight = height
                newWidth = (height * aspectRatio).roundToInt()
            }
            setMeasuredDimension(newWidth, newHeight)
        }
    }
}