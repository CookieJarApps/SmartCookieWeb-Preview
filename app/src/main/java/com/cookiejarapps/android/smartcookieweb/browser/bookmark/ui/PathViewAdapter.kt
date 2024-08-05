package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R

class PathViewAdapter<T : PathView.Path>(context: Context, private val pathView: PathView) : RecyclerView.Adapter<PathViewAdapter.BreadcrumbViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val items = mutableListOf<T>()

    private val leftOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 38.toFloat(), context.resources.displayMetrics)
    private val endPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16.toFloat(), context.resources.displayMetrics)
    private val standardPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.toFloat(), context.resources.displayMetrics)

    val crumbs: List<T>
        get() = items

    var selectedItem: Int = -1
        private set

    fun addItem(item: T) {
        if (selectedItem != items.lastIndex) {
            val next = selectedItem + 1
            if ((items.size > next && items[next] == item)) {
                selectedItem = next
                notifyDataSetChanged()
                pathView.linearLayoutManager.scrollToPositionWithOffset(selectedItem, leftOffset.toInt())
                return
            }
            while (items.size > selectedItem + 1) {
                items.removeAt(items.lastIndex)
            }
        }
        items.add(item)
        selectedItem = items.size - 1
        notifyDataSetChanged()
        pathView.linearLayoutManager.scrollToPositionWithOffset(selectedItem, leftOffset.toInt())
    }

    fun select(index: Int) {
        if (index < 0 || items.size <= index){
            throw IllegalArgumentException()
        }
        selectedItem = index
        notifyDataSetChanged()
        pathView.linearLayoutManager.scrollToPositionWithOffset(selectedItem, leftOffset.toInt())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreadcrumbViewHolder {
        val holder = BreadcrumbViewHolder(inflater.inflate(R.layout.path_item, parent, false))
        holder.title.setOnClickListener { pathView.listener?.onPathItemClick(holder.adapterPosition) }
        return holder
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BreadcrumbViewHolder, position: Int) {
        val item = items[position]
        val textView = holder.title
        textView.text = item.title

        if (position == selectedItem) {
            textView.setTextColor(pathView.highlightedTextColor)
        } else {
            textView.setTextColor(pathView.textColor)
        }

        val padding: Int

        padding = if (position == items.lastIndex){
            endPadding.toInt()
        } else{
            standardPadding.toInt()
        }

        textView.setPadding(standardPadding.toInt(), 0, padding, 0)
    }

    open class BreadcrumbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view as TextView
    }
}