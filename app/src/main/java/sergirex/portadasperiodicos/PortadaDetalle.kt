package sergirex.portadasperiodicos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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

@AndroidEntryPoint
class PortadaDetalle : AppCompatActivity() {

    // Using View Binding to replace findViewById
    private lateinit var binding: ActivityPortadaDetalleBinding

    // Using ViewModel to store UI state and survive configuration changes
    private val viewModel: PortadaDetalleViewModel by viewModels()

    private lateinit var mSectionsPagerAdapter: ViewPagerAdapter

    private val adManager by lazy { AdManager(this) }

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

        keepBannerAboveNavigationBar()
        setupToolbar()
        setupFabs()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.favoriteIds.collect { renderFavorite() } }
        }
        loadSectionsAdapter()
        showBannerAd()
        // Count the visit once per real open (not when the system recreates the Activity).
        if (savedInstanceState == null) {
            lifecycleScope.launch { if (viewModel.onCoverOpened()) showInAppReviewPrompt() }
        }
    }

    /** The banner is the bottom-most view: pad it by the navigation/gesture bar so it isn't drawn under it. */
    private fun keepBannerAboveNavigationBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bannerContainerDetail) { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
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
        val portadaList = IntentCompat.getParcelableArrayListExtra(intent, EXTRA_PORTADAS, Portada::class.java).orEmpty()
        val initialPortada = intent.getStringExtra(EXTRA_SELECTED_ID)

        mSectionsPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        mSectionsPagerAdapter.setPortadas(portadaList)

        binding.viewpager2.adapter = mSectionsPagerAdapter

        // Set initial position
        val initialPosition = portadaList.indexOfFirst { it.id == initialPortada }
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
        supportActionBar?.title = portada.id
        renderFavorite()
    }

    private fun renderFavorite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val isFavorite = portada.id in viewModel.favoriteIds.value
        binding.favButton.setImageResource(if (isFavorite) R.drawable.ic_favorite_black_24dp else R.drawable.ic_favorite_border_black_24dp)
    }

    private fun showBannerAd() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!BillingManager.isAdsRemoved(prefs)) {
            adManager.loadBanner(binding.bannerContainerDetail, DETAIL_BANNER_PLACEMENT)
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
        val coverDate = viewModel.resolvedDates[portada.id] ?: portada.fecha
        val imageUrl = CoverUrls.full(coverDate, portada.country, portada.id)
        val savePortada = SavePortada(this, requestStoragePermission)

        when (item.itemId) {
            R.id.share -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    lifecycleScope.launch { savePortada.share(imageUrl, portada.id) }
                }
            }
            R.id.save -> {
                if (savePortada.isExternalStorageWritable && savePortada.checkPermissions()) {
                    val file = File(savePortada.albumStorageDir, "${portada.id}_${coverDate.replace("/", "")}.jpg")
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
        val initialSelection = viewModel.lastSelectedDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .setSelection(initialSelection)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            viewModel.lastSelectedDateMillis = selection
            val newDate = EditionDate.fromUtcMillis(selection)

            val currentPosition = binding.viewpager2.currentItem
            mSectionsPagerAdapter.updateDateForPortada(currentPosition, newDate)
            mSectionsPagerAdapter.getPortadaAt(currentPosition)?.let { viewModel.resolvedDates.remove(it.id) }

            // Find the current fragment and tell it to reload
            val currentFragment = supportFragmentManager.findFragmentByTag("f$currentPosition")
            (currentFragment as? PortadaDetalleFragment)?.reloadWithDate(newDate)
        }

        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun openNewspaperWebsite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val url = "https://www.${portada.domain}"
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
        viewModel.toggleFavorite(portada.toRef())
    }

    override fun onDestroy() {
        adManager.destroy()
        super.onDestroy()
    }

    private fun showInAppReviewPrompt() {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(this, reviewInfo)
                // The API doesn't say whether the dialog was shown or the user reviewed, so any
                // completion counts as a prompt, to avoid asking again too soon.
                flow.addOnCompleteListener { viewModel.onReviewPromptShown() }
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
            return PortadaDetalleFragment.newInstance(portada.id, portada.country, portada.fecha)
        }
    }

    companion object {
        private const val EXTRA_PORTADAS = "portadas"
        private const val EXTRA_SELECTED_ID = "selected_id"
        private const val DETAIL_BANNER_PLACEMENT = "799967435028134_814221240269420"

        fun createIntent(context: Context, portadas: List<Portada>, selectedId: String): Intent =
            Intent(context, PortadaDetalle::class.java)
                .putParcelableArrayListExtra(EXTRA_PORTADAS, ArrayList(portadas))
                .putExtra(EXTRA_SELECTED_ID, selectedId)
    }
}
