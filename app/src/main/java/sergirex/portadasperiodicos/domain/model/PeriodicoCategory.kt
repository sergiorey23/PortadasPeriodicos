package sergirex.portadasperiodicos.domain.model

/**
 * The five sections the app organizes newspapers into. This is the single
 * source of truth for "which categories exist" — previously that notion was
 * implicit, spread across five near-identical Fragments, a set of parallel
 * string arrays in Periodicos.java, and a set of string resources
 * (first_tab..fifth_tab) that all had to be kept in sync by hand.
 */
enum class PeriodicoCategory {
    GENERAL,
    DEPORTES,
    ECONOMIA,
    LOCALES,
    INTERNACIONAL
}
