package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.Fragment
import com.cookiejarapps.android.smartcookieweb.R
import kotlinx.android.synthetic.main.fragment_about.*
import org.mozilla.geckoview.BuildConfig


class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_about, container, false)
        activity?.title = getString(R.string.settings_about)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val aboutText = try {
            val packageInfo =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString()
            val componentsAbbreviation = getString(R.string.mozac)
            val componentsVersion =
                mozilla.components.Build.version + ", " + mozilla.components.Build.gitHash
            val maybeGecko = getString(R.string.geckoview)
            val geckoVersion =
                BuildConfig.MOZ_APP_VERSION + "-" + BuildConfig.MOZ_APP_BUILDID

            String.format(
                "%s (Version #%s)\n%s: %s\n%s: %s",
                packageInfo.versionName,
                versionCode,
                componentsAbbreviation,
                componentsVersion,
                maybeGecko,
                geckoVersion
            )
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }

        val content = getString(R.string.settings_about)

        about_text.text = aboutText
        about_content.text = content

        val pair: Array<Pair<String, String>> = arrayOf(
            Pair(resources.getString(R.string.app_name), resources.getString(R.string.license_gpl)),
            Pair(resources.getString(R.string.mozac), resources.getString(R.string.mpl_license)),
            Pair(resources.getString(R.string.geckoview), resources.getString(R.string.mpl_license))
        )

        val adapter: ArrayAdapter<Pair<String, String>> = object : ArrayAdapter<Pair<String, String>>(
            requireContext(),
            R.layout.license_list_item,
            R.id.title,
            pair
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text1 = view.findViewById<View>(R.id.title) as TextView
                val text2 = view.findViewById<View>(R.id.content) as TextView
                text1.text = pair.get(position).first
                text2.text = pair.get(position).second
                return view
            }
        }

        about_list.adapter = adapter
    }

}