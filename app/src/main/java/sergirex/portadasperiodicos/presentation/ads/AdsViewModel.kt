package sergirex.portadasperiodicos.presentation.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import sergirex.portadasperiodicos.domain.repository.AdsEntitlementRepository
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    entitlement: AdsEntitlementRepository
) : ViewModel() {

    /**
     * Debug builds only: lets a developer whose Google account owns the purchase still see the banners.
     * Switch on with `adb shell run-as sergirex.portadasperiodicos touch files/force_ads`, off by deleting it.
     */
    private val ignorePurchase = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
        File(context.filesDir, FORCE_ADS_FILE).exists()

    /** null until the stored value has been read, so a buyer never sees a banner flash while it loads. */
    val adsRemoved: StateFlow<Boolean?> =
        entitlement.adsRemoved
            .map { removed -> removed && !ignorePurchase }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private companion object {
        const val FORCE_ADS_FILE = "force_ads"
    }
}
