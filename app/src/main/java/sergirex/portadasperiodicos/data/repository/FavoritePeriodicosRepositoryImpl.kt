package sergirex.portadasperiodicos.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the same "periodicos" SharedPreferences file (key = domain, value =
 * legacy-encoded "id:domain:country" string) that PortadaDetalle's favorite
 * toggle and FavoritosWidget already read/write. Left as-is deliberately:
 * changing this storage format is out of scope here and would also require
 * updating FavoritosWidget (a later migration phase) in lockstep.
 */
@Singleton
class FavoritePeriodicosRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FavoritePeriodicosRepository {

    override fun getFavorites(): List<PeriodicoRef> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all.values
            .filterIsInstance<String>()
            .map { PeriodicoRef.fromLegacyEncoded(it) }

    private companion object {
        const val PREFS_NAME = "periodicos"
    }
}
