package net.dodian.uber.game.engine.systems.cache

import net.dodian.cache.objects.GameObjectData

private const val REGION_SIZE = 64
private const val MAP_PLANES = 4

data class DecodedMapTile(
    val offsetX: Int,
    val offsetY: Int,
    val plane: Int,
    val height: Int,
    val overlay: Int,
    val overlayType: Int,
    val overlayOrientation: Int,
    val attributes: Int,
    val underlay: Int,
) {
    fun isBlocked(): Boolean = (attributes and BLOCKED) == BLOCKED || isWater()

    fun isWater(): Boolean = overlay == 6

    fun isBridge(): Boolean = (attributes and BRIDGE) == BRIDGE

    companion object {
        const val BLOCKED: Int = 0x1
        const val BRIDGE: Int = 0x2
    }
}

data class DecodedMapObject(
    val objectId: Int,
    val x: Int,
    val y: Int,
    val plane: Int,
    val type: Int,
    val rotation: Int,
    val regionId: Int,
)

data class DecodedMapTileGrid(
    val regionId: Int,
    val tiles: Array<Array<Array<DecodedMapTile>>>,
) {
    fun getTile(x: Int, y: Int, plane: Int): DecodedMapTile = tiles[plane][x][y]
}

data class MapDecodeSummary(
    val regionCount: Int,
    val tileCount: Int,
    val objectCount: Int,
    val blockingObjectCount: Int,
    val walkableObjectCount: Int,
)

data class MapIndexEntry(
    val regionId: Int,
    val landscapeArchiveId: Int,
    val objectArchiveId: Int,
    val priority: Boolean = false,
) {
    val regionX: Int
        get() = regionId shr 8

    val regionY: Int
        get() = regionId and 0xFF

    val baseX: Int
        get() = (regionId shr 8) * REGION_SIZE

    val baseY: Int
        get() = (regionId and 0xFF) * REGION_SIZE
}

data class MapIndexTable(
    val regions: List<MapIndexEntry>,
    val tileGrids: Map<Int, DecodedMapTileGrid> = emptyMap(),
    val objects: List<DecodedMapObject> = emptyList(),
    val summary: MapDecodeSummary = regions.toSummary(),
)

private fun List<MapIndexEntry>.toSummary(
    tileGrids: Map<Int, DecodedMapTileGrid> = emptyMap(),
    objects: List<DecodedMapObject> = emptyList(),
): MapDecodeSummary {
    val definitionCache = HashMap<Int, GameObjectData>()
    var blockingObjects = 0
    var walkableObjects = 0
    for (obj in objects) {
        val definition = definitionCache.getOrPut(obj.objectId) { GameObjectData.forId(obj.objectId) }
        if (definition.isSolid() && !definition.isWalkable()) {
            blockingObjects++
        }
        if (definition.isWalkable()) {
            walkableObjects++
        }
    }

    return MapDecodeSummary(
        regionCount = size,
        tileCount = tileGrids.size * MAP_PLANES * REGION_SIZE * REGION_SIZE,
        objectCount = objects.size,
        blockingObjectCount = blockingObjects,
        walkableObjectCount = walkableObjects,
    )
}

fun MapIndexTable.withDecodedData(
    tileGrids: Map<Int, DecodedMapTileGrid>,
    objects: List<DecodedMapObject>,
): MapIndexTable =
    copy(
        tileGrids = tileGrids,
        objects = objects,
        summary = regions.toSummary(tileGrids, objects),
    )


