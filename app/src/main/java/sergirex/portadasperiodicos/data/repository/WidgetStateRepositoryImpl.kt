package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasperiodicos.data.local.editSafely
import sergirex.portadasperiodicos.data.local.safeData
import sergirex.portadasperiodicos.domain.repository.WidgetStateRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val LEGACY_PREFS = "IndiceWDT"
private const val LEGACY_KEY = "index"

private val Context.widgetDataStore by preferencesDataStore(
    name = "widget",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS, keysToMigrate = setOf(LEGACY_KEY)))
    }
)

@Singleton
class WidgetStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetStateRepository {

    override suspend fun index(): Int = context.widgetDataStore.safeData.first()[INDEX] ?: 0

    override suspend fun setIndex(index: Int) {
        context.widgetDataStore.editSafely { it[INDEX] = index }
    }

    private companion object {
        val INDEX = intPreferencesKey(LEGACY_KEY)
    }
}
