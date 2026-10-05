package sergirex.portadasperiodicos.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import sergirex.portadasperiodicos.data.local.PeriodicoDto
import sergirex.portadasperiodicos.data.local.toDomain
import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the newspaper catalog from assets/periodicos.json.
 *
 * Replaces Periodicos.java's five static String[] constants. Those arrays
 * mixed a presentation concern (which tab shows what) with raw data, lived
 * only in compiled code (changing the catalog meant a new app release even
 * for e.g. fixing a dead URL), and relied on a hand-rolled ":"-split encoding
 * with no validation. A JSON asset plus a typed DTO gives the same "ships
 * inside the APK, no network needed" deployment model the old arrays had,
 * but with a schema, and a single place (this class) that knows how to read it.
 *
 * The catalog is parsed once and cached in memory for the process lifetime —
 * it's ~60 small entries, re-parsing per call would be pure waste.
 */
@Singleton
class PeriodicosRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : PeriodicosRepository {

    private val catalog: List<Periodico> by lazy { loadCatalog() }

    override suspend fun getByCategory(category: PeriodicoCategory): Result<List<Periodico>> =
        withContext(Dispatchers.IO) {
            runCatching {
                catalog.filter { it.category == category }
            }
        }

    private fun loadCatalog(): List<Periodico> =
        context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
            .let { json.decodeFromString<List<PeriodicoDto>>(it) }
            .map { it.toDomain() }

    private companion object {
        const val CATALOG_ASSET = "periodicos.json"
    }
}
