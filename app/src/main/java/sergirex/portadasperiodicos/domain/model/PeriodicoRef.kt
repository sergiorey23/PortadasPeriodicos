package sergirex.portadasperiodicos.domain.model

/**
 * The minimal identity needed to resolve and load a newspaper's cover —
 * everything PortadaCoverRepository needs and nothing else. Kept separate
 * from [Periodico] (the catalog entry, which also carries a [PeriodicoCategory])
 * because cover-loading has no notion of category: a favorite can belong to
 * any category, and the old code already treated id/domain/country as the
 * only fields that mattered once you're past "which tab is this in".
 */
data class PeriodicoRef(
    val id: String,
    val domain: String,
    val country: String
) {
    companion object {
        /**
         * Parses the legacy "id:domain:country" (or bare "domain.com") encoding
         * that favorites are still stored in. Mirrors exactly the derivation
         * GetPortadas.java used to do inline, so existing favorites (and their
         * cache keys/filenames, both derived from [id]) keep working unchanged.
         */
        fun fromLegacyEncoded(raw: String): PeriodicoRef {
            val parts = raw.split(":")
            return if (parts.size > 1) {
                PeriodicoRef(id = parts[0], domain = parts[1], country = parts.getOrElse(2) { "es" })
            } else {
                PeriodicoRef(id = raw.substringBefore("."), domain = raw, country = "es")
            }
        }
    }
}

fun Periodico.toRef(): PeriodicoRef = PeriodicoRef(id, domain, country)
