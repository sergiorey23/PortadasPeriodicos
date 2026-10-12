package sergirex.portadasperiodicos.data.catalog

/** Builds catalog JSON for tests: [count] valid newspapers (ids p0, p1, ...) plus any [extra] raw entries. */
fun catalogJson(
    version: Int = 1,
    schema: Int = 1,
    count: Int = 12,
    prefix: String = "p",
    extra: List<String> = emptyList()
): String {
    val valid = (0 until count).map {
        """{"id":"$prefix$it","name":"Paper $it","domain":"$prefix$it.es","country":"es","category":"GENERAL"}"""
    }
    return """{"schemaVersion":$schema,"catalogVersion":$version,"periodicos":[${(valid + extra).joinToString(",")}]}"""
}
