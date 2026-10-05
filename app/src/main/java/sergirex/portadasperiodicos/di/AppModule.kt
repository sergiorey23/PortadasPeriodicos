package sergirex.portadasperiodicos.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import sergirex.portadasperiodicos.data.repository.PeriodicosRepositoryImpl
import sergirex.portadasperiodicos.data.repository.ThemeRepositoryImpl
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.ThemeRepository
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

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = Json { ignoreUnknownKeys = true }
    }
}
