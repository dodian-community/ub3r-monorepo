package net.dodian.uber.game.engine.systems.skills.agility

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.skills.agility.runtime.AgilityPassageOverlayService

class Agility(private val c: Client) {
    private fun runLater(delayMs: Int, action: () -> Unit) {
        ContentTiming.runLaterMs(delayMs) {
            action()
        }
    }

    private fun queueAgilityWalk(deltaX: Int, deltaY: Int, durationMs: Long) {
        AgilityPassageOverlayService.grantForDelta(c, deltaX, deltaY, durationMs)
        c.AddToWalkCords(deltaX, deltaY, durationMs)
    }

    private fun isBusy(): Boolean = c.isMovementLocked

    fun kbdEntrance() {
        if (isBusy()) {
            return
        }
        val distance =
            when (c.position.x) {
                3304 -> 1
                3305 -> -1
                else -> 0
            }
        if (distance == 0) {
            return
        }
        c.setMovementLocked(true)
        c.ReplaceObject(3305, 9376, 6452, -3, 0)
        c.ReplaceObject(3305, 9375, 6451, -1, 0)
        val time = 600
        queueAgilityWalk(distance, 0, time.toLong())
        runLater(time) {
            c.ReplaceObject(3305, 9376, 6452, 0, 0)
            c.ReplaceObject(3305, 9375, 6451, 0, 0)
            c.setMovementLocked(false)
        }
    }
}
