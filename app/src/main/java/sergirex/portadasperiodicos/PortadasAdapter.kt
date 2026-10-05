package sergirex.portadasperiodicos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import sergirex.portadasperiodicos.databinding.PortadaItemBinding
import sergirex.portadasperiodicos.domain.model.PortadaCover

/**
 * ListAdapter (DiffUtil-backed) over a single immutable List<PortadaCover>
 * snapshot from PortadasViewModel's StateFlow, replacing the old mutable
 * internal list with imperative addPortada()/clear() calls driven by
 * GetPortadas' listener callbacks. Image loading is now Coil's job
 * (memory+disk caching included) instead of the Bitmap each PortadaResult
 * used to carry pre-decoded.
 */
class PortadasAdapter(
    private val onCoverClick: (PortadaCover) -> Unit
) : ListAdapter<PortadaCover, PortadasAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PortadaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: PortadaItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cover: PortadaCover) {
            binding.portadaImageView.load(cover.imageUrl)
            binding.root.setOnClickListener { onCoverClick(cover) }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PortadaCover>() {
            override fun areItemsTheSame(oldItem: PortadaCover, newItem: PortadaCover) =
                oldItem.periodico.id == newItem.periodico.id

            override fun areContentsTheSame(oldItem: PortadaCover, newItem: PortadaCover) =
                oldItem == newItem
        }
    }
}
