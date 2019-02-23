package com.rizlee.rangeseekbar.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object BitmapUtil {

    fun toBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        drawable.apply {
            val width = if (!bounds.isEmpty) bounds.width() else intrinsicWidth
            val height = if (!bounds.isEmpty) bounds.height() else intrinsicHeight

            val bitmap = Bitmap.createBitmap(if (width <= 0) 1 else width, if (height <= 0) 1 else height,
                    Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                setBounds(0, 0, width, height)
                draw(this)
            }
            return bitmap
        }
    }
}