package sergirex.portadasperiodicos.presentation.portadas

import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover

/**
 * Single immutable snapshot the Fragment renders — the whole point of
 * unidirectional data flow: the UI never mutates an adapter imperatively
 * (the old onPortadaLoaded()/adapter.addPortada() callback chain), it just
 * re-renders whenever this changes.
 *
 * [allPeriodicos] is the full requested list (kept even for entries whose
 * cover hasn't resolved yet) so the detail screen's swipe-between-covers
 * ViewPager still gets every candidate, matching the old GetPortadas
 * behavior of handing PortadaResult.allPortadas() the complete input array.
 * [covers] is the subset that has actually resolved so far, which is what
 * the grid displays.
 */
data class PortadasUiState(
    val allPeriodicos: List<PeriodicoRef> = emptyList(),
    val covers: List<PortadaCover> = emptyList(),
    val isRefreshing: Boolean = false
) {
    /** A load finished, there were newspapers to show, and none of their covers could be fetched. */
    val showLoadFailed: Boolean get() = !isRefreshing && allPeriodicos.isNotEmpty() && covers.isEmpty()
}
