package com.cookiejarapps.android.smartcookieweb.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryActivity: AppCompatActivity(), SearchView.OnQueryTextListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        GlobalScope.launch {
            val history = components.historyStorage.getDetailedVisits(0).reversed()
            runOnUiThread {
                recyclerView.adapter = HistoryItemRecyclerViewAdapter(
                    history
                )
            }
        }

        recyclerView.addOnItemTouchListener(
            HistoryRecyclerViewItemTouchListener(this, recyclerView, object: HistoryRecyclerViewItemTouchListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {
                    onBackPressed()
                    GlobalScope.launch {
                        components.sessionUseCases.loadUrl((recyclerView.adapter as HistoryItemRecyclerViewAdapter).getItem(position).url)
                    }
                }

                override fun onLongItemClick(view: View?, position: Int) {
                    // TODO: delete actions
                }
            })
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
        (findViewById<RecyclerView>(R.id.list).adapter as HistoryItemRecyclerViewAdapter).getFilter()?.filter(query)
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