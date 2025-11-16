package sergirex.portadasperiodicos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sergirex.portadasperiodicos.databinding.FragmentPortadaDetalleBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PortadaDetalleFragment : Fragment() {

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

        // Launch a coroutine to fetch the cover image in the background
        viewLifecycleOwner.lifecycleScope.launch {
            getCover()
        }
    }

    private suspend fun getCover() {
        // Ensure we have the necessary data to proceed
        if (newspaperTitle == null || countryCode == null || initialDate == null) {
            showError("Información insuficiente para cargar la portada.")
            return
        }

        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE)
        val calendar = Calendar.getInstance()
        try {
            calendar.time = formatter.parse(initialDate!!)!!
        } catch (e: Exception) {
            showError("Fecha inválida.")
            return
        }

        var coverFound = false
        var finalDate: String? = null

        // Try to find the cover, going back up to 20 days
        for (i in 0 until 20) {
            val currentDate = formatter.format(calendar.time)
            val url = "https://img.kiosko.net/$currentDate/$countryCode/$newspaperTitle.jpg"

            // Run the network request on a background thread
            val success = withContext(Dispatchers.IO) {
                loadImageWithPicasso(url)
            }

            if (success) {
                coverFound = true
                finalDate = currentDate
                break // Exit the loop as soon as the cover is found
            } else {
                // If not found, go to the previous day
                calendar.add(Calendar.DATE, -1)
            }
        }

        // Update the UI on the main thread
        binding.loadingProgressBar.visibility = View.GONE
        if (coverFound && finalDate != null) {
            showDateIfNotToday(finalDate)
            binding.imagenExtendida.visibility = View.VISIBLE
        } else {
            showError("No se pudo encontrar ninguna portada.")
        }
    }

    // A helper suspend function to wrap Picasso's callback in a coroutine
    private suspend fun loadImageWithPicasso(url: String): Boolean = suspendCoroutine { continuation ->
        Picasso.get().load(url).into(binding.imagenExtendida, object : com.squareup.picasso.Callback {
            override fun onSuccess() {
                continuation.resume(true) // Resume coroutine with success
            }

            override fun onError(e: Exception?) {
                continuation.resume(false) // Resume coroutine with failure
            }
        })
    }

    private fun showDateIfNotToday(coverDate: String) {
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE)
        val todayCalendar = Calendar.getInstance()
        if (todayCalendar.get(Calendar.HOUR_OF_DAY) < 6) {
            todayCalendar.add(Calendar.DATE, -1)
        }
        val todayDate = formatter.format(todayCalendar.time)

        if (coverDate != todayDate) {
            val dateStr = coverDate.split("/")
            binding.fechaPortada.text = "${dateStr[2]}/${dateStr[1]}/${dateStr[0]}"
            binding.fechaPortada.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        binding.loadingProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        // Optionally, navigate back or show an error image
        parentFragmentManager.popBackStack()
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