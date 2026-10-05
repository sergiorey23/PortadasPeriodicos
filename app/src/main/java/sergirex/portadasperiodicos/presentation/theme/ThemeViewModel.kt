package sergirex.portadasperiodicos.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.usecase.GetThemeModeUseCase
import sergirex.portadasperiodicos.domain.usecase.SetThemeModeUseCase
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

    val themeMode: StateFlow<ThemeMode> = getThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(mode)
        }
    }
}
