package sergirex.portadasperiodicos;

import static sergirex.portadasperiodicos.SavePortada.permission;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Portadas extends AppCompatActivity implements BillingManager.BillingListener {
    static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 0;
    static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATION = 1;
    private DrawerLayout mDrawerLayout;
    private ViewPager2 mViewPager;
    private List<String> categorias;
    private AlertDialog alertDialog;
    private AlertDialog alertDialogNoConn;
    private TabLayout tabLayout;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private SharedPreferences prefs;
    private SharedPreferences prefsPor;
    private BillingManager billingManager;
    private AdManager adManager;
    private int favsCount = 0;
    private Long today;
    private String fecha;
    private ProgressBar pd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        billingManager = new BillingManager(this, prefs, this);
        adManager = new AdManager(this);


        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if(calendar.get(Calendar.HOUR_OF_DAY) < 6){
            calendar.add(Calendar.DATE, -1);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
        fecha = formatter.format(calendar.getTime());
        String fechaPortadas = getSharedPreferences("FechasGeneral", Context.MODE_PRIVATE).getString("fechaPortadas", null);
        boolean descargar = fechaPortadas == null || !fechaPortadas.equals(fecha);

        setContentView(R.layout.activity_portadas);

        Toolbar toolbar = findViewById(R.id.toolbar);
        //toolbar.setTitle(R.string.covers);
        setSupportActionBar(toolbar);

        mDrawerLayout = findViewById(R.id.drawerLayout);
        pd = findViewById(R.id.progressBar);

        NavigationView mNavigationView = findViewById(R.id.navView);
        if(prefs.getBoolean("remove_fb_ads", false)){
            hideRemoveAdsMenuItem(mNavigationView);
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


        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    moveTaskToBack(true);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this,onBackPressedCallback);

        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            mDrawerLayout.closeDrawers();
            int itemId = menuItem.getItemId();
            if (itemId == R.id.nav_help)
                showDialog().show();
            else if(itemId == R.id.nav_remove_ads){
                billingManager.showPurchaseDialog();
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
        }else{
            if (Build.VERSION.SDK_INT <= 33) {
                startAlarmBroadcastReceiver(this);
            }
        }


        if(!prefs.getBoolean("remove_fb_ads", false)) {
            LinearLayout adContainer = findViewById(R.id.bannerContainer);
            adManager.loadBannerAd(adContainer);
        }
    }

    private void hideRemoveAdsMenuItem(NavigationView mNavigationView) {
        Menu nav_Menu = mNavigationView.getMenu();
        nav_Menu.findItem(R.id.nav_remove_ads).setVisible(false);
    }


    public void startAlarmBroadcastReceiver(Context context) {
        AlarmBroadcastReceiver alarmBroadcastReceiver = new AlarmBroadcastReceiver();
        alarmBroadcastReceiver.startAlarmBroadcastReceiver(context,false);
    }

    void loadSectionsAdapter(){
        mViewPager = findViewById(R.id.viewpager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getLifecycle());
        if(!prefsPor.getAll().isEmpty()) {
            mSectionsPagerAdapter.addFragment(getString(R.string.fav_tab),Favoritos.newInstance(fecha));
            favsCount = prefsPor.getAll().size();
        }
        mSectionsPagerAdapter.addFragment(getString(R.string.first_tab),General.newInstance(fecha));
        mSectionsPagerAdapter.addFragment(getString(R.string.second_tab),Deportes.newInstance(fecha));
        mSectionsPagerAdapter.addFragment(getString(R.string.third_tab),Economia.newInstance(fecha));
        mSectionsPagerAdapter.addFragment(getString(R.string.fourth_tab),Locales.newInstance(fecha));
        mSectionsPagerAdapter.addFragment(getString(R.string.fifth_tab),Internacional.newInstance(fecha));

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getItemCount()-1);
        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> tab.setText(categorias.get(position))
        ).attach();
        String lpValue = prefs.getString("init_category", getString(R.string.first_tab));
        if(prefsPor.getAll().isEmpty()) {
            switch (lpValue.substring(0,3)) {
                case "Dep":
                case "Spo":
                    mViewPager.setCurrentItem(1,false);
                    break;
                case "Eco":
                    mViewPager.setCurrentItem(2,false);
                    break;
                case "Loc":
                    mViewPager.setCurrentItem(3,false);
                    break;
                case "Int":
                    mViewPager.setCurrentItem(4,false);
                    break;
                default: mViewPager.setCurrentItem(0,false);
            }
        }else{
            switch (lpValue.substring(0,3)) {
                case "Fav":
                    mViewPager.setCurrentItem(0,false);
                    break;
                case "Dep":
                case "Spo":
                    mViewPager.setCurrentItem(2,false);
                    break;
                case "Eco":
                    mViewPager.setCurrentItem(3,false);
                    break;
                case "Loc":
                    mViewPager.setCurrentItem(4,false);
                    break;
                case "Int":
                    mViewPager.setCurrentItem(5,false);
                    break;
                default: mViewPager.setCurrentItem(1,false);
            }
        }
    }

    void showAboutInfo(){
        View aboutLayout = getLayoutInflater().inflate(R.layout.about, mDrawerLayout ,false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.close, null);
        builder.setView(aboutLayout);
        TextView appSource = aboutLayout.findViewById(R.id.appSource);
        TextView tv = aboutLayout.findViewById(R.id.appVersion);


        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;

            if(versionName != null)
                tv.setText("v".concat(versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tv.setText("v".concat("N/A"));
        }

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
        Button portadas_revistas = aboutLayout.findViewById(R.id.portadas_revistas);
        portadas_revistas.setOnClickListener(v -> {
            Uri uri = Uri.parse("market://details?id=sergirex.portadasrevistas");
            Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(myAppLinkToMarket);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
            }
        });
        builder.create();
        builder.show();
    }

    private AlertDialog showDialog() {
        if (alertDialog == null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
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

    @Override
    public void onPurchaseAcknowledged() {
        adManager.destroy();
        findViewById(R.id.bannerContainer).setVisibility(View.GONE);
        NavigationView mNavigationView = findViewById(R.id.navView);
        hideRemoveAdsMenuItem(mNavigationView);
    }

    /**
     * A {@link FragmentStateAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<Long> fragmentsIds = new ArrayList<>();
        void addFragment(String title, Fragment fragment) {
            categorias.add(title);
            mFragments.add(fragment);
            fragmentsIds.add((long) fragment.hashCode());
        }

        void addFirstFragment(String title, Fragment fragment) {
            categorias.add(0,title);
            mFragments.add(0,fragment);
            new TabLayoutMediator(tabLayout, mViewPager,
                    (tab, position) -> tab.setText(categorias.get(position))
            ).attach();
            fragmentsIds.add(0,(long) fragment.hashCode());
        }

        @Override
        public long getItemId(int position) {
            return fragmentsIds.get(position);
        }

        @Override
        public boolean containsItem(long itemId) {
            return fragmentsIds.contains(itemId);
        }

        void removeFragment() {
            tabLayout.removeTabAt(0);
            categorias.remove(0);
            mFragments.remove(0);
            fragmentsIds.remove(0);
        }

        public
        SectionsPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
            super(fm, lifecycle);
            categorias = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemCount() {
            return categorias.size();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.fav){
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
                        this.fecha = formatter.format(new Date(aLong));

                        loadSectionsAdapter();
                    });
            return true;
        }
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
        final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(f);
        scanIntent.setData(contentUri);
        ctxt.sendBroadcast(scanIntent);

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
        }
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
    protected void onResume() {
        super.onResume();

        if(mSectionsPagerAdapter == null){
            return;
        }
        int count = prefsPor.getAll().size();
        if(count > 0){
            if(!categorias.get(0).equals(getString(R.string.fav_tab))) {
                mSectionsPagerAdapter.addFirstFragment(getString(R.string.fav_tab), Favoritos.newInstance(fecha));
                mSectionsPagerAdapter.notifyItemInserted(0);
                mViewPager.setCurrentItem(0);
            }else if(favsCount != count){
                mSectionsPagerAdapter.removeFragment();
                mSectionsPagerAdapter.addFirstFragment(getString(R.string.fav_tab),Favoritos.newInstance(fecha));
                mSectionsPagerAdapter.notifyItemChanged(0);
                favsCount = count;
            }
        }else if(mSectionsPagerAdapter.getItemCount() > 5) {
            mSectionsPagerAdapter.removeFragment();
            mSectionsPagerAdapter.notifyItemRemoved(0);
        }
    }

    @Override
    protected void onDestroy() {
        if (billingManager != null) {
            billingManager.destroy();
        }
        if (adManager != null) {
            adManager.destroy();
        }
        super.onDestroy();
    }
}
