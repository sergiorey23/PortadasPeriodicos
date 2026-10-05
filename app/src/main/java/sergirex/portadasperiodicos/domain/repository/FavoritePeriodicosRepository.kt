package sergirex.portadasperiodicos.domain.repository

import sergirex.portadasperiodicos.domain.model.PeriodicoRef

/**
 * The user's favorited newspapers, regardless of category. Kept separate
 * from [PeriodicosRepository] (the fixed catalog) since favorites are
 * user-authored state, not static data shipped with the app.
 */
interface FavoritePeriodicosRepository {
    fun getFavorites(): List<PeriodicoRef>
}
