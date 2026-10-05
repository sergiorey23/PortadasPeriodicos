package sergirex.portadasperiodicos

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PortadasUtils {
    const val MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1
    private const val DATE_PATTERN = "yyyy/MM/dd"

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

    /**
     * "Today", except before 6am it's still yesterday's edition — most
     * newspapers haven't published their new cover yet at that hour. This
     * logic used to be copy-pasted in Portadas.java's onCreate and in every
     * category Fragment's refreshFragment().
     */
    @JvmStatic
    fun effectiveTodayDate(): String {
        val calendar = Calendar.getInstance().apply {
            time = Date()
            if (get(Calendar.HOUR_OF_DAY) < 6) add(Calendar.DATE, -1)
        }
        return SimpleDateFormat(DATE_PATTERN, Locale.FRANCE).format(calendar.time)
    }
}