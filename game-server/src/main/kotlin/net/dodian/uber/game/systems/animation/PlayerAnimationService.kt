package net.dodian.uber.game.systems.animation

import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.loop.GameCycleClock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private data class PlayerAnimationRequest(
    val animationId: Int,
    val delay: Int,
    val source: PlayerAnimationSource,
    val priority: Int,
    val requestedCycle: Long,
    val expiresAfterCycle: Long?,
    val sequence: Long,
)

private data class PlayerAnimationState(
    var winningRequest: PlayerAnimationRequest? = null,
)

object PlayerAnimationService {
    private val sequence = AtomicLong()
    private val states = ConcurrentHashMap<Int, PlayerAnimationState>()

    @JvmStatic
    fun request(
        player: Client,
        animationId: Int,
        delay: Int,
        source: PlayerAnimationSource,
        expiresAfterCycle: Long? = null,
    ) {
        val request = PlayerAnimationRequest(
            animationId = animationId,
            delay = delay,
            source = source,
            priority = source.priority,
            requestedCycle = GameCycleClock.currentCycle(),
            expiresAfterCycle = expiresAfterCycle,
            sequence = sequence.incrementAndGet(),
        )
        val state = states.computeIfAbsent(player.slot) { PlayerAnimationState() }
        synchronized(state) {
            val current = state.winningRequest
            if (current == null || shouldReplace(current, request)) {
                state.winningRequest = request
                apply(player, request)
            }
        }
    }

    @JvmStatic
    fun requestAttack(player: Client, animationId: Int, delay: Int = 0) {
        request(player, animationId, delay, PlayerAnimationSource.ATTACK)
    }

    @JvmStatic
    fun requestBlockReaction(player: Client, animationId: Int, delay: Int = 0) {
        request(
            player = player,
            animationId = animationId,
            delay = delay,
            source = PlayerAnimationSource.BLOCK_REACTION,
            expiresAfterCycle = GameCycleClock.currentCycle(),
        )
    }

    @JvmStatic
    fun requestResetClear(player: Client) {
        request(
            player = player,
            animationId = -1,
            delay = 0,
            source = PlayerAnimationSource.RESET_CLEAR,
            expiresAfterCycle = GameCycleClock.currentCycle(),
        )
    }

    @JvmStatic
    fun flush(player: Client, cycleNow: Long) {
        val state = states[player.slot] ?: return
        synchronized(state) {
            val request = state.winningRequest ?: return
            val expired = request.expiresAfterCycle?.let { cycleNow > it } ?: false
            if (expired || request.requestedCycle < cycleNow) {
                state.winningRequest = null
                if (states[player.slot] === state && state.winningRequest == null) {
                    states.remove(player.slot, state)
                }
            }
        }
    }

    private fun shouldReplace(
        current: PlayerAnimationRequest,
        candidate: PlayerAnimationRequest,
    ): Boolean {
        if (candidate.requestedCycle != current.requestedCycle) {
            return candidate.requestedCycle > current.requestedCycle
        }
        if (candidate.priority != current.priority) {
            return candidate.priority > current.priority
        }
        if (candidate.source != current.source) {
            return candidate.sequence > current.sequence
        }
        return candidate.sequence > current.sequence
    }

    private fun apply(
        player: Client,
        request: PlayerAnimationRequest,
    ) {
        player.animationId = request.animationId
        player.animationDelay = request.delay * 10
        player.updateFlags.setRequired(UpdateFlag.ANIM, true)
    }
}
