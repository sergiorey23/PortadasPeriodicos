package sergirex.portadasperiodicos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PortadaCoverRepositoryImplTest {

    private val requested = mutableListOf<String>()

    /** A fake server: a cover exists only for the dates in [published]; every request is recorded. */
    private fun repository(published: Set<String>): PortadaCoverRepositoryImpl {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requested += request.url.encodedPath
            val date = request.url.pathSegments.take(3).joinToString("/")
            Response.Builder()
                .request(request).protocol(Protocol.HTTP_1_1).message("fake")
                .code(if (date in published) 200 else 404)
                .body("".toResponseBody())
                .build()
        }.build()
        return PortadaCoverRepositoryImpl(client)
    }

    private val today = EditionDate.today()
    private val yesterday = EditionDate.lookback(today, 2)[1]
    private val tomorrow = EditionDate.format(java.util.Date(System.currentTimeMillis() + 24 * 3600 * 1000L))

    @Test
    fun `resolves to the newest published edition at or before the target`() = runBlocking {
        val repo = repository(published = setOf(yesterday))
        assertEquals(yesterday, repo.resolveDate("elpais", "es", today))
    }

    @Test
    fun `a published edition for today is reused without probing again`() = runBlocking {
        val repo = repository(published = setOf(today))
        repo.resolveDate("elpais", "es", today)
        requested.clear()

        assertEquals(today, repo.resolveDate("elpais", "es", today))
        assertEquals(0, requested.size)
    }

    @Test
    fun `a new day invalidates the remembered resolution`() = runBlocking {
        val repo = repository(published = setOf(today, tomorrow))
        assertEquals(today, repo.resolveDate("elpais", "es", today))

        // Same paper, next day's target: must hit the server again and pick up the new edition.
        assertEquals(tomorrow, repo.resolveDate("elpais", "es", tomorrow))
    }

    @Test
    fun `forceRefresh re-probes even when a resolution is remembered`() = runBlocking {
        val repo = repository(published = setOf(today))
        repo.resolveDate("elpais", "es", today)
        requested.clear()

        repo.resolveDate("elpais", "es", today, forceRefresh = true)
        assertEquals(1, requested.size)
    }

    @Test
    fun `an edition not published yet falls back, and is trusted only for a short while`() = runBlocking {
        val repo = repository(published = setOf(yesterday))
        assertEquals(yesterday, repo.resolveDate("elpais", "es", today))
        requested.clear()

        // Within the recheck window the fallback is reused (no requests)...
        assertEquals(yesterday, repo.resolveDate("elpais", "es", today))
        assertEquals(0, requested.size)
        // ...and a forced refresh finds nothing newer, still falls back.
        assertEquals(yesterday, repo.resolveDate("elpais", "es", today, forceRefresh = true))
    }

    @Test
    fun `no published edition within the lookback window resolves to null`() = runBlocking {
        assertNull(repository(published = emptySet()).resolveDate("elpais", "es", today))
    }

    @Test
    fun `getCovers emits in list order and skips newspapers with no cover`() = runBlocking {
        val repo = repository(published = setOf(today))
        val covers = repo.getCovers(
            listOf(PeriodicoRef("a", "a.es", "es"), PeriodicoRef("b", "b.es", "es")),
            today,
            forceRefresh = false
        ).toList()
        assertEquals(listOf("a", "b"), covers.map { it.periodico.id })
        assertEquals(today, covers.first().resolvedDate)
    }

    @Test
    fun `cancelling a lookup cancels its in-flight request instead of leaving it running`() = runBlocking {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            started.countDown()
            // A request that never completes by itself; it only stops when its call is cancelled.
            while (!chain.call().isCanceled()) Thread.sleep(10)
            cancelled.countDown()
            throw IOException("Canceled")
        }.build()
        val repo = PortadaCoverRepositoryImpl(client)

        val job = launch(Dispatchers.Default) { repo.resolveDate("elpais", "es", today) }
        assertTrue("request should have started", started.await(5, TimeUnit.SECONDS))
        job.cancel()

        assertTrue("the HTTP call should be cancelled with the coroutine", cancelled.await(5, TimeUnit.SECONDS))
        job.join()
    }
}
