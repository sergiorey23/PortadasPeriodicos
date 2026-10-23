package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.AdsPolicy
import sergirex.portadasperiodicos.domain.model.ConsentStatus

class AdsPolicyTest {

    @Test
    fun `a banner needs consent`() {
        assertTrue(AdsPolicy.shouldShowBanner(adsRemoved = false, consent = ConsentStatus.GRANTED))
        assertFalse(AdsPolicy.shouldShowBanner(adsRemoved = false, consent = ConsentStatus.DENIED))
        assertFalse(AdsPolicy.shouldShowBanner(adsRemoved = false, consent = ConsentStatus.UNKNOWN))
    }

    @Test
    fun `nobody who bought remove ads sees a banner`() {
        ConsentStatus.entries.forEach { assertFalse(AdsPolicy.shouldShowBanner(adsRemoved = true, consent = it)) }
    }

    @Test
    fun `consent is only asked while undecided`() {
        assertTrue(AdsPolicy.shouldAskConsent(adsRemoved = false, consent = ConsentStatus.UNKNOWN))
        assertFalse(AdsPolicy.shouldAskConsent(adsRemoved = false, consent = ConsentStatus.GRANTED))
        assertFalse(AdsPolicy.shouldAskConsent(adsRemoved = false, consent = ConsentStatus.DENIED))
    }

    @Test
    fun `buyers are never asked`() {
        ConsentStatus.entries.forEach { assertFalse(AdsPolicy.shouldAskConsent(adsRemoved = true, consent = it)) }
    }
}
