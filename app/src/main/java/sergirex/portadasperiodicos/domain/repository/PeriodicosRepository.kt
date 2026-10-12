package sergirex.portadasperiodicos.domain.repository

import kotlinx.coroutines.flow.StateFlow
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

    /** The catalog entry with this id, or null if it isn't (or no longer is) in the catalog. */
    suspend fun getById(id: String): Periodico?

    /** Every newspaper in the catalog (empty only if the catalog couldn't be read at all). */
    suspend fun getAll(): List<Periodico>

    /** Increases whenever the catalog content changes (a newer copy was downloaded). */
    val catalogVersion: StateFlow<Int>

    /**
     * Checks the published catalog and adopts it if it is newer and valid. Skipped when checked recently
     * (unless [force]). Never throws: if anything goes wrong the current catalog stays. Returns true if it changed.
     */
    suspend fun refresh(force: Boolean = false): Boolean
}
