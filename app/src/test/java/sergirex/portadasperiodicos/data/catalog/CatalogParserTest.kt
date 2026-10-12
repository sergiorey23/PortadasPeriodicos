package sergirex.portadasperiodicos.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

class CatalogParserTest {

    @Test
    fun `a valid catalog is read with its version`() {
        val parsed = CatalogParser.parse(catalogJson(version = 7, count = 12))!!
        assertEquals(7, parsed.version)
        assertEquals(12, parsed.periodicos.size)
        assertEquals(0, parsed.skipped)
        assertEquals("Paper 3", parsed.periodicos[3].name)
    }

    @Test
    fun `bad entries are skipped without affecting the rest`() {
        val bad = listOf(
            """{"id":"","name":"No id","domain":"a.es","country":"es","category":"GENERAL"}""",
            """{"id":"noname","name":"  ","domain":"a.es","country":"es","category":"GENERAL"}""",
            """{"id":"weird","name":"Weird","domain":"a.es","country":"es","category":"SPACE"}""",
            """{"id":"p1","name":"Duplicate of p1","domain":"a.es","country":"es","category":"GENERAL"}""",
            """{"id":"incomplete"}""",
            """"just a string""""
        )
        val parsed = CatalogParser.parse(catalogJson(count = 12, extra = bad))!!
        assertEquals(12, parsed.periodicos.size)
        assertEquals(6, parsed.skipped)
        assertEquals("Paper 1", parsed.periodicos.first { it.id == "p1" }.name)
    }

    @Test
    fun `a file written for a newer app is rejected`() {
        assertNull(CatalogParser.parse(catalogJson(schema = CatalogParser.SUPPORTED_SCHEMA + 1)))
    }

    @Test
    fun `a catalog with too few usable entries is rejected so a bad deploy cannot empty the app`() {
        assertNull(CatalogParser.parse(catalogJson(count = CatalogParser.MIN_ENTRIES - 1)))
        assertNull(CatalogParser.parse(catalogJson(count = 0)))
    }

    @Test
    fun `things that are not a catalog are rejected`() {
        listOf("", "not json", "42", "null", "{}", """{"periodicos":"nope"}""", "<html>404</html>").forEach {
            assertNull("accepted: $it", CatalogParser.parse(it))
        }
    }

    @Test
    fun `the original bare array format is still understood`() {
        val bare = (0 until 12).joinToString(",", "[", "]") {
            """{"id":"b$it","name":"B $it","domain":"b$it.es","country":"es","category":"LOCALES"}"""
        }
        val parsed = CatalogParser.parse(bare)!!
        assertEquals(0, parsed.version)
        assertEquals(PeriodicoCategory.LOCALES, parsed.periodicos.first().category)
        assertNotNull(parsed)
    }

    @Test
    fun `unknown extra fields are ignored so the format can grow`() {
        val text = catalogJson().replace(""""country":"es"""", """"country":"es","newField":123""")
        assertEquals(12, CatalogParser.parse(text)!!.periodicos.size)
    }
}
