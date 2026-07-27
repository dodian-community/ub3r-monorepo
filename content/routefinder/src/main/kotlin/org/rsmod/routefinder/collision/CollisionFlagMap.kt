package org.rsmod.routefinder.collision

/**
 * Collision flags split into 64x64 region-plane pages.
 *
 * A zone may be allocated (and therefore default to open tiles) without retaining a zero-filled
 * [IntArray]. Payload arrays are only materialized for zones containing flags or when a caller
 * explicitly requests the legacy mutable array through [allocateIfAbsent] or [flags].
 */
public class CollisionFlagMap {
    public constructor()

    /** Compatibility constructor for callers that previously supplied the dense directory. */
    public constructor(flags: Array<IntArray?>) {
        this.flags.load(flags)
    }

    /**
     * Source-compatible indexed access to the old zone directory.
     *
     * Reading an allocated entry materializes and pins its mutable payload. Routing lookups bypass
     * this view so implicitly-open zones remain sparse.
     */
    public val flags: CollisionZoneDirectory = CollisionZoneDirectory()

    public inline val defaultFlag: Int
        inline get() = DEFAULT_COLLISION_FLAG

    public operator fun get(absoluteX: Int, absoluteZ: Int, level: Int): Int {
        val zoneIndex = zoneIndex(absoluteX, absoluteZ, level)
        return flags.getTile(zoneIndex, tileIndex(absoluteX, absoluteZ), DEFAULT_COLLISION_FLAG)
    }

    public operator fun set(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        flags.setTile(
            zoneIndex = zoneIndex(absoluteX, absoluteZ, level),
            tileIndex = tileIndex(absoluteX, absoluteZ),
            mask = mask,
        )
    }

    public fun add(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        if (mask == 0) {
            markZoneAllocated(absoluteX, absoluteZ, level)
            return
        }
        flags.addTile(
            zoneIndex = zoneIndex(absoluteX, absoluteZ, level),
            tileIndex = tileIndex(absoluteX, absoluteZ),
            mask = mask,
        )
    }

    public fun remove(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        val currentFlags = this[absoluteX, absoluteZ, level]
        this[absoluteX, absoluteZ, level] = currentFlags and mask.inv()
    }

    /** Marks an 8x8 zone open without allocating a zero-filled tile payload. */
    public fun markZoneAllocated(absoluteX: Int, absoluteZ: Int, level: Int) {
        flags.markAllocated(zoneIndex(absoluteX, absoluteZ, level))
    }

    /** Marks every 8x8 zone in a 64x64 region plane open without allocating tile payloads. */
    public fun markRegionPlaneAllocated(absoluteX: Int, absoluteZ: Int, level: Int) {
        flags.markRegionPlaneAllocated(regionPlaneIndex(absoluteX, absoluteZ, level))
    }

    public fun allocateIfAbsent(absoluteX: Int, absoluteZ: Int, level: Int): IntArray =
        flags.materialize(zoneIndex(absoluteX, absoluteZ, level), pin = true)

    public fun deallocateIfPresent(absoluteX: Int, absoluteZ: Int, level: Int) {
        flags.deallocate(zoneIndex(absoluteX, absoluteZ, level))
    }

    public fun isZoneAllocated(absoluteX: Int, absoluteZ: Int, level: Int): Boolean =
        flags.isAllocated(zoneIndex(absoluteX, absoluteZ, level))

    public fun metrics(): CollisionFlagMapMetrics = flags.metrics()

    public companion object {
        public const val DEFAULT_COLLISION_FLAG: Int = -1

        internal const val TOTAL_ZONE_COUNT: Int = 2048 * 2048 * 4
        internal const val ZONES_PER_REGION_PLANE: Int = 8 * 8
        internal const val TOTAL_REGION_PLANE_COUNT: Int = 256 * 256 * 4
        internal const val ZONE_TILE_COUNT: Int = 8 * 8

        private fun tileIndex(x: Int, z: Int): Int = (x and 0x7) or ((z and 0x7) shl 3)

        private fun zoneIndex(x: Int, z: Int, level: Int): Int =
            ((x shr 3) and 0x7FF) or (((z shr 3) and 0x7FF) shl 11) or ((level and 0x3) shl 22)

        private fun regionPlaneIndex(x: Int, z: Int, level: Int): Int =
            ((x shr 6) and 0xFF) or (((z shr 6) and 0xFF) shl 8) or ((level and 0x3) shl 16)
    }
}

