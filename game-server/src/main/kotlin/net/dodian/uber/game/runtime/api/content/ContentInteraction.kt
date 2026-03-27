package net.dodian.uber.game.runtime.api.content

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.DispatchTiming
import net.dodian.uber.game.runtime.interaction.ObjectInteractionPolicy
import net.dodian.uber.game.runtime.interaction.PlayerInteractionGuardService
import net.dodian.uber.game.runtime.interaction.PlayerTickThrottleService

typealias ContentObjectInteractionPolicy = ObjectInteractionPolicy
typealias ContentDispatchTiming = DispatchTiming
typealias ContentInteractionType = ObjectInteractionPolicy.InteractionType
typealias ContentObjectDistanceRule = ObjectInteractionPolicy.DistanceRule

object ContentInteraction {
    const val BUTTON_GENERAL: String = PlayerTickThrottleService.BUTTON_GENERAL
    const val TRADE_REQUEST: String = PlayerTickThrottleService.TRADE_REQUEST
    const val DUEL_REQUEST: String = PlayerTickThrottleService.DUEL_REQUEST
    const val TRADE_CONFIRM_STAGE_ONE: String = PlayerTickThrottleService.TRADE_CONFIRM_STAGE_ONE
    const val TRADE_CONFIRM_STAGE_TWO: String = PlayerTickThrottleService.TRADE_CONFIRM_STAGE_TWO
    const val DUEL_CONFIRM_STAGE_ONE: String = PlayerTickThrottleService.DUEL_CONFIRM_STAGE_ONE
    const val DUEL_CONFIRM_STAGE_TWO: String = PlayerTickThrottleService.DUEL_CONFIRM_STAGE_TWO
    const val DUEL_RULES: String = PlayerTickThrottleService.DUEL_RULES
    const val DUEL_BODY_RULES: String = PlayerTickThrottleService.DUEL_BODY_RULES
    const val DUEL_ACCEPT_WIN: String = PlayerTickThrottleService.DUEL_ACCEPT_WIN
    const val CHAT_PRIVACY: String = PlayerTickThrottleService.CHAT_PRIVACY
    const val PICKUP_GROUND_ITEM: String = PlayerTickThrottleService.PICKUP_GROUND_ITEM
    const val CLICK_ITEM: String = PlayerTickThrottleService.CLICK_ITEM
    const val WEB_OBSTACLE: String = PlayerTickThrottleService.WEB_OBSTACLE
    const val YANILLE_CHEST: String = PlayerTickThrottleService.YANILLE_CHEST
    const val LEGENDS_CHEST: String = PlayerTickThrottleService.LEGENDS_CHEST
    const val THIEVING_GENERIC: String = PlayerTickThrottleService.THIEVING_GENERIC

    @JvmStatic
    fun tryAcquire(player: Client, key: String, cooldownTicks: Int): Boolean {
        return PlayerTickThrottleService.tryAcquire(player, key, cooldownTicks)
    }

    @JvmStatic
    fun tryAcquireMs(player: Client, key: String, cooldownMs: Long): Boolean {
        return PlayerTickThrottleService.tryAcquireMs(player, key, cooldownMs)
    }

    @JvmStatic
    fun nearestBoundaryCardinalPolicy(settleTicks: Int = 1): ContentObjectInteractionPolicy {
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_CARDINAL,
            requireMovementSettled = true,
            settleTicks = settleTicks.coerceAtLeast(0),
        )
    }

    @JvmStatic
    fun nearestBoundaryAnyPolicy(settleTicks: Int = 1): ContentObjectInteractionPolicy {
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.NEAREST_BOUNDARY_ANY,
            requireMovementSettled = true,
            settleTicks = settleTicks.coerceAtLeast(0),
        )
    }

    @JvmStatic
    fun legacyObjectDistancePolicy(settleTicks: Int = 1): ContentObjectInteractionPolicy {
        return ObjectInteractionPolicy(
            distanceRule = ObjectInteractionPolicy.DistanceRule.LEGACY_OBJECT_DISTANCE,
            requireMovementSettled = true,
            settleTicks = settleTicks.coerceAtLeast(0),
        )
    }

    @JvmStatic
    fun canStartDialogue(player: Client): Boolean = PlayerInteractionGuardService.canStartDialogue(player)

    @JvmStatic
    fun canOpenBank(player: Client): Boolean = PlayerInteractionGuardService.canOpenBank(player)

    @JvmStatic
    fun canOpenShop(player: Client): Boolean = PlayerInteractionGuardService.canOpenShop(player)

    @JvmStatic
    fun blockingInteractionMessage(player: Client): String? = PlayerInteractionGuardService.blockingInteractionMessage(player)
}
