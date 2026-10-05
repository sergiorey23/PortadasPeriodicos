package sergirex.portadasperiodicos

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import sergirex.portadasperiodicos.data.repository.PeriodicosRepositoryImpl
import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import android.content.Context

/**
 * Temporary compatibility bridge for the legacy Java call sites (the five
 * category Fragments, and transitively GetPortadas/Portadas.java) that still
 * expect `String[]` arrays in the old "id:domain:country" encoding and aren't
 * wired into Hilt yet. Those classes are themselves migrated in a later phase
 * (once GetPortadas moves to Kotlin + coroutines); until then, this object
 * is a "strangler fig" seam: the real source of truth is already
 * PeriodicosRepository/assets/periodicos.json, this just re-encodes it into
 * the shape the not-yet-migrated code expects, and is deleted once nothing
 * calls it anymore.
 *
 * Deliberately NOT Hilt-injected: it's called from plain Java Fragments with
 * no @AndroidEntryPoint, so it builds its own throwaway repository instance
 * per call instead. That's acceptable only because it's transitional scaffolding.
 */
object Periodicos {
    private val json = Json { ignoreUnknownKeys = true }

    @JvmStatic
    fun general(context: Context): Array<String> = legacyArray(context, PeriodicoCategory.GENERAL)

    @JvmStatic
    fun deportes(context: Context): Array<String> = legacyArray(context, PeriodicoCategory.DEPORTES)

    @JvmStatic
    fun economia(context: Context): Array<String> = legacyArray(context, PeriodicoCategory.ECONOMIA)

    @JvmStatic
    fun locales(context: Context): Array<String> = legacyArray(context, PeriodicoCategory.LOCALES)

    @JvmStatic
    fun internacional(context: Context): Array<String> = legacyArray(context, PeriodicoCategory.INTERNACIONAL)

    private fun legacyArray(context: Context, category: PeriodicoCategory): Array<String> = runBlocking {
        PeriodicosRepositoryImpl(context.applicationContext, json)
            .getByCategory(category)
            .getOrElse { emptyList() }
            .map { it.toLegacyEncodedString() }
            .toTypedArray()
    }

    private fun Periodico.toLegacyEncodedString(): String = "$id:$domain:$country"
}
