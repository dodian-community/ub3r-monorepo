package net.dodian.uber.game.engine.systems.cache

import java.util.AbstractList

object CacheCollisionAuditStore {
    @Volatile
    private var entriesByRegion: Map<Int, MapIndexEntry> = emptyMap()

    @Volatile
    private var packedObjectsByRegion: Map<Int, LongArray> = emptyMap()

    @JvmStatic
    fun publish(regions: List<MapIndexEntry>, regionObjects: Map<Int, LongArray>) {
        entriesByRegion = regions.associateBy { it.regionId }
        packedObjectsByRegion = regionObjects
    }

    @JvmStatic
    fun entryForTile(x: Int, y: Int): MapIndexEntry? = entriesByRegion[regionId(x, y)]

    @JvmStatic
    fun objectsForRegion(regionId: Int): List<CacheCollisionAuditObject> {
        val packed = packedObjectsByRegion[regionId] ?: return emptyList()
        return object : AbstractList<CacheCollisionAuditObject>() {
            override val size: Int
                get() = packed.size

            override fun get(index: Int): CacheCollisionAuditObject = unpack(regionId, packed[index])
        }
    }

    @JvmStatic
    fun objectsForTile(x: Int, y: Int): List<CacheCollisionAuditObject> = objectsForRegion(regionId(x, y))

    @JvmStatic
    fun packedObjectCount(): Long = packedObjectsByRegion.values.sumOf { it.size.toLong() }

    @JvmStatic
    fun packedBytes(): Long = packedObjectCount() * Long.SIZE_BYTES

    internal fun pack(obj: CacheCollisionAuditObject): Long {
        require(obj.objectId in 0..0xffff) { "Object id cannot be packed: ${obj.objectId}" }
        require(obj.rawPlane in 0..3) { "Raw plane cannot be packed: ${obj.rawPlane}" }
        require(obj.effectivePlane in -1..3) { "Effective plane cannot be packed: ${obj.effectivePlane}" }
        require(obj.type in 0..63) { "Object type cannot be packed: ${obj.type}" }

        val regionBaseX = (obj.regionId ushr 8) shl 6
        val regionBaseY = (obj.regionId and 0xff) shl 6
        val localX = obj.x - regionBaseX
        val localY = obj.y - regionBaseY
        require(localX in 0..63 && localY in 0..63) { "Object is outside region ${obj.regionId}: ${obj.x},${obj.y}" }

        val skippedCode =
            when (obj.skippedReason) {
                null -> 0
                "plane_underflow" -> 1
                "tarnish_removed_object" -> 2
                else -> error("Unknown skipped reason: ${obj.skippedReason}")
            }
        return (obj.objectId.toLong() and 0xffffL) or
            ((localX.toLong() and 0x3fL) shl 16) or
            ((localY.toLong() and 0x3fL) shl 22) or
            ((obj.rawPlane.toLong() and 0x3L) shl 28) or
            (((obj.effectivePlane + 1).toLong() and 0x7L) shl 30) or
            ((obj.type.toLong() and 0x3fL) shl 33) or
            (((obj.rotation and 0x3).toLong()) shl 39) or
            ((skippedCode.toLong() and 0x3L) shl 41)
    }

    internal fun unpack(regionId: Int, packed: Long): CacheCollisionAuditObject {
        val skippedReason =
            when (((packed ushr 41) and 0x3L).toInt()) {
                0 -> null
                1 -> "plane_underflow"
                2 -> "tarnish_removed_object"
                else -> error("Invalid packed skipped reason")
            }
        return CacheCollisionAuditObject(
            objectId = (packed and 0xffffL).toInt(),
            x = ((regionId ushr 8) shl 6) + ((packed ushr 16) and 0x3fL).toInt(),
            y = ((regionId and 0xff) shl 6) + ((packed ushr 22) and 0x3fL).toInt(),
            rawPlane = ((packed ushr 28) and 0x3L).toInt(),
            effectivePlane = ((packed ushr 30) and 0x7L).toInt() - 1,
            type = ((packed ushr 33) and 0x3fL).toInt(),
            rotation = ((packed ushr 39) and 0x3L).toInt(),
            regionId = regionId,
            skippedReason = skippedReason,
        )
    }

    internal fun regionIdsForTests(): Set<Int> = packedObjectsByRegion.keys

    internal fun clearForTests() {
        entriesByRegion = emptyMap()
        packedObjectsByRegion = emptyMap()
    }

    @JvmStatic
    fun regionId(x: Int, y: Int): Int = ((x shr 6) shl 8) or (y shr 6)
}
