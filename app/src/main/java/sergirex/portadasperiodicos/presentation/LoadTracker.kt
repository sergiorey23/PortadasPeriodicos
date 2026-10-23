package sergirex.portadasperiodicos.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Knows when a ViewModel's [StateFlow]s hold their real stored values instead of their placeholder defaults, so a
 * screen can wait for [await] before its first frame and never show a switch flip or an entry get disabled afterwards.
 * [expected] is how many flows are created through [track].
 */
class LoadTracker(expected: Int) {
    private val pending = MutableStateFlow(expected)

    suspend fun await() {
        pending.first { it <= 0 }
    }

    /** Like `stateIn(Eagerly, initial)`, counting the first real value as loaded. */
    fun <T> track(scope: CoroutineScope, flow: Flow<T>, initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        scope.launch {
            var first = true
            flow.collect { value ->
                state.value = value
                if (first) {
                    first = false
                    pending.update { it - 1 }
                }
            }
        }
        return state
    }
}
