package com.cookiejarapps.android.smartcookieweb.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import java.util.*

open class HistoryItemRecyclerViewAdapter(
    private var values: List<String>
)
    : RecyclerView.Adapter<HistoryItemRecyclerViewAdapter.ViewHolder>() {

    lateinit var filtered: MutableList<String>
    lateinit var oldList: MutableList<String>

    open fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()

                filtered = if (charString.isEmpty()) {
                    oldList
                } else {
                    val filteredList: MutableList<String> = ArrayList<String>()
                    for (row in oldList) {
                        if (row.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row)
                        }
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = filtered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                values = filterResults.values as MutableList<String>
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_list_item, parent, false)
        oldList = values as MutableList<String>
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.contentView.text = item
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contentView: TextView = view.findViewById(R.id.content)

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }
}