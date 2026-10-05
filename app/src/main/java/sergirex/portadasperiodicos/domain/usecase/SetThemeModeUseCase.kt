package sergirex.portadasperiodicos.domain.usecase

import sergirex.portadasperiodicos.domain.model.ThemeMode
import sergirex.portadasperiodicos.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
