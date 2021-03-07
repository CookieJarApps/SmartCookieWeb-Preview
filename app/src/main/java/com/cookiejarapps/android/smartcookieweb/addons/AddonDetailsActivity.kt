package com.cookiejarapps.android.smartcookieweb.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.showInformationDialog
import mozilla.components.feature.addons.ui.translateName
import mozilla.components.feature.addons.ui.translateDescription
import mozilla.components.feature.addons.update.DefaultAddonUpdater
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

// An activity to show the details of an add-on.
class AddonDetailsActivity : AppCompatActivity() {

    private val updateAttemptStorage: DefaultAddonUpdater.UpdateAttemptStorage by lazy {
        DefaultAddonUpdater.UpdateAttemptStorage(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(UserPreferences(this).followSystem){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setContentView(R.layout.activity_add_on_details)
        val addon = requireNotNull(intent.getParcelableExtra<Addon>("add_on"))
        initViews(addon)
    }

    private fun initViews(addon: Addon) {
        title = addon.translateName(this)

        val detailsView = findViewById<TextView>(R.id.details)
        val detailsText = addon.translateDescription(this)

        val htmlText = detailsText.replace("\n", "<br>")
        val text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)

        detailsView.text = text
        detailsView.movementMethod = LinkMovementMethod.getInstance()

        val authorsView = findViewById<TextView>(R.id.author_text)

        val authorText = addon.authors.joinToString { author ->
            author.name + " \n"
        }

        authorsView.text = authorText

        val versionView = findViewById<TextView>(R.id.version_text)
        versionView.text = addon.installedState?.version?.ifEmpty { addon.version } ?: addon.version

        if (addon.isInstalled()) {
            versionView.setOnLongClickListener {
                showUpdaterDialog(addon)
                true
            }
        }

        val lastUpdatedView = findViewById<TextView>(R.id.last_updated_text)
        lastUpdatedView.text = formatDate(addon.updatedAt)

        findViewById<View>(R.id.home_page_text).setOnClickListener {
            val intent =
                    Intent(Intent.ACTION_VIEW).setData(Uri.parse(addon.siteUrl))
            startActivity(intent)
        }

        addon.rating?.let {
            val ratingNum = findViewById<TextView>(R.id.users_count)
            val ratingView = findViewById<RatingBar>(R.id.rating_view)

            ratingNum.text = getFormattedAmount(it.reviews)

            val ratingContentDescription = getString(R.string.mozac_feature_addons_rating_content_description)
            ratingView.contentDescription = String.format(ratingContentDescription, it.average)
            ratingView.rating = it.average
        }
    }

    private fun showUpdaterDialog(addon: Addon) {
        val context = this@AddonDetailsActivity
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val updateAttempt = updateAttemptStorage.findUpdateAttemptBy(addon.id)
            updateAttempt?.let {
                withContext(Dispatchers.Main) {
                    it.showInformationDialog(context)
                }
            }
        }
    }

    private fun formatDate(text: String): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return DateFormat.getDateInstance().format(formatter.parse(text)!!)
    }
}
