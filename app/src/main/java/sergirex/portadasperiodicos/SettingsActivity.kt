package sergirex.portadasperiodicos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.presentation.settings.SettingsViewModel
import sergirex.portadasperiodicos.presentation.theme.ThemeViewModel

/**
 * Theme.Portadas is DayNight-aware (see values/themes.xml and the color roles in
 * values/colors.xml + values-night/colors.xml), so AppCompatDelegate's night mode — set once in
 * MyApplication from the same ThemeRepository this screen writes to — is all that's needed for
 * this (and every other) Activity to render in the right mode.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, PreferencesFragment())
                .commit()
        }
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

            // All three are stored through repositories (DataStore), not Preference's own
            // default SharedPreferences file: the views below are kept in sync from the
            // ViewModels' flows instead.
            findPreference<ListPreference>(KEY_THEME)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    themeViewModel.setThemeMode((newValue as String).toThemeMode())
                    true
                }
            }
            findPreference<ListPreference>(KEY_INIT_CATEGORY)?.apply {
                isPersistent = false
                setOnPreferenceChangeListener { _, newValue ->
                    settingsViewModel.setInitialTab(HomeTab.fromKey(newValue as String))
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
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val themePreference = findPreference<ListPreference>(KEY_THEME)
            val tabPreference = findPreference<ListPreference>(KEY_INIT_CATEGORY)
            val notificationPreference = findPreference<SwitchPreferenceCompat>(KEY_DAILY_NOTIFICATION)

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        themeViewModel.themeMode.collect { mode -> themePreference?.show(mode.toPreferenceValue()) }
                    }
                    launch {
                        settingsViewModel.initialTab.collect { tab -> tabPreference?.show(tab.key) }
                    }
                    launch {
                        settingsViewModel.dailyNotificationEnabled.collect { notificationPreference?.isChecked = it }
                    }
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
            const val KEY_INIT_CATEGORY = "init_category"
            const val KEY_DAILY_NOTIFICATION = "daily_notification"
        }
    }
}
