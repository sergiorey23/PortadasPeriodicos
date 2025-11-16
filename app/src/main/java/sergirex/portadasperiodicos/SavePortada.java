package sergirex.portadasperiodicos;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static sergirex.portadasperiodicos.PortadasUtils.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasperiodicos.PortadasUtils.scanFile;

import com.google.android.material.snackbar.Snackbar;

/**
 * Created by Sergio on 20/06/2017.
 * Nothing else to add
 */

class SavePortada {
    private static final String LOG_TAG = "CREATE DIRECTORY";
    private final Context context;
    static int permission = 0;

    SavePortada(Context context) {
        this.context = context;
    }

    boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permission = 1;
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            return false;
        } else {
            return true;
        }
    }


    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Portadas");
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    Bitmap getThumbFile(String title) {
        File file = new File(context.getCacheDir() + File.separator + title + ".jpg");
        if (file.exists())
            return BitmapFactory.decodeFile(file.getPath());
        else
            return null;
    }

    void saveFile(File path, String fileName, Bitmap pic) {
        try {
            File file = new File(path, fileName + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            pic.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Uri saveFile(String fileName, Bitmap pic) {
        try {
            File file = new File(context.getCacheDir(), fileName + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            pic.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

class DownloadPortada {

    // CHANGED: Removed the ProgressDialog.
    // We now use a weak reference to the context and view to prevent memory leaks.
    private final WeakReference<Context> contextRef;
    private final WeakReference<View> viewRef; // NEW: Needed for the Snackbar

    private final File file; // Used when saving to gallery
    private final SavePortada sp; // Used when sharing
    private final String fileName; // Used when sharing

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // NEW: Updated constructor for saving to gallery
    DownloadPortada(Context context, View view, File file) {
        this.contextRef = new WeakReference<>(context);
        this.viewRef = new WeakReference<>(view);
        this.file = file;
        this.sp = null;
        this.fileName = null;
    }

    // NEW: Updated constructor for sharing
    DownloadPortada(Context context, View view, SavePortada sp, String fileName) {
        this.contextRef = new WeakReference<>(context);
        this.viewRef = new WeakReference<>(view);
        this.sp = sp;
        this.fileName = fileName;
        this.file = null;
    }


    public void execute(String... urlPortada) {
        onPreExecute();
        executor.execute(() -> {
            // The result now contains the Uri of the saved file if successful
            DownloadResult result = doInBackground(urlPortada);
            handler.post(() -> onPostExecute(result));
        });
    }

    private void onPreExecute() {
        Context context = contextRef.get();
        if (context == null) return;

        // CHANGED: Show a simple, non-blocking Toast instead of a ProgressDialog.
        if (file != null) { // Only show toast if we are saving
            Toast.makeText(context, "Iniciando descarga...", Toast.LENGTH_SHORT).show();
        }
    }

    private DownloadResult doInBackground(String... urlPortada) {
        Context context = contextRef.get();
        if (context == null) {
            return new DownloadResult(false, null); // Abort if context is gone
        }

        try {
            URL url = new URL(urlPortada[0]);
            InputStream is = (InputStream) url.getContent();
            Bitmap portadaBM = BitmapFactory.decodeStream(is);
            if (portadaBM == null) return new DownloadResult(false, null);

            if (file != null) { // Save to gallery
                Uri imageUri = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageUri = saveBitmapToMediaStore(context, portadaBM, file.getName());
                } else {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        portadaBM.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    }
                    scanFile(context, file);
                    // For older devices, get a content URI for the Snackbar action
                    imageUri = FileProvider.getUriForFile(context, "sergirex.portadasperiodicos.fileprovider", file);
                }
                return new DownloadResult(true, imageUri);
            } else { // Share
                Uri fileURI = sp.saveFile(fileName, portadaBM);
                if (fileURI == null) {
                    return new DownloadResult(false, null);
                }
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_STREAM, fileURI);
                i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + context.getPackageName());
                i.setType("image/jpeg");
                // Post the intent action back to the main thread
                handler.post(() -> context.startActivity(Intent.createChooser(i, "Compartir portada")));
                return new DownloadResult(true, null); // Success, but no URI needed for Snackbar
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new DownloadResult(false, null);
        }
    }

    private void onPostExecute(DownloadResult result) {
        Context context = contextRef.get();
        View view = viewRef.get();

        // Don't show any UI feedback if the view or context is gone
        if (context == null || view == null || file == null) {
            return;
        }

        if (result.success()) {
            String message = "Portada guardada en la galería";
            Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

            // NEW: Add an "Open" action to the Snackbar if we have a valid URI
            if (result.imageUri() != null) {
                snackbar.setAction("ABRIR", v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(result.imageUri(), "image/jpeg");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "No se encontró una app para abrir la imagen.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            snackbar.show();
        } else {
            Snackbar.make(view, "No se pudo guardar la portada.", Snackbar.LENGTH_LONG).show();
        }
    }

    private Uri saveBitmapToMediaStore(Context context, Bitmap bitmap, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Portadas");
        }

        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (imageUri == null) {
            throw new IOException("Failed to create new MediaStore record.");
        }

        try (OutputStream fos = resolver.openOutputStream(imageUri)) {
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } else {
                throw new IOException("Failed to open output stream for " + imageUri);
            }
        }
        return imageUri;
    }

    // NEW: A simple helper class to return multiple values from doInBackground
        private record DownloadResult(boolean success, Uri imageUri) {
    }
}