/** Sparse, indexed compatibility view over collision zone payloads. */
public class CollisionZoneDirectory internal constructor() {
    private val pages: Array<ZonePage?> = arrayOfNulls(CollisionFlagMap.TOTAL_REGION_PLANE_COUNT)
    private var openZones: Int = 0
    private var materializedZones: Int = 0
    private var activePages: Int = 0

    public val size: Int
        get() = CollisionFlagMap.TOTAL_ZONE_COUNT

    public val indices: IntRange
        get() = 0 until size

    /** Legacy indexed access. An open sparse zone is materialized so its returned array is mutable. */
    public operator fun get(zoneIndex: Int): IntArray? {
        if (!isAllocated(zoneIndex)) return null
        return materialize(zoneIndex, pin = true)
    }

    public operator fun set(zoneIndex: Int, value: IntArray?) {
        if (value == null) {
            deallocate(zoneIndex)
            return
        }
        require(value.size == CollisionFlagMap.ZONE_TILE_COUNT) {
            "Collision zone payload must contain ${CollisionFlagMap.ZONE_TILE_COUNT} tiles"
        }
        val page = page(zoneIndex, create = true)!!
        val localZone = localZoneIndex(zoneIndex)
        val bit = 1L shl localZone
        if (page.openMask and bit == 0L) {
            page.openMask = page.openMask or bit
            openZones++
        }
        if (page.payloads[localZone] == null) materializedZones++
        page.payloads[localZone] = value
        page.pinnedMask = page.pinnedMask or bit
    }

    internal fun getTile(zoneIndex: Int, tileIndex: Int, defaultFlag: Int): Int {
        val page = page(zoneIndex, create = false) ?: return defaultFlag
        val localZone = localZoneIndex(zoneIndex)
        val bit = 1L shl localZone
        if (page.openMask and bit == 0L) return defaultFlag
        return page.payloads[localZone]?.get(tileIndex) ?: 0
    }

    internal fun setTile(zoneIndex: Int, tileIndex: Int, mask: Int) {
        markAllocated(zoneIndex)
        val page = page(zoneIndex, create = false)!!
        val localZone = localZoneIndex(zoneIndex)
        var payload = page.payloads[localZone]
        if (payload == null) {
            if (mask == 0) return
            payload = IntArray(CollisionFlagMap.ZONE_TILE_COUNT)
            page.payloads[localZone] = payload
            materializedZones++
        }
        payload[tileIndex] = mask
        releaseEmptyPayload(page, localZone, payload)
    }

    internal fun addTile(zoneIndex: Int, tileIndex: Int, mask: Int) {
        markAllocated(zoneIndex)
        val page = page(zoneIndex, create = false)!!
        val localZone = localZoneIndex(zoneIndex)
        val payload =
            page.payloads[localZone]
                ?: IntArray(CollisionFlagMap.ZONE_TILE_COUNT).also {
                    page.payloads[localZone] = it
                    materializedZones++
                }
        payload[tileIndex] = payload[tileIndex] or mask
    }

    internal fun markAllocated(zoneIndex: Int) {
        val page = page(zoneIndex, create = true)!!
        val bit = 1L shl localZoneIndex(zoneIndex)
        if (page.openMask and bit == 0L) {
            page.openMask = page.openMask or bit
            openZones++
        }
    }

    internal fun markRegionPlaneAllocated(regionPlaneIndex: Int) {
        val page = pages[regionPlaneIndex] ?: ZonePage().also {
            pages[regionPlaneIndex] = it
            activePages++
        }
        openZones += java.lang.Long.bitCount(page.openMask.inv())
        page.openMask = ALL_ZONES_OPEN
    }

    internal fun materialize(zoneIndex: Int, pin: Boolean): IntArray {
        markAllocated(zoneIndex)
        val page = page(zoneIndex, create = false)!!
        val localZone = localZoneIndex(zoneIndex)
        val bit = 1L shl localZone
        if (pin) page.pinnedMask = page.pinnedMask or bit
        return page.payloads[localZone]
            ?: IntArray(CollisionFlagMap.ZONE_TILE_COUNT).also {
                page.payloads[localZone] = it
                materializedZones++
            }
    }

