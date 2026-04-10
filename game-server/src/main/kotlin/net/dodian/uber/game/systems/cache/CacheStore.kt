package net.dodian.uber.game.systems.cache

import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

class CacheStore(
    private val root: Path,
) : Closeable {
    private var opened = false
    private var dataFile: RandomAccessFile? = null
    private var indexFiles: List<RandomAccessFile> = emptyList()

    @Synchronized
    fun open(): CacheStore {
        if (opened) {
            return this
        }

        if (!Files.isDirectory(root)) {
            opened = true
            return this
        }

        val dataPath = root.resolve(DATA_FILE_NAME)
        if (!Files.isRegularFile(dataPath)) {
            opened = true
            return this
        }

        dataFile = RandomAccessFile(dataPath.toFile(), "r")

        val discoveredIndexes = ArrayList<RandomAccessFile>()
        var index = 0
        while (true) {
            val indexPath = root.resolve(INDEX_FILE_PREFIX + index)
            if (!Files.isRegularFile(indexPath)) {
                break
            }
            discoveredIndexes += RandomAccessFile(indexPath.toFile(), "r")
            index++
        }

        indexFiles = discoveredIndexes
        opened = true
        return this
    }

    fun isAvailable(): Boolean =
        Files.isDirectory(root) &&
            Files.isRegularFile(root.resolve(DATA_FILE_NAME)) &&
            Files.isRegularFile(root.resolve(INDEX_FILE_PREFIX + 0))

    fun describe(): String = root.toAbsolutePath().normalize().toString()

    fun hasFile(relativePath: String): Boolean =
        if (relativePath == MAP_INDEX_NAME) {
            readMapIndex() != null
        } else {
            Files.isRegularFile(resolve(relativePath))
        }

    @Synchronized
    fun readFile(relativePath: String): ByteArray? {
        if (!opened) {
            open()
        }

        val path = resolve(relativePath)
        if (!Files.isRegularFile(path)) {
            return null
        }
        return Files.readAllBytes(path)
    }

    @Synchronized
    fun readStoreFile(cache: Int, file: Int): ByteArray? {
        if (!opened) {
            open()
        }

        val data = dataFile ?: return null
        if (cache < 0 || file < 0 || cache >= indexFiles.size) {
            return null
        }

        val index = indexFiles[cache]
        val indexOffset = file.toLong() * INDEX_ENTRY_SIZE
        if (indexOffset + INDEX_ENTRY_SIZE > index.length()) {
            return null
        }

        val indexBuffer = ByteArray(INDEX_ENTRY_SIZE.toInt())
        index.seek(indexOffset)
        index.readFully(indexBuffer)

        val fileSize = CacheUtils.readMedium(indexBuffer, 0)
        var sector = CacheUtils.readMedium(indexBuffer, 3)
        val maxSector = data.length() / DATA_SECTOR_SIZE
        if (fileSize < 0 || sector <= 0 || sector > maxSector) {
            return null
        }

        val payload = ByteArray(fileSize)
        var bytesRead = 0
        var part = 0
        val expectedStoreId = cache + 1

        val sectorBuffer = ByteArray(DATA_SECTOR_SIZE.toInt())
        while (bytesRead < fileSize) {
            if (sector == 0L.toInt()) {
                return null
            }

            val unread = minOf(DATA_BLOCK_SIZE, fileSize - bytesRead)
            data.seek(sector.toLong() * DATA_SECTOR_SIZE)
            data.readFully(sectorBuffer, 0, DATA_HEADER_SIZE + unread)

            val currentFile = CacheUtils.readUnsignedShort(sectorBuffer, 0)
            val currentPart = CacheUtils.readUnsignedShort(sectorBuffer, 2)
            val nextSector = CacheUtils.readMedium(sectorBuffer, 4)
            val currentStore = sectorBuffer[7].toInt() and 0xFF

            if (currentFile != file || currentPart != part || currentStore != expectedStoreId) {
                return null
            }
            if (nextSector < 0 || nextSector > maxSector) {
                return null
            }

            System.arraycopy(sectorBuffer, DATA_HEADER_SIZE, payload, bytesRead, unread)
            bytesRead += unread
            sector = nextSector
            part++
        }

        return payload
    }

    @Synchronized
    fun readArchiveFile(cache: Int, file: Int, name: String): ByteArray? {
        val archiveBytes = readStoreFile(cache, file) ?: return null
        val archive = Archive.decode(archiveBytes) ?: return null
        return archive.readFile(name)
    }

    fun readMapIndex(): ByteArray? = readArchiveFile(VERSION_LIST_STORE, VERSION_LIST_FILE, MAP_INDEX_NAME)

    private fun resolve(relativePath: String): Path = root.resolve(relativePath)

    @Synchronized
    override fun close() {
        dataFile?.close()
        indexFiles.forEach(RandomAccessFile::close)
        dataFile = null
        indexFiles = emptyList()
        opened = false
    }

    private companion object {
        const val INDEX_ENTRY_SIZE = 6L
        const val DATA_BLOCK_SIZE = 512
        const val DATA_HEADER_SIZE = 8
        const val DATA_SECTOR_SIZE = 520L
        const val VERSION_LIST_STORE = 0
        const val VERSION_LIST_FILE = 5
        const val MAP_INDEX_NAME = "map_index"
        const val DATA_FILE_NAME = "main_file_cache.dat"
        const val INDEX_FILE_PREFIX = "main_file_cache.idx"
    }
}
