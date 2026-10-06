package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.PeriodicoRef

/**
 * The user's favorited newspapers, regardless of category, in the order they were added.
 * Kept separate from [PeriodicosRepository] (the fixed catalog) since favorites are
 * user-authored state. Exposed as a [Flow] so every screen reacts to a change on its own.
 */
interface FavoritePeriodicosRepository {
    val favorites: Flow<List<PeriodicoRef>>

    /** Adds [periodico] to the favorites, or removes it if it is already there. */
    suspend fun toggle(periodico: PeriodicoRef)
}
