package sergirex.portadasperiodicos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.usecase.GetThemeModeUseCase
import javax.inject.Inject

/**
 * @HiltAndroidApp makes this the root of the Hilt dependency graph — every
 * @AndroidEntryPoint Activity/Fragment and every @Inject constructor in the
 * app ultimately resolves through here.
 *
 * The old MyApplication.java manually registered a
 * SharedPreferences.OnSharedPreferenceChangeListener and re-read a raw
 * "theme" string on every change. Here the Application just collects
 * GetThemeModeUseCase's Flow once; ThemeRepositoryImpl (backed by DataStore)
 * does the "notify observers on change" part for free.
 */
@HiltAndroidApp
class MyApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var getThemeMode: GetThemeModeUseCase

    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    // Lives for the whole process, same as the night-mode setting it applies —
    // there's nothing to cancel it early for, unlike a ViewModel's viewModelScope.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            getThemeMode().collect { mode -> AppCompatDelegate.setDefaultNightMode(mode.toNightMode()) }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
        .build()

    private fun ThemeMode.toNightMode(): Int = when (this) {
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
