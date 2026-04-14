package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.engine.systems.cache.CacheCollisionAuditStore
import net.dodian.uber.game.engine.systems.cache.CollisionBuildService
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionFlag
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager

object ClipProbeService {
    data class ObjectProbe(
        val objectId: Int,
        val objectName: String,
        val type: Int,
        val rotation: Int,
        val anchorX: Int,
        val anchorY: Int,
        val solid: Boolean,
        val walkable: Boolean,
        val hasActions: Boolean,
        val blockWalk: Int,
        val blockRange: Boolean,
        val breakRouteFinding: Boolean,
        val sizeX: Int,
        val sizeY: Int,
        val blocksByTypeRule: Boolean,
        val overlapsCurrent: Boolean,
        val overlapsLuna: Boolean,
        val directionalContact: Boolean,
        val reasonTags: List<String>,
    )

    data class TileProbe(
        val x: Int,
        val y: Int,
        val z: Int,
        val rawFlags: Int,
        val fullMobBlocked: Boolean,
        val terrainBlocked: Boolean,
        val staticOverridePresent: Boolean,
        val runtimeOverlayPresent: Boolean,
        val objectMatches: List<ObjectProbe>,
    ) {
        val blockedByCurrentObjects: Boolean =
            objectMatches.any { it.blocksByTypeRule && it.overlapsCurrent }
        val blockedByLunaObjects: Boolean =
            objectMatches.any { it.blocksByTypeRule && it.overlapsLuna }
        val likelyFootprintMismatch: Boolean =
            blockedByCurrentObjects && !blockedByLunaObjects
        val likelyTerrainOrUnknownSource: Boolean =
            fullMobBlocked && !blockedByCurrentObjects && !runtimeOverlayPresent
    }

    @JvmStatic
    fun probeTile(x: Int, y: Int, z: Int): TileProbe {
        val collision = CollisionManager.global()
        val rawFlags = collision.getFlags(x, y, z)
        val fullMobBlocked = collision.isTileBlocked(x, y, z)
        val terrainBlocked = false
        val staticOverridePresent = StaticObjectOverrides.all().any { it.position.x == x && it.position.y == y && it.position.z == z }
        val runtimeOverlayPresent = ObjectClipService.getAppliedClip(Position(x, y, z)) != null

        val objectMatches =
            CacheCollisionAuditStore
                .objectsForTile(x, y)
                .asSequence()
                .filter { it.plane == z }
                .mapNotNull { obj ->
                    val definition = GameObjectData.forId(obj.objectId)
                    val blocksByTypeRule =
                        CollisionBuildService.isTypeWalkBlocking(
                            type = obj.type,
                            blockWalk = definition.blockWalk(),
                            objectName = definition.name,
                        )
                    val overlapsCurrent =
                        CollisionBuildService.occupiesTile(
                            objectX = obj.x,
                            objectY = obj.y,
                            tileX = x,
                            tileY = y,
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
                            tileX = x,
                            tileY = y,
                            type = obj.type,
                            rotation = obj.rotation,
                            sizeX = definition.sizeX,
                            sizeY = definition.sizeY,
                            mode = CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE,
                        )
                    val directionalContact =
                        obj.type in 0..3 &&
                            kotlin.math.abs(obj.x - x) <= 1 &&
                            kotlin.math.abs(obj.y - y) <= 1
                    val reasonTags = ArrayList<String>(3)
                    if (blocksByTypeRule) {
                        reasonTags += "blocked_by_type_rule"
                    }
                    if (obj.type in 0..3 && directionalContact) {
                        reasonTags += "directional_block"
                    } else if (blocksByTypeRule) {
                        reasonTags += "full_tile_block"
                    }
                    if (!overlapsCurrent && !overlapsLuna && !directionalContact) {
                        null
                    } else {
                        ObjectProbe(
                            objectId = obj.objectId,
                            objectName = definition.name,
                            type = obj.type,
                            rotation = obj.rotation,
                            anchorX = obj.x,
                            anchorY = obj.y,
                            solid = definition.isSolid(),
                            walkable = definition.isWalkable(),
                            hasActions = definition.hasActions(),
                            blockWalk = definition.blockWalk(),
                            blockRange = definition.blockRange(),
                            breakRouteFinding = definition.breakRouteFinding(),
                            sizeX = definition.sizeX,
                            sizeY = definition.sizeY,
                            blocksByTypeRule = blocksByTypeRule,
                            overlapsCurrent = overlapsCurrent,
                            overlapsLuna = overlapsLuna,
                            directionalContact = directionalContact,
                            reasonTags = reasonTags,
                        )
                    }
                }.toList()

        return TileProbe(
            x = x,
            y = y,
            z = z,
            rawFlags = rawFlags,
            fullMobBlocked = fullMobBlocked,
            terrainBlocked = terrainBlocked,
            staticOverridePresent = staticOverridePresent,
            runtimeOverlayPresent = runtimeOverlayPresent,
            objectMatches = objectMatches,
        )
    }

    @JvmStatic
    fun scanArea(x1: Int, y1: Int, x2: Int, y2: Int, z: Int): List<TileProbe> {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)
        val probes = ArrayList<TileProbe>((maxX - minX + 1) * (maxY - minY + 1))
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                probes += probeTile(x, y, z)
            }
        }
        return probes
    }

    @JvmStatic
    fun formatFlags(rawFlags: Int): String {
        val tags = ArrayList<String>(12)
        if (rawFlags and CollisionFlag.FULL_MOB_BLOCK == CollisionFlag.FULL_MOB_BLOCK) {
            tags += "FULL_MOB_BLOCK"
        }
        if (rawFlags and CollisionFlag.MOB_NORTH != 0) tags += "N"
        if (rawFlags and CollisionFlag.MOB_SOUTH != 0) tags += "S"
        if (rawFlags and CollisionFlag.MOB_EAST != 0) tags += "E"
        if (rawFlags and CollisionFlag.MOB_WEST != 0) tags += "W"
        if (rawFlags and CollisionFlag.MOB_NORTH_EAST != 0) tags += "NE"
        if (rawFlags and CollisionFlag.MOB_NORTH_WEST != 0) tags += "NW"
        if (rawFlags and CollisionFlag.MOB_SOUTH_EAST != 0) tags += "SE"
        if (rawFlags and CollisionFlag.MOB_SOUTH_WEST != 0) tags += "SW"
        if (rawFlags and CollisionFlag.ROUTE_BLOCKER != 0) tags += "ROUTE"
        return if (tags.isEmpty()) "none" else tags.joinToString(",")
    }

    @JvmStatic
    fun formatOverlapSummary(probe: TileProbe, maxEntries: Int): String {
        if (probe.objectMatches.isEmpty()) {
            return "none"
        }
        val capped = probe.objectMatches.take(maxEntries.coerceAtLeast(1))
        val details =
            capped.joinToString(" | ") { match ->
                "id=${match.objectId},type=${match.type},rot=${match.rotation},anchor=${match.anchorX},${match.anchorY}," +
                    "name=${match.objectName}," +
                    "size=${match.sizeX}x${match.sizeY},solid=${match.solid},walkable=${match.walkable},actions=${match.hasActions}," +
                    "blockWalk=${match.blockWalk},blockRange=${match.blockRange},breakRoute=${match.breakRouteFinding}," +
                    "ovCurrent=${match.overlapsCurrent},ovLuna=${match.overlapsLuna},dirContact=${match.directionalContact}," +
                    "reasons=${match.reasonTags.joinToString("+")}"
            }
        val omitted = probe.objectMatches.size - capped.size
        return if (omitted > 0) "$details | ...+$omitted" else details
    }
}
