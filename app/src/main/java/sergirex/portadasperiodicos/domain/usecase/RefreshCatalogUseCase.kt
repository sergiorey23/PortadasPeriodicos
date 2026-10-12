package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import javax.inject.Inject

class RefreshCatalogUseCase @Inject constructor(
    private val repository: PeriodicosRepository
) {
    suspend operator fun invoke(force: Boolean = false): Boolean = repository.refresh(force)
}
