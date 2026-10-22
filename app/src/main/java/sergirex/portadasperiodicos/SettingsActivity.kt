package sergirex.portadasperiodicos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.CatalogRefreshResult
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.ReminderSchedule
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.repository.AdsEntitlementRepository
import sergirex.portadasperiodicos.notifications.NotificationChannels
import sergirex.portadasperiodicos.presentation.ads.AdsViewModel
import sergirex.portadasperiodicos.presentation.settings.SettingsViewModel
import sergirex.portadasperiodicos.presentation.theme.ThemeViewModel
import sergirex.portadasperiodicos.ui.PureBlackTheme
import java.text.DateFormatSymbols
import java.util.Calendar
import javax.inject.Inject

/**
 * Theme.Portadas is DayNight-aware (see values/themes.xml and the color roles in
 * values/colors.xml + values-night/colors.xml), so AppCompatDelegate's night mode — set once in
 * MyApplication from the same ThemeRepository this screen writes to — is all that's needed for
 * this (and every other) Activity to render in the right mode.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var adsEntitlement: AdsEntitlementRepository

    /** Google Play purchase handling (restore and buy), shared with the preferences fragment. */
    lateinit var billing: BillingManager
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        PureBlackTheme.apply(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        billing = BillingManager(this, lifecycleScope, adsEntitlement) { findViewById(android.R.id.content) }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, PreferencesFragment())
                .commit()
        }
    }

    override fun onDestroy() {
        billing.destroy()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    @AndroidEntryPoint
    class PreferencesFragment : PreferenceFragmentCompat() {

        // Shared with SettingsActivity's Hilt component via the Activity scope —
        // this Fragment never talks to a repository/DataStore directly.
        private val themeViewModel: ThemeViewModel by activityViewModels()
        private val settingsViewModel: SettingsViewModel by activityViewModels()
        private val adsViewModel: AdsViewModel by activityViewModels()

        private val billing get() = (requireActivity() as SettingsActivity).billing

        private val requestNotificationPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                settingsViewModel.onNotificationPermissionRequested()
                if (granted) {
                    settingsViewModel.setDailyNotificationEnabled(true)
                } else {
                    Toast.makeText(requireContext(), R.string.notifications_permission_denied, Toast.LENGTH_LONG).show()
                }
            }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Values are stored through repositories (DataStore), not Preference's own default
            // SharedPreferences file: the views are kept in sync from the ViewModels' flows instead.
            findPreference<ListPreference>(KEY_THEME)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    themeViewModel.setThemeMode((newValue as String).toThemeMode())
                    true
                }
            }
            findPreference<SwitchPreferenceCompat>(KEY_PURE_BLACK)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    settingsViewModel.setPureBlack(newValue as Boolean)
                    true
                }
            }
            findPreference<Preference>(KEY_LANGUAGE)?.apply {
                isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                setOnPreferenceClickListener { openAppLanguageSettings(); true }
            }
            findPreference<ListPreference>(KEY_INIT_CATEGORY)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    settingsViewModel.setInitialTab(HomeTab.fromKey(newValue as String))
                    true
                }
            }
            findPreference<ListPreference>(KEY_GRID_COLUMNS)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    settingsViewModel.setGridColumns((newValue as String).toInt())
                    true
                }
            }
            findPreference<SwitchPreferenceCompat>(KEY_DAILY_NOTIFICATION)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    val enable = newValue as Boolean
                    if (enable && !canPostNotifications()) {
                        // Applied in the permission callback once (and if) the user grants it.
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        false
                    } else {
                        settingsViewModel.setDailyNotificationEnabled(enable)
                        true
                    }
                }
            }
            findPreference<Preference>(KEY_NOTIFICATION_TIME)?.setOnPreferenceClickListener { showTimePicker(); true }
            findPreference<Preference>(KEY_NOTIFICATION_DAYS)?.setOnPreferenceClickListener { showDaysDialog(); true }
            findPreference<Preference>(KEY_NOTIFICATION_SOUND)?.apply {
                isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                setOnPreferenceClickListener { openChannelSettings(); true }
            }
            findPreference<Preference>(KEY_REMOVE_ADS)?.setOnPreferenceClickListener { billing.showPurchaseDialog(); true }
            findPreference<Preference>(KEY_RESTORE_PURCHASE)?.setOnPreferenceClickListener { billing.restorePurchases(); true }
            findPreference<Preference>(KEY_CATALOG_REFRESH)?.setOnPreferenceClickListener { refreshCatalog(); true }
            findPreference<Preference>(KEY_CLEAR_CACHE)?.setOnPreferenceClickListener { clearImageCache(); true }
            findPreference<Preference>(KEY_FEEDBACK)?.setOnPreferenceClickListener { sendFeedback(); true }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val themePreference = findPreference<ListPreference>(KEY_THEME)
            val pureBlackPreference = findPreference<SwitchPreferenceCompat>(KEY_PURE_BLACK)
            val tabPreference = findPreference<ListPreference>(KEY_INIT_CATEGORY)
            val gridPreference = findPreference<ListPreference>(KEY_GRID_COLUMNS)
            val notificationPreference = findPreference<SwitchPreferenceCompat>(KEY_DAILY_NOTIFICATION)
            val timePreference = findPreference<Preference>(KEY_NOTIFICATION_TIME)
            val daysPreference = findPreference<Preference>(KEY_NOTIFICATION_DAYS)
            val removeAdsPreference = findPreference<Preference>(KEY_REMOVE_ADS)

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        themeViewModel.themeMode.collect { mode ->
                            themePreference?.show(mode.toPreferenceValue())
                            // Pure black only exists in dark mode.
                            pureBlackPreference?.isEnabled = mode != ThemeMode.LIGHT
                        }
                    }
                    launch { settingsViewModel.pureBlack.collect { pureBlackPreference?.isChecked = it } }
                    launch { settingsViewModel.initialTab.collect { tab -> tabPreference?.show(tab.key) } }
                    launch { settingsViewModel.gridColumns.collect { gridPreference?.show(it.toString()) } }
                    launch {
                        settingsViewModel.dailyNotificationEnabled.collect { notificationPreference?.isChecked = it }
                    }
                    launch {
                        settingsViewModel.dailyNotificationMinutes.collect { timePreference?.summary = formatTime(it) }
                    }
                    launch {
                        settingsViewModel.dailyNotificationDays.collect { daysPreference?.summary = daysSummary(it) }
                    }
                    launch {
                        adsViewModel.adsRemoved.collect { removed ->
                            removeAdsPreference?.apply {
                                isEnabled = removed != true
                                setSummary(if (removed == true) R.string.ads_removed_summary else R.string.remove_ads_summary)
                            }
                        }
                    }
                    launch { updateCatalogStatus() }
                    launch { updateCacheSummary() }
                }
            }
        }

        private fun ListPreference.show(value: String) {
            this.value = value
            summary = entries.getOrNull(findIndexOfValue(value))
        }

        private fun canPostNotifications(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        // --- Reminder time and days ---

        private fun formatTime(minutes: Int): String {
            val time = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
            }.time
            return DateFormat.getTimeFormat(requireContext()).format(time)
        }

        private fun showTimePicker() {
            val minutes = settingsViewModel.dailyNotificationMinutes.value
            val format = if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(format)
                .setHour(minutes / 60)
                .setMinute(minutes % 60)
                .setTitleText(R.string.notification_time_picker_title)
                .build()
            picker.addOnPositiveButtonClickListener {
                settingsViewModel.setDailyNotificationMinutes(picker.hour * 60 + picker.minute)
            }
            picker.show(childFragmentManager, TAG_TIME_PICKER)
        }

        private fun daysSummary(days: Set<Int>): String = when (days) {
            ReminderSchedule.ALL_DAYS -> getString(R.string.notification_days_every_day)
            ReminderSchedule.WEEKDAYS -> getString(R.string.notification_days_weekdays)
            else -> {
                val names = DateFormatSymbols.getInstance().shortWeekdays
                ReminderSchedule.DAYS_IN_ORDER.filter { it in days }.joinToString(", ") { names[it] }
            }
        }

        private fun showDaysDialog() {
            val order = ReminderSchedule.DAYS_IN_ORDER
            val fullNames = DateFormatSymbols.getInstance().weekdays
            val names = order.map { day -> fullNames[day].replaceFirstChar { it.uppercase() } }.toTypedArray()
            val selected = settingsViewModel.dailyNotificationDays.value.toMutableSet()
            val checked = BooleanArray(order.size) { order[it] in selected }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.notification_days_dialog_title)
                .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                    if (isChecked) selected += order[which] else selected -= order[which]
                }
                .setNeutralButton(R.string.notification_days_weekdays_only) { _, _ ->
                    settingsViewModel.setDailyNotificationDays(ReminderSchedule.WEEKDAYS)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (selected.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.notification_days_pick_one, Toast.LENGTH_SHORT).show()
                    } else {
                        settingsViewModel.setDailyNotificationDays(selected)
                    }
                }
                .show()
        }

        // --- System settings shortcuts ---

        private fun openAppLanguageSettings() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
            requireContext().startActivityOrToast(intent)
        }

        private fun openChannelSettings() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val context = requireContext()
            NotificationChannels.ensureDaily(context)
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannels.DAILY)
            context.startActivityOrToast(intent)
        }

        // --- Catalog ---

        private suspend fun updateCatalogStatus() {
            val preference = findPreference<Preference>(KEY_CATALOG_STATUS) ?: return
            val status = settingsViewModel.catalogStatus()
            val checked = if (status.lastCheckedAt == 0L) {
                getString(R.string.catalog_never_checked)
            } else {
                DateUtils.getRelativeTimeSpanString(status.lastCheckedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            }
            preference.summary = getString(R.string.catalog_status_summary, status.version, status.entries, checked)
        }

        private fun refreshCatalog() {
            val refresh = findPreference<Preference>(KEY_CATALOG_REFRESH) ?: return
            refresh.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val message = when (settingsViewModel.refreshCatalog()) {
                    CatalogRefreshResult.UPDATED -> R.string.catalog_refresh_updated
                    CatalogRefreshResult.UP_TO_DATE -> R.string.catalog_refresh_up_to_date
                    CatalogRefreshResult.FAILED -> R.string.catalog_refresh_failed
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                updateCatalogStatus()
                refresh.isEnabled = true
            }
        }

        // --- Storage ---

        private suspend fun updateCacheSummary() {
            val size = settingsViewModel.imageCacheSize()
            findPreference<Preference>(KEY_CLEAR_CACHE)?.summary =
                getString(R.string.cache_clear_summary, Formatter.formatShortFileSize(requireContext(), size))
        }

        private fun clearImageCache() {
            viewLifecycleOwner.lifecycleScope.launch {
                settingsViewModel.clearImageCache()
                Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show()
                updateCacheSummary()
            }
        }

        // --- Feedback ---

        private fun sendFeedback() {
            viewLifecycleOwner.lifecycleScope.launch {
                val context = requireContext()
                val status = settingsViewModel.catalogStatus()
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                val body = getString(
                    R.string.feedback_body,
                    info.versionName ?: "?",
                    PackageInfoCompat.getLongVersionCode(info).toInt(),
                    status.version,
                    status.entries,
                    Build.VERSION.RELEASE,
                    Build.MODEL
                )
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject))
                    .putExtra(Intent.EXTRA_TEXT, body)
                context.startActivityOrToast(intent)
            }
        }

        private fun String.toThemeMode(): ThemeMode = when (this) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }

        private fun ThemeMode.toPreferenceValue(): String = when (this) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM -> "default"
        }

        private companion object {
            const val KEY_THEME = "theme"
            const val KEY_PURE_BLACK = "pure_black"
            const val KEY_LANGUAGE = "app_language"
            const val KEY_INIT_CATEGORY = "init_category"
            const val KEY_GRID_COLUMNS = "grid_columns"
            const val KEY_DAILY_NOTIFICATION = "daily_notification"
            const val KEY_NOTIFICATION_TIME = "daily_notification_time"
            const val KEY_NOTIFICATION_DAYS = "daily_notification_days"
            const val KEY_NOTIFICATION_SOUND = "notification_sound"
            const val KEY_REMOVE_ADS = "remove_ads"
            const val KEY_RESTORE_PURCHASE = "restore_purchase"
            const val KEY_CATALOG_STATUS = "catalog_status"
            const val KEY_CATALOG_REFRESH = "catalog_refresh"
            const val KEY_CLEAR_CACHE = "clear_image_cache"
            const val KEY_FEEDBACK = "send_feedback"
            const val TAG_TIME_PICKER = "reminder_time"
        }
    }
}
