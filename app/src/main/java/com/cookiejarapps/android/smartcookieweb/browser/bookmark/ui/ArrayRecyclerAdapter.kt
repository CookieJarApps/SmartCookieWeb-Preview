package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.*


abstract class ArrayRecyclerAdapter<T, VH : ArrayRecyclerAdapter.ArrayViewHolder<T>>(
    context: Context,
    val items: MutableList<T>,
    private var listener: BookmarkRecyclerViewClickInterface?
) : RecyclerView.Adapter<VH>() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var sortMode: Boolean = true

    private val itemSelected: SparseBooleanArray = SparseBooleanArray()

    var selectedItemCount: Int = 0
        private set

    val selectedItems: List<Int>
        get() {
            return (0 until itemSelected.size())
                .asSequence()
                .filter { itemSelected.valueAt(it) }
                .map { itemSelected.keyAt(it) }
                .toList()
        }

    open fun onBindViewHolder(holder: VH, item: T, position: Int) {}

    protected abstract fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): VH

    fun move(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun size(): Int {
        return items.size
    }

    open operator fun get(index: Int): T {
        return items[index]
    }

    fun add(item: T) {
        items.add(item)
    }

    fun add(index: Int, item: T) {
        items.add(index, item)
    }

    operator fun set(index: Int, item: T): T {
        return items.set(index, item)
    }

    fun remove(index: Int): T {
        val item = items.removeAt(index)
        notifyItemRemoved(index)
        return item
    }

    fun clear() {
        items.clear()
    }

    protected fun setRecyclerListener(listener: BookmarkRecyclerViewClickInterface) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return onCreateViewHolder(inflater, parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pos = holder.adapterPosition
        if (items.size > pos && items[pos] != null) {
            holder.setUp(items[pos])
            onBindViewHolder(holder, items[pos], pos)
        }
    }

    protected open fun onItemClick(v: View, position: Int, item: T?) {
        var calPosition = position
        calPosition = searchPosition(calPosition, item)
        if (calPosition < 0) return
        listener?.onRecyclerItemClicked(v, calPosition)
    }

    private fun onItemLongClick(v: View, position: Int, item: T?): Boolean {
        var calPosition = position
        calPosition = searchPosition(calPosition, item)
        return calPosition >= 0 && !sortMode && listener?.onRecyclerItemLongClicked(v, calPosition) ?: false
    }

    protected fun searchPosition(position: Int, item: T?): Int {
        var calPosition = position
        if (calPosition < 0 || calPosition >= itemCount || get(calPosition) != item) {
            if (calPosition > 0 && get(calPosition - 1) == item){
                return calPosition - 1
            }

            calPosition = items.indexOf(item)
            if (calPosition < 0) {
                notifyDataSetChanged()
            }
            return calPosition
        }
        return calPosition
    }

    open fun toggle(position: Int) {
        setSelect(position, !itemSelected.get(position, false))
    }

    open fun setSelect(position: Int, isSelect: Boolean) {
        val old = itemSelected.get(position, false)
        itemSelected.put(position, isSelect)

        if (old != isSelect) {
            notifyItemChanged(position)
            if (isSelect) selectedItemCount++ else selectedItemCount--
        }
    }

    fun isSelected(position: Int): Boolean {
        return itemSelected.get(position, false)
    }

    open class ArrayViewHolder<I>(itemView: View, adapter: ArrayRecyclerAdapter<I, *>) : RecyclerView.ViewHolder(itemView) {

        private var target: I? = null
        protected val item: I
            get() = target ?: throw NullPointerException()

        init {
            itemView.setOnClickListener { v -> adapter.onItemClick(v, adapterPosition, item) }

            itemView.setOnLongClickListener { v -> adapter.onItemLongClick(v, adapterPosition, item) }
        }

        open fun setUp(item: I) {
            target = item
        }
    }
}