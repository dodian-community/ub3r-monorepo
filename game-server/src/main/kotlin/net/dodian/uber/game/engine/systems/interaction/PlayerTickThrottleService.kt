package net.dodian.uber.game.engine.systems.interaction

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.loop.GameCycleClock

object PlayerTickThrottleService {

    const val BUTTON_GENERAL: String = "button:general"
    const val TRADE_REQUEST: String = "social:trade-request"
    const val DUEL_REQUEST: String = "social:duel-request"
    const val TRADE_CONFIRM_STAGE_ONE: String = "trade:confirm-one"
    const val TRADE_CONFIRM_STAGE_TWO: String = "trade:confirm-two"
    const val DUEL_CONFIRM_STAGE_ONE: String = "duel:confirm-one"
    const val DUEL_CONFIRM_STAGE_TWO: String = "duel:confirm-two"
    const val DUEL_RULES: String = "duel:rules"
    const val DUEL_BODY_RULES: String = "duel:body-rules"
    const val DUEL_ACCEPT_WIN: String = "duel:accept-win"
    const val CHAT_PRIVACY: String = "chat:privacy"
    const val PICKUP_GROUND_ITEM: String = "action:pickup-ground"
    const val CLICK_ITEM: String = "action:click-item"
    const val WEB_OBSTACLE: String = "object:web-obstacle"
    const val YANILLE_CHEST: String = "object:yanille-chest"
    const val LEGENDS_CHEST: String = "object:legends-chest"
    const val THIEVING_GENERIC: String = "skill:thieving"

    @JvmStatic
    @Deprecated(
        message = "Use ContentInteraction.tryAcquire for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentInteraction.tryAcquire(player, key, cooldownTicks)",
            imports = arrayOf("net.dodian.uber.game.api.content.ContentInteraction"),
        ),
    )
    fun tryAcquire(player: Client, key: String, cooldownTicks: Int): Boolean {
        val ticks = cooldownTicks.coerceAtLeast(1).toLong()
        val now = GameCycleClock.currentCycle()
        if (player.getThrottleUntilCycle(key) > now) {
            return false
        }
        player.setThrottleUntilCycle(key, now + ticks)
        return true
    }

    @JvmStatic
    @Deprecated(
        message = "Use ContentInteraction.tryAcquireMs for content-facing calls.",
        replaceWith = ReplaceWith(
            expression = "ContentInteraction.tryAcquireMs(player, key, cooldownMs)",
            imports = arrayOf("net.dodian.uber.game.api.content.ContentInteraction"),
        ),
    )
    fun tryAcquireMs(player: Client, key: String, cooldownMs: Long): Boolean {
        return tryAcquire(player, key, GameCycleClock.ticksForDurationMs(cooldownMs))
    }
}
