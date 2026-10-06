package sergirex.portadasperiodicos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import sergirex.portadasperiodicos.databinding.AboutBinding
import sergirex.portadasperiodicos.databinding.ActivityPortadasBinding
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

/**
 * Required structurally: Portadas hosts PortadasFragment (@AndroidEntryPoint), and Hilt
 * requires the hosting Activity to be an entry point too, even though Portadas itself
 * doesn't inject anything yet.
 */
@AndroidEntryPoint
class Portadas : AppCompatActivity(), BillingManager.BillingListener {

    private lateinit var binding: ActivityPortadasBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsPor: SharedPreferences
    private var billingManager: BillingManager? = null
    private var adManager: AdManager? = null

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var alertDialog: AlertDialog? = null
    private var favsCount = 0

    // Remembers the user's last date-picker selection across re-openings of the dialog.
    private var today: Long? = null

    // Must be registered before STARTED (i.e. as a field, not inside onCreate) per the
    // Activity Result API contract.
    private val requestPostNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startAlarmBroadcastReceiver(applicationContext)
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

        prefsPor = getSharedPreferences("periodicos", Context.MODE_PRIVATE)

        if (prefs.getInt("rate", 0) == 2) {
            prefs.edit().putInt("rate", 0).apply()
        }

        // Always build the tabs: the covers screens load on their own (and retry on the next
        // onStart), and anything already cached still shows offline. Starting offline used to
        // skip this and leave the screen blank even after the network came back.
        loadSectionsAdapter()
        if (!isOnline()) showNoConnectionDialog()

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

