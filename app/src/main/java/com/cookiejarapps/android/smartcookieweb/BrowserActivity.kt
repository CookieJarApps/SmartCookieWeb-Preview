package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.Html
import android.text.Spanned
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpanned
import com.cookiejarapps.android.smartcookieweb.popup.PopupMenu
import com.cookiejarapps.android.smartcookieweb.tabs.TabInstance
import com.cookiejarapps.android.smartcookieweb.tabs.TabManager
import org.mozilla.geckoview.*


class BrowserActivity : AppCompatActivity(), View.OnClickListener {
  private lateinit var geckoView: GeckoView
  private val geckoSession = GeckoSession()
  private lateinit var urlEditText: EditText
  private lateinit var progressView: ProgressBar
  private lateinit var urlBar: AutoCompleteTextView
  private var sGeckoRuntime: GeckoRuntime? = null
  private var mUseMultiprocess = false
  private var mFullAccessibilityTree = false
  private val mUseTrackingProtection = false
  private val mUsePrivateBrowsing = false
  private var mEnableRemoteDebugging = false
  private var mTabSessionManager: TabManager? = null
  private val USE_MULTIPROCESS_EXTRA = "use_multiprocess"
  private val FULL_ACCESSIBILITY_TREE_EXTRA = "full_accessibility_tree"
  private var trackersBlockedList: List<ContentBlocking.BlockEvent> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    mUseMultiprocess = intent.getBooleanExtra(
      this.USE_MULTIPROCESS_EXTRA,
      true
    )
    mEnableRemoteDebugging = true
    mFullAccessibilityTree = intent.getBooleanExtra(
      this.FULL_ACCESSIBILITY_TREE_EXTRA,
      false
    )

    setupToolbar()

    setupUrlEditText()

    setupGeckoView(savedInstanceState)

    progressView = findViewById(R.id.page_progress)

    urlBar = findViewById(R.id.location_view)

