package sergirex.portadasperiodicos.data.catalog

/** The catalog shipped inside the app (assets/periodicos.json): what's used until a newer copy has been downloaded. */
fun interface BundledCatalog {
    fun read(): String
}
