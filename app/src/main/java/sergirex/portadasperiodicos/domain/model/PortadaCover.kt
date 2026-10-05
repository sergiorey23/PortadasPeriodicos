package sergirex.portadasperiodicos.domain.model

/**
 * A newspaper's resolved cover: which edition (date) actually has a published
 * image, and the URL to load it from. Deliberately holds no Bitmap — decoding
 * and caching the image itself is a presentation-layer concern (Coil), not a
 * domain one. The old GetPortadas.PortadaResult mixed both: this is the
 * "date resolution" half, the half that's actually business logic.
 */
data class PortadaCover(
    val periodico: PeriodicoRef,
    val resolvedDate: String,
    val imageUrl: String
)
