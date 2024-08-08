package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.settings.HomepageBackgroundChoice
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class CustomizationSettingsFragment : BaseSettingsFragment() {

    private lateinit var getBackgroundImageUri: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getBackgroundImageUri =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    // Handle the returned Uri
                    if (uri != null) {
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        UserPreferences(requireContext()).homepageBackgroundUrl = uri.toString()
                        Toast.makeText(
                            context,
                            requireContext().resources.getText(R.string.successful),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        UserPreferences(requireContext()).homepageBackgroundChoice =
                            HomepageBackgroundChoice.NONE.ordinal
                        Toast.makeText(
                            context,
                            requireContext().resources.getText(R.string.failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_customization)

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_move_navbar),
            isChecked = UserPreferences(requireContext()).shouldUseBottomToolbar,
            onCheckChange = {
                UserPreferences(requireContext()).shouldUseBottomToolbar = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_swipe_refresh),
            isChecked = UserPreferences(requireContext()).swipeToRefresh,
            onCheckChange = {
                UserPreferences(requireContext()).swipeToRefresh = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_hide_url_bar),
            isChecked = UserPreferences(requireContext()).hideBarWhileScrolling,
            onCheckChange = {
                UserPreferences(requireContext()).hideBarWhileScrolling = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_show_protocol),
            isChecked = UserPreferences(requireContext()).showUrlProtocol,
            onCheckChange = {
                UserPreferences(requireContext()).showUrlProtocol = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_show_tabs_in_grid),
            isChecked = UserPreferences(requireContext()).showTabsInGrid,
            onCheckChange = {
                UserPreferences(requireContext()).showTabsInGrid = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_swap_drawers),
            isChecked = UserPreferences(requireContext()).swapDrawers,
            onCheckChange = {
                UserPreferences(requireContext()).swapDrawers = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_shortcuts_visible),
            isChecked = UserPreferences(requireContext()).showShortcuts,
            onCheckChange = {
                UserPreferences(requireContext()).showShortcuts = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_stack_from_bottom),
            isChecked = UserPreferences(requireContext()).stackFromBottom,
            onCheckChange = {
                UserPreferences(requireContext()).stackFromBottom = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_bar_addon_list),
            onClick = { pickBarAddonList() }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_load_shortcut_icons),
            isChecked = UserPreferences(requireContext()).loadShortcutIcons,
            onCheckChange = {
                UserPreferences(requireContext()).loadShortcutIcons = it
            }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_app_theme_type),
            onClick = { pickAppTheme() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_web_theme_type),
            onClick = { pickWebTheme() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_homepage_background_image),
            onClick = { pickHomepageBackground() },
            summary = resources.getStringArray(R.array.homepage_background_image_types)[UserPreferences(
                requireContext()
            ).homepageBackgroundChoice]
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_auto_font_size),
            isChecked = UserPreferences(requireContext()).autoFontSize,
            onCheckChange = {
                UserPreferences(requireContext()).autoFontSize = it
                preferenceScreen.findPreference<SeekBarPreference>(resources.getString(R.string.key_font_size_factor))!!.isEnabled =
                    !it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        seekbarPreference(
            preference = requireContext().resources.getString(R.string.key_font_size_factor),
            isEnabled = !UserPreferences(requireContext()).autoFontSize,
            onStateChanged = {
                UserPreferences(requireContext()).fontSizeFactor = it.toFloat() / 100
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
            }
        )

    }

    private fun pickBarAddonList() {
        val context = requireContext()
        val userPreferences = UserPreferences(context)

        val allAddons = context.components.store.state.extensions.filter { it.value.enabled }.filter { it.value.browserAction != null || it.value.pageAction != null }

        // Get currently allowed add-on IDs
        val allowedAddonIds =
            if (UserPreferences(requireContext()).showAddonsInBar) {
                allAddons.map { it.value.id }
            } else userPreferences.barAddonsList.split(",").filter { it.isNotEmpty() }

        // Prepare the list for the dialog
        val addonNames = allAddons.map { it.value.name }
        val checkedItems = allAddons.map { allowedAddonIds.contains(it.value.id) }.toBooleanArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.bar_addon_list)
            .setMultiChoiceItems(addonNames.toTypedArray(), checkedItems) { _, _, _ ->
                // We'll handle the selection when the user clicks OK
            }
            .setPositiveButton(R.string.mozac_feature_prompts_ok) { _, _ ->
                if(UserPreferences(requireContext()).showAddonsInBar) {
                    UserPreferences(requireContext()).showAddonsInBar = false
                }

                // Save the selected add-ons
                val selectedAddonIds =
                    allAddons
                        .map { it.value }
                        .filterIndexed { index, _ -> checkedItems[index] }
                        .joinToString(",") { it.id }

                userPreferences.barAddonsList = selectedAddonIds

                Toast.makeText(
                    context,
                    R.string.app_restart,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()


    }

    private fun pickHomepageBackground() {
        val startingChoice = UserPreferences(requireContext()).homepageBackgroundChoice
        val singleItems =
            resources.getStringArray(R.array.homepage_background_image_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).homepageBackgroundChoice

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.homepage_background_image))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).homepageBackgroundChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ -> }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { _, which ->
                UserPreferences(requireContext()).homepageBackgroundChoice = which
                preferenceScreen.findPreference<Preference>(requireContext().resources.getString(R.string.key_homepage_background_image))!!.summary =
                    singleItems[which]
                when (which) {
                    HomepageBackgroundChoice.URL.ordinal -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle(resources.getString(R.string.homepage_background_image))
                            .setMessage(resources.getString(R.string.url))
                            .setView(R.layout.dialog_edittext)
                            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, _ ->
                                val editText =
                                    (dialog as AlertDialog).findViewById<EditText>(R.id.edit_text)
                                if (editText != null) {
                                    UserPreferences(requireContext()).homepageBackgroundUrl =
                                        editText.text.toString()
                                    Toast.makeText(
                                        context,
                                        requireContext().resources.getText(R.string.successful),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    UserPreferences(requireContext()).homepageBackgroundChoice =
                                        HomepageBackgroundChoice.NONE.ordinal
                                    Toast.makeText(
                                        context,
                                        requireContext().resources.getText(R.string.failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                                UserPreferences(requireContext()).homepageBackgroundChoice =
                                    HomepageBackgroundChoice.NONE.ordinal
                            }
                            .show()
                            .apply {
                                findViewById<EditText>(R.id.edit_text)?.setText(
                                    UserPreferences(
                                        requireContext()
                                    ).homepageBackgroundUrl
                                )
                            }
                    }

                    HomepageBackgroundChoice.GALLERY.ordinal -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        }
                        getBackgroundImageUri.launch(intent)
                    }
                }
            }
            .show()
    }

    private fun pickAppTheme() {
        val startingChoice = UserPreferences(requireContext()).appThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).appThemeChoice

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.theme))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).appThemeChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ -> }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                UserPreferences(requireContext()).appThemeChoice = which
            }
            .show()
    }

    private fun pickWebTheme() {
        val startingChoice = UserPreferences(requireContext()).webThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).webThemeChoice

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.theme))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).webThemeChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ -> }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                UserPreferences(requireContext()).webThemeChoice = which
            }
            .show()
    }

}