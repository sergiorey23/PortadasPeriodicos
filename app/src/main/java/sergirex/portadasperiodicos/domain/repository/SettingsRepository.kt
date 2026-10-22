package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.HomeTab

/** User-facing settings other than the theme (which has its own [ThemeRepository]). */
interface SettingsRepository {
    val initialTab: Flow<HomeTab>
    val dailyNotificationEnabled: Flow<Boolean>

    /** Whether the system notification-permission prompt was already shown, so it isn't repeated every launch. */
    val notificationPermissionRequested: Flow<Boolean>

    /** Minutes after midnight the reminder fires at (default 09:00). */
    val dailyNotificationMinutes: Flow<Int>

    /** Days of the week the reminder is allowed on, as [java.util.Calendar] day constants (default: all). */
    val dailyNotificationDays: Flow<Set<Int>>

    /** Columns of the cover grid: 0 follows the screen size, otherwise 1 to 3. */
    val gridColumns: Flow<Int>

    /** True for a pure-black dark theme (AMOLED screens). */
    val pureBlack: Flow<Boolean>

    suspend fun setInitialTab(tab: HomeTab)
    suspend fun setDailyNotificationMinutes(minutes: Int)
    suspend fun setDailyNotificationDays(days: Set<Int>)
    suspend fun setGridColumns(columns: Int)
    suspend fun setPureBlack(enabled: Boolean)
    suspend fun setDailyNotificationEnabled(enabled: Boolean)
    suspend fun setNotificationPermissionRequested()
}
