package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.utils.Utils
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.icons.preparer.TippyTopIconPreparer
import mozilla.components.support.ktx.android.net.hostWithoutCommonPrefixes
import okhttp3.internal.wait

internal class ShortcutGridAdapter(
        private val context: Context,
        private val shortcuts: MutableList<ShortcutEntity>
) :
    BaseAdapter() {
    private var layoutInflater: LayoutInflater? = null
    private lateinit var imageView: ImageView
    private lateinit var nameView: TextView
    var list = shortcuts

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
    ): View {
        var convertView = convertView
        if (layoutInflater == null) {
            layoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }
        if (convertView == null) {
            convertView = layoutInflater!!.inflate(R.layout.shortcut_item, null)
        }
        imageView = convertView!!.findViewById(R.id.shortcut_icon)
        nameView = convertView.findViewById(R.id.shortcut_name)

        val protocolUrl = if(shortcuts[position].url!!.startsWith("http")) shortcuts[position].url else "https://" +  shortcuts[position].url

        val iconPlaceholder =
            Utils().createImage(name = getUrlCharacter(protocolUrl!!), context = context)
                .toDrawable(context.resources)
        if(UserPreferences(context).loadShortcutIcons) {
            context.components.icons.loadIntoView(
                imageView,
                IconRequest(protocolUrl),
                iconPlaceholder,
                iconPlaceholder
            )
        } else {
            imageView.setImageDrawable(iconPlaceholder)
        }

        nameView.text = shortcuts[position].title

        return convertView
    }

    private fun getUrlHost(url: String): String {
        val uri = Uri.parse(url)

        val host = uri.hostWithoutCommonPrefixes
        if (!host.isNullOrEmpty()) {
            return host
        }

        val path = uri.path
        if (!path.isNullOrEmpty()) {
            return path
        }

        return url
    }

    internal fun getUrlCharacter(url: String): String {
        val snippet = getUrlHost(url)

        snippet.forEach { character ->
            if (character.isLetterOrDigit()) {
                return character.uppercaseChar().toString()
            }
        }

        return "?"
    }
}