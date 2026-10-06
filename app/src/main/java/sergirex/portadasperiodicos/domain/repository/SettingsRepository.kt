package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.HomeTab

/** User-facing settings other than the theme (which has its own [ThemeRepository]). */
interface SettingsRepository {
    val initialTab: Flow<HomeTab>
    val dailyNotificationEnabled: Flow<Boolean>

    /** Whether the system notification-permission prompt was already shown, so it isn't repeated every launch. */
    val notificationPermissionRequested: Flow<Boolean>

    suspend fun setInitialTab(tab: HomeTab)
    suspend fun setDailyNotificationEnabled(enabled: Boolean)
    suspend fun setNotificationPermissionRequested()
}
