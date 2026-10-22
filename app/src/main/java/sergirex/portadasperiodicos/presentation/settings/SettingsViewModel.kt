package sergirex.portadasperiodicos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.data.local.ImageCacheControl
import sergirex.portadasperiodicos.domain.model.CatalogRefreshResult
import sergirex.portadasperiodicos.domain.model.CatalogStatus
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.ReminderSchedule
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import sergirex.portadasperiodicos.domain.usecase.ClearFavoritesUseCase
import sergirex.portadasperiodicos.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasperiodicos.notifications.NotificationScheduler
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: NotificationScheduler,
    private val catalog: PeriodicosRepository,
    private val imageCache: ImageCacheControl,
    observeFavorites: ObserveFavoritesUseCase,
    private val clearAllFavorites: ClearFavoritesUseCase
) : ViewModel() {

    val initialTab: StateFlow<HomeTab> =
        settings.initialTab.stateIn(viewModelScope, SharingStarted.Eagerly, HomeTab.default)

    val dailyNotificationEnabled: StateFlow<Boolean> =
        settings.dailyNotificationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val dailyNotificationMinutes: StateFlow<Int> =
        settings.dailyNotificationMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, ReminderSchedule.DEFAULT_MINUTES)

    val dailyNotificationDays: StateFlow<Set<Int>> =
        settings.dailyNotificationDays.stateIn(viewModelScope, SharingStarted.Eagerly, ReminderSchedule.ALL_DAYS)

    val gridColumns: StateFlow<Int> = settings.gridColumns.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val pureBlack: StateFlow<Boolean> = settings.pureBlack.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** How many favorites the user has (the ones shown, i.e. whose newspaper is still in the catalog). */
    val favoritesCount: StateFlow<Int> =
        observeFavorites().map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Deletes every favorite, then calls [onDone] on the main thread. */
    fun clearFavorites(onDone: () -> Unit) {
        viewModelScope.launch {
            clearAllFavorites()
            onDone()
        }
    }

    fun setInitialTab(tab: HomeTab) {
        viewModelScope.launch { settings.setInitialTab(tab) }
    }

    fun setDailyNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyNotificationEnabled(enabled)
            scheduler.sync()
        }
    }

    fun setDailyNotificationMinutes(minutes: Int) {
        viewModelScope.launch {
            settings.setDailyNotificationMinutes(minutes)
            scheduler.sync()
        }
    }

    fun setDailyNotificationDays(days: Set<Int>) {
        viewModelScope.launch {
            settings.setDailyNotificationDays(days)
            scheduler.sync()
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { settings.setGridColumns(columns) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { settings.setPureBlack(enabled) }
    }

    /** The system permission prompt is shown once automatically, and only if the reminder is on. */
    suspend fun shouldAskNotificationPermission(): Boolean =
        settings.dailyNotificationEnabled.first() && !settings.notificationPermissionRequested.first()

    fun onNotificationPermissionRequested() {
        viewModelScope.launch { settings.setNotificationPermissionRequested() }
    }

    /** Call after the permission prompt resolves: alarms are only scheduled while notifications can be shown. */
    fun onNotificationPermissionResult() {
        viewModelScope.launch { scheduler.sync() }
    }

    suspend fun catalogStatus(): CatalogStatus = catalog.status()

    suspend fun refreshCatalog(): CatalogRefreshResult = catalog.refreshNow()

    suspend fun imageCacheSize(): Long = imageCache.sizeBytes()

    suspend fun clearImageCache() = imageCache.clear()
}
