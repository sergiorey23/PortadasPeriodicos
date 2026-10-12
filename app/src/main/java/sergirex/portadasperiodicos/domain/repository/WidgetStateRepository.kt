package sergirex.portadasperiodicos.domain.repository

/** Which favorite the home-screen widget is showing. */
interface WidgetStateRepository {
    suspend fun index(): Int

    suspend fun setIndex(index: Int)
}
