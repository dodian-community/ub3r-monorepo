package net.dodian.uber.game.systems.follow

import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object FollowService {
    private val followStates = ConcurrentHashMap<Long, FollowState>()

    @JvmStatic
    fun clear() {
        followStates.clear()
    }

    @JvmStatic
    fun isFollowing(follower: Client): Boolean = followStates.containsKey(follower.longName)

    /**
     * Stores or refreshes the follow intent. The actual movement/mask work is
     * performed on the game thread during tick processing.
     */
    @JvmStatic
    fun requestFollow(
        follower: Client,
        target: Client?,
    ) {
        if (!isUsableFollower(follower)) {
            followStates.remove(follower.longName)
            return
        }
        if (!isUsableTarget(target) || target == null || follower.slot == target.slot || follower.longName == target.longName) {
            followStates.remove(follower.longName)
            return
        }

        followStates[follower.longName] =
            FollowState(
                followerSlot = follower.slot,
                followerLongName = follower.longName,
                targetSlot = target.slot,
                targetLongName = target.longName,
                startedCycle = GameCycleClock.currentCycle(),
                firstTickHandled = false,
                lastTargetX = target.position.x,
                lastTargetY = target.position.y,
                lastTargetDeltaX = target.lastWalkDeltaX,
                lastTargetDeltaY = target.lastWalkDeltaY,
            )
    }

    @JvmStatic
    fun processTick() {
        if (followStates.isEmpty()) {
            return
        }

        val staleKeys = ArrayList<Long>()
        val orderedStates = ArrayList(followStates.entries)
        orderedStates.sortBy { it.value.followerSlot }
        for ((followerKey, state) in orderedStates) {
            val follower = PlayerRegistry.playersOnline[state.followerLongName]
            if (!isUsableFollower(follower) || follower == null || follower.slot != state.followerSlot) {
                staleKeys.add(followerKey)
                continue
            }

            val target = PlayerRegistry.playersOnline[state.targetLongName]
            if (!isUsableTarget(target) || target == null || target.slot != state.targetSlot) {
                clearFollowerState(follower)
                staleKeys.add(followerKey)
                continue
            }

            processFollowingInternal(follower, target, state)
        }

        for (key in staleKeys) {
            followStates.remove(key)
        }
    }

    /**
     * Reassert follow-facing after other per-tick systems have run so incidental
     * non-follow resets do not override active follow interaction masks.
     */
    @JvmStatic
    fun reassertFacingTick() {
        if (followStates.isEmpty()) {
            return
        }
        for ((_, state) in followStates) {
            val follower = PlayerRegistry.playersOnline[state.followerLongName] ?: continue
            if (!isUsableFollower(follower) || follower.slot != state.followerSlot) {
                continue
            }
            // Active combat keeps facing authority.
            if (follower.target != null || follower.combatTargetState != null) {
                continue
            }
            val target = PlayerRegistry.playersOnline[state.targetLongName] ?: continue
            if (!isUsableTarget(target) || target.slot != state.targetSlot) {
                continue
            }
            follower.setFocus(target.position.x, target.position.y)
        }
    }

    /**
     * Applies one tick of follow behavior for the supplied follower/target.
     *
     * The method is also safe to call in tests without pre-registering state;
     * it will seed a fresh state entry if necessary.
     */
    @JvmStatic
    fun processFollowing(
        follower: Client,
        target: Client?,
    ) {
        if (!isUsableFollower(follower)) {
            followStates.remove(follower.longName)
            return
        }

        val state =
            followStates[follower.longName]
                ?: if (isUsableTarget(target) && target != null) {
                    FollowState(
                        followerSlot = follower.slot,
                        followerLongName = follower.longName,
                        targetSlot = target.slot,
                        targetLongName = target.longName,
                        startedCycle = GameCycleClock.currentCycle(),
                        firstTickHandled = false,
                        lastTargetX = target.position.x,
                        lastTargetY = target.position.y,
                        lastTargetDeltaX = target.lastWalkDeltaX,
                        lastTargetDeltaY = target.lastWalkDeltaY,
                    ).also { followStates[follower.longName] = it }
                } else {
                    null
                }

        if (state == null) {
            return
        }
        if (!isUsableTarget(target) || target == null || target.slot != state.targetSlot || target.longName != state.targetLongName) {
            clearFollowerState(follower)
            followStates.remove(follower.longName)
            return
        }

        processFollowingInternal(follower, target, state)
    }

    @JvmStatic
    fun cancelFollow(follower: Client) {
        followStates.remove(follower.longName)
        clearFollowerState(follower)
    }

    @JvmStatic
    fun cancelFollowIntent(follower: Client) {
        followStates.remove(follower.longName)
    }

    private fun processFollowingInternal(
        follower: Client,
        target: Client,
        state: FollowState,
    ) {
        val currentCycle = GameCycleClock.currentCycle()
        val firstFollowTick = !state.firstTickHandled
        // On the first tick of following, clear any stale walking queue.
        if (firstFollowTick && state.startedCycle == currentCycle) {
            clearQueuedWalking(follower)
        }

        // Cancel if on different planes.
        if (follower.position.z != target.position.z) {
            clearFollowerState(follower)
            followStates.remove(follower.longName)
            return
        }

        // Reassert a coordinate-facing mask so follow preserves walking direction visuals.
        follower.setFocus(target.position.x, target.position.y)

        val fx = follower.position.x
        val fy = follower.position.y
        val tx = target.position.x
        val ty = target.position.y
        val z = follower.position.z
        val targetDeltaX = normalizeDelta(target.lastWalkDeltaX)
        val targetDeltaY = normalizeDelta(target.lastWalkDeltaY)
        val targetMoved = state.lastTargetX != tx || state.lastTargetY != ty

        // Luna parity: stop following once Euclidean distance reaches 15+ tiles.
        val dxToTarget = fx - tx
        val dyToTarget = fy - ty
        val squaredDistance = dxToTarget * dxToTarget + dyToTarget * dyToTarget
        if (squaredDistance >= 225) {
            clearFollowerState(follower)
            followStates.remove(follower.longName)
            return
        }

        // Strict Luna behavior: if the target tile is unchanged this tick, keep
        // interaction but do not issue new follow route commands.
        if (!firstFollowTick && !targetMoved) {
            followStates[follower.longName] =
                state.copy(
                    targetSlot = target.slot,
                    targetLongName = target.longName,
                    firstTickHandled = true,
                    lastTargetX = tx,
                    lastTargetY = ty,
                    lastTargetDeltaX = targetDeltaX,
                    lastTargetDeltaY = targetDeltaY,
                )
            return
        }

        val tileDistance = max(abs(tx - fx), abs(ty - fy))
        if (tileDistance == 0) {
            FollowRouting.enqueueRandomCardinalStep(follower, z)
        } else if (tileDistance == 1) {
            clearQueuedWalking(follower)
        } else {
            routeFollowerTowardsTarget(follower, target, state, z)
        }

        followStates[follower.longName] =
            state.copy(
                targetSlot = target.slot,
                targetLongName = target.longName,
                firstTickHandled = true,
                lastTargetX = tx,
                lastTargetY = ty,
                lastTargetDeltaX = targetDeltaX,
                lastTargetDeltaY = targetDeltaY,
            )
    }

    private fun clearFollowerState(follower: Client) {
        clearQueuedWalking(follower)
    }

    private fun clearQueuedWalking(follower: Client) {
        follower.wQueueReadPtr = 0
        follower.wQueueWritePtr = 0
        follower.newWalkCmdSteps = 0
        follower.newWalkCmdIsRunning = false
        follower.walkingBlock = false
    }

    private fun routeFollowerTowardsTarget(
        follower: Client,
        target: Client,
        state: FollowState,
        z: Int,
    ) {
        val preferredDestination = selectWalkBehindDestination(target, state)
        FollowRouting.routeToEntityBoundary(
            follower = follower,
            targetX = target.position.x,
            targetY = target.position.y,
            targetSize = target.getSize(),
            z = z,
            preferredDestination = preferredDestination,
        )
    }

    /**
     * Luna-style walk-behind destination.
     *
     * If the target has a known last movement delta, follow the tile behind that
     * movement direction. Otherwise, walk directly to the target tile.
     */
    private fun selectWalkBehindDestination(target: Client, state: FollowState): Pair<Int, Int> {
        val tx = target.position.x
        val ty = target.position.y
        var dx = target.lastWalkDeltaX.coerceIn(-1, 1)
        var dy = target.lastWalkDeltaY.coerceIn(-1, 1)
        if (dx == 0 && dy == 0) {
            dx = state.lastTargetDeltaX.coerceIn(-1, 1)
            dy = state.lastTargetDeltaY.coerceIn(-1, 1)
        }
        return if (dx == 0 && dy == 0) {
            tx to ty
        } else {
            (tx - dx) to (ty - dy)
        }
    }

    private fun normalizeDelta(delta: Int): Int = delta.coerceIn(-1, 1)

    private fun isUsableFollower(player: Client?): Boolean =
        player != null &&
            player.isActive &&
            !player.disconnected &&
            player.channel?.isActive == true

    private fun isUsableTarget(player: Client?): Boolean =
        player != null &&
            player.isActive &&
            !player.disconnected &&
            player.currentHealth > 0 &&
            !player.isDeathSequenceActive() &&
            player.channel?.isActive == true

}
