package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject

class ClearFavoritesUseCase @Inject constructor(
    private val repository: FavoritePeriodicosRepository
) {
    suspend operator fun invoke() = repository.clear()
}
