package sergirex.portadasperiodicos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.domain.model.CoverOpenOutcome
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.usecase.MarkReviewPromptedUseCase
import sergirex.portadasperiodicos.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasperiodicos.domain.usecase.RecordCoverOpenedUseCase
import sergirex.portadasperiodicos.domain.usecase.ResolveCoverDateUseCase
import sergirex.portadasperiodicos.domain.usecase.ToggleFavoriteUseCase
import javax.inject.Inject

@HiltViewModel
class PortadaDetalleViewModel @Inject constructor(
    private val resolveCoverDate: ResolveCoverDateUseCase,
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val recordCoverOpened: RecordCoverOpenedUseCase,
    private val markReviewPrompted: MarkReviewPromptedUseCase
) : ViewModel() {
    // LiveData to hold the state of the FABs (expanded or not)
    val areFabsExpanded = MutableLiveData(false)

    // Holds the last selected date from the date picker to restore it
    var lastSelectedDateMillis: Long? = null

    // The edition each newspaper actually resolved to (it can fall back to an older day), so
    // Save/Share fetch the image that is on screen. Lives here so it survives rotation.
    val resolvedDates = mutableMapOf<String, String>()

    val favoriteIds: StateFlow<Set<String>> = observeFavorites()
        .map { favorites -> favorites.mapTo(HashSet()) { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavorite(periodico: PeriodicoRef) {
        viewModelScope.launch { toggleFavoriteUseCase(periodico) }
    }

    suspend fun onCoverOpened(): CoverOpenOutcome = recordCoverOpened()

    fun onReviewPromptShown() {
        viewModelScope.launch { markReviewPrompted() }
    }

    suspend fun resolveDate(id: String, country: String, targetDate: String): String? =
        resolveCoverDate(id, country, targetDate)
}
