package sergirex.portadasperiodicos.data.repository

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sergirex.portadasperiodicos.data.catalog.CatalogCache
import sergirex.portadasperiodicos.data.catalog.RemoteCatalogSource
import sergirex.portadasperiodicos.data.catalog.catalogJson
import sergirex.portadasperiodicos.domain.model.CatalogRefreshResult
import java.io.File
import java.io.IOException
import java.nio.file.Files

class PeriodicosRepositoryImplTest {

    private lateinit var dir: File
    private var now = 1_000_000_000L
    private val problems = mutableListOf<String>()

    /** What the fake "server" answers next: a status code with a body, or an IOException when null. */
    private var answer: Pair<Int, String>? = 304 to ""

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("catalog-test").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun repository(bundledVersion: Int = 1): PeriodicosRepositoryImpl {
        val client = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val reply = answer ?: throw IOException("offline")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(reply.first)
                .message("test")
                .header("ETag", "\"v${reply.first}\"")
                .body(reply.second.toResponseBody("application/json".toMediaType()))
                .build()
        }).build()
        return PeriodicosRepositoryImpl(
            bundled = { catalogJson(version = bundledVersion, count = 12, prefix = "b") },
            cache = CatalogCache(dir),
            remote = RemoteCatalogSource(client, "https://example.test/periodicos.json")
        ).also {
            it.clock = { now }
            it.onProblem = { message -> problems += message }
        }
    }

    @Test
    fun `status describes the bundled catalog before any check`() = runBlocking {
        val status = repository(bundledVersion = 3).status()
        assertEquals(3, status.version)
        assertEquals(12, status.entries)
        assertEquals(0L, status.lastCheckedAt)
    }

    @Test
    fun `a newer download is adopted and recorded in the status`() = runBlocking {
        val repository = repository()
        answer = 200 to catalogJson(version = 5, count = 15, prefix = "r")

        assertEquals(CatalogRefreshResult.UPDATED, repository.refreshNow())

        val status = repository.status()
        assertEquals(5, status.version)
        assertEquals(15, status.entries)
        assertEquals(now, status.lastCheckedAt)
        assertEquals("r0", repository.getAll().first().id)
    }

    @Test
    fun `an unchanged catalog reports up to date`() = runBlocking {
        val repository = repository()
        answer = 304 to ""
        assertEquals(CatalogRefreshResult.UP_TO_DATE, repository.refreshNow())
        assertEquals(now, repository.status().lastCheckedAt)
    }

    @Test
    fun `an older download is never adopted`() = runBlocking {
        val repository = repository(bundledVersion = 9)
        answer = 200 to catalogJson(version = 2, count = 20, prefix = "old")

        assertEquals(CatalogRefreshResult.UP_TO_DATE, repository.refreshNow())
        assertEquals(9, repository.status().version)
    }

    @Test
    fun `an invalid download is rejected and the catalog is kept`() = runBlocking {
        val repository = repository()
        answer = 200 to catalogJson(version = 7, count = 3) // too few entries

        assertEquals(CatalogRefreshResult.UP_TO_DATE, repository.refreshNow())
        assertEquals(1, repository.status().version)
        assertTrue(problems.any { "rejected" in it })
    }

    @Test
    fun `being offline reports failure and is not recorded as a check`() = runBlocking {
        val repository = repository()
        answer = null

        assertEquals(CatalogRefreshResult.FAILED, repository.refreshNow())
        assertEquals(0L, repository.status().lastCheckedAt)
    }

    @Test
    fun `the normal refresh is rate limited but the manual one is not`() = runBlocking {
        val repository = repository()
        answer = 304 to ""
        assertFalse(repository.refresh(force = false)) // first check: goes to the server
        answer = 200 to catalogJson(version = 4, count = 12, prefix = "n")

        now += 60_000 // a minute later
        assertFalse(repository.refresh(force = false)) // skipped: checked recently
        assertEquals(1, repository.status().version)

        assertEquals(CatalogRefreshResult.UPDATED, repository.refreshNow())
        assertEquals(4, repository.status().version)
    }
}
