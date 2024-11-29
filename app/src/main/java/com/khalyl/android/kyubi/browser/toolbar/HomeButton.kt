package com.khalyl.android.kyubi.browser.toolbar

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class HomeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    init {
        // Set default properties for an icon-only button
        text = "" // Remove text
        isClickable = true
        isFocusable = true
    }
}
