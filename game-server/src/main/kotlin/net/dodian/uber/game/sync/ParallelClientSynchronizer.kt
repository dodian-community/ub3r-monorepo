package net.dodian.uber.game.sync

import net.dodian.uber.game.modelkt.entity.Npc
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.game.sync.task.PhasedSynchronizationTask
import net.dodian.uber.game.sync.task.PlayerSynchronizationTask
import net.dodian.utilities.ThreadUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Phaser

class ParallelClientSynchronizer(
    private val executor: ExecutorService = Executors.newFixedThreadPool(
        ThreadUtil.AVAILABLE_PROCESSORS,
        ThreadUtil.create("ClientSynchronizer")
    )
) : ClientSynchronizer() {

    private val phaser = Phaser(1)

    override fun synchronize(players: Iterable<Player>, npcs: Iterable<Npc>) {
        val playerCount = players.count()
        val npcCount = npcs.count()

        phaser.bulkRegister(playerCount)
        players.forEach {
            val task = PlayerSynchronizationTask(it)
            executor.submit(PhasedSynchronizationTask(phaser, task))
        }
        phaser.arriveAndAwaitAdvance()
    }
}