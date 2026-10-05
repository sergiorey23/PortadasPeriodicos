package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover

/**
 * Resolves covers for a list of newspapers. Modeled as a [Flow] that emits one
 * [PortadaCover] per newspaper as it's resolved — not a single
 * `List<PortadaCover>` — because resolving each one can mean several network
 * round trips (walking backwards through dates), and the old UI behavior of
 * "covers pop into the grid as they're found" is worth keeping; a
 * suspend-returning-a-List would force the whole grid to wait for the
 * slowest newspaper.
 */
interface PortadaCoverRepository {
    fun getCovers(
        periodicos: List<PeriodicoRef>,
        targetDate: String,
        cacheGroup: String,
        forceRefresh: Boolean
    ): Flow<PortadaCover>
}
