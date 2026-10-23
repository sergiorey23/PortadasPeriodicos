package sergirex.portadasperiodicos.data.repository

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasperiodicos.data.local.editSafely
import sergirex.portadasperiodicos.data.local.safeData
import sergirex.portadasperiodicos.domain.model.AdConsent
import sergirex.portadasperiodicos.domain.model.ConsentStatus
import sergirex.portadasperiodicos.domain.repository.ConsentRepository
import javax.inject.Inject
import javax.inject.Singleton

// Deliberately not in the backup rules: a new phone asks again.
private val Context.consentDataStore by preferencesDataStore(name = "consent")

@Singleton
class ConsentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConsentRepository {

    override val consent: Flow<AdConsent> = context.consentDataStore.safeData.map { prefs ->
        val status = prefs[STATUS]?.let { stored -> ConsentStatus.entries.firstOrNull { it.name == stored } }
            ?: ConsentStatus.UNKNOWN
        if (status == ConsentStatus.UNKNOWN) AdConsent.UNKNOWN else AdConsent(status, prefs[DECIDED_AT] ?: 0L)
    }

    override suspend fun setStatus(status: ConsentStatus) {
        context.consentDataStore.editSafely {
            it[STATUS] = status.name
            it[DECIDED_AT] = if (status == ConsentStatus.UNKNOWN) 0L else System.currentTimeMillis()
        }
    }

    private companion object {
        val STATUS = stringPreferencesKey("ads_consent")
        val DECIDED_AT = longPreferencesKey("ads_consent_at")
    }
}
