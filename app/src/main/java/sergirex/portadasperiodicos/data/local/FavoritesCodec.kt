package sergirex.portadasperiodicos.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sergirex.portadasperiodicos.domain.model.PeriodicoRef

@Serializable
private data class FavoriteDto(val id: String, val domain: String, val country: String, val name: String? = null)

/** How the favorites list is stored inside DataStore: a JSON array under a single key. */
object FavoritesCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(favorites: List<PeriodicoRef>): String =
        json.encodeToString(favorites.map { FavoriteDto(it.id, it.domain, it.country, it.name) })

    fun decode(raw: String?): List<PeriodicoRef> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FavoriteDto>>(raw) }
            .getOrDefault(emptyList())
            .map { PeriodicoRef(it.id, it.name ?: it.id, it.domain, it.country) }
    }
}
