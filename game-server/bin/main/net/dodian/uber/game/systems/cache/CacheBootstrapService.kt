package net.dodian.uber.game.systems.cache

import java.util.HashMap
import java.nio.file.Path
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.slf4j.LoggerFactory

class CacheBootstrapService(
    private val cachePath: Path = Path.of("data/cache"),
) {
    private val logger = LoggerFactory.getLogger(CacheBootstrapService::class.java)
    private val collisionBuildService = CollisionBuildService(CollisionManager.global())

    fun bootstrap(): MapIndexTable {
        val store = CacheStore(cachePath).open()
        return try {
            val objectDefinitions = ObjectDefinitionDecoder.decode(store)
            GameObjectData.replaceDefinitions(objectDefinitions.definitions)
            if (objectDefinitions.definitionCount > 0) {
                logger.info(
                    "Loaded cache object definitions: count={}, blocking={}, interactive={}",
                    objectDefinitions.definitionCount,
                    objectDefinitions.blockingCount,
                    objectDefinitions.interactiveCount,
                )
            }
            val decoder = MapDecoder(store)
            val regions = decoder.decodeIndexEntries()
            val firstPass = rebuildCollisionStreaming(decoder, regions)
            if (firstPass.footprintMismatchAtKnownTile &&
                CollisionBuildService.LIVE_FOOTPRINT_MODE != CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE
            ) {
                logger.info(
                    "Detected footprint mismatch at known tile 2727,9773 on rotated mode; switching to Luna-style unrotated footprint for object types 9..21 and rebuilding collision.",
                )
                CollisionBuildService.LIVE_FOOTPRINT_MODE = CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE
            }
            val finalPass =
                if (CollisionBuildService.LIVE_FOOTPRINT_MODE == CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE &&
                    firstPass.footprintMismatchAtKnownTile
                ) {
                    rebuildCollisionStreaming(decoder, regions)
                } else {
                    firstPass
                }
            val summary = finalPass.summary
            logger.info(
                "Cache decode complete: regions={}, tiles={}, objects={}, blockingObjects={}, walkableObjects={}",
                summary.regionCount,
                summary.tileCount,
                summary.objectCount,
                summary.blockingObjectCount,
                summary.walkableObjectCount,
            )
            logger.info("Cache collision offenders: typeDistribution={}", finalPass.blockingTypeDistribution)
            logger.info("Cache collision offenders: nonSolidBlockedByTypeRule={}", finalPass.nonSolidBlockedByTypeRule)
            logger.info("Cache collision offenders: topBlockingAnchorTiles={}", finalPass.topBlockingAnchors)
            if (store.isAvailable().not()) {
                logger.warn("Cache bootstrap: cache directory missing at {}", store.describe())
            }
            if (!store.hasFile("map_index")) {
                logger.warn("Cache bootstrap: missing map_index at {}", store.describe())
            }
            if (regions.isEmpty()) {
                logger.warn("Cache bootstrap: no map regions decoded from {}", store.describe())
            } else {
                logger.info(
                    "Cache bootstrap: loaded {} map regions from {}",
                    regions.size,
                    store.describe(),
                )
            }
            logger.info(
                "World collision ready from decoded cache: regions={}, tiles={}, objects={}",
                summary.regionCount,
                summary.tileCount,
                summary.objectCount,
            )
            MapIndexTable(regions = regions, summary = summary)
        } catch (exception: Exception) {
            logger.warn("Cache bootstrap: failed to decode cache at {}", store.describe(), exception)
            collisionBuildService.rebuild(MapIndexTable(emptyList()))
            CacheCollisionAuditStore.publish(emptyList(), emptyMap())
            MapIndexTable(emptyList())
        } finally {
            store.close()
        }
    }

    private fun rebuildCollisionStreaming(decoder: MapDecoder, regions: List<MapIndexEntry>): RebuildCollisionResult {
        collisionBuildService.clear()
        if (regions.isEmpty()) {
            return RebuildCollisionResult(
                summary =
                    MapDecodeSummary(
                        regionCount = 0,
                        tileCount = 0,
                        objectCount = 0,
                        blockingObjectCount = 0,
                        walkableObjectCount = 0,
                    ),
                footprintMismatchAtKnownTile = false,
                blockingTypeDistribution = "none",
                nonSolidBlockedByTypeRule = "none",
                topBlockingAnchors = "none",
            )
        }

        var tileGridsDecoded = 0
        var objectCount = 0
        var blockingObjects = 0
        var walkableObjects = 0
        val definitionCache = HashMap<Int, GameObjectData>(1024)
        val regionObjects = HashMap<Int, MutableList<DecodedMapObject>>(regions.size)
        val blockingByType = HashMap<Int, Int>(32)
        val nonSolidBlockedByTypeRule = HashMap<String, Int>(128)
        val blockingAnchorCounts = HashMap<Long, Int>(2048)
        var knownTileBlockedByCurrentObjects = false
        var knownTileBlockedByLunaObjects = false

        for (region in regions) {
            val decoded = decoder.decodeRegion(region)
            decoded.tileGrid?.let { grid ->
                tileGridsDecoded++
                collisionBuildService.applyTerrain(grid)
            }

            if (decoded.objects.isNotEmpty()) {
                regionObjects.getOrPut(region.regionId) { ArrayList(decoded.objects.size) }.addAll(decoded.objects)
            }
            for (obj in decoded.objects) {
                val definition = definitionCache.getOrPut(obj.objectId) { GameObjectData.forId(obj.objectId) }
                collisionBuildService.applyObjectData(obj, definition)
                objectCount++
                val blocksByType =
                    CollisionBuildService.isTypeUnwalkable(
                        type = obj.type,
                        solid = definition.isSolid(),
                        walkable = definition.isWalkable(),
                        hasActions = definition.hasActions(),
                    )
                if (blocksByType && obj.plane == KNOWN_TILE_Z) {
                    val overlapsCurrent =
                        CollisionBuildService.occupiesTile(
                            objectX = obj.x,
                            objectY = obj.y,
                            tileX = KNOWN_TILE_X,
                            tileY = KNOWN_TILE_Y,
                            type = obj.type,
                            rotation = obj.rotation,
                            sizeX = definition.sizeX,
                            sizeY = definition.sizeY,
                            mode = CollisionBuildService.FootprintMode.ROTATED,
                        )
                    val overlapsLuna =
                        CollisionBuildService.occupiesTile(
                            objectX = obj.x,
                            objectY = obj.y,
                            tileX = KNOWN_TILE_X,
                            tileY = KNOWN_TILE_Y,
                            type = obj.type,
                            rotation = obj.rotation,
                            sizeX = definition.sizeX,
                            sizeY = definition.sizeY,
                            mode = CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE,
                        )
                    if (overlapsCurrent) {
                        knownTileBlockedByCurrentObjects = true
                    }
                    if (overlapsLuna) {
                        knownTileBlockedByLunaObjects = true
                    }
                }
                if (blocksByType) {
                    blockingByType.merge(obj.type, 1, Int::plus)
                    if (!definition.isSolid()) {
                        val key = "${obj.objectId}:${obj.type}"
                        nonSolidBlockedByTypeRule.merge(key, 1, Int::plus)
                    }
                    val anchorKey = anchorKey(obj.x, obj.y, obj.plane)
                    blockingAnchorCounts.merge(anchorKey, 1, Int::plus)
                }
                if (definition.isSolid() && !definition.isWalkable()) {
                    blockingObjects++
                }
                if (definition.isWalkable()) {
                    walkableObjects++
                }
            }
        }

        CacheCollisionAuditStore.publish(
            regions = regions,
            regionObjects = regionObjects.mapValues { it.value.toList() },
        )

        return RebuildCollisionResult(
            summary =
                MapDecodeSummary(
                    regionCount = regions.size,
                    tileCount = tileGridsDecoded * 64 * 64 * 4,
                    objectCount = objectCount,
                    blockingObjectCount = blockingObjects,
                    walkableObjectCount = walkableObjects,
                ),
            footprintMismatchAtKnownTile = knownTileBlockedByCurrentObjects && !knownTileBlockedByLunaObjects,
            blockingTypeDistribution = topEntriesString(blockingByType, 12) { (type, count) -> "$type=$count" },
            nonSolidBlockedByTypeRule = topEntriesString(nonSolidBlockedByTypeRule, 12) { (key, count) -> "$key=$count" },
            topBlockingAnchors =
                topEntriesString(blockingAnchorCounts, 12) { (key, count) ->
                    val x = ((key shr 42) and 0x1FFFFF).toInt()
                    val y = ((key shr 21) and 0x1FFFFF).toInt()
                    val z = (key and 0x3).toInt()
                    "($x,$y,$z)=$count"
                },
        )
    }

    private data class RebuildCollisionResult(
        val summary: MapDecodeSummary,
        val footprintMismatchAtKnownTile: Boolean,
        val blockingTypeDistribution: String,
        val nonSolidBlockedByTypeRule: String,
        val topBlockingAnchors: String,
    )

    private companion object {
        const val KNOWN_TILE_X = 2727
        const val KNOWN_TILE_Y = 9773
        const val KNOWN_TILE_Z = 0

        fun anchorKey(x: Int, y: Int, z: Int): Long =
            ((x.toLong() and 0x1FFFFF) shl 42) or
                ((y.toLong() and 0x1FFFFF) shl 21) or
                (z.toLong() and 0x3)

        fun <K> topEntriesString(map: Map<K, Int>, limit: Int, formatter: (Map.Entry<K, Int>) -> String): String {
            if (map.isEmpty()) {
                return "none"
            }
            return map.entries
                .asSequence()
                .sortedByDescending { it.value }
                .take(limit)
                .joinToString(", ", transform = formatter)
        }
    }
}
