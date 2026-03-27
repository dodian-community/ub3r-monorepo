package net.dodian.uber.game.model.`object`

import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.systems.zone.ZoneUpdateBus
import net.dodian.utilities.zoneUpdateBatchingEnabled

object GlobalObject {
    private val globalObjects = CopyOnWriteArrayList<Object>()

    @JvmStatic
    fun getGlobalObject(): CopyOnWriteArrayList<Object> = globalObjects

    @JvmStatic
    fun updateNewObject(worldObject: Object) {
        if (zoneUpdateBatchingEnabled) {
            ZoneUpdateBus.queueGlobalObjectNew(
                Position(worldObject.x, worldObject.y, worldObject.z),
                worldObject.id,
                worldObject.face,
                worldObject.type,
            )
            return
        }
        for (player in PlayerHandler.players) {
            val client = player as? Client ?: continue
            if (!client.isActive) {
                continue
            }
            if (client.withinDistance(worldObject)) {
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
    fun updateOldObject(worldObject: Object) {
        if (zoneUpdateBatchingEnabled) {
            ZoneUpdateBus.queueGlobalObjectOld(
                Position(worldObject.x, worldObject.y, worldObject.z),
                worldObject.oldId,
                worldObject.face,
                worldObject.type,
            )
            return
        }
        for (player in PlayerHandler.players) {
            val client = player as? Client ?: continue
            if (!client.isActive) {
                continue
            }
            if (client.withinDistance(worldObject)) {
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
    fun addGlobalObject(worldObject: Object, time: Int): Boolean {
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
            if (!client.withinDistance(worldObject)) {
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
    fun hasGlobalObject(worldObject: Object): Boolean {
        for (existing in globalObjects) {
            if (existing.x == worldObject.x && existing.y == worldObject.y) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getGlobalObject(objectX: Int, objectY: Int): Object? {
        for (existing in globalObjects) {
            if (existing.x == objectX && existing.y == objectY) {
                return existing
            }
        }
        return null
    }
}
