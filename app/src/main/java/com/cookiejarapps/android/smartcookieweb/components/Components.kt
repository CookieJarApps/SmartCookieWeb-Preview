package com.cookiejarapps.android.smartcookieweb.components

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_USER_PRESENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.startActivity
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BuildConfig
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.addons.AddonsActivity
import com.cookiejarapps.android.smartcookieweb.browser.ThemeChoice
import com.cookiejarapps.android.smartcookieweb.downloads.DownloadService
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.media.MediaSessionService
import com.cookiejarapps.android.smartcookieweb.settings.activity.SettingsActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.menu.WebExtensionBrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuCheckbox
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.engine.EngineMiddleware
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.thumbnails.ThumbnailsMiddleware
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.mediaquery.PreferredColorScheme
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.amo.AddonCollectionProvider
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import mozilla.components.feature.addons.migration.SupportedAddonsChecker
import mozilla.components.feature.addons.update.AddonUpdater
import mozilla.components.feature.addons.update.DefaultAddonUpdater
import mozilla.components.feature.app.links.AppLinksInterceptor
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.autofill.AutofillConfiguration
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.customtabs.CustomTabIntentProcessor
import mozilla.components.feature.customtabs.store.CustomTabsServiceStore
import mozilla.components.feature.downloads.DownloadMiddleware
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.intent.processing.TabIntentProcessor
import mozilla.components.feature.media.MediaSessionFeature
import mozilla.components.feature.media.middleware.RecordingDevicesMiddleware
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.WebAppInterceptor
import mozilla.components.feature.pwa.WebAppShortcutManager
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.pwa.intent.TrustedWebActivityIntentProcessor
import mozilla.components.feature.pwa.intent.WebAppIntentProcessor
import mozilla.components.feature.readerview.ReaderViewMiddleware
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.toDefaultSearchEngineProvider
import mozilla.components.feature.search.middleware.SearchMiddleware
import mozilla.components.feature.search.region.RegionMiddleware
import mozilla.components.feature.session.HistoryDelegate
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.middleware.LastAccessMiddleware
import mozilla.components.feature.session.middleware.undo.UndoMiddleware
import mozilla.components.feature.sitepermissions.SitePermissionsStorage
import mozilla.components.feature.tabs.CustomTabsUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.webcompat.WebCompatFeature
import mozilla.components.feature.webnotifications.WebNotificationFeature
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.service.digitalassetlinks.local.StatementApi
import mozilla.components.service.digitalassetlinks.local.StatementRelationChecker
import mozilla.components.service.location.LocationService
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.request.AppRequestInterceptor
import com.cookiejarapps.android.smartcookieweb.utils.ClipboardHandler
import mozilla.components.browser.session.ext.toTabSessionState
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import java.util.concurrent.TimeUnit


private const val DAY_IN_MINUTES = 24 * 60L

@Suppress("LargeClass")
open class Components(private val applicationContext: Context) {
    companion object {
        const val BROWSER_PREFERENCES = "browser_preferences"
        const val PREF_LAUNCH_EXTERNAL_APP = "launch_external_app"
    }

    val publicSuffixList by lazy { PublicSuffixList(applicationContext) }

    val clipboardHandler by lazy { ClipboardHandler(applicationContext) }

    val preferences: SharedPreferences =
            applicationContext.getSharedPreferences(BROWSER_PREFERENCES, Context.MODE_PRIVATE)


