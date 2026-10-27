package sergirex.portadasperiodicos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.RotateDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.color.MaterialColors
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.TooltipCompat
import android.content.res.ColorStateList
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.IntentCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.ActivityPortadaDetalleBinding
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.presentation.ads.AdsViewModel
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.InterstitialPolicy
import java.io.File
import sergirex.portadasperiodicos.ui.ActivityTransitions
import sergirex.portadasperiodicos.ui.PureBlackTheme
import sergirex.portadasperiodicos.ui.SpringMotion

@AndroidEntryPoint
class PortadaDetalle : AppCompatActivity() {

    // Using View Binding to replace findViewById
    private lateinit var binding: ActivityPortadaDetalleBinding

    // Using ViewModel to store UI state and survive configuration changes
    private val viewModel: PortadaDetalleViewModel by viewModels()

    private lateinit var mSectionsPagerAdapter: ViewPagerAdapter

    private val adsViewModel: AdsViewModel by viewModels()
    private var bannerRequested = false
    private val adManagerDelegate = lazy { AdManager(this) }
    private val adManager by adManagerDelegate

    @Inject
    lateinit var interstitialPolicy: InterstitialPolicy

    // Whether this open asked for a Play review: that is enough of an interruption, so no interstitial on the way out.
    private var reviewPromptShown = false

