package sergirex.portadasperiodicos.domain.model

/** A tab on the home screen: the user's favorites, or one catalog category. */
sealed interface HomeTab {
    /** Stable, language-independent identifier (also what the "initial tab" setting stores). */
    val key: String

    data object Favorites : HomeTab {
        override val key = "favorites"
    }

    data class Category(val category: PeriodicoCategory) : HomeTab {
        override val key: String get() = category.name
    }

    companion object {
        val categories: List<HomeTab> = PeriodicoCategory.entries.map(::Category)
        val default: HomeTab = Category(PeriodicoCategory.GENERAL)

        /**
         * Reads a stored key. Older versions saved the localized tab label instead ("Deportes",
         * "Sports", ...) and matched it by its first three letters; that's still understood here so
         * nobody's chosen initial tab resets on upgrade.
         */
        fun fromKey(key: String?): HomeTab {
            if (key == null) return default
            if (key == Favorites.key) return Favorites
            categories.firstOrNull { it.key == key }?.let { return it }
            return when (key.take(3).lowercase()) {
                "fav" -> Favorites
                "dep", "spo" -> Category(PeriodicoCategory.DEPORTES)
                "eco" -> Category(PeriodicoCategory.ECONOMIA)
                "loc" -> Category(PeriodicoCategory.LOCALES)
                "int" -> Category(PeriodicoCategory.INTERNACIONAL)
                else -> default
            }
        }
    }
}
