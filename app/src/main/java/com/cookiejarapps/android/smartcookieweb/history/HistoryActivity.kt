package com.cookiejarapps.android.smartcookieweb.history

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutEntity
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.VisitInfo
import android.R.attr.label

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService


class HistoryActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    var history: List<VisitInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        GlobalScope.launch {
            history = components.historyStorage.getDetailedVisits(0).reversed()
            runOnUiThread {
                recyclerView.adapter = HistoryItemRecyclerViewAdapter(
                    history!!
                )
            }
        }

        recyclerView.addOnItemTouchListener(
            HistoryRecyclerViewItemTouchListener(
                this,
                recyclerView,
                object : HistoryRecyclerViewItemTouchListener.OnItemClickListener {
                    override fun onItemClick(view: View?, position: Int) {
                        onBackPressed()
                        GlobalScope.launch {
                            components.sessionUseCases.loadUrl(
                                (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                    position
                                ).url
                            )
                        }
                    }

                    override fun onLongItemClick(view: View?, position: Int) {
                        val items = arrayOf(
                            resources.getString(R.string.open_new),
                            resources.getString(R.string.open_new_private),
                            resources.getString(R.string.mozac_selection_context_menu_share),
                            resources.getString(R.string.copy),
                            resources.getString(R.string.remove_history_item)
                        )

                        MaterialAlertDialogBuilder(this@HistoryActivity)
                            .setTitle(resources.getString(R.string.action_history))
                            .setItems(items) { dialog, which ->
                                when (which) {
                                    0 -> {
                                        onBackPressed()
                                        components.tabsUseCases.addTab.invoke(
                                            (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                position
                                            ).url,
                                            selectTab = true
                                        )
                                    }
                                    1 -> {
                                        onBackPressed()
                                        components.tabsUseCases.addTab.invoke(
                                            (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                position
                                            ).url,
                                            selectTab = true,
                                            private = true
                                        )
                                    }
                                    2 -> {
                                        val shareIntent = Intent(Intent.ACTION_SEND)
                                        shareIntent.type = "text/plain"
                                        shareIntent.putExtra(
                                            Intent.EXTRA_TEXT,
                                            (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                position
                                            ).url
                                        )
                                        ContextCompat.startActivity(
                                            this@HistoryActivity,
                                            Intent.createChooser(
                                                shareIntent,
                                                resources.getString(R.string.mozac_selection_context_menu_share)
                                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            null
                                        )
                                    }
                                    3 -> {
                                        val clipboard = getSystemService(
                                            this@HistoryActivity,
                                            ClipboardManager::class.java
                                        )
                                        val clip = ClipData.newPlainText(
                                            "URL",
                                            (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                position
                                            ).url
                                        )
                                        clipboard?.setPrimaryClip(clip)
                                    }
                                    4 -> {
                                        GlobalScope.launch {
                                            components.historyStorage.deleteVisit(
                                                (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                    position
                                                ).url,
                                                (recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(
                                                    position
                                                ).visitTime
                                            )
                                            history = components.historyStorage.getDetailedVisits(0)
                                                .reversed()
                                            runOnUiThread {
                                                recyclerView.adapter =
                                                    HistoryItemRecyclerViewAdapter(
                                                        history!!
                                                    )
                                                (recyclerView.adapter as HistoryItemRecyclerViewAdapter).notifyItemRemoved(
                                                    position
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            .show()
                    }
                }
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchItem: MenuItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchItem.getActionView() as SearchView
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextChange(query: String?): Boolean {
        (findViewById<RecyclerView>(R.id.list).adapter as HistoryItemRecyclerViewAdapter).getFilter()
            ?.filter(query)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}