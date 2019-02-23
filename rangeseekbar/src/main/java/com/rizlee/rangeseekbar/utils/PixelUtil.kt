package com.rizlee.rangeseekbar.utils

import android.content.Context
import android.util.DisplayMetrics

object PixelUtil {

    fun dpToPx(context: Context, dp: Int) = Math.round(dp * getPixelScaleFactor(context))

    fun pxToDp(context: Context, px: Int) = Math.round(px / getPixelScaleFactor(context))

    private fun getPixelScaleFactor(context: Context) = context.resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT
}