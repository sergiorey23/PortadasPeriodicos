package sergirex.portadasperiodicos.presentation.portadas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import sergirex.portadasperiodicos.domain.model.toRef
import sergirex.portadasperiodicos.domain.usecase.GetFavoritePeriodicosUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPeriodicosByCategoryUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPortadaCoversUseCase
import javax.inject.Inject

/**
 * Shared by every category tab AND Favorites — previously six near-identical
 * Fragments (General/Deportes/Economia/Locales/Internacional/Favoritos) each
 * carried their own copy of "create a GetPortadas, call execute(), implement
 * PortadasListener, push results into an adapter" wired to a hardcoded data
 * source. The only real difference between them was *which* list of
 * newspapers to load, which [categoryName] (absent for Favorites, via
 * SavedStateHandle from the Fragment's arguments) now captures.
 */
@HiltViewModel
class PortadasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPeriodicosByCategory: GetPeriodicosByCategoryUseCase,
    private val getFavoritePeriodicos: GetFavoritePeriodicosUseCase,
    private val getPortadaCovers: GetPortadaCoversUseCase
) : ViewModel() {

    private val categoryName: String? = savedStateHandle[ARG_CATEGORY]

    private val _uiState = MutableStateFlow(PortadasUiState())
    val uiState: StateFlow<PortadasUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadedDate: String? = null

    /**
     * Loads on first use and again whenever the day changed since the last successful load.
     * The Activity is never destroyed when the user leaves (back just minimizes), so without
     * the date check a ViewModel would keep showing the covers it loaded days ago.
     */
    fun loadIfNeeded() {
        if (loadJob?.isActive == true) return
        if (loadedDate != EditionDate.today()) load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val periodicos = categoryName
                ?.let { getPeriodicosByCategory(PeriodicoCategory.valueOf(it)).getOrDefault(emptyList()).map { p -> p.toRef() } }
                ?: getFavoritePeriodicos()
            val today = EditionDate.today()

            _uiState.update {
                it.copy(allPeriodicos = periodicos, covers = if (periodicos.isEmpty()) emptyList() else it.covers, isRefreshing = true)
            }

            // Keep showing the previous covers until the first new one arrives, so a failed
            // (e.g. offline) reload doesn't blank the screen.
            var received = false
            getPortadaCovers(periodicos, today, forceRefresh)
                .catch { /* leave whatever resolved so far on screen; nothing more to try */ }
                .collect { cover ->
                    _uiState.update { it.copy(covers = if (received) it.covers + cover else listOf(cover)) }
                    received = true
                }
            // Only a load that produced covers counts, so an offline attempt is retried on the next onStart.
            if (received) loadedDate = today

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    companion object {
        const val ARG_CATEGORY = "category"
    }
}
