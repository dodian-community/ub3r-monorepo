package net.dodian.uber.game.model.objects

import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

object GlobalObject {
    private val globalObjects = CopyOnWriteArrayList<WorldObject>()

    @JvmStatic
    fun getGlobalObject(): CopyOnWriteArrayList<WorldObject> = globalObjects

    @JvmStatic
    fun updateNewObject(worldObject: WorldObject) {
        for (player in PlayerRegistry.players) {
            val client = player as? Client ?: continue
            if (!client.isActive) {
                continue
            }
            if (isWithinPlayerUpdateDistance(client, worldObject)) {
                client.ReplaceObject2(
                    Position(worldObject.x, worldObject.y, worldObject.z),
                    worldObject.id,
                    worldObject.face,
                    worldObject.type,
                )
            }
        }
    }

    @JvmStatic
    fun updateOldObject(worldObject: WorldObject) {
        for (player in PlayerRegistry.players) {
            val client = player as? Client ?: continue
            if (!client.isActive) {
                continue
            }
            if (isWithinPlayerUpdateDistance(client, worldObject)) {
                client.ReplaceObject(
                    worldObject.x,
                    worldObject.y,
                    worldObject.oldId,
                    worldObject.face,
                    worldObject.type,
                )
            }
        }
    }

    @JvmStatic
    fun addGlobalObject(worldObject: WorldObject, time: Int): Boolean {
        val expiresAt = worldObject.getAttachment() as? Long
        if ((expiresAt != null && expiresAt - System.currentTimeMillis() > 0L) || hasGlobalObject(worldObject)) {
            return false
        }
        worldObject.setAttachment(System.currentTimeMillis() + time)
        globalObjects += worldObject
        updateNewObject(worldObject)
        return true
    }

    @JvmStatic
    fun updateObject(client: Client) {
        val now = System.currentTimeMillis()
        for (worldObject in globalObjects) {
            if (!isWithinPlayerUpdateDistance(client, worldObject)) {
                continue
            }
            val expiresAt = worldObject.getAttachment() as Long
            if (expiresAt - now > 0L) {
                updateNewObject(worldObject)
            } else {
                updateOldObject(worldObject)
                globalObjects.remove(worldObject)
            }
        }
    }

    @JvmStatic
    fun hasGlobalObject(worldObject: WorldObject): Boolean {
        for (existing in globalObjects) {
            if (existing.x == worldObject.x && existing.y == worldObject.y) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getGlobalObject(objectX: Int, objectY: Int): WorldObject? {
        for (existing in globalObjects) {
            if (existing.x == objectX && existing.y == objectY) {
                return existing
            }
        }
        return null
    }

    private fun isWithinPlayerUpdateDistance(client: Client, worldObject: WorldObject): Boolean {
        if (client.position.z != worldObject.z) {
            return false
        }
        val deltaX = worldObject.x - client.position.x
        val deltaY = worldObject.y - client.position.y
        return deltaX <= 64 && deltaX >= -64 && deltaY <= 64 && deltaY >= -64
    }
}

