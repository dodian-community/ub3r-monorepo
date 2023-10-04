package net.dodian.cache

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.dodian.library.extensions.argument
import net.dodian.library.extensions.unzip
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

fun main(args: Array<String>) {
    val revision = (args.argument("revision") ?: "317").split(".")

    CacheDownloader(
        buildMajor = revision[0].toIntOrNull() ?: 317,
        buildMinor = if (revision.size > 1) revision[1].toIntOrNull() else null,
        path = Path(args.argument("path") ?: "./data")
    ).download()
}

/**
 * Downloads the desired cache from https://archive.openrs2.org/.
 */
class CacheDownloader(
    private val buildMajor: Int = 317,
    private val buildMinor: Int? = null,
    private val path: Path = Path("./data"),
    private val baseUrl: String = "https://archive.openrs2.org",
    private val deleteZipAfter: Boolean = true
) {

    private val mapper = ObjectMapper()
        .findAndRegisterModules()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val cacheFile: Path
        get() = if (buildMinor != null)
            path.resolve("cache-$buildMajor.$buildMinor.zip")
        else path.resolve("cache-$buildMajor.zip")

    fun download() {
        if (path.notExists())
            path.createDirectories()

        val caches = mapper.readValue<List<CacheInfo>>(URL("$baseUrl/caches.json"))

        val cacheInfo = caches.filter { it.game == "runescape" && it.builds.isNotEmpty() }.singleOrNull {
            it.builds.any { build ->
                build.major == buildMajor && (build.minor == buildMinor || (buildMinor == null && build.minor == 1))
            }
        } ?: error("Couldn't find cache for build: $buildMajor.$buildMinor.")

        val cacheZipUrl = "$baseUrl/caches/${cacheInfo.scope}/${cacheInfo.id}/disk.zip"
        BufferedInputStream(URL(cacheZipUrl).openStream()).use { input ->
            FileOutputStream(cacheFile.toString()).use { output ->
                val dataBuffer = ByteArray(1024)

                var bytesRead: Int
                while (input.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1)
                    output.write(dataBuffer, 0, bytesRead)

                output.close()
            }

            input.close()
        }

        cacheFile.unzip(path, deleteAfter = deleteZipAfter)
    }
}

data class CacheInfo(
    val id: Int,
    val scope: String,
    val game: String,
    val environment: String,
    val language: String,
    val builds: List<CacheBuildInfo> = emptyList(),
    val timestamp: String? = null,
    val sources: List<String> = emptyList(),
    val size: Long,
    val blocks: Long
)

data class CacheBuildInfo(
    val major: Int,
    val minor: Int? = null
)