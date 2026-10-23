package sergirex.portadasperiodicos.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.usecase.GetThemeModeUseCase
import sergirex.portadasperiodicos.domain.usecase.SetThemeModeUseCase
import sergirex.portadasperiodicos.presentation.LoadTracker
import javax.inject.Inject

/**
 * Unidirectional data flow for the theme setting: the Settings screen reads
 * [themeMode] (a single StateFlow<ThemeMode>, always with a current value —
 * no "is it loaded yet" null-checking at the call site) and calls
 * [setThemeMode] in response to user input; it never touches DataStore or
 * SharedPreferences directly.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase
) : ViewModel() {

    private val tracker = LoadTracker(expected = 1)

    val themeMode: StateFlow<ThemeMode> = tracker.track(viewModelScope, getThemeMode(), ThemeMode.SYSTEM)

    /** Waits until [themeMode] holds the stored mode rather than the placeholder. */
    suspend fun awaitLoaded() = tracker.await()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }
}
