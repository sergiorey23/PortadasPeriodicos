package sergirex.portadasperiodicos.domain.model

/**
 * The app's theme preference. LIGHT/DARK force a mode; SYSTEM follows the
 * device's own day/night setting.
 *
 * Replaces three magic strings ("default", "light", "dark") that used to be
 * read directly out of SharedPreferences and switched on in two different
 * places (MyApplication and SettingsActivity) with no shared constant.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
