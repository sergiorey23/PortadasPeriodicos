package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.Engagement
import sergirex.portadasperiodicos.domain.model.InterstitialPolicy
import sergirex.portadasperiodicos.domain.model.ReviewPromptPolicy

class PoliciesTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_000 * day

    private fun engaged(opens: Int = 5, installedDaysAgo: Long = 3, promptedDaysAgo: Long? = null) =
        Engagement(opens, now - installedDaysAgo * day, promptedDaysAgo?.let { now - it * day })

    @Test
    fun `review is requested once the user is engaged enough`() {
        assertTrue(ReviewPromptPolicy.shouldPrompt(engaged(), now))
    }

    @Test
    fun `never on first use`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(opens = 1, installedDaysAgo = 0), now))
    }

    @Test
    fun `not before enough covers were opened`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(opens = 4), now))
    }

    @Test
    fun `not before a few days after install`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(installedDaysAgo = 2), now))
    }

    @Test
    fun `not again soon after a previous request, but again after the cooldown`() {
        assertFalse(ReviewPromptPolicy.shouldPrompt(engaged(promptedDaysAgo = 89), now))
        assertTrue(ReviewPromptPolicy.shouldPrompt(engaged(promptedDaysAgo = 90), now))
    }

    @Test
    fun `interstitial every third open only`() {
        assertFalse(InterstitialPolicy.shouldShow(0))
        assertFalse(InterstitialPolicy.shouldShow(1))
        assertFalse(InterstitialPolicy.shouldShow(2))
        assertTrue(InterstitialPolicy.shouldShow(3))
        assertFalse(InterstitialPolicy.shouldShow(4))
        assertTrue(InterstitialPolicy.shouldShow(6))
    }
}
