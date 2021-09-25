package com.cookiejarapps.android.smartcookieweb.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.icons.IconRequest


open class Utils {

    /**
     * Generates letter bitmap in style of Mozac [DefaultIconGenerator] using first letter of string instead of URL
     */
    fun createImage(context: Context, name: String): Bitmap {
        val size = context.resources.getDimension(R.dimen.mozac_browser_icons_size_default)
        val sizePx = size.toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundColor = Color.LTGRAY

        val paint = Paint()
        paint.color = backgroundColor

        val sizeRect = RectF(0f, 0f, size, size)
        val cornerRadius = context.resources.getDimension(R.dimen.mozac_browser_icons_generator_default_corner_radius)
        canvas.drawRoundRect(sizeRect, cornerRadius, cornerRadius, paint)

        val character = name.first().toString()

        // The text size is calculated dynamically based on the target icon size (1/8th). For an icon
        // size of 112dp we'd use a text size of 14dp (112 / 8).
        val textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                size * 1 / 8f,
                context.resources.displayMetrics
        )

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = textSize
        paint.isAntiAlias = true

        canvas.drawText(
                character,
                canvas.width / 2f,
                (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f),
                paint
        )

        return bitmap
    }

    fun isTablet(context: Context): Boolean {
        return ((context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE)
    }
}