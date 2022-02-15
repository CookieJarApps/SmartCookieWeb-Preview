package com.cookiejarapps.android.smartcookieweb.addons

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentAddOnsBinding
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentBrowserBinding
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.*
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import org.mozilla.gecko.util.ThreadUtils
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.collections.ArrayList


// Fragment used for managing add-ons.

class AddonsFragment : Fragment(), AddonsManagerAdapterDelegate {
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private val scope = CoroutineScope(Dispatchers.IO)
    private var adapter: AddonsAdapter? = null
    private var addons: List<Addon>? = null

    private var _binding: FragmentAddOnsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        _binding = FragmentAddOnsBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        bindRecyclerView(rootView)
    }

    override fun onStart() {
        super.onStart()

        this@AddonsFragment.view?.let { view ->
            bindRecyclerView(view)
            bindSpinner(view)
        }

        findPreviousPermissionDialogFragment()?.let { dialog ->
            dialog.onPositiveButtonClicked = onConfirmPermissionButtonClicked
        }

        findPreviousInstallationDialogFragment()?.let { dialog ->
            dialog.onConfirmButtonClicked = onConfirmInstallationButtonClicked
            dialog.addonCollectionProvider = requireContext().components.addonCollectionProvider
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = getString(R.string.search)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return filterAddonByQuery(query.trim())
            }

            override fun onQueryTextChange(query: String): Boolean {
                return filterAddonByQuery(query.trim())
            }
        })
    }

    private fun filterAddonByQuery(query: String): Boolean {
        val filteredList = arrayListOf<Addon>()

        addons?.forEach { addon ->
            val names = addon.translatableName
            names["en"]?.let { name ->
                if (name.toLowerCase(Locale.ENGLISH).contains(query.toLowerCase(Locale.ENGLISH))) {
                    filteredList.add(addon)
                }
            }
        }

        adapter?.updateAddons(filteredList)

        if (filteredList.isEmpty()) {
            binding.addOnsNoResults.visibility = View.VISIBLE
            binding.addOnsList.visibility = View.GONE
        } else {
            view?.let { view ->
                binding.addOnsNoResults.visibility = View.GONE
                binding.addOnsList.visibility = View.VISIBLE
            }
        }

        return true
    }

    private fun bindSpinner(rootView: View) {
        val users = arrayOf(
            requireContext().resources.getString(R.string.sort_rating),
            requireContext().resources.getString(R.string.sort_a_z),
            requireContext().resources.getString(R.string.sort_z_a)
        )

        spinner = rootView.findViewById(R.id.sort_spinner)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, users)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(adapter)
        spinner.setSelection(UserPreferences(requireContext()).addonSort)

        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                row: Long
            ) {
                UserPreferences(requireContext()).addonSort = position
                if(recyclerView.adapter != null && recyclerView.adapter is AddonsAdapter){
                    (recyclerView.adapter as AddonsAdapter).reSort()
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                return
            }
        }
    }

    private fun bindRecyclerView(rootView: View) {
        recyclerView = binding.addOnsList
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        scope.launch {
            try {
                addons = requireContext().components.addonManager.getAddons()

                scope.launch(Dispatchers.Main) {
                    context?.let {
                        val addonCollectionProvider = requireContext().components.addonCollectionProvider

                        val style = AddonsAdapter.Style(
                            dividerColor = requireContext().theme.resolveAttribute(android.R.attr.textColorSecondary),
                            dividerHeight = R.dimen.mozac_browser_menu_item_divider_height,
                            addonNameTextColor = requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary),
                            sectionsTextColor = requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary)
                        )

                        binding.addOnsNoResults.isVisible = false
                        binding.addOnsLoading.isVisible = false

                        if (adapter == null) {
                            adapter = AddonsAdapter(
                                addonCollectionProvider = addonCollectionProvider,
                                addonsManagerDelegate = this@AddonsFragment,
                                addons = addons!!,
                                style = style,
                                userPreferences = UserPreferences(requireContext())
                            )
                            recyclerView.adapter = adapter

                            val bundle: Bundle? = arguments
                            if (bundle?.getString("ADDON_ID") != null) {
                                val addonId = bundle.getString("ADDON_ID", "")
                                val addonUrl = bundle.getString("ADDON_URL", "")
                                installAddonById(addons!!, addonId, addonUrl)
                            }
                        } else {
                            adapter?.updateAddons(addons!!)
                        }
                    }
                }
            } catch (e: AddonManagerException) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        R.string.mozac_feature_addons_failed_to_query_add_ons,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.addOnsNoResults.isVisible = true
                }
            }
        }
    }

    @VisibleForTesting
    internal fun installAddonById(supportedAddons: List<Addon>, id: String, url: String) {
        val addonToInstall = supportedAddons.find { it.downloadId == id }
        if (addonToInstall == null) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setMessage(resources.getString(R.string.addon_not_available))
                .setPositiveButton(R.string.mozac_feature_prompts_ok) { dialog, id ->

                    val loadingDialog = ProgressDialog.show(
                        activity, "",
                        requireContext().resources.getString(R.string.loading), true
                    )

                    components.engine.installWebExtension("", url, onSuccess = {
                        CoroutineScope(Dispatchers.IO).launch {
                            addons = requireContext().components.addonManager.getAddons()
                            ThreadUtils.runOnUiThread {
                                loadingDialog.dismiss()
                                Toast.makeText(
                                    requireContext(),
                                    requireContext().resources.getString(R.string.installed),
                                    Toast.LENGTH_LONG
                                ).show()
                                adapter?.updateAddons(addons!!)
                            }
                        }
                    },
                        onError = { _, _ ->
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), requireContext().resources.getString(R.string.error), Toast.LENGTH_LONG).show()
                        })
                }
                .setNegativeButton(R.string.cancel) { dialog, id ->
                    dialog.cancel()
                }

            builder.create().show()
        } else {
            if (addonToInstall.isInstalled()) {
                // error
            } else {
                showPermissionDialog(addonToInstall)
            }
        }
    }

    override fun onAddonItemClicked(addon: Addon) {
        val context = requireContext()

        if (addon.isInstalled()) {
            val intent = Intent(context, InstalledAddonDetailsActivity::class.java)
            intent.putExtra("add_on", addon)
            context.startActivity(intent)
        } else {
            val intent = Intent(context, AddonDetailsActivity::class.java)
            intent.putExtra("add_on", addon)
            this.startActivity(intent)
        }
    }

    override fun onInstallAddonButtonClicked(addon: Addon) {
        showPermissionDialog(addon)
    }

    override fun onNotYetSupportedSectionClicked(unsupportedAddons: List<Addon>) {
        val intent = Intent(context, NotYetSupportedAddonActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("add_ons", ArrayList(unsupportedAddons))
        requireContext().startActivity(intent)
    }

    private fun isAlreadyADialogCreated(): Boolean {
        return findPreviousPermissionDialogFragment() != null && findPreviousInstallationDialogFragment() != null
    }

    private fun findPreviousPermissionDialogFragment(): PermissionsDialogFragment? {
        return parentFragmentManager.findFragmentByTag(
            PERMISSIONS_DIALOG_FRAGMENT_TAG
        ) as? PermissionsDialogFragment
    }

    private fun findPreviousInstallationDialogFragment(): AddonInstallationDialogFragment? {
        return parentFragmentManager.findFragmentByTag(
            INSTALLATION_DIALOG_FRAGMENT_TAG
        ) as? AddonInstallationDialogFragment
    }

    private fun showPermissionDialog(addon: Addon) {
        if (isInstallationInProgress) {
            return
        }

        val dialog = PermissionsDialogFragment.newInstance(
            addon = addon,
            onPositiveButtonClicked = onConfirmPermissionButtonClicked
        )

        if (!isAlreadyADialogCreated() && isAdded) {
            dialog.show(parentFragmentManager, PERMISSIONS_DIALOG_FRAGMENT_TAG)
        }
    }

    private fun showInstallationDialog(addon: Addon) {
        if (isInstallationInProgress) {
            return
        }
        val addonCollectionProvider = requireContext().components.addonCollectionProvider
        val dialog = AddonInstallationDialogFragment.newInstance(
            addon = addon,
            addonCollectionProvider = addonCollectionProvider,
            onConfirmButtonClicked = onConfirmInstallationButtonClicked
        )

        if (!isAlreadyADialogCreated() && isAdded) {
            dialog.show(parentFragmentManager, INSTALLATION_DIALOG_FRAGMENT_TAG)
        }
    }

    private val onConfirmInstallationButtonClicked: ((Addon, Boolean) -> Unit) = { addon, allowInPrivateBrowsing ->
        if (allowInPrivateBrowsing) {
            requireContext().components.addonManager.setAddonAllowedInPrivateBrowsing(
                addon,
                allowInPrivateBrowsing
            )
        }
    }

    private val onConfirmPermissionButtonClicked: ((Addon) -> Unit) = { addon ->
        binding.addonProgressOverlay.root.visibility = View.VISIBLE
        isInstallationInProgress = true

        val installOperation = requireContext().components.addonManager.installAddon(
            addon,
            onSuccess = { installedAddon ->
                context?.let {
                    adapter?.updateAddon(installedAddon)
                    binding.addonProgressOverlay.root.visibility = View.GONE
                    isInstallationInProgress = false
                    showInstallationDialog(installedAddon)
                }
            },
            onError = { _, e ->
                // No need to display an error message if installation was cancelled by the user.
                if (e !is CancellationException) {
                    Toast.makeText(
                        requireContext(), getString(
                            R.string.mozac_feature_addons_failed_to_install,
                            addon.translateName(requireContext())
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                binding.addonProgressOverlay.root.visibility = View.GONE
                isInstallationInProgress = false
            }
        )

        binding.addonProgressOverlay.cancelButton.setOnClickListener {
            MainScope().launch {
                // Hide the installation progress overlay once cancellation is successful.
                if (installOperation.cancel().await()) {
                    binding.addonProgressOverlay.root.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Whether or not an add-on installation is in progress.
     */
    private var isInstallationInProgress = false

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
        private const val INSTALLATION_DIALOG_FRAGMENT_TAG = "ADDONS_INSTALLATION_DIALOG_FRAGMENT"
    }
}