    /**
     * Shows the interstitial when the user leaves, which is a natural pause (unlike on open, which would delay what they
     * came to see). Enabled only while one is loaded and the policy allows it.
     */
    private val interstitialBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = false
            interstitialPolicy.recordShown()
            if (!adManager.showInterstitial { finishAfterTransition() }) finishAfterTransition()
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        PureBlackTheme.apply(this)
        ActivityTransitions.prepareExpandingChild(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPortadaDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityTransitions.expandFrom(this, binding.detailContent, R.id.detailAppBar)

        onBackPressedDispatcher.addCallback(this, interstitialBackCallback)
        keepContentAboveNavigationBar()
        setupToolbar()
        setupFabs()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.favoriteIds.collect { renderFavorite() } }
        }
        loadSectionsAdapter()
        showBannerAd()
        // Count the visit once per real open (not when the system recreates the Activity).
        if (savedInstanceState == null) {
            interstitialPolicy.recordOpen()
            lifecycleScope.launch {
                if (viewModel.onCoverOpened()) {
                    reviewPromptShown = true
                    interstitialBackCallback.isEnabled = false
                    showInAppReviewPrompt()
                }
            }
        }
    }

    private var navigationBarInset = 0

    /**
     * Keeps the bottom-most content off the navigation/gesture bar: the banner is padded by it, and without a banner the
     * content area is, plus a little extra so the image and buttons aren't glued to the bottom of the screen.
     */
    private fun keepContentAboveNavigationBar() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            navigationBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            updateBottomSpace()
            insets
        }
    }

    private fun updateBottomSpace() {
        val bannerShown = binding.bannerContainerDetail.visibility == View.VISIBLE
        binding.bannerContainerDetail.updatePadding(bottom = if (bannerShown) navigationBarInset else 0)
        binding.detailContent.updatePadding(
            bottom = if (bannerShown) 0 else navigationBarInset + resources.getDimensionPixelSize(R.dimen.detail_no_banner_bottom_space)
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // The "+" turns into an "x" by rotating only the icon (not the whole button, whose shape would turn into a diamond).
    // The drawable's level 0..10000 maps to 0..90 degrees, so a bouncy spring can overshoot the resting 45.
    private val addIcon: RotateDrawable by lazy {
        RotateDrawable().apply {
            drawable = AppCompatResources.getDrawable(this@PortadaDetalle, R.drawable.baseline_add_24)
            fromDegrees = 0f
            toDegrees = 90f
            isPivotXRelative = true
            pivotX = 0.5f
            isPivotYRelative = true
            pivotY = 0.5f
        }
    }

    private fun setupFabs() {
        binding.addFab.setImageDrawable(addIcon)
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
        val minis = listOf(binding.httpButton, binding.favButton)
        minis.forEach { it.isClickable = isExpanded }

        if (!animate) {
            addIcon.level = if (isExpanded) ADD_ICON_OPEN_LEVEL.toInt() else 0
            minis.forEach { fab ->
                fab.visibility = if (isExpanded) View.VISIBLE else View.INVISIBLE
                fab.translationY = if (isExpanded) 0f else fabTravel()
                fab.alpha = if (isExpanded) 1f else 0f
                fab.scaleX = if (isExpanded) 1f else FAB_HIDDEN_SCALE
                fab.scaleY = fab.scaleX
            }
            return
        }

        SpringMotion.animateProperty(this, addIcon, ICON_LEVEL, if (isExpanded) ADD_ICON_OPEN_LEVEL else 0f, SpringMotion.Spring.FAST_SPATIAL)
        minis.forEachIndexed { index, fab ->
            if (isExpanded && fab.visibility != View.VISIBLE) {
                // Start from just below and small, so the springs have somewhere to travel from.
                fab.translationY = fabTravel()
                fab.alpha = 0f
                fab.scaleX = FAB_HIDDEN_SCALE
                fab.scaleY = FAB_HIDDEN_SCALE
                fab.visibility = View.VISIBLE
            }
            // Each button starts a moment after the previous one; they leave in the opposite order.
            val delay = (if (isExpanded) index else minis.lastIndex - index) * FAB_STAGGER_MS
            fab.postDelayed({
                if (viewModel.areFabsExpanded.value != isExpanded) return@postDelayed // toggled again meanwhile
                val scale = if (isExpanded) 1f else FAB_HIDDEN_SCALE
                SpringMotion.animate(fab, DynamicAnimation.TRANSLATION_Y, if (isExpanded) 0f else fabTravel(), SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.SCALE_X, scale, SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.SCALE_Y, scale, SpringMotion.Spring.DEFAULT_SPATIAL)
                SpringMotion.animate(fab, DynamicAnimation.ALPHA, if (isExpanded) 1f else 0f, SpringMotion.Spring.DEFAULT_EFFECTS) {
                    if (!isExpanded) fab.visibility = View.INVISIBLE
                }
            }, delay)
        }
    }

    /** How far below its place a hidden menu button waits. */
    private fun fabTravel() = FAB_TRAVEL_DP * resources.displayMetrics.density

    private fun loadSectionsAdapter() {
        val portadaList = IntentCompat.getParcelableArrayListExtra(intent, EXTRA_PORTADAS, Portada::class.java).orEmpty()
        if (portadaList.isEmpty()) {
            // Launched without the covers it should show (not possible from the app itself): nothing to display.
            finish()
            return
        }
        val initialPortada = intent.getStringExtra(EXTRA_SELECTED_ID)

        mSectionsPagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        mSectionsPagerAdapter.setPortadas(portadaList)

        binding.viewpager2.adapter = mSectionsPagerAdapter

        // Listen before choosing the first page: ViewPager2 reports a page change synchronously inside
        // setCurrentItem(), and doesn't report the initial page at all when it is the first one.
        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForPage(position)
            }
        })

        val initialPosition = portadaList.indexOfFirst { it.id == initialPortada }
        if (initialPosition != -1) {
            binding.viewpager2.setCurrentItem(initialPosition, false)
        }
        // Don't depend on any callback for the page that is showing from the start (also after a rotation).
        updateUiForPage(binding.viewpager2.currentItem)
    }

    private fun updateUiForPage(position: Int) {
        val portada = mSectionsPagerAdapter.getPortadaAt(position) ?: return
        supportActionBar?.title = portada.name
        renderFavorite()
    }

    private fun renderFavorite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val isFavorite = portada.id in viewModel.favoriteIds.value
        // Same heart in both states: the color tells them apart (a favorite is colored, otherwise it's muted).
        val tint = if (isFavorite) {
            ContextCompat.getColor(this, R.color.favorite_active)
        } else {
            MaterialColors.getColor(binding.favButton, com.google.android.material.R.attr.colorOnSurfaceVariant)
        }
        binding.favButton.imageTintList = ColorStateList.valueOf(tint)
        val label = getString(if (isFavorite) R.string.fav_remove else R.string.fav_add)
        binding.favButton.contentDescription = label
        TooltipCompat.setTooltipText(binding.favButton, label)
    }

    /** Shows the banner unless the user owns "remove ads"; reacts if the purchase happens while this screen is open. */
    private fun showBannerAd() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                adsViewModel.adsRemoved.collect { removed ->
                    when (removed) {
                        null -> Unit
                        true -> {
                            if (adManagerDelegate.isInitialized()) adManager.destroy()
                            bannerRequested = false
                            interstitialBackCallback.isEnabled = false
                            binding.bannerContainerDetail.visibility = View.GONE
                            updateBottomSpace()
                        }
                        false -> {
                            binding.bannerContainerDetail.visibility = View.VISIBLE
                            updateBottomSpace()
                            if (!bannerRequested) {
                                bannerRequested = true
                                adManager.loadBanner(binding.bannerContainerDetail, DETAIL_BANNER_PLACEMENT)
                                // Only ask for one when this exit is due: a request nobody will see just wastes fill.
                                if (interstitialPolicy.shouldShow()) {
                                    adManager.loadInterstitial(DETAIL_INTERSTITIAL_PLACEMENT) {
                                        interstitialBackCallback.isEnabled = !reviewPromptShown && interstitialPolicy.shouldShow()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finishAfterTransition() {
        // The cover grew out of one grid cell; if the user swiped to another cover, don't shrink into the wrong cell.
        val showing = if (::mSectionsPagerAdapter.isInitialized) {
            mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem)?.id
        } else {
            null
        }
        if (showing != null && showing != intent.getStringExtra(EXTRA_SELECTED_ID)) {
            ActivityTransitions.stopSharing(this, binding.detailContent, R.id.detailRoot)
        }
        // The detail window's opaque background normally fades out over the whole return, which keeps the home screen (and
        // its toolbar) hidden until the end; drop it right away so the toolbars swap as the return starts.
        window.setTransitionBackgroundFadeDuration(0)
        // Don't leave the app bar to the window transition, which keeps it on screen until the return is over: fade it
        // out ourselves right now, so the home toolbar underneath shows from the start.
        binding.detailAppBar.animate().alpha(0f).setDuration(APP_BAR_FADE_MS).start()
        super.finishAfterTransition()
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
            // No Custom Tabs provider: fall back to any browser.
            startActivityOrToast(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun toggleFavorite() {
        val portada = mSectionsPagerAdapter.getPortadaAt(binding.viewpager2.currentItem) ?: return
        val wasFavorite = portada.id in viewModel.favoriteIds.value
        viewModel.toggleFavorite(portada.toRef())
        Snackbar.make(binding.root, if (wasFavorite) R.string.fav_removed else R.string.fav_added, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.addFab)
            .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
            .show()
    }

    override fun onDestroy() {
        // Don't build (and initialize the SDK for) an AdManager just to destroy it.
        if (adManagerDelegate.isInitialized()) adManager.destroy()
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
        private const val APP_BAR_FADE_MS = 150L
        private const val DETAIL_BANNER_PLACEMENT = "799967435028134_1424357055922499"
        private const val DETAIL_INTERSTITIAL_PLACEMENT = "799967435028134_1424357802589091"

        // Button menu motion
        private const val ADD_ICON_OPEN_LEVEL = 5000f // 45 degrees of the 0..90 range
        private const val FAB_HIDDEN_SCALE = 0.5f
        private const val FAB_TRAVEL_DP = 56f
        private const val FAB_STAGGER_MS = 40L

        private val ICON_LEVEL = object : FloatPropertyCompat<RotateDrawable>("level") {
            override fun getValue(drawable: RotateDrawable) = drawable.level.toFloat()
            override fun setValue(drawable: RotateDrawable, value: Float) {
                drawable.level = value.toInt().coerceIn(0, 10_000)
            }
        }

        fun createIntent(context: Context, portadas: List<Portada>, selectedId: String): Intent =
            Intent(context, PortadaDetalle::class.java)
                .putParcelableArrayListExtra(EXTRA_PORTADAS, ArrayList(portadas))
                .putExtra(EXTRA_SELECTED_ID, selectedId)
    }
}
