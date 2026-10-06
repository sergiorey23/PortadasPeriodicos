package sergirex.portadasperiodicos.presentation.portadas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.toRef
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import sergirex.portadasperiodicos.domain.usecase.GetFavoritePeriodicosUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPeriodicosByCategoryUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPortadaCoversUseCase
import sergirex.portadasperiodicos.domain.usecase.ObserveFavoritesUseCase
import javax.inject.Inject

/**
 * Holds the state of every home tab, scoped to the home Activity rather than to each tab's Fragment.
 *
 * Fragments can then be destroyed and recreated freely by the pager (so tabs load lazily and
 * only neighbours stay alive) without losing the covers already resolved, and one date
 * selection applies to all tabs. A tab's [PortadasUiState] is loaded the first time it's shown
 * and re-loaded when the selected date or the day changed, or when its newspapers changed
 * (favorites).
 */
@HiltViewModel
class PortadasViewModel @Inject constructor(
    private val getPeriodicosByCategory: GetPeriodicosByCategoryUseCase,
    private val getFavoritePeriodicos: GetFavoritePeriodicosUseCase,
    private val getPortadaCovers: GetPortadaCoversUseCase,
    private val settings: SettingsRepository,
    observeFavorites: ObserveFavoritesUseCase
) : ViewModel() {

    private class TabLoader {
        val state = MutableStateFlow(PortadasUiState())
        var job: Job? = null

        /** The target date of the last load that produced covers. */
        var loadedFor: String? = null
    }

    private val loaders = HashMap<String, TabLoader>()

    private val _selectedDate = MutableStateFlow<String?>(null)

    /** The date picked by the user, or null for "today's edition". */
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    /** The tabs to show; the favorites tab exists only while there is at least one favorite. Empty until known. */
    val tabs: StateFlow<List<HomeTab>> = observeFavorites()
        .map { it.isNotEmpty() }
        .distinctUntilChanged()
        .map { hasFavorites -> if (hasFavorites) listOf(HomeTab.Favorites) + HomeTab.categories else HomeTab.categories }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** True once the initial-tab setting has been applied, so later tab changes don't re-select it. */
    var initialTabApplied = false

    suspend fun initialTab(): HomeTab = settings.initialTab.first()

    fun uiState(tab: HomeTab): StateFlow<PortadasUiState> = loader(tab).state.asStateFlow()

    fun selectDate(date: String?) {
        if (date == _selectedDate.value) return
        // Cancel before publishing the new date: collectors of selectedDate run immediately on the
        // main thread and start the reload, which must not be cancelled right after it began.
        loaders.values.forEach { it.job?.cancel() }
        _selectedDate.value = date
    }

    /** No-op while a load is running or when what's on screen is already current. */
    fun loadIfNeeded(tab: HomeTab) {
        val loader = loader(tab)
        if (loader.job?.isActive == true) return
        loader.job = viewModelScope.launch {
            val periodicos = periodicosFor(tab)
            val target = currentTarget()
            if (periodicos == loader.state.value.allPeriodicos && loader.loadedFor == target) return@launch
            fetch(loader, periodicos, target, forceRefresh = false)
        }
    }

    fun refresh(tab: HomeTab) {
        val loader = loader(tab)
        loader.job?.cancel()
        loader.job = viewModelScope.launch { fetch(loader, periodicosFor(tab), currentTarget(), forceRefresh = true) }
    }

    private fun loader(tab: HomeTab) = loaders.getOrPut(tab.key) { TabLoader() }

    private fun currentTarget() = _selectedDate.value ?: EditionDate.today()

    private suspend fun periodicosFor(tab: HomeTab): List<PeriodicoRef> = when (tab) {
        HomeTab.Favorites -> getFavoritePeriodicos()
        is HomeTab.Category -> getPeriodicosByCategory(tab.category).getOrDefault(emptyList()).map { it.toRef() }
    }

    private suspend fun fetch(loader: TabLoader, periodicos: List<PeriodicoRef>, target: String, forceRefresh: Boolean) {
        loader.state.update {
            it.copy(
                allPeriodicos = periodicos,
                covers = if (periodicos.isEmpty()) emptyList() else it.covers,
                isRefreshing = true,
                targetDate = target
            )
        }
        try {
            // Keep showing the previous covers until the first new one arrives, so a failed
            // reload doesn't blank the screen.
            var received = false
            getPortadaCovers(periodicos, target, forceRefresh)
                .catch { error -> Log.w(TAG, "Loading covers failed", error) } // keep whatever resolved so far on screen
                .collect { cover ->
                    loader.state.update { it.copy(covers = if (received) it.covers + cover else listOf(cover)) }
                    received = true
                }
            // Only a load that produced covers counts, so a failed attempt is retried next time.
            if (received) loader.loadedFor = target
        } finally {
            loader.state.update { it.copy(isRefreshing = false) }
        }
    }

    private companion object {
        const val TAG = "PortadasViewModel"
    }
}
