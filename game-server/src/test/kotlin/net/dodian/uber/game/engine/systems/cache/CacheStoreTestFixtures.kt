package net.dodian.uber.game.engine.systems.cache

import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

object CacheStoreTestFixtures {
    data class TileCoordinate(
        val x: Int,
        val y: Int,
        val plane: Int,
    )

    data class FixtureMapObject(
        val id: Int,
        val x: Int,
        val y: Int,
        val plane: Int,
        val type: Int,
        val rotation: Int,
    )

    data class FixtureObjectDefinition(
        val id: Int,
        val name: String = "null",
        val description: String = "null",
        val sizeX: Int = 1,
        val sizeY: Int = 1,
        val solid: Boolean = true,
        val interactive: Boolean? = null,
        val interactions: List<String> = emptyList(),
        val hollow: Boolean = false,
        val supportItems: Int? = null,
        val rawOpcodes: ByteArray = byteArrayOf(),
    )

    fun createMapIndex(regionId: Int, landscapeArchiveId: Int, objectArchiveId: Int, priority: Boolean = false): ByteArray =
        ByteArrayOutputStream().use { output ->
            writeUnsignedShort(output, 1)
            writeUnsignedShort(output, regionId)
            writeUnsignedShort(output, landscapeArchiveId)
            writeUnsignedShort(output, objectArchiveId)
            output.write(if (priority) 1 else 0)
            output.toByteArray()
        }

    fun createVersionListArchive(vararg entries: Pair<String, ByteArray>): ByteArray {
        val archiveBody = ByteArrayOutputStream().use { output ->
            writeUnsignedShort(output, entries.size)
            entries.forEach { (name, bytes) ->
                writeInt(output, CacheUtils.hash(name))
                writeMedium(output, bytes.size)
                writeMedium(output, bytes.size)
            }
            entries.forEach { (_, bytes) ->
                output.write(bytes)
            }
            output.toByteArray()
        }

        val compressedBody = compressArchiveBody(archiveBody)
        return ByteArrayOutputStream().use { output ->
            writeMedium(output, archiveBody.size)
            writeMedium(output, compressedBody.size)
            output.write(compressedBody)
            output.toByteArray()
        }
    }

    fun writeStoreFile(root: Path, cache: Int, file: Int, payload: ByteArray) {
        Files.createDirectories(root)
        val dataPath = root.resolve("main_file_cache.dat")
        for (index in 0..cache) {
            val path = root.resolve("main_file_cache.idx$index")
            if (!Files.exists(path)) {
                Files.createFile(path)
            }
        }
        val indexPath = root.resolve("main_file_cache.idx$cache")

        RandomAccessFile(dataPath.toFile(), "rw").use { dataFile ->
            if (dataFile.length() == 0L) {
                dataFile.setLength(520L)
            }

            RandomAccessFile(indexPath.toFile(), "rw").use { indexFile ->
                val firstSector = nextFreeSector(dataFile)
                indexFile.seek(file.toLong() * 6L)
                writeMedium(indexFile, payload.size)
                writeMedium(indexFile, firstSector)

                var written = 0
                var sector = firstSector
                var part = 0
                while (written < payload.size) {
                    val chunk = minOf(512, payload.size - written)
                    val nextSector = if (written + chunk >= payload.size) 0 else sector + 1
                    dataFile.seek(sector.toLong() * 520L)
                    dataFile.writeShort(file)
                    dataFile.writeShort(part)
                    writeMedium(dataFile, nextSector)
                    dataFile.writeByte(cache + 1)
                    dataFile.write(payload, written, chunk)
                    written += chunk
                    sector = nextSector
                    part++
                }
            }
        }
    }

    fun writeVersionListArchive(root: Path, mapIndex: ByteArray) {
        writeStoreFile(root, cache = 0, file = 5, payload = createVersionListArchive("map_index" to mapIndex))
    }

    fun createObjectDefinitionFiles(definitions: List<FixtureObjectDefinition>): Pair<ByteArray, ByteArray> {
        val sorted = definitions.sortedBy { it.id }
        val maxId = sorted.maxOfOrNull { it.id } ?: -1
        val byId = sorted.associateBy { it.id }

        val locDat = ByteArrayOutputStream().use { dat ->
            writeUnsignedShort(dat, 0)
            val lengths = ArrayList<Int>(maxId + 1)
            for (id in 0..maxId) {
                val encoded = encodeObjectDefinition(byId[id])
                lengths += encoded.size
                dat.write(encoded)
            }
            val locIdx = ByteArrayOutputStream().use { idx ->
                writeUnsignedShort(idx, maxId + 1)
                lengths.forEach { writeUnsignedShort(idx, it) }
                idx.toByteArray()
            }
            dat.toByteArray() to locIdx
        }
        return locDat
    }

    fun writeObjectDefinitionArchive(root: Path, definitions: List<FixtureObjectDefinition>) {
        val (locDat, locIdx) = createObjectDefinitionFiles(definitions)
        writeStoreFile(
            root,
            cache = 0,
            file = 2,
            payload = createVersionListArchive("loc.dat" to locDat, "loc.idx" to locIdx),
        )
    }

