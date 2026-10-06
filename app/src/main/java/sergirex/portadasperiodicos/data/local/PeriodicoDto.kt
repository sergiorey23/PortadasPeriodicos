package sergirex.portadasperiodicos.data.local

import kotlinx.serialization.Serializable
import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

/**
 * Wire shape of each entry in assets/periodicos.json. Kept separate from the
 * domain [Periodico] model on purpose: the JSON schema is a data-layer detail
 * (today it's a bundled asset, tomorrow it could be a remote endpoint) and
 * shouldn't leak its serialization annotations into the domain/UI layers.
 */
@Serializable
data class PeriodicoDto(
    val id: String,
    val name: String,
    val domain: String,
    val country: String = "es",
    val category: String
)

fun PeriodicoDto.toDomain(): Periodico = Periodico(
    id = id,
    name = name,
    domain = domain,
    country = country,
    category = PeriodicoCategory.valueOf(category)
)
