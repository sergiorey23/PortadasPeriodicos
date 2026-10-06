package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoritePeriodicosRepository
) {
    suspend operator fun invoke(periodico: PeriodicoRef) = repository.toggle(periodico)
}
