package com.cookiejarapps.android.smartcookieweb.popup

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.cookiejarapps.android.smartcookieweb.R
import javax.inject.Inject

class CustomAdapter(private val mContext: Context, private val Title: Array<String>, private val imge: IntArray) : BaseAdapter() {

    override fun getCount(): Int {
        // TODO Auto-generated method stub
        return Title.size
    }

    override fun getItemId(position: Int): Long {
        // TODO Auto-generated method stub
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val row: View
        row = inflater.inflate(R.layout.menu_row, parent, false)
        val title: TextView
        val i1: ImageView
        i1 = row.findViewById<View>(R.id.imgIcon) as ImageView
        title = row.findViewById<View>(R.id.txtTitle) as TextView
        title.textSize = 14f
        title.text = Title[position]
        i1.setImageResource(imge[position])
        return row
    }

    override fun getItem(position: Int): Any {
        return Title.get(position)
    }

}