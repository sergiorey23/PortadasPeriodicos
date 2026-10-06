package sergirex.portadasperiodicos.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.PeriodicoRef
import sergirex.portadasperiodicos.domain.repository.FavoritePeriodicosRepository
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritePeriodicosRepository
) {
    operator fun invoke(): Flow<List<PeriodicoRef>> = repository.favorites
}
