package org.rsmod.routefinder.collision

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionFlagMapSparseTest {
    @Test
    fun `region plane stays open while empty payloads are reclaimed`() {
        val map = CollisionFlagMap()
        map.markRegionPlaneAllocated(3200, 3200, 2)

        for (x in 3200 until 3264) {
            for (z in 3200 until 3264) {
                assertEquals(0, map[x, z, 2])
            }
        }
        assertEquals(map.defaultFlag, map[3199, 3200, 2])
        assertEquals(map.defaultFlag, map[3264, 3200, 2])
        assertEquals(
            CollisionFlagMapMetrics(
                openZones = 64,
                materializedZones = 0,
                activePages = 1,
                payloadBytes = 0,
                estimatedDirectoryBytes = map.metrics().estimatedDirectoryBytes,
                estimatedRetainedBytes = map.metrics().estimatedRetainedBytes,
            ),
            map.metrics(),
        )

        map.add(3210, 3220, 2, 0x400)
        assertEquals(0x400, map[3210, 3220, 2])
        assertEquals(1, map.metrics().materializedZones)

        map.remove(3210, 3220, 2, 0x400)
        assertEquals(0, map[3210, 3220, 2])
        assertEquals(0, map.metrics().materializedZones)
        assertEquals(64, map.metrics().openZones)

        map.deallocateIfPresent(3210, 3220, 2)
        assertEquals(map.defaultFlag, map[3210, 3220, 2])
        assertEquals(63, map.metrics().openZones)
    }

    @Test
    fun `sparse implementation matches upstream operations`() {
        val sparse = CollisionFlagMap()
        val upstream = UpstreamCollisionFlagMap()
        val random = Random(0x5EED)
        val coordinatePool =
            listOf(
                Triple(0, 0, 0),
                Triple(7, 7, 3),
                Triple(8, 8, 1),
                Triple(3200, 3200, 0),
                Triple(3263, 3263, 2),
                Triple(16376, 16376, 3),
                Triple(16383, 16383, 0),
            )

        repeat(5_000) {
            val (x, z, level) = coordinatePool[random.nextInt(coordinatePool.size)]
            val tileX = (x and -8) + random.nextInt(8)
            val tileZ = (z and -8) + random.nextInt(8)
            val mask = 1 shl random.nextInt(24)
            when (random.nextInt(5)) {
                0 -> {
                    sparse.allocateIfAbsent(tileX, tileZ, level)
                    upstream.allocateIfAbsent(tileX, tileZ, level)
                }
                1 -> {
                    sparse[tileX, tileZ, level] = mask
                    upstream[tileX, tileZ, level] = mask
                }
                2 -> {
                    sparse.add(tileX, tileZ, level, mask)
                    upstream.add(tileX, tileZ, level, mask)
                }
                3 -> {
                    sparse.remove(tileX, tileZ, level, mask)
                    upstream.remove(tileX, tileZ, level, mask)
                }
                else -> {
                    sparse.deallocateIfPresent(tileX, tileZ, level)
                    upstream.deallocateIfPresent(tileX, tileZ, level)
                }
            }

            for ((sampleX, sampleZ, sampleLevel) in coordinatePool) {
                assertEquals(
                    upstream[sampleX, sampleZ, sampleLevel],
                    sparse[sampleX, sampleZ, sampleLevel],
                    "flag mismatch at ($sampleX, $sampleZ, $sampleLevel)",
                )
                assertEquals(
                    upstream.isZoneAllocated(sampleX, sampleZ, sampleLevel),
                    sparse.isZoneAllocated(sampleX, sampleZ, sampleLevel),
                    "allocation mismatch at ($sampleX, $sampleZ, $sampleLevel)",
                )
            }
        }
    }

    @Test
    fun `full cache region count needs no empty tile payloads`() {
        val map = CollisionFlagMap()
        var remaining = 2_607
        for (regionX in 0 until 256) {
            for (regionZ in 0 until 256) {
                if (remaining-- <= 0) break
                for (level in 0 until 4) {
                    map.markRegionPlaneAllocated(regionX shl 6, regionZ shl 6, level)
                }
            }
            if (remaining <= 0) break
        }

        val metrics = map.metrics()
        assertEquals(2_607 * 4 * 64, metrics.openZones)
        assertEquals(0, metrics.materializedZones)
        assertEquals(2_607 * 4, metrics.activePages)
        assertTrue(metrics.estimatedDirectoryBytes < 5L * 1024 * 1024)
        assertFalse(map.isZoneAllocated(16320, 16320, 0))
    }

    /** Exact behavioral model of the upstream dense array implementation. */
    private class UpstreamCollisionFlagMap {
        private val flags = arrayOfNulls<IntArray>(2048 * 2048 * 4)

        operator fun get(x: Int, z: Int, level: Int): Int =
            flags[zoneIndex(x, z, level)]?.get(tileIndex(x, z)) ?: -1

        operator fun set(x: Int, z: Int, level: Int, mask: Int) {
            val tiles = flags[zoneIndex(x, z, level)] ?: allocateIfAbsent(x, z, level)
            tiles[tileIndex(x, z)] = mask
        }

        fun add(x: Int, z: Int, level: Int, mask: Int) {
            val zone = zoneIndex(x, z, level)
            val tile = tileIndex(x, z)
            val current = flags[zone]?.get(tile) ?: 0
            this[x, z, level] = current or mask
        }

        fun remove(x: Int, z: Int, level: Int, mask: Int) {
            this[x, z, level] = this[x, z, level] and mask.inv()
        }

        fun allocateIfAbsent(x: Int, z: Int, level: Int): IntArray {
            val index = zoneIndex(x, z, level)
            return flags[index] ?: IntArray(64).also { flags[index] = it }
        }

        fun deallocateIfPresent(x: Int, z: Int, level: Int) {
            flags[zoneIndex(x, z, level)] = null
        }

        fun isZoneAllocated(x: Int, z: Int, level: Int): Boolean =
            flags[zoneIndex(x, z, level)] != null

        private fun tileIndex(x: Int, z: Int): Int = (x and 0x7) or ((z and 0x7) shl 3)

        private fun zoneIndex(x: Int, z: Int, level: Int): Int =
            ((x shr 3) and 0x7FF) or (((z shr 3) and 0x7FF) shl 11) or ((level and 0x3) shl 22)
    }
}
