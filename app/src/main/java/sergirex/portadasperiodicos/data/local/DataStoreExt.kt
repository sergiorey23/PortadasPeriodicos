package sergirex.portadasperiodicos.data.local

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

private const val TAG = "DataStore"

/** The preferences, or empty ones if the file can't be read (corrupted/unavailable): defaults beat a crash. */
val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { error ->
        if (error is IOException) {
            Log.w(TAG, "Couldn't read preferences, using defaults", error)
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

/** Like [edit], but a write failure (disk full, ...) is logged instead of crashing the caller. Returns whether it was saved. */
suspend fun DataStore<Preferences>.editSafely(transform: (MutablePreferences) -> Unit): Boolean = try {
    edit { transform(it) }
    true
} catch (error: IOException) {
    Log.w(TAG, "Couldn't save preferences", error)
    false
}
