package com.absinthe.anywhere_.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.anywhere_.AppBarActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.databinding.ActivitySettingsBinding
import com.absinthe.anywhere_.listener.OnDocumentResultListener
import com.absinthe.anywhere_.model.Settings
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.handler.URLSchemeHandler
import com.absinthe.anywhere_.utils.manager.DialogManager
import com.absinthe.anywhere_.utils.manager.URLManager
import rikka.material.app.DayNightDelegate
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
import timber.log.Timber

class SettingsActivity : AppBarActivity<ActivitySettingsBinding>() {

    override fun setViewBinding() = ActivitySettingsBinding.inflate(layoutInflater)

    override fun getToolBar() = binding.toolbar.toolBar

    override fun getAppBarLayout() = binding.toolbar.appBar

    class SettingsFragment : PreferenceFragmentCompat() {

        companion object {
            init {
                SimpleMenuPreference.setLightFixEnabled(true)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            //Normal
            findPreference<ListPreference>(Const.PREF_WORKING_MODE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.workingMode = newValue as String
                    true
                }
            }
            findPreference<SwitchPreference>(Const.PREF_CLOSE_AFTER_LAUNCH)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.closeAfterLaunch = newValue as Boolean
                    true
                }
            }

            //View
            findPreference<Preference>(Const.PREF_CHANGE_BACKGROUND)?.apply {
                setOnPreferenceClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "image/*"
                        }
                        requireActivity().startActivityForResult(intent, Const.REQUEST_CODE_IMAGE_CAPTURE)
                        (requireActivity() as SettingsActivity).setDocumentResultListener(object : OnDocumentResultListener {
                            override fun onResult(uri: Uri) {
                                GlobalValues.backgroundUri = uri.toString()
                                GlobalValues.clearActionBarType()
                                AppUtils.restart()
                            }
                        })
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                        ToastUtil.makeText(R.string.toast_no_document_app)
                    }
                    true
                }
            }
            findPreference<Preference>(Const.PREF_RESET_BACKGROUND)?.apply {
                setOnPreferenceClickListener {
                    DialogManager.showResetBackgroundDialog(requireActivity())
                    true
                }
            }
            findPreference<ListPreference>(Const.PREF_DARK_MODE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue.toString() == Const.DARK_MODE_AUTO) {
                        DialogManager.showDarkModeTimePickerDialog(requireActivity() as SettingsActivity)
                    } else {
                        GlobalValues.darkMode = newValue.toString()
                        DayNightDelegate.setDefaultNightMode(Settings.getTheme())
                        requireActivity().recreate()
                    }
                    true
                }
            }
            findPreference<ListPreference>(Const.PREF_CARD_MODE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.cardMode = newValue.toString()
                    GlobalValues.cardModeLiveData.value = newValue
                    true
                }
            }
            findPreference<ListPreference>(Const.PREF_CARD_BACKGROUND)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.sCardBackgroundMode = newValue.toString()
                    GlobalValues.cardModeLiveData.value = newValue
                    true
                }
            }
            findPreference<SwitchPreference>(Const.PREF_MD2_TOOLBAR)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.isMd2Toolbar = newValue as Boolean
                    AppUtils.restart()
                    true
                }
            }
            findPreference<Preference>(Const.PREF_ICON_PACK)?.apply {
                setOnPreferenceClickListener {
                    DialogManager.showIconPackChoosingDialog(requireActivity() as SettingsActivity)
                    true
                }
            }

            //Advanced
            findPreference<SwitchPreference>(Const.PREF_PAGES)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.isPages = newValue as Boolean
                    AppUtils.restart()
                    true
                }
            }
            findPreference<Preference>(Const.PREF_CLEAR_SHORTCUTS)?.apply {
                if (!AppUtils.atLeastNMR1()) {
                    isVisible = false
                } else {
                    setOnPreferenceClickListener {
                        DialogManager.showClearShortcutsDialog(requireActivity())
                        true
                    }
                }
            }
            findPreference<Preference>(Const.PREF_TILES)?.apply {
                if (!AppUtils.atLeastN()) {
                    isVisible = false
                }
            }
            findPreference<SwitchPreference>(Const.PREF_COLLECTOR_PLUS)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.isCollectorPlus = newValue as Boolean
                    if (newValue) {
                        DialogManager.showIntervalSetupDialog(requireActivity() as SettingsActivity)
                    }
                    true
                }
            }
            findPreference<SwitchPreference>(Const.PREF_EXCLUDE_FROM_RECENT)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.isExcludeFromRecent = newValue as Boolean
                    true
                }
            }
            findPreference<ListPreference>(Const.PREF_SHOW_SHELL_RESULT_MODE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    GlobalValues.showShellResultMode = newValue as String
                    true
                }
            }

            //Others
            findPreference<Preference>(Const.PREF_HELP)?.apply {
                setOnPreferenceClickListener {
                    try {
                        CustomTabsIntent.Builder().build().apply {
                            launchUrl(requireActivity(), URLManager.DOCUMENT_PAGE.toUri())
                        }
                    } catch (e: ActivityNotFoundException) {
                        Timber.e(e)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = URLManager.DOCUMENT_PAGE.toUri()
                            }
                            requireActivity().startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            ToastUtil.makeText(R.string.toast_no_react_url)
                        }
                    }
                    true
                }
            }
            findPreference<Preference>(Const.PREF_BETA)?.apply {
                setOnPreferenceClickListener {
                    try {
                        CustomTabsIntent.Builder().build().apply {
                            launchUrl(requireActivity(), URLManager.BETA_DISTRIBUTE_URL.toUri())
                        }
                    } catch (e: ActivityNotFoundException) {
                        Timber.e(e)
                        try {
                            URLSchemeHandler.parse(requireActivity(), URLManager.BETA_DISTRIBUTE_URL)
                        } catch (e: ActivityNotFoundException) {
                            ToastUtil.makeText(R.string.toast_no_react_url)
                        }
                    }
                    true
                }
            }
        }

        override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
            val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
            recyclerView.fixEdgeEffect()
            recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            recyclerView.isVerticalScrollBarEnabled = false

            val lp = recyclerView.layoutParams
            if (lp is FrameLayout.LayoutParams) {
                lp.rightMargin = recyclerView.context.resources.getDimension(rikka.material.R.dimen.rd_activity_horizontal_margin).toInt()
                lp.leftMargin = lp.rightMargin
            }

            recyclerView.borderViewDelegate.borderVisibilityChangedListener =
                BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean -> (activity as SettingsActivity?)?.appBar?.setRaised(!top) }

            return recyclerView
        }
    }

}