package com.cookiejarapps.android.smartcookieweb

import android.Manifest
import android.app.*
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.di.injector
import com.cookiejarapps.android.smartcookieweb.icon.TabCountView
import com.cookiejarapps.android.smartcookieweb.popup.PopupMenu
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.tabs.TabSession
import com.cookiejarapps.android.smartcookieweb.tabs.TabSessionManager
import com.cookiejarapps.android.smartcookieweb.utils.FileUtils.Companion.readBundleFromStorage
import com.cookiejarapps.android.smartcookieweb.utils.FileUtils.Companion.writeBundleToStorage
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import org.mozilla.geckoview.*
import org.mozilla.geckoview.ContentBlocking.BlockEvent
import org.mozilla.geckoview.GeckoRuntime.ActivityDelegate
import org.mozilla.geckoview.GeckoSession.*
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement
import org.mozilla.geckoview.GeckoSession.HistoryDelegate.HistoryList
import org.mozilla.geckoview.GeckoSession.MediaDelegate.RecordingDevice
import org.mozilla.geckoview.GeckoSession.NavigationDelegate.LoadRequest
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.MediaCallback
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import org.mozilla.geckoview.WebExtension.CreateTabDetails
import org.mozilla.geckoview.WebExtension.UpdateTabDetails
import java.io.*
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

class BrowserActivity : AppCompatActivity(), ToolbarLayout.TabListener, WebExtensionDelegate, OnSharedPreferenceChangeListener {
    private var mTabSessionManager: TabSessionManager? = null
    private var mGeckoView: GeckoView? = null
    private var mFullAccessibilityTree = false
    private val mUsePrivateBrowsing = false
    private val mCollapsed = false
    private var mKillProcessOnDestroy = false
    private val mDesktopMode = false
    private var mTrackingProtectionException = false
    private val mPopupSession: TabSession? = null
    private val mPopupView: View? = null
    private var mShowNotificationsRejected = false
    private val mAcceptedPersistentStorage = ArrayList<String?>()
    private var mToolbarView: LinearLayout? = null
    private var mCurrentUri: String? = null
    private var mCanGoBack = false
    private var mCanGoForward = false
    private var mFullScreen = false
    private val mNotificationIDMap = HashMap<String, Int>()
    private val mNotificationMap = HashMap<Int, WebNotification>()
    private var mLastID = 100
    private var mProgressView: ProgressBar? = null
    private var mPendingDownloads = LinkedList<WebResponse>()
    private var mNextActivityResultCode = 10
    private val mPendingActivityResult = HashMap<Int, GeckoResult<Intent>>()

