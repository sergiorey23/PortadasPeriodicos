package sergirex.portadasperiodicos.domain.model

/** What the user answered when asked whether Meta may use data from the device to measure and show ads. */
enum class ConsentStatus { UNKNOWN, GRANTED, DENIED }

/** The answer and when it was given (epoch millis, 0 while it is [ConsentStatus.UNKNOWN]), kept as proof of consent. */
data class AdConsent(val status: ConsentStatus, val decidedAt: Long) {
    companion object {
        val UNKNOWN = AdConsent(ConsentStatus.UNKNOWN, 0L)
    }
}

/**
 * When the ad SDK may be used. Meta's publisher terms require the user's consent before the Audience Network
 * tools run, so nothing is initialised or requested until it is [ConsentStatus.GRANTED]; and people who bought
 * "remove ads" never see an ad, so they are never asked.
 */
object AdsPolicy {
    fun shouldShowBanner(adsRemoved: Boolean, consent: ConsentStatus): Boolean =
        !adsRemoved && consent == ConsentStatus.GRANTED

    fun shouldAskConsent(adsRemoved: Boolean, consent: ConsentStatus): Boolean =
        !adsRemoved && consent == ConsentStatus.UNKNOWN
}
