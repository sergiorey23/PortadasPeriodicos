package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate
import java.util.TimeZone

class EditionDateTest {

    @Test
    fun `a date picker selection keeps its calendar day in any time zone`() {
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("America/Los_Angeles", "Europe/Madrid", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val picked = EditionDate.toUtcMillis("2026/10/06")!! // what MaterialDatePicker returns: UTC midnight
                assertEquals(zone, "2026/10/06", EditionDate.fromUtcMillis(picked))
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `lookback lists the start date and the days before it, newest first`() {
        assertEquals(listOf("2026/03/01", "2026/02/28", "2026/02/27"), EditionDate.lookback("2026/03/01", 3))
    }

    @Test
    fun `display format reverses the date`() {
        assertEquals("06/10/2026", EditionDate.toDisplay("2026/10/06"))
    }

    @Test
    fun `today is not past, earlier days are`() {
        assertFalse(EditionDate.isPast(EditionDate.today()))
        assertTrue(EditionDate.isPast(EditionDate.lookback(EditionDate.today(), 2)[1]))
    }

    @Test
    fun `cover urls expose their edition date`() {
        val url = CoverUrls.thumbnail("2026/10/06", "es", "elpais")
        assertEquals("https://img.kiosko.net/2026/10/06/es/elpais.640.jpg", url)
        assertEquals("2026/10/06", CoverUrls.editionDateOf(listOf("2026", "10", "06", "es", "elpais.640.jpg")))
        assertEquals(null, CoverUrls.editionDateOf(listOf("static", "logo.png")))
    }
}
