package sergirex.portadasperiodicos.data.catalog

import java.io.File

/**
 * The last good copy of the remote catalog, kept in a private folder with the ETag it came with and the
 * time it was last checked. Writes go to a temporary file first so a crash or full disk can never leave
 * a half-written catalog behind.
 */
class CatalogCache(private val dir: File) {
    private val catalogFile get() = File(dir, "catalog.json")
    private val etagFile get() = File(dir, "catalog.etag")
    private val checkedFile get() = File(dir, "catalog.checked")

    fun readCatalog(): String? = readOrNull(catalogFile)
    fun readEtag(): String? = readOrNull(etagFile)?.takeIf { it.isNotBlank() }
    fun lastCheckedAt(): Long = readOrNull(checkedFile)?.trim()?.toLongOrNull() ?: 0L

    fun saveCatalog(text: String, etag: String?) {
        write(catalogFile, text)
        if (etag != null) write(etagFile, etag) else etagFile.delete()
    }

    fun markChecked(now: Long) = write(checkedFile, now.toString())

    private fun readOrNull(file: File): String? = runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()

    private fun write(target: File, text: String) {
        dir.mkdirs()
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "Couldn't replace ${target.name}" }
        }
    }
}
