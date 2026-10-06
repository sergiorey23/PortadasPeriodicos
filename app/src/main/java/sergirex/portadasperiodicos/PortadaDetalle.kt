package sergirex.portadasperiodicos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.facebook.ads.*
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.ActivityPortadaDetalleBinding
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate
import java.io.File
import java.util.Date

@AndroidEntryPoint
class PortadaDetalle : AppCompatActivity() {

    // Using View Binding to replace findViewById
    private lateinit var binding: ActivityPortadaDetalleBinding

    // Using ViewModel to store UI state and survive configuration changes
    private val viewModel: PortadaDetalleViewModel by viewModels()

    private lateinit var prefsPer: SharedPreferences
    private lateinit var mSectionsPagerAdapter: ViewPagerAdapter

    private var interstitialAd: InterstitialAd? = null

    // Must be registered before STARTED (i.e. as a field, not inside a click handler) per
    // the Activity Result API contract; SavePortada can't register its own since only an
    // Activity/Fragment can.
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Permiso concedido. Por favor, intente la acción de nuevo.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permiso denegado. No se puede guardar ni compartir la portada.", Toast.LENGTH_LONG).show()
            }
        }

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
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
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
        val portadas = intent.getStringArrayExtra("Portadas")
        val initialPortada = intent.getStringExtra("selectedPortada")
        val fecha = intent.getStringExtra("Fecha")

        mSectionsPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        val portadaList = portadas?.mapNotNull {
            val parts = it.split(":")
            val title = if (parts.size > 1) parts[0] else it.split(".")[0]
            val web = if (parts.size > 1) parts[1] else it
            val country = if (parts.size > 2) parts[2] else "es"
            Portada(it, title, fecha.orEmpty(), web, country)
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
            val ad = InterstitialAd(this, "799967435028134_801174748240736")
            interstitialAd = ad
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
            ad.loadAd(
                ad.buildLoadAdConfig()
                    .withAdListener(interstitialAdListener)
                    .build()
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
        // The actual cover image lives on the img. subdomain as a .jpg — not the
        // kiosko.net/....html article page this used to point at, which is an HTML
        // document and can never decode as a Bitmap (confirmed against the real
        // kiosko.net: that URL 301-redirects to an HTML page, Content-Type text/html).
        val coverDate = viewModel.resolvedDates[portada.title] ?: portada.fecha
        val imageUrl = CoverUrls.full(coverDate, portada.siglaPais, portada.title)
        val savePortada = SavePortada(this, requestStoragePermission)

        when (item.itemId) {
            R.id.share -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    lifecycleScope.launch { savePortada.share(imageUrl, portada.title) }
                }
            }
            R.id.save -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    val file = File(savePortada.albumStorageDir, "${portada.title}_${coverDate.replace("/", "")}.jpg")
                    if (file.exists()) {
                        Toast.makeText(this, "Ya se ha guardado la portada.", Toast.LENGTH_LONG).show()
                    } else {
                        lifecycleScope.launch { savePortada.saveToGallery(binding.root, imageUrl, file) }
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
            val newDate = EditionDate.format(Date(selection))

            val currentPosition = binding.viewpager2.currentItem
            mSectionsPagerAdapter.updateDateForPortada(currentPosition, newDate)
            mSectionsPagerAdapter.getPortadaAt(currentPosition)?.let { viewModel.resolvedDates.remove(it.title) }

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
            theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
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