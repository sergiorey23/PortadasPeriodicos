package sergirex.portadasperiodicos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import sergirex.portadasperiodicos.domain.model.PeriodicoRef

/** One newspaper in the detail screen's pager, plus the edition date it was asked to show. */
@Parcelize
data class Portada(
    val id: String,
    val domain: String,
    val country: String,
    var fecha: String
) : Parcelable {
    fun toRef() = PeriodicoRef(id, domain, country)
}
