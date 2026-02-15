package net.dodian.uber.game.content.objects.services

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import java.util.concurrent.ConcurrentHashMap

object ObjectSpawnService {
    data class ActiveGlobalSpawn(
        val objectId: Int,
        val position: Position,
        val face: Int,
        val type: Int,
        val oldObjectId: Int,
        val durationMs: Long,
        val createdAt: Long,
    )

    private val activeSpawns = ConcurrentHashMap<String, ActiveGlobalSpawn>()

    fun spawnTemporaryGlobal(
        objectId: Int,
        position: Position,
        face: Int,
        type: Int,
        oldObjectId: Int,
        durationMs: Long,
    ): Boolean {
        if (durationMs <= 0) {
            return false
        }
        val worldObject = GameObject(objectId, position.x, position.y, position.z, type, face, oldObjectId)
        val created = GlobalObject.addGlobalObject(worldObject, durationMs.toInt())
        if (created) {
            activeSpawns[key(position)] = ActiveGlobalSpawn(
                objectId = objectId,
                position = position.copy(),
                face = face,
                type = type,
                oldObjectId = oldObjectId,
                durationMs = durationMs,
                createdAt = System.currentTimeMillis(),
            )
        }
        return created
    }

    fun removeTemporaryGlobal(position: Position): Boolean {
        val current = GlobalObject.getGlobalObject(position.x, position.y) ?: return false
        GlobalObject.getGlobalObject().remove(current)
        activeSpawns.remove(key(position))
        return true
    }

    internal fun activeSpawnsForTests(): Map<String, ActiveGlobalSpawn> = activeSpawns.toMap()

    internal fun clearForTests() {
        activeSpawns.clear()
    }

    private fun key(position: Position): String = "${position.x}:${position.y}:${position.z}"
}
