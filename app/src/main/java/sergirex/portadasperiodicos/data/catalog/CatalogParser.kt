package sergirex.portadasperiodicos.data.catalog

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sergirex.portadasperiodicos.data.local.PeriodicoDto
import sergirex.portadasperiodicos.data.local.toDomain
import sergirex.portadasperiodicos.domain.model.Periodico

/** A catalog that passed validation. [version] grows with every published change; the newest one wins. */
data class ParsedCatalog(val version: Int, val periodicos: List<Periodico>, val skipped: Int)

/**
 * Reads the catalog JSON (the bundled asset, the cached copy and the remote file all use this):
 *
 * ```
 * { "schemaVersion": 1, "catalogVersion": 7, "periodicos": [ { "id", "name", "domain", "country", "category" }, ... ] }
 * ```
 *
 * It is deliberately forgiving about single entries and strict about the whole file, because the file is
 * edited by hand and downloaded: one bad entry (blank name, unknown category, duplicated id) is skipped
 * instead of breaking the app, but a file that is not a catalog at all, was written for a newer app, or
 * has almost no usable entries is rejected, so a bad deploy can never empty the app.
 */
object CatalogParser {
    /** The newest file format this app understands. */
    const val SUPPORTED_SCHEMA = 1
    const val MIN_ENTRIES = 10

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): ParsedCatalog? {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return null
        val (schema, version, entries) = when (root) {
            is JsonObject -> Triple(
                root["schemaVersion"]?.intOrNull() ?: 1,
                root["catalogVersion"]?.intOrNull() ?: 0,
                root["periodicos"] as? JsonArray ?: return null
            )
            is JsonArray -> Triple(1, 0, root) // the original bare-array format
            else -> return null
        }
        if (schema > SUPPORTED_SCHEMA) return null

        val seen = HashSet<String>()
        var skipped = 0
        val valid = entries.mapNotNull { element ->
            val entry = element.toValidEntry()
            if (entry == null || !seen.add(entry.id)) {
                skipped++
                null
            } else {
                entry
            }
        }
        return if (valid.size >= MIN_ENTRIES) ParsedCatalog(version, valid, skipped) else null
    }

    private fun JsonElement.toValidEntry(): Periodico? {
        val dto = runCatching { json.decodeFromJsonElement(PeriodicoDto.serializer(), this) }.getOrNull() ?: return null
        if (dto.id.isBlank() || dto.name.isBlank() || dto.domain.isBlank() || dto.country.isBlank()) return null
        return runCatching { dto.toDomain() }.getOrNull() // unknown category -> skipped
    }

    private fun JsonElement.intOrNull(): Int? = runCatching { jsonPrimitive.int }.getOrNull()
}
