package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover

/** Finds which edition (date) of a newspaper's cover is actually published. */
interface PortadaCoverRepository {

    /**
     * Emits one [PortadaCover] per newspaper as it's resolved (a [Flow], not a List, because
     * resolving can take several round trips and covers should pop into the grid as found).
     */
    fun getCovers(
        periodicos: List<PeriodicoRef>,
        targetDate: String,
        forceRefresh: Boolean
    ): Flow<PortadaCover>

    /** The newest published date at or before [targetDate] (yyyy/MM/dd), or null if none in the lookback window. */
    suspend fun resolveDate(id: String, country: String, targetDate: String, forceRefresh: Boolean = false): String?
}
