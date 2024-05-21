package sergirex.portadasperiodicos;

import static sergirex.portadasperiodicos.SavePortada.permission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Portadas extends AppCompatActivity {
    static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;
    static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATION = 1;
    private static final String TAG = "InAppPurchaseTag";
    private static final String CHANNEL_ID = "26081995";
    private DrawerLayout mDrawerLayout;
    private ViewPager mViewPager;
    private AlertDialog alertDialog;
    private AlertDialog alertDialogNoConn;
    private AdView bottomBanner;
    private TabLayout tabLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private SharedPreferences prefs;
    private SharedPreferences prefsPor;
    private BillingClient billingClient;
    private Handler handler;
    private List <ProductDetails> productDetailsList;
    private int favsCount = 0;
    private boolean dark = false;
    private boolean descargar= false;
    private Long today;
    private String fecha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme","default");
        switch (theme) {
            case "default":
                switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        setTheme(R.style.AppThemeDark);
                        dark = true;
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                        setTheme(R.style.AppTheme);
                        break;
                }
                break;
            case "light":
                setTheme(R.style.AppTheme);
                break;
            case "dark":
                setTheme(R.style.AppThemeDark);
                dark = true;
        }
        super.onCreate(savedInstanceState);

        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if(calendar.get(Calendar.HOUR_OF_DAY) < 4){
            calendar.add(Calendar.DATE, -1);
        }
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        fecha = formatter.format(calendar.getTime());
        String fechaPortadas = getSharedPreferences("FechasGeneral", Context.MODE_PRIVATE).getString("fechaPortadas", null);
        descargar = fechaPortadas == null || !fechaPortadas.equals(fecha);

        setContentView(R.layout.activity_portadas);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawerLayout);
        NavigationView mNavigationView = findViewById(R.id.navView);
        if(prefs.getBoolean("remove_fb_ads", false)){
            Menu nav_Menu = mNavigationView.getMenu();
            nav_Menu.findItem(R.id.nav_remove_ads).setVisible(false);
        }

        tabLayout = findViewById(R.id.tabs);
        prefsPor = getSharedPreferences("periodicos", Context.MODE_PRIVATE);

        int rateDialog = prefs.getInt("rate",0);
        if(rateDialog == 2){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("rate",0);
            editor.apply();
        }

        if (descargar && !isOnline()) {
            if (alertDialogNoConn == null)
                alertDialogNoConn = createNoConnectionDialog();
            alertDialogNoConn.show();
        }else{
            loadSectionsAdapter();
        }


        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            mDrawerLayout.closeDrawers();
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_help)
                showDialog().show();
            else if(itemId == R.id.nav_remove_ads){
                handler = new Handler();
                productDetailsList = new ArrayList<>();
                removeFBAds();
            }
            else if (itemId == R.id.nav_rate)
                launchMarket();
            else if (itemId == R.id.nav_share) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String sAux = "Descarga la app de Portadas gratis!\n\nhttps://play.google.com/store/apps/details?id=" + getPackageName();
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, null));
            } else if (itemId == R.id.nav_about) {
                showAboutInfo();
            }else if (itemId == R.id.nav_settings){
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
            return true;
        });

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name,
                R.string.app_name);

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerToggle.syncState();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT > 33) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        MY_PERMISSIONS_REQUEST_POST_NOTIFICATION);
            }
        }


        if(!prefs.getBoolean("remove_fb_ads", false)) {
            AudienceNetworkAds.initialize(this);
            //AdSettings.setTestMode(true);
            // Instantiate an AdView object.
            // NOTE: The placement ID from the Facebook Monetization Manager identifies your App.
            // To get test ads, add IMG_16_9_APP_INSTALL# to your placement id. Remove this when your app is ready to serve real ads.
            boolean isPhone = getResources().getBoolean(R.bool.isPhone);
            if (isPhone) {
                bottomBanner = new AdView(this, "799967435028134_799969321694612", AdSize.BANNER_HEIGHT_50);
            } else {
                bottomBanner = new AdView(this, "799967435028134_799969321694612", AdSize.BANNER_HEIGHT_90);
            }

            // Find the Ad Container
            LinearLayout adContainer = findViewById(R.id.bannerContainer);
            adContainer.addView(bottomBanner);
            bottomBanner.loadAd();
        }
    }


    public void startAlarmBroadcastReceiver(Context context) {
        Intent _intent = new Intent(context, AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, _intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    void removeFBAds(){
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(
                        (billingResult, list) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    handlePurchase(purchase);
                                }
                            }
                        }
                ).build();

        //start the connection after initializing the billing client
        establishConnection();
    }

    void establishConnection() {

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    showProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                establishConnection();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    void showProducts() {

        ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                //Product 1
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("remove_ads_id")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(
                params,
                (billingResult, prodDetailsList) -> {
                    // Process the result
                    productDetailsList.clear();
                    handler.postDelayed(() -> {
                        productDetailsList.addAll(prodDetailsList);
                        if (!productDetailsList.isEmpty()) {
                            productDetailsList.addAll(prodDetailsList);
                            Log.d(TAG, productDetailsList.size() + " number of products");
                            String price = Objects.requireNonNull(productDetailsList.get(0).getOneTimePurchaseOfferDetails()).getFormattedPrice();
                            String productName = productDetailsList.get(0).getName();
                            new MaterialAlertDialogBuilder(this,R.style.Theme_MyApp_Dialog_Alert)
                                    .setTitle(productName)
                                    .setMessage("Deshazte de la publicidad por "+price+" de por vida")
                                    .setIcon(R.mipmap.news_icon)
                                    .setNegativeButton("Cancelar", null)
                                    .setNeutralButton("Restaurar compra",(dialog, which) -> {
                                        restorePurchases();

                                    })
                                    .setPositiveButton("Comprar", (dialog, which) -> {
                                        launchPurchaseFlow(productDetailsList.get(0));
                                    }).show();
                        }else{
                            Toast.makeText(this, "No products available", Toast.LENGTH_LONG).show();
                        }
                    }, 1000);

                }
        );

    }

    void launchPurchaseFlow(ProductDetails productDetails) {
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    void handlePurchase(Purchase purchases) {

        if(!purchases.isAcknowledged()){
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.getPurchaseToken())
                    .build(), billingResult -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("remove_fb_ads",true);
                    editor.apply();
                    bottomBanner.removeAllViews();
                }
            });
            Log.d(TAG, "Purchase Token: " + purchases.getPurchaseToken());
            Log.d(TAG, "Purchase Time: " + purchases.getPurchaseTime());
            Log.d(TAG, "Purchase OrderID: " + purchases.getOrderId());
        }
    }
    void restorePurchases() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                establishConnection();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {
                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Snackbar sb;
                                    if (!list.isEmpty()) {
                                        //prefs.setIsRemoveAd(true); // set true to activate remove ad feature
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean("remove_fb_ads",true);
                                        editor.apply();
                                        sb = Snackbar.make(mDrawerLayout, "Successfully restored", Snackbar.LENGTH_SHORT);
                                        bottomBanner.removeAllViews();
                                    } else {
                                        Log.d(TAG, "Oops, No purchase found.");
                                        sb = Snackbar.make(mDrawerLayout, "No purchase found", Snackbar.LENGTH_SHORT);
                                        //prefs.setIsRemoveAd(false); // set false to de-activate remove ad feature
                                    }
                                    sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                                    sb.show();
                                }
                            });
                }
            }
        });
    }
    void loadSectionsAdapter(){
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount()-1);
        tabLayout.setupWithViewPager(mViewPager);
        String lpValue = prefs.getString("init_category", getString(R.string.first_tab));
        if(prefsPor.getAll().isEmpty()) {
            switch (lpValue.substring(0,3)) {
                case "Dep":
                case "Spo":
                    mViewPager.setCurrentItem(1);
                    break;
                case "Eco":
                    mViewPager.setCurrentItem(2);
                    break;
                case "Loc":
                    mViewPager.setCurrentItem(3);
                    break;
                case "Int":
                    mViewPager.setCurrentItem(4);
                    break;
                default: mViewPager.setCurrentItem(0);
            }
        }else{
            switch (lpValue.substring(0,3)) {
                case "Fav":
                    mViewPager.setCurrentItem(0);
                    break;
                case "Dep":
                case "Spo":
                    mViewPager.setCurrentItem(2);
                    break;
                case "Eco":
                    mViewPager.setCurrentItem(3);
                    break;
                case "Loc":
                    mViewPager.setCurrentItem(4);
                    break;
                case "Int":
                    mViewPager.setCurrentItem(5);
                    break;
                default: mViewPager.setCurrentItem(1);
            }
        }
    }

    void showAboutInfo(){
        View aboutLayout = getLayoutInflater().inflate(R.layout.about, mDrawerLayout ,false);
        int theme = dark ? R.style.AppThemeDark : R.style.AppTheme;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, theme);
        builder.setPositiveButton(R.string.close, null);
        builder.setView(aboutLayout);
        TextView appSource = aboutLayout.findViewById(R.id.appSource);
        TextView tv = aboutLayout.findViewById(R.id.appVersion);

        tv.setText("v".concat(BuildConfig.VERSION_NAME));
        appSource.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.lasportadas.es/"));
            startActivity(intent);
        });

        TextView privacy = aboutLayout.findViewById(R.id.privacy_policy);
        privacy.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://www.lasportadas.es/privacy.php");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });
