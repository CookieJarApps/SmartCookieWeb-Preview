package com.cookiejarapps.android.smartcookieweb

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.cookiejarapps.android.smartcookieweb.addons.WebExtensionPopupFragment
import com.cookiejarapps.android.smartcookieweb.browser.*
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.browser.tabs.TabsTrayFragment
import com.cookiejarapps.android.smartcookieweb.ext.alreadyOnDestination
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.ext.nav
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.search.SearchDialogFragmentDirections
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_toolbar.*
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.contextmenu.ext.DefaultSelectionActionDelegate
import mozilla.components.feature.search.ext.legacy
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupFeature


/**
 * Activity that holds the [BrowserFragment].
 */
open class BrowserActivity : AppCompatActivity(), ComponentCallbacks2, NavHostActivity {

    lateinit var browsingModeManager: BrowsingModeManager

    private var isToolbarInflated = false
    private lateinit var navigationToolbar: Toolbar

    private val navHost by lazy {
        supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
    }

    private val externalSourceIntentProcessors by lazy {
        listOf(
            OpenBrowserIntentProcessor(this, ::getIntentSessionId),
            OpenSpecificTabIntentProcessor(this)
        )
    }

    private val webExtensionPopupFeature by lazy {
        WebExtensionPopupFeature(components.store, ::openPopup)
    }

    protected open fun getIntentSessionId(intent: SafeIntent): String? = null

    @VisibleForTesting
    internal fun isActivityColdStarted(startingIntent: Intent, activityIcicle: Bundle?): Boolean {
        return activityIcicle == null && startingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        components.publicSuffixList.prefetch()

        if(UserPreferences(this).firstLaunch){
            UserPreferences(this).firstLaunch = false
        }

        //TODO: Move to settings page so app restart no longer required
        //TODO: Adding search engine to list every time isn't great, but fixes search engine issues
        components.searchUseCases.addSearchEngine(
            SearchEngineList().getEngines()[UserPreferences(
                this
            ).searchEngineChoice]
        )
        components.searchUseCases.selectSearchEngine(
            SearchEngineList().getEngines()[UserPreferences(this).searchEngineChoice]
        )

        browsingModeManager = createBrowsingModeManager(
            if (UserPreferences(this).lastKnownPrivate) BrowsingMode.Private else BrowsingMode.Normal
        )

        if (isActivityColdStarted(
                intent,
                savedInstanceState
            )) {
            navigateToBrowserOnColdStart()
        }

        if(UserPreferences(this).appThemeChoice == ThemeChoice.SYSTEM.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if(UserPreferences(this).appThemeChoice == ThemeChoice.LIGHT.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        val rightDrawer = if(UserPreferences(this).swapDrawers) TabsTrayFragment() else BookmarkFragment()
        val leftDrawer = if(UserPreferences(this).swapDrawers) BookmarkFragment() else TabsTrayFragment()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.right_drawer, rightDrawer)
            commit()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.left_drawer, leftDrawer)
            commit()
        }

