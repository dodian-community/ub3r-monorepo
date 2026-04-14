package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.objects.DoorRegistry
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.engine.systems.cache.CollisionBuildService
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionDirection
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

object ObjectClipService {
    private val collisionBuildService = CollisionBuildService(CollisionManager.global())
    private val logger = LoggerFactory.getLogger(ObjectClipService::class.java)

    data class AppliedClip(
        val position: Position,
        val objectId: Int,
        val type: Int,
        val direction: Int,
        val solid: Boolean,
    )

    private val appliedClips = ConcurrentHashMap<String, AppliedClip>()

    @JvmStatic
    fun bootstrapStartupOverlays(worldObjects: Iterable<WorldObject>) {
        clearTrackedClips()

        var appliedWorldObjects = 0
        for (worldObject in worldObjects) {
            if (worldObject.id <= 0 || worldObject.x <= 0 || worldObject.y <= 0) {
                continue
            }
            applyDecodedObject(
                position = Position(worldObject.x, worldObject.y, worldObject.z),
                objectId = worldObject.id,
                type = worldObject.type,
                direction = worldObject.face,
                obj = GameObjectData.forId(worldObject.id),
            )
            appliedWorldObjects++
        }

        var appliedDoors = 0
        for (index in DoorRegistry.doorId.indices) {
            val objectId = DoorRegistry.doorId[index]
            val x = DoorRegistry.doorX[index]
            val y = DoorRegistry.doorY[index]
            if (objectId <= 0 || x <= 0 || y <= 0) {
                continue
            }
            applyDecodedObject(
                position = Position(x, y, DoorRegistry.doorHeight[index]),
                objectId = objectId,
                type = 0,
                direction = DoorRegistry.doorFace[index],
                obj = GameObjectData.forId(objectId),
            )
            appliedDoors++
        }

        logger.info(
            "Applied startup collision overlays: worldObjects={}, doors={}",
            appliedWorldObjects,
            appliedDoors,
        )

        applyStaticOverrides(StaticObjectOverrides.all())
    }

    @JvmStatic
    fun applyStaticOverrides(overrides: Iterable<StaticObjectOverride>) {
        var applied = 0
        for (override in overrides) {
            applyStaticOverride(override)
            applied++
        }
        if (applied > 0) {
            logger.info("Applied static map overrides: count={}", applied)
        }
    }

    @JvmStatic
    fun applyDecodedObject(position: Position, objectId: Int, type: Int, direction: Int, obj: GameObjectData?) {
        removeDecodedObject(position)
        if (obj == null) {
            return
        }
        appliedClips[key(position)] = AppliedClip(position.copy(), objectId, type, direction, obj.isSolid())
        collisionBuildService.applyObject(
            id = objectId,
            x = position.x,
            y = position.y,
            z = position.z,
            type = type,
            rotation = direction,
            sizeX = obj.sizeX,
            sizeY = obj.sizeY,
            solid = obj.isSolid(),
            walkable = obj.isWalkable(),
            hasActions = obj.hasActions(),
            objectName = obj.name,
            blockWalk = obj.blockWalk(),
            blockRange = obj.blockRange(),
            breakRouteFinding = obj.breakRouteFinding(),
        )
    }

    @JvmStatic
    fun removeDecodedObject(position: Position) {
        removeTrackedClip(position)
    }

    @Suppress("UNUSED_PARAMETER")
    fun remove(position: Position, type: Int, direction: Int, solid: Boolean) {
        // Removal only updates the applied-clip bookkeeper; collision flags remain managed by the
        // decoded-object path through CollisionBuildService and the global collision manager.
        removeTrackedClip(position)
    }

    internal fun getAppliedForTests(position: Position): AppliedClip? = appliedClips[key(position)]

    @JvmStatic
    fun getAppliedClip(position: Position): AppliedClip? = appliedClips[key(position)]

    internal fun clearForTests() {
        clearTrackedClips()
    }

    private fun clearTrackedClips() {
        appliedClips.values.map { it.position.copy() }.forEach(::removeTrackedClip)
    }

    private fun applyStaticOverride(override: StaticObjectOverride) {
        removeDecodedObject(override.position)
        clearStaticCollisionBroadly(override.position)
        if (override.replacementObjectId >= 0) {
            applyDecodedObject(
                position = override.position,
                objectId = override.replacementObjectId,
                type = override.replacementType,
                direction = override.replacementFace,
                obj = GameObjectData.forId(override.replacementObjectId),
            )
        }
    }

    private fun clearStaticCollisionBroadly(position: Position) {
        val collision = CollisionManager.global()
        collision.clearSolid(position.x, position.y, position.z)
        for (direction in CollisionDirection.WNES) {
            collision.clearWall(position.x, position.y, position.z, direction)
        }
    }

    private fun removeTrackedClip(position: Position) {
        val existing = appliedClips.remove(key(position)) ?: return
        val definition = GameObjectData.forId(existing.objectId)
        collisionBuildService.removeObject(
            id = existing.objectId,
            x = existing.position.x,
            y = existing.position.y,
            z = existing.position.z,
            type = existing.type,
            rotation = existing.direction,
            sizeX = definition.sizeX,
            sizeY = definition.sizeY,
            solid = definition.isSolid(),
            walkable = definition.isWalkable(),
            hasActions = definition.hasActions(),
            objectName = definition.name,
            blockWalk = definition.blockWalk(),
            blockRange = definition.blockRange(),
            breakRouteFinding = definition.breakRouteFinding(),
        )
    }

    private fun key(position: Position): String = "${position.x}:${position.y}:${position.z}"
}