//        Button paypal = aboutLayout.findViewById(R.id.paypal);
//        paypal.setOnClickListener(view -> {
//            Uri uri = Uri.parse("https://www.paypal.com/donate?hosted_button_id=4H8LT2PVZEE78");
//            Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
//            browserIntent.setPackage("com.instagram.android.p2pmobile");
//
//            try {
//                startActivity(browserIntent);
//            } catch (ActivityNotFoundException e) {
//                startActivity(new Intent(Intent.ACTION_VIEW, uri));
//            }
//        });

        Button button2 = aboutLayout.findViewById(R.id.contact);
        button2.setOnClickListener(view -> {
            Uri uri = Uri.parse("mailto:sssergiooo23@gmail.com");
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
            emailIntent.setData(uri);
            startActivity(emailIntent);
        });
        builder.create();
        builder.show();
    }

    private AlertDialog showDialog() {
        if (alertDialog == null) {
            int theme = dark ? R.style.AppThemeDark : R.style.AppTheme;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, theme);
            alertDialogBuilder.setTitle(R.string.help);
            alertDialogBuilder.setIcon(R.mipmap.news_icon);
            alertDialogBuilder.setPositiveButton("OK", null);
            alertDialog = alertDialogBuilder.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                alertDialog.setMessage(Html.fromHtml("<Big>" + getString(R.string.help_description) + "<br/><br/>"+ getString(R.string.help_description2) + "</Big>" + "<br/><br/>" + getString(R.string.rate_app), Html.FROM_HTML_MODE_LEGACY));
            else
                alertDialog.setMessage(Html.fromHtml("<Big>" + getString(R.string.help_description) + "<br/><br/>"+ getString(R.string.help_description2) + "</Big>" + "<br/><br/>" + getString(R.string.rate_app)));
        }
        return alertDialog;
    }

    private void launchMarket() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private final List<String> categorias = new ArrayList<>();
        private final List<Fragment> mFragments = new ArrayList<>();

        void addFragment(String title, Fragment fragment) {
            categorias.add(0, title);
            mFragments.add(0, fragment);
        }

        void removeFragment() {
            categorias.remove(0);
            mFragments.remove(0);
            notifyDataSetChanged();
        }

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            if(!prefsPor.getAll().isEmpty()) {
                categorias.add(getString(R.string.fav_tab));
                mFragments.add(Favoritos.newInstance(fecha));
                favsCount = prefsPor.getAll().size();
            }
            categorias.add(getString(R.string.first_tab));
            mFragments.add(General.newInstance(fecha));
            categorias.add(getString(R.string.second_tab));
            mFragments.add(Deportes.newInstance(fecha));
            categorias.add(getString(R.string.third_tab));
            mFragments.add(Economia.newInstance(fecha));
            categorias.add(getString(R.string.fourth_tab));
            mFragments.add(Locales.newInstance(fecha));
            categorias.add(getString(R.string.fifth_tab));
            mFragments.add(Internacional.newInstance(fecha));
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return categorias.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return categorias.get(position);
        }


        @Override
        public int getItemPosition(@NonNull Object object) {
            if(object instanceof Favoritos)
                return POSITION_NONE;
            return mFragments.indexOf(object);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        /*MenuItem shareItem = menu.findItem(R.id.date);

        if (dark) {
            shareItem.setVisible(false);
        }*/

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.refresh) {
            if (descargar && !isOnline()) {
                if (alertDialogNoConn == null)
                    alertDialogNoConn = createNoConnectionDialog();
                alertDialogNoConn.show();
                return false;
            }
            loadSectionsAdapter();
        } else if(menuItem.getItemId() == R.id.fav){
            if(!prefsPor.getAll().isEmpty()) {
                mViewPager.setCurrentItem(0);
            }else{
                Snackbar sb = Snackbar.make(mDrawerLayout, "No tienes ninguna portada favorita", Snackbar.LENGTH_SHORT);
                sb.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
                sb.show();
            }
            return true;
        } else if(menuItem.getItemId() == R.id.date) {
            if (today == null) today = MaterialDatePicker.todayInUtcMilliseconds();
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                    .datePicker()
                    .setTitleText("Select date").setSelection(today)
                    .build();
            datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
            datePicker.addOnPositiveButtonClickListener(
                    aLong -> {
                        //datePicker.getHeaderText()
                        if (aLong > new Date().getTime()) {
                            Toast.makeText(this, "La fecha debe ser anterior a la actual", Toast.LENGTH_LONG).show();
                            return;
                        }
                        today = aLong;
                        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                        this.fecha = formatter.format(aLong);
                        ;
                        loadSectionsAdapter();
                    });
            return true;
        }
        Fragment fragment = mSectionsPagerAdapter.getItem(tabLayout.getSelectedTabPosition());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.detach(fragment).attach(fragment).commit();
        return true;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static void scanFile(Context ctxt, File f, String mimeType) {
        MediaScannerConnection
                .scanFile(ctxt, new String[] {f.getAbsolutePath()},
                        new String[] {mimeType}, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(f);
            scanIntent.setData(contentUri);
            ctxt.sendBroadcast(scanIntent);
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            ctxt.sendBroadcast(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
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
        } else if (requestCode == MY_PERMISSIONS_REQUEST_POST_NOTIFICATION){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAlarmBroadcastReceiver(getApplicationContext());
            }
            requestWritePermission();
        }
    }

    public void requestWritePermission(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            }
        }else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
            }
        }
    }

    public AlertDialog createNoConnectionDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(" Error de conexión");
        alertDialogBuilder.setIcon(R.mipmap.news_icon);
        alertDialogBuilder.setPositiveButton("Reintentar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(isOnline())
                   loadSectionsAdapter();
                else
                    alertDialogBuilder.show();
            }
        })
                .setNegativeButton("Cencelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        alertDialogNoConn.dismiss();
                    }
                });
        alertDialogBuilder.setMessage("No hay conexión a internet. Por favor, comprueba tu conexión");
        return alertDialogBuilder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = prefs.getString("theme", "default");

        if (theme.equals("dark") && !dark || theme.equals("light") && dark){
            recreate();
        }else if(theme.equals("default")){
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    if(!dark){
                        recreate();
                    }
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    if(dark){
                        recreate();
                    }
            }
        }
        /*if (mBottomBanner != null) {
            mBottomBanner.resume();
        }*/

        if(mSectionsPagerAdapter == null){
            return;
        }
        int count = prefsPor.getAll().size();
        if(count > 0){
            if(!mSectionsPagerAdapter.categorias.get(0).equals(getString(R.string.fav_tab))) {
                int currentTab = tabLayout.getSelectedTabPosition()+1;
                mSectionsPagerAdapter.addFragment(getString(R.string.fav_tab), Favoritos.newInstance(fecha));
                mViewPager.setAdapter(mSectionsPagerAdapter);
                mViewPager.setCurrentItem(currentTab);
            }else if(favsCount != count){
                Fragment fragment = mSectionsPagerAdapter.getItem(0);
                getSupportFragmentManager().beginTransaction().detach(fragment).attach(fragment).commit();
                favsCount = count;
            }
        }else if(mSectionsPagerAdapter.getCount() > 5) {
            int currentTab = tabLayout.getSelectedTabPosition();
            mSectionsPagerAdapter.removeFragment();
            if(currentTab != 0){
                mViewPager.setCurrentItem(currentTab-1);
            }
        }
        /*if(billingClient != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                                    handlePurchase(purchase);
                                }
                            }
                        }
                    }
            );
        }*/
    }
    /*@Override
    protected void onPause() {
        super.onPause();
        if (bottomBanner != null) {
            bottomBanner.pause();
        }
    }*/


    @Override
    protected void onDestroy() {
        if (bottomBanner != null) {
            bottomBanner.destroy();
        }
        super.onDestroy();
    }
}
