package net.dodian.uber.game.content.objects.services

import net.dodian.uber.game.event.Event
import net.dodian.uber.game.event.EventManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import java.util.concurrent.ConcurrentHashMap

object PersonalObjectService {
    data class PersonalObjectState(
        val objectId: Int,
        val face: Int,
        val type: Int,
        val expiresAt: Long?,
        val revertObjectId: Int?,
        val revertFace: Int?,
        val revertType: Int?,
    )

    private val perPlayerObjects = ConcurrentHashMap<String, PersonalObjectState>()

    fun show(
        client: Client,
        position: Position,
        objectId: Int,
        face: Int,
        type: Int,
    ) {
        client.ReplaceObject2(position, objectId, face, type)
        perPlayerObjects[key(client, position)] = PersonalObjectState(
            objectId = objectId,
            face = face,
            type = type,
            expiresAt = null,
            revertObjectId = null,
            revertFace = null,
            revertType = null,
        )
    }

    fun showTemporary(
        client: Client,
        position: Position,
        objectId: Int,
        face: Int,
        type: Int,
        durationMs: Long,
        revertObjectId: Int,
        revertFace: Int,
        revertType: Int,
    ) {
        show(client, position, objectId, face, type)
        val expiresAt = System.currentTimeMillis() + durationMs
        perPlayerObjects[key(client, position)] = PersonalObjectState(
            objectId = objectId,
            face = face,
            type = type,
            expiresAt = expiresAt,
            revertObjectId = revertObjectId,
            revertFace = revertFace,
            revertType = revertType,
        )

        EventManager.getInstance().registerEvent(object : Event(durationMs.toInt()) {
            override fun execute() {
                if (client.disconnected) {
                    perPlayerObjects.remove(key(client, position))
                    stop()
                    return
                }
                client.ReplaceObject2(position, revertObjectId, revertFace, revertType)
                perPlayerObjects.remove(key(client, position))
                stop()
            }
        })
    }

    internal fun stateForTests(client: Client, position: Position): PersonalObjectState? {
        return perPlayerObjects[key(client, position)]
    }

    internal fun clearForTests() {
        perPlayerObjects.clear()
    }

    private fun key(client: Client, position: Position): String {
        return "${client.slot}:${position.x}:${position.y}:${position.z}"
    }
}
