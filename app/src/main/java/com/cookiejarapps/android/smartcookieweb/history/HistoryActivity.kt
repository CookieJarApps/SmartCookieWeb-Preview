package com.cookiejarapps.android.smartcookieweb.history

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        GlobalScope.launch {
            recyclerView.adapter = HistoryItemRecyclerViewAdapter(
                components.historyStorage.getVisited().reversed()
            )
        }

        recyclerView.addOnItemTouchListener(
            HistoryRecyclerViewItemTouchListener(this, recyclerView, object: HistoryRecyclerViewItemTouchListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {
                    onBackPressed()
                    GlobalScope.launch {
                        components.sessionUseCases.loadUrl(components.historyStorage.getVisited().reversed()[position])
                    }
                }

                override fun onLongItemClick(view: View?, position: Int) {
                    // TODO: delete actions
                }
            })
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}