package sergirex.portadasperiodicos;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static sergirex.portadasperiodicos.Portadas.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasperiodicos.Portadas.scanFile;

/**
 * Created by Sergio on 20/06/2017.
 * Nothing else to add
 */

class SavePortada {
    private static final String LOG_TAG = "CREATE DIRECTORY";
    private Context context;
    static int permission = 0;

    SavePortada(Context context) {
        this.context = context;
    }

    boolean checkPermissions() {
        String WRITE_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        } else {
            WRITE_EXTERNAL_STORAGE = Manifest.permission.READ_MEDIA_IMAGES;
        }
        if (ContextCompat.checkSelfPermission(context,
                WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permission = 1;
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        }else {
            return true;
        }
        return false;
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
            File file = new File(path, fileName+".jpg");
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

class DownloadPortada extends AsyncTask<String, FileOutputStream, FileOutputStream> {

    private ProgressDialog pd;
    private File file;
    private Context ctx;
    private SavePortada sp;
    private String fileName;

    DownloadPortada(Context ctx, SavePortada sp, String fileName) {
        this.ctx = ctx;
        this.sp = sp;
        this.fileName = fileName;
    }

    DownloadPortada(Context ctx, File file) {
        this.ctx = ctx;
        this.file = file;
    }

    @Override
    protected void onPreExecute() {
        pd = new ProgressDialog(ctx);
        pd.setCancelable(true);
        pd.setMessage("Descargando...");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }

    @Override
    protected FileOutputStream doInBackground(String... urlPortada) {
        try {
            URL url = new URL(urlPortada[0]);
            InputStream is = (InputStream) url.getContent();
            Bitmap portadaBM = BitmapFactory.decodeStream(is);
            if (file != null) {
                FileOutputStream fos = new FileOutputStream(file);
                portadaBM.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                return fos;
            } else {
                Uri fileURI = sp.saveFile(fileName, portadaBM);
                if (fileURI == null)
                    return null;
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_STREAM, fileURI);
                i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + ctx.getPackageName());
                i.setType("image/jpeg");
                ctx.startActivity(Intent.createChooser(i, "Compartir portada"));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(FileOutputStream fos) {
        if (fos != null) {
            try {
                fos.close();
                scanFile(ctx, file, "images/jpeg");
                Toast.makeText(ctx, "Portada guardada correctamente en " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pd.cancel();
        pd.dismiss();
    }
}
