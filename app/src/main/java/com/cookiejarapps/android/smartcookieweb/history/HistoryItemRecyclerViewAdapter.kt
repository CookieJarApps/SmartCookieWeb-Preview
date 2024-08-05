package com.cookiejarapps.android.smartcookieweb.history

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.concept.storage.VisitInfo
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import okhttp3.internal.toCanonicalHost
import java.text.DateFormat.getDateTimeInstance
import java.text.SimpleDateFormat
import java.util.*

open class HistoryItemRecyclerViewAdapter(
    private var values: List<VisitInfo>
)
    : RecyclerView.Adapter<HistoryItemRecyclerViewAdapter.ViewHolder>() {

    lateinit var filtered: MutableList<VisitInfo>
    lateinit var oldList: MutableList<VisitInfo>

    open fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()

                filtered = if (charString.isEmpty()) {
                    oldList
                } else {
                    val filteredList: MutableList<VisitInfo> = ArrayList<VisitInfo>()
                    for (row in oldList) {
                        if (row.url.lowercase(Locale.getDefault()).contains(charString.lowercase(Locale.getDefault())) || row.title?.lowercase(
                                Locale.getDefault()
                            )
                                ?.contains(
                                charString.lowercase(Locale.getDefault())
                            ) == true) {
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
                values = filterResults.values as MutableList<VisitInfo>
                notifyDataSetChanged()
            }
        }
    }

    fun getItem(position: Int) = values[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_list_item, parent, false)
        oldList = values as MutableList<VisitInfo>
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]

        val date = Date(item.visitTime)
        val format = getDateTimeInstance()

        val title = item.title ?.takeIf(String::isNotEmpty) ?: item.url.tryGetHostFromUrl()

        holder.titleView.text = title
        holder.urlView.text = item.url
        holder.timeView.text = format.format(date)
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.historyTitle)
        val urlView: TextView = view.findViewById(R.id.historyUrl)
        val timeView: TextView = view.findViewById(R.id.historyTime)
    }
}