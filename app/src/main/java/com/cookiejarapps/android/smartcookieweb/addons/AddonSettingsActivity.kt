package com.cookiejarapps.android.smartcookieweb.addons

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.settings.ThemeChoice
import com.cookiejarapps.android.smartcookieweb.databinding.ActivityAddOnSettingsBinding
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentAddOnSettingsBinding
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.translateName
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences

// An activity to show the settings of an add-on.

class AddonSettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivityAddOnSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddOnSettingsBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        when (UserPreferences(this).appThemeChoice) {
            ThemeChoice.SYSTEM.ordinal -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            ThemeChoice.LIGHT.ordinal -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        val addon = requireNotNull(intent.getParcelableExtra<Addon>("add_on"))
        title = addon.translateName(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.addonSettingsContainer, AddonSettingsFragment.create(addon))
            .commit()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.engine.createView(context, attrs).asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    // A fragment to show the settings of an add-on with [EngineView].
    class AddonSettingsFragment : Fragment() {
        private lateinit var addon: Addon
        private lateinit var engineSession: EngineSession

        private var _binding: FragmentAddOnSettingsBinding? = null
        protected val binding get() = _binding!!

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            addon = requireNotNull(arguments?.getParcelable("add_on"))
            engineSession = components.engine.createSession()

            _binding = FragmentAddOnSettingsBinding.inflate(inflater, container, false)
            val view = binding.root

            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            binding.addonSettingsEngineView.render(engineSession)
            addon.installedState?.optionsPageUrl?.let {
                engineSession.loadUrl(it)
            }
        }

        override fun onDestroyView() {
            engineSession.close()
            super.onDestroyView()
        }

        companion object {
            // Create an [AddonSettingsFragment] with add_on as a required parameter.
            fun create(addon: Addon) = AddonSettingsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("add_on", addon)
                }
            }
        }
    }
}