    fun createLandscapeArchive(customTiles: Map<TileCoordinate, ByteArray> = emptyMap()): ByteArray =
        gzip(
            ByteArrayOutputStream().use { output ->
                for (plane in 0 until 4) {
                    for (x in 0 until 64) {
                        for (y in 0 until 64) {
                            val data = customTiles[TileCoordinate(x, y, plane)] ?: byteArrayOf(0)
                            output.write(data)
                        }
                    }
                }
                output.toByteArray()
            },
        )

    fun createObjectArchive(objects: List<FixtureMapObject>): ByteArray =
        gzip(
            ByteArrayOutputStream().use { output ->
                var previousId = -1
                for (obj in objects.sortedBy { it.id }) {
                    writeUnsignedSmartCumulative(output, obj.id - previousId)
                    previousId = obj.id

                    val positionData = (obj.plane shl 12) or (obj.x shl 6) or obj.y
                    writeUnsignedSmart(output, positionData + 1)
                    output.write((obj.type shl 2) or (obj.rotation and 0x3))
                    output.write(0)
                }
                output.write(0)
                output.toByteArray()
            },
        )

    fun writeRegionArchives(
        root: Path,
        regionId: Int,
        landscapeArchiveId: Int,
        objectArchiveId: Int,
        landscapeArchive: ByteArray,
        objectArchive: ByteArray,
        priority: Boolean = false,
    ) {
        writeVersionListArchive(
            root,
            createMapIndex(
                regionId = regionId,
                landscapeArchiveId = landscapeArchiveId,
                objectArchiveId = objectArchiveId,
                priority = priority,
            ),
        )
        writeStoreFile(root, cache = 4, file = landscapeArchiveId, payload = landscapeArchive)
        writeStoreFile(root, cache = 4, file = objectArchiveId, payload = objectArchive)
    }

    private fun compressArchiveBody(data: ByteArray): ByteArray =
        ByteArrayOutputStream().use { output ->
            BZip2CompressorOutputStream(output, 1).use { bzip ->
                bzip.write(data)
            }
            output.toByteArray().copyOfRange(4, output.size())
        }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().use { output ->
            GZIPOutputStream(output).use { gzip ->
                gzip.write(data)
            }
            output.toByteArray()
        }

    private fun nextFreeSector(dataFile: RandomAccessFile): Int {
        var sector = ((dataFile.length() + 519L) / 520L).toInt()
        if (sector == 0) {
            sector = 1
        }
        return sector
    }

    private fun writeUnsignedShort(output: ByteArrayOutputStream, value: Int) {
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun writeString(output: ByteArrayOutputStream, value: String) {
        output.write(value.toByteArray(Charsets.ISO_8859_1))
        output.write(10)
    }

    private fun encodeObjectDefinition(definition: FixtureObjectDefinition?): ByteArray {
        if (definition == null) {
            return byteArrayOf(0)
        }
        return ByteArrayOutputStream().use { output ->
            if (definition.rawOpcodes.isNotEmpty()) {
                output.write(definition.rawOpcodes)
            }
            if (definition.name != "null") {
                output.write(2)
                writeString(output, definition.name)
            }
            if (definition.description != "null") {
                output.write(3)
                writeString(output, definition.description)
            }
            if (definition.sizeX != 1) {
                output.write(14)
                output.write(definition.sizeX)
            }
            if (definition.sizeY != 1) {
                output.write(15)
                output.write(definition.sizeY)
            }
            if (!definition.solid) {
                output.write(17)
            }
            definition.interactive?.let {
                output.write(19)
                output.write(if (it) 1 else 0)
            }
            definition.interactions.take(5).forEachIndexed { index, action ->
                output.write(30 + index)
                writeString(output, action)
            }
            if (definition.hollow) {
                output.write(74)
            }
            definition.supportItems?.let {
                output.write(75)
                output.write(it)
            }
            output.write(0)
            output.toByteArray()
        }
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        output.write((value ushr 24) and 0xFF)
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun writeMedium(output: ByteArrayOutputStream, value: Int) {
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun writeUnsignedSmart(output: ByteArrayOutputStream, value: Int) {
        require(value >= 0) { "Unsigned smart values must be non-negative." }
        if (value < 128) {
            output.write(value)
        } else {
            writeUnsignedShort(output, value + 32768)
        }
    }

    private fun writeUnsignedSmartCumulative(output: ByteArrayOutputStream, value: Int) {
        var remaining = value
        while (remaining >= 32767) {
            writeUnsignedSmart(output, 32767)
            remaining -= 32767
        }
        writeUnsignedSmart(output, remaining)
    }

    private fun writeMedium(file: RandomAccessFile, value: Int) {
        file.writeByte((value ushr 16) and 0xFF)
        file.writeByte((value ushr 8) and 0xFF)
        file.writeByte(value and 0xFF)
    }
}


