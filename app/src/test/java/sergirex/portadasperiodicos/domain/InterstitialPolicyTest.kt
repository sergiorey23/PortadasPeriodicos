package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.InterstitialPolicy

class InterstitialPolicyTest {
    private fun InterstitialPolicy.open(times: Int) = repeat(times) { recordOpen() }

    @Test
    fun `waits for enough covers before the first interstitial`() {
        val policy = InterstitialPolicy()
        policy.open(InterstitialPolicy.EVERY_N_OPENS - 1)
        assertFalse(policy.shouldShow(now = 0))
        policy.recordOpen()
        assertTrue(policy.shouldShow(now = 0))
    }

    @Test
    fun `counts again after one is shown`() {
        val policy = InterstitialPolicy()
        policy.open(InterstitialPolicy.EVERY_N_OPENS)
        policy.recordShown(now = 1_000)
        policy.open(InterstitialPolicy.EVERY_N_OPENS - 1)
        assertFalse(policy.shouldShow(now = 1_000 + InterstitialPolicy.MIN_INTERVAL_MS))
    }

    @Test
    fun `keeps a minimum time between interstitials`() {
        val policy = InterstitialPolicy()
        policy.open(InterstitialPolicy.EVERY_N_OPENS)
        policy.recordShown(now = 1_000)
        policy.open(InterstitialPolicy.EVERY_N_OPENS)
        assertFalse(policy.shouldShow(now = 1_000 + InterstitialPolicy.MIN_INTERVAL_MS - 1))
        assertTrue(policy.shouldShow(now = 1_000 + InterstitialPolicy.MIN_INTERVAL_MS))
    }
}
