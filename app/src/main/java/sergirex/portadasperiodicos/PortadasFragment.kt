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

/**
 * Replaces General/Deportes/Economia/Locales/Internacional/Favoritos.java —
 * six Fragments whose only difference was which newspapers to load. All the
 * loading/caching/retry logic now lives in PortadaCoverRepositoryImpl behind
 * PortadasViewModel; this class only renders whatever PortadasUiState says.
 */
@AndroidEntryPoint
class PortadasFragment : Fragment() {

    private val viewModel: PortadasViewModel by activityViewModels()
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

        val spanCount = if (resources.getBoolean(R.bool.isTablet)) 3 else 2
        adapter = PortadasAdapter(onCoverClick = ::openDetail)
        binding.recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        binding.recyclerView.adapter = adapter
        binding.refreshLayout.setOnRefreshListener { viewModel.refresh(tab) }
        // The grid is wrapped (with the empty-state text) in a FrameLayout, so tell the
        // pull-to-refresh gesture to look at the RecyclerView's scroll position instead.
        binding.refreshLayout.setOnChildScrollUpCallback { _, _ -> binding.recyclerView.canScrollVertically(-1) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState(tab).collect { state ->
                        adapter.submitList(state.covers)
                        binding.refreshLayout.isRefreshing = state.isRefreshing
                        binding.emptyStateText.isVisible = state.showLoadFailed
                    }
                }
                // Fires on every start and whenever the selected date changes.
                launch { viewModel.selectedDate.collect { viewModel.loadIfNeeded(tab) } }
            }
        }
    }

    private fun openDetail(clicked: PortadaCover) {
        val state = viewModel.uiState(tab).value
        val portadas = state.allPeriodicos.map { Portada(it.id, it.domain, it.country, state.targetDate) }
        startActivity(PortadaDetalle.createIntent(requireContext(), portadas, clicked.periodico.id))
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
