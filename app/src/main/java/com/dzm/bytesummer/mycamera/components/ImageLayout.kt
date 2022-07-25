package com.dzm.bytesummer.mycamera.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout

class ImageLayout : LinearLayout {

    private var numOneLine = 0
    private var nowLine: LinearLayout? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun addView(child: View?) {
        if (nowLine == null || numOneLine == 3) {
            nowLine = addLine()
            numOneLine = 0
            super.addView(nowLine)
        }
        nowLine!!.addView(child)
        numOneLine++
    }

    private fun addLine(): LinearLayout {
        val lineLayout = LinearLayout(context)
        lineLayout.orientation = HORIZONTAL
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lineLayout.layoutParams = lp
        return lineLayout
    }
}