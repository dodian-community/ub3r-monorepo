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
            val summary = rebuildCollisionStreaming(decoder, regions)
            logger.info(
                "Cache decode complete: regions={}, tiles={}, objects={}, blockingObjects={}, walkableObjects={}",
                summary.regionCount,
                summary.tileCount,
                summary.objectCount,
                summary.blockingObjectCount,
                summary.walkableObjectCount,
            )
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
            MapIndexTable(emptyList())
        } finally {
            store.close()
        }
    }

    private fun rebuildCollisionStreaming(decoder: MapDecoder, regions: List<MapIndexEntry>): MapDecodeSummary {
        collisionBuildService.clear()
        if (regions.isEmpty()) {
            return MapDecodeSummary(
                regionCount = 0,
                tileCount = 0,
                objectCount = 0,
                blockingObjectCount = 0,
                walkableObjectCount = 0,
            )
        }

        var tileGridsDecoded = 0
        var objectCount = 0
        var blockingObjects = 0
        var walkableObjects = 0
        val definitionCache = HashMap<Int, GameObjectData>(1024)

        for (region in regions) {
            val decoded = decoder.decodeRegion(region)
            decoded.tileGrid?.let { grid ->
                tileGridsDecoded++
                collisionBuildService.applyTerrain(grid)
            }

            for (obj in decoded.objects) {
                val definition = definitionCache.getOrPut(obj.objectId) { GameObjectData.forId(obj.objectId) }
                collisionBuildService.applyObjectData(obj, definition)
                objectCount++
                if (definition.isSolid() && !definition.isWalkable()) {
                    blockingObjects++
                }
                if (definition.isWalkable()) {
                    walkableObjects++
                }
            }
        }

        return MapDecodeSummary(
            regionCount = regions.size,
            tileCount = tileGridsDecoded * 64 * 64 * 4,
            objectCount = objectCount,
            blockingObjectCount = blockingObjects,
            walkableObjectCount = walkableObjects,
        )
    }
}
