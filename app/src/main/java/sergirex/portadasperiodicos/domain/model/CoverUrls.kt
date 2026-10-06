package sergirex.portadasperiodicos.domain.model

/** The one place that knows how cover image URLs are laid out on the kiosko.net CDN. */
object CoverUrls {
    const val HOST_SUFFIX = "kiosko.net"
    private const val BASE = "https://img.$HOST_SUFFIX"

    fun full(date: String, country: String, id: String) = "$BASE/$date/$country/$id.jpg"

    fun thumbnail(date: String, country: String, id: String) = "$BASE/$date/$country/$id.640.jpg"

    /** The edition date encoded in a cover URL's path (`/yyyy/MM/dd/country/file`), or null. */
    fun editionDateOf(pathSegments: List<String>): String? =
        pathSegments.take(3).takeIf { it.size == 3 && it.all { s -> s.all(Char::isDigit) } }?.joinToString("/")
}
