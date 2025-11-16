package sergirex.portadasperiodicos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.facebook.ads.*
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewManagerFactory
import sergirex.portadasperiodicos.databinding.ActivityPortadaDetalleBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PortadaDetalle : AppCompatActivity() {

    // Using View Binding to replace findViewById
    private lateinit var binding: ActivityPortadaDetalleBinding

    // Using ViewModel to store UI state and survive configuration changes
    private val viewModel: PortadaDetalleViewModel by viewModels()

    private lateinit var prefsPer: SharedPreferences
    private lateinit var mSectionsPagerAdapter: ViewPagerAdapter

    private var interstitialAd: InterstitialAd? = null

    // Animations are loaded once
    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPortadaDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsPer = getSharedPreferences("periodicos", Context.MODE_PRIVATE)

        setupToolbar()
        setupFabs()
        loadSectionsAdapter()
        setupAdsAndReview()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupFabs() {
        binding.addFab.setOnClickListener {
            val isExpanded = viewModel.areFabsExpanded.value ?: false
            viewModel.areFabsExpanded.value = !isExpanded
        }

        // Observe the state from the ViewModel to update the UI
        viewModel.areFabsExpanded.observe(this) { isExpanded ->
            toggleFabs(isExpanded)
        }

        binding.httpButton.setOnClickListener { openNewspaperWebsite() }
        binding.favButton.setOnClickListener { toggleFavorite() }

        // Set initial state based on ViewModel (handles rotation)
        toggleFabs(viewModel.areFabsExpanded.value ?: false, animate = false)
    }

    private fun toggleFabs(isExpanded: Boolean, animate: Boolean = true) {
        val visibility = if (isExpanded) View.VISIBLE else View.INVISIBLE
        val clickable = isExpanded

        if (animate) {
            binding.httpButton.startAnimation(if (isExpanded) fromBottom else toBottom)
            binding.favButton.startAnimation(if (isExpanded) fromBottom else toBottom)
            binding.addFab.startAnimation(if (isExpanded) rotateOpen else rotateClose)
        }

        binding.httpButton.visibility = visibility
        binding.favButton.visibility = visibility
        binding.httpButton.isClickable = clickable
        binding.favButton.isClickable = clickable
    }

    private fun loadSectionsAdapter() {
        if (!isNetworkAvailable(this)) {
            showNoConnectionDialog()
            return
        }

        val portadas = intent.getStringArrayExtra("Portadas")
        val initialPortada = intent.getStringExtra("selectedPortada")
        val fecha = intent.getStringExtra("Fecha")

        mSectionsPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        val portadaList = portadas?.mapNotNull {
            val parts = it.split(":")
            val title = if (parts.size > 1) parts[0] else it.split(".")[0]
            val web = if (parts.size > 1) parts[1] else it
            val country = if (parts.size > 2) parts[2] else "es"
            Portada(it, title, fecha, web, country)
        } ?: emptyList()

        mSectionsPagerAdapter.setPortadas(portadaList)

        binding.viewpager2.adapter = mSectionsPagerAdapter

        // Set initial position
        val initialPosition = portadaList.indexOfFirst { it.title == initialPortada }
        if (initialPosition != -1) {
            binding.viewpager2.setCurrentItem(initialPosition, false)
        }

        // Use a callback to react to page changes
        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForPage(position)
            }
        })
    }

    private fun updateUiForPage(position: Int) {
        val portada = mSectionsPagerAdapter.getPortadaAt(position) ?: return
        supportActionBar?.title = portada.title
        val isFavorite = prefsPer.contains(portada.webPeriodico)
        binding.favButton.setImageResource(if (isFavorite) R.drawable.ic_favorite_black_24dp else R.drawable.ic_favorite_border_black_24dp)
    }

    private fun setupAdsAndReview() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val showAd = intent.getIntExtra("showAd", 0)

        if (prefs.getInt("rate", 0) == 0 && showAd % 2 == 0) {
            showInAppReviewPrompt(prefs)
        }

        if (!prefs.getBoolean("remove_fb_ads", false)) {
            loadFacebookAds(showAd)
        }
    }

    private fun loadFacebookAds(showAd: Int) {
        AudienceNetworkAds.initialize(this)
        // Banner Ad
        val bottomBanner = AdView(this, "799967435028134_814221240269420", AdSize.BANNER_HEIGHT_50)
        binding.bannerContainerDetail.addView(bottomBanner)
        bottomBanner.loadAd()

        // Interstitial Ad
        if (showAd % 3 == 0) {
            interstitialAd = InterstitialAd(this, "799967435028134_801174748240736")
            val interstitialAdListener = object : InterstitialAdListener {
                override fun onInterstitialDisplayed(ad: Ad) {}
                override fun onInterstitialDismissed(ad: Ad) {}
                override fun onError(ad: Ad, adError: AdError) {
                    Log.e("AD_ERROR", "Interstitial ad failed to load: " + adError.errorMessage)
                }
                override fun onAdLoaded(ad: Ad) {
                    interstitialAd?.show()
                }
                override fun onAdClicked(ad: Ad) {}
                override fun onLoggingImpression(ad: Ad) {}
            }
            interstitialAd?.loadAd(
                interstitialAd?.buildLoadAdConfig()
                    ?.withAdListener(interstitialAdListener)
                    ?.build()
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_portada, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val position = binding.viewpager2.currentItem
        val portada = mSectionsPagerAdapter.getPortadaAt(position) ?: return false
        val url = "https://kiosko.net/${portada.fecha}/${portada.siglaPais}/${portada.title}.html"
        val savePortada = SavePortada(this)

        when (item.itemId) {
            R.id.share -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    // Pass the root view for the Snackbar
                    DownloadPortada(this, binding.root, savePortada, portada.title).execute(url)
                }
            }
            R.id.save -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    val file = File(savePortada.albumStorageDir, "${portada.title}_${portada.fecha?.replace("/", "")}.jpg")
                    if (file.exists()) {
                        Toast.makeText(this, "Ya se ha guardado la portada.", Toast.LENGTH_LONG).show()
                    } else {
                        // Pass the root view for the Snackbar
                        DownloadPortada(this, binding.root, file).execute(url)
                    }
                }
            }
            R.id.date -> showDatePicker()
        }
        return true
    }

    private fun showDatePicker() {
        val today = viewModel.lastSelectedDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .setSelection(today)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            viewModel.lastSelectedDateMillis = selection
            val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE)
            val newDate = formatter.format(Date(selection))

            val currentPosition = binding.viewpager2.currentItem
            mSectionsPagerAdapter.updateDateForPortada(currentPosition, newDate)

            // Find the current fragment and tell it to reload
            val currentFragment = supportFragmentManager.findFragmentByTag("f$currentPosition")
            (currentFragment as? PortadaDetalleFragment)?.reloadWithDate(newDate)
        }

        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun openNewspaperWebsite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val url = "https://www.${portada.webPeriodico}"
        try {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            val color = typedValue.data

            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().setToolbarColor(color).build()
                )
                .build()
            intent.launchUrl(this, Uri.parse(url))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFavorite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val editor = prefsPer.edit()
        if (prefsPer.contains(portada.webPeriodico)) {
            editor.remove(portada.webPeriodico)
            binding.favButton.setImageResource(R.drawable.ic_favorite_border_black_24dp)
        } else {
            editor.putString(portada.webPeriodico, portada.periodico)
            binding.favButton.setImageResource(R.drawable.ic_favorite_black_24dp)
        }
        editor.apply()
    }

    // Check for network connectivity
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For modern Android versions
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showNoConnectionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error de conexión")
            .setIcon(R.mipmap.news_icon)
            .setMessage("No hay conexión a internet. Por favor, comprueba tu conexión.")
            .setPositiveButton("Reintentar") { dialog, _ ->
                dialog.dismiss()
                loadSectionsAdapter() // Retry loading
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroy() {
        interstitialAd?.destroy()
        interstitialAd = null
        super.onDestroy()
    }

    private fun showInAppReviewPrompt(prefs: SharedPreferences) {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The review flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even if the review dialog was shown. Thus, no matter
                    // the result, we update the shared preferences to avoid asking again.
                    prefs.edit().putInt("rate", 1).apply()
                }
            } else {
                // There was some error, log it.
                Log.e("InAppReview", "Review flow request failed.", task.exception)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PortadasUtils.MY_PERMISSIONS_REQUEST_WRITE_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permiso concedido. Por favor, intente la acción de nuevo.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Permiso denegado. No se puede guardar ni compartir la portada.", Toast.LENGTH_LONG).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    // --- Inner Adapter Class ---
    class ViewPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {
        private var portadas: List<Portada> = emptyList()

        fun setPortadas(portadaList: List<Portada>) {
            this.portadas = portadaList
            notifyDataSetChanged()
        }



        fun getPortadaAt(position: Int): Portada? = portadas.getOrNull(position)

        fun updateDateForPortada(position: Int, newDate: String) {
            portadas.getOrNull(position)?.fecha = newDate
        }

        override fun getItemCount(): Int = portadas.size

        override fun createFragment(position: Int): Fragment {
            val portada = portadas[position]
            return PortadaDetalleFragment.newInstance(portada.title, portada.siglaPais, portada.fecha)
        }
    }
}