package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.AdConsent
import sergirex.portadasperiodicos.domain.model.ConsentStatus

interface ConsentRepository {
    val consent: Flow<AdConsent>

    /** Records the answer with the current time; [ConsentStatus.UNKNOWN] clears it. */
    suspend fun setStatus(status: ConsentStatus)
}
