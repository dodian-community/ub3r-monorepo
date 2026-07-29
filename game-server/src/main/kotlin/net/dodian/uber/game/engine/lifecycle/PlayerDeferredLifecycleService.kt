package net.dodian.uber.game.engine.lifecycle

import java.util.Collections
import java.util.WeakHashMap
import java.util.function.BooleanSupplier
import net.dodian.uber.game.engine.event.GameEventScheduler
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.state.GroundItemIntentStateAdapter
import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.social.exchange.TradingService
import net.dodian.uber.game.model.item.GroundItem
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.engine.tasking.TaskHandle

object PlayerDeferredLifecycleService {
    private const val GAME_TICK_MS = 600L
    private const val PERIODIC_SAVE_INTERVAL_TICKS = 100
    private const val PERIODIC_PROGRESS_SAVE_INTERVAL_TICKS = 6000

    private data class PlayerDeferredState(
        var xLogExpiryTask: TaskHandle? = null,
        var pickupWatchTask: TaskHandle? = null,
        var periodicSaveTask: TaskHandle? = null,
        var periodicProgressSaveTask: TaskHandle? = null,
        var dailyResetTask: TaskHandle? = null,
        var dailyResetStartAtMs: Long = 0L,
        var pickupRouteTargetX: Int = Int.MIN_VALUE,
        var pickupRouteTargetY: Int = Int.MIN_VALUE,
        var pickupRouteTargetZ: Int = Int.MIN_VALUE,
    )

    private val states = Collections.synchronizedMap(WeakHashMap<Client, PlayerDeferredState>())

    @JvmStatic
    fun scheduleXLogExpiry(player: Client, expiresAtCycle: Long) {
        val state = stateFor(player)
        state.xLogExpiryTask?.cancel()
        val delayTicks = (expiresAtCycle - GameCycleClock.currentCycle()).coerceAtLeast(0L).toInt()
        state.xLogExpiryTask =
            GameEventScheduler.runRepeating(
                delayTicks = delayTicks,
                intervalTicks = 1,
                action =
                    BooleanSupplier {
                        if (player.disconnected || !player.xLog) {
                            state.xLogExpiryTask = null
                            return@BooleanSupplier false
                        }
                        if (player.isWalkBlocked()) {
                            return@BooleanSupplier true
                        }
                        player.UsingAgility = false
                        player.disconnected = true
                        state.xLogExpiryTask = null
                        false
                    },
            )
    }

    @JvmStatic
    fun scheduleGroundPickupArrivalWatch(player: Client, groundItemRef: GroundItem?) {
        cancelGroundPickupArrivalWatch(player)
        if (groundItemRef == null) {
            return
        }
        val state = stateFor(player)
        state.pickupRouteTargetX = Int.MIN_VALUE
        state.pickupRouteTargetY = Int.MIN_VALUE
        state.pickupRouteTargetZ = Int.MIN_VALUE
        state.pickupWatchTask =
            GameEventScheduler.runRepeating(
                delayTicks = 1,
                intervalTicks = 1,
                action =
                    BooleanSupplier {
                        if (player.disconnected || !GroundItemIntentStateAdapter.wantsPickup(player)) {
                            state.pickupWatchTask = null
                            return@BooleanSupplier false
                        }
                        val attempt = GroundItemIntentStateAdapter.target(player) ?: run {
                            state.pickupWatchTask = null
                            return@BooleanSupplier false
                        }
                        if (player.position.x == attempt.x &&
                            player.position.y == attempt.y &&
                            player.position.z == attempt.z
                        ) {
                            player.pickUpItem(attempt.x, attempt.y)
                            if (!GroundItemIntentStateAdapter.wantsPickup(player)) {
                                state.pickupWatchTask = null
                                return@BooleanSupplier false
                            }
                            return@BooleanSupplier true
                        }
                        // Not there yet. The client's own walk request is what got the player
                        // moving, but it's computed locally by the client and doesn't reliably
                        // route around obstacles (unlike object interaction, which is always
                        // server-routed via FollowRouting). Once per new destination, replace
                        // whatever's queued with a real collision-aware server route — mirrors
                        // ObjectApproachRoutingService/InteractionProcessor's lastRoutePosition
                        // guard so we don't re-path every tick while our own route is draining.
                        val alreadyRoutedHere = state.pickupRouteTargetX == attempt.x &&
                            state.pickupRouteTargetY == attempt.y &&
                            state.pickupRouteTargetZ == attempt.z
                        val walkInProgress = player.hasMovementRoute()
                        if (!alreadyRoutedHere || !walkInProgress) {
                            if (FollowRouting.routeToExactTile(player, attempt.x, attempt.y, attempt.z)) {
                                state.pickupRouteTargetX = attempt.x
                                state.pickupRouteTargetY = attempt.y
                                state.pickupRouteTargetZ = attempt.z
                            }
                        }
                        true
                    },
            )
    }

