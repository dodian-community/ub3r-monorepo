package net.dodian.uber.game.content.objects.services

import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.scheduler.QueueTask
import net.dodian.uber.game.runtime.scheduler.QueueTaskService
import net.dodian.uber.game.runtime.zone.ZoneUpdateBus
import net.dodian.utilities.queueTasksEnabled
import net.dodian.utilities.zoneUpdateBatchingEnabled
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
        if (zoneUpdateBatchingEnabled) {
            ZoneUpdateBus.queuePersonalObject(client.dbId, position, objectId, face, type)
        } else {
            client.ReplaceObject2(position, objectId, face, type)
        }
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

        if (queueTasksEnabled) {
            val ticks = ((durationMs + 599L) / 600L).toInt().coerceAtLeast(1)
            QueueTaskService.schedule(ticks, 0, QueueTask {
                if (client.disconnected) {
                    perPlayerObjects.remove(key(client, position))
                    return@QueueTask false
                }
                if (zoneUpdateBatchingEnabled) {
                    ZoneUpdateBus.queuePersonalObject(client.dbId, position, revertObjectId, revertFace, revertType)
                } else {
                    client.ReplaceObject2(position, revertObjectId, revertFace, revertType)
                }
                perPlayerObjects.remove(key(client, position))
                false
            })
            return
        }

        GameEventScheduler.runLaterMs(durationMs.toInt()) {
            if (client.disconnected) {
                perPlayerObjects.remove(key(client, position))
                return@runLaterMs
            }
            if (zoneUpdateBatchingEnabled) {
                ZoneUpdateBus.queuePersonalObject(client.dbId, position, revertObjectId, revertFace, revertType)
            } else {
                client.ReplaceObject2(position, revertObjectId, revertFace, revertType)
            }
            perPlayerObjects.remove(key(client, position))
        }
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
