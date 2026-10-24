package sergirex.portadasperiodicos

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves or shares a newspaper cover. The image comes through Coil's cache (`loadImageBytes`), the same
 * pipeline every other screen uses, and its original JPEG bytes are written as they are: re-encoding a
 * decoded bitmap made files 2-3x bigger, slower to write, and needed the whole bitmap in memory.
 *
 * `checkPermissions()` can't call `ActivityCompat.requestPermissions` itself the way the
 * old Java version did (casting `context` to `Activity`): an `ActivityResultLauncher` has
 * to be registered as a field of the hosting Activity *before* it's STARTED, so the caller
 * (PortadaDetalle) registers one and hands it in here.
 */
class SavePortada(
    private val context: Context,
    private val requestPermissionLauncher: ActivityResultLauncher<String>
) {

    fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return false
    }

    val isExternalStorageWritable: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    val albumStorageDir: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Portadas")
            .apply { mkdirs() }

    /** Downloads [imageUrl] and saves it into the public Pictures/Portadas gallery folder as [file]. */
    suspend fun saveToGallery(view: View, imageUrl: String, file: File) {
        Toast.makeText(context, "Iniciando descarga...", Toast.LENGTH_SHORT).show()

        val bytes = context.loadImageBytes(imageUrl)
        if (bytes == null) {
            Snackbar.make(view, "No se pudo guardar la portada.", Snackbar.LENGTH_LONG).show()
            return
        }

        val imageUri = withContext(Dispatchers.IO) { writeToGallery(bytes, file) }
        if (imageUri == null) {
            Snackbar.make(view, "No se pudo guardar la portada.", Snackbar.LENGTH_LONG).show()
            return
        }

        val snackbar = Snackbar.make(view, "Portada guardada en la galería", Snackbar.LENGTH_LONG)
        snackbar.setAction("ABRIR") {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivityOrToast(intent)
        }
        snackbar.show()
    }

    /** Returns the saved image's Uri, or null if anything went wrong (partial output is cleaned up). */
    private fun writeToGallery(bytes: ByteArray, file: File): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(bytes, file.name)
            } else {
                try {
                    FileOutputStream(file).use { it.write(bytes) }
                } catch (e: IOException) {
                    file.delete()
                    throw e
                }
                PortadasUtils.scanFile(context, file)
                FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    /** Downloads [imageUrl] and hands it off to the system share sheet. Tells the user if the download or the temporary file fails. */
    suspend fun share(imageUrl: String, fileName: String) {
        val bytes = context.loadImageBytes(imageUrl)
        val fileUri = bytes?.let { withContext(Dispatchers.IO) { saveToCache(fileName, it) } }
        if (fileUri == null) {
            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}")
            type = "image/jpeg"
        }
        context.startActivityOrToast(Intent.createChooser(intent, "Compartir portada"))
    }

    private fun saveToCache(fileName: String, bytes: ByteArray): Uri? {
        return try {
            val file = File(context.cacheDir, "$fileName.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file)
        } catch (e: IOException) {
            null
        }
    }

    private fun saveToMediaStore(bytes: ByteArray, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Portadas")
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val wrote = try {
            resolver.openOutputStream(imageUri)?.use { it.write(bytes); true }
        } catch (e: IOException) {
            null
        }
        if (wrote == true) return imageUri
        resolver.delete(imageUri, null, null)
        return null
    }
}
