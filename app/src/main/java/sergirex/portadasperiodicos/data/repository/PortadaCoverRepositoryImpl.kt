package sergirex.portadasperiodicos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Resolves "which edition is current" per newspaper: probes backwards from the target date
 * until a cover exists (HEAD requests, no download). Image bytes are Coil's job.
 *
 * Resolutions are memoized for the process lifetime, but only trusted while they can't be stale:
 *  - a past target date is immutable, so its answer is final;
 *  - for today, an answer equal to today is final, while a fallback to an older edition is
 *    re-probed after [FALLBACK_RECHECK_MS] — otherwise a cover that wasn't out yet when the app
 *    first opened would stay "yesterday's" for the whole day;
 *  - a different target date (a new day) never matches, so everything is re-resolved.
 */
@Singleton
class PortadaCoverRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : PortadaCoverRepository {

    private data class Resolution(val target: String, val date: String, val checkedAt: Long)

    private val resolutions = ConcurrentHashMap<String, Resolution>()

    override fun getCovers(
        periodicos: List<PeriodicoRef>,
        targetDate: String,
        forceRefresh: Boolean
    ): Flow<PortadaCover> = flow {
        coroutineScope {
            // Probe several newspapers at once, but emit in list order as each one resolves.
            val gate = Semaphore(MAX_CONCURRENT_RESOLUTIONS)
            val pending = periodicos.map { p ->
                p to async { gate.withPermit { resolveDate(p.id, p.country, targetDate, forceRefresh) } }
            }
            for ((p, date) in pending) {
                date.await()?.let { emit(PortadaCover(p, it, CoverUrls.thumbnail(it, p.country, p.id))) }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun resolveDate(
        id: String,
        country: String,
        targetDate: String,
        forceRefresh: Boolean
    ): String? = withContext(Dispatchers.IO) {
        val key = "$id|$country"
        if (!forceRefresh) resolutions[key]?.takeIf { it.isUsableFor(targetDate) }?.let { return@withContext it.date }

        val date = EditionDate.lookback(targetDate, MAX_DAYS_BACK).firstOrNull {
            currentCoroutineContext().ensureActive()
            coverExists(CoverUrls.thumbnail(it, country, id))
        } ?: return@withContext null
        resolutions[key] = Resolution(targetDate, date, System.currentTimeMillis())
        date
    }

    private fun Resolution.isUsableFor(requested: String): Boolean = target == requested &&
        (EditionDate.isPast(target) || date == target || System.currentTimeMillis() - checkedAt < FALLBACK_RECHECK_MS)

    private suspend fun coverExists(url: String): Boolean = try {
        client.newCall(Request.Builder().url(url).head().build()).await().use { it.isSuccessful }
    } catch (error: IOException) {
        false
    }

    /** Suspends until the response arrives; cancelling the coroutine cancels the HTTP call too. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, _, _ -> response.close() }
            }
        })
    }

    private companion object {
        const val MAX_DAYS_BACK = 20
        const val MAX_CONCURRENT_RESOLUTIONS = 6
        const val FALLBACK_RECHECK_MS = 30 * 60 * 1000L
    }
}
