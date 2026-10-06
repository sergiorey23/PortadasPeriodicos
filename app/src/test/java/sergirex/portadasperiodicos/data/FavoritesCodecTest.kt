package sergirex.portadasperiodicos.data

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasperiodicos.data.local.FavoritesCodec
import sergirex.portadasperiodicos.domain.model.PeriodicoRef

class FavoritesCodecTest {

    @Test
    fun `favorites round-trip in order`() {
        val favorites = listOf(PeriodicoRef("elpais", "elpais.com", "es"), PeriodicoRef("lemonde", "lemonde.fr", "fr"))
        assertEquals(favorites, FavoritesCodec.decode(FavoritesCodec.encode(favorites)))
    }

    @Test
    fun `missing or corrupted data decodes to no favorites`() {
        assertEquals(emptyList<PeriodicoRef>(), FavoritesCodec.decode(null))
        assertEquals(emptyList<PeriodicoRef>(), FavoritesCodec.decode(""))
        assertEquals(emptyList<PeriodicoRef>(), FavoritesCodec.decode("{not json"))
    }

    @Test
    fun `legacy encoded favorites are understood`() {
        assertEquals(PeriodicoRef("elpais", "elpais.com", "es"), PeriodicoRef.fromLegacyEncoded("elpais:elpais.com:es"))
        assertEquals(PeriodicoRef("elpais", "elpais.com", "es"), PeriodicoRef.fromLegacyEncoded("elpais.com"))
    }
}
