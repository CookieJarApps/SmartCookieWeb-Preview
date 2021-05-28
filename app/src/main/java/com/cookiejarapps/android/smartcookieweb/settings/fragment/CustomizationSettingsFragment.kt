package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class CustomizationSettingsFragment : BaseSettingsFragment() {

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
            isEnabled = !UserPreferences(requireContext()).stackFromBottom,
            onCheckChange = {
                UserPreferences(requireContext()).showTabsInGrid = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
                preferenceScreen.findPreference<SwitchPreferenceCompat>(requireContext().resources.getString(R.string.key_stack_from_bottom))?.isEnabled = !it
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
            preference = requireContext().resources.getString(R.string.key_stack_from_bottom),
            isChecked = UserPreferences(requireContext()).stackFromBottom,
            isEnabled = !UserPreferences(requireContext()).showTabsInGrid,
            onCheckChange = {
                UserPreferences(requireContext()).stackFromBottom = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
                preferenceScreen.findPreference<SwitchPreferenceCompat>(requireContext().resources.getString(R.string.key_show_tabs_in_grid))?.isEnabled = !it
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_show_addons_in_bar),
            isChecked = UserPreferences(requireContext()).showAddonsInBar,
            onCheckChange = {
                UserPreferences(requireContext()).showAddonsInBar = it
                Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
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

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_auto_font_size),
            isChecked = UserPreferences(requireContext()).autoFontSize,
            onCheckChange = {
                UserPreferences(requireContext()).autoFontSize = it
                preferenceScreen.findPreference<SeekBarPreference>(resources.getString(R.string.key_font_size_factor))!!.isEnabled = !it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        seekbarPreference(
                preference = requireContext().resources.getString(R.string.key_font_size_factor),
                isEnabled = !UserPreferences(requireContext()).autoFontSize,
                onStateChanged = {
                    UserPreferences(requireContext()).fontSizeFactor = it.toFloat() / 100
                    Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
                }
        )

    }

    private fun pickAppTheme(){
        val startingChoice = UserPreferences(requireContext()).appThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).appThemeChoice

        MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.theme))
                .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                    UserPreferences(requireContext()).appThemeChoice = startingChoice
                }
                .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->}
                .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                    UserPreferences(requireContext()).appThemeChoice = which
                }
                .show()
    }

    private fun pickWebTheme(){
        val startingChoice = UserPreferences(requireContext()).webThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).webThemeChoice

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.theme))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).webThemeChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->}
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                UserPreferences(requireContext()).webThemeChoice = which
            }
            .show()
    }

}