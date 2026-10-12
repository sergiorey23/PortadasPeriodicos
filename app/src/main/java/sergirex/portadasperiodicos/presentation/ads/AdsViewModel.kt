package sergirex.portadasperiodicos.presentation.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import sergirex.portadasperiodicos.domain.repository.AdsEntitlementRepository
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    entitlement: AdsEntitlementRepository
) : ViewModel() {

    /** null until the stored value has been read, so a buyer never sees a banner flash while it loads. */
    val adsRemoved: StateFlow<Boolean?> =
        entitlement.adsRemoved.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
