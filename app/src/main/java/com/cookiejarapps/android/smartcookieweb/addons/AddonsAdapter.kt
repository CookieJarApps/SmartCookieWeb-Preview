package com.cookiejarapps.android.smartcookieweb.addons

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.AddonSortType
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.amo.AMOAddonsProvider
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import mozilla.components.feature.addons.ui.CustomViewHolder
import mozilla.components.feature.addons.ui.CustomViewHolder.AddonViewHolder
import mozilla.components.feature.addons.ui.CustomViewHolder.SectionViewHolder
import mozilla.components.feature.addons.ui.setIcon
import mozilla.components.feature.addons.ui.translateName
import mozilla.components.feature.addons.ui.translateSummary
import mozilla.components.support.ktx.android.content.appName
import mozilla.components.support.ktx.android.content.appVersionName
import java.util.*
import kotlin.math.roundToInt


private const val VIEW_HOLDER_TYPE_SECTION = 0
private const val VIEW_HOLDER_TYPE_ADDON = 1

/**
 * An adapter for displaying add-on items. This will display information related to the state of
 * an add-on such as recommended, unsupported or installed. In addition, it will perform actions
 * such as installing an add-on.
 *
 * @property addonCollectionProvider Provider of AMO collection API.
 * @property addonsManagerDelegate Delegate that will provides method for handling the add-on items.
 * @param addons The list of add-on based on the AMO store.
 * @property style Indicates how items should look like.
 * @property excludedAddonIDs The list of add-on IDs to be excluded from the recommended section.
 */
