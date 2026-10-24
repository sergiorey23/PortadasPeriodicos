package sergirex.portadasperiodicos

import android.content.Context
import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.disk.DiskCache
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Fetches [url] through Coil (memory + disk cache) and returns the decoded bitmap, or null on failure.
 * Used where there's no ImageView to hand Coil as a target (widget, detail screen —
 * where `ImageRequest.target()` combined with `execute()` was found to hang).
 */
suspend fun Context.loadBitmap(url: String, configure: ImageRequest.Builder.() -> Unit = {}): Bitmap? {
    val result = imageLoader.execute(ImageRequest.Builder(this).data(url).apply(configure).build())
    return ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
}

/**
 * The image file at [url] exactly as the server sent it (the JPEG bytes), or null on failure. Read from
 * Coil's disk cache — the detail screen has normally just downloaded it — so saving or sharing neither
 * downloads it again nor re-encodes a decoded bitmap.
 */
suspend fun Context.loadImageBytes(url: String): ByteArray? {
    val diskCache = imageLoader.diskCache ?: return null
    withContext(Dispatchers.IO) { diskCache.read(url) }?.let { return it }
    // Not cached yet: let Coil download it (which stores it on disk), then read it back.
    if (imageLoader.execute(ImageRequest.Builder(this).data(url).build()) !is SuccessResult) return null
    return withContext(Dispatchers.IO) { diskCache.read(url) }
}

/** Coil keys network images by their URL. */
private fun DiskCache.read(key: String): ByteArray? = try {
    openSnapshot(key)?.use { snapshot -> fileSystem.read(snapshot.data) { readByteArray() } }
} catch (error: IOException) {
    null
}
