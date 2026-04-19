package net.dodian.uber.game.engine.sync.playerinfo.state

import net.dodian.uber.game.engine.sync.playerinfo.dispatch.PlayerSyncRecoveryReason

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client

class PlayerInfoStateValidator {
    private val seen = BooleanArray(Constants.maxPlayers + 1)
    private val touched = IntArray(Constants.maxPlayers + 1)
    private var touchedCount = 0

    fun validate(viewer: Client, state: ViewerPlayerInfoState): PlayerSyncRecoveryReason? {
        clearSeen()
        if (viewer.playerListSize < 0 || viewer.playerListSize > MAX_LOCALS) {
            return PlayerSyncRecoveryReason.LOCAL_COUNT_OUT_OF_RANGE
        }
        for (i in 0 until viewer.playerListSize) {
            val local = viewer.playerList[i] ?: return PlayerSyncRecoveryReason.STALE_LOCAL
            if (!local.isActive) {
                return PlayerSyncRecoveryReason.STALE_LOCAL
            }
            if (markSeen(local.slot)) {
                return PlayerSyncRecoveryReason.DUPLICATE_LOCAL
            }
        }
        val desiredState = state.desiredLocalState
        for (index in desiredState.pendingAddHead until desiredState.pendingAddTail) {
            val slot = desiredState.pendingAddSlots[index]
            if (slot >= 0 && isSeen(slot)) {
                return PlayerSyncRecoveryReason.PENDING_ALREADY_LOCAL
            }
        }
        if (desiredState.lastRegionBaseX != Int.MIN_VALUE &&
            (desiredState.lastRegionBaseX != viewer.mapRegionX ||
                desiredState.lastRegionBaseY != viewer.mapRegionY ||
                desiredState.lastPlane != viewer.position.z)
        ) {
            return PlayerSyncRecoveryReason.REGION_STATE_MISMATCH
        }
        return null
    }

    private fun clearSeen() {
        for (index in 0 until touchedCount) {
            seen[touched[index]] = false
        }
        touchedCount = 0
    }

    private fun markSeen(slot: Int): Boolean {
        if (slot < 0 || slot >= seen.size) {
            return false
        }
        if (seen[slot]) {
            return true
        }
        seen[slot] = true
        touched[touchedCount++] = slot
        return false
    }

    private fun isSeen(slot: Int): Boolean = slot >= 0 && slot < seen.size && seen[slot]

    companion object {
        private const val MAX_LOCALS = 255
    }
}
