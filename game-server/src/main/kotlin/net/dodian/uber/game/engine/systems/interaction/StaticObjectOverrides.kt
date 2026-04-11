package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/**
 * Authoritative overrides for static cache objects that this server intentionally
 * removes or replaces globally.
 *
 * These must be applied both to client visuals and to startup collision so the
 * server never blocks tiles for objects the client no longer sees.
 */
data class StaticObjectOverride(
    val position: Position,
    val replacementObjectId: Int,
    val replacementFace: Int,
    val replacementType: Int,
)

object StaticObjectOverrides {
    private val overrides =
        buildList {
            add(removedDoor(2669, 2713, 0, objectType = 11))
            add(removedDoor(2713, 3483, 0))
            add(removedDoor(2716, 3472, 0))
            add(removedDoor(2594, 3102, 0))
            add(removedDoor(2816, 3438, 0))
            addAll(removedRectangle(2600..2609, 3111..3119, 0, objectType = 10))
        }

    @JvmStatic
    fun all(): List<StaticObjectOverride> = overrides

    @JvmStatic
    fun replayTo(viewer: Client) {
        for (override in overrides) {
            viewer.ReplaceObject2(
                override.position,
                override.replacementObjectId,
                override.replacementFace,
                override.replacementType,
            )
        }
    }

    private fun removedDoor(x: Int, y: Int, z: Int, objectType: Int = 0): StaticObjectOverride =
        StaticObjectOverride(
            position = Position(x, y, z),
            replacementObjectId = -1,
            replacementFace = -1,
            replacementType = objectType,
        )

    private fun removedRectangle(
        xRange: IntRange,
        yRange: IntRange,
        z: Int,
        objectType: Int,
    ): List<StaticObjectOverride> {
        val rows = ArrayList<StaticObjectOverride>(xRange.count() * yRange.count())
        for (x in xRange) {
            for (y in yRange) {
                rows +=
                    StaticObjectOverride(
                        position = Position(x, y, z),
                        replacementObjectId = -1,
                        replacementFace = -1,
                        replacementType = objectType,
                    )
            }
        }
        return rows
    }
}
