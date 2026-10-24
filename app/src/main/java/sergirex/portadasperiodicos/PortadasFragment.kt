package sergirex.portadasperiodicos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.PortadaLayoutBinding
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.presentation.portadas.PortadasViewModel
import sergirex.portadasperiodicos.presentation.settings.SettingsViewModel
import sergirex.portadasperiodicos.ui.ActivityTransitions

/**
 * Replaces General/Deportes/Economia/Locales/Internacional/Favoritos.java —
 * six Fragments whose only difference was which newspapers to load. All the
 * loading/caching/retry logic now lives in PortadaCoverRepositoryImpl behind
 * PortadasViewModel; this class only renders whatever PortadasUiState says.
 */
@AndroidEntryPoint
class PortadasFragment : Fragment() {

    private val viewModel: PortadasViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private val tab: HomeTab by lazy { HomeTab.fromKey(requireArguments().getString(ARG_TAB)) }
    private var _binding: PortadaLayoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PortadasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PortadaLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val grid = GridLayoutManager(context, defaultSpanCount())
        adapter = PortadasAdapter(
            columnWidth = { (binding.recyclerView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels) / grid.spanCount },
            onCoverClick = ::openDetail
        )
        binding.recyclerView.layoutManager = grid
        binding.recyclerView.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { viewModel.refresh(tab) }

        // The user can pick 1 to 3 columns in Settings; 0 keeps the screen-size default.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.gridColumns.collect { columns ->
                    grid.spanCount = if (columns in 1..3) columns else defaultSpanCount()
                }
            }
        }

        // Render whenever the page is on screen or being swiped to...
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState(tab).collect { state ->
                    adapter.submitList(state.covers)
                    binding.refreshLayout.isRefreshing = state.isRefreshing
                    binding.emptyStateText.isVisible = state.showLoadFailed
                }
            }
        }
        // ...but only fetch for the page the user is actually on (RESUMED). Pages that are merely
        // passed through when jumping straight to a far tab are STARTED only; fetching for them would
        // queue their requests ahead of the tab the user chose.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Fires on every resume and whenever the selected date changes.
                viewModel.selectedDate.collect { viewModel.loadIfNeeded(tab) }
            }
        }
    }

    private fun defaultSpanCount() = if (resources.getBoolean(R.bool.isTablet)) 3 else 2

    private fun openDetail(clicked: PortadaCover, cell: View) {
        val state = viewModel.uiState(tab).value
        val portadas = state.allPeriodicos.map { Portada(it.id, it.name, it.domain, it.country, state.targetDate) }
        ActivityTransitions.launchFrom(requireActivity(), PortadaDetalle.createIntent(requireContext(), portadas, clicked.periodico.id), cell)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAB = "tab"

        fun newInstance(tab: HomeTab): PortadasFragment = PortadasFragment().apply {
            arguments = Bundle().apply { putString(ARG_TAB, tab.key) }
        }
    }
}
