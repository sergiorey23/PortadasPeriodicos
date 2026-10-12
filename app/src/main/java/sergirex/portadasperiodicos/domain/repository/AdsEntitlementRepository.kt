package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow

/** Whether the user bought "remove ads". Google Play is the authority; this is the app's local copy of it. */
interface AdsEntitlementRepository {
    val adsRemoved: Flow<Boolean>

    suspend fun setAdsRemoved(removed: Boolean)
}
