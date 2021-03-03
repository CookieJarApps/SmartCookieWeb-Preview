package com.cookiejarapps.android.smartcookieweb.addons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cookiejarapps.android.smartcookieweb.R


// An activity to manage add-ons.

class AddonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, AddonsFragment())
                commit()
            }
        }
    }
}
