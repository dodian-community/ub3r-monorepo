package net.dodian.uber.game.engine.systems.cache

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class CacheUpdateService(
    private val cachePath: Path = Path.of("data/cache"),
) {
    private val logger = LoggerFactory.getLogger(CacheUpdateService::class.java)

    companion object {
        private const val CACHE_HOST = "https://www.exorth.net/dodian/"
        private const val CACHE_NAME = "cache.zip"
        private val REQUIRED_CACHE_ENTRIES = arrayOf(
            "main_file_cache.idx4",
            "main_file_cache.idx5",
            "obj.dat",
            "obj.idx",
            "settings.dat",
            "sprites.dat",
            "sprites.idx",
            "tradable.dat",
            "packed_sprites"
        )
    }

    fun updateIfNecessary(force: Boolean = false) {
        try {
            val currentVersion = getCurrentVersion()
            val latestVersion = getLatestVersion()
            val cachePresent = isCachePresent()

            val shouldUpdate = force || !cachePresent || (latestVersion > 0.0 && latestVersion > currentVersion)

            if (shouldUpdate) {
                logger.info("Cache update required. Current: {}, Latest: {}, Present: {}", currentVersion, latestVersion, cachePresent)
                downloadAndExtract(latestVersion)
                logger.info("Cache updated successfully to version {}", latestVersion)
            } else {
                logger.info("Cache is up to date (version {})", currentVersion)
            }
        } catch (e: Exception) {
            logger.error("Failed to update cache", e)
        }
    }

    private fun isCachePresent(): Boolean {
        for (entry in REQUIRED_CACHE_ENTRIES) {
            val file = cachePath.resolve(entry).toFile()
            if (!file.exists()) {
                return false
            }
            if (entry == "packed_sprites" && !file.isDirectory) {
                return false
            }
        }
        return true
    }

    private fun getCurrentVersion(): Double {
        val versionFile = cachePath.resolve("version.txt").toFile()
        if (!versionFile.exists()) return 0.0
        return try {
            versionFile.readText().trim().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getLatestVersion(): Double {
        return try {
            val url = URL(CACHE_HOST + "version.txt")
            val connection = url.openConnection() as HttpURLConnection
            connection.addRequestProperty("User-Agent", "Mozilla/4.76")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readLine().trim().toDouble() }
            } else {
                // version.txt missing but maybe cache.zip exists?
                if (checkCacheZipExists()) 1.0 else 0.0
            }
        } catch (e: Exception) {
            if (checkCacheZipExists()) 1.0 else 0.0
        }
    }

    private fun checkCacheZipExists(): Boolean {
        return try {
            val url = URL(CACHE_HOST + CACHE_NAME)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.addRequestProperty("User-Agent", "Mozilla/4.76")
            connection.connectTimeout = 5000
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }

    private fun downloadAndExtract(latestVersion: Double) {
        val zipFile = cachePath.resolve(CACHE_NAME)
        Files.createDirectories(cachePath)

        logger.info("Downloading cache from {}{}...", CACHE_HOST, CACHE_NAME)
        URL(CACHE_HOST + CACHE_NAME).openStream().use { input ->
            Files.copy(input, zipFile, StandardCopyOption.REPLACE_EXISTING)
        }

        logger.info("Extracting cache...")
        unzip(zipFile.toFile(), cachePath.toFile())

        // Cleanup zip
        Files.deleteIfExists(zipFile)

        // Update version.txt
        cachePath.resolve("version.txt").toFile().writeText(latestVersion.toString())
    }

    private fun unzip(zipFile: File, destDir: File) {
        ZipArchiveInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextZipEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextZipEntry
            }
        }
    }
}
