package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val INITIAL_TAB_KEY_NAME = "init_category"

// "init_category" in the old default SharedPreferences file is imported under the same key; the
// stored value may be a legacy localized label, which HomeTab.fromKey understands.
private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
                keysToMigrate = setOf(INITIAL_TAB_KEY_NAME)
            )
        )
    }
)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val dataStore: DataStore<Preferences> = context.settingsDataStore

    private val data: Flow<Preferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val initialTab: Flow<HomeTab> = data.map { HomeTab.fromKey(it[INITIAL_TAB]) }
    override val dailyNotificationEnabled: Flow<Boolean> = data.map { it[DAILY_NOTIFICATION] ?: true }
    override val notificationPermissionRequested: Flow<Boolean> = data.map { it[PERMISSION_REQUESTED] ?: false }

    override suspend fun setInitialTab(tab: HomeTab) {
        dataStore.edit { it[INITIAL_TAB] = tab.key }
    }

    override suspend fun setDailyNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[DAILY_NOTIFICATION] = enabled }
    }

    override suspend fun setNotificationPermissionRequested() {
        dataStore.edit { it[PERMISSION_REQUESTED] = true }
    }

    private companion object {
        val INITIAL_TAB = stringPreferencesKey(INITIAL_TAB_KEY_NAME)
        val DAILY_NOTIFICATION = booleanPreferencesKey("daily_notification")
        val PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
