package sergirex.portadasperiodicos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.size.Dimension
import coil3.size.Size
import sergirex.portadasperiodicos.domain.model.CoverUrls
import sergirex.portadasperiodicos.domain.model.EditionDate
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
    private val columnWidth: () -> Int,
    private val onCoverClick: (PortadaCover, View) -> Unit
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
            binding.portadaImageView.contentDescription = cover.periodico.name
            // An explicit size, not Coil measuring the wrap_content ImageView (that left recreated pages blank).
            // Decoding at the column width instead of the full thumbnail saves memory with 3 columns or on
            // tablets; capped at the thumbnail's own width so a single column isn't upscaled.
            val width = columnWidth().coerceIn(1, CoverUrls.THUMBNAIL_WIDTH)
            binding.portadaImageView.load(cover.imageUrl) { size(Size(Dimension(width), Dimension.Undefined)) }
            // Judged against the day the cover was asked for: a picked date is named in the toolbar, so its covers need no ribbon.
            val isOld = cover.resolvedDate != cover.requestedDate
            binding.dateRibbon.isVisible = isOld
            if (isOld) binding.dateRibbon.text = EditionDate.toRibbonDisplay(cover.resolvedDate)
            binding.root.setOnClickListener { onCoverClick(cover, binding.root) }
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
