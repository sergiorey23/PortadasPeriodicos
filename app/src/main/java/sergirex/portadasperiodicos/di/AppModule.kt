package sergirex.portadasperiodicos.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import sergirex.portadasperiodicos.data.network.CoverCacheControlInterceptor
import sergirex.portadasperiodicos.data.repository.EngagementRepositoryImpl
import sergirex.portadasperiodicos.data.repository.FavoritePeriodicosRepositoryImpl
import sergirex.portadasperiodicos.data.repository.PeriodicosRepositoryImpl
import sergirex.portadasperiodicos.data.repository.PortadaCoverRepositoryImpl
import sergirex.portadasperiodicos.data.repository.SettingsRepositoryImpl
import sergirex.portadasperiodicos.data.repository.ThemeRepositoryImpl
import sergirex.portadasperiodicos.domain.repository.EngagementRepository
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import sergirex.portadasperiodicos.domain.repository.ThemeRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Wires domain-layer interfaces to their data-layer implementations. This is
 * the one place in the app that knows a concrete PeriodicosRepositoryImpl or
 * ThemeRepositoryImpl exists — everything else (UseCases, ViewModels) depends
 * only on the PeriodicosRepository/ThemeRepository interfaces, which is what
 * makes them testable with a fake/in-memory implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPeriodicosRepository(impl: PeriodicosRepositoryImpl): PeriodicosRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindPortadaCoverRepository(impl: PortadaCoverRepositoryImpl): PortadaCoverRepository

    @Binds
    @Singleton
    abstract fun bindFavoritePeriodicosRepository(impl: FavoritePeriodicosRepositoryImpl): FavoritePeriodicosRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindEngagementRepository(impl: EngagementRepositoryImpl): EngagementRepository

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }

        /** Shared by Coil (image downloads) and the cover-date probes, so both reuse connections. */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                // Covers all come from one host; the default of 5 concurrent requests per host made a
                // tab wait behind another tab's lookups and thumbnails.
                .dispatcher(Dispatcher().apply { maxRequestsPerHost = 12 })
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .addNetworkInterceptor(CoverCacheControlInterceptor())
                .build()
    }
}
