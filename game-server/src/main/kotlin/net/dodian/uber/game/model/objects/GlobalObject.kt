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

    /**
     * Refreshes dynamic-object state for a single arriving viewer (e.g. on region entry via
     * [net.dodian.uber.game.model.entity.player.Player.customObjects]). Sends only to [client] -
     * it must never delegate to [updateNewObject]/[updateOldObject], which broadcast to every
     * nearby player; doing so here previously turned every viewer's per-tick refresh into an
     * O(players x objects x players) broadcast storm. Expiry sweeping (and the accompanying
     * broadcast-to-everyone-nearby, which is correct there since expiry is a real world event)
     * lives in [sweepExpired] instead, so this method never mutates [globalObjects].
     */
    @JvmStatic
    fun updateObject(client: Client) {
        val now = System.currentTimeMillis()
        for (worldObject in globalObjects) {
            if (!isWithinPlayerUpdateDistance(client, worldObject)) {
                continue
            }
            val expiresAt = worldObject.getAttachment() as? Long ?: continue
            if (expiresAt - now > 0L) {
                client.ReplaceObject2(
                    Position(worldObject.x, worldObject.y, worldObject.z),
                    worldObject.id,
                    worldObject.face,
                    worldObject.type,
                )
            } else {
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

    /**
     * World-level expiry sweep - must be called exactly once per tick (see
     * [net.dodian.uber.game.engine.phases.WorldMaintenancePhase]), never per-viewer. Reverts and
     * removes any [globalObjects] entry whose timer has elapsed, broadcasting the revert to every
     * nearby player via [updateOldObject] (correct here: an expiry is a genuine world event).
     */
    @JvmStatic
    fun sweepExpired() {
        val now = System.currentTimeMillis()
        for (worldObject in globalObjects) {
            val expiresAt = worldObject.getAttachment() as? Long ?: continue
            if (expiresAt - now > 0L) {
                continue
            }
            updateOldObject(worldObject)
            globalObjects.remove(worldObject)
        }
    }

    @JvmStatic
    fun hasGlobalObject(worldObject: WorldObject): Boolean {
        for (existing in globalObjects) {
            if (existing.x == worldObject.x && existing.y == worldObject.y && existing.z == worldObject.z) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun getGlobalObject(objectX: Int, objectY: Int, objectZ: Int): WorldObject? {
        for (existing in globalObjects) {
            if (existing.x == objectX && existing.y == objectY && existing.z == objectZ) {
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
