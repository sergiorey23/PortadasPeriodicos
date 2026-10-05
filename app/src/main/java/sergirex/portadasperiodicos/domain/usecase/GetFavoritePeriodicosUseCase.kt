package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject

class GetFavoritePeriodicosUseCase @Inject constructor(
    private val repository: FavoritePeriodicosRepository
) {
    operator fun invoke(): List<PeriodicoRef> = repository.getFavorites()
}
