package sergirex.portadasperiodicos.domain.repository

import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

/**
 * Source of truth for which newspapers exist. The domain layer only knows
 * this interface — it has no idea the data actually lives in a JSON asset
 * (that's an implementation detail of the data layer), which is what lets
 * UseCases and ViewModels be unit-tested with a fake repository.
 */
interface PeriodicosRepository {
    suspend fun getByCategory(category: PeriodicoCategory): Result<List<Periodico>>
}
