package sergirex.portadasperiodicos;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.Objects;

import coil.ImageLoader;
import coil.request.ImageRequest;

import static sergirex.portadasperiodicos.GetPortadas.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasperiodicos.Portadas.scanFile;
import static sergirex.portadasperiodicos.SavePortada.permission;

//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;

public class PortadaDetalle extends AppCompatActivity {
    private Portada portada;
    private boolean dark = false;
    private SharedPreferences prefs;
    private SharedPreferences prefsPer;
    private AlertDialog alertDialogNoConn;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("switch_preference", false)) {
            dark = true;
            setTheme(R.style.Theme_AppCompat_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portada_detalle);

        portada = (Portada) getIntent().getSerializableExtra("Portada");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        String title = portada.getTitle().replace("_", " ");
        title = title.substring(0, 1).toUpperCase() + title.substring(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        imageView = findViewById(R.id.imagen_extendida);

        loadContent();

        final FloatingActionButton fabfav = findViewById(R.id.favButton);
        prefsPer = getSharedPreferences("periodicos", Context.MODE_PRIVATE);
        if (prefsPer.contains(portada.getWebPeriodico())) {
            fabfav.setImageResource(R.drawable.ic_favorite_black_24dp);
        }
        fabfav.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefsPer.edit();
            if (prefsPer.contains(portada.getWebPeriodico())) {
                editor.remove(portada.getWebPeriodico());
                fabfav.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            } else {
                editor.putString(portada.getWebPeriodico(), portada.getPeriodico());
                fabfav.setImageResource(R.drawable.ic_favorite_black_24dp);
            }
            editor.apply();
        });
    }

    void loadContent() {
        if (!isOnline()) {
            if (alertDialogNoConn == null)
                alertDialogNoConn = createNoConnectionDialog();
            alertDialogNoConn.show();
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setCancelable(false);
            pd.show();
            pd.setIndeterminate(true);
            ImageLoader imageLoader = new ImageLoader.Builder(this)
                    .availableMemoryPercentage(0.25)
                    .crossfade(true)
                    .build();

            ImageRequest request = new ImageRequest.Builder(this)
                    .data(portada.getUrlPortada())
                    .crossfade(true)
                    .target(imageView)
                    .build();
            imageLoader.enqueue(request);
            pd.cancel();
            pd.dismiss();
            /*Picasso.get().load(portada.getUrlPortada())
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            pd.cancel();
                            pd.dismiss();
                        }

                        @Override
                        public void onError(Exception e) {
                            pd.cancel();
                            pd.dismiss();
                            Picasso.get().load(portada.getUrlPortada())
                                    .into(imageView);
                        }
                    });*/
        }

        /*AdView mBottomBanner = findViewById(R.id.av_bottom_banner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mBottomBanner.loadAd(adRequest);
        savePortada = new SavePortada(this);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.getBoolean("switch_preference", false) && !dark) {
            dark = true;
            recreate();
        }
    }

    public void onClickWebBtn(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www." + portada.getWebPeriodico()));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_portada, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SavePortada savePortada = new SavePortada(this);
        switch (item.getItemId()) {
            case R.id.share:
                File cacheFile = savePortada.saveFile(getCacheDir(), portada.getTitle(), ((BitmapDrawable) imageView.getDrawable()).getBitmap());
                Uri fileURI = FileProvider.getUriForFile(this, "sergirex.portadasperiodicos.fileprovider", cacheFile);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.putExtra(Intent.EXTRA_STREAM, fileURI);
                i.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + getPackageName());
                i.setType("image/png");
                startActivity(Intent.createChooser(i, "Compartir portada"));
                cacheFile.delete();
                return true;
            case R.id.save:
                if (savePortada.isExternalStorageWritable()) {
                    if (!savePortada.checkPermissions())
                        return false;
                    File file;
                    if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + portada.getTitle() + ".png")).exists()) {
                        Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Ya se ha guardado la portada", Snackbar.LENGTH_SHORT);
                        sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                        sb.show();
                        return true;
                    }
                    savePortada.saveFile(savePortada.getAlbumStorageDir(), portada.getTitle(), ((BitmapDrawable) imageView.getDrawable()).getBitmap());

                    scanFile(this, file, "images/png");
                    Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Portada guarda correctamente en " + file.getAbsolutePath(), Snackbar.LENGTH_SHORT);
                    sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                    sb.show();
                } else {
                    Toast.makeText(this, "Internal Storage unreadable", Toast.LENGTH_LONG).show();
                }
                return true;
        }

        return false;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public AlertDialog createNoConnectionDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Error de conexión");
        alertDialogBuilder.setIcon(R.mipmap.news_icon);
        alertDialogBuilder.setOnCancelListener(dialogInterface -> onBackPressed());
        alertDialogBuilder.setPositiveButton("Reintentar", (dialog, id) -> {
            if (isOnline())
                loadContent();
            else
                alertDialogBuilder.show();
        })
                .setNegativeButton("Cancelar", (dialog, id) -> onBackPressed());
        alertDialogBuilder.setMessage("No hay conexión a internet. Por favor, comprueba tu conexión");
        return alertDialogBuilder.create();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permission == 1) {
                    Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Permiso concedido. Vuelva a relizar la operacíon", Snackbar.LENGTH_LONG);
                    sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                    sb.show();
                }
            } else {
                Snackbar sb = Snackbar.make(findViewById(R.id.imagen_extendida), "Para poder compartir y guardar fotos, es necesario otorgar permisos de almacenamiento", Snackbar.LENGTH_LONG);
                sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                sb.show();
            }
        }
    }
}