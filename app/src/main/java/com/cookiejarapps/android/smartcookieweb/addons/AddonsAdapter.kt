package com.cookiejarapps.android.smartcookieweb.addons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.AddonSortType
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.amo.AddonCollectionProvider
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import mozilla.components.feature.addons.ui.CustomViewHolder
import mozilla.components.feature.addons.ui.CustomViewHolder.AddonViewHolder
import mozilla.components.feature.addons.ui.CustomViewHolder.SectionViewHolder
import mozilla.components.feature.addons.ui.CustomViewHolder.UnsupportedSectionViewHolder
import mozilla.components.feature.addons.ui.translateName
import mozilla.components.feature.addons.ui.translateSummary
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

private const val VIEW_HOLDER_TYPE_SECTION = 0
private const val VIEW_HOLDER_TYPE_ADDON = 1

/**
 * An adapter for displaying add-on items. This will display information related to the state of
 * an add-on such as recommended, unsupported or installed. In addition, it will perform actions
 * such as installing an add-on. Compatible with AddonsManagerAdapter.
 *
 * Unlike AddonsManagerAdapter in Mozilla Android Components, this correctly recognises sideloaded
 * add-ons as their own category, instead of add-ons left over from a previous Fennec install.
 *
 * @property addonCollectionProvider Provider of AMO collection API.
 * @property addonsManagerDelegate Delegate that will provides method for handling the add-on items.
 * @param addons The list of add-on based on the AMO store.
 * @property style Indicates how items should look like.
 * @property excludedAddonIDs The list of add-on IDs to be excluded from the recommended section.
 */
