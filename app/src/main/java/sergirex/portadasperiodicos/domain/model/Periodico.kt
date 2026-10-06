package sergirex.portadasperiodicos.domain.model

/**
 * A single newspaper the app can show covers for.
 *
 * This replaces the legacy "id:domain:country" colon-encoded String that used
 * to flow through the whole app (Periodicos.java's static arrays, GetPortadas'
 * manual String.split(":"), the Portada record's untyped fields). That encoding
 * was a classic "stringly typed data" smell: three unrelated pieces of
 * information packed into one String, with a parser re-implemented at every
 * consumer and no compiler help if a field was missing or misplaced.
 *
 * @param id unique slug (also the image/cache filename key), e.g. "elpais"
 * @param name the title shown to the user, e.g. "El País"
 * @param domain the newspaper's web domain, e.g. "elpais.com"
 * @param country ISO-ish country code used for the cover's image path, e.g. "es"
 * @param category the section this newspaper is listed under
 */
data class Periodico(
    val id: String,
    val name: String,
    val domain: String,
    val country: String,
    val category: PeriodicoCategory
)
