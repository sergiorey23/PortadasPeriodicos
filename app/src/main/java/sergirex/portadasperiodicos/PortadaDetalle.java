package sergirex.portadasperiodicos;

import static sergirex.portadasperiodicos.Portadas.MY_PERMISSIONS_REQUEST_WRITE_STORAGE;
import static sergirex.portadasperiodicos.SavePortada.permission;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PortadaDetalle extends AppCompatActivity {

    private String initialPortada;
    private Portada portada;
    private SharedPreferences prefsPer;
    private AlertDialog alertDialogNoConn;
    private InterstitialAd interstitialAd;
    private Long today;
    private ViewPager2 mViewPager2;
    private ViewPagerAdapter mSectionsPagerAdapter;
    private boolean clicked = false;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portada_detalle);

        initialPortada = getIntent().getStringExtra("selectedPortada");
        int showAd = getIntent().getIntExtra("showAd",0);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        prefsPer = getSharedPreferences("periodicos", Context.MODE_PRIVATE);

        Animation rotateOpen = AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim);
        Animation rotateClose = AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim);
        Animation fromBottom = AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim);
        Animation toBottom = AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim);

        FloatingActionButton fabWeb = findViewById(R.id.httpButton);
        fabWeb.setOnClickListener(v -> {
            try {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.color.colorPrimary, typedValue, true);
                int color = typedValue.data;
                String url = "https://www." + portada.webPeriodico();
                CustomTabColorSchemeParams customTabColorSchemeParams = new CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(color).build();
                CustomTabsIntent intent = new CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .setDefaultColorSchemeParams(customTabColorSchemeParams)
                        .build();
                intent.launchUrl(PortadaDetalle.this, Uri.parse(url));
            }catch (ActivityNotFoundException e){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www." + portada.webPeriodico()));
                startActivity(intent);
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        FloatingActionButton favfab = findViewById(R.id.favButton);
        favfab.setOnDragListener((view, dragEvent) -> false);
        favfab.setOnClickListener(view -> {
            SharedPreferences.Editor editor = prefsPer.edit();
            if (prefsPer.contains(portada.webPeriodico())) {
                editor.remove(portada.webPeriodico());
                favfab.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            } else {
                editor.putString(portada.webPeriodico(), portada.periodico());
                favfab.setImageResource(R.drawable.ic_favorite_black_24dp);
            }
            editor.apply();
        });

        FloatingActionButton addFab = findViewById(R.id.addFab);
        addFab.setOnClickListener(v -> {
            if(!clicked){
                fabWeb.setVisibility(View.VISIBLE);
                favfab.setVisibility(View.VISIBLE);
                fabWeb.startAnimation(fromBottom);
                favfab.startAnimation(fromBottom);
                addFab.startAnimation(rotateOpen);
                fabWeb.setClickable(true);
                favfab.setClickable(true);
            }else{
                fabWeb.setVisibility(View.INVISIBLE);
                favfab.setVisibility(View.INVISIBLE);
                fabWeb.startAnimation(toBottom);
                favfab.startAnimation(toBottom);
                addFab.startAnimation(rotateClose);
                fabWeb.setClickable(false);
                favfab.setClickable(false);
            }
            clicked = !clicked;
        });

        loadSectionsAdapter();


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int rate = prefs.getInt("rate",0);
        if(rate == 0 && showAd%2==0) {
            SharedPreferences.Editor editor = prefs.edit();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.rateTitle)
                    .setMessage(R.string.rateDescription)
                    .setIcon(R.mipmap.news_icon)
                    .setNeutralButton("No", (dialog, which) -> {
                        editor.putInt("rate", 1);
                        editor.apply();
                    }).setNegativeButton(R.string.later, (dialog, which) -> {
                        editor.putInt("rate", 2);
                        editor.apply();
                    }).setPositiveButton(R.string.sure, (dialog, which) -> {
                        editor.putInt("rate", 1);
                        editor.apply();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ReviewManager manager = ReviewManagerFactory.create(this);
                            Task<ReviewInfo> request = manager.requestReviewFlow();
                            request.addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // We can get the ReviewInfo object
                                    ReviewInfo reviewInfo = task.getResult();
                                    Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                                    flow.addOnCompleteListener(t -> {
                                        Toast.makeText(this, "¡Gracias!", Toast.LENGTH_LONG).show();
                                    });
                                } else {
                                    // There was some problem, log or handle the error code.
                                    Log.e("Review Error", task.getException().getMessage());
                                }
                            });
                        }else {
                            Uri uri = Uri.parse("market://details?id=" + getPackageName());
                            Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(myAppLinkToMarket);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
                            }
                        }
                    }).show();
        }
        if(!prefs.getBoolean("remove_fb_ads", false)) {
            AudienceNetworkAds.initialize(this);
            //AdSettings.setTestMode(true);
            AdView bottomBanner = new AdView(this, "799967435028134_814221240269420", AdSize.BANNER_HEIGHT_50);
            // Find the Ad Container
            LinearLayout adContainer = findViewById(R.id.bannerContainerDetail);
            adContainer.addView(bottomBanner);
            bottomBanner.loadAd();
            if (showAd%3==0) {
                interstitialAd = new InterstitialAd(this, "799967435028134_801174748240736");
                String TAG = "AD";
                InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {

                    @Override
                    public void onInterstitialDisplayed(Ad ad) {
                        // Interstitial ad displayed callback
                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        // Interstitial dismissed callback
                    }

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        // Ad error callback
                        Log.e(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
                    }

                    @Override
                    public void onAdLoaded(Ad ad) {
                        // Interstitial ad is loaded and ready to be displayed
                        Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");
                        // Show the ad
                        interstitialAd.show();
                    }

                    @Override
                    public void onAdClicked(Ad ad) {
                        // Ad clicked callback
                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {
                        // Ad impression logged callback
                    }
                };

                interstitialAd.loadAd(interstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());
            }
        }
    }

    private void loadSectionsAdapter() {
        if (!isOnline()) {
            if (alertDialogNoConn == null)
                alertDialogNoConn = createNoConnectionDialog();
            alertDialogNoConn.show();
        }else {
            mViewPager2 = findViewById(R.id.viewpager2);
            String[] portadas = getIntent().getStringArrayExtra("Portadas");
            String fecha = getIntent().getStringExtra("Fecha");
            mSectionsPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
            int pos = 0;
            if (portadas != null) {
                for (int i = 0; i < portadas.length; i++) {
                    String[] periodicoArray = portadas[i].split(":");
                    String siglaPais = "es";
                    String title, webPeriodico;
                    if (periodicoArray.length > 1) {
                        title = periodicoArray[0];
                        webPeriodico = periodicoArray[1];
                        if (periodicoArray.length == 3) {
                            siglaPais = periodicoArray[2];
                        }
                    } else {
                        title = portadas[i].split("\\.")[0];
                        webPeriodico = portadas[i];
                    }
                    if (title.equals(initialPortada)){
                        pos = i;
                    }
                    mSectionsPagerAdapter.addFragment(PortadaDetalleFragment.newInstance(title, siglaPais, fecha));
                    Portada p = new Portada(portadas[i],title,fecha,webPeriodico,siglaPais);
                    mSectionsPagerAdapter.addPortada(p);
                }
            }

            mViewPager2.setAdapter(mSectionsPagerAdapter);
            mViewPager2.setCurrentItem(pos,false);
        }
    }

    public static class ViewPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<Portada> mPortadas = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        public void addFragment(Fragment fragment) {
            mFragments.add(fragment);
        }

        void addFragmentAt(int position, Fragment fragment) {
            mFragments.add(position, fragment);
        }

        public void addPortada(Portada p) {
            mPortadas.add(p);
        }

        public void removeFragment(int position){
            mFragments.remove(position);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemCount() {
            return mFragments.size();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public AlertDialog createNoConnectionDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(" Error de conexión");
        alertDialogBuilder.setIcon(R.mipmap.news_icon);
        alertDialogBuilder.setPositiveButton("Reintentar", (dialog, id) -> {
            if(isOnline())
                loadSectionsAdapter();
            else
                alertDialogBuilder.show();
        })
                .setNegativeButton("Cencelar", (dialog, id) -> alertDialogNoConn.dismiss());
        alertDialogBuilder.setMessage("No hay conexión a internet. Por favor, comprueba tu conexión");
        return alertDialogBuilder.create();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permission == 1) {
                    Toast.makeText(this, "Permiso concedido. Vuelva a relizar la operacíon", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Para poder compartir y guardar fotos, es necesario otorgar permisos de almacenamiento", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_portada,
                menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int position = mViewPager2.getCurrentItem();
        portada = mSectionsPagerAdapter.mPortadas.get(position);

        String url = "https://kiosko.net/" + portada.fecha() + "/" + portada.siglaPais() + "/" + portada.title() + ".html";

        SavePortada savePortada = new SavePortada(this);
        if(item.getItemId() == R.id.share){
            if (savePortada.isExternalStorageWritable()) {
                if (savePortada.checkPermissions()) {
                    new DownloadPortada(this, savePortada, portada.title()).execute(url);
                }
            }
        } else if (item.getItemId() == R.id.save) {
            if (savePortada.isExternalStorageWritable()) {
                if (savePortada.checkPermissions()) {
                    File file;
                    if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + portada.title() + "_" + (portada.fecha() != null ? portada.fecha().replace("/", "") : "") + ".jpg")).exists()) {
                        Toast.makeText(this, "Ya se ha guardado la portada.", Toast.LENGTH_LONG).show();
                        return true;
                    }
                    DownloadPortada dp = new DownloadPortada(this, file);
                    dp.execute(url);
                }
            } else {
                Toast.makeText(this, "Internal Storage unreadable", Toast.LENGTH_LONG).show();
            }
        }else if(item.getItemId() == R.id.date){
            if (today == null) today = MaterialDatePicker.todayInUtcMilliseconds();
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                    .datePicker()
                    .setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(
                                    DateValidatorPointBackward.now()).build())
                    .setTitleText("Select date").setSelection(today)
                    .build();
            datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
            datePicker.addOnPositiveButtonClickListener(
                    aLong -> {
                        //datePicker.getHeaderText()
                        today = aLong;
                        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
                        String fecha = formatter.format(new Date(aLong));
                        mSectionsPagerAdapter.removeFragment(position);
                        mSectionsPagerAdapter.addFragmentAt(position, PortadaDetalleFragment.newInstance(portada.title(), portada.siglaPais(), fecha));
                        mSectionsPagerAdapter.notifyItemChanged(position);
                    });
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        int position = mViewPager2.getCurrentItem();
        if(mSectionsPagerAdapter.mPortadas.size() > 0) {
            portada = mSectionsPagerAdapter.mPortadas.get(position);
            if (portada != null && getSupportActionBar() != null)
                getSupportActionBar().setTitle(portada.title());
        }
    }

    @Override
    protected void onDestroy() {
        if (interstitialAd != null) {
            interstitialAd.destroy();
        }
        super.onDestroy();
    }
}
