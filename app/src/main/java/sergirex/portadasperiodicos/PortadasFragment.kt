package sergirex.portadasperiodicos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.PortadaLayoutBinding
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.domain.model.toLegacyEncodedString
import sergirex.portadasperiodicos.presentation.portadas.PortadasViewModel

/**
 * Replaces General/Deportes/Economia/Locales/Internacional/Favoritos.java —
 * six Fragments whose only difference was which newspapers to load. All the
 * loading/caching/retry logic now lives in PortadaCoverRepositoryImpl behind
 * PortadasViewModel; this class only renders whatever PortadasUiState says.
 */
@AndroidEntryPoint
class PortadasFragment : Fragment() {

    private val viewModel: PortadasViewModel by viewModels()
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
        binding.refreshLayout.setOnRefreshListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.covers)
                    binding.refreshLayout.isRefreshing = state.isRefreshing
                }
            }
        }

        viewModel.loadIfNeeded()
    }

    private fun openDetail(clicked: PortadaCover) {
        val state = viewModel.uiState.value
        val intent = Intent(requireContext(), PortadaDetalle::class.java).apply {
            putExtra(EXTRA_PORTADAS, state.allPeriodicos.map { it.toLegacyEncodedString() }.toTypedArray())
            putExtra(EXTRA_SELECTED_PORTADA, clicked.periodico.id)
            putExtra(EXTRA_FECHA, clicked.resolvedDate)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val EXTRA_PORTADAS = "Portadas"
        private const val EXTRA_SELECTED_PORTADA = "selectedPortada"
        private const val EXTRA_FECHA = "Fecha"

        @JvmStatic
        fun newInstance(category: PeriodicoCategory): PortadasFragment = PortadasFragment().apply {
            arguments = Bundle().apply { putString(PortadasViewModel.ARG_CATEGORY, category.name) }
        }

        @JvmStatic
        fun newInstanceFavorites(): PortadasFragment = PortadasFragment()
    }
}