    @JvmStatic
    fun cancelGroundPickupArrivalWatch(player: Client) {
        val state = states[player] ?: return
        state.pickupWatchTask?.cancel()
        state.pickupWatchTask = null
    }

    @JvmStatic
    fun signalTradeFinalizeReady(player: Client) {
        if (!player.inTrade || !player.tradeResetNeeded) {
            return
        }
        val other = player.getClient(player.trade_reqId) ?: return
        if (!player.validClient(player.trade_reqId)) {
            TradingService.reset(player)
            return
        }
        if (other.tradeResetNeeded) {
            TradingService.reset(player)
            TradingService.reset(other)
        }
    }

    @JvmStatic
    fun schedulePeriodicPersistence(player: Client) {
        val state = stateFor(player)
        state.periodicSaveTask?.cancel()
        state.periodicProgressSaveTask?.cancel()

        val now = System.currentTimeMillis()
        val nextSaveAt = player.lastSave + (PERIODIC_SAVE_INTERVAL_TICKS * GAME_TICK_MS)
        val nextProgressAt = player.lastProgressSave + (PERIODIC_PROGRESS_SAVE_INTERVAL_TICKS * GAME_TICK_MS)
        state.periodicSaveTask =
            GameEventScheduler.runRepeating(
                delayTicks = millisToTicksCeil(nextSaveAt - now),
                intervalTicks = PERIODIC_SAVE_INTERVAL_TICKS,
                action =
                    BooleanSupplier {
                        if (player.disconnected) {
                            state.periodicSaveTask = null
                            return@BooleanSupplier false
                        }
                        val tickNow = System.currentTimeMillis()
                        player.saveStats(PlayerSaveReason.PERIODIC, false, false)
                        player.lastSave = tickNow
                        true
                    },
            )
        state.periodicProgressSaveTask =
            GameEventScheduler.runRepeating(
                delayTicks = millisToTicksCeil(nextProgressAt - now),
                intervalTicks = PERIODIC_PROGRESS_SAVE_INTERVAL_TICKS,
                action =
                    BooleanSupplier {
                        if (player.disconnected) {
                            state.periodicProgressSaveTask = null
                            return@BooleanSupplier false
                        }
                        val tickNow = System.currentTimeMillis()
                        player.saveStats(PlayerSaveReason.PERIODIC_PROGRESS, false, true)
                        player.lastProgressSave = tickNow
                        true
                    },
            )
    }

    @JvmStatic
    fun cancelPeriodicPersistence(player: Client) {
        val state = states[player] ?: return
        state.periodicSaveTask?.cancel()
        state.periodicSaveTask = null
        state.periodicProgressSaveTask?.cancel()
        state.periodicProgressSaveTask = null
    }

    @JvmStatic
    fun scheduleDailyResetTrigger(player: Client) {
        val state = stateFor(player)
        state.dailyResetTask?.cancel()
        val startDelayMs = (player.dailyLogin.coerceAtLeast(0).toLong() * GAME_TICK_MS)
        state.dailyResetStartAtMs = System.currentTimeMillis() + startDelayMs
        state.dailyResetTask =
            GameEventScheduler.runRepeating(
                delayTicks = 1,
                intervalTicks = 1,
                action =
                    BooleanSupplier {
                        if (player.disconnected) {
                            state.dailyResetTask = null
                            return@BooleanSupplier false
                        }
                        if (System.currentTimeMillis() < state.dailyResetStartAtMs) {
                            return@BooleanSupplier true
                        }
                        player.dailyLogin = 0
                        player.battlestavesData()
                        true
                    },
            )
    }

    @JvmStatic
    fun cancelDailyResetTrigger(player: Client) {
        val state = states[player] ?: return
        state.dailyResetTask?.cancel()
        state.dailyResetTask = null
    }

    @JvmStatic
    fun cancelAll(player: Client) {
        val state = states.remove(player) ?: return
        state.xLogExpiryTask?.cancel()
        state.pickupWatchTask?.cancel()
        state.periodicSaveTask?.cancel()
        state.periodicProgressSaveTask?.cancel()
        state.dailyResetTask?.cancel()
    }

    private fun stateFor(player: Client): PlayerDeferredState {
        return states.getOrPut(player) { PlayerDeferredState() }
    }

    private fun millisToTicksCeil(millis: Long): Int {
        if (millis <= 0L) {
            return 0
        }
        return ((millis + GAME_TICK_MS - 1L) / GAME_TICK_MS).toInt()
    }
}