        // POST_NOTIFICATIONS is a runtime permission from API 33 (Tiramisu) on; older versions need none.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startAlarmBroadcastReceiver(this)
        } else {
            requestPostNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!prefs.getBoolean("remove_fb_ads", false)) {
            adManager?.loadBannerAd(binding.bannerContainer)
        }
    }

    private fun hideRemoveAdsMenuItem(navigationView: NavigationView) {
        navigationView.menu.findItem(R.id.nav_remove_ads).isVisible = false
    }

    private fun startAlarmBroadcastReceiver(context: Context) {
        AlarmBroadcastReceiver().startAlarmBroadcastReceiver(context, false)
    }

    private fun loadSectionsAdapter() {
        val adapter = SectionsPagerAdapter(supportFragmentManager, lifecycle)
        mSectionsPagerAdapter = adapter

        val hasFavorites = prefsPor.all.isNotEmpty()
        if (hasFavorites) {
            adapter.addFragment(getString(R.string.fav_tab), PortadasFragment.newInstanceFavorites())
            favsCount = prefsPor.all.size
        }
        adapter.addFragment(getString(R.string.first_tab), PortadasFragment.newInstance(PeriodicoCategory.GENERAL))
        adapter.addFragment(getString(R.string.second_tab), PortadasFragment.newInstance(PeriodicoCategory.DEPORTES))
        adapter.addFragment(getString(R.string.third_tab), PortadasFragment.newInstance(PeriodicoCategory.ECONOMIA))
        adapter.addFragment(getString(R.string.fourth_tab), PortadasFragment.newInstance(PeriodicoCategory.LOCALES))
        adapter.addFragment(getString(R.string.fifth_tab), PortadasFragment.newInstance(PeriodicoCategory.INTERNACIONAL))

        binding.viewpager.adapter = adapter
        binding.viewpager.offscreenPageLimit = adapter.itemCount - 1
        TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
            tab.text = adapter.titleAt(position)
        }.attach()

        val categoryPrefix = prefs.getString("init_category", getString(R.string.first_tab))!!.take(3)
        val currentItem = if (!hasFavorites) {
            when (categoryPrefix) {
                "Dep", "Spo" -> 1
                "Eco" -> 2
                "Loc" -> 3
                "Int" -> 4
                else -> 0
            }
        } else {
            when (categoryPrefix) {
                "Fav" -> 0
                "Dep", "Spo" -> 2
                "Eco" -> 3
                "Loc" -> 4
                "Int" -> 5
                else -> 1
            }
        }
        binding.viewpager.setCurrentItem(currentItem, false)
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

    /**
     * A [FragmentStateAdapter] that returns a fragment corresponding to one of the
     * sections/tabs/pages. Owns `categories` itself instead of mutating a field on the
     * outer Activity (the original Java version had the Activity's `categorias` list
     * reached into and mutated by this adapter) — an inner class so it can still reach
     * `binding.tabs`/`binding.viewpager` for [addFirstFragment]/[removeFragment], the same
     * access the original relied on.
     */
    private inner class SectionsPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fm, lifecycle) {

        private val categories = mutableListOf<String>()
        private val fragments = mutableListOf<Fragment>()
        private val fragmentIds = mutableListOf<Long>()

        val firstCategoryTitleOrNull: String? get() = categories.firstOrNull()

        fun titleAt(position: Int): String = categories[position]

        fun addFragment(title: String, fragment: Fragment) {
            categories.add(title)
            fragments.add(fragment)
            fragmentIds.add(fragment.hashCode().toLong())
        }

        fun addFirstFragment(title: String, fragment: Fragment) {
            categories.add(0, title)
            fragments.add(0, fragment)
            TabLayoutMediator(binding.tabs, binding.viewpager) { tab, position ->
                tab.text = categories[position]
            }.attach()
            fragmentIds.add(0, fragment.hashCode().toLong())
        }

        fun removeFragment() {
            binding.tabs.removeTabAt(0)
            categories.removeAt(0)
            fragments.removeAt(0)
            fragmentIds.removeAt(0)
        }

        override fun getItemId(position: Int): Long = fragmentIds[position]

        override fun containsItem(itemId: Long): Boolean = fragmentIds.contains(itemId)

        override fun createFragment(position: Int): Fragment = fragments[position]

        override fun getItemCount(): Int = categories.size
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fav -> {
                if (prefsPor.all.isNotEmpty()) {
                    binding.viewpager.currentItem = 0
                } else {
                    Snackbar.make(binding.drawerLayout, "No tienes ninguna portada favorita", Snackbar.LENGTH_SHORT)
                        .apply { animationMode = Snackbar.ANIMATION_MODE_FADE }
                        .show()
                }
            }
            R.id.date -> {
                val initialSelection = today ?: MaterialDatePicker.todayInUtcMilliseconds()
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setCalendarConstraints(
                        CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
                    )
                    .setTitleText("Select date")
                    .setSelection(initialSelection)
                    .build()
                datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
                datePicker.addOnPositiveButtonClickListener { selection ->
                    today = selection
                    loadSectionsAdapter()
                }
            }
        }
        return true
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Built fresh each time: re-showing a dialog from inside its own button handler is a no-op,
    // because AlertDialog dismisses itself right after the handler returns.
    private fun showNoConnectionDialog() {
        AlertDialog.Builder(this)
            .setTitle(" Error de conexión")
            .setIcon(R.mipmap.news_icon)
            .setMessage("No hay conexión a internet. Por favor, comprueba tu conexión")
            .setPositiveButton("Reintentar") { _, _ -> if (isOnline()) loadSectionsAdapter() else showNoConnectionDialog() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val adapter = mSectionsPagerAdapter ?: return
        val count = prefsPor.all.size
        when {
            count > 0 && adapter.firstCategoryTitleOrNull != getString(R.string.fav_tab) -> {
                adapter.addFirstFragment(getString(R.string.fav_tab), PortadasFragment.newInstanceFavorites())
                adapter.notifyItemInserted(0)
                binding.viewpager.currentItem = 0
            }
            count > 0 && favsCount != count -> {
                adapter.removeFragment()
                adapter.addFirstFragment(getString(R.string.fav_tab), PortadasFragment.newInstanceFavorites())
                adapter.notifyItemChanged(0)
                favsCount = count
            }
            count == 0 && adapter.itemCount > 5 -> {
                adapter.removeFragment()
                adapter.notifyItemRemoved(0)
            }
        }
    }

    override fun onDestroy() {
        billingManager?.destroy()
        adManager?.destroy()
        super.onDestroy()
    }
}
