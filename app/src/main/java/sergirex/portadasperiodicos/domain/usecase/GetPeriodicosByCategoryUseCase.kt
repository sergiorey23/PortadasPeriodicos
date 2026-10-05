package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.model.Periodico
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory
import sergirex.portadasperiodicos.domain.repository.PeriodicosRepository
import javax.inject.Inject

/**
 * Single-responsibility interactor: "give me the newspapers for this category".
 * A thin wrapper today, but it's the seam where future business rules (e.g.
 * filtering by the user's region, or sorting by popularity) land without
 * touching the repository or any ViewModel.
 */
class GetPeriodicosByCategoryUseCase @Inject constructor(
    private val repository: PeriodicosRepository
) {
    suspend operator fun invoke(category: PeriodicoCategory): Result<List<Periodico>> =
        repository.getByCategory(category)
}
