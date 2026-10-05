package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.ThemeMode

/**
 * Source of truth for the user's theme preference. Exposed as a [Flow] rather
 * than a one-shot getter so every observer (the Application process applying
 * AppCompatDelegate's night mode, the Settings screen showing the current
 * choice) reacts automatically when it changes, instead of the old
 * SharedPreferences.OnSharedPreferenceChangeListener callback wiring.
 */
interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