    internal fun deallocate(zoneIndex: Int) {
        val pageIndex = pageIndex(zoneIndex)
        val page = pages[pageIndex] ?: return
        val localZone = localZoneIndex(zoneIndex)
        val bit = 1L shl localZone
        if (page.openMask and bit == 0L) return
        page.openMask = page.openMask and bit.inv()
        page.pinnedMask = page.pinnedMask and bit.inv()
        openZones--
        if (page.payloads[localZone] != null) {
            page.payloads[localZone] = null
            materializedZones--
        }
        if (page.openMask == 0L) {
            pages[pageIndex] = null
            activePages--
        }
    }

    internal fun isAllocated(zoneIndex: Int): Boolean {
        val page = page(zoneIndex, create = false) ?: return false
        return page.openMask and (1L shl localZoneIndex(zoneIndex)) != 0L
    }

    internal fun metrics(): CollisionFlagMapMetrics =
        (materializedZones.toLong() * CollisionFlagMap.ZONE_TILE_COUNT * Int.SIZE_BYTES).let { payloadBytes ->
            val directoryBytes =
                ARRAY_HEADER_BYTES +
                    pages.size.toLong() * REFERENCE_BYTES +
                    activePages.toLong() * ESTIMATED_PAGE_BYTES
            CollisionFlagMapMetrics(
                openZones = openZones,
                materializedZones = materializedZones,
                activePages = activePages,
                payloadBytes = payloadBytes,
                estimatedDirectoryBytes = directoryBytes,
                estimatedRetainedBytes =
                    payloadBytes +
                        materializedZones.toLong() * ARRAY_HEADER_BYTES +
                        directoryBytes +
                        ESTIMATED_MAP_AND_DIRECTORY_OBJECT_BYTES,
            )
        }

    internal fun load(dense: Array<IntArray?>) {
        require(dense.size == CollisionFlagMap.TOTAL_ZONE_COUNT) {
            "Dense collision directory must contain ${CollisionFlagMap.TOTAL_ZONE_COUNT} zones"
        }
        dense.forEachIndexed { index, payload ->
            if (payload != null) this[index] = payload
        }
    }

    private fun releaseEmptyPayload(page: ZonePage, localZone: Int, payload: IntArray) {
        val bit = 1L shl localZone
        if (page.pinnedMask and bit != 0L || payload.any { it != 0 }) return
        page.payloads[localZone] = null
        materializedZones--
    }

    private fun page(zoneIndex: Int, create: Boolean): ZonePage? {
        val index = pageIndex(zoneIndex)
        var page = pages[index]
        if (page == null && create) {
            page = ZonePage()
            pages[index] = page
            activePages++
        }
        return page
    }

    private companion object {
        private const val ALL_ZONES_OPEN: Long = -1L
        private const val REFERENCE_BYTES: Long = 4L
        private const val ARRAY_HEADER_BYTES: Long = 16L
        private const val ESTIMATED_PAGE_BYTES: Long = 312L
        private const val ESTIMATED_MAP_AND_DIRECTORY_OBJECT_BYTES: Long = 48L

        private fun pageIndex(zoneIndex: Int): Int {
            val zoneX = zoneIndex and 0x7FF
            val zoneZ = (zoneIndex ushr 11) and 0x7FF
            val level = (zoneIndex ushr 22) and 0x3
            return (zoneX ushr 3) or ((zoneZ ushr 3) shl 8) or (level shl 16)
        }

        private fun localZoneIndex(zoneIndex: Int): Int {
            val localX = zoneIndex and 0x7
            val localZ = (zoneIndex ushr 11) and 0x7
            return localX or (localZ shl 3)
        }
    }
}

public data class CollisionFlagMapMetrics(
    public val openZones: Int,
    public val materializedZones: Int,
    public val activePages: Int,
    public val payloadBytes: Long,
    public val estimatedDirectoryBytes: Long,
    public val estimatedRetainedBytes: Long,
)

private class ZonePage {
    var openMask: Long = 0L
    var pinnedMask: Long = 0L
    val payloads: Array<IntArray?> = arrayOfNulls(CollisionFlagMap.ZONES_PER_REGION_PLANE)
}
