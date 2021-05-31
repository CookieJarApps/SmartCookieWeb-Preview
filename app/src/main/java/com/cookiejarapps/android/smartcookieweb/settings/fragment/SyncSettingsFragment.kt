package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.sync.LoginFragment
import com.cookiejarapps.android.smartcookieweb.sync.SyncItemFragment
import kotlinx.coroutines.*
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.sync.*
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import mozilla.components.lib.dataprotect.generateEncryptionKey
import mozilla.components.service.fxa.FxaAuthData
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.sync.GlobalSyncableStoreProvider
import mozilla.components.service.fxa.sync.SyncStatusObserver
import mozilla.components.service.fxa.toAuthType
import mozilla.components.service.sync.logins.SyncableLoginsStorage
import org.mozilla.gecko.util.ThreadUtils.runOnUiThread
import java.lang.Exception

private const val PASSWORDS_ENCRYPTION_KEY_STRENGTH = 256

class SyncSettingsFragment : BaseSettingsFragment() {

    private lateinit var loadingDialog: ProgressDialog

    private val historyStorage = lazy {
        PlacesHistoryStorage(requireContext())
    }

    private val bookmarksStorage = lazy {
        PlacesBookmarksStorage(requireContext())
    }

    private val securePreferences by lazy { SecureAbove22Preferences(requireContext(), "key_store") }

    private val passwordsEncryptionKey by lazy {
        securePreferences.getString(SyncEngine.Passwords.nativeName)
            ?: generateEncryptionKey(PASSWORDS_ENCRYPTION_KEY_STRENGTH).also {
                securePreferences.putString(SyncEngine.Passwords.nativeName, it)
            }
    }

    private val passwordsStorage = lazy { SyncableLoginsStorage(requireContext(), passwordsEncryptionKey) }


    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_sync)

        val bundle = this.arguments
        if (bundle != null) {
            val code = bundle.getString("CODE", "")
            val state = bundle.getString("STATE", "")
            val action = bundle.getString("ACTION", "")

            if(bundle.getBoolean("LOGGEDIN", false)){
                GlobalScope.launch(Dispatchers.Main) {
                    components.accountManager.finishAuthentication(
                        FxaAuthData(action.toAuthType(), code = code, state = state)
                    )
                }
            }
        }

        loadingDialog = ProgressDialog.show(
            activity, "",
            requireContext().resources.getString(R.string.loading), true
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_sync_enabled),
            isChecked = UserPreferences(requireContext()).syncEnabled,
            onCheckChange = {
                UserPreferences(requireContext()).syncEnabled = it
                updateEnabled()
            }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_sync_login),
            onClick = {
                GlobalScope.launch(Dispatchers.Main) {
                    requireContext().components.accountManager.beginAuthentication()
                        ?.let {
                            runOnUiThread {
                                requireActivity().supportFragmentManager.beginTransaction().apply {
                                    replace(
                                        R.id.container, LoginFragment.create(
                                            it,
                                            "https://accounts.firefox.com/oauth/success/3c49430b43dfba77"
                                        )
                                    )
                                    addToBackStack(null)
                                    commit()
                                }
                            }
                        }
                }
            }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_sync_logout),
            onClick = {
                GlobalScope.launch(Dispatchers.Main) {
                    requireContext().components.accountManager.logout()
                }
            }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_synced_items),
            onClick = {
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(
                        R.id.container, SyncItemFragment()
                    )
                    addToBackStack(null)
                    commit()
                }
            }
        )

        // Hide the loading dialog if we're not logged in yet
        if(components.accountManager.authenticatedAccount() == null){
            loadingDialog.dismiss()
        }

        //TODO: Receive commands as wel as sending
        components.accountManager.register(accountObserver, owner = this, autoPause = true)
        components.accountManager.registerForSyncEvents(syncObserver, owner = this, autoPause = true)

        GlobalSyncableStoreProvider.configureStore(SyncEngine.History to historyStorage)
        GlobalSyncableStoreProvider.configureStore(SyncEngine.Bookmarks to bookmarksStorage)
        GlobalSyncableStoreProvider.configureStore(SyncEngine.Passwords to passwordsStorage)

        GlobalScope.launch(Dispatchers.Main) {
            components.accountManager.start()
        }

        updateEnabled()
    }


    //TODO: Strings here
        private val syncObserver = object : SyncStatusObserver {
        override fun onStarted() {
            runOnUiThread {
                Toast.makeText(context, "SYNCING...", Toast.LENGTH_LONG).show()
            }
        }

        override fun onIdle() {
            runOnUiThread {
                Toast.makeText(context, "SYNCED!", Toast.LENGTH_LONG).show()
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_synced_items))!!.isVisible = true
            }
        }

        override fun onError(error: Exception?) {
            runOnUiThread {
                Toast.makeText(context, "ERROR!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val accountObserver = object : AccountObserver {
        override fun onLoggedOut() {
            runOnUiThread{
                loadingDialog.dismiss()
                Toast.makeText(context, "LOGGED OUT!", Toast.LENGTH_LONG).show()
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_login))!!.isVisible = true
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_logout))!!.isEnabled = false
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_synced_items))!!.isEnabled = false
            }
        }

        override fun onAuthenticationProblems() {
            runOnUiThread {
                loadingDialog.dismiss()
                Toast.makeText(context, "NOT AUTHENTICATED!", Toast.LENGTH_LONG).show()
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_login))!!.isVisible = true
            }
        }

        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
            runOnUiThread {
                loadingDialog.dismiss()
                Toast.makeText(context, "AUTHENTICATED!", Toast.LENGTH_LONG).show()
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_logout))!!.isVisible = true
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_login))!!.isVisible = false
            }
        }

        override fun onProfileUpdated(profile: Profile) {
            runOnUiThread {
                loadingDialog.dismiss()
                //Toast.makeText(context, "PROFILE UPDATED!", Toast.LENGTH_LONG).show()
            }
        }

        override fun onFlowError(error: AuthFlowError) {
            runOnUiThread {
                loadingDialog.dismiss()
                Toast.makeText(context, "ERROR!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateEnabled(){
        val enabled = UserPreferences(requireContext()).syncEnabled
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_login))!!.isEnabled = enabled
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_logout))!!.isEnabled = enabled
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_sync_type))!!.isEnabled = enabled
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_synced_items))!!.isEnabled = enabled
    }
}