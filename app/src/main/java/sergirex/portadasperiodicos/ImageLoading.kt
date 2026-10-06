package sergirex.portadasperiodicos

import android.content.Context
import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult

/**
 * Fetches [url] through Coil (memory + disk cache) and returns the decoded bitmap, or null on failure.
 * Used where there's no ImageView to hand Coil as a target (widget, share/save, detail screen —
 * where `ImageRequest.target()` combined with `execute()` was found to hang).
 */
suspend fun Context.loadBitmap(url: String): Bitmap? {
    val result = imageLoader.execute(ImageRequest.Builder(this).data(url).build())
    return ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
}
