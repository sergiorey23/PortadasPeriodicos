package sergirex.portadasperiodicos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import sergirex.portadasperiodicos.presentation.LoadTracker
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

    // One tracker for everything the Settings screen shows, so it can wait until the values are the stored ones.
    private val tracker = LoadTracker(expected = 7)

    val initialTab: StateFlow<HomeTab> = tracker.track(viewModelScope, settings.initialTab, HomeTab.default)

    val dailyNotificationEnabled: StateFlow<Boolean> =
        tracker.track(viewModelScope, settings.dailyNotificationEnabled, true)

    val dailyNotificationMinutes: StateFlow<Int> =
        tracker.track(viewModelScope, settings.dailyNotificationMinutes, ReminderSchedule.DEFAULT_MINUTES)

    val dailyNotificationDays: StateFlow<Set<Int>> =
        tracker.track(viewModelScope, settings.dailyNotificationDays, ReminderSchedule.ALL_DAYS)

    val gridColumns: StateFlow<Int> = tracker.track(viewModelScope, settings.gridColumns, 0)

    val pureBlack: StateFlow<Boolean> = tracker.track(viewModelScope, settings.pureBlack, false)

    /** How many favorites the user has (the ones shown, i.e. whose newspaper is still in the catalog). */
    val favoritesCount: StateFlow<Int> =
        tracker.track(viewModelScope, observeFavorites().map { it.size }, 0)

    /** Catalog and cache details, read by [prepare] so the screen can show them from its first frame. */
    var preparedCatalogStatus: CatalogStatus? = null
        private set
    var preparedImageCacheSize: Long? = null
        private set

    /** Waits until every value the Settings screen shows has been read, including the slower catalog and cache details. */
    suspend fun prepare() {
        tracker.await()
        preparedCatalogStatus = catalog.status()
        preparedImageCacheSize = imageCache.sizeBytes()
    }

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