    @JvmField
    @Inject
    var userPreferences: UserPreferences? = null
    private val mCommitListener = OnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            val text = v.text.toString()
            if ((text.contains(".") || text.contains(":")) && !text.contains(" ")) {
                mTabSessionManager!!.currentSession!!.loadUri(text)
            } else {
                mTabSessionManager!!.currentSession!!.loadUri(SEARCH_URI_BASE + text)
            }
            mGeckoView!!.requestFocus()
            saveState()
            return@OnEditorActionListener true
        }
        false
    }
    private val mPopupListener = View.OnClickListener { v: View? ->
        val popUpClass = PopupMenu()
        popUpClass.showPopupWindow(v!!, this@BrowserActivity)
    }
    private val mTabButton = View.OnClickListener { v: View? ->
        val tabCountButton = mToolbarView!!.findViewById<FrameLayout>(R.id.tab_count_button)
        val tabButtonMenu = android.widget.PopupMenu(this, tabCountButton)
        for (idx in 0 until mTabSessionManager!!.sessionCount()) {
            tabButtonMenu.menu.add(0, idx, idx,
                    mTabSessionManager!!.getSession(idx)!!.title)
        }
        tabButtonMenu.setOnMenuItemClickListener { item: MenuItem ->
            switchToTab(item.itemId)
            true
        }
        tabButtonMenu.show()
    }

    override fun openNewTab(details: CreateTabDetails): TabSession {
        val newSession = createSession(details.cookieStoreId)
        val tabCountView: TabCountView = mToolbarView!!.findViewById(R.id.tab_count_view)
        tabCountView.updateCount(mTabSessionManager!!.sessionCount())
        if (details.active === java.lang.Boolean.TRUE) {
            setGeckoViewSession(newSession, false)
        }
        saveState()
        return newSession
    }

    private val SETTINGS: MutableList<Setting<*>> = ArrayList()

    private abstract inner class Setting<T>(private val mKey: Int, private val mDefaultKey: Int, private val mReloadCurrentSession: Boolean) {
        private var mValue: T? = null
        fun onPrefChange(pref: SharedPreferences) {
            val defaultValue = getDefaultValue(mDefaultKey, resources)
            val key = resources.getString(mKey)
            val value = getValue(key, defaultValue, pref)
            if (value() != value) {
                setValue(value)
            }
        }

        private fun setValue(newValue: T) {
            mValue = newValue
            for (session in mTabSessionManager!!.getSessions()) {
                setValue(session.settings, value())
            }
            if (sGeckoRuntime != null) {
                setValue(sGeckoRuntime!!.settings, value())
                if (sExtensionManager != null) {
                    setValue(sGeckoRuntime!!.webExtensionController, value())
                }
            }
            val current: GeckoSession? = mTabSessionManager!!.currentSession
            if (mReloadCurrentSession && current != null) {
                current.reload()
            }
        }

        fun value(): T {
            return if (mValue == null) getDefaultValue(mDefaultKey, resources) else mValue!!
        }

        protected abstract fun getDefaultValue(key: Int, res: Resources): T
        protected abstract fun getValue(key: String?, defaultValue: T,
                                        preferences: SharedPreferences): T

        /** Override one of these to define the behavior when this setting changes.  */
        protected open fun setValue(settings: GeckoSessionSettings, value: T) {}
        protected open fun setValue(settings: GeckoRuntimeSettings, value: T) {}
        protected open fun setValue(controller: WebExtensionController, value: T) {}

        init {
            SETTINGS.add(this)
        }
    }

    private open inner class StringSetting @JvmOverloads constructor(key: Int, defaultValueKey: Int,
                                                                     reloadCurrentSession: Boolean = false) : Setting<String?>(key, defaultValueKey, reloadCurrentSession) {
        override fun getDefaultValue(key: Int, res: Resources): String? {
            return res.getString(key)
        }

        public override fun getValue(key: String?, defaultValue: String?,
                                     preferences: SharedPreferences): String? {
            return preferences.getString(key, defaultValue)
        }
    }

    private open inner class BooleanSetting @JvmOverloads constructor(key: Int, defaultValueKey: Int,
                                                                      reloadCurrentSession: Boolean = false) : Setting<Boolean>(key, defaultValueKey, reloadCurrentSession) {
        override fun getDefaultValue(key: Int, res: Resources): Boolean {
            return res.getBoolean(key)
        }

        public override fun getValue(key: String?, defaultValue: Boolean,
                                     preferences: SharedPreferences): Boolean {
            return preferences.getBoolean(key, defaultValue)
        }
    }

    private open inner class IntSetting @JvmOverloads constructor(key: Int, defaultValueKey: Int,
                                                                  reloadCurrentSession: Boolean = false) : Setting<Int>(key, defaultValueKey, reloadCurrentSession) {
        override fun getDefaultValue(key: Int, res: Resources): Int {
            return res.getInteger(key)
        }

        public override fun getValue(key: String?, defaultValue: Int,
                                     preferences: SharedPreferences): Int {
            return preferences.getString(key, Integer.toString(defaultValue))!!.toInt()
        }
    }

    private val mPreferredColorScheme: IntSetting = object : IntSetting(
            R.string.key_preferred_color_scheme, R.integer.preferred_color_scheme_default,  /* reloadCurrentSession */
            true
    ) {
        public override fun setValue(settings: GeckoRuntimeSettings, value: Int) {
            settings.preferredColorScheme = value
        }
    }
    private val mUserAgent: StringSetting = object : StringSetting(
            R.string.key_user_agent_override, R.string.user_agent_override_default,  /* reloadCurrentSession */
            true
    ) {
        override fun setValue(settings: GeckoSessionSettings, value: String?) {
            settings.userAgentOverride = if (value!!.isEmpty()) null else value
        }
    }
    private val mRemoteDebugging: BooleanSetting = object : BooleanSetting(
            R.string.key_remote_debugging, R.bool.remote_debugging_default
    ) {
        public override fun setValue(settings: GeckoRuntimeSettings, value: Boolean) {
            settings.remoteDebuggingEnabled = value
        }
    }
    private val mTrackingProtection: BooleanSetting = object : BooleanSetting(
            R.string.key_tracking_protection, R.bool.tracking_protection_default
    ) {
        public override fun setValue(settings: GeckoRuntimeSettings, value: Boolean) {
            mTabSessionManager!!.setUseTrackingProtection(value)
            settings.contentBlocking.strictSocialTrackingProtection = value
        }
    }
    private val mEnhancedTrackingProtection: StringSetting = object : StringSetting(
            R.string.key_enhanced_tracking_protection, R.string.enhanced_tracking_protection_default
    ) {
        override fun setValue(settings: GeckoRuntimeSettings, value: String?) {
            val etpLevel: Int
            etpLevel = when (value) {
                "disabled" -> ContentBlocking.EtpLevel.NONE
                "standard" -> ContentBlocking.EtpLevel.DEFAULT
                "strict" -> ContentBlocking.EtpLevel.STRICT
                else -> throw RuntimeException("Invalid ETP level: $value")
            }
            settings.contentBlocking.enhancedTrackingProtectionLevel = etpLevel
        }
    }
    private val mAllowAutoplay = BooleanSetting(
            R.string.key_autoplay, R.bool.autoplay_default, true
    )
    private val mAllowExtensionsInPrivateBrowsing: BooleanSetting = object : BooleanSetting(
            R.string.key_allow_extensions_in_private_browsing,
            R.bool.allow_extensions_in_private_browsing_default
    ) {
        public override fun setValue(controller: WebExtensionController, value: Boolean) {
            controller.setAllowedInPrivateBrowsing(
                    sExtensionManager!!.extension,
                    value)
        }
    }

    private fun onPreferencesChange(preferences: SharedPreferences) {
        for (setting in SETTINGS) {
            setting.onPrefChange(preferences)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.injector.inject(this)
        Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
                " - application start")
        createNotificationChannel()
        setContentView(R.layout.geckoview_activity)
        mGeckoView = findViewById(R.id.gecko_view)
        mTabSessionManager = TabSessionManager()
        //val tabsDrawer = findViewById<FrameLayout>(R.id.left_drawer)
        //val tabsView = TabsDrawerView(this)
        //tabsDrawer.addView(tabsView)
        setSupportActionBar(findViewById(R.id.toolbar))
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        // Read initial preference state
        onPreferencesChange(preferences)
        mToolbarView = findViewById(R.id.toolbar_layout)
        mFullAccessibilityTree = intent.getBooleanExtra(FULL_ACCESSIBILITY_TREE_EXTRA, false)
        mProgressView = findViewById(R.id.page_progress)
        if (sGeckoRuntime == null) {
            val runtimeSettingsBuilder = GeckoRuntimeSettings.Builder()
            if (BuildConfig.DEBUG) {
                // In debug builds, we want to load JavaScript resources fresh with
                // each build.
                runtimeSettingsBuilder.arguments(arrayOf("-purgecaches"))
            }
            val extras = intent.extras
            if (extras != null) {
                runtimeSettingsBuilder.extras(extras)
            }
            runtimeSettingsBuilder
                    .remoteDebuggingEnabled(mRemoteDebugging.value())
                    .consoleOutput(true)
                    .contentBlocking(ContentBlocking.Settings.Builder()
                            .antiTracking(ContentBlocking.AntiTracking.DEFAULT or
                                    ContentBlocking.AntiTracking.STP)
                            .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.DEFAULT)
                            .build())
                    .preferredColorScheme(mPreferredColorScheme.value())
                    .javaScriptEnabled(userPreferences!!.javaScriptEnabled)
                    .aboutConfigEnabled(true)
            sGeckoRuntime = GeckoRuntime.create(this, runtimeSettingsBuilder.build())
            sExtensionManager = WebExtensionManager(sGeckoRuntime, mTabSessionManager)
            mTabSessionManager!!.setTabObserver(sExtensionManager)
            sGeckoRuntime!!.webExtensionController.setDebuggerDelegate(sExtensionManager!!)

            // `getSystemService` call requires API level 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sGeckoRuntime!!.webNotificationDelegate = object : WebNotificationDelegate {
                    var notificationManager = getSystemService(NotificationManager::class.java)
                    override fun onShowNotification(notification: WebNotification) {
                        val clickIntent = Intent(this@BrowserActivity, BrowserActivity::class.java)
                        clickIntent.putExtra("onClick", notification.tag)
                        val dismissIntent = PendingIntent.getActivity(this@BrowserActivity, mLastID, clickIntent, 0)
                        val builder = Notification.Builder(this@BrowserActivity)
                                .setContentTitle(notification.title)
                                .setContentText(notification.text)
                                .setSmallIcon(R.drawable.ic_status_logo)
                                .setContentIntent(dismissIntent)
                                .setAutoCancel(true)
                        mNotificationIDMap[notification.tag] = mLastID
                        mNotificationMap[mLastID] = notification
                        if (notification.imageUrl != null && notification.imageUrl!!.length > 0) {
                            val executor = GeckoWebExecutor(sGeckoRuntime!!)
                            val response = executor.fetch(
                                    WebRequest.Builder(notification.imageUrl!!)
                                            .addHeader("Accept", "image")
                                            .build())
                            response.accept { value: WebResponse? ->
                                val bitmap = BitmapFactory.decodeStream(value!!.body)
                                builder.setLargeIcon(Icon.createWithBitmap(bitmap))
                                notificationManager.notify(mLastID++, builder.build())
                            }
                        } else {
                            notificationManager.notify(mLastID++, builder.build())
                        }
                    }

                    override fun onCloseNotification(notification: WebNotification) {
                        if (mNotificationIDMap.containsKey(notification.tag)) {
                            val id = mNotificationIDMap[notification.tag]!!
                            notificationManager.cancel(id)
                            mNotificationMap.remove(id)
                            mNotificationIDMap.remove(notification.tag)
                        }
                    }
                }
            }
            sGeckoRuntime!!.delegate = GeckoRuntime.Delegate {
                mKillProcessOnDestroy = true
                finish()
            }
            sGeckoRuntime!!.activityDelegate = ActivityDelegate { pendingIntent: PendingIntent ->
                val result = GeckoResult<Intent>()
                try {
                    val code = mNextActivityResultCode++
                    mPendingActivityResult[code] = result
                    this@BrowserActivity.startIntentSenderForResult(pendingIntent.intentSender, code, null, 0, 0, 0)
                } catch (e: SendIntentException) {
                    result.completeExceptionally(e)
                }
                result
            }
        }
        sExtensionManager!!.setExtensionDelegate(this)

        mGeckoView!!.setDynamicToolbarMaxHeight(findViewById<View>(R.id.toolbar).layoutParams.height)
        val autoCompleteTextView = mToolbarView!!.findViewById<AutoCompleteTextView>(R.id.location_view)
        autoCompleteTextView.setOnEditorActionListener(mCommitListener)
        val tabCountButton = mToolbarView!!.findViewById<FrameLayout>(R.id.tab_count_button)
        tabCountButton.setOnClickListener(mTabButton)
        val moreButton = mToolbarView!!.findViewById<FrameLayout>(R.id.more_button)
        moreButton.setOnClickListener(mPopupListener)
        val tabCountView: TabCountView = mToolbarView!!.findViewById(R.id.tab_count_view)
        tabCountView.updateCount(mTabSessionManager!!.sessionCount())
        val bundle = readBundleFromStorage(application, "SAVED_TABS.parcel")
        if (bundle != null) {
            mTabSessionManager!!.closeSession(mTabSessionManager!!.getSession(0))
            for (key in bundle.keySet()) {
                if (bundle.getString(key) != null) {
                    createNewTab()
                    mTabSessionManager!!.currentSession!!.loadUri(bundle.getString(key)!!)
                }
            }
        }
    }

    override fun getSession(session: GeckoSession): TabSession {
        return mTabSessionManager!!.getSession(session)!!
    }

    override fun getCurrentSession(): TabSession {
        return mTabSessionManager!!.currentSession!!
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        onPreferencesChange(sharedPreferences)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createSession(cookieStoreId: String? = null): TabSession {
        val settingsBuilder = GeckoSessionSettings.Builder()
        settingsBuilder
                .usePrivateMode(mUsePrivateBrowsing)
                .fullAccessibilityTree(mFullAccessibilityTree)
                .userAgentOverride(mUserAgent.value()!!)
                .viewportMode(if (mDesktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .userAgentMode(if (mDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .useTrackingProtection(mTrackingProtection.value())
        if (cookieStoreId != null) {
            settingsBuilder.contextId(cookieStoreId)
        }
        val session = mTabSessionManager!!.newSession(settingsBuilder.build())
        connectSession(session)
        return session
    }

    private fun connectSession(session: GeckoSession) {
        session.contentDelegate = ExampleContentDelegate()
        session.historyDelegate = ExampleHistoryDelegate()
        val cb = ExampleContentBlockingDelegate()
        session.contentBlockingDelegate = cb
        session.progressDelegate = ExampleProgressDelegate(cb)
        session.navigationDelegate = ExampleNavigationDelegate()
        val prompt = BasicGeckoViewPrompt(this)
        prompt.filePickerRequestCode = REQUEST_FILE_PICKER
        session.promptDelegate = prompt
        val permission = ExamplePermissionDelegate()
        permission.androidPermissionRequestCode = REQUEST_PERMISSIONS
        session.permissionDelegate = permission
        session.mediaDelegate = ExampleMediaDelegate(this)
        session.selectionActionDelegate = BasicSelectionActionDelegate(this)
        if (sExtensionManager!!.extension != null) {
            val sessionController = session.webExtensionController
            sessionController.setActionDelegate(sExtensionManager!!.extension, sExtensionManager)
            sessionController.setTabDelegate(sExtensionManager!!.extension, sExtensionManager)
        }
        updateDesktopMode(session)
    }

    private fun recreateSession(session: TabSession? = mTabSessionManager!!.currentSession) {
        var session = session
        if (session != null) {
            mTabSessionManager!!.closeSession(session)
        }
        session = createSession()
        session.open(sGeckoRuntime!!)
        mTabSessionManager!!.setCurrentSession(session)
        mGeckoView!!.setSession(session)
        sGeckoRuntime!!.webExtensionController.setTabActive(session, true)
        if (mCurrentUri != null) {
            session.loadUri(mCurrentUri!!)
        }
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            mTabSessionManager!!.setCurrentSession((mGeckoView!!.session as TabSession?)!!)
            sGeckoRuntime!!.webExtensionController.setTabActive(mGeckoView!!.session!!, true)
        } else {
            recreateSession()
        }
    }

    // TODO: VERY basic tab saving - see how ReferenceBrowser / Android Components do this
    private fun saveState() {
        val scheduler = Schedulers.io()
        val outState = Bundle(ClassLoader.getSystemClassLoader())
        Log.d("BrowserActivity", "Saving tab state")
        for (idx in 0 until mTabSessionManager!!.sessionCount()) {
            outState.putString("GECKOVIEW_$idx", mTabSessionManager!!.getSession(idx)!!.uri)
        }
        writeBundleToStorage(application, outState, "SAVED_TABS.parcel")
                .subscribeOn(scheduler)
                .subscribe()
    }

    private fun updateDesktopMode(session: GeckoSession) {
        session.settings.viewportMode = if (mDesktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        session.settings.userAgentMode = if (mDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
    }

    override fun onBackPressed() {
        val session: GeckoSession? = mTabSessionManager!!.currentSession
        if (mFullScreen && session != null) {
            session.exitFullScreen()
            return
        }
        if (mCanGoBack && session != null) {
            session.goBack()
            return
        }
        super.onBackPressed()
    }

    private fun updateTrackingProtectionException() {
        if (sGeckoRuntime == null) {
            return
        }
        val session = mTabSessionManager!!.currentSession ?: return
        sGeckoRuntime!!.contentBlockingController
                .checkException(session)
                .accept { value: Boolean? -> mTrackingProtectionException = value as Boolean }
    }

    fun createNewTab() {
        val startTime = sGeckoRuntime!!.profilerController.profilerTime
        val newSession = createSession()
        newSession.open(sGeckoRuntime!!)
        setGeckoViewSession(newSession)
        val tabCountView: TabCountView = mToolbarView!!.findViewById(R.id.tab_count_view)
        tabCountView.updateCount(mTabSessionManager!!.sessionCount())
        sGeckoRuntime!!.profilerController.addMarker("Create new tab", startTime)
    }

    override fun closeTab(session: TabSession) {
        saveState()
        if (mTabSessionManager!!.sessionCount() > 1) {
            mTabSessionManager!!.closeSession(session)
            val tabSession = mTabSessionManager!!.currentSession
            setGeckoViewSession(tabSession)
            tabSession!!.reload()
            val tabCountView: TabCountView = mToolbarView!!.findViewById(R.id.tab_count_view)
            tabCountView.updateCount(mTabSessionManager!!.sessionCount())
        } else {
            recreateSession(session)
        }
    }

    override fun updateTab(session: TabSession, details: UpdateTabDetails) {
        if (details.active === java.lang.Boolean.TRUE) {
            switchToSession(session, false)
        }
    }

    override fun onBrowserActionClick() {
        sExtensionManager!!.onClicked(mTabSessionManager!!.currentSession)
    }

    fun switchToSession(session: TabSession?, activateTab: Boolean) {
        val currentSession = mTabSessionManager!!.currentSession
        if (session != currentSession) {
            setGeckoViewSession(session, activateTab)
            mCurrentUri = session!!.uri
            if (!session.isOpen) {
                // Session's process was previously killed; reopen
                session.open(sGeckoRuntime!!)
                session.loadUri(mCurrentUri!!)
            }
            val autoCompleteTextView = mToolbarView!!.findViewById<AutoCompleteTextView>(R.id.location_view)
            autoCompleteTextView.setText(mCurrentUri)
        }
    }

    override fun switchToTab(index: Int) {
        val nextSession = mTabSessionManager!!.getSession(index)
        switchToSession(nextSession, true)
    }

    private fun setGeckoViewSession(session: TabSession?) {
        setGeckoViewSession(session, true)
    }

    private fun setGeckoViewSession(session: TabSession?, activateTab: Boolean) {
        val controller = sGeckoRuntime!!.webExtensionController
        val previousSession = mGeckoView!!.releaseSession()
        if (previousSession != null) {
            controller.setTabActive(previousSession, false)
        }
        mGeckoView!!.setSession(session!!)
        if (activateTab) {
            controller.setTabActive(session, true)
        }
        mTabSessionManager!!.setCurrentSession(session)
    }

    public override fun onDestroy() {
        if (mKillProcessOnDestroy) {
            Process.killProcess(Process.myPid())
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (ACTION_SHUTDOWN == intent.action) {
            mKillProcessOnDestroy = true
            if (sGeckoRuntime != null) {
                sGeckoRuntime!!.shutdown()
            }
            finish()
            return
        }
        if (intent.hasExtra("onClick")) {
            val key = intent.extras!!.getInt("onClick")
            val notification = mNotificationMap[key]
            if (notification != null) {
                notification.click()
                mNotificationMap.remove(key)
            }
        }
        setIntent(intent)
        if (intent.data != null) {
            loadFromIntent(intent)
        }
    }

    private fun loadFromIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            mTabSessionManager!!.currentSession!!.load(
                    GeckoSession.Loader()
                            .uri(uri.toString())
                            .flags(GeckoSession.LOAD_FLAGS_EXTERNAL))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        if (requestCode == REQUEST_FILE_PICKER) {
            val prompt = mTabSessionManager!!.currentSession!!.promptDelegate as BasicGeckoViewPrompt?
            prompt!!.onFileCallbackResult(resultCode, data)
        } else if (mPendingActivityResult.containsKey(requestCode)) {
            val result = mPendingActivityResult.remove(requestCode)!!
            if (resultCode == RESULT_OK) {
                result.complete(data)
            } else {
                result.completeExceptionally(RuntimeException("Unknown error"))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSIONS) {
            val permission = mTabSessionManager!!.currentSession!!.permissionDelegate as ExamplePermissionDelegate?
            permission!!.onRequestPermissionsResult(permissions, grantResults)
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            continueDownloads()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun continueDownloads() {
        val downloads = mPendingDownloads
        mPendingDownloads = LinkedList()
        for (response in downloads) {
            downloadFile(response)
        }
    }

    private fun downloadFile(response: WebResponse) {
        if (response.body == null) {
            return
        }
        if (ContextCompat.checkSelfPermission(this@BrowserActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            mPendingDownloads.add(response)
            ActivityCompat.requestPermissions(this@BrowserActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE)
            return
        }
        val filename = getFileName(response)
        try {
            val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .absolutePath + "/" + filename
            val bufferSize = 1024 // to read in 1Mb increments
            val buffer = ByteArray(bufferSize)
            try {
                BufferedOutputStream(FileOutputStream(downloadsPath)).use { out ->
                    var len: Int
                    while (response.body!!.read(buffer).also { len = it } != -1) {
                        out.write(buffer, 0, len)
                    }
                }
            } catch (e: Throwable) {
                Log.i(LOGTAG, e.stackTrace.toString())
            }
        } catch (e: Throwable) {
            Log.i(LOGTAG, e.stackTrace.toString())
        }
    }

    private fun getFileName(response: WebResponse): String {
        val filename: String
        val contentDispositionHeader: String?
        contentDispositionHeader = if (response.headers.containsKey("content-disposition") || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            response.headers["content-disposition"]
        } else {
            response.headers.getOrDefault("Content-Disposition", "default filename=GVDownload")
        }
        val pattern = Pattern.compile("(filename=\"?)(.+)(\"?)")
        val matcher = pattern.matcher(contentDispositionHeader)
        filename = if (matcher.find()) {
            matcher.group(2).replace("\\s".toRegex(), "%20")
        } else {
            "GVEdownload"
        }
        return filename
    }

    private var mErrorTemplate: String? = null
    private fun createErrorPage(error: String): String? {
        if (mErrorTemplate == null) {
            var stream: InputStream? = null
            var reader: BufferedReader? = null
            val builder = StringBuilder()
            try {
                stream = resources.assets.open("error.html")
                reader = BufferedReader(InputStreamReader(stream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    builder.append(line)
                    builder.append("\n")
                }
                mErrorTemplate = builder.toString()
            } catch (e: IOException) {
                Log.d(LOGTAG, "Failed to open error page template", e)
                return null
            } finally {
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e: IOException) {
                        Log.e(LOGTAG, "Failed to close error page template stream", e)
                    }
                }
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.e(LOGTAG, "Failed to close error page template reader", e)
                    }
                }
            }
        }
        return mErrorTemplate!!.replace("\$ERROR", error)
    }

    private inner class ExampleHistoryDelegate : HistoryDelegate {
        private val mVisitedURLs: HashSet<String>
        override fun onVisited(session: GeckoSession, url: String,
                               lastVisitedURL: String?, flags: Int): GeckoResult<Boolean>? {
            Log.i(LOGTAG, "Visited URL: $url")
            mVisitedURLs.add(url)
            return GeckoResult.fromValue(true)
        }

        override fun getVisited(session: GeckoSession, urls: Array<String>): GeckoResult<BooleanArray>? {
            val visited = BooleanArray(urls.size)
            for (i in urls.indices) {
                visited[i] = mVisitedURLs.contains(urls[i])
            }
            return GeckoResult.fromValue(visited)
        }

        override fun onHistoryStateChange(session: GeckoSession,
                                          state: HistoryList) {
            Log.i(LOGTAG, "History state updated")
        }

        init {
            mVisitedURLs = HashSet()
        }
    }

    private inner class ExampleContentDelegate : ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            Log.i(LOGTAG, "Content title changed to $title")
            val tabSession = mTabSessionManager!!.getSession(session)
            if (tabSession != null) {
                tabSession.title = title
            }
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            window.setFlags(if (fullScreen) WindowManager.LayoutParams.FLAG_FULLSCREEN else 0,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
            mFullScreen = fullScreen
            if (fullScreen) {
                supportActionBar!!.hide()
            } else {
                supportActionBar!!.show()
            }
        }

        override fun onFocusRequest(session: GeckoSession) {
            Log.i(LOGTAG, "Content requesting focus")
        }

        override fun onCloseRequest(session: GeckoSession) {
            if (session === mTabSessionManager!!.currentSession) {
                finish()
            }
        }

        override fun onContextMenu(session: GeckoSession,
                                   screenX: Int, screenY: Int,
                                   element: ContextElement) {
            Log.d(LOGTAG, "onContextMenu screenX=" + screenX +
                    " screenY=" + screenY +
                    " type=" + element.type +
                    " linkUri=" + element.linkUri +
                    " title=" + element.title +
                    " alt=" + element.altText +
                    " srcUri=" + element.srcUri)
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            downloadFile(response)
        }

        override fun onCrash(session: GeckoSession) {
            Log.e(LOGTAG, "Crashed, reopening session")
            session.open(sGeckoRuntime!!)
        }

        override fun onKill(session: GeckoSession) {
            val tabSession = mTabSessionManager!!.getSession(session) ?: return
            if (tabSession != mTabSessionManager!!.currentSession) {
                Log.e(LOGTAG, "Background session killed")
                return
            }
            check(!isForeground) { "Foreground content process unexpectedly killed by OS!" }
            Log.e(LOGTAG, "Current session killed, reopening")
            tabSession.open(sGeckoRuntime!!)
            tabSession.loadUri(tabSession.uri!!)
        }

        override fun onFirstComposite(session: GeckoSession) {
            Log.d(LOGTAG, "onFirstComposite")
        }

        override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
            Log.d(LOGTAG, "onWebAppManifest: $manifest")
        }

        private var activeAlert = false
        override fun onSlowScript(geckoSession: GeckoSession,
                                  scriptFileName: String): GeckoResult<SlowScriptResponse>? {
            val prompt = mTabSessionManager!!.currentSession!!.promptDelegate as BasicGeckoViewPrompt?
            if (prompt != null) {
                val result = GeckoResult<SlowScriptResponse>()
                if (!activeAlert) {
                    activeAlert = true
                    prompt.onSlowScriptPrompt(geckoSession, getString(R.string.slow_script), result)
                }
                return result.then { value: SlowScriptResponse? ->
                    activeAlert = false
                    GeckoResult.fromValue(value)
                }
            }
            return null
        }

        override fun onMetaViewportFitChange(session: GeckoSession, viewportFit: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                return
            }
            val layoutParams = window.attributes
            if (viewportFit == "cover") {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else if (viewportFit == "contain") {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            } else {
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            window.attributes = layoutParams
        }
    }

    private inner class ExampleProgressDelegate(private val mCb: ExampleContentBlockingDelegate) : ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            Log.i(LOGTAG, "Starting to load page at $url")
            Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
                    " - page load start")
            mCb.clearCounters()
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            Log.i(LOGTAG, "Stopping page load " + if (success) "successfully" else "unsuccessfully")
            Log.i(LOGTAG, "zerdatime " + SystemClock.elapsedRealtime() +
                    " - page load stop")
            mCb.logCounters()
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            Log.i(LOGTAG, "onProgressChange $progress")
            mProgressView!!.progress = progress
            if (progress > 0 && progress < 100) {
                mProgressView!!.visibility = View.VISIBLE
            } else {
                mProgressView!!.visibility = View.GONE
            }
        }

        override fun onSecurityChange(session: GeckoSession, securityInfo: SecurityInformation) {
            Log.i(LOGTAG, "Security status changed to " + securityInfo.securityMode)
        }

        override fun onSessionStateChange(session: GeckoSession, state: GeckoSession.SessionState) {
            Log.i(LOGTAG, "New Session state: $state")
        }
    }

    private inner class ExamplePermissionDelegate : PermissionDelegate {
        var androidPermissionRequestCode = 1
        private var mCallback: PermissionDelegate.Callback? = null

        internal inner class ExampleNotificationCallback(private val mCallback: PermissionDelegate.Callback) : PermissionDelegate.Callback {
            override fun reject() {
                mShowNotificationsRejected = true
                mCallback.reject()
            }

            override fun grant() {
                mShowNotificationsRejected = false
                mCallback.grant()
            }
        }

        internal inner class ExamplePersistentStorageCallback(private val mCallback: PermissionDelegate.Callback, private val mUri: String?) : PermissionDelegate.Callback {
            override fun reject() {
                mCallback.reject()
            }

            override fun grant() {
                mAcceptedPersistentStorage.add(mUri)
                mCallback.grant()
            }
        }

        fun onRequestPermissionsResult(permissions: Array<String>?,
                                       grantResults: IntArray) {
            if (mCallback == null) {
                return
            }
            val cb: PermissionDelegate.Callback = mCallback!!
            mCallback = null
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // At least one permission was not granted.
                    cb.reject()
                    return
                }
            }
            cb.grant()
        }

        override fun onAndroidPermissionsRequest(session: GeckoSession, permissions: Array<String>?,
                                                 callback: PermissionDelegate.Callback) {
            if (Build.VERSION.SDK_INT >= 23) {
                // requestPermissions was introduced in API 23.
                mCallback = callback
                requestPermissions(permissions!!, androidPermissionRequestCode)
            } else {
                callback.grant()
            }
        }

        override fun onContentPermissionRequest(session: GeckoSession, uri: String?,
                                                type: Int, callback: PermissionDelegate.Callback) {
            val resId: Int
            var contentPermissionCallback = callback
            if (PermissionDelegate.PERMISSION_GEOLOCATION == type) {
                resId = R.string.request_geolocation
            } else if (PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION == type) {
                if (mShowNotificationsRejected) {
                    Log.w(LOGTAG, "Desktop notifications already denied by user.")
                    callback.reject()
                    return
                }
                resId = R.string.request_notification
                contentPermissionCallback = ExampleNotificationCallback(callback)
            } else if (PermissionDelegate.PERMISSION_PERSISTENT_STORAGE == type) {
                if (mAcceptedPersistentStorage.contains(uri)) {
                    Log.w(LOGTAG, "Persistent Storage for $uri already granted by user.")
                    callback.grant()
                    return
                }
                resId = R.string.request_storage
                contentPermissionCallback = ExamplePersistentStorageCallback(callback, uri)
            } else if (PermissionDelegate.PERMISSION_XR == type) {
                resId = R.string.request_xr
            } else if (PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE == type || PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE == type) {
                if (!mAllowAutoplay.value()) {
                    Log.d(LOGTAG, "Rejecting autoplay request")
                    callback.reject()
                } else {
                    Log.d(LOGTAG, "Granting autoplay request")
                    callback.grant()
                }
                return
            } else if (PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS == type) {
                resId = R.string.request_media_key_system_access
            } else {
                Log.w(LOGTAG, "Unknown permission: $type")
                callback.reject()
                return
            }
            val title = getString(resId, Uri.parse(uri).authority)
            val prompt = mTabSessionManager!!.currentSession!!.promptDelegate as BasicGeckoViewPrompt?
            prompt!!.onPermissionPrompt(session, title, contentPermissionCallback)
        }

        private fun normalizeMediaName(sources: Array<PermissionDelegate.MediaSource>?): Array<String?>? {
            if (sources == null) {
                return null
            }
            val res = arrayOfNulls<String>(sources.size)
            for (i in sources.indices) {
                val mediaSource = sources[i].source
                val name = sources[i].name
                if (PermissionDelegate.MediaSource.SOURCE_CAMERA == mediaSource) {
                    if (name!!.toLowerCase(Locale.ROOT).contains("front")) {
                        res[i] = getString(R.string.media_front_camera)
                    } else {
                        res[i] = getString(R.string.media_back_camera)
                    }
                } else if (!name!!.isEmpty()) {
                    res[i] = name
                } else if (PermissionDelegate.MediaSource.SOURCE_MICROPHONE == mediaSource) {
                    res[i] = getString(R.string.media_microphone)
                } else {
                    res[i] = getString(R.string.media_other)
                }
            }
            return res
        }

        override fun onMediaPermissionRequest(session: GeckoSession, uri: String,
                                              video: Array<PermissionDelegate.MediaSource>?, audio: Array<PermissionDelegate.MediaSource>?,
                                              callback: MediaCallback) {
            // If we don't have device permissions at this point, just automatically reject the request
            // as we will have already have requested device permissions before getting to this point
            // and if we've reached here and we don't have permissions then that means that the user
            // denied them.
            if ((audio != null
                            && ContextCompat.checkSelfPermission(this@BrowserActivity,
                            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                    || (video != null
                            && ContextCompat.checkSelfPermission(this@BrowserActivity,
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                callback.reject()
                return
            }
            val host = Uri.parse(uri).authority
            val title: String
            title = if (audio == null) {
                getString(R.string.request_video, host)
            } else if (video == null) {
                getString(R.string.request_audio, host)
            } else {
                getString(R.string.request_media, host)
            }
            val videoNames = normalizeMediaName(video)
            val audioNames = normalizeMediaName(audio)
            val prompt = mTabSessionManager!!.currentSession!!.promptDelegate as BasicGeckoViewPrompt?
            prompt!!.onMediaPrompt(session, title, video, audio, videoNames, audioNames, callback)
        }
    }

    private inner class ExampleNavigationDelegate : NavigationDelegate {
        override fun onLocationChange(session: GeckoSession, url: String?) {
            val autoCompleteTextView = mToolbarView!!.findViewById<AutoCompleteTextView>(R.id.location_view)
            autoCompleteTextView.setText(url)
            val tabSession = mTabSessionManager!!.getSession(session)
            tabSession?.onLocationChange(url!!)
            mCurrentUri = url
            updateTrackingProtectionException()
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            mCanGoBack = canGoBack
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            mCanGoForward = canGoForward
        }

        override fun onLoadRequest(session: GeckoSession,
                                   request: LoadRequest): GeckoResult<AllowOrDeny>? {
            Log.d(LOGTAG, "onLoadRequest=" + request.uri +
                    " triggerUri=" + request.triggerUri +
                    " where=" + request.target +
                    " isRedirect=" + request.isRedirect +
                    " isDirectNavigation=" + request.isDirectNavigation)
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        override fun onSubframeLoadRequest(session: GeckoSession,
                                           request: LoadRequest): GeckoResult<AllowOrDeny>? {
            Log.d(LOGTAG, "onSubframeLoadRequest=" + request.uri +
                    " triggerUri=" + request.triggerUri +
                    " isRedirect=" + request.isRedirect +
                    "isDirectNavigation=" + request.isDirectNavigation)
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
            val newSession = createSession()
            val tabCountView: TabCountView = mToolbarView!!.findViewById(R.id.tab_count_view)
            tabCountView.updateCount(mTabSessionManager!!.sessionCount())
            setGeckoViewSession(newSession)
            return GeckoResult.fromValue(newSession)
        }

        private fun categoryToString(category: Int): String {
            return when (category) {
                WebRequestError.ERROR_CATEGORY_UNKNOWN -> "ERROR_CATEGORY_UNKNOWN"
                WebRequestError.ERROR_CATEGORY_SECURITY -> "ERROR_CATEGORY_SECURITY"
                WebRequestError.ERROR_CATEGORY_NETWORK -> "ERROR_CATEGORY_NETWORK"
                WebRequestError.ERROR_CATEGORY_CONTENT -> "ERROR_CATEGORY_CONTENT"
                WebRequestError.ERROR_CATEGORY_URI -> "ERROR_CATEGORY_URI"
                WebRequestError.ERROR_CATEGORY_PROXY -> "ERROR_CATEGORY_PROXY"
                WebRequestError.ERROR_CATEGORY_SAFEBROWSING -> "ERROR_CATEGORY_SAFEBROWSING"
                else -> "UNKNOWN"
            }
        }

        private fun errorToString(error: Int): String {
            return when (error) {
                WebRequestError.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                WebRequestError.ERROR_SECURITY_SSL -> "ERROR_SECURITY_SSL"
                WebRequestError.ERROR_SECURITY_BAD_CERT -> "ERROR_SECURITY_BAD_CERT"
                WebRequestError.ERROR_NET_RESET -> "ERROR_NET_RESET"
                WebRequestError.ERROR_NET_INTERRUPT -> "ERROR_NET_INTERRUPT"
                WebRequestError.ERROR_NET_TIMEOUT -> "ERROR_NET_TIMEOUT"
                WebRequestError.ERROR_CONNECTION_REFUSED -> "ERROR_CONNECTION_REFUSED"
                WebRequestError.ERROR_UNKNOWN_PROTOCOL -> "ERROR_UNKNOWN_PROTOCOL"
                WebRequestError.ERROR_UNKNOWN_HOST -> "ERROR_UNKNOWN_HOST"
                WebRequestError.ERROR_UNKNOWN_SOCKET_TYPE -> "ERROR_UNKNOWN_SOCKET_TYPE"
                WebRequestError.ERROR_UNKNOWN_PROXY_HOST -> "ERROR_UNKNOWN_PROXY_HOST"
                WebRequestError.ERROR_MALFORMED_URI -> "ERROR_MALFORMED_URI"
                WebRequestError.ERROR_REDIRECT_LOOP -> "ERROR_REDIRECT_LOOP"
                WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> "ERROR_SAFEBROWSING_PHISHING_URI"
                WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI -> "ERROR_SAFEBROWSING_MALWARE_URI"
                WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> "ERROR_SAFEBROWSING_UNWANTED_URI"
                WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> "ERROR_SAFEBROWSING_HARMFUL_URI"
                WebRequestError.ERROR_CONTENT_CRASHED -> "ERROR_CONTENT_CRASHED"
                WebRequestError.ERROR_OFFLINE -> "ERROR_OFFLINE"
                WebRequestError.ERROR_PORT_BLOCKED -> "ERROR_PORT_BLOCKED"
                WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> "ERROR_PROXY_CONNECTION_REFUSED"
                WebRequestError.ERROR_FILE_NOT_FOUND -> "ERROR_FILE_NOT_FOUND"
                WebRequestError.ERROR_FILE_ACCESS_DENIED -> "ERROR_FILE_ACCESS_DENIED"
                WebRequestError.ERROR_INVALID_CONTENT_ENCODING -> "ERROR_INVALID_CONTENT_ENCODING"
                WebRequestError.ERROR_UNSAFE_CONTENT_TYPE -> "ERROR_UNSAFE_CONTENT_TYPE"
                WebRequestError.ERROR_CORRUPTED_CONTENT -> "ERROR_CORRUPTED_CONTENT"
                else -> "UNKNOWN"
            }
        }

        private fun createErrorPage(category: Int, error: Int): String? {
            if (mErrorTemplate == null) {
                var stream: InputStream? = null
                var reader: BufferedReader? = null
                val builder = StringBuilder()
                try {
                    stream = resources.assets.open("error.html")
                    reader = BufferedReader(InputStreamReader(stream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        builder.append(line)
                        builder.append("\n")
                    }
                    mErrorTemplate = builder.toString()
                } catch (e: IOException) {
                    Log.d(LOGTAG, "Failed to open error page template", e)
                    return null
                } finally {
                    if (stream != null) {
                        try {
                            stream.close()
                        } catch (e: IOException) {
                            Log.e(LOGTAG, "Failed to close error page template stream", e)
                        }
                    }
                    if (reader != null) {
                        try {
                            reader.close()
                        } catch (e: IOException) {
                            Log.e(LOGTAG, "Failed to close error page template reader", e)
                        }
                    }
                }
            }
            return this@BrowserActivity.createErrorPage(categoryToString(category) + " : " + errorToString(error))
        }

        override fun onLoadError(session: GeckoSession, uri: String?,
                                 error: WebRequestError): GeckoResult<String>? {
            Log.d(LOGTAG, "onLoadError=" + uri +
                    " error category=" + error.category +
                    " error=" + error.code)
            return GeckoResult.fromValue("data:text/html," + createErrorPage(error.category, error.code))
        }
    }

    private inner class ExampleContentBlockingDelegate : ContentBlocking.Delegate {
        private var mBlockedAds = 0
        private var mBlockedAnalytics = 0
        private var mBlockedSocial = 0
        private var mBlockedContent = 0
        private var mBlockedTest = 0
        private var mBlockedStp = 0
        fun clearCounters() {
            mBlockedAds = 0
            mBlockedAnalytics = 0
            mBlockedSocial = 0
            mBlockedContent = 0
            mBlockedTest = 0
            mBlockedStp = 0
        }

        fun logCounters() {
            Log.d(LOGTAG, "Trackers blocked: " + mBlockedAds + " ads, " +
                    mBlockedAnalytics + " analytics, " +
                    mBlockedSocial + " social, " +
                    mBlockedContent + " content, " +
                    mBlockedTest + " test, " +
                    mBlockedStp + "stp")
        }

        override fun onContentBlocked(session: GeckoSession,
                                      event: BlockEvent) {
            Log.d(LOGTAG, "onContentBlocked" +
                    " AT: " + event.antiTrackingCategory +
                    " SB: " + event.safeBrowsingCategory +
                    " CB: " + event.cookieBehaviorCategory +
                    " URI: " + event.uri)
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.TEST != 0) {
                mBlockedTest++
            }
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.AD != 0) {
                mBlockedAds++
            }
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.ANALYTIC != 0) {
                mBlockedAnalytics++
            }
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.SOCIAL != 0) {
                mBlockedSocial++
            }
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.CONTENT != 0) {
                mBlockedContent++
            }
            if (event.antiTrackingCategory and
                    ContentBlocking.AntiTracking.STP != 0) {
                mBlockedStp++
            }
        }

        override fun onContentLoaded(session: GeckoSession,
                                     event: BlockEvent) {
            Log.d(LOGTAG, "onContentLoaded" +
                    " AT: " + event.antiTrackingCategory +
                    " SB: " + event.safeBrowsingCategory +
                    " CB: " + event.cookieBehaviorCategory +
                    " URI: " + event.uri)
        }
    }

    private inner class ExampleMediaDelegate(private val mActivity: Activity) : MediaDelegate {
        private var mLastNotificationId = 100
        private var mNotificationId: Int? = null
        override fun onRecordingStatusChanged(session: GeckoSession, devices: Array<RecordingDevice>) {
            val message: String
            val icon: Int
            val notificationManager = NotificationManagerCompat.from(mActivity)
            var camera: RecordingDevice? = null
            var microphone: RecordingDevice? = null
            for (device in devices) {
                if (device.type == RecordingDevice.Type.CAMERA) {
                    camera = device
                } else if (device.type == RecordingDevice.Type.MICROPHONE) {
                    microphone = device
                }
            }
            if (camera != null && microphone != null) {
                Log.d(LOGTAG, "ExampleDeviceDelegate:onRecordingDeviceEvent display alert_mic_camera")
                message = resources.getString(R.string.device_sharing_camera_and_mic)
                icon = R.drawable.ic_mic_camera
            } else if (camera != null) {
                Log.d(LOGTAG, "ExampleDeviceDelegate:onRecordingDeviceEvent display alert_camera")
                message = resources.getString(R.string.device_sharing_camera)
                icon = R.drawable.ic_camera
            } else if (microphone != null) {
                Log.d(LOGTAG, "ExampleDeviceDelegate:onRecordingDeviceEvent display alert_mic")
                message = resources.getString(R.string.device_sharing_microphone)
                icon = R.drawable.ic_mic
            } else {
                Log.d(LOGTAG, "ExampleDeviceDelegate:onRecordingDeviceEvent dismiss any notifications")
                if (mNotificationId != null) {
                    notificationManager.cancel(mNotificationId!!)
                    mNotificationId = null
                }
                return
            }
            if (mNotificationId == null) {
                mNotificationId = ++mLastNotificationId
            }
            val intent = Intent(mActivity, BrowserActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(mActivity.applicationContext, 0, intent, 0)
            val builder = NotificationCompat.Builder(mActivity.applicationContext, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(resources.getString(R.string.app_name))
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
            notificationManager.notify(mNotificationId!!, builder.build())
        }
    }

    companion object {
        private const val LOGTAG = "BrowserActivity"
        private const val FULL_ACCESSIBILITY_TREE_EXTRA = "full_accessibility_tree"
        private const val SEARCH_URI_BASE = "https://www.google.com/search?q="
        private const val ACTION_SHUTDOWN = "org.mozilla.geckoview_example.SHUTDOWN"
        private const val CHANNEL_ID = "SmartCookieWeb"
        private const val REQUEST_FILE_PICKER = 1
        private const val REQUEST_PERMISSIONS = 2
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 3
        private var sGeckoRuntime: GeckoRuntime? = null
        private var sExtensionManager: WebExtensionManager? = null
        private val isForeground: Boolean
            private get() {
                val appProcessInfo = RunningAppProcessInfo()
                ActivityManager.getMyMemoryState(appProcessInfo)
                return appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                        appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE
            }
    }
}