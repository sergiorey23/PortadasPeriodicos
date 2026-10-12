package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.data.local.editSafely
import sergirex.portadasperiodicos.data.local.safeData
import sergirex.portadasperiodicos.domain.repository.AdsEntitlementRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val LEGACY_KEY = "remove_fb_ads"

// The old flag lived in the default SharedPreferences under the same key; it is imported once so people who
// already bought keep their entitlement (and Google Play re-confirms it at the next start anyway).
private val Context.entitlementDataStore by preferencesDataStore(
    name = "entitlements",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "${context.packageName}_preferences",
                keysToMigrate = setOf(LEGACY_KEY)
            )
        )
    }
)

@Singleton
class AdsEntitlementRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AdsEntitlementRepository {

    override val adsRemoved: Flow<Boolean> = context.entitlementDataStore.safeData.map { it[ADS_REMOVED] ?: false }

    override suspend fun setAdsRemoved(removed: Boolean) {
        context.entitlementDataStore.editSafely { it[ADS_REMOVED] = removed }
    }

    private companion object {
        val ADS_REMOVED = booleanPreferencesKey(LEGACY_KEY)
    }
}
