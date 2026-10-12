package sergirex.portadasperiodicos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.data.catalog.CatalogParser
import java.io.File

/** Guards the bundled catalog and its published copy: a typo would otherwise only show up at runtime. */
class CatalogAssetTest {

    private val asset = File("src/main/assets/periodicos.json")
    private val published = File("../docs/catalog/periodicos.json")

    private val catalog = CatalogParser.parse(asset.readText())

    @Test
    fun `the bundled catalog is valid and every entry survived validation`() {
        assertNotNull("the bundled catalog doesn't validate", catalog)
        assertEquals("some entries were skipped as invalid", 0, catalog!!.skipped)
    }

    @Test
    fun `every newspaper has a human-readable name`() {
        // The old title was the id (a slug); a name that still looks like one was probably forgotten.
        catalog!!.periodicos.forEach { assertTrue("${it.id} has a slug-like name '${it.name}'", '_' !in it.name) }
    }

    @Test
    fun `the published copy is identical to the bundled one`() {
        assertEquals(
            "docs/catalog/periodicos.json must be a copy of app/src/main/assets/periodicos.json",
            asset.readText(),
            published.readText()
        )
    }
}