    findViewById<FrameLayout>(R.id.more_button).setOnClickListener(this)
  }

  private fun setupGeckoView(savedInstanceState: Bundle?) {
    geckoView = findViewById(R.id.geckoview)

    mTabSessionManager = TabManager()

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
        .remoteDebuggingEnabled(mEnableRemoteDebugging)
        .consoleOutput(true)
        .contentBlocking(
          ContentBlocking.Settings.Builder()
           // .categories(ContentBlocking.AT_DEFAULT)
            .build()
        )
        .aboutConfigEnabled(true)
      //  .crashHandler(ExampleCrashHandler::class.java)
     sGeckoRuntime = GeckoRuntime.create(this, runtimeSettingsBuilder.build())
    }

    /*if (savedInstanceState == null) {
      var session: TabInstance = intent.getParcelableArrayExtra("session").map { it as TabInstance }[0]
      if (session != null) {
        connectSession(session)
        if (!session.isOpen()) {
          session.open(sGeckoRuntime!!)
        }
        mFullAccessibilityTree = session.getSettings().getFullAccessibilityTree()
        mTabSessionManager!!.currentSession = session
        geckoView.setSession(session)
      } else {
        session = createSession()
        mTabSessionManager!!.currentSession = session
        geckoView.setSession(
          session
        )
        loadFromIntent(intent)
      }
    }*/

    //val runtime = GeckoRuntime.getDefault(this)
    geckoSession.open(sGeckoRuntime!!)
    geckoView.setSession(geckoSession)
    geckoSession.loadUri(INITIAL_URL)
    urlEditText.setText(INITIAL_URL)

    geckoSession.progressDelegate = createProgressDelegate()
    geckoSession.navigationDelegate = createNavigationDelegate()
    geckoSession.settings.useTrackingProtection = true
    geckoSession.contentBlockingDelegate = createBlockingDelegate()
  }

  private fun loadFromIntent(intent: Intent) {
    val uri = intent.data
    if (uri != null) {
      mTabSessionManager!!.currentSession.loadUri(uri.toString())
    }
  }

  private fun createSession(): TabInstance {
    val session: TabInstance = mTabSessionManager!!.newSession(
      GeckoSessionSettings.Builder()
        .usePrivateMode(mUsePrivateBrowsing)
        .useTrackingProtection(mUseTrackingProtection)
        .fullAccessibilityTree(mFullAccessibilityTree)
        .build()
    )
    connectSession(session)
    return session
  }

  private fun connectSession(session: GeckoSession) {
    session.progressDelegate = createProgressDelegate()
    session.navigationDelegate = createNavigationDelegate()
    session.contentBlockingDelegate = createBlockingDelegate()
    session.selectionActionDelegate = BasicSelectionActionDelegate(this)
    //updateTrackingProtection(session)
  }

  private fun setupToolbar() {
    setSupportActionBar(findViewById(R.id.toolbar))
    supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
  }

  private fun setupUrlEditText() {
    urlEditText = findViewById(R.id.location_view)
    urlEditText.setOnEditorActionListener(object : View.OnFocusChangeListener, TextView.OnEditorActionListener {

      override fun onFocusChange(view: View?, hasFocus: Boolean) = Unit

      override fun onEditorAction(textView: TextView, actionId: Int, event: KeyEvent?): Boolean {
        onCommit(textView.text.toString())
        textView.hideKeyboard()
        return true
      }

    })
  }

  override fun onClick(v: View) {
    val popUpClass = PopupMenu()
    when (v.id) {
      R.id.more_button -> {
        popUpClass.showPopupWindow(v, this)
      }
    }
  }

  fun onCommit(text: String) {
    if ((text.contains(".") || text.contains(":")) && !text.contains(" ")) {
      geckoSession.loadUri(text)
    } else {
      geckoSession.loadUri(SEARCH_URI_BASE + text)
    }
    geckoView.requestFocus()
  }

  private fun createProgressDelegate(): GeckoSession.ProgressDelegate {
    return object : GeckoSession.ProgressDelegate {
      override fun onPageStop(session: GeckoSession, success: Boolean) = Unit
      override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) = Unit
      override fun onPageStart(session: GeckoSession, url: String) = Unit

      override fun onProgressChange(session: GeckoSession, progress: Int) {
        progressView.progress = progress

        if (progress in 1..99) {
          progressView.visibility = View.VISIBLE
        } else {
          progressView.visibility = View.GONE
        }
      }
    }
  }

  fun back(){
    geckoSession.goBack()
  }

  override fun onBackPressed() {
    geckoSession.goBack()
    super.onBackPressed()
  }

  private fun createNavigationDelegate(): GeckoSession.NavigationDelegate{
    return object: GeckoSession.NavigationDelegate{
      override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest
      ): GeckoResult<AllowOrDeny>? {
        urlBar.setText(request.uri)
        return super.onLoadRequest(session, request)
      }
    }
  }

  private fun createBlockingDelegate(): ContentBlocking.Delegate {
    return object : ContentBlocking.Delegate {
      override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
        trackersBlockedList = trackersBlockedList + event
      }
    }
  }

  fun showAdDialog(){
    var friendlyURLs = getFriendlyTrackersUrls()
    if(friendlyURLs.isNullOrEmpty()){
      friendlyURLs = listOf(resources.getString(R.string.no_ads).toSpanned())
    }
    showDialog(friendlyURLs)
  }

  private fun getFriendlyTrackersUrls(): List<Spanned> {
    return trackersBlockedList.map { blockEvent ->

      val host = Uri.parse(blockEvent.uri).host
      val category = blockEvent.categoryToString()

      Html.fromHtml("<b><font color='#D55C7C'>[$category]</font></b> <br> $host", HtmlCompat.FROM_HTML_MODE_COMPACT)

    }
  }

  private fun ContentBlocking.BlockEvent.categoryToString(): String {
    val stringResource = when (antiTrackingCategory) {
      ContentBlocking.AntiTracking.NONE-> R.string.none
      ContentBlocking.AntiTracking.ANALYTIC -> R.string.analytic
      ContentBlocking.AntiTracking.AD -> R.string.ad
      ContentBlocking.AntiTracking.TEST -> R.string.test
      ContentBlocking.AntiTracking.SOCIAL -> R.string.social
      ContentBlocking.AntiTracking.CONTENT -> R.string.content
      else -> R.string.none
    }
    return getString(stringResource)
  }

}
