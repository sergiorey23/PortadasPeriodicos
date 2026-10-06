package sergirex.portadasperiodicos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.AboutBinding
import sergirex.portadasperiodicos.databinding.ActivityPortadasBinding
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.presentation.portadas.PortadasViewModel
import sergirex.portadasperiodicos.presentation.portadas.titleRes
import sergirex.portadasperiodicos.presentation.settings.SettingsViewModel

/** The home screen: a pager of newspaper-cover tabs, the navigation drawer and the toolbar actions. */
@AndroidEntryPoint
class Portadas : AppCompatActivity(), BillingManager.BillingListener {

    private lateinit var binding: ActivityPortadasBinding
    private lateinit var prefs: SharedPreferences
    private var billingManager: BillingManager? = null
    private var adManager: AdManager? = null
    private var alertDialog: AlertDialog? = null

    private val viewModel: PortadasViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var tabsAdapter: TabsAdapter

    // Must be registered before STARTED per the Activity Result API contract.
    private val requestPostNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            settingsViewModel.onNotificationPermissionResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The manifest sets SplashTheme (a plain AppCompat theme) for a clean cold-start
        // background; switch to the real MaterialComponents theme before inflating any
        // Material widgets (e.g. TabLayout), which require a MaterialComponents theme.
        setTheme(R.style.AppTheme)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        billingManager = BillingManager(this, prefs, this)
        adManager = AdManager(this)

        binding = ActivityPortadasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (prefs.getBoolean("remove_fb_ads", false)) {
            hideRemoveAdsMenuItem(binding.navView)
        }

        setUpTabs()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    moveTaskToBack(true)
                }
            }
        })

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_help -> showDialog().show()
                R.id.nav_remove_ads -> billingManager?.showPurchaseDialog()
                R.id.nav_rate -> launchMarket()
                R.id.nav_share -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Descarga la app de Portadas gratis!\n\nhttps://play.google.com/store/apps/details?id=$packageName"
                        )
                    }
                    startActivity(Intent.createChooser(intent, null))
                }
                R.id.nav_about -> showAboutInfo()
                R.id.nav_settings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
            true
        }

        val drawerToggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        askNotificationPermissionOnce()

        if (!prefs.getBoolean("remove_fb_ads", false)) {
            adManager?.loadBannerAd(binding.bannerContainer)
        }
    }

    private fun setUpTabs() {
        tabsAdapter = TabsAdapter(this)
        binding.viewpager.adapter = tabsAdapter
        // The mediator observes the adapter, so tabs added/removed by the adapter update the TabLayout by themselves.
        TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.setText(tabsAdapter.tabs[position].titleRes)
        }.attach()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.tabs.collect(::onTabsChanged) }
                launch {
                    viewModel.selectedDate.collect { date ->
                        supportActionBar?.subtitle = date?.let(EditionDate::toDisplay)
                    }
                }
            }
        }
    }

    private suspend fun onTabsChanged(tabs: List<HomeTab>) {
        if (tabs.isEmpty()) return
        val current = tabsAdapter.tabs.getOrNull(binding.viewpager.currentItem)
        tabsAdapter.submit(tabs)

        val selected = when {
            !viewModel.initialTabApplied -> viewModel.initialTab().also { viewModel.initialTabApplied = true }
            else -> current // keep the user on the tab they were on when tabs are added/removed
        }
        binding.viewpager.setCurrentItem(tabs.indexOf(selected).coerceAtLeast(0), false)
    }

    private fun askNotificationPermissionOnce() {
        lifecycleScope.launch {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@Portadas, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission && settingsViewModel.shouldAskNotificationPermission()) {
                settingsViewModel.onNotificationPermissionRequested()
                requestPostNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun hideRemoveAdsMenuItem(navigationView: NavigationView) {
        navigationView.menu.findItem(R.id.nav_remove_ads).isVisible = false
    }

    private fun showAboutInfo() {
        val aboutBinding = AboutBinding.inflate(layoutInflater, binding.drawerLayout, false)

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        aboutBinding.appVersion.text = "v${versionName ?: "N/A"}"

        aboutBinding.appSource.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lasportadas.es/")))
        }

        aboutBinding.privacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.lasportadas.es/privacy.php")))
        }

        aboutBinding.contact.setOnClickListener {
            val uri = Uri.parse("mailto:sssergiooo23@gmail.com")
            startActivity(Intent(Intent.ACTION_SENDTO, uri))
        }

        aboutBinding.portadasRevistas.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=sergirex.portadasrevistas")))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show()
            }
        }

        AlertDialog.Builder(this)
            .setPositiveButton(R.string.close, null)
            .setView(aboutBinding.root)
            .show()
    }

    private fun showDialog(): AlertDialog {
        alertDialog?.let { return it }

        val message = "<Big>${getString(R.string.help_description)}<br/><br/>" +
            "${getString(R.string.help_description2)}</Big><br/><br/>${getString(R.string.rate_app)}"
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.help)
            .setIcon(R.mipmap.news_icon)
            .setPositiveButton("OK", null)
            .create()
        dialog.setMessage(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
            else
                @Suppress("DEPRECATION") Html.fromHtml(message)
        )
        alertDialog = dialog
        return dialog
    }

    private fun launchMarket() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPurchaseAcknowledged() {
        adManager?.destroy()
        binding.bannerContainer.visibility = View.GONE
        hideRemoveAdsMenuItem(binding.navView)
    }

    /** Pager adapter over [HomeTab]s; changes are diffed so only the affected pages are added or removed. */
    private class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        var tabs: List<HomeTab> = emptyList()
            private set

        fun submit(newTabs: List<HomeTab>) {
            val old = tabs
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = newTabs.size
                override fun areItemsTheSame(oldPosition: Int, newPosition: Int) = old[oldPosition] == newTabs[newPosition]
                override fun areContentsTheSame(oldPosition: Int, newPosition: Int) = true
            })
            tabs = newTabs
            diff.dispatchUpdatesTo(this)
        }

        override fun getItemCount() = tabs.size
        override fun createFragment(position: Int): Fragment = PortadasFragment.newInstance(tabs[position])
        override fun getItemId(position: Int) = tabs[position].key.hashCode().toLong()
        override fun containsItem(itemId: Long) = tabs.any { it.key.hashCode().toLong() == itemId }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fav -> {
                val favoritesIndex = tabsAdapter.tabs.indexOf(HomeTab.Favorites)
                if (favoritesIndex >= 0) {
                    binding.viewpager.currentItem = favoritesIndex
                } else {
                    Snackbar.make(binding.drawerLayout, "No tienes ninguna portada favorita", Snackbar.LENGTH_SHORT)
                        .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
                        .show()
                }
            }
            R.id.date -> showDatePicker()
        }
        return true
    }

    private fun showDatePicker() {
        val selected = viewModel.selectedDate.value
        val initialSelection = selected?.let(EditionDate::toUtcMillis) ?: MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            )
            .setTitleText(R.string.select_date)
            .setSelection(initialSelection)
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = EditionDate.fromUtcMillis(selection)
            // Picking today's edition goes back to the default view (and its freshness rules).
            viewModel.selectDate(date.takeUnless { it >= EditionDate.today() })
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    override fun onDestroy() {
        billingManager?.destroy()
        adManager?.destroy()
        super.onDestroy()
    }
}
