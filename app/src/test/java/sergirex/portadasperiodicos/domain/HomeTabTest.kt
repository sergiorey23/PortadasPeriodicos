package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

class HomeTabTest {

    @Test
    fun `stable keys round-trip`() {
        (listOf(HomeTab.Favorites) + HomeTab.categories).forEach {
            assertEquals(it, HomeTab.fromKey(it.key))
        }
    }

    @Test
    fun `labels stored by older versions are still understood in both languages`() {
        val deportes = HomeTab.Category(PeriodicoCategory.DEPORTES)
        assertEquals(deportes, HomeTab.fromKey("Deportes"))
        assertEquals(deportes, HomeTab.fromKey("Sports"))
        assertEquals(HomeTab.Category(PeriodicoCategory.ECONOMIA), HomeTab.fromKey("Economy"))
        assertEquals(HomeTab.Category(PeriodicoCategory.LOCALES), HomeTab.fromKey("Locales"))
        assertEquals(HomeTab.Category(PeriodicoCategory.INTERNACIONAL), HomeTab.fromKey("International"))
        assertEquals(HomeTab.Favorites, HomeTab.fromKey("Favoritos"))
        assertEquals(HomeTab.Favorites, HomeTab.fromKey("Favorites"))
    }

    @Test
    fun `unknown or missing values fall back to General`() {
        assertEquals(HomeTab.default, HomeTab.fromKey(null))
        assertEquals(HomeTab.default, HomeTab.fromKey("???"))
        assertEquals(HomeTab.Category(PeriodicoCategory.GENERAL), HomeTab.fromKey("General"))
    }
}
