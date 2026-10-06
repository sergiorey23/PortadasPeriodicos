package sergirex.portadasperiodicos.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import sergirex.portadasperiodicos.notifications.NotificationScheduler
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: NotificationScheduler
) : ViewModel() {

    val initialTab: StateFlow<HomeTab> =
        settings.initialTab.stateIn(viewModelScope, SharingStarted.Eagerly, HomeTab.default)

    val dailyNotificationEnabled: StateFlow<Boolean> =
        settings.dailyNotificationEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setInitialTab(tab: HomeTab) {
        viewModelScope.launch { settings.setInitialTab(tab) }
    }

    fun setDailyNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDailyNotificationEnabled(enabled)
            scheduler.sync()
        }
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
}
