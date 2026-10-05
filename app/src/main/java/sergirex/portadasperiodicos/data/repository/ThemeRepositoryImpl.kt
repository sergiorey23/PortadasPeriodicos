package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.repository.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persists the theme preference with Jetpack DataStore instead of raw
 * SharedPreferences.
 *
 * The old code (MyApplication.java) read a "theme" SharedPreferences key with
 * three magic string values ("default"/"light"/"dark") and reacted to changes
 * via SharedPreferences.OnSharedPreferenceChangeListener — a manual,
 * easy-to-get-wrong callback registered once and never unregistered.
 * DataStore's `.data` is a Flow by construction: every observer (the
 * Application process applying AppCompatDelegate's night mode, the Settings
 * screen) just collects it and gets updates for free, no listener bookkeeping.
 *
 * Note: this is a different storage mechanism from the one used for favorites
 * and the widget's cached state, which stay on plain SharedPreferences —
 * that decision (made when fixing the broken build) was specifically about
 * the home-screen widget needing synchronous cross-process reads from
 * onReceive()/onUpdate(), a constraint that doesn't apply to the theme
 * preference, which is only ever read by Activities.
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { prefs -> prefs[THEME_KEY]?.toThemeModeOrNull() ?: migrateFromLegacyPreferenceOrDefault() }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[THEME_KEY] = mode.name }
    }

    /**
     * One-time migration for people upgrading from the old SharedPreferences-based
     * theme setting, so their existing choice survives this refactor instead of
     * silently resetting to System. Persisting the migrated value here means this
     * only runs once per install: the next read finds THEME_KEY already set.
     */
    private suspend fun migrateFromLegacyPreferenceOrDefault(): ThemeMode {
        val legacyValue = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(LEGACY_PREFERENCE_KEY, null)
        val migrated = when (legacyValue) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        setThemeMode(migrated)
        return migrated
    }

    private fun String.toThemeModeOrNull(): ThemeMode? = runCatching { ThemeMode.valueOf(this) }.getOrNull()

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        const val LEGACY_PREFERENCE_KEY = "theme"
    }
}
