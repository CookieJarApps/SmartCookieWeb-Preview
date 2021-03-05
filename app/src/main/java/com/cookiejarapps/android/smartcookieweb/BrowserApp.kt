package com.cookiejarapps.android.smartcookieweb

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.appservices.Megazord
import mozilla.components.browser.state.action.SystemAction
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import mozilla.components.support.base.facts.Facts
import mozilla.components.support.base.facts.processor.LogFactProcessor
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.rustlog.RustLog
import mozilla.components.support.webextensions.WebExtensionSupport
import java.util.concurrent.TimeUnit

class BrowserApp : Application() {

    private val logger = Logger("BrowserApp")

    val components by lazy { Components(this) }

    override fun onCreate() {
        super.onCreate()

        Megazord.init()
        RustLog.enable()

        if (!isMainProcess()) {
            return
        }

        Facts.registerProcessor(LogFactProcessor())

        components.engine.warmUp()
        restoreBrowserState()

        GlobalScope.launch(Dispatchers.IO) {
            components.webAppManifestStorage.warmUpScopes(System.currentTimeMillis())
        }
        components.downloadsUseCases.restoreDownloads()
        try {
            GlobalAddonDependencyProvider.initialize(
                components.addonManager,
                components.addonUpdater
            )
            WebExtensionSupport.initialize(
                components.engine,
                components.store,
                onNewTabOverride = {
                    _, engineSession, url ->
                        components.tabsUseCases.addTab(url, selectTab = true, engineSession = engineSession)
                },
                onCloseTabOverride = {
                    _, sessionId -> components.tabsUseCases.removeTab(sessionId)
                },
                onSelectTabOverride = {
                    _, sessionId -> components.tabsUseCases.selectTab(sessionId)
                },
                onUpdatePermissionRequest = components.addonUpdater::onUpdatePermissionRequest,
                onExtensionsLoaded = { extensions ->
                    components.addonUpdater.registerForFutureUpdates(extensions)
                    components.supportedAddonsChecker.registerForChecks()
                }
            )
        } catch (e: UnsupportedOperationException) {
            // Web extension support is only available for engine gecko
            Logger.error("Failed to initialize web extension support", e)
        }
    }

    private fun restoreBrowserState() = GlobalScope.launch(Dispatchers.Main) {
        components.tabsUseCases.restore(components.sessionStorage)

        components.sessionStorage.autoSave(components.store)
            .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
            .whenGoingToBackground()
            .whenSessionsChange()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        logger.debug("onTrimMemory: $level")

        runOnlyInMainProcess {
            components.store.dispatch(SystemAction.LowMemoryAction(level))

            components.icons.onTrimMemory(level)
        }
    }
}
