package sergirex.portadasperiodicos.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.EditionDate
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import sergirex.portadasperiodicos.domain.usecase.GetFavoritePeriodicosUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPeriodicosByCategoryUseCase
import sergirex.portadasperiodicos.domain.usecase.GetPortadaCoversUseCase
import sergirex.portadasperiodicos.domain.usecase.ObserveFavoritesUseCase
import sergirex.portadasperiodicos.presentation.portadas.PortadasViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PortadasViewModelTest {

    private val general = HomeTab.Category(PeriodicoCategory.GENERAL)
    private val favoritesFlow = MutableStateFlow<List<PeriodicoRef>>(emptyList())
    private val coverRequests = mutableListOf<String>() // the target date of each load

    private val catalog = object : PeriodicosRepository {
        override suspend fun getById(id: String): Periodico? = null
        override suspend fun getByCategory(category: PeriodicoCategory) =
            Result.success(listOf(Periodico("elpais", "El País", "elpais.com", "es", category), Periodico("abc", "ABC", "abc.es", "es", category)))
    }
    private val favorites = object : FavoritePeriodicosRepository {
        override val favorites: Flow<List<PeriodicoRef>> = favoritesFlow
        override suspend fun toggle(periodico: PeriodicoRef) = Unit
    }
    private val covers = object : PortadaCoverRepository {
        override fun getCovers(periodicos: List<PeriodicoRef>, targetDate: String, forceRefresh: Boolean): Flow<PortadaCover> {
            coverRequests += targetDate
            return flow { periodicos.forEach { emit(PortadaCover(it, targetDate, "url/${it.id}")) } }
        }
        override suspend fun resolveDate(id: String, country: String, targetDate: String, forceRefresh: Boolean) = targetDate
    }
    private val settings = object : SettingsRepository {
        override val initialTab = flowOf(HomeTab.default)
        override val dailyNotificationEnabled = flowOf(true)
        override val notificationPermissionRequested = flowOf(false)
        override suspend fun setInitialTab(tab: HomeTab) = Unit
        override suspend fun setDailyNotificationEnabled(enabled: Boolean) = Unit
        override suspend fun setNotificationPermissionRequested() = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PortadasViewModel(
        GetPeriodicosByCategoryUseCase(catalog),
        GetFavoritePeriodicosUseCase(favorites),
        GetPortadaCoversUseCase(covers),
        settings,
        ObserveFavoritesUseCase(favorites)
    )

    @Test
    fun `a tab loads once and stays loaded`() = runTest {
        val vm = viewModel()
        vm.loadIfNeeded(general)
        advanceUntilIdle()
        vm.loadIfNeeded(general)
        advanceUntilIdle()

        assertEquals(listOf("elpais", "abc"), vm.uiState(general).value.covers.map { it.periodico.id })
        assertEquals(1, coverRequests.size)
    }

    @Test
    fun `selecting a date reloads every tab for that date, and clearing it goes back to today`() = runTest {
        val vm = viewModel()
        vm.loadIfNeeded(general)
        advanceUntilIdle()

        vm.selectDate("2020/01/02")
        vm.loadIfNeeded(general)
        advanceUntilIdle()
        assertEquals("2020/01/02", vm.uiState(general).value.targetDate)
        assertEquals("2020/01/02", vm.uiState(general).value.covers.first().resolvedDate)

        vm.selectDate(null)
        vm.loadIfNeeded(general)
        advanceUntilIdle()
        assertEquals(EditionDate.today(), vm.uiState(general).value.targetDate)
    }

    @Test
    fun `a screen that reloads as soon as the date changes is not cancelled by the change itself`() = runTest {
        val vm = viewModel()
        vm.loadIfNeeded(general)
        advanceUntilIdle()

        // Like the Fragments: collect the date with an immediate dispatcher and reload right away.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.selectedDate.collect { vm.loadIfNeeded(general) }
        }
        advanceUntilIdle()

        vm.selectDate("2020/01/02")
        advanceUntilIdle()

        assertEquals("2020/01/02", vm.uiState(general).value.covers.first().resolvedDate)
    }

    @Test
    fun `favorites tab appears only while there is a favorite, and its content follows the list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(HomeTab.categories, vm.tabs.value)

        favoritesFlow.value = listOf(PeriodicoRef("elpais", "El País", "elpais.com", "es"))
        advanceUntilIdle()
        assertEquals(listOf(HomeTab.Favorites) + HomeTab.categories, vm.tabs.value)

        vm.loadIfNeeded(HomeTab.Favorites)
        advanceUntilIdle()
        assertEquals(listOf("elpais"), vm.uiState(HomeTab.Favorites).value.covers.map { it.periodico.id })

        favoritesFlow.value = listOf(PeriodicoRef("elpais", "El País", "elpais.com", "es"), PeriodicoRef("abc", "ABC", "abc.es", "es"))
        vm.loadIfNeeded(HomeTab.Favorites)
        advanceUntilIdle()
        assertEquals(listOf("elpais", "abc"), vm.uiState(HomeTab.Favorites).value.covers.map { it.periodico.id })

        favoritesFlow.value = emptyList()
        advanceUntilIdle()
        assertEquals(HomeTab.categories, vm.tabs.value)
    }

    @Test
    fun `a load that finds nothing is retried next time and flags the failure`() = runTest {
        val failing = object : PortadaCoverRepository by covers {
            override fun getCovers(periodicos: List<PeriodicoRef>, targetDate: String, forceRefresh: Boolean): Flow<PortadaCover> {
                coverRequests += targetDate
                return emptyFlow()
            }
        }
        val vm = PortadasViewModel(
            GetPeriodicosByCategoryUseCase(catalog), GetFavoritePeriodicosUseCase(favorites),
            GetPortadaCoversUseCase(failing), settings, ObserveFavoritesUseCase(favorites)
        )
        vm.loadIfNeeded(general)
        advanceUntilIdle()
        assertTrue(vm.uiState(general).value.showLoadFailed)

        vm.loadIfNeeded(general)
        advanceUntilIdle()
        assertEquals(2, coverRequests.size)
    }
}
