package sergirex.portadasperiodicos.domain.model

/** How much the user has used the app so far. */
data class Engagement(
    val coverOpens: Int,
    /** Epoch millis of the app's installation. */
    val installedAt: Long,
    /** Epoch millis of the last in-app review request, or null if none yet. */
    val lastReviewPromptAt: Long?
)

/** What to do right after a cover has been opened. */
data class CoverOpenOutcome(val showInterstitial: Boolean, val askForReview: Boolean)

/**
 * When to ask for a Play review, following the usual guidance: only after the user has
 * clearly gotten value from the app (several covers viewed, a few days since install), never
 * on first use, and not again for a long while after a request (Play also rate-limits it).
 */
object ReviewPromptPolicy {
    const val MIN_COVER_OPENS = 5
    const val MIN_DAYS_SINCE_INSTALL = 3
    const val MIN_DAYS_BETWEEN_PROMPTS = 90
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun shouldPrompt(engagement: Engagement, now: Long): Boolean =
        engagement.coverOpens >= MIN_COVER_OPENS &&
            now - engagement.installedAt >= MIN_DAYS_SINCE_INSTALL * DAY_MILLIS &&
            engagement.lastReviewPromptAt.let { it == null || now - it >= MIN_DAYS_BETWEEN_PROMPTS * DAY_MILLIS }
}

/** Interstitials are spaced out: one every [EVERY_NTH_OPEN] cover opens, not on every one. */
object InterstitialPolicy {
    const val EVERY_NTH_OPEN = 3

    fun shouldShow(coverOpens: Int): Boolean = coverOpens > 0 && coverOpens % EVERY_NTH_OPEN == 0
}
