package sergirex.portadasperiodicos

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File

object PortadasUtils {
    const val MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1

    /**
     * Helper function to trigger the media scanner.
     * Uses MediaScannerConnection which is the modern approach.
     */
    fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
    }
}