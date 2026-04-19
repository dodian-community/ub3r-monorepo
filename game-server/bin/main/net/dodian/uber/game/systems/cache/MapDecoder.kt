package net.dodian.uber.game.systems.cache

class MapDecoder(
    private val store: CacheStore,
) {
    fun decodeIndexEntries(): List<MapIndexEntry> {
        val data = store.readMapIndex() ?: return emptyList()
        if (data.size < 2) {
            return emptyList()
        }

        val declaredCount = CacheUtils.readUnsignedShort(data, 0)
        val startOffset: Int
        val entryCount: Int
        val entrySize: Int

        val remaining = data.size - 2
        if (declaredCount > 0 && remaining >= declaredCount * 6 && remaining % declaredCount == 0) {
            startOffset = 2
            entryCount = declaredCount
            entrySize = remaining / declaredCount
        } else {
            startOffset = 0
            entrySize = if (data.size % 7 == 0) 7 else 6
            entryCount = data.size / entrySize
        }

        val regions = ArrayList<MapIndexEntry>(entryCount)
        var offset = startOffset
        repeat(entryCount) {
            if (offset + 6 > data.size) {
                return@repeat
            }
            val regionId = CacheUtils.readUnsignedShort(data, offset)
            val landscapeArchiveId = CacheUtils.readUnsignedShort(data, offset + 2)
            val objectArchiveId = CacheUtils.readUnsignedShort(data, offset + 4)
            val priority = entrySize >= 7 && offset + 6 < data.size && data[offset + 6].toInt() != 0
            regions.add(
                MapIndexEntry(
                    regionId = regionId,
                    landscapeArchiveId = landscapeArchiveId,
                    objectArchiveId = objectArchiveId,
                    priority = priority,
                ),
            )
            offset += entrySize
        }
        return regions
    }

    fun decodeIndexTable(): MapIndexTable {
        val regions = decodeIndexEntries()
        val tileGrids = LinkedHashMap<Int, DecodedMapTileGrid>(regions.size)
        val objects = ArrayList<DecodedMapObject>()

        for (region in regions) {
            decodeTileGrid(region)?.let { tileGrids[region.regionId] = it }
            objects += decodeObjects(region)
        }

        return MapIndexTable(regions).withDecodedData(tileGrids = tileGrids, objects = objects)
    }

    fun decodeRegion(region: MapIndexEntry): DecodedRegionData {
        val tileGrid = decodeTileGrid(region)
        val objects = decodeObjects(region)
        return DecodedRegionData(tileGrid = tileGrid, objects = objects)
    }

    private fun decodeTileGrid(region: MapIndexEntry): DecodedMapTileGrid? {
        if (!region.hasLandscapeArchive()) {
            return null
        }

        val archive = store.readStoreFile(MAP_STORE, region.landscapeArchiveId) ?: return null
        val data = CacheUtils.unzipGzip(archive)
        val reader = CacheBuffer(data)
        val tiles =
            Array(MAP_PLANES) { plane ->
                Array(REGION_SIZE) { x ->
                    Array(REGION_SIZE) { y ->
                        DecodedMapTile(
                            offsetX = x,
                            offsetY = y,
                            plane = plane,
                            height = 0,
                            overlay = 0,
                            overlayType = 0,
                            overlayOrientation = 0,
                            attributes = 0,
                            underlay = 0,
                        )
                    }
                }
            }

        for (plane in 0 until MAP_PLANES) {
            for (x in 0 until REGION_SIZE) {
                for (y in 0 until REGION_SIZE) {
                    tiles[plane][x][y] = decodeTile(region, x, y, plane, reader, tiles)
                }
            }
        }

        return DecodedMapTileGrid(regionId = region.regionId, tiles = tiles)
    }

    private fun decodeTile(
        region: MapIndexEntry,
        x: Int,
        y: Int,
        plane: Int,
        reader: CacheBuffer,
        tiles: Array<Array<Array<DecodedMapTile>>>,
    ): DecodedMapTile {
        var height: Int
        var overlay = 0
        var overlayType = 0
        var overlayOrientation = 0
        var attributes = 0
        var underlay = 0

        while (true) {
            val opcode = reader.readUnsignedByte()
            when {
                opcode == 0 -> {
                    height =
                        if (plane == 0) {
                            -computeHeight(NOISE_X_SEED + region.baseX + x, NOISE_Y_SEED + region.baseY + y) * 8
                        } else {
                            tiles[plane - 1][x][y].height - 240
                        }
                    break
                }

                opcode == 1 -> {
                    var newHeight = reader.readUnsignedByte()
                    if (newHeight == 1) {
                        newHeight = 0
                    }
                    height =
                        if (plane == 0) {
                            -(newHeight * 8)
                        } else {
                            tiles[plane - 1][x][y].height - newHeight * 8
                        }
                    break
                }

                opcode <= 49 -> {
                    overlay = reader.readByte()
                    overlayType = (opcode - 2) / 4
                    overlayOrientation = (opcode - 2) and 0x3
                }

                opcode <= 81 -> {
                    attributes = opcode - 49
                }

                else -> {
                    underlay = opcode - 81
                }
            }
        }

        return DecodedMapTile(
            offsetX = x,
            offsetY = y,
            plane = plane,
            height = height,
            overlay = overlay,
            overlayType = overlayType,
            overlayOrientation = overlayOrientation,
            attributes = attributes,
            underlay = underlay,
        )
    }

    private fun decodeObjects(region: MapIndexEntry): List<DecodedMapObject> {
        if (!region.hasObjectArchive()) {
            return emptyList()
        }

        val archive = store.readStoreFile(MAP_STORE, region.objectArchiveId) ?: return emptyList()
        val data = CacheUtils.unzipGzip(archive)
        val reader = CacheBuffer(data)
        val objects = ArrayList<DecodedMapObject>()

        var objectId = -1
        while (true) {
            val objectIdOffset = reader.readUnsignedSmart2()
            if (objectIdOffset == 0) {
                break
            }
            objectId += objectIdOffset

            var objectPositionData = 0
            while (true) {
                val positionOffset = reader.readUnsignedSmart()
                if (positionOffset == 0) {
                    break
                }
                objectPositionData += positionOffset - 1

                val offsetX = (objectPositionData shr 6) and 0x3F
                val offsetY = objectPositionData and 0x3F
                val plane = (objectPositionData shr 12) and 0x3
                val otherData = reader.readUnsignedByte()
                val type = otherData shr 2
                val rotation = otherData and 0x3

                objects +=
                    DecodedMapObject(
                        objectId = objectId,
                        x = region.baseX + offsetX,
                        y = region.baseY + offsetY,
                        plane = plane,
                        type = type,
                        rotation = rotation,
                        regionId = region.regionId,
                    )
            }
        }

        return objects
    }

    private fun MapIndexEntry.hasLandscapeArchive(): Boolean = landscapeArchiveId >= 0 && landscapeArchiveId != MISSING_ARCHIVE_ID

    private fun MapIndexEntry.hasObjectArchive(): Boolean = objectArchiveId >= 0 && objectArchiveId != MISSING_ARCHIVE_ID

    private fun computeHeight(x: Int, y: Int): Int {
        var mapHeight =
            (interpolatedNoise(x + 45365, y + 0x16713, 4) - 128) +
                ((interpolatedNoise(x + 10294, y + 37821, 2) - 128) shr 1) +
                ((interpolatedNoise(x, y, 1) - 128) shr 2)
        mapHeight = (mapHeight * 0.3).toInt() + 35
        if (mapHeight < 10) {
            return 10
        }
        if (mapHeight > 60) {
            return 60
        }
        return mapHeight
    }

    private fun interpolatedNoise(deltaX: Int, deltaY: Int, scale: Int): Int {
        val x = deltaX / scale
        val localX = deltaX and (scale - 1)
        val y = deltaY / scale
        val localY = deltaY and (scale - 1)

        val southWest = weightedNoise(x, y)
        val southEast = weightedNoise(x + 1, y)
        val northWest = weightedNoise(x, y + 1)
        val northEast = weightedNoise(x + 1, y + 1)
        val interpolationA = interpolate(southWest, southEast, localX, scale)
        val interpolationB = interpolate(northWest, northEast, localX, scale)
        return interpolate(interpolationA, interpolationB, localY, scale)
    }

    private fun weightedNoise(x: Int, y: Int): Int {
        val corners = noise(x - 1, y - 1) + noise(x + 1, y - 1) + noise(x - 1, y + 1) + noise(x + 1, y + 1)
        val sides = noise(x - 1, y) + noise(x + 1, y) + noise(x, y - 1) + noise(x, y + 1)
        val center = noise(x, y)
        return corners / 16 + sides / 8 + center / 4
    }

    private fun noise(x: Int, y: Int): Int {
        var n = x + y * 57
        n = n shl 13 xor n
        val noise = n * (n * n * 15731 + 0xC0AE5) + 0x5208DD0D and Int.MAX_VALUE
        return noise shr 19 and 0xFF
    }

    private fun interpolate(a: Int, b: Int, delta: Int, scale: Int): Int {
        val factor = COSINE[(delta * 1024) / scale]
        val blend = (65536 - factor) shr 1
        return (a * (65536 - blend) shr 16) + (b * blend shr 16)
    }

    private companion object {
        const val MAP_STORE = 4
        const val MAP_PLANES = 4
        const val REGION_SIZE = 64
        const val MISSING_ARCHIVE_ID = 65535
        const val NOISE_X_SEED = 0xE3B7B
        const val NOISE_Y_SEED = 0x87CCE
        val COSINE = IntArray(2048) { index -> (65536.0 * kotlin.math.cos(index * 0.0030679615)).toInt() }
    }
}

data class DecodedRegionData(
    val tileGrid: DecodedMapTileGrid?,
    val objects: List<DecodedMapObject>,
)
