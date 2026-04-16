package net.dodian.uber.game.skill.runtime.action

import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.Client

object SkillStateCoordinator {
    @JvmStatic
    fun beginSession(player: Client, sessionKey: String): Boolean {
        val existing = player.activeSkillSessionKey
        if (existing != null && existing != sessionKey) {
            return false
        }
        if (existing == null) {
            player.setActiveSkillSession(sessionKey, GameCycleClock.currentCycle())
        }
        return true
    }

    @JvmStatic
    fun endSession(player: Client, sessionKey: String) {
        val existing = player.activeSkillSessionKey ?: return
        if (existing == sessionKey) {
            player.clearActiveSkillSession()
        }
    }

    @JvmStatic
    fun interruptSession(player: Client) {
        player.clearActiveSkillSession()
    }
}
