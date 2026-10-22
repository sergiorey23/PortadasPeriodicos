package sergirex.portadasperiodicos.domain.model

/** What the app knows about its newspaper catalog, for the Settings screen. */
data class CatalogStatus(
    val version: Int,
    val entries: Int,
    /** Epoch millis of the last successful check of the published catalog, 0 if it never happened. */
    val lastCheckedAt: Long
)

enum class CatalogRefreshResult { UPDATED, UP_TO_DATE, FAILED }
