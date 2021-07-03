package com.cookiejarapps.android.smartcookieweb

import android.app.ProgressDialog
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.ActionBar
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
import com.cookiejarapps.android.smartcookieweb.utils.PrintUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.feature.contextmenu.ext.DefaultSelectionActionDelegate
import mozilla.components.feature.search.ext.createSearchEngine
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import mozilla.components.support.ktx.kotlinx.coroutines.flow.filterChanged
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

    private var printExtension: mozilla.components.concept.engine.webextension.WebExtension? =
        null

    protected open fun getIntentSessionId(intent: SafeIntent): String? = null

    @VisibleForTesting
    internal fun isActivityColdStarted(startingIntent: Intent, activityIcicle: Bundle?): Boolean {
        return activityIcicle == null && startingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        components.publicSuffixList.prefetch()

        browsingModeManager = createBrowsingModeManager(
            if (UserPreferences(this).lastKnownPrivate) BrowsingMode.Private else BrowsingMode.Normal
        )

        setContentView(R.layout.activity_main)

        if(UserPreferences(this).firstLaunch){
            UserPreferences(this).firstLaunch = false
        }
        //TODO: remove this once most people have updated
        if(UserPreferences(this).showTabsInGrid && UserPreferences(this).stackFromBottom) UserPreferences(this).stackFromBottom = false

        //TODO: Move to settings page so app restart no longer required
        //TODO: Differentiate between using search engine / adding to list - the code below removes all from list as I don't support adding to list, only setting as default
        for(i in components.store.state.search.customSearchEngines){
            components.searchUseCases.removeSearchEngine(i)
        }

        if(UserPreferences(this).customSearchEngine){
            GlobalScope.launch {
                val customSearch =
                    createSearchEngine(
                        name = "Custom Search",
                        url = UserPreferences(this@BrowserActivity).customSearchEngineURL,
                        icon = components.icons.loadIcon(IconRequest(UserPreferences(this@BrowserActivity).customSearchEngineURL))
                            .await().bitmap
                    )

                runOnUiThread {
                    components.searchUseCases.addSearchEngine(
                        customSearch
                    )
                    components.searchUseCases.selectSearchEngine(
                        customSearch
                    )
                }
            }
        }
        else{
            if(SearchEngineList().getEngines()[UserPreferences(this).searchEngineChoice].type == SearchEngine.Type.BUNDLED){
                components.searchUseCases.selectSearchEngine(
                    SearchEngineList().getEngines()[UserPreferences(this).searchEngineChoice]
                )
            }
            else{
                components.searchUseCases.addSearchEngine(
                    SearchEngineList().getEngines()[UserPreferences(
                        this
                    ).searchEngineChoice]
                )
                components.searchUseCases.selectSearchEngine(
                    SearchEngineList().getEngines()[UserPreferences(this).searchEngineChoice]
                )
            }
        }

        if (isActivityColdStarted(intent, savedInstanceState) &&
            !externalSourceIntentProcessors.any { it.process(intent, navHost.navController, this.intent) }) {
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

        // TODO: test performance impact
        installPrintExtension()

        lifecycle.addObserver(webExtensionPopupFeature)
    }

    final override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // TODO: temporary fix
        openToBrowser(BrowserDirection.FromGlobal)
        intent?.let {
            handleNewIntent(it)
        }
    }

    open fun handleNewIntent(intent: Intent) {
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
                            searchEngine = engine
                    )
            } else {
                components.searchUseCases.defaultSearch.invoke(searchTermOrURL, engine)
            }
        }
    }

    private fun installPrintExtension(){

        val messageDelegate: MessageHandler = object :
            MessageHandler {
            override fun onMessage(
                message: Any, source: EngineSession?
            ): Any {
                val converter = PrintUtils.instance
                val htmlString = message.toString()
                converter!!.convert(this@BrowserActivity, htmlString, components.sessionManager.selectedSession?.url)
                printExtension?.let { components.engine.disableWebExtension(it, onSuccess = {}) }

                return ""
            }
        }

        components.engine.installWebExtension(
            "print@cookiejarapps.com",
            "resource://android/assets/print/",
            onSuccess = { extension ->
                extension?.let { components.engine.disableWebExtension(it, onSuccess = {}) }
                printExtension = extension
                val store = components.store
                store.flowScoped { flow ->
                    flow.map { it.tabs }
                        .filterChanged { it.engineState.engineSession }
                        .collect { state ->
                            val session = state.engineState.engineSession ?: return@collect

                            extension.registerContentMessageHandler(
                                session,
                                "browser",
                                messageDelegate
                            )
                        }
                }
            }
        )
    }

    fun printPage(){
        // Reload page and enable add-on at the same time to load the add-on, then reload again to trigger add-on on page
        printExtension?.let { components.engine.enableWebExtension(it, onSuccess = {
            components.sessionUseCases.stopLoading.invoke()
            components.sessionUseCases.reload.invoke()
        }
        )}
    }

    companion object {
        const val OPEN_TO_BROWSER = "open_to_browser"
    }
}