    fun darkEnabled(): PreferredColorScheme {
        val darkOn =
                (applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
        return when {
            UserPreferences(applicationContext).themeChoice == ThemeChoice.DARK.ordinal -> PreferredColorScheme.Dark
            UserPreferences(applicationContext).themeChoice == ThemeChoice.LIGHT.ordinal -> PreferredColorScheme.Light
            darkOn -> PreferredColorScheme.Dark
            else -> PreferredColorScheme.Light
        }
    }

    // Engine Settings
    val engineSettings by lazy {
        DefaultSettings().apply {
            historyTrackingDelegate = HistoryDelegate(lazyHistoryStorage)
            requestInterceptor = AppRequestInterceptor(applicationContext)
            remoteDebuggingEnabled = true
            supportMultipleWindows = true
            preferredColorScheme = darkEnabled()
            javascriptEnabled = UserPreferences(applicationContext).javaScriptEnabled
        }
    }

    val addonUpdater =
            DefaultAddonUpdater(applicationContext, AddonUpdater.Frequency(1, TimeUnit.DAYS))

    // Engine
    open val engine: Engine by lazy {
        GeckoEngine(applicationContext, engineSettings, runtime).also {
            WebCompatFeature.install(it)
        }
    }

    open val client: Client by lazy { GeckoViewFetchClient(applicationContext, runtime) }

    val icons by lazy { BrowserIcons(applicationContext, client) }

    // Storage
    private val lazyHistoryStorage = lazy { PlacesHistoryStorage(applicationContext) }
    val historyStorage by lazy { lazyHistoryStorage.value }

    val sessionStorage by lazy { SessionStorage(applicationContext, engine) }

    val permissionStorage by lazy { SitePermissionsStorage(applicationContext) }

    val thumbnailStorage by lazy { ThumbnailStorage(applicationContext) }

    val store by lazy {
        BrowserStore(
                middleware = listOf(
                        DownloadMiddleware(applicationContext, DownloadService::class.java),
                        ReaderViewMiddleware(),
                        ThumbnailsMiddleware(thumbnailStorage),
                        UndoMiddleware(::sessionManagerLookup),
                        RegionMiddleware(
                                applicationContext,
                                LocationService.default()
                        ),
                        SearchMiddleware(applicationContext),
                        RecordingDevicesMiddleware(applicationContext),
                        LastAccessMiddleware()
                ) + EngineMiddleware.create(engine, ::findSessionById)
        )
    }

    private fun findSessionById(tabId: String): Session? {
        return sessionManager.findSessionById(tabId)
    }

    private fun sessionManagerLookup(): SessionManager {
        return sessionManager
    }

    val sessionManager by lazy {
        SessionManager(engine, store).apply {
            icons.install(engine, store)

            WebNotificationFeature(
                    applicationContext, engine, icons, R.drawable.ic_notification,
                    permissionStorage, BrowserActivity::class.java
            )

            MediaSessionFeature(applicationContext, MediaSessionService::class.java, store).start()
        }
    }

    val sessionUseCases by lazy { SessionUseCases(store, sessionManager) }

    // Addons
    val addonManager by lazy {
        AddonManager(store, engine, addonCollectionProvider, addonUpdater)
    }

    // TODO: Swap out version code for proper collection user
    val addonCollectionProvider by lazy {
        if(UserPreferences(applicationContext).customAddonCollection){
            AddonCollectionProvider(
                    applicationContext,
                    client,
                    collectionUser = UserPreferences(applicationContext).customAddonCollectionUser,
                    collectionName = UserPreferences(applicationContext).customAddonCollectionName,
                    maxCacheAgeInMinutes = 0,
            )
        }
        else{
            AddonCollectionProvider(
                    applicationContext,
                    client,
                    collectionUser = BuildConfig.VERSION_CODE.toString(),
                    collectionName = "scw",
                    maxCacheAgeInMinutes = DAY_IN_MINUTES,
                    serverURL = "https://addons.smartcookieweb.com"
            )
        }
    }

    val supportedAddonsChecker by lazy {
        DefaultSupportedAddonsChecker(
                applicationContext, SupportedAddonsChecker.Frequency(
                1,
                TimeUnit.DAYS
        )
        )
    }

    val searchUseCases by lazy {
        SearchUseCases(store, store.toDefaultSearchEngineProvider(), tabsUseCases)
    }

    val defaultSearchUseCase by lazy {
        { searchTerms: String ->
            searchUseCases.defaultSearch.invoke(
                    searchTerms = searchTerms,
                    searchEngine = null,
                    parentSessionId = null
            )
        }
    }
    val appLinksUseCases by lazy { AppLinksUseCases(applicationContext) }

    val appLinksInterceptor by lazy {
        AppLinksInterceptor(
                applicationContext,
                interceptLinkClicks = true,
                launchInApp = {
                    applicationContext.components.preferences.getBoolean(
                            PREF_LAUNCH_EXTERNAL_APP,
                            false
                    )
                }
        )
    }

    val webAppInterceptor by lazy {
        WebAppInterceptor(
                applicationContext,
                webAppManifestStorage
        )
    }

    private val runtime by lazy {
        val builder = GeckoRuntimeSettings.Builder()
        builder.aboutConfigEnabled(true)
        GeckoRuntime.create(applicationContext, builder.build())
    }

    val webAppManifestStorage by lazy { ManifestStorage(applicationContext) }
    val webAppShortcutManager by lazy { WebAppShortcutManager(
            applicationContext,
            client,
            webAppManifestStorage
    ) }
    val webAppUseCases by lazy { WebAppUseCases(applicationContext, store, webAppShortcutManager) }

    val tabsUseCases: TabsUseCases by lazy { TabsUseCases(store, sessionManager) }
    val downloadsUseCases: DownloadsUseCases by lazy { DownloadsUseCases(store) }
    val contextMenuUseCases: ContextMenuUseCases by lazy { ContextMenuUseCases(store) }
}