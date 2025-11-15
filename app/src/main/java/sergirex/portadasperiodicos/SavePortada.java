package sergirex.portadasperiodicos;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static sergirex.portadasperiodicos.Portadas.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasperiodicos.Portadas.scanFile;

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

    private ProgressDialog pd;
    private File file;
    private final Context ctx;
    private SavePortada sp;
    private String fileName;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    DownloadPortada(Context ctx, SavePortada sp, String fileName) {
        this.ctx = ctx;
        this.sp = sp;
        this.fileName = fileName;
    }

    DownloadPortada(Context ctx, File file) {
        this.ctx = ctx;
        this.file = file;
    }

    public void execute(String... urlPortada) {
        onPreExecute();
        executor.execute(() -> {
            boolean success = doInBackground(urlPortada);
            handler.post(() -> onPostExecute(success));
        });
    }

    private void onPreExecute() {
        pd = new ProgressDialog(ctx);
        pd.setCancelable(true);
        pd.setMessage("Descargando...");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }

    private boolean doInBackground(String... urlPortada) {
        try {
            URL url = new URL(urlPortada[0]);
            InputStream is = (InputStream) url.getContent();
            Bitmap portadaBM = BitmapFactory.decodeStream(is);
            if (portadaBM == null) return false;

            if (file != null) { // Save to gallery
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveBitmapToMediaStore(ctx, portadaBM, file.getName());
                } else {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        portadaBM.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    }
                    scanFile(ctx, file, "image/jpeg");
                }
                return true;
            } else { // Share
                Uri fileURI = sp.saveFile(fileName, portadaBM);
                if (fileURI == null)
                    return false;
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_STREAM, fileURI);
                i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + ctx.getPackageName());
                i.setType("image/jpeg");
                handler.post(() -> ctx.startActivity(Intent.createChooser(i, "Compartir portada")));
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void onPostExecute(boolean success) {
        pd.cancel();
        pd.dismiss();
        if (file != null) { // Only show toast if we were saving.
            if (success) {
                String message = "Portada guardada correctamente en la galería.";
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && file != null) {
                    message = "Portada guardada correctamente en " + file.getAbsolutePath();
                }
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ctx, "No se pudo guardar la portada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveBitmapToMediaStore(Context context, Bitmap bitmap, String fileName) throws IOException {
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
    }

}