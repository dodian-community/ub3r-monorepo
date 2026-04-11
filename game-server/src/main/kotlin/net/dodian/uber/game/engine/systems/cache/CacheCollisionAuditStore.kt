package net.dodian.uber.game.engine.systems.cache

object CacheCollisionAuditStore {
    @Volatile
    private var entriesByRegion: Map<Int, MapIndexEntry> = emptyMap()

    @Volatile
    private var objectsByRegion: Map<Int, List<DecodedMapObject>> = emptyMap()

    @JvmStatic
    fun publish(regions: List<MapIndexEntry>, regionObjects: Map<Int, List<DecodedMapObject>>) {
        entriesByRegion = regions.associateBy { it.regionId }
        objectsByRegion = regionObjects
    }

    @JvmStatic
    fun entryForTile(x: Int, y: Int): MapIndexEntry? = entriesByRegion[regionId(x, y)]

    @JvmStatic
    fun objectsForRegion(regionId: Int): List<DecodedMapObject> = objectsByRegion[regionId].orEmpty()

    @JvmStatic
    fun objectsForTile(x: Int, y: Int): List<DecodedMapObject> = objectsForRegion(regionId(x, y))

    internal fun clearForTests() {
        entriesByRegion = emptyMap()
        objectsByRegion = emptyMap()
    }

    @JvmStatic
    fun regionId(x: Int, y: Int): Int = ((x shr 6) shl 8) or (y shr 6)
}
