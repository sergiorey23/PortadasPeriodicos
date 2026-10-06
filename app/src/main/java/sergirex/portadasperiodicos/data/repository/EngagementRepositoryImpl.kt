package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasperiodicos.domain.model.Engagement
import sergirex.portadasperiodicos.domain.repository.EngagementRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val LEGACY_RATE_KEY = "rate"

// The old code kept a 0/1/2 "rate" flag in the default SharedPreferences; it's imported so the
// "already asked" state survives (see recordCoverOpened).
private val Context.engagementDataStore by preferencesDataStore(
    name = "engagement",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
                keysToMigrate = setOf(LEGACY_RATE_KEY)
            )
        )
    }
)

@Singleton
class EngagementRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : EngagementRepository {

    private val installedAt: Long by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }

    override suspend fun recordCoverOpened(): Engagement {
        context.engagementDataStore.edit { prefs ->
            prefs[COVER_OPENS] = (prefs[COVER_OPENS] ?: 0) + 1
            // Someone who was already asked by an older version: count that as a prompt made now.
            if (prefs[LEGACY_RATE] != null) {
                if ((prefs[LEGACY_RATE] ?: 0) > 0 && prefs[LAST_REVIEW_PROMPT] == null) {
                    prefs[LAST_REVIEW_PROMPT] = System.currentTimeMillis()
                }
                prefs.remove(LEGACY_RATE)
            }
        }
        val prefs = context.engagementDataStore.data.first()
        return Engagement(prefs[COVER_OPENS] ?: 0, installedAt, prefs[LAST_REVIEW_PROMPT])
    }

    override suspend fun markReviewPrompted() {
        context.engagementDataStore.edit { it[LAST_REVIEW_PROMPT] = System.currentTimeMillis() }
    }

    private companion object {
        val COVER_OPENS = intPreferencesKey("cover_opens")
        val LAST_REVIEW_PROMPT = longPreferencesKey("last_review_prompt_at")
        val LEGACY_RATE = intPreferencesKey(LEGACY_RATE_KEY)
    }
}
