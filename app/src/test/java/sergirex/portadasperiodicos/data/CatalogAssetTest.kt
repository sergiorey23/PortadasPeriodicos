package sergirex.portadasperiodicos.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.data.local.PeriodicoDto
import sergirex.portadasperiodicos.data.local.toDomain
import java.io.File

/** Guards the bundled catalog: a typo here would otherwise only show up as a missing title or a crash at runtime. */
class CatalogAssetTest {

    private val catalog: List<PeriodicoDto> =
        Json.decodeFromString(File("src/main/assets/periodicos.json").readText())

    @Test
    fun `every newspaper has a human-readable name`() {
        catalog.forEach { assertTrue("${it.id} has no name", it.name.isNotBlank()) }
        // The old title was the id (a slug); a name that still looks like one was probably forgotten.
        catalog.forEach { assertTrue("${it.id} still has a slug-like name '${it.name}'", '_' !in it.name) }
    }

    @Test
    fun `ids are unique and categories are valid`() {
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        catalog.forEach { it.toDomain() } // throws on an unknown category
    }
}
