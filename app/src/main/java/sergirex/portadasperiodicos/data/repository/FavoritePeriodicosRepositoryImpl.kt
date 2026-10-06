package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.data.local.FavoritesCodec
import sergirex.portadasperiodicos.data.local.editSafely
import sergirex.portadasperiodicos.data.local.safeData
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject
import javax.inject.Singleton

private val FAVORITES_KEY = stringPreferencesKey("favorites")

/**
 * One-time import of the old "periodicos" SharedPreferences file (key = domain, value = legacy
 * "id:domain:country" string) so existing users keep their favorites.
 */
private class LegacyFavoritesMigration(private val context: Context) : DataMigration<Preferences> {
    private val legacy get() = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: Preferences) =
        FAVORITES_KEY !in currentData && legacy.all.isNotEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val favorites = legacy.all.values.filterIsInstance<String>().map { PeriodicoRef.fromLegacyEncoded(it) }
        return currentData.toMutablePreferences().apply { this[FAVORITES_KEY] = FavoritesCodec.encode(favorites) }
    }

    override suspend fun cleanUp() {
        legacy.edit().clear().apply()
    }

    private companion object {
        const val LEGACY_PREFS = "periodicos"
    }
}

private val Context.favoritesDataStore by preferencesDataStore(
    name = "favorites",
    produceMigrations = { context -> listOf(LegacyFavoritesMigration(context)) }
)

@Singleton
class FavoritePeriodicosRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FavoritePeriodicosRepository {

    override val favorites: Flow<List<PeriodicoRef>> = context.favoritesDataStore.safeData
        .map { FavoritesCodec.decode(it[FAVORITES_KEY]) }

    override suspend fun toggle(periodico: PeriodicoRef) {
        context.favoritesDataStore.editSafely { prefs ->
            val current = FavoritesCodec.decode(prefs[FAVORITES_KEY])
            val updated = if (current.any { it.id == periodico.id }) current.filterNot { it.id == periodico.id } else current + periodico
            prefs[FAVORITES_KEY] = FavoritesCodec.encode(updated)
        }
    }
}
