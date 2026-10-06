package sergirex.portadasperiodicos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import sergirex.portadasperiodicos.databinding.FragmentPortadaDetalleBinding
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate

@AndroidEntryPoint
class PortadaDetalleFragment : Fragment() {

    private val viewModel: PortadaDetalleViewModel by activityViewModels()

    // Use View Binding for safe and efficient view access
    private var _binding: FragmentPortadaDetalleBinding? = null
    private val binding get() = _binding!!

    // Properties to hold fragment arguments
    private var newspaperTitle: String? = null
    private var countryCode: String? = null
    private var initialDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments passed to the fragment
        arguments?.let {
            newspaperTitle = it.getString(ARG_TITLE)
            countryCode = it.getString(ARG_SIGLA_PAIS)
            initialDate = it.getString(ARG_FECHA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using View Binding
        _binding = FragmentPortadaDetalleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCover()
    }

    /** Reloads the cover for a newly picked date (called from the parent Activity's date picker). */
    fun reloadWithDate(newDate: String) {
        initialDate = newDate
        if (_binding == null) return // no view right now: it loads with the new date when it gets one
        binding.fechaPortada.visibility = View.GONE
        binding.imagenExtendida.visibility = View.GONE
        binding.loadingProgressBar.visibility = View.VISIBLE
        loadCover()
    }

    private fun loadCover() {
        // Launch a coroutine to fetch the cover image in the background
        viewLifecycleOwner.lifecycleScope.launch {
            getCover()
        }
    }

    private suspend fun getCover() {
        val title = newspaperTitle
        val country = countryCode
        val date = initialDate
        if (title == null || country == null || date == null) {
            showError("Información insuficiente para cargar la portada.")
            return
        }

        // The repository remembers which edition each newspaper resolved to, so reopening a
        // cover doesn't re-probe the server day by day; Coil then serves the image itself
        // from its disk cache when it can.
        val coverDate = viewModel.resolveDate(title, country, date)
        val loaded = coverDate?.let { loadImageWithCoil(CoverUrls.full(it, country, title)) } == true

        binding.loadingProgressBar.visibility = View.GONE
        if (coverDate != null && loaded) {
            viewModel.resolvedDates[title] = coverDate
            showDateIfNotToday(coverDate)
            binding.imagenExtendida.visibility = View.VISIBLE
        } else {
            showError("No se pudo encontrar ninguna portada.")
        }
    }

    /** Applies the bitmap manually: `ImageRequest.target()` + `execute()` was found to hang (see [loadBitmap]). */
    private suspend fun loadImageWithCoil(url: String): Boolean {
        val bitmap = requireContext().loadBitmap(url) ?: return false
        binding.imagenExtendida.setImageBitmap(bitmap)
        return true
    }

    private fun showDateIfNotToday(coverDate: String) {
        if (coverDate != EditionDate.today()) {
            binding.fechaPortada.text = EditionDate.toDisplay(coverDate)
            binding.fechaPortada.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.loadingProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Set the ActionBar title
        val formattedTitle = newspaperTitle?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
        (activity as? AppCompatActivity)?.supportActionBar?.title = formattedTitle
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Avoid memory leaks by nullifying the binding reference
        _binding = null
    }

    // Companion object to provide a factory method for creating the fragment
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_SIGLA_PAIS = "sigla_pais"
        private const val ARG_FECHA = "fecha"

        @JvmStatic
        fun newInstance(title: String, siglaPais: String, fecha: String) =
            PortadaDetalleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_SIGLA_PAIS, siglaPais)
                    putString(ARG_FECHA, fecha)
                }
            }
    }
}