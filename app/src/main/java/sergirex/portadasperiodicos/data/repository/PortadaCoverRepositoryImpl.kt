package sergirex.portadasperiodicos.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replaces GetPortadas.java. The old class did two unrelated things in one
 * doInBackground(): figure out which date actually has a published cover
 * (walking backwards up to 20 days), AND fetch+decode+cache the full bitmap
 * for that date — using a WeakReference<Context>/ExecutorService/Handler
 * trio to get background work back onto the main thread, the pre-coroutines
 * way of doing this.
 *
 * Here, resolving the date is still this class's job (it's genuine business
 * logic: "which edition is current"), but loading the actual image is left
 * to Coil at the call site (the RecyclerView adapter) — a dedicated image
 * library already does caching/decoding better than the old hand-rolled
 * SavePortada.saveFile() thumbnail cache, so duplicating that here would be
 * pure waste. Existence is checked with a HEAD request (confirmed against
 * the real img.kiosko.net: a missing date 302-redirects to a 404, which
 * HttpURLConnection follows and reports via getResponseCode() — no need to
 * download the image just to know if it exists, unlike the old GET-based
 * `url.getContent()` check).
 */
@Singleton
class PortadaCoverRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PortadaCoverRepository {

    override fun getCovers(
        periodicos: List<PeriodicoRef>,
        targetDate: String,
        cacheGroup: String,
        forceRefresh: Boolean
    ): Flow<PortadaCover> = flow {
        val cachePrefs = context.getSharedPreferences(CACHE_PREFS_PREFIX + cacheGroup, Context.MODE_PRIVATE)
        val dateChangedSinceLastFetch = cachePrefs.getString(KEY_LAST_FETCHED_DATE, null) != targetDate
        val editor = if (dateChangedSinceLastFetch) cachePrefs.edit() else null

        for (periodico in periodicos) {
            currentCoroutineContext().ensureActive()

            val cachedDate = cachePrefs.getString(periodico.id, null)
            val resolvedDate = if (!forceRefresh && !dateChangedSinceLastFetch && cachedDate != null) {
                cachedDate
            } else {
                resolveAvailableDate(periodico, targetDate)
            }

            if (resolvedDate != null) {
                editor?.putString(periodico.id, resolvedDate)
                emit(PortadaCover(periodico, resolvedDate, thumbnailUrl(resolvedDate, periodico)))
            }
        }

        if (dateChangedSinceLastFetch) {
            editor?.putString(KEY_LAST_FETCHED_DATE, targetDate)
            editor?.apply()
        }
    }.flowOn(Dispatchers.IO)

    /** Walks backwards from [startDate] up to [MAX_DAYS_BACK] days looking for a published edition. */
    private suspend fun resolveAvailableDate(periodico: PeriodicoRef, startDate: String): String? {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.FRANCE)
        val calendar = Calendar.getInstance().apply {
            time = runCatching { formatter.parse(startDate) }.getOrNull() ?: Date()
        }
        repeat(MAX_DAYS_BACK) {
            currentCoroutineContext().ensureActive()
            val candidateDate = formatter.format(calendar.time)
            if (coverExists(thumbnailUrl(candidateDate, periodico))) return candidateDate
            calendar.add(Calendar.DATE, -1)
        }
        return null
    }

    private fun coverExists(imageUrl: String): Boolean = runCatching {
        (URL(imageUrl).openConnection() as HttpURLConnection).run {
            requestMethod = "HEAD"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            try {
                responseCode == HttpURLConnection.HTTP_OK
            } finally {
                disconnect()
            }
        }
    }.getOrDefault(false)

    private fun thumbnailUrl(date: String, periodico: PeriodicoRef) =
        "https://img.kiosko.net/$date/${periodico.country}/${periodico.id}.640.jpg"

    private companion object {
        const val CACHE_PREFS_PREFIX = "Fechas"
        const val KEY_LAST_FETCHED_DATE = "fechaPortadas"
        const val MAX_DAYS_BACK = 20
        const val DATE_PATTERN = "yyyy/MM/dd"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000
    }
}
