package net.dodian.uber.game.skill.runtime.action

import net.dodian.uber.game.engine.state.GatheringSessionStateAdapter
import net.dodian.uber.game.model.entity.player.Client

object SkillStateCoordinator {
    @JvmStatic
    fun beginSession(player: Client, sessionKey: String): Boolean {
        return GatheringSessionStateAdapter.begin(player, sessionKey)
    }

    @JvmStatic
    fun endSession(player: Client, sessionKey: String) {
        GatheringSessionStateAdapter.end(player, sessionKey)
    }

    @JvmStatic
    fun interruptSession(player: Client) {
        GatheringSessionStateAdapter.interrupt(player)
    }
}
