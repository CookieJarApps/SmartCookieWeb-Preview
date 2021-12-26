package com.cookiejarapps.android.smartcookieweb.browser

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.android.synthetic.main.tab_preview.view.*
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.concept.base.images.ImageLoadRequest
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import kotlin.math.max

class FakeTab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val thumbnailLoader = ThumbnailLoader(context.components.thumbnailStorage)

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.tab_preview, this, true)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        previewThumbnail.translationY = if (UserPreferences(context).toolbarPosition != ToolbarPosition.BOTTOM.ordinal) {
            fake_toolbar.height.toFloat()
        } else {
            0f
        }

        if (UserPreferences(context).toolbarPosition != ToolbarPosition.BOTTOM.ordinal) {
             fake_toolbar.updateLayoutParams<LayoutParams> {
                gravity = Gravity.TOP
            }
        }
    }

    fun loadPreviewThumbnail(thumbnailId: String) {
        doOnNextLayout {
            val thumbnailSize = max(previewThumbnail.height, previewThumbnail.width)
            thumbnailLoader.loadIntoView(
                previewThumbnail,
                ImageLoadRequest(thumbnailId, thumbnailSize)
            )
        }
    }
}
