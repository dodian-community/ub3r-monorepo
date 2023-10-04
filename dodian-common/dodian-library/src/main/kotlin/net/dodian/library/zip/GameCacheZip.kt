package net.dodian.library.zip

import com.github.michaelbull.logging.InlineLogger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.streams.asSequence


private val logger = InlineLogger()

class GameCacheZip(
    val cachePath: Path = Path("./data/cache/packed"),
    val tempZipPath: Path = Path("./data/temp/game_cache.zip")
) {

    private fun tempZipExists() = Files.exists(tempZipPath)

    // TODO: need to implement a way to check whether the ZIP is up to date or not.
    private fun tempZipUpToDate() = true

    private fun cacheExists() = Files.exists(cachePath.resolve("main_file_cache.dat2"))

    private fun zip(): ZipArchive? {
        if (!cacheExists()) {
            logger.error { "Cache doesn't exist at: ${cachePath.toAbsolutePath()}" }
            return null
        }

        val files = Files.walk(cachePath).map { it.toFile() }
            .filter { it.extension.isNotEmpty() }.asSequence().toList()

        return ZipArchive().withFiles(files)
    }

    fun createTempZip() {
        if (tempZipExists() && tempZipUpToDate()) {
            logger.debug { "Temporary ZIP file already exists and is up to date at: ${tempZipPath.toAbsolutePath()}" }
            return
        }

        val archive = zip()
        if (archive == null) {
            logger.debug { "ZIP data was null, ensure that the cache files are present." }
            return
        }

        archive.saveTo(tempZipPath)
        logger.info { "Successfully created a temporary cache ZIP at: ${tempZipPath.toAbsolutePath()}" }
    }

    fun bytes(): ByteArray = tempZipPath.toFile().readBytes()
}
