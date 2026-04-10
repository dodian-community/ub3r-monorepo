package net.dodian.uber.game.engine.lifecycle

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason

object PlayerPersistenceTickService {
    private const val PERIODIC_SAVE_INTERVAL_MS = 60_000L
    private const val PERIODIC_PROGRESS_SAVE_INTERVAL_MS = 3_600_000L

    @JvmStatic
    fun process(player: Client, wallClockNow: Long) {
        if (wallClockNow - player.lastSave >= PERIODIC_SAVE_INTERVAL_MS) {
            player.saveStats(PlayerSaveReason.PERIODIC, false, false)
            player.lastSave = wallClockNow
        }

        if (wallClockNow - player.lastProgressSave >= PERIODIC_PROGRESS_SAVE_INTERVAL_MS) {
            player.saveStats(PlayerSaveReason.PERIODIC_PROGRESS, false, true)
            player.lastProgressSave = wallClockNow
        }
    }
}
