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
import sergirex.portadasperiodicos.PortadasUtils
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
    private val cacheGroup: String = categoryName ?: CACHE_GROUP_FAVORITES

    private val _uiState = MutableStateFlow(PortadasUiState())
    val uiState: StateFlow<PortadasUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** No-op if a load already ran for this ViewModel instance (e.g. after a configuration change). */
    fun loadIfNeeded() {
        if (_uiState.value.allPeriodicos.isEmpty()) load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val periodicos = categoryName
                ?.let { getPeriodicosByCategory(PeriodicoCategory.valueOf(it)).getOrDefault(emptyList()).map { p -> p.toRef() } }
                ?: getFavoritePeriodicos()

            _uiState.update { it.copy(allPeriodicos = periodicos, covers = emptyList(), isRefreshing = true) }

            getPortadaCovers(periodicos, PortadasUtils.effectiveTodayDate(), cacheGroup, forceRefresh)
                .catch { /* leave whatever resolved so far on screen; nothing more to try */ }
                .collect { cover -> _uiState.update { it.copy(covers = it.covers + cover) } }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    companion object {
        const val ARG_CATEGORY = "category"
        private const val CACHE_GROUP_FAVORITES = "Favoritos"
    }
}
