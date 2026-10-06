package sergirex.portadasperiodicos.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.model.PortadaCover
import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import javax.inject.Inject

class GetPortadaCoversUseCase @Inject constructor(
    private val repository: PortadaCoverRepository
) {
    operator fun invoke(
        periodicos: List<PeriodicoRef>,
        targetDate: String,
        forceRefresh: Boolean
    ): Flow<PortadaCover> = repository.getCovers(periodicos, targetDate, forceRefresh)
}
