package com.cookiejarapps.android.smartcookieweb.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.settings.ThemeChoice
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapter
import mozilla.components.feature.addons.ui.UnsupportedAddonsAdapterDelegate
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences

// Activity for managing unsupported add-ons, or add-ons that were installed but are no longer available.

class NotYetSupportedAddonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val addons = requireNotNull(intent.getParcelableArrayListExtra<Addon>("add_ons"))

        if(UserPreferences(this).appThemeChoice == ThemeChoice.SYSTEM.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if(UserPreferences(this).appThemeChoice == ThemeChoice.LIGHT.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, NotYetSupportedAddonFragment.create(addons))
            .commit()
    }

    // Fragment for managing add-ons that are not yet supported by the browser.
    class NotYetSupportedAddonFragment : Fragment(), UnsupportedAddonsAdapterDelegate {
        private lateinit var addons: List<Addon>
        private var adapter: UnsupportedAddonsAdapter? = null

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            addons = requireNotNull(arguments?.getParcelableArrayList("add_ons"))
            return inflater.inflate(R.layout.fragment_other_addons, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val context = requireContext()
            val recyclerView: RecyclerView = view.findViewById(R.id.unsupported_add_ons_list)
            adapter = UnsupportedAddonsAdapter(
                addonManager = context.components.addonManager,
                unsupportedAddonsAdapterDelegate = this@NotYetSupportedAddonFragment,
                addons = addons
            )

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
        }

        override fun onUninstallError(addonId: String, throwable: Throwable) {
            Toast.makeText(context, "Failed to remove add-on", Toast.LENGTH_SHORT).show()
        }

        override fun onUninstallSuccess() {
            Toast.makeText(context, "Successfully removed add-on", Toast.LENGTH_SHORT)
                .show()
            if (adapter?.itemCount == 0) {
                activity?.onBackPressed()
            }
        }

        companion object {
            fun create(addons: ArrayList<Addon>) = NotYetSupportedAddonFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("add_ons", addons)
                }
            }
        }
    }
}
