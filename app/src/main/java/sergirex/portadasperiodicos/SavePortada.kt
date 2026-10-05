package sergirex.portadasperiodicos

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves or shares a newspaper cover. Fetching the image now goes through Coil
 * (`fetchBitmap`) instead of the old hand-rolled `URL.getContent()` +
 * `BitmapFactory.decodeStream`, the same pipeline every other screen uses.
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

        val bitmap = fetchBitmap(imageUrl)
        if (bitmap == null) {
            Snackbar.make(view, "No se pudo guardar la portada.", Snackbar.LENGTH_LONG).show()
            return
        }

        val imageUri = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveBitmapToMediaStore(bitmap, file.name)
            } else {
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                PortadasUtils.scanFile(context, file)
                FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file)
            }
        }

        val snackbar = Snackbar.make(view, "Portada guardada en la galería", Snackbar.LENGTH_LONG)
        imageUri?.let { uri ->
            snackbar.setAction("ABRIR") {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/jpeg")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No se encontró una app para abrir la imagen.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        snackbar.show()
    }

    /** Downloads [imageUrl] and hands it off to the system share sheet. Fails silently if the download fails, matching the original behavior. */
    suspend fun share(imageUrl: String, fileName: String) {
        val bitmap = fetchBitmap(imageUrl) ?: return
        val fileUri = withContext(Dispatchers.IO) { saveBitmapToCache(fileName, bitmap) } ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${context.packageName}")
            type = "image/jpeg"
        }
        context.startActivity(Intent.createChooser(intent, "Compartir portada"))
    }

    private suspend fun fetchBitmap(url: String): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).build()
        val result = context.imageLoader.execute(request)
        return ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
    }

    private fun saveBitmapToCache(fileName: String, bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "$fileName.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file)
        } catch (e: IOException) {
            null
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Portadas")
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val wrote = resolver.openOutputStream(imageUri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return if (wrote == true) imageUri else null
    }
}
