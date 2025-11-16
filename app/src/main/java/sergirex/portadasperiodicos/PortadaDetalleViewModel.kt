package sergirex.portadasperiodicos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PortadaDetalleViewModel : ViewModel() {
    // LiveData to hold the state of the FABs (expanded or not)
    val areFabsExpanded = MutableLiveData<Boolean>(false)

    // Holds the last selected date from the date picker to restore it
    var lastSelectedDateMillis: Long? = null
}