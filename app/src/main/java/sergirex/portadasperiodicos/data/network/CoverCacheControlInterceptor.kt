package sergirex.portadasperiodicos.data.network

import okhttp3.Interceptor
import okhttp3.Response
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate

/**
 * img.kiosko.net sends only Last-Modified/ETag, so Coil falls back to a heuristic freshness
 * (a fraction of the file's age) and re-downloads or revalidates unpredictably. Covers are
 * addressed by date in the URL, so their lifetime is known:
 *  - an edition from a past day never changes -> cache it for a year, never hit the network again;
 *  - today's edition may still be replaced early in the day -> trust the cache for 30 minutes,
 *    then revalidate with the ETag (a cheap 304 when unchanged).
 * Because the date is part of the URL, a new day automatically means new cache keys.
 */
class CoverCacheControlInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!request.url.host.endsWith(CoverUrls.HOST_SUFFIX) || (!response.isSuccessful && response.code != 304)) {
            return response
        }

        val date = CoverUrls.editionDateOf(request.url.pathSegments) ?: return response
        val cacheControl = if (EditionDate.isPast(date)) PAST_EDITION else TODAYS_EDITION
        if (response.header("Cache-Control") == cacheControl) return response
        return response.newBuilder().header("Cache-Control", cacheControl).build()
    }

    private companion object {
        const val PAST_EDITION = "public, max-age=31536000, immutable"
        const val TODAYS_EDITION = "public, max-age=1800"
    }
}
