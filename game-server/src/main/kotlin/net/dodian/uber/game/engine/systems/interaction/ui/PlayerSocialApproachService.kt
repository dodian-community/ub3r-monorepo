package net.dodian.uber.game.engine.systems.interaction.ui

import java.util.concurrent.ConcurrentHashMap
import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.engine.systems.interaction.EntityInteractionReach
import net.dodian.uber.game.engine.systems.interaction.EntityReachResult
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.social.exchange.TradingService
import net.dodian.uber.game.social.exchange.DuelingService
import net.dodian.uber.game.netty.listener.out.SendMessage

/** Owns the movement phase of trade and duel requests. */
object PlayerSocialApproachService {
    private val pending = ConcurrentHashMap<Client, PendingApproach>()

    @JvmStatic
    fun requestTrade(client: Client, targetSlot: Int) = request(client, targetSlot, SocialInteraction.TRADE)

    @JvmStatic
    fun requestDuel(client: Client, targetSlot: Int) = request(client, targetSlot, SocialInteraction.DUEL)

    @JvmStatic
    fun cancel(client: Client) {
        pending.remove(client)
    }

    @JvmStatic
    fun process(client: Client) {
        val state = pending[client] ?: return
        if (state.ticks >= MAX_APPROACH_TICKS) {
            pending.remove(client, state)
            return
        }
        val target =
            if (client.validClient(state.targetSlot) && client.slot != state.targetSlot) {
                client.getClient(state.targetSlot)
            } else {
                null
            }
        if (target == null) {
            pending.remove(client, state)
            return
        }
        when (EntityInteractionReach.resolve(client, target, INTERACTION_DISTANCE)) {
            EntityReachResult.REACHED -> {
                pending.remove(client, state)
                client.claimServerRoutedInteractionMovement()
                execute(client, state)
            }
            EntityReachResult.OVERLAPPING,
            EntityReachResult.OUT_OF_RANGE,
            -> {
                val targetMoved = target.position.x != state.targetX || target.position.y != state.targetY
                val routeExhausted = !client.hasMovementRoute()
                if (targetMoved || routeExhausted) {
                    val refreshed = state.copy(
                        targetX = target.position.x,
                        targetY = target.position.y,
                        ticks = state.ticks + 1,
                    )
                    pending[client] = refreshed
                    if (!route(client, target)) fail(client, target, refreshed)
                } else {
                    pending[client] = state.copy(ticks = state.ticks + 1)
                }
            }
            EntityReachResult.DIFFERENT_PLANE,
            EntityReachResult.INVALID_TARGET,
            -> pending.remove(client, state)
        }
    }

    private fun request(client: Client, targetSlot: Int, interaction: SocialInteraction) {
        client.claimServerRoutedInteractionMovement()
        if (!client.validClient(targetSlot) || client.slot == targetSlot) return
        val target = client.getClient(targetSlot) ?: return
        client.setFocus(target.position.x, target.position.y)
        val state = PendingApproach(targetSlot, interaction, target.position.x, target.position.y, 0)
        when (EntityInteractionReach.resolve(client, target, INTERACTION_DISTANCE)) {
            EntityReachResult.REACHED -> {
                pending.remove(client)
                execute(client, state)
            }
            EntityReachResult.OVERLAPPING,
            EntityReachResult.OUT_OF_RANGE,
            -> {
                pending[client] = state
                if (!route(client, target)) fail(client, target, state)
            }
            EntityReachResult.DIFFERENT_PLANE,
            EntityReachResult.INVALID_TARGET,
            -> pending.remove(client)
        }
    }

    private fun route(client: Client, target: Client): Boolean {
        client.claimServerRoutedInteractionMovement()
        return FollowRouting.routeToEntityBoundary(
            client,
            target.position.x,
            target.position.y,
            target.size,
            target.position.z,
            null,
            false,
        )
    }

    private fun fail(client: Client, target: Client, state: PendingApproach) {
        pending.remove(client, state)
        client.send(SendMessage("I can't reach ${target.playerName}!"))
    }

    private fun execute(client: Client, state: PendingApproach) {
        when (state.interaction) {
            SocialInteraction.TRADE -> TradingService.request(client, state.targetSlot)
            SocialInteraction.DUEL -> DuelingService.request(client, state.targetSlot)
        }
    }

    private data class PendingApproach(
        val targetSlot: Int,
        val interaction: SocialInteraction,
        val targetX: Int,
        val targetY: Int,
        val ticks: Int,
    )

    private enum class SocialInteraction { TRADE, DUEL }

    private const val INTERACTION_DISTANCE = 1
    private const val MAX_APPROACH_TICKS = 40
}
