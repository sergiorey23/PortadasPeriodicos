package sergirex.portadasperiodicos.data.local

import android.content.Context
import coil3.SingletonImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Size and clearing of the cover images Coil keeps in memory and on disk. */
@Singleton
class ImageCacheControl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val loader get() = SingletonImageLoader.get(context)

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) { loader.diskCache?.size ?: 0L }

    suspend fun clear() = withContext(Dispatchers.IO) {
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }
}
