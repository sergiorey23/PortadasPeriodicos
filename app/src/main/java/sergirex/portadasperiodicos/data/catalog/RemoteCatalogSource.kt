package sergirex.portadasperiodicos.data.catalog

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

sealed interface RemoteCatalog {
    /** The server confirmed our cached copy is current (HTTP 304). */
    data object NotModified : RemoteCatalog
    data class Updated(val text: String, val etag: String?) : RemoteCatalog
    data class Failed(val reason: String) : RemoteCatalog
}

/** Downloads the catalog file, sending the cached ETag so an unchanged file costs a tiny 304 instead of a download. */
class RemoteCatalogSource(private val client: OkHttpClient, private val url: String) {

    fun fetch(cachedEtag: String?): RemoteCatalog = try {
        val request = Request.Builder().url(url).apply { cachedEtag?.let { header("If-None-Match", it) } }.build()
        client.newCall(request).execute().use { response -> interpret(response) }
    } catch (error: IOException) {
        RemoteCatalog.Failed(error.message ?: "network error")
    }

    private fun interpret(response: Response): RemoteCatalog {
        if (response.code == 304) return RemoteCatalog.NotModified
        if (!response.isSuccessful) return RemoteCatalog.Failed("HTTP ${response.code}")
        val body = response.body ?: return RemoteCatalog.Failed("empty body")
        // The real file is a few KB; refuse anything absurd instead of reading it into memory.
        if (body.contentLength() > MAX_BYTES) return RemoteCatalog.Failed("too large")
        val text = body.source().use { source ->
            source.request(MAX_BYTES + 1)
            if (source.buffer.size > MAX_BYTES) null else source.buffer.readUtf8()
        } ?: return RemoteCatalog.Failed("too large")
        return RemoteCatalog.Updated(text, response.header("ETag"))
    }

    private companion object {
        const val MAX_BYTES = 512L * 1024
    }
}