        lifecycle.addObserver(webExtensionPopupFeature)
    }

    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleNewIntent(it)
        }
    }

    open fun handleNewIntent(intent: Intent) {
        openToBrowser(BrowserDirection.FromGlobal, null)
        val value = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        if(value != null && !value.toString().startsWith("content://")){
            components.tabsUseCases.addTab.invoke(value.toString())
        }
        else if(value.toString().startsWith("content://")){
            handleInstallAddon(value as Uri)
        }

        val intentProcessors = externalSourceIntentProcessors
        val intentHandled =
            intentProcessors.any { it.process(intent, navHost.navController, this.intent) }
        browsingModeManager.mode = BrowsingMode.Normal

        if (intentHandled) {
            supportFragmentManager
                .primaryNavigationFragment
                ?.childFragmentManager
                ?.fragments
                ?.lastOrNull()
        }
    }

    open fun handleInstallAddon(value: Uri){
        var result: String? = null
        if (value.getScheme().equals("content")) {
            val cursor: Cursor? = contentResolver.query(value, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = value.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result!!.substring(cut + 1)
                }
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.load_xpi)

        builder.setMessage(resources.getString(R.string.load_xpi_description, result))
            .setCancelable(false)
            .setPositiveButton(R.string.mozac_feature_prompts_ok) { dialog, id ->
                components.engine.installWebExtension("", value.toString(), onSuccess = {
                    Toast.makeText(this, "INSTALLED!", Toast.LENGTH_LONG).show()
                },
                onError = { exception, e ->
                   Log.d("gdsgsd", e.stackTraceToString())
                })
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, id ->
                dialog.cancel()
            }

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    open fun navigateToBrowserOnColdStart() {
        if (!browsingModeManager.mode.isPrivate) {
            openToBrowser(BrowserDirection.FromGlobal, null)
        }
    }

    protected open fun createBrowsingModeManager(initialMode: BrowsingMode): BrowsingModeManager {
        return DefaultBrowsingModeManager(initialMode, UserPreferences(this)) {}
    }

    final override fun onBackPressed() {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is UserInteractionHandler && it.onBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.engine.createView(context, attrs).apply {
                selectionActionDelegate = DefaultSelectionActionDelegate(
                    store = components.store,
                    context = context
                )
            }.asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    override fun getSupportActionBarAndInflateIfNecessary(): ActionBar {
        if (!isToolbarInflated) {
            navigationToolbar = navigationToolbarStub.inflate() as Toolbar

            setSupportActionBar(navigationToolbar)
            // Add ids to this that we don't want to have a toolbar back button
            setupNavigationToolbar()

            isToolbarInflated = true
        }
        return supportActionBar!!
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.forEach {
            if (it is ActivityResultHandler && it.onActivityResult(requestCode, data, resultCode)) {
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Suppress("SpreadOperator")
    fun setupNavigationToolbar(vararg topLevelDestinationIds: Int) {
        NavigationUI.setupWithNavController(
            navigationToolbar,
            navHost.navController,
            AppBarConfiguration.Builder(*topLevelDestinationIds).build()
        )

        navigationToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val fm: FragmentManager = supportFragmentManager
        val editNameDialogFragment =
            WebExtensionPopupFragment()

        val bundle = Bundle()
        bundle.putString("web_extension_id", webExtensionState.id)
        intent.putExtra("web_extension_name", webExtensionState.name)

        editNameDialogFragment.arguments = bundle

        editNameDialogFragment.show(fm, "fragment_edit_name")
    }

    @Suppress("LongParameterList")
    fun openToBrowserAndLoad(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        customTabSessionId: String? = null,
        engine: SearchEngine? = null,
        forceSearch: Boolean = false,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none()
    ) {
        openToBrowser(from, customTabSessionId)
        load(searchTermOrURL, newTab, engine, forceSearch, flags)
    }

    fun openToBrowser(from: BrowserDirection, customTabSessionId: String? = null) {
        if (navHost.navController.alreadyOnDestination(R.id.browserFragment)) return
        @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
        val directions = getNavDirections(from, customTabSessionId)
        if (directions != null) {
            navHost.navController.nav(fragmentId, directions)
        }
    }

    protected open fun getNavDirections(
        from: BrowserDirection,
        customTabSessionId: String?
    ): NavDirections? = when (from) {
        BrowserDirection.FromGlobal ->
            NavGraphDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromHome ->
            HomeFragmentDirections.actionGlobalBrowser(customTabSessionId)
        BrowserDirection.FromSearchDialog ->
            SearchDialogFragmentDirections.actionGlobalBrowser(customTabSessionId)
    }

    private fun load(
        searchTermOrURL: String,
        newTab: Boolean,
        engine: SearchEngine?,
        forceSearch: Boolean,
        flags: EngineSession.LoadUrlFlags = EngineSession.LoadUrlFlags.none()
    ) {
        val mode = browsingModeManager.mode

        val loadUrlUseCase = if (newTab) {
            when (mode) {
                BrowsingMode.Private -> components.tabsUseCases.addPrivateTab
                BrowsingMode.Normal -> components.tabsUseCases.addTab
            }
        } else components.sessionUseCases.loadUrl

        if ((!forceSearch && searchTermOrURL.isUrl()) || engine == null) {
            loadUrlUseCase.invoke(searchTermOrURL.toNormalizedUrl(), flags)
        } else {
            if (newTab) {
                components.searchUseCases.newTabSearch
                    .invoke(
                        searchTermOrURL,
                        SessionState.Source.USER_ENTERED,
                        true,
                        mode.isPrivate,
                        searchEngine = engine.legacy()
                    )
            } else {
                components.searchUseCases.defaultSearch.invoke(searchTermOrURL, engine.legacy())
            }
        }
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
    }
}
