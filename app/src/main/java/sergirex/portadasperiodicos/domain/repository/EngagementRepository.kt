package sergirex.portadasperiodicos.domain.repository

import sergirex.portadasperiodicos.domain.model.Engagement

interface EngagementRepository {
    /** Counts one more cover opened and returns the updated totals. */
    suspend fun recordCoverOpened(): Engagement

    suspend fun markReviewPrompted()
}
