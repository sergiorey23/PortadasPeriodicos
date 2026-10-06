package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.model.CoverOpenOutcome
import sergirex.portadasperiodicos.domain.model.InterstitialPolicy
import sergirex.portadasperiodicos.domain.model.ReviewPromptPolicy
import sergirex.portadasperiodicos.domain.repository.EngagementRepository
import javax.inject.Inject

class RecordCoverOpenedUseCase @Inject constructor(
    private val repository: EngagementRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): CoverOpenOutcome {
        val engagement = repository.recordCoverOpened()
        return CoverOpenOutcome(
            showInterstitial = InterstitialPolicy.shouldShow(engagement.coverOpens),
            askForReview = ReviewPromptPolicy.shouldPrompt(engagement, now)
        )
    }
}
