package sergirex.portadasperiodicos

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.presentation.theme.ThemeViewModel

/**
 * No longer needs to pick a theme by hand: AppTheme is DayNight-aware (see
 * values/styles.xml and values-night/styles.xml), so AppCompatDelegate's
 * night mode — set once in MyApplication from the same ThemeRepository this
 * screen writes to — is all that's needed for this (and every other)
 * Activity to render in the right mode.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, PreferencesFragment())
            .commit()
    }

    @AndroidEntryPoint
    class PreferencesFragment : PreferenceFragmentCompat() {

        // Shared with SettingsActivity's Hilt component via the Activity scope —
        // this Fragment never talks to ThemeRepository/DataStore directly.
        private val themeViewModel: ThemeViewModel by activityViewModels()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val themePreference = findPreference<ListPreference>(KEY_THEME) ?: return
            // Persisted through ThemeRepository (DataStore), not Preference's own
            // default SharedPreferences file.
            themePreference.isPersistent = false
            themePreference.setOnPreferenceChangeListener { _, newValue ->
                themeViewModel.setThemeMode((newValue as String).toThemeMode())
                true
            }

            val categoriesPreference = findPreference<ListPreference>(KEY_INIT_CATEGORY) ?: return
            categoriesPreference.summary = categoriesPreference.value
            categoriesPreference.setOnPreferenceChangeListener { preference, newValue ->
                preference.summary = newValue.toString()
                true
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val themePreference = findPreference<ListPreference>(KEY_THEME) ?: return

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    themeViewModel.themeMode.collect { mode ->
                        val value = mode.toPreferenceValue()
                        themePreference.value = value
                        themePreference.summary =
                            themePreference.entries.getOrNull(themePreference.findIndexOfValue(value))
                    }
                }
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
            const val KEY_INIT_CATEGORY = "init_category"
        }
    }
}
