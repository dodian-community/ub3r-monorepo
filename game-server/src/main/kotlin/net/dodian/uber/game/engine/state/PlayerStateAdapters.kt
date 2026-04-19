package net.dodian.uber.game.engine.state

import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.systems.interaction.InteractionIntent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.GroundItem

object GatheringSessionStateAdapter {
    @JvmStatic
    fun begin(player: Client, sessionKey: String): Boolean {
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
    fun end(player: Client, sessionKey: String) {
        if (player.activeSkillSessionKey == sessionKey) {
            player.clearActiveSkillSession()
        }
    }

    @JvmStatic
    fun interrupt(player: Client) {
        player.clearActiveSkillSession()
    }
}

object InteractionSessionStateAdapter {
    @JvmStatic
    fun pending(player: Client): InteractionIntent? = player.pendingInteraction

    @JvmStatic
    fun schedule(player: Client, intent: InteractionIntent) {
        player.pendingInteraction = intent
    }

    @JvmStatic
    fun clear(player: Client) {
        player.pendingInteraction = null
        player.activeInteraction = null
    }

    @JvmStatic
    fun isPending(player: Client, intent: InteractionIntent): Boolean = player.pendingInteraction === intent
}

object GroundItemIntentStateAdapter {
    @JvmStatic
    fun beginPickup(player: Client, target: GroundItem) {
        player.pickupWanted = true
        player.attemptGround = target
    }

    @JvmStatic
    fun clearPickup(player: Client) {
        player.pickupWanted = false
        player.attemptGround = null
    }

    @JvmStatic
    fun wantsPickup(player: Client): Boolean = player.pickupWanted

    @JvmStatic
    fun target(player: Client): GroundItem? = player.attemptGround
}

object TeleportIntentStateAdapter {
    @JvmStatic
    fun isTeleporting(player: Client): Boolean = player.doingTeleport()

    @JvmStatic
    fun didTeleport(player: Client): Boolean = player.didTeleport()

    @JvmStatic
    fun isTeleportingOrTeleported(player: Client): Boolean = player.doingTeleport() || player.didTeleport()
}