@Suppress("LargeClass", "DEPRECATION")
class AddonsAdapter(
    private val addonCollectionProvider: AMOAddonsProvider,
    private val addonsManagerDelegate: AddonsManagerAdapterDelegate,
    private val addons: List<Addon>,
    private val style: Style? = null,
    private val excludedAddonIDs: List<String> = emptyList(),
    private val context: Context
) : ListAdapter<Any, CustomViewHolder>(DifferCallback) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @VisibleForTesting
    internal var addonsMap: MutableMap<String, Addon> = addons.associateBy({ it.id }, { it }).toMutableMap()

    internal val userPreferences: UserPreferences = UserPreferences(context)

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
        val contentWrapperView = view.findViewById<View>(R.id.add_on_content_wrapper)
        val iconView = view.findViewById<ImageView>(R.id.add_on_icon)
        val titleView = view.findViewById<TextView>(R.id.add_on_name)
        val summaryView = view.findViewById<TextView>(R.id.add_on_description)
        val ratingView = view.findViewById<RatingBar>(R.id.rating)
        val ratingAccessibleView = view.findViewById<TextView>(R.id.rating_accessibility)
        val reviewCountView = view.findViewById<TextView>(R.id.review_count)
        val addButton = view.findViewById<ImageView>(R.id.add_button)
        val allowedInPrivateBrowsingLabel = view.findViewById<ImageView>(R.id.allowed_in_private_browsing_label)
        val messageBarWarningView = view.findViewById<View>(R.id.add_on_messagebar_warning)
        val messageBarErrorView = view.findViewById<View>(R.id.add_on_messagebar_error)
        return AddonViewHolder(
            view,
            contentWrapperView,
            iconView,
            titleView,
            summaryView,
            ratingView,
            ratingAccessibleView,
            reviewCountView,
            addButton,
            allowedInPrivateBrowsingLabel,
            messageBarWarningView,
            messageBarErrorView
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
            else -> {}
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

    internal fun bindAddon(
        holder: AddonViewHolder,
        addon: Addon,
        appName: String = holder.itemView.context.appName,
        appVersion: String = holder.itemView.context.appVersionName) {
        val context = holder.itemView.context
        addon.rating?.let {
            val reviewCount = context.getString(R.string.mozac_feature_addons_user_rating_count_2)
            val ratingContentDescription =
                String.format(
                    context.getString(R.string.mozac_feature_addons_rating_content_description_2),
                    it.average,
                )
            holder.ratingView.contentDescription = ratingContentDescription

            holder.ratingAccessibleView.text = ratingContentDescription
            holder.ratingView.rating = it.average
            holder.reviewCountView.text = String.format(reviewCount, getFormattedAmount(it.reviews))
        }

        val addonName = if (addon.translatableName.isNotEmpty()) {
            addon.translateName(context)
        } else {
            addon.id
        }

        holder.titleView.text = addonName

        if (addon.translatableSummary.isNotEmpty()) {
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
        style?.maybeSetPrivateBrowsingLabelDrawable(holder.allowedInPrivateBrowsingLabel)

        holder.iconView.setIcon(addon)

        style?.maybeSetAddonNameTextColor(holder.titleView)
        style?.maybeSetAddonSummaryTextColor(holder.summaryView)

        bindMessageBars(
            context,
            holder.messageBarWarningView,
            holder.messageBarErrorView,
            onLearnMoreLinkClicked = { link -> addonsManagerDelegate.onLearnMoreLinkClicked(link, addon) },
            addon,
            addonName,
            appName,
            appVersion,
        )
    }

    internal fun createListWithSections(addons: List<Addon>): List<Any> {
        val addonList = ArrayList<Any>()
        val recommendedAddons = ArrayList<Addon>()
        val installedAddons = ArrayList<Addon>()
        val disabledAddons = ArrayList<Addon>()

        addons.forEach { addon ->
            when {
                addon.inRecommendedSection() -> recommendedAddons.add(addon)
                addon.inInstalledSection() -> installedAddons.add(addon)
                addon.inDisabledSection() -> disabledAddons.add(addon)
            }
        }

        sort(recommendedAddons, userPreferences)
        sort(installedAddons, userPreferences)
        sort(disabledAddons, userPreferences)

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

        internal fun maybeSetPrivateBrowsingLabelDrawable(imageView: ImageView) {
            addonAllowPrivateBrowsingLabelDrawableRes?.let {
                imageView.setImageDrawable(AppCompatResources.getDrawable(imageView.context, it))
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

    private fun sort(array: ArrayList<Addon>, userPreferences: UserPreferences) {
        when(userPreferences.addonSort){
            AddonSortType.RATING.ordinal -> {
                array.sortWith { item1, item2 ->
                    if (item1.rating != null && item2.rating != null) {
                        if (item1.rating!!.average == 0F && item2.rating!!.average == 0F) {
                            item1.translateName(context).compareTo(
                                item2.translateName(context),
                                true
                            )
                        } else if ((item1.rating!!.average * 2).roundToInt() / 2.0 == (item2.rating!!.average * 2).roundToInt() / 2.0) {
                            -item1.rating!!.reviews.compareTo(item2.rating!!.reviews)
                        } else {
                            -item1.rating!!.average.compareTo(item2.rating!!.average)
                        }
                    } else {
                        item1.translateName(context).compareTo(
                            item2.translateName(context),
                            true
                        )
                    }
                }
            }
            AddonSortType.A_Z.ordinal-> {
                sortByAZ(array)
            }
            AddonSortType.Z_A.ordinal -> {
                sortByAZ(array)
                array.reverse()
            }
        }
    }

    fun sortByAZ(array: ArrayList<Addon>){
        array.sortWith { item1, item2 ->
            item1.translateName(context).compareTo(
                item2.translateName(context),
                true
            )
        }
    }

    fun sortAddonList(){
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

    companion object {
        /**
         * Bind an add-on to the message bars layout.
         *
         * @param context UI context
         * @param messageBarWarningView The view of the "warning" message bar.
         * @param messageBarErrorView The view of the "error" messagebar.
         * @param onLearnMoreLinkClicked A callback function that reacts to a click on a learn more link. The link is
         * passed to this function. In most cases, we'll want to open the link in a tab.
         * @param addon The [Addon] to bind.
         * @param addonName The add-on name.
         * @param appName The application name.
         * @param appVersion The application version.
         */
        @Suppress("LongParameterList")
        fun bindMessageBars(
            context: Context,
            messageBarWarningView: View,
            messageBarErrorView: View,
            onLearnMoreLinkClicked: (AddonsManagerAdapterDelegate.LearnMoreLinks) -> Unit,
            addon: Addon,
            addonName: String,
            appName: String,
            appVersion: String,
        ) {
            // Make the message-bars invisible by default.
            messageBarWarningView.isVisible = false
            messageBarErrorView.isVisible = false

            val messageBarErrorTextView = messageBarErrorView.findViewById<TextView>(
                R.id.add_on_messagebar_error_text,
            )
            val messageBarErrorLearnMoreLink = messageBarErrorView.findViewById<TextView>(
                R.id.add_on_messagebar_error_learn_more_link,
            )
            // Ensure the link is visible when this view holder gets recycled.
            messageBarErrorLearnMoreLink.isVisible = true
            // This learn more link should be underlined.
            messageBarErrorLearnMoreLink.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            messageBarErrorLearnMoreLink.text = context.getString(R.string.mozac_feature_addons_status_learn_more)

            if (addon.isDisabledAsBlocklisted()) {
                messageBarErrorTextView.text = context.getString(R.string.mozac_feature_addons_status_blocklisted_1)
                // We need to adjust the link text because the BLOCKLISTED_ADDON link isn't a SUMO page.
                messageBarErrorLearnMoreLink.text = context.getString(R.string.mozac_feature_addons_status_see_details)
                messageBarErrorLearnMoreLink.setOnClickListener {
                    onLearnMoreLinkClicked(AddonsManagerAdapterDelegate.LearnMoreLinks.BLOCKLISTED_ADDON)
                }
                messageBarErrorView.isVisible = true
            } else if (addon.isDisabledAsNotCorrectlySigned()) {
                messageBarErrorTextView.text =
                    context.getString(R.string.mozac_feature_addons_status_unsigned, addonName)
                messageBarErrorLearnMoreLink.setOnClickListener {
                    onLearnMoreLinkClicked(AddonsManagerAdapterDelegate.LearnMoreLinks.ADDON_NOT_CORRECTLY_SIGNED)
                }
                messageBarErrorView.isVisible = true
            } else if (addon.isDisabledAsIncompatible()) {
                messageBarErrorTextView.text = context.getString(
                    R.string.mozac_feature_addons_status_incompatible,
                    addonName,
                    appName,
                    appVersion,
                )
                // There is no link when the add-on is disabled because it isn't compatible with the application
                // version.
                messageBarErrorLearnMoreLink.isVisible = false
                messageBarErrorView.isVisible = true
            } else if (addon.isSoftBlocked()) {
                messageBarWarningView.findViewById<TextView>(R.id.add_on_messagebar_warning_text).text =
                    context.getString(
                        // Soft-blocked add-ons can be re-enabled. That's why we check whether the add-on is enabled
                        // first.
                        if (addon.isEnabled()) {
                            R.string.mozac_feature_addons_status_softblocked_re_enabled
                        } else {
                            R.string.mozac_feature_addons_status_softblocked_1
                        },
                    )
                // This learn more link should be underlined.
                messageBarWarningView.findViewById<TextView>(
                    R.id.add_on_messagebar_warning_learn_more_link,
                ).paintFlags = Paint.UNDERLINE_TEXT_FLAG
                messageBarWarningView.findViewById<TextView>(R.id.add_on_messagebar_warning_learn_more_link)
                    .setOnClickListener {
                        onLearnMoreLinkClicked(AddonsManagerAdapterDelegate.LearnMoreLinks.BLOCKLISTED_ADDON)
                    }
                messageBarWarningView.isVisible = true
            }
        }
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