package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.repository.PortadaCoverRepository
import javax.inject.Inject

class ResolveCoverDateUseCase @Inject constructor(
    private val repository: PortadaCoverRepository
) {
    suspend operator fun invoke(id: String, country: String, targetDate: String): String? =
        repository.resolveDate(id, country, targetDate)
}