@Suppress("LargeClass")
class AddonsAdapter(
    private val addonCollectionProvider: AddonCollectionProvider,
    private val addonsManagerDelegate: AddonsManagerAdapterDelegate,
    private val addons: List<Addon>,
    private val style: Style? = null,
    private val excludedAddonIDs: List<String> = emptyList(),
    private val userPreferences: UserPreferences
) : ListAdapter<Any, CustomViewHolder>(DifferCallback) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @VisibleForTesting
    internal var addonsMap: MutableMap<String, Addon> = addons.associateBy({ it.id }, { it }).toMutableMap()

    init {
        submitList(createListWithSections(addons))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return when (viewType) {
            VIEW_HOLDER_TYPE_ADDON -> createAddonViewHolder(parent)
            VIEW_HOLDER_TYPE_SECTION -> createSectionViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown ViewHolder")
        }
    }

    private fun createSectionViewHolder(parent: ViewGroup): CustomViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.mozac_feature_addons_section_item, parent, false)
        val titleView = view.findViewById<TextView>(R.id.title)
        val divider = view.findViewById<View>(R.id.divider)
        return SectionViewHolder(view, titleView, divider)
    }

    private fun createAddonViewHolder(parent: ViewGroup): AddonViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.mozac_feature_addons_item, parent, false)
        val iconView = view.findViewById<ImageView>(R.id.add_on_icon)
        val titleView = view.findViewById<TextView>(R.id.add_on_name)
        val summaryView = view.findViewById<TextView>(R.id.add_on_description)
        val ratingView = view.findViewById<RatingBar>(R.id.rating)
        val ratingAccessibilityView = view.findViewById<TextView>(R.id.rating_accessibility)
        val userCountView = view.findViewById<TextView>(R.id.users_count)
        val addButton = view.findViewById<ImageView>(R.id.add_button)
        val allowedInPrivateBrowsingLabel = view.findViewById<ImageView>(R.id.allowed_in_private_browsing_label)
        return AddonViewHolder(
            view,
            iconView,
            titleView,
            summaryView,
            ratingView,
            ratingAccessibilityView,
            userCountView,
            addButton,
            allowedInPrivateBrowsingLabel
        )
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Addon -> VIEW_HOLDER_TYPE_ADDON
            is Section -> VIEW_HOLDER_TYPE_SECTION
            else -> throw IllegalArgumentException("Unknown ViewType")
        }
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is SectionViewHolder -> bindSection(holder, item as Section, position)
            is AddonViewHolder -> bindAddon(holder, item as Addon)
        }
    }

    internal fun bindSection(holder: SectionViewHolder, section: Section, position: Int) {
        holder.titleView.setText(section.title)

        style?.let {
            holder.divider.isVisible = it.visibleDividers && position != 0
            it.maybeSetSectionsTextColor(holder.titleView)
            it.maybeSetSectionsTypeFace(holder.titleView)
            it.maybeSetSectionsDividerStyle(holder.divider)
        }
    }

    internal fun bindAddon(holder: AddonViewHolder, addon: Addon) {
        val context = holder.itemView.context
        addon.rating?.let {
            val userCount = context.getString(R.string.mozac_feature_addons_user_rating_count_2)
            val ratingContentDescription =
                String.format(
                    context.getString(R.string.mozac_feature_addons_rating_content_description),
                    it.average
                )
            holder.ratingView.contentDescription = ratingContentDescription
            holder.ratingAccessibleView.text = ratingContentDescription
            holder.ratingView.rating = it.average
            holder.userCountView.text = String.format(userCount, getFormattedAmount(it.reviews))
        } ?: run {
            holder.ratingView.visibility = View.GONE
            holder.userCountView.visibility = View.GONE
        }

        holder.titleView.text =
            if (addon.translatableName.isNotEmpty()) {
                addon.translateName(context)
            } else {
                addon.id
            }

        if (!addon.translatableSummary.isEmpty()) {
            holder.summaryView.text = addon.translateSummary(context)
        } else {
            holder.summaryView.visibility = View.GONE
        }

        holder.itemView.tag = addon
        holder.itemView.setOnClickListener {
            addonsManagerDelegate.onAddonItemClicked(addon)
        }

        holder.addButton.isVisible = !addon.isInstalled()
        holder.addButton.setOnClickListener {
            if (!addon.isInstalled()) {
                addonsManagerDelegate.onInstallAddonButtonClicked(addon)
            }
        }

        holder.allowedInPrivateBrowsingLabel.isVisible = addon.isAllowedInPrivateBrowsing()
        style?.maybeSetPrivateBrowsingLabelDrawale(holder.allowedInPrivateBrowsingLabel)

        fetchIcon(addon, holder.iconView)
        style?.maybeSetAddonNameTextColor(holder.titleView)
        style?.maybeSetAddonSummaryTextColor(holder.summaryView)
    }

    internal fun fetchIcon(addon: Addon, iconView: ImageView, scope: CoroutineScope = this.scope): Job {
        return scope.launch {
            try {
                // Don't fade in icon if it loads in less than 1 second
                val startTime = System.currentTimeMillis()
                val iconBitmap = addonCollectionProvider.getAddonIconBitmap(addon)
                val loadTime: Double = (System.currentTimeMillis() - startTime) / 1000.toDouble()
                if (iconBitmap != null) {
                    scope.launch(Main) {
                        if (loadTime < 1) {
                            iconView.setImageDrawable(BitmapDrawable(iconView.resources, iconBitmap))
                        } else {
                            setWithAnimation(iconView, iconBitmap)
                        }
                    }
                } else if (addon.installedState?.icon != null) {
                    scope.launch(Main) {
                        iconView.setImageDrawable(BitmapDrawable(iconView.resources, addon.installedState!!.icon))
                    }
                }
                else{
                    // If icon is null, load placeholder
                    scope.launch(Main) {
                        val context = iconView.context
                        val att = context.theme.resolveAttribute(android.R.attr.textColorPrimary)
                        val drawable = ContextCompat.getDrawable(context, R.drawable.mozac_ic_extensions)
                        drawable?.setColorFilter(ContextCompat.getColor(context, att), PorterDuff.Mode.SRC_ATOP)
                        iconView.setImageDrawable(
                            drawable
                        )
                    }
                }
            } catch (e: IOException) {
                // If icon load fails, load placeholder
                scope.launch(Main) {
                    val context = iconView.context
                    val att = context.theme.resolveAttribute(android.R.attr.textColorPrimary)
                    val drawable = ContextCompat.getDrawable(context, R.drawable.mozac_ic_extensions)
                    drawable?.setColorFilter(ContextCompat.getColor(context, att), PorterDuff.Mode.SRC_ATOP)
                    iconView.setImageDrawable(
                        drawable
                    )
                }
            }
        }
    }

    internal fun createListWithSections(addons: List<Addon>): List<Any> {
        val addonList = ArrayList<Any>()
        val recommendedAddons = ArrayList<Addon>()
        val installedAddons = ArrayList<Addon>()
        val disabledAddons = ArrayList<Addon>()
        val sideloadedAddons = ArrayList<Addon>()

        addons.forEach { addon ->
            when {
                addon.inRecommendedSection() -> recommendedAddons.add(addon)
                addon.inInstalledSection() -> installedAddons.add(addon)
                addon.inDisabledSection() -> disabledAddons.add(addon)
                addon.inSideloadedSection() -> sideloadedAddons.add(addon)
            }
        }

        //TODO: When uninstalling and installing add-ons, whole thing must be refreshed. LOOK INTO THIS!!! 30 mins then publish
        sort(recommendedAddons, userPreferences)
        sort(installedAddons, userPreferences)
        sort(disabledAddons, userPreferences)
        sort(sideloadedAddons, userPreferences)

        // Add sideloaded section
        if (sideloadedAddons.isNotEmpty()) {
            addonList.add(Section(R.string.sideloaded_addons, false))
            addonList.addAll(sideloadedAddons)
        }

        // Add installed section and addons if available
        if (installedAddons.isNotEmpty()) {
            addonList.add(Section(R.string.mozac_feature_addons_enabled, false))
            addonList.addAll(installedAddons)
        }

        // Add disabled section and addons if available
        if (disabledAddons.isNotEmpty()) {
            addonList.add(Section(R.string.mozac_feature_addons_disabled_section, true))
            addonList.addAll(disabledAddons)
        }

        // Add recommended section and addons if available
        if (recommendedAddons.isNotEmpty()) {
            addonList.add(Section(R.string.mozac_feature_addons_recommended_section, true))
            addonList.addAll(recommendedAddons)
        }

        return addonList
    }

    internal data class Section(@StringRes val title: Int, val visibleDivider: Boolean = true)

    data class Style(
        @ColorRes
        val sectionsTextColor: Int? = null,
        @ColorRes
        val addonNameTextColor: Int? = null,
        @ColorRes
        val addonSummaryTextColor: Int? = null,
        val sectionsTypeFace: Typeface? = null,
        @DrawableRes
        val addonAllowPrivateBrowsingLabelDrawableRes: Int? = null,
        val visibleDividers: Boolean = true,
        @ColorRes
        val dividerColor: Int? = null,
        @DimenRes
        val dividerHeight: Int? = null
    ) {
        internal fun maybeSetSectionsTextColor(textView: TextView) {
            sectionsTextColor?.let {
                val color = ContextCompat.getColor(textView.context, it)
                textView.setTextColor(color)
            }
        }

        internal fun maybeSetSectionsTypeFace(textView: TextView) {
            sectionsTypeFace?.let {
                textView.typeface = it
            }
        }

        internal fun maybeSetAddonNameTextColor(textView: TextView) {
            addonNameTextColor?.let {
                val color = ContextCompat.getColor(textView.context, it)
                textView.setTextColor(color)
            }
        }

        internal fun maybeSetAddonSummaryTextColor(textView: TextView) {
            addonSummaryTextColor?.let {
                val color = ContextCompat.getColor(textView.context, it)
                textView.setTextColor(color)
            }
        }

        internal fun maybeSetPrivateBrowsingLabelDrawale(imageView: ImageView) {
            addonAllowPrivateBrowsingLabelDrawableRes?.let {
                imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, it))
            }
        }

        internal fun maybeSetSectionsDividerStyle(divider: View) {
            dividerColor?.let {
                divider.setBackgroundColor(it)
            }
            dividerHeight?.let {
                divider.layoutParams.height = divider.context.resources.getDimensionPixelOffset(it)
            }
        }
    }

    fun sort(array: ArrayList<Addon>, userPreferences: UserPreferences) {
        if(userPreferences.addonSort == AddonSortType.RATING.ordinal){
            array.sortWith { item1, item2 ->
                if (item1.rating != null || item2.rating != null) {
                    if (item1.rating!!.average == 0F && item2.rating!!.average == 0F) {
                        item1.translatableName["en-us"]!!.compareTo(item2.translatableName["en-us"]!!)
                    } else if (item1.rating!!.average == item2.rating!!.average) {
                        -item1.rating!!.reviews.compareTo(item2.rating!!.reviews)
                    } else {
                        -item1.rating!!.average.compareTo(item2.rating!!.average)
                    }
                } else {
                    if (item1.translatableName["en-us"] != null && item2.translatableName["en-us"] != null) {
                        item1.translatableName["en-us"]!!.compareTo(item2.translatableName["en-us"]!!)
                    } else {
                        item1.id.compareTo(item2.id)
                    }
                }
            }
        }
        else if(userPreferences.addonSort == AddonSortType.A_Z.ordinal){
            array.sortWith { item1, item2 ->
                if (item1.translatableName["en-us"] != null && item2.translatableName["en-us"] != null) {
                    item1.translatableName["en-us"]!!.compareTo(
                        item2.translatableName["en-us"]!!,
                        true
                    )
                } else {
                    item1.id.compareTo(item2.id)
                }
            }
        }
        else if(userPreferences.addonSort == AddonSortType.Z_A.ordinal){
            array.sortWith { item1, item2 ->
                if (item1.translatableName["en-us"] != null && item2.translatableName["en-us"] != null) {
                    item1.translatableName["en-us"]!!.compareTo(
                        item2.translatableName["en-us"]!!,
                        true
                    )
                } else {
                    item1.id.compareTo(item2.id)
                }
            }
            array.reverse()
        }
    }

    fun reSort(){
        submitList(createListWithSections(addonsMap.values.toList()))
    }

    fun updateAddon(addon: Addon) {
        addonsMap[addon.id] = addon
        submitList(createListWithSections(addonsMap.values.toList()))
    }

    fun updateAddons(addons: List<Addon>) {
        addonsMap = addons.associateBy({ it.id }, { it }).toMutableMap()
        submitList(createListWithSections(addons))
    }

    internal object DifferCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Addon && newItem is Addon -> oldItem.id == newItem.id
                oldItem is Section && newItem is Section -> oldItem.title == newItem.title
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }

    internal fun setWithAnimation(image: ImageView, bitmap: Bitmap, duration: Int = 1500) {
        with(image) {
            val bitmapDrawable = BitmapDrawable(context.resources, bitmap)
            val animation = TransitionDrawable(arrayOf(drawable, bitmapDrawable))
            animation.isCrossFadeEnabled = true
            setImageDrawable(animation)
            animation.startTransition(duration)
        }
    }
}

private fun Addon.inRecommendedSection() = !isInstalled()
private fun Addon.inSideloadedSection() = isInstalled() && !isSupported()
private fun Addon.inInstalledSection() = isInstalled() && isSupported() && isEnabled()
private fun Addon.inDisabledSection() = isInstalled() && isSupported() && !isEnabled()