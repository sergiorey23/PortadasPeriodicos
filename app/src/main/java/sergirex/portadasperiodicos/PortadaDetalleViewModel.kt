package sergirex.portadasperiodicos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import sergirex.portadasperiodicos.domain.usecase.ResolveCoverDateUseCase
import javax.inject.Inject

@HiltViewModel
class PortadaDetalleViewModel @Inject constructor(
    private val resolveCoverDate: ResolveCoverDateUseCase
) : ViewModel() {
    // LiveData to hold the state of the FABs (expanded or not)
    val areFabsExpanded = MutableLiveData(false)

    // Holds the last selected date from the date picker to restore it
    var lastSelectedDateMillis: Long? = null

    // The edition each newspaper actually resolved to (it can fall back to an older day), so
    // Save/Share fetch the image that is on screen. Lives here so it survives rotation.
    val resolvedDates = mutableMapOf<String, String>()

    suspend fun resolveDate(id: String, country: String, targetDate: String): String? =
        resolveCoverDate(id, country, targetDate)
}
