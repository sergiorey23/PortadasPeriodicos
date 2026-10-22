package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.data.local.editSafely
import sergirex.portadasperiodicos.data.local.safeData
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.ReminderSchedule
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
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

    private val data: Flow<Preferences> = dataStore.safeData

    override val initialTab: Flow<HomeTab> = data.map { HomeTab.fromKey(it[INITIAL_TAB]) }
    override val dailyNotificationEnabled: Flow<Boolean> = data.map { it[DAILY_NOTIFICATION] ?: true }
    override val notificationPermissionRequested: Flow<Boolean> = data.map { it[PERMISSION_REQUESTED] ?: false }

    override val dailyNotificationMinutes: Flow<Int> =
        data.map { (it[NOTIFICATION_MINUTES] ?: ReminderSchedule.DEFAULT_MINUTES).coerceIn(0, MINUTES_IN_DAY - 1) }
    override val dailyNotificationDays: Flow<Set<Int>> = data.map { prefs ->
        prefs[NOTIFICATION_DAYS]?.mapNotNull { it.toIntOrNull() }?.filter { it in ReminderSchedule.ALL_DAYS }?.toSet()
            ?: ReminderSchedule.ALL_DAYS
    }
    override val gridColumns: Flow<Int> = data.map { (it[GRID_COLUMNS] ?: 0).coerceIn(0, 3) }
    override val pureBlack: Flow<Boolean> = data.map { it[PURE_BLACK] ?: false }

    override suspend fun setDailyNotificationMinutes(minutes: Int) {
        dataStore.editSafely { it[NOTIFICATION_MINUTES] = minutes.coerceIn(0, MINUTES_IN_DAY - 1) }
    }

    override suspend fun setDailyNotificationDays(days: Set<Int>) {
        dataStore.editSafely { it[NOTIFICATION_DAYS] = days.map(Int::toString).toSet() }
    }

    override suspend fun setGridColumns(columns: Int) {
        dataStore.editSafely { it[GRID_COLUMNS] = columns.coerceIn(0, 3) }
    }

    override suspend fun setPureBlack(enabled: Boolean) {
        dataStore.editSafely { it[PURE_BLACK] = enabled }
    }

    override suspend fun setInitialTab(tab: HomeTab) {
        dataStore.editSafely { it[INITIAL_TAB] = tab.key }
    }

    override suspend fun setDailyNotificationEnabled(enabled: Boolean) {
        dataStore.editSafely { it[DAILY_NOTIFICATION] = enabled }
    }

    override suspend fun setNotificationPermissionRequested() {
        dataStore.editSafely { it[PERMISSION_REQUESTED] = true }
    }

    private companion object {
        val INITIAL_TAB = stringPreferencesKey(INITIAL_TAB_KEY_NAME)
        val DAILY_NOTIFICATION = booleanPreferencesKey("daily_notification")
        val PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
        val NOTIFICATION_MINUTES = intPreferencesKey("daily_notification_minutes")
        val NOTIFICATION_DAYS = stringSetPreferencesKey("daily_notification_days")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        const val MINUTES_IN_DAY = 24 * 60
    }
}
