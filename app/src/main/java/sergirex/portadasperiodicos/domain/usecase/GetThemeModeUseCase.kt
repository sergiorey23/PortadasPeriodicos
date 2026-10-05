package sergirex.portadasperiodicos.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.repository.ThemeRepository
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
