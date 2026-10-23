package sergirex.portadasperiodicos.presentation.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import sergirex.portadasperiodicos.domain.model.AdConsent
import sergirex.portadasperiodicos.domain.model.AdsPolicy
import sergirex.portadasperiodicos.domain.model.ConsentStatus
import sergirex.portadasperiodicos.domain.repository.AdsEntitlementRepository
import sergirex.portadasperiodicos.domain.repository.ConsentRepository
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    entitlement: AdsEntitlementRepository,
    private val consentRepository: ConsentRepository
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

    /** The user's answer about ads and data; null until it has been read. */
    val adConsent: StateFlow<AdConsent?> =
        consentRepository.consent.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Whether a banner may be requested: only without the purchase and with consent. null until both are known,
     * so no banner (and no ad SDK) is touched while the answer is still loading.
     */
    val showBanner: StateFlow<Boolean?> = combine(adsRemoved, adConsent) { removed, consent ->
        if (removed == null || consent == null) null else AdsPolicy.shouldShowBanner(removed, consent.status)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True while the consent question has to be shown. */
    val askConsent: StateFlow<Boolean> = combine(adsRemoved, adConsent) { removed, consent ->
        removed != null && consent != null && AdsPolicy.shouldAskConsent(removed, consent.status)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setConsent(granted: Boolean) {
        viewModelScope.launch {
            consentRepository.setStatus(if (granted) ConsentStatus.GRANTED else ConsentStatus.DENIED)
        }
    }

    private companion object {
        const val FORCE_ADS_FILE = "force_ads"
    }
}
