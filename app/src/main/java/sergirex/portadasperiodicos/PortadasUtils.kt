package sergirex.portadasperiodicos

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File

object PortadasUtils {

    /**
     * Helper function to trigger the media scanner.
     * Uses MediaScannerConnection which is the modern approach.
     */
    @JvmStatic
    fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
    }
}